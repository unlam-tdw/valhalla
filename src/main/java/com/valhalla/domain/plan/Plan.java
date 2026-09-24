package com.valhalla.domain.plan;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "plans")
@SuppressWarnings("PMD.TooManyFields")
public class Plan {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long IdPlan;

  @NotBlank(message = "Plan name is required")
  @Column(nullable = false)
  private String name;

  private String description;

  private LocalDate eventDate;

  @ManyToOne
  @JoinColumn(name = "administrator_id")
  private User administrator;

  private LocalDate eventDateCreated;

  private LocalTime startTime;

  private LocalTime endTime;

  private String codigo;

  private Boolean isPublic = false;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
    name = "plan_places",
    joinColumns = @JoinColumn(name = "plan_id"),
    inverseJoinColumns = @JoinColumn(name = "place_id")
  )
  private List<Place> places = new ArrayList<>();

  @ManyToOne
  @JoinColumn(name = "selected_place_id")
  private Place selectedPlace;

  @Transient
  private Map<User, Long> participants = new HashMap<>();

  public Long getIdPlan() {
    return IdPlan;
  }

  public void setIdPlan(Long idPlan) {
    IdPlan = idPlan;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public LocalDate getEventDate() {
    return eventDate;
  }

  public void setEventDate(LocalDate eventDate) {
    this.eventDate = eventDate;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public User getAdministrator() {
    return administrator;
  }

  public void setAdministrator(User administrator) {
    this.administrator = administrator;
  }

  public LocalDate getEventDateCreated() {
    return eventDateCreated;
  }

  public void setEventDateCreated(LocalDate eventDateCreated) {
    this.eventDateCreated = eventDateCreated;
  }

  public LocalTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalTime startTime) {
    this.startTime = startTime;
  }

  public LocalTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalTime endTime) {
    this.endTime = endTime;
  }

  public String getCodigo() {
    return codigo;
  }

  public void setCodigo(String codigo) {
    this.codigo = codigo;
  }

  public Boolean getIsPublic() {
    return isPublic != null ? isPublic : false;
  }

  public void setIsPublic(Boolean aPublic) {
    isPublic = aPublic;
  }

  public List<Place> getPlaces() {
    return places;
  }

  public void setPlaces(List<Place> places) {
    this.places = places;
  }

  public Place getSelectedPlace() {
    return selectedPlace;
  }

  public void setSelectedPlace(Place selectedPlace) {
    this.selectedPlace = selectedPlace;
  }

  public Map<User, Long> getParticipants() {
    return participants;
  }

  public void setParticipants(Map<User, Long> participants) {
    this.participants = participants;
  }
}
