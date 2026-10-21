package com.valhalla.integration.config;

import com.valhalla.domain.weather.WeatherForecast;
import com.valhalla.domain.weather.WeatherOutcome;
import com.valhalla.domain.weather.WeatherService;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Reemplaza al {@code WeatherServiceImpl} real en los tests de integración: el pronóstico es un
 * servicio externo y no queremos que la suite dependa de la red. Por defecto no hay pronóstico;
 * cada test que necesite uno lo fija con {@link #stubHourly(List)} o {@link #stubPending()} y lo
 * limpia en {@link #reset()}.
 */
@Configuration
public class WeatherTestConfig {

  private static final AtomicReference<WeatherOutcome> STUB = new AtomicReference<>(
    WeatherOutcome.unavailable()
  );

  public static void stubHourly(List<WeatherForecast> hourly) {
    STUB.set(hourly == null ? WeatherOutcome.unavailable() : WeatherOutcome.available(hourly));
  }

  public static void stubForecast(WeatherForecast forecast) {
    stubHourly(forecast == null ? List.of() : List.of(forecast));
  }

  public static void stubPending() {
    STUB.set(WeatherOutcome.pending());
  }

  public static void reset() {
    STUB.set(WeatherOutcome.unavailable());
  }

  @Bean
  @Primary
  public WeatherService weatherService() {
    return (latitude, longitude, date) -> STUB.get();
  }
}
