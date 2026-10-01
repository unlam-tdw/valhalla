package com.valhalla.config;

import java.util.Properties;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Test JPA configuration: in-memory HSQLDB; only the DataSource and dialect differ. */
@Configuration
public class JpaTestConfig extends BaseJpaConfig {

  /**
   * The service implementations live in {@code com.valhalla.infrastructure}, which this context
   * component-scans, and they all need an encoder. Production gets it from {@code SecurityConfig},
   * which a JPA-only test does not load.
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public DataSource dataSource() {
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName("org.hsqldb.jdbcDriver");
    dataSource.setUrl("jdbc:hsqldb:mem:db_");
    dataSource.setUsername("sa");
    dataSource.setPassword("");
    return dataSource;
  }

  @Override
  protected Properties jpaProperties() {
    Properties properties = new Properties();
    properties.setProperty("hibernate.show_sql", "true");
    properties.setProperty("hibernate.format_sql", "true");
    properties.setProperty("hibernate.hbm2ddl.auto", "create");
    return properties;
  }
}
