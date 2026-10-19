package com.valhalla.domain.weather;

/**
 * Estado del pronóstico para la fecha y hora del plan.
 *
 * <ul>
 *   <li>{@link #AVAILABLE}: hay pronóstico para ese momento.</li>
 *   <li>{@link #PENDING}: la fecha está más allá del horizonte del proveedor, todavía no hay datos
 *       pero probablemente los haya más cerca de la fecha.</li>
 *   <li>{@link #UNAVAILABLE}: no hay nada que mostrar (sin coordenadas, fecha pasada o el proveedor
 *       no responde).</li>
 * </ul>
 */
public enum WeatherStatus {
  AVAILABLE,
  PENDING,
  UNAVAILABLE,
}
