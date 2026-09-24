package com.valhalla.domain.planplace;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.plan.Plan;
import jakarta.persistence.*;

@Entity
@Table(name = "plan_places")
public class PlanPlace {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plan_id", nullable = false)
  private Plan plan;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "place_id", nullable = false)
  private Place place;

  @Column(nullable = false)
  private Integer position;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Plan getPlan() {
    return plan;
  }

  public void setPlan(Plan plan) {
    this.plan = plan;
  }

  public Place getPlace() {
    return place;
  }

  public void setPlace(Place place) {
    this.place = place;
  }

  public Integer getPosition() {
    return position;
  }

  public void setPosition(Integer position) {
    this.position = position;
  }
}
