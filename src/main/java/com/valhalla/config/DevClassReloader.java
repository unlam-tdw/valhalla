package com.valhalla.config;

import jakarta.servlet.ServletContext;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Polls target/classes for .class file changes and triggers a Jetty context
 * reload after a debounce period. Uses polling instead of WatchService because
 * inotify does not work over Docker Desktop volume mounts.
 */
public final class DevClassReloader {

  private static final Logger LOGGER = Logger.getLogger(DevClassReloader.class.getName());
  private static final long POLL_INTERVAL_MS = 2000;
  private static final long DEBOUNCE_MS = 3000;

  private final Path classesDir;
  private final ScheduledExecutorService scheduler =
    Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread schedulerThread = new Thread(runnable, "class-reload-scheduler");
      schedulerThread.setDaemon(true);
      return schedulerThread;
    });
  private volatile ScheduledFuture<?> pollFuture;
  private volatile ScheduledFuture<?> pendingReload;
  private volatile long lastModified = 0;
  private volatile Object contextHandler;
  private volatile Method reloadMethod;

  public DevClassReloader(Path classesDir) {
    this.classesDir = classesDir;
  }

  public void start(ServletContext ctx) {
    // Use reflection to get Jetty's ContextHandler and its reload() method
    // to avoid compile-time dependency on Jetty internals.
    try {
      // ctx is Jetty's ServletContextHandler$Context which has getContextHandler()
      Method getContextHandler = ctx.getClass().getMethod("getContextHandler");
      this.contextHandler = getContextHandler.invoke(ctx);
      this.reloadMethod = contextHandler.getClass().getMethod("reload");
      if (LOGGER.isLoggable(Level.INFO)) {
        LOGGER.info("DevClassReloader: Jetty reload() available");
      }
    } catch (Exception e) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning(
          "DevClassReloader: cannot access Jetty reload(), disabled: " + e.getMessage()
        );
      }
      return;
    }
    try {
      this.lastModified = scanLastModified();
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Initial class scan failed", e);
    }
    pollFuture =
      scheduler.scheduleWithFixedDelay(
        this::poll,
        POLL_INTERVAL_MS,
        POLL_INTERVAL_MS,
        TimeUnit.MILLISECONDS
      );
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info(
        "DevClassReloader polling: " +
        classesDir.toAbsolutePath() +
        " every " +
        POLL_INTERVAL_MS +
        "ms"
      );
    }
  }

  private void poll() {
    try {
      long current = scanLastModified();
      if (current != lastModified) {
        lastModified = current;
        scheduleReload();
      }
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "DevClassReloader poll error", e);
    }
  }

  private long scanLastModified() throws IOException {
    long max = 0;
    try (var stream = Files.walk(classesDir)) {
      for (Path path : (Iterable<Path>) stream::iterator) {
        if (path.toString().endsWith(".class")) {
          long modified = Files.getLastModifiedTime(path).toMillis();
          if (modified > max) {
            max = modified;
          }
        }
      }
    }
    return max;
  }

  private void scheduleReload() {
    if (pendingReload != null) {
      pendingReload.cancel(false);
    }
    pendingReload = scheduler.schedule(this::triggerReload, DEBOUNCE_MS, TimeUnit.MILLISECONDS);
  }

  private void triggerReload() {
    try {
      if (LOGGER.isLoggable(Level.INFO)) {
        LOGGER.info("Class changes detected, reloading context...");
      }
      reloadMethod.invoke(contextHandler);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Context reload failed", e);
    }
  }

  public void stop() {
    if (pollFuture != null) {
      pollFuture.cancel(false);
    }
    scheduler.shutdownNow();
  }
}
