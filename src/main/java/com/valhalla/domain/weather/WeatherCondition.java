package com.valhalla.domain.weather;

import java.util.HashMap;
import java.util.Map;

/**
 * Condición climática derivada del código WMO que devuelve Open-Meteo. Cada constante reúne la
 * descripción en español, el ícono (clases de Font Awesome), la gravedad y el mensaje de alerta
 * que ve la persona cuando el clima puede afectar el plan.
 */
public enum WeatherCondition {
  CLEAR("Despejado", "fa-sun", WeatherSeverity.NONE, null),
  PARTLY_CLOUDY("Parcialmente nublado", "fa-cloud-sun", WeatherSeverity.NONE, null),
  OVERCAST("Nublado", "fa-cloud", WeatherSeverity.NONE, null),
  FOG(
    "Niebla",
    "fa-smog",
    WeatherSeverity.INFO,
    "Hay niebla prevista a la hora del plan. Manejá con precaución."
  ),
  DRIZZLE(
    "Llovizna",
    "fa-cloud-rain",
    WeatherSeverity.INFO,
    "Puede haber lloviznas a la hora del plan."
  ),
  RAIN(
    "Lluvia",
    "fa-cloud-showers-heavy",
    WeatherSeverity.WARNING,
    "Se espera lluvia a la hora del plan. Llevá paraguas."
  ),
  RAIN_SHOWERS(
    "Chaparrones",
    "fa-cloud-showers-heavy",
    WeatherSeverity.WARNING,
    "Se esperan chaparrones a la hora del plan. Llevá paraguas."
  ),
  FREEZING_RAIN(
    "Lluvia helada",
    "fa-cloud-rain",
    WeatherSeverity.SEVERE,
    "Hay lluvia helada prevista a la hora del plan. Extremá las precauciones."
  ),
  SNOW(
    "Nieve",
    "fa-snowflake",
    WeatherSeverity.WARNING,
    "Se espera nieve a la hora del plan. Abrigate."
  ),
  SNOW_SHOWERS(
    "Nevadas",
    "fa-snowflake",
    WeatherSeverity.WARNING,
    "Se esperan nevadas a la hora del plan. Abrigate."
  ),
  THUNDERSTORM(
    "Tormenta",
    "fa-cloud-bolt",
    WeatherSeverity.SEVERE,
    "Hay tormenta prevista a la hora del plan. Considerá reprogramar."
  ),
  UNKNOWN("Sin datos", "fa-circle-question", WeatherSeverity.NONE, null);

  private static final Map<Integer, WeatherCondition> CODE_MAP = buildCodeMap();

  private final String description;
  private final String icon;
  private final WeatherSeverity severity;
  private final String alertMessage;

  WeatherCondition(String description, String icon, WeatherSeverity severity, String alertMessage) {
    this.description = description;
    this.icon = icon;
    this.severity = severity;
    this.alertMessage = alertMessage;
  }

  /** Traduce el código WMO de Open-Meteo; cualquier código desconocido cae en {@link #UNKNOWN}. */
  public static WeatherCondition fromCode(int code) {
    return CODE_MAP.getOrDefault(code, UNKNOWN);
  }

  /** {@code true} cuando la condición amerita mostrar el aviso en el detalle del plan. */
  public boolean isAlert() {
    return severity != WeatherSeverity.NONE;
  }

  public String getDescription() {
    return description;
  }

  public String getIcon() {
    return icon;
  }

  public WeatherSeverity getSeverity() {
    return severity;
  }

  public String getAlertMessage() {
    return alertMessage;
  }

  private static Map<Integer, WeatherCondition> buildCodeMap() {
    Map<Integer, WeatherCondition> map = new HashMap<>();
    register(map, CLEAR, 0);
    register(map, PARTLY_CLOUDY, 1, 2);
    register(map, OVERCAST, 3);
    register(map, FOG, 45, 48);
    register(map, DRIZZLE, 51, 53, 55);
    register(map, FREEZING_RAIN, 56, 57, 66, 67);
    register(map, RAIN, 61, 63, 65);
    register(map, SNOW, 71, 73, 75, 77);
    register(map, RAIN_SHOWERS, 80, 81, 82);
    register(map, SNOW_SHOWERS, 85, 86);
    register(map, THUNDERSTORM, 95, 96, 99);
    return map;
  }

  private static void register(
    Map<Integer, WeatherCondition> map,
    WeatherCondition condition,
    int... codes
  ) {
    for (int code : codes) {
      map.put(code, condition);
    }
  }
}
