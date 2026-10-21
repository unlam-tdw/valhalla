package com.valhalla.infrastructure.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Recorte de la respuesta de Open-Meteo: solo el bloque {@code hourly} que necesita el plan. Se
 * ignoran el resto de los campos para que un cambio del proveedor no rompa el parseo.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenMeteoResponse(Hourly hourly) {
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Hourly(
    List<String> time,
    @JsonProperty("temperature_2m") List<Double> temperature,
    @JsonProperty("apparent_temperature") List<Double> apparentTemperature,
    @JsonProperty("precipitation_probability") List<Integer> precipitationProbability,
    @JsonProperty("weather_code") List<Integer> weatherCode,
    @JsonProperty("wind_speed_10m") List<Double> windSpeed
  ) {}
}
