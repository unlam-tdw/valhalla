package com.valhalla.e2e;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;

import com.valhalla.e2e.views.ForgotPasswordPage;
import com.valhalla.e2e.views.LoginPage;
import com.valhalla.e2e.views.RecoveredPasswordPage;
import com.valhalla.e2e.views.RegisterPage;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Browser coverage of the public auth flow. Scenario ids E-01 and E-02 of docs/specs/08-AUT.md.
 *
 * <p>E2eBase wipes the database before each test, so the single admin seed is all that exists
 * beforehand and a fixed email per test cannot collide with a previous run.
 */
public class UserAuthViewE2E extends E2eBase {

  private static final String EMAIL = "aut.registrado@unlam.edu.ar";
  private static final String PASSWORD = "secret123";

  @Test
  void shouldRegisterLoginAndLandOnPlans() throws MalformedURLException {
    givenUserRegisters(EMAIL, PASSWORD);

    givenUserSignsInWith(EMAIL, PASSWORD);

    thenShouldBeOnPath("/plans");
  }

  @Test
  void shouldRecoverPasswordAndSignIn() throws MalformedURLException {
    givenUserRegisters(EMAIL, PASSWORD);

    String tempPassword = whenUserRecoversPassword(EMAIL);
    assertThat("recovery must hand back a password", tempPassword, not(emptyOrNullString()));

    givenUserSignsInWith(EMAIL, tempPassword);

    thenShouldBeOnPath("/plans");
  }

  // --- steps ---

  private void givenUserRegisters(String email, String password) throws MalformedURLException {
    RegisterPage registerPage = new RegisterPage(page);
    registerPage.typeEmail(email);
    registerPage.typePassword(password);
    registerPage.clickRegister();

    thenShouldBeOnPath("/auth/login");
  }

  private void givenUserSignsInWith(String email, String password) {
    LoginPage loginPage = new LoginPage(page, "/auth/login");
    loginPage.typeEmail(email);
    loginPage.typePassword(password);
    loginPage.clickSignIn();
  }

  /** Reads the temporary password off the confirmation view, which only exists after a rotation. */
  private String whenUserRecoversPassword(String email) {
    ForgotPasswordPage forgotPasswordPage = new ForgotPasswordPage(page);
    forgotPasswordPage.typeEmail(email);
    forgotPasswordPage.clickRecover();
    return new RecoveredPasswordPage(page).getTempPassword();
  }

  private void thenShouldBeOnPath(String path) throws MalformedURLException {
    URL url = URI.create(page.url()).toURL();
    assertThat(
      "expected to land on " + path + " but landed on " + url,
      url.getPath(),
      is(matchesPattern("^" + Pattern.quote(path) + "(?:;jsessionid=[^/\\s]+)?$"))
    );
  }
}
