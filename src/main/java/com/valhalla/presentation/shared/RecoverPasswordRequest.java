package com.valhalla.presentation.shared;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Password recovery input. AC-07 asks for a valid email, so the format is checked before
 * the domain is asked to rotate anything.
 */
public class RecoverPasswordRequest {

  @NotBlank(message = "Email is required")
  @Email(message = "Email is not valid")
  private String email;

  public RecoverPasswordRequest() {}

  public RecoverPasswordRequest(String email) {
    this.email = email;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }
}
