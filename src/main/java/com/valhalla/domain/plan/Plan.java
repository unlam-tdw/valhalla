package com.valhalla.domain.plan;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.planplace.PlanPlace;
import com.valhalla.domain.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Entity
@Table(name = "plans")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long IdPlan;

    @NotBlank(message = "Plan name is required")
    @Column(nullable = false)
    private String name;

    private String description;

    private Date eventDate;

    @ManyToOne
    @JoinColumn(name = "administrator_id", nullable = false)
    private User administrator;

    private Date eventDateCreated;

    private LocalTime startTime;

    private LocalTime endTime;

    private String codigo;

    private Boolean isPublic;

    @ManyToMany
    @JoinTable(
            name = "plan_places",
            joinColumns = @JoinColumn(name = "plan_id"),
            inverseJoinColumns = @JoinColumn(name = "place_id")
    )
    private ArrayList<Place> places = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "selected_place_id")
    private Place selectedPlace = null;

    private Map<User, Long> participants = new HashMap<User, Long>();

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

    public Date getEventDate() {
        return eventDate;
    }

    public void setEventDate(Date eventDate) {
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

    public Date getEventDateCreated() {
        return eventDateCreated;
    }

    public void setEventDateCreated(Date eventDateCreated) {
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

    public Boolean getPublic() {
        return isPublic;
    }

    public void setPublic(Boolean aPublic) {
        isPublic = aPublic;
    }

    public ArrayList<Place> getPlaces() {
        return places;
    }

    public void setPlaces(ArrayList<Place> places) {
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