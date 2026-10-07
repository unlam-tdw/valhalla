package com.valhalla.domain.plan;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.time.LocalTime;

@Embeddable
public class PlanSchedule {

  @Column(name = "eventDate")
  private LocalDate date;

  @Column(name = "eventTime")
  private LocalTime time;

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
}
