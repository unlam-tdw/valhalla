package com.valhalla.e2e;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
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
  void shouldRenderPlacesAndFilterByCategory() {
    page.navigate(
      "http://127.0.0.1:8080/places",
      new Page.NavigateOptions().setWaitUntil(WaitUntilState.COMMIT)
    );

    assertThat(placeCards().count(), is(equalTo(10)));

    page.locator("select[name='category']").selectOption("RESTAURANT");
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Apply Filters")).click();
    page.waitForURL("**/places?*");

    assertThat(placeCards().count(), is(equalTo(2)));
    assertThat(
      placeCards().allTextContents().stream().allMatch(cardText -> cardText.contains("RESTAURANT")),
      is(true)
    );
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
    return page.locator("main a[href*='/places/']");
  }
}
