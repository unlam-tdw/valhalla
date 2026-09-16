package com.valhalla.e2e.views;

import com.microsoft.playwright.Page;

public class NewUserPage extends WebPage {

  public NewUserPage(Page page) {
    super(page);
  }

  public void typeFirstName(String name) {
    this.typeIntoElement("#firstName", name);
  }

  public void typeLastName(String name) {
    this.typeIntoElement("#lastName", name);
  }

  public void typeEmail(String email) {
    this.typeIntoElement("#email", email);
  }

  public void selectRole(String role) {
    this.page.selectOption("#role", role);
  }

  public void clickCreate() {
    this.clickElement("#btn-create");
  }

  public String getErrorMessage() {
    return this.getElementText("p.alert.alert-danger");
  }
}
