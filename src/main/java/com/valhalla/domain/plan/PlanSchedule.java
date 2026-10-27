package com.valhalla.domain.plan;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDate;
import java.time.LocalTime;

@Embeddable
public class PlanSchedule {

  @Column(name = "eventDate")
  private LocalDate date;

  @Column(name = "eventTime")
  private LocalTime time;

  // [SCH] Hasta que hora se pueden agendar paradas en cada jornada. Default 23:00 para que una
  // fila vieja, sin la columna, siga comportandose como un plan de dia completo.
  @Column(name = "day_end_time")
  private LocalTime dayEndTime = LocalTime.of(23, 0);

  // [SCH] Modo con el que el planificador estima los traslados. Caminar es el default: la apuesta
  // conservadora para un plan sin preferencia explicita.
  @Enumerated(EnumType.STRING)
  @Column(name = "transport_mode")
  private TransportMode transportMode = TransportMode.WALK;

  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public LocalTime getTime() {
    return time;
  }

  public void setTime(LocalTime time) {
    this.time = time;
  }

  public LocalTime getDayEndTime() {
    return dayEndTime == null ? LocalTime.of(23, 0) : dayEndTime;
  }

  public void setDayEndTime(LocalTime dayEndTime) {
    this.dayEndTime = dayEndTime;
  }

  public TransportMode getTransportMode() {
    return transportMode == null ? TransportMode.WALK : transportMode;
  }

  public void setTransportMode(TransportMode transportMode) {
    this.transportMode = transportMode;
  }
}
