package com.valhalla.config;

import java.util.TimeZone;

/** Centralizes environment variable resolution with sensible defaults. */
public final class EnvironmentConfig {

  private static final String ENV_DB_HOST = "DB_HOST";
  private static final String ENV_DB_PORT = "DB_PORT";
  private static final String ENV_DB_NAME = "DB_NAME";
  private static final String ENV_DB_USER = "DB_USER";
  private static final String ENV_DB_PASSWORD = "DB_PASSWORD";

  private static final String DEFAULT_DB_HOST = "localhost";
  private static final String DEFAULT_DB_PORT = "5432";
  private static final String DEFAULT_DB_NAME = "valhalla";
  private static final String DEFAULT_DB_USER = "user";
  private static final String DEFAULT_DB_PASSWORD = "user";

  private EnvironmentConfig() {}

  public static String dbHost() {
    return envValue(ENV_DB_HOST, DEFAULT_DB_HOST);
  }

  public static String dbPort() {
    return envValue(ENV_DB_PORT, DEFAULT_DB_PORT);
  }

  public static String dbName() {
    return envValue(ENV_DB_NAME, DEFAULT_DB_NAME);
  }

  public static String dbUser() {
    return envValue(ENV_DB_USER, DEFAULT_DB_USER);
  }

  public static String dbPassword() {
    return envValue(ENV_DB_PASSWORD, DEFAULT_DB_PASSWORD);
  }

  public static String databaseUrl() {
    return String.format("jdbc:postgresql://%s:%s/%s", dbHost(), dbPort(), dbName());
  }

  /**
   * Pins the JVM default timezone to UTC. Must be called before opening a PostgreSQL connection.
   *
   * <p>pgjdbc always sends {@code TimeZone.getDefault()} as a startup parameter and ignores the
   * {@code TimeZone} and {@code options} connection properties for it, so the JVM default is the
   * only control point. PostgreSQL 16 rejects names such as {@code America/Buenos_Aires} (the zone
   * was renamed to {@code America/Argentina/Buenos_Aires}) with {@code FATAL: invalid value for
   * parameter "TimeZone"}. The failure is not self-explanatory downstream: Hibernate reports "Unable
   * to determine Dialect without JDBC metadata" and Jetty then answers 503, so the app looks like a
   * wiring bug rather than a clock problem.
   */
  public static void pinTimeZoneToUtc() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  }

  private static String envValue(String variable, String defaultValue) {
    String value = System.getenv(variable);
    return value == null ? defaultValue : value;
  }
}
