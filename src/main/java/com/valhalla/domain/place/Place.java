package com.valhalla.domain.place;

import com.valhalla.domain.ubicacion.Ubicacion;
import jakarta.persistence.*;

@Entity
@Table(name = "places")
public class Place {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  private String description;

  public Ubicacion getLocation() {
    return location;
  }

  public void setLocation(Ubicacion location) {
    this.location = location;
  }

  private Ubicacion location;

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
}
