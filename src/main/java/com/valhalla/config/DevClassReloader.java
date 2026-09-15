package com.valhalla.config;

import jakarta.servlet.ServletContext;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Watches target/classes for .class file changes and triggers a Jetty context
 * reload after a debounce period (no changes for 3s). Avoids the race condition
 * with Jetty's built-in scanner which reads partially-written .class files
 * over Docker volume mounts.
 */
public final class DevClassReloader {

  private static final Logger LOGGER = Logger.getLogger(DevClassReloader.class.getName());
  private static final long DEBOUNCE_MS = 3000;

  private final Path classesDir;
  private final ScheduledExecutorService scheduler =
    Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread watcherThread = new Thread(runnable, "class-reload-scheduler");
      watcherThread.setDaemon(true);
      return watcherThread;
    });
  private final ExecutorService watcherExecutor = Executors.newSingleThreadExecutor(runnable -> {
    Thread fileWatcher = new Thread(runnable, "class-file-watcher");
    fileWatcher.setDaemon(true);
    return fileWatcher;
  });
  private volatile ScheduledFuture<?> pendingReload;
  private volatile boolean running = true;
  private volatile ServletContext servletContext;

  public DevClassReloader(Path classesDir) {
    this.classesDir = classesDir;
  }

  public void start(ServletContext ctx) {
    this.servletContext = ctx;
    watcherExecutor.submit(this::watch);
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("DevClassReloader watching: " + classesDir.toAbsolutePath());
    }
  }

  private void watch() {
    try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
      classesDir.register(
        watcher,
        StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_MODIFY
      );

      while (running) {
        WatchKey key = watcher.take();
        for (WatchEvent<?> event : key.pollEvents()) {
          Path changed = (Path) event.context();
          if (changed != null && changed.toString().endsWith(".class")) {
            scheduleReload();
          }
        }
        key.reset();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "DevClassReloader stopped", e);
    }
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
        LOGGER.info("Class changes settled, reloading context...");
      }
      servletContext.setAttribute("org.eclipse.jetty.server.context.reload", this);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Context reload failed", e);
    }
  }

  public void stop() {
    running = false;
    scheduler.shutdownNow();
    watcherExecutor.shutdownNow();
  }
}
