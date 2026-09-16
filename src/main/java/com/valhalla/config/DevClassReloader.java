package com.valhalla.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Polls target/classes for .class changes. When detected, exits the JVM with
 * code 1 so Docker's restart policy restarts the container with fresh classes.
 *
 * Jetty's Maven plugin (MavenWebAppContext) does not support runtime class
 * reloading — contextHandler.reload() throws. A container restart is the only
 * reliable way to pick up new .class files in this setup.
 */
public final class DevClassReloader {

  private static final Logger LOGGER = Logger.getLogger(DevClassReloader.class.getName());
  private static final long POLL_INTERVAL_MS = 2000;

  private final Path classesDir;
  private final ScheduledExecutorService scheduler =
    Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread schedulerThread = new Thread(runnable, "class-reload-scheduler");
      schedulerThread.setDaemon(true);
      return schedulerThread;
    });
  private volatile ScheduledFuture<?> pollFuture;
  private volatile long lastModified = 0;

  public DevClassReloader(Path classesDir) {
    this.classesDir = classesDir;
  }

  public void start() {
    try {
      this.lastModified = scanLastModified();
    } catch (IOException e) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.log(Level.WARNING, "Initial class scan failed", e);
      }
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
      if (current != lastModified && lastModified != 0) {
        if (LOGGER.isLoggable(Level.INFO)) {
          LOGGER.info("Class changes detected, restarting container...");
        }
        // Exit with code 1 → Docker restarts the container → fresh classes loaded
        System.exit(1); // NOPMD — intentional: dev-only container restart trigger
      }
      lastModified = current;
    } catch (IOException e) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.log(Level.WARNING, "DevClassReloader poll error", e);
      }
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

  public void stop() {
    if (pollFuture != null) {
      pollFuture.cancel(false);
    }
    scheduler.shutdownNow();
  }
}
