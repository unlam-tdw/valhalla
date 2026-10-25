package com.valhalla.domain.plan;

/**
 * [SCH] Modo de traslado entre dos paradas del itinerario, con la velocidad que usa el planificador.
 *
 * <p>La velocidad es efectiva, no de circulacion pura: ya incluye la espera del transporte y el
 * tiempo de estacionamiento. Asi el horario calculado no promete llegadas que despues no se
 * cumplen.
 */
public enum TransportMode {
  WALK(4.5),
  BIKE(14),
  BUS(16),
  CAR(22);

  private final double effectiveSpeedKmh;

  TransportMode(double effectiveSpeedKmh) {
    this.effectiveSpeedKmh = effectiveSpeedKmh;
  }

  /** Kilometros por hora efectivos del modo, espera y estacionamiento incluidos. */
  public double speedKmh() {
    return effectiveSpeedKmh;
  }
}
