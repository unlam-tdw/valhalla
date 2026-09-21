package com.valhalla.e2e.views;

import com.microsoft.playwright.Page;

public class LoginPage extends WebPage {

  public LoginPage(Page page) {
    super(page);
    page.navigate(baseUrl() + "/admin/login");
  }

  public String getNavbarText() {
    return this.getElementText("nav a.navbar-brand");
  }

  public String getErrorMessage() {
    return this.getElementText("p.alert.alert-danger.my-4");
  }

  public void typeEmail(String email) {
    this.typeIntoElement("#email", email);
  }

  public void typePassword(String password) {
    this.typeIntoElement("#password", password);
  }

  public void clickSignIn() {
    this.clickElement("#btn-login");
  }

  public void clickLogout() {
    this.clickElement("button:text-is('Logout')");
  }

  public void clickRegister() {
    this.clickElement("#btn-register");
  }

  public void waitForSessionTimerToTick() {
    this.page.waitForFunction(
        "() => { const el = document.getElementById('session-clock-app'); " +
        "return el !== null && el.textContent.includes(':') " +
        "&& !el.textContent.includes('0:00'); }"
      );
  }
}
