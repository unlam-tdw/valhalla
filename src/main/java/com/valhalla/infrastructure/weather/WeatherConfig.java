package com.valhalla.infrastructure.weather;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Cliente HTTP dedicado al pronóstico. Timeouts cortos a propósito: el clima es un agregado de la
 * vista, así que un proveedor lento no debe demorar el render del detalle del plan.
 */
@Configuration
public class WeatherConfig {

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(4);

  @Bean
  public RestTemplate weatherRestTemplate() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(CONNECT_TIMEOUT);
    factory.setReadTimeout(READ_TIMEOUT);
    return new RestTemplate(factory);
  }
}
