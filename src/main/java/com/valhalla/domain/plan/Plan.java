package com.valhalla.domain.plan;

import com.valhalla.domain.planplace.PlanPlace;
import com.valhalla.domain.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "plans")
public class Plan {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column
  private String description;

  private LocalDate eventDate;

  private Boolean isPublic = false;

  @Column(unique = true)
  private String shortCode;

  @ManyToOne
  @JoinColumn(name = "administrator_id")
  private User administrator;

  @OneToMany(mappedBy = "plan", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  @OrderBy("sortOrder ASC")
  private List<PlanPlace> planPlaces = new ArrayList<>();

  public List<PlanPlace> getPlanPlaces() {
    return planPlaces;
  }

  public void addPlanPlace(PlanPlace planPlace) {
    planPlaces.add(planPlace);
    planPlace.setPlan(this);
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public Boolean getIsPublic() {
    return isPublic != null ? isPublic : false;
  }

  public void setIsPublic(Boolean isPublic) {
    this.isPublic = isPublic;
  }

  public String getShortCode() {
    return shortCode;
  }

  public void setShortCode(String shortCode) {
    this.shortCode = shortCode;
  }

  public User getAdministrator() {
    return administrator;
  }

  public void setAdministrator(User administrator) {
    this.administrator = administrator;
  }

  /**
   * Copies only the fields the form owns. {@code id}, {@code shortCode} and {@code administrator}
   * are deliberately left alone: a form post never carries them, so copying them would blank the
   * owner and the share code on every update.
   */
  public void updateFrom(Plan changes) {
    this.name = changes.getName();
    this.description = changes.getDescription();
    this.eventDate = changes.getEventDate();
    this.isPublic = changes.getIsPublic();
  }
}
