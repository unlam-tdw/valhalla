package com.valhalla.e2e;

import com.valhalla.config.EnvironmentConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.TimeZone;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Resets the database to a known state using JDBC directly (no external tools). */
public class ResetDatabase {

  private ResetDatabase() {}

  /** Deletes all users and seeds the default admin user via JDBC. */
  public static void cleanDatabase() {
    String bcryptHash = new BCryptPasswordEncoder().encode("password");
    String[] statements = {
      "DELETE FROM users",
      "ALTER SEQUENCE users_id_seq RESTART WITH 1",
      "INSERT INTO users(email, password, role, active) " +
      "VALUES('test@unlam.edu.ar', '" +
      bcryptHash +
      "', 'ADMIN', TRUE) ON CONFLICT (email) DO NOTHING",
    };

    try (Connection connection = openConnection()) {
      for (String sql : statements) {
        try (Statement statement = connection.createStatement()) {
          statement.execute(sql);
        }
      }
      System.out.println("Database cleaned successfully");
    } catch (SQLException e) {
      System.err.println("Error cleaning the database: " + e.getMessage());
    }
  }

  private static Connection openConnection() throws SQLException {
    // Force UTC timezone — PostgreSQL rejects the deprecated "America/Buenos_Aires"
    // name that the JDBC driver picks up from the JVM default timezone.
    TimeZone previousTz = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    java.util.Properties props = new java.util.Properties();
    props.setProperty("user", EnvironmentConfig.dbUser());
    props.setProperty("password", EnvironmentConfig.dbPassword());
    try {
      return DriverManager.getConnection(EnvironmentConfig.databaseUrl(), props);
    } finally {
      TimeZone.setDefault(previousTz);
    }
  }
}
