package com.valhalla.presentation.shared;

import java.util.Objects;

public class UserSession {

  private final String email;
  private final String role;
  private final String firstName;
  private final String lastName;

  public UserSession(String email, String role, String firstName, String lastName) {
    this.email = email;
    this.role = role;
    this.firstName = firstName;
    this.lastName = lastName;
  }

  public String getEmail() {
    return email;
  }

  public String getRole() {
    return role;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getFullName() {
    if (firstName != null && lastName != null) {
      return firstName + " " + lastName;
    }
    if (firstName != null) {
      return firstName;
    }
    return email;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    UserSession that = (UserSession) other;
    return (
      Objects.equals(email, that.email) &&
      Objects.equals(role, that.role) &&
      Objects.equals(firstName, that.firstName) &&
      Objects.equals(lastName, that.lastName)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(email, role, firstName, lastName);
  }
}
