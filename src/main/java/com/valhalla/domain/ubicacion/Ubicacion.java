package com.valhalla.domain.ubicacion;

import jakarta.persistence.*;

@Entity
@Table(name = "ubicaciones")
public class Ubicacion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String street;
  private int number;
  private int cp;
//  private String localidad;
//  private String provincia;

//  public String getLocalidad() {
//    return localidad;
//  }
//
//  public void setLocalidad(String localidad) {
//    this.localidad = localidad;
//  }
//
//  public String getProvincia() {
//    return provincia;
//  }
//
//  public void setProvincia(String provincia) {
//    this.provincia = provincia;
//  }

  public int getCp() {
    return cp;
  }

  public void setCp(int cp) {
    this.cp = cp;
  }

  public int getNumber() {
    return number;
  }

  public void setNumber(int number) {
    this.number = number;
  }

//  public String getStreet() {
//    return street;
//  }
//
//  public void setStreet(String street) {
//    this.street = street;
//  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }
}
