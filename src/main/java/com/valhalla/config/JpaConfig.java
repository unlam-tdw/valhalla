package com.valhalla.config;

import java.util.Properties;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Production JPA configuration: PostgreSQL with parameters from environment variables. */
@Configuration
public class JpaConfig extends BaseJpaConfig {

  @Bean
  public DataSource dataSource() {
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName("org.postgresql.Driver");
    dataSource.setUrl(EnvironmentConfig.databaseUrl());
    dataSource.setUsername(EnvironmentConfig.dbUser());
    dataSource.setPassword(EnvironmentConfig.dbPassword());
    return dataSource;
  }

  @Override
  protected Properties jpaProperties() {
    Properties properties = new Properties();
    properties.setProperty("hibernate.show_sql", "true");
    properties.setProperty("hibernate.format_sql", "true");
    properties.setProperty("hibernate.hbm2ddl.auto", "update");
    return properties;
  }
}
