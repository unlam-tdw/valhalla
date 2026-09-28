package com.valhalla.e2e.views;

import com.microsoft.playwright.Page;

/** Self-service registration: email and password only (AC-01). */
public class RegisterPage extends WebPage {

  public RegisterPage(Page page) {
    super(page);
    page.navigate(baseUrl() + "/auth/register");
  }

  public String getErrorMessage() {
    return this.getElementText("p.alert.alert-danger");
  }

  public void typeEmail(String email) {
    this.typeIntoElement("#email", email);
  }

  public void typePassword(String password) {
    this.typeIntoElement("#password", password);
  }

  public void clickRegister() {
    this.clickElement("#btn-register");
  }
}
