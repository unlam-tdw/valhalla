package com.valhalla.e2e.views;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class UsersPage extends WebPage {

  public UsersPage(Page page) {
    super(page);
  }

  public void navigateToUsers() {
    page.navigate("localhost:8080/admin/users");
  }

  public String getHeading() {
    return this.getElementText("h2");
  }

  public int getUserCount() {
    return this.page.locator("tbody tr").count();
  }

  public boolean hasRowWithEmail(String email) {
    return (
      this.page.locator("tbody tr").filter(new Locator.FilterOptions().setHasText(email)).count() >
      0
    );
  }

  public void clickNewUser() {
    this.clickElement("a[href='/users/new']");
  }

  public void clickEditOnRow(String email) {
    this.page.locator("tbody tr")
      .filter(new Locator.FilterOptions().setHasText(email))
      .locator("a:has-text('Edit')")
      .click();
  }

  public void clickDeactivateOnRow(String email) {
    this.page.locator("tbody tr")
      .filter(new Locator.FilterOptions().setHasText(email))
      .locator("button:has-text('Deactivate')")
      .click();
  }

  public void clickDeleteOnRow(String email) {
    this.page.locator("tbody tr")
      .filter(new Locator.FilterOptions().setHasText(email))
      .locator("button:has-text('Delete')")
      .click();
  }

  public void waitForDeactivated(String email) {
    this.page.locator("tbody tr")
      .filter(new Locator.FilterOptions().setHasText(email))
      .locator("span:has-text('Inactive')")
      .waitFor();
  }
}
