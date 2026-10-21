package com.valhalla.domain.weather;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Pronóstico puntual del plan: la condición predominante, la temperatura y el viento a la hora del
 * evento. Es un valor inmutable; {@code condition} incluye la descripción, el ícono y la alerta.
 */
public record WeatherForecast(
  LocalDate date,
  LocalTime time,
  double temperatureC,
  double apparentTemperatureC,
  int precipitationProbability,
  double windSpeedKmh,
  WeatherCondition condition
) {}
