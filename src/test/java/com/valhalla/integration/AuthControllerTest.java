package com.valhalla.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.valhalla.domain.login.LoginService;
import com.valhalla.domain.user.User;
import com.valhalla.domain.user.UserRepository;
import com.valhalla.domain.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * End-to-end-ish coverage of the three security chains behind the public auth flow.
 * Scenario ids I-01..I-13 of docs/specs/08-AUT.md.
 *
 * <p>I-11 ({@code GET /places} anonymous -> {@code /auth/login}) and I-12 ({@code GET /plans}
 * anonymous -> {@code /auth/login}) are already covered by
 * {@code SecurityConfigTest#shouldRedirectToLoginWhenAccessingPlacesWithoutSession} and its
 * sibling, so they are not duplicated here.
 *
 * <p>The {@code SecurityConfig} permission checks for these same routes live in
 * {@code SecurityConfigTest} as S-01 and S-05, where the spec files them; this class owns what
 * the routes render.
 */
@WebIntegrationTest
public class AuthControllerTest {

  private static final String USER_EMAIL = "aut.user@unlam.edu.ar";
  private static final String USER_PASSWORD = "user-password";
  private static final String PASSWORD = "secret123";
  private static final String VALIDATE_LOGIN = "/auth/validate-login";

  @Autowired
  private WebApplicationContext wac;

  @Autowired
  private LoginService loginService;

  @Autowired
  private UserService userService;

  @Autowired
  private UserRepository userRepository;

  private MockMvc mockMvc;

  @BeforeEach
  public void setUp() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
    if (userRepository.findByEmail(USER_EMAIL).isEmpty()) {
      loginService.register(USER_EMAIL, USER_PASSWORD);
    }
  }

  // --- I-01 ---

  @Test
  public void shouldRenderTheRegisterPage() throws Exception {
    this.mockMvc.perform(get("/auth/register"))
      .andExpect(status().isOk())
      .andExpect(view().name("pages/auth/user/register"))
      .andExpect(content().string(not(containsString("firstName"))));
  }

  // --- I-17 ---

  @Test
  public void shouldRenderTheForgotPasswordPage() throws Exception {
    this.mockMvc.perform(get("/auth/forgot-password"))
      .andExpect(status().isOk())
      .andExpect(view().name("pages/auth/user/forgot-password"))
      // The submit button only renders if Thymeleaf bound the recoverPasswordRequest form object.
      .andExpect(content().string(containsString("id=\"btn-recover\"")));
  }

  // --- I-02 ---

  @Test
  public void shouldRegisterAndThenLetTheUserLogIn() throws Exception {
    String email = uniqueEmail();

    this.mockMvc.perform(
        post("/auth/register").with(csrf()).param("email", email).param("password", PASSWORD)
      )
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/auth/login"));

    this.mockMvc.perform(
        post(VALIDATE_LOGIN).with(csrf()).param("username", email).param("password", PASSWORD)
      )
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/plans"));
  }

  // --- I-03 ---

  @Test
  public void shouldReRenderRegisterWithAnErrorOnDuplicateEmail() throws Exception {
    String email = uniqueEmail();
    register(email, PASSWORD);

    this.mockMvc.perform(
        post("/auth/register").with(csrf()).param("email", email).param("password", PASSWORD)
      )
      .andExpect(status().isOk())
      .andExpect(view().name("pages/auth/user/register"))
      .andExpect(content().string(containsString("Ese email ya está registrado")));
  }

  @Test
  public void shouldReRenderRegisterWithValidationErrors() throws Exception {
    this.mockMvc.perform(
        post("/auth/register").with(csrf()).param("email", "not-an-email").param("password", "123")
      )
      .andExpect(status().isOk())
      .andExpect(view().name("pages/auth/user/register"))
      .andExpect(content().string(containsString("Email is not valid")))
      .andExpect(content().string(containsString("Password must be at least 6 characters")));
  }

  // --- I-04 ---

  @Test
  public void shouldLandOnPlansWhenAUserLogsIn() throws Exception {
    this.mockMvc.perform(
        post(VALIDATE_LOGIN)
          .with(csrf())
          .param("username", USER_EMAIL)
          .param("password", USER_PASSWORD)
      )
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/plans"));
  }

  @Test
  public void shouldLandOnAdminHomeWhenAnAdminLogsInThroughTheUserForm() throws Exception {
    String email = uniqueEmail();
    userService.create(email, PASSWORD, "ADMIN", "Admin", "AUT");

    this.mockMvc.perform(
        post(VALIDATE_LOGIN).with(csrf()).param("username", email).param("password", PASSWORD)
      )
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/admin/home"));
  }

  // --- I-05 ---

  @Test
  public void shouldRedirectToLoginWithErrorOnWrongPassword() throws Exception {
    this.mockMvc.perform(
        post(VALIDATE_LOGIN)
          .with(csrf())
          .param("username", USER_EMAIL)
          .param("password", "wrong-password")
      )
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/auth/login?error=true"));
  }

  // --- I-06 ---

  @Test
  public void shouldRedirectToLoginWithErrorForADeactivatedUser() throws Exception {
    String email = uniqueEmail();
    userService.create(email, PASSWORD, "USER", "Deactivated", "AUT");
    User user = userRepository.findByEmail(email).orElseThrow();
    userService.deactivate(user.getId());

    this.mockMvc.perform(
        post(VALIDATE_LOGIN).with(csrf()).param("username", email).param("password", PASSWORD)
      )
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/auth/login?error=true"));
  }

  // --- I-07 ---

  @Test
  public void shouldRejectLoginPostWithoutCsrf() throws Exception {
    this.mockMvc.perform(
        post(VALIDATE_LOGIN).param("username", USER_EMAIL).param("password", USER_PASSWORD)
      )
      .andExpect(status().isForbidden());
  }

  // --- I-08 ---

  @Test
  public void shouldRotateThePasswordAndInvalidateTheOldOne() throws Exception {
    String email = uniqueEmail();
    register(email, PASSWORD);

    String tempPassword = extractTempPassword(
      this.mockMvc.perform(post("/auth/recover").with(csrf()).param("email", email))
    );

    logIn(email, PASSWORD)
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/auth/login?error=true"));

    logIn(email, tempPassword)
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/plans"));
  }

  // --- I-09 ---

  @Test
  public void shouldTellTheUserTheEmailIsUnknownOnRecovery() throws Exception {
    this.mockMvc.perform(post("/auth/recover").with(csrf()).param("email", "nadie@unlam.edu.ar"))
      .andExpect(status().isOk())
      .andExpect(view().name("pages/auth/user/forgot-password"))
      .andExpect(content().string(containsString("Email no encontrado")))
      .andExpect(content().string(not(containsString("temp-password"))));
  }

  // --- I-10 ---

  @Test
  public void shouldRedirectToLoginAndInvalidateTheSessionOnLogout() throws Exception {
    MockHttpSession session = new MockHttpSession();
    this.mockMvc.perform(
        post(VALIDATE_LOGIN)
          .session(session)
          .with(csrf())
          .param("username", USER_EMAIL)
          .param("password", USER_PASSWORD)
      )
      .andExpect(redirectedUrl("/plans"));

    this.mockMvc.perform(post("/auth/logout").session(session).with(csrf()))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/auth/login?logout=true"));

    assertThat("session must be invalidated after logout", session.isInvalid(), is(true));
  }

  // --- I-13 ---

  @Test
  public void shouldKeepAdminUsersBehindTheAdminLogin() throws Exception {
    this.mockMvc.perform(get("/admin/users"))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrlPattern("**/admin/login"));
  }

  // --- AC-13 ---

  @Test
  public void shouldShowRegisterLinkInNavbarWhenAnonymous() throws Exception {
    this.mockMvc.perform(get("/auth/login"))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("href=\"/auth/register\"")));
  }

  @Test
  @WithMockUser(username = "user@unlam.edu.ar", roles = { "USER" })
  public void shouldShowLogoutFormInNavbarOnceSignedIn() throws Exception {
    this.mockMvc.perform(get("/"))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("action=\"/auth/logout\"")));
  }

  // --- AC-14 ---

  @Test
  public void shouldKeepAdminPermissionsAfterLoggingInThroughTheUserForm() throws Exception {
    String email = uniqueEmail();
    userService.create(email, PASSWORD, "ADMIN", "Admin", "AUT");
    MockHttpSession session = logInAndKeepSession(email, PASSWORD);

    this.mockMvc.perform(get("/admin/users").session(session)).andExpect(status().isOk());
  }

  // --- helpers ---

  private static String uniqueEmail() {
    return "aut-" + java.util.UUID.randomUUID().toString().substring(0, 8) + "@unlam.edu.ar";
  }

  private void register(String email, String password) throws Exception {
    this.mockMvc.perform(
        post("/auth/register").with(csrf()).param("email", email).param("password", password)
      )
      .andExpect(status().is3xxRedirection());
  }

  private ResultActions logIn(String email, String password) throws Exception {
    MockHttpServletRequestBuilder request = post(VALIDATE_LOGIN)
      .with(csrf())
      .param("username", email)
      .param("password", password);
    return this.mockMvc.perform(request);
  }

  /**
   * Logs in through the user form and hands the session back, so a later request can prove the
   * session it produced is the one being authorized.
   */
  private MockHttpSession logInAndKeepSession(String email, String password) throws Exception {
    MockHttpSession session = new MockHttpSession();
    this.mockMvc.perform(
        post(VALIDATE_LOGIN)
          .session(session)
          .with(csrf())
          .param("username", email)
          .param("password", password)
      )
      .andExpect(status().is3xxRedirection());
    return session;
  }

  private static String extractTempPassword(ResultActions actions) throws Exception {
    String body = actions.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    java.util.regex.Matcher matcher = java.util.regex.Pattern
      .compile("id=\"temp-password\"[^>]*>([^<]+)<")
      .matcher(body);
    assertThat("the recovery view must show a temporary password", matcher.find(), is(true));
    assertThat(matcher.group(1).trim().isEmpty(), is(false));
    return matcher.group(1).trim();
  }
}
