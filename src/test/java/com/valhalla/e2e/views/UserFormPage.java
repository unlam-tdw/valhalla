package com.valhalla.e2e.views;

import com.microsoft.playwright.Page;

public class UserFormPage extends WebPage {

  public UserFormPage(Page page) {
    super(page);
  }

  public String getHeading() {
    return this.getElementText("h3");
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
    this.page.locator("#role")
      .evaluate("el => el.dispatchEvent(new Event('change', {bubbles: true}))");
  }

  public void clickCreate() {
    this.clickElement("#btn-create");
  }

  public void clickCancel() {
    this.clickElement("a:has-text('Cancel')");
  }
}
