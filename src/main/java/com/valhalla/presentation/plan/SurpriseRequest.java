package com.valhalla.presentation.plan;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/** [SURPRISE] Formulario del botón "Sorprendeme" del feed. */
public class SurpriseRequest {

  @NotNull(message = "El presupuesto es obligatorio")
  @DecimalMin(value = "0.00", message = "El presupuesto no puede ser negativo")
  private BigDecimal budget;

  @NotNull(message = "La fecha es obligatoria")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate date;

  private Integer people;

  private String style;

  public BigDecimal getBudget() {
    return budget;
  }

  public void setBudget(BigDecimal budget) {
    this.budget = budget;
  }

  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public Integer getPeople() {
    return people;
  }

  public void setPeople(Integer people) {
    this.people = people;
  }

  public String getStyle() {
    return style;
  }

  public void setStyle(String style) {
    this.style = style;
  }
}
