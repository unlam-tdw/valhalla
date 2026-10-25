package com.valhalla.presentation.plan;

import com.valhalla.domain.plan.TransportMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

public class PlanRequest {

  @NotBlank(message = "Plan name is required")
  private String name;

  private String description;

  // [SCH] La fecha es la base del itinerario: sin ella no hay jornada donde ubicar las paradas, asi
  // que el plan no se puede crear.
  @NotNull(message = "Plan date is required")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate eventDate;

  @DateTimeFormat(pattern = "HH:mm")
  private LocalTime eventTime;

  // [SCH] Ventana de cada jornada y modo de traslado con los que el planificador arma el horario.
  @DateTimeFormat(pattern = "HH:mm")
  private LocalTime dayEndTime;

  private TransportMode transportMode;

  private Boolean isPublic;

  @jakarta.validation.Valid
  private com.valhalla.domain.plan.PlanSettings settings =
    new com.valhalla.domain.plan.PlanSettings();

  public com.valhalla.domain.plan.PlanSettings getSettings() {
    return settings;
  }

  public void setSettings(com.valhalla.domain.plan.PlanSettings settings) {
    this.settings = settings;
  }

  private List<PlanPlaceRequest> places = new ArrayList<>();

  public PlanRequest() {}

  public PlanRequest(String name, String description, LocalDate eventDate, Boolean isPublic) {
    this.name = name;
    this.description = description;
    this.eventDate = eventDate;
    this.isPublic = isPublic;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public LocalDate getEventDate() {
    return eventDate;
  }

  public void setEventDate(LocalDate eventDate) {
    this.eventDate = eventDate;
  }

  public LocalTime getEventTime() {
    return eventTime;
  }

  public void setEventTime(LocalTime eventTime) {
    this.eventTime = eventTime;
  }

  public LocalTime getDayEndTime() {
    return dayEndTime;
  }

  public void setDayEndTime(LocalTime dayEndTime) {
    this.dayEndTime = dayEndTime;
  }

  public TransportMode getTransportMode() {
    return transportMode;
  }

  public void setTransportMode(TransportMode transportMode) {
    this.transportMode = transportMode;
  }

  public Boolean getIsPublic() {
    return isPublic;
  }

  public void setIsPublic(Boolean isPublic) {
    this.isPublic = isPublic;
  }

  public List<PlanPlaceRequest> getPlaces() {
    return places;
  }

  public void setPlaces(List<PlanPlaceRequest> places) {
    this.places = places;
  }
}
