package com.valhalla.e2e;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

import com.microsoft.playwright.*;
import com.valhalla.e2e.views.LoginPage;
import com.valhalla.e2e.views.NewUserPage;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LoginViewE2E {

  static Playwright playwright;
  static Browser browser;
  BrowserContext context;
  LoginPage loginPage;

  @BeforeAll
  static void openBrowser() {
    playwright = Playwright.create();
    browser = playwright.chromium().launch();
    //browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false).setSlowMo(500));
  }

  @AfterAll
  static void closeBrowser() {
    playwright.close();
  }

  @BeforeEach
  void createContextAndPage() {
    ResetDatabase.cleanDatabase();

    context = browser.newContext();
    Page page = context.newPage();
    loginPage = new LoginPage(page);
  }

  @AfterEach
  void closeContext() {
    context.close();
  }

  @Test
  void shouldShowUNLAMInTheNavbar() throws MalformedURLException {
    givenUserIsOnLoginPage();
    thenShouldSeeUNLAMInNavbar();
  }

  @Test
  void shouldShowErrorWhenSigningInWithAnUnknownUser() {
    givenUserFillsLoginFormWith("damian@unlam.edu.ar", "unlam");
    whenUserClicksSignIn();
    thenShouldSeeAnErrorMessage();
  }

  @Test
  void shouldNavigateToHomeWhenUserExists() throws MalformedURLException {
    givenUserFillsLoginFormWith("test@unlam.edu.ar", "test");
    whenUserClicksSignIn();
    thenShouldBeRedirectedToHome();
  }

  @Test
  void shouldRegisterAUserAndSignInSuccessfully() throws MalformedURLException {
    givenUserNavigatesToRegistrationPage();
    givenUserRegistersWith("juan@unlam.edu.ar", "123456");
    givenUserIsOnLoginPage();
    givenUserFillsLoginFormWith("juan@unlam.edu.ar", "123456");
    whenUserClicksSignIn();
    thenShouldBeRedirectedToHome();
  }

  private void thenShouldSeeUNLAMInNavbar() {
    String text = loginPage.getNavbarText();
    assertThat("UNLAM", equalToIgnoringCase(text));
  }

  private void givenUserIsOnLoginPage() throws MalformedURLException {
    URL loginUrl = loginPage.getCurrentUrl();
    assertThat(loginUrl.getPath(), matchesPattern("^/admin/login(?:;jsessionid=[^/\\s]+)?$"));
  }

  private void whenUserClicksSignIn() {
    loginPage.clickSignIn();
  }

  private void thenShouldBeRedirectedToHome() throws MalformedURLException {
    loginPage.waitForPath("/admin/home");
    URL url = loginPage.getCurrentUrl();
    assertThat(url.getPath(), matchesPattern("^/admin/home(?:;jsessionid=[^/\\s]+)?$"));
    loginPage.waitForSessionTimerToTick();
  }

  private void thenShouldSeeAnErrorMessage() {
    String text = loginPage.getErrorMessage();
    assertThat("Invalid email or password", equalToIgnoringCase(text));
  }

  private void givenUserFillsLoginFormWith(String email, String password) {
    loginPage.typeEmail(email);
    loginPage.typePassword(password);
  }

  private void givenUserNavigatesToRegistrationPage() {
    loginPage.clickRegister();
  }

  private void givenUserRegistersWith(String email, String password) {
    NewUserPage newUserPage = new NewUserPage(context.pages().get(0));
    newUserPage.typeEmail(email);
    newUserPage.typePassword(password);
    newUserPage.clickRegister();
    newUserPage.waitForPath("/admin/login");
  }
}
