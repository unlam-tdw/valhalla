package com.valhalla.e2e;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PlansViewE2E {

  static Playwright playwright;
  static Browser browser;
  BrowserContext context;
  Page page;

  @BeforeAll
  static void openBrowser() {
    playwright = Playwright.create();
    browser = playwright.chromium().launch();
  }

  @AfterAll
  static void closeBrowser() {
    playwright.close();
  }

  @BeforeEach
  void createContextAndLogin() {
    ResetDatabase.cleanDatabase();

    context = browser.newContext();
    page = context.newPage();

    page.navigate("http://localhost:8080/admin/login");
    page.fill("input[name=email]", "test@unlam.edu.ar");
    page.fill("input[name=password]", "password");
    page.click("button[type=submit]");
    page.waitForURL("**/admin/home");
  }

  @AfterEach
  void closeContext() {
    context.close();
  }

  @Test
  void T_PLN_014_crearPlan_verEnListaYEnDetalle() {
    page.navigate("http://localhost:8080/planes/crear");

    page.fill("input[name=name]", "Viaje E2E de prueba");
    page.click("button[type=submit]");

    page.waitForURL("**/planes");
    assertThat(page.content(), containsString("Viaje E2E de prueba"));

    page.click("a.detail-button");
    assertThat(page.content(), containsString("Viaje E2E de prueba"));

    page.navigate("http://localhost:8080/planes");

    page.onDialog(dialog -> dialog.accept());
    page.click("button.delete-button");

    page.waitForURL("**/planes");
    assertThat(page.content(), not(containsString("Viaje E2E de prueba")));
  }
}