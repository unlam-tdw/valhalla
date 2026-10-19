package com.valhalla.domain.weather;

import java.time.LocalDate;

/**
 * Pronóstico del clima para un punto y una fecha. Devuelve {@link WeatherOutcome#pending()} cuando
 * la fecha está más allá del horizonte del proveedor y {@link WeatherOutcome#unavailable()} cuando
 * no hay datos (sin coordenadas, fecha pasada o el proveedor no responde). La hora no hace falta:
 * la serie horaria del día permite mostrar el clima a cualquier hora del recorrido.
 */
@FunctionalInterface
public interface WeatherService {
  WeatherOutcome hourlyFor(Double latitude, Double longitude, LocalDate date);
}
