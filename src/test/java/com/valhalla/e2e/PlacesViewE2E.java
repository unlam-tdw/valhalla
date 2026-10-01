package com.valhalla.e2e;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PlacesViewE2E {

  private static Playwright playwright;
  private static Browser browser;
  private BrowserContext context;
  private Page page;

  @BeforeAll
  static void openBrowser() {
    playwright = Playwright.create();
    browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
  }

  @AfterAll
  static void closeBrowser() {
    playwright.close();
  }

  @BeforeEach
  void createAuthenticatedContext() {
    ResetDatabase.cleanDatabase();
    context = browser.newContext();
    page = context.newPage();
    loginAsAdmin();
  }

  @AfterEach
  void closeContext() {
    context.close();
  }

  @Test
  void shouldRenderMapWithMarkersAndFilterByCategory() {
    page.navigate(
      "http://127.0.0.1:8080/places",
      new Page.NavigateOptions().setWaitUntil(WaitUntilState.COMMIT)
    );

    // Wait for Vue to mount and Leaflet to load
    page.waitForSelector(
      "#map.leaflet-container",
      new Page.WaitForSelectorOptions().setTimeout(10000)
    );
    page.waitForSelector(
      ".leaflet-interactive",
      new Page.WaitForSelectorOptions().setTimeout(10000)
    );

    // AC-01: Map loads centered on Buenos Aires
    assertThat(page.locator("#map.leaflet-container").count(), is(equalTo(1)));

    // AC-02: All places shown as markers (10 places seeded)
    assertThat(page.locator(".leaflet-interactive").count(), is(equalTo(10)));

    // AC-05: Sidebar shows all place cards
    assertThat(placeCards().count(), is(equalTo(10)));

    // AC-06: Filter by category updates markers AND sidebar
    page.locator("aside select").selectOption("RESTAURANT");
    page.waitForTimeout(500);

    assertThat(page.locator(".leaflet-interactive").count(), is(equalTo(2)));
    assertThat(placeCards().count(), is(equalTo(2)));
    assertThat(
      placeCards().allTextContents().stream().allMatch(cardText -> cardText.contains("RESTAURANT")),
      is(true)
    );

    // AC-07: Search by name filters markers AND sidebar
    page.locator("aside select").selectOption("");
    page.locator("aside input[type='text']").fill("Don");
    page.waitForTimeout(500);

    assertThat(page.locator(".leaflet-interactive").count(), is(equalTo(1)));
    assertThat(placeCards().count(), is(equalTo(1)));
    assertThat(placeCards().first().textContent().contains("Don"), is(true));

    // Clear filters restores all results
    page.locator("button:has-text('Clear Filters')").click();
    page.waitForTimeout(500);

    assertThat(page.locator(".leaflet-interactive").count(), is(equalTo(10)));
    assertThat(placeCards().count(), is(equalTo(10)));
  }

  @Test
  void shouldShowPopupOnMarkerClick() {
    page.navigate(
      "http://127.0.0.1:8080/places",
      new Page.NavigateOptions().setWaitUntil(WaitUntilState.COMMIT)
    );

    page.waitForSelector(
      ".leaflet-interactive",
      new Page.WaitForSelectorOptions().setTimeout(10000)
    );

    // AC-04: Click marker shows popup with name, category, and link
    page.locator(".leaflet-interactive").first().click();
    page.waitForSelector(".leaflet-popup", new Page.WaitForSelectorOptions().setTimeout(5000));

    Locator popup = page.locator(".leaflet-popup");
    assertThat(popup.count(), is(equalTo(1)));
    assertThat(popup.textContent().length(), is(greaterThan(0)));
    assertThat(popup.locator("a[href*='/places/']").count(), is(greaterThan(0)));
  }

  private void loginAsAdmin() {
    page.navigate(
      "http://127.0.0.1:8080/admin/login",
      new Page.NavigateOptions().setWaitUntil(WaitUntilState.COMMIT)
    );
    page.locator("#email").fill("test@unlam.edu.ar");
    page.locator("#password").fill("password");
    page.locator("#btn-login").click();
    page.waitForURL("**/admin/home");
  }

  private Locator placeCards() {
    return page.locator("aside a[href*='/places/']");
  }
}
