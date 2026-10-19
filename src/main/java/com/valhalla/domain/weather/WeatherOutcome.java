package com.valhalla.domain.weather;

import java.util.List;

/**
 * Resultado de pedir el clima para un plan. Cuando el estado es {@link WeatherStatus#AVAILABLE},
 * {@code hourly} trae la serie horaria del dia; en {@link WeatherStatus#PENDING} y
 * {@link WeatherStatus#UNAVAILABLE} es vacia.
 */
public record WeatherOutcome(WeatherStatus status, List<WeatherForecast> hourly) {
  /** Hay pronóstico para la fecha del plan: la serie horaria completa del día. */
  public static WeatherOutcome available(List<WeatherForecast> hourly) {
    return new WeatherOutcome(WeatherStatus.AVAILABLE, hourly);
  }

  /** La fecha todavía está fuera del horizonte de pronóstico: no hay datos aún. */
  public static WeatherOutcome pending() {
    return new WeatherOutcome(WeatherStatus.PENDING, List.of());
  }

  /** No hay pronóstico que mostrar. */
  public static WeatherOutcome unavailable() {
    return new WeatherOutcome(WeatherStatus.UNAVAILABLE, List.of());
  }
}
