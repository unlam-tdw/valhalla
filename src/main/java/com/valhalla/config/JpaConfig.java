package com.valhalla.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Properties;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JpaConfig extends BaseJpaConfig {

  /** Techo del pool: la app es un WAR unico, no un cluster de procesos peleando por conexiones. */
  private static final int MAX_POOL_SIZE = 10;

  /**
   * Pool y no una conexion por transaccion: {@code DriverManagerDataSource} abria una conexion
   * fisica nueva para cada transaccion y la cerraba al terminar, o sea handshake TCP y
   * autenticacion contra PostgreSQL por request.
   */
  @Bean
  public DataSource dataSource() {
    HikariConfig config = new HikariConfig();
    config.setDriverClassName("org.postgresql.Driver");
    config.setJdbcUrl(EnvironmentConfig.databaseUrl());
    config.setUsername(EnvironmentConfig.dbUser());
    config.setPassword(EnvironmentConfig.dbPassword());
    config.setMaximumPoolSize(MAX_POOL_SIZE);
    config.setPoolName("valhalla");
    return new HikariDataSource(config);
  }

  @Override
  protected Properties jpaProperties() {
    Properties properties = new Properties();
    // El SQL por consola se apaga en produccion: formatearlo cuesta en cada request y no lo lee
    // nadie fuera de una sesion de debug. Los tests conservan su propio JpaTestConfig.
    properties.setProperty("hibernate.show_sql", "false");
    properties.setProperty("hibernate.format_sql", "false");
    properties.setProperty("hibernate.hbm2ddl.auto", "update");
    return properties;
  }
}
