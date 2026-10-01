package com.valhalla.e2e;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;

import com.valhalla.e2e.views.ForgotPasswordPage;
import com.valhalla.e2e.views.LoginPage;
import com.valhalla.e2e.views.RecoveredPasswordPage;
import com.valhalla.e2e.views.RegisterPage;
import com.valhalla.e2e.views.WebPage;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Browser coverage of the public auth flow. Scenario ids E-01..E-08 of docs/specs/08-AUT.md.
 *
 * <p>E2eBase wipes the database before each test, so the single admin seed is all that exists
 * beforehand and a fixed email per test cannot collide with a previous run.
 */
public class UserAuthViewE2E extends E2eBase {

  private static final String EMAIL = "aut.registrado@unlam.edu.ar";
  private static final String PASSWORD = "secret123";
  private static final String WRONG_PASSWORD = "no-es-la-clave";
  private static final String SHORT_PASSWORD = "123";
  private static final String UNKNOWN_EMAIL = "nadie@unlam.edu.ar";

  @Test
  void shouldRegisterLoginAndLandOnTheLandingPage() throws MalformedURLException {
    givenUserRegisters(EMAIL, PASSWORD);

    givenUserSignsInWith(EMAIL, PASSWORD);

    thenShouldBeOnPath("/");

    // The navbar is one fragment shared by both chains, so its brand is covered once on the admin
    // login page. What has no counterpart there is the signed-in half of the fragment, which only
    // renders once a session exists and is what this chain exercises.
    WebPage landing = new WebPage(page);
    assertThat(
      "the authenticated navbar names the signed-in user",
      landing.getNavbarSignedInAs(),
      containsString(EMAIL)
    );
    // Naming the whole set, not just the presence of the items: the navbar used to offer a
    // "Planes" link to /plans, which had no controller. It rendered fine, so no other assertion
    // could see the difference between a live item and a 404 waiting to be clicked. PlanController
    // landed in 03-PLN, so "Plans" belongs back in the set and now has somewhere to resolve to.
    assertThat(
      "the authenticated navbar offers only items that resolve",
      landing.getNavbarItems(),
      contains("UNLAM", "Plans", "Logout")
    );
  }

  @Test
  void shouldRecoverPasswordAndSignIn() throws MalformedURLException {
    givenUserRegisters(EMAIL, PASSWORD);

    String tempPassword = whenUserRecoversPassword(EMAIL);
    assertThat("recovery must hand back a password", tempPassword, not(emptyOrNullString()));

    givenUserSignsInWith(EMAIL, tempPassword);

    thenShouldBeOnPath("/");
  }

  @Test
  void shouldSignOutAndReturnToLogin() throws MalformedURLException {
    givenUserRegisters(EMAIL, PASSWORD);
    LoginPage loginPage = givenUserSignsInWith(EMAIL, PASSWORD);

    loginPage.clickLogout();

    // Asserting the notice, not just the path: the logout redirect carries ?logout=true, and a
    // path-only assertion would also pass on a page that never rendered the signed-out state.
    assertThat(
      "the session-gone notice only renders on a completed logout",
      loginPage.getLogoutNotice(),
      containsString("Has cerrado sesión")
    );
    thenShouldBeOnPath("/auth/login");
  }

  @Test
  void shouldTellTheUserTheEmailIsAlreadyRegistered() throws MalformedURLException {
    givenUserRegisters(EMAIL, PASSWORD);

    RegisterPage registerPage = new RegisterPage(page);
    registerPage.typeEmail(EMAIL);
    registerPage.typePassword(PASSWORD);
    registerPage.clickRegister();

    assertThat(registerPage.getErrorMessage(), containsString("Ese email ya está registrado"));
    thenShouldBeOnPath("/auth/register");
  }

  @Test
  void shouldRejectAPasswordShorterThanSixCharacters() throws MalformedURLException {
    RegisterPage registerPage = new RegisterPage(page);
    registerPage.typeEmail(EMAIL);
    registerPage.typePassword(SHORT_PASSWORD);
    registerPage.clickRegister();

    assertThat(
      "the DTO owns the minimum length, not a minlength attribute",
      registerPage.getFieldError(),
      containsString("at least 6 characters")
    );
    thenShouldBeOnPath("/auth/register");
  }

  @Test
  void shouldNotSendAMalformedEmailToTheServer() throws MalformedURLException {
    RegisterPage registerPage = new RegisterPage(page);
    registerPage.typeEmail("not-an-email");
    registerPage.typePassword(PASSWORD);
    registerPage.clickRegister();

    assertThat(
      "type=email makes the browser block the submit, so the DTO's @Email never sees it",
      registerPage.isEmailRejectedByBrowser(),
      is(true)
    );
    thenShouldBeOnPath("/auth/register");
  }

  @Test
  void shouldTellTheUserTheCredentialsAreWrong() throws MalformedURLException {
    givenUserRegisters(EMAIL, PASSWORD);

    LoginPage loginPage = givenUserSignsInWith(EMAIL, WRONG_PASSWORD);

    // Read the alert before the path: the locator auto-waits, which removes the race between the
    // failed-login redirect and the assertion. Reusing the instance matters — the alert is keyed
    // off ?error=true, so re-navigating to /auth/login would drop it.
    assertThat(loginPage.getErrorMessage(), containsString("inválidos"));
    thenShouldBeOnPath("/auth/login");
  }

  @Test
  void shouldTellTheUserTheEmailIsUnknownOnRecovery() throws MalformedURLException {
    ForgotPasswordPage forgotPasswordPage = new ForgotPasswordPage(page);
    forgotPasswordPage.typeEmail(UNKNOWN_EMAIL);
    forgotPasswordPage.clickRecover();

    assertThat(forgotPasswordPage.getErrorMessage(), containsString("Email no encontrado"));
    // The recover handler returns the view without redirecting, so the browser stays on the POST
    // target. That is what a user sees; asserting /auth/forgot-password here would encode a
    // redirect the app does not do.
    thenShouldBeOnPath("/auth/recover");
  }

  // --- steps ---

  private void givenUserRegisters(String email, String password) throws MalformedURLException {
    RegisterPage registerPage = new RegisterPage(page);
    registerPage.typeEmail(email);
    registerPage.typePassword(password);
    registerPage.clickRegister();

    thenShouldBeOnPath("/auth/login");
  }

  /** Hands the page back so a caller can keep driving the page the sign-in left it on. */
  private LoginPage givenUserSignsInWith(String email, String password) {
    LoginPage loginPage = new LoginPage(page, "/auth/login");
    loginPage.typeEmail(email);
    loginPage.typePassword(password);
    loginPage.clickSignIn();
    return loginPage;
  }

  /** Reads the temporary password off the confirmation view, which only exists after a rotation. */
  private String whenUserRecoversPassword(String email) {
    ForgotPasswordPage forgotPasswordPage = new ForgotPasswordPage(page);
    forgotPasswordPage.typeEmail(email);
    forgotPasswordPage.clickRecover();
    return new RecoveredPasswordPage(page).getTempPassword();
  }

  private void thenShouldBeOnPath(String path) throws MalformedURLException {
    new WebPage(page).waitForPath(path);
    URL url = URI.create(page.url()).toURL();
    assertThat(
      "expected to land on " + path + " but landed on " + url,
      url.getPath(),
      is(matchesPattern("^" + Pattern.quote(path) + "(?:;jsessionid=[^/\\s]+)?$"))
    );
  }
}
