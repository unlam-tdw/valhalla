package com.valhalla.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.valhalla.domain.login.LoginService;
import com.valhalla.domain.user.UserRepository;
import com.valhalla.presentation.shared.SessionInterceptor;
import com.valhalla.presentation.shared.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebIntegrationTest
public class LoginControllerTest {

  private static final String LOGIN_EMAIL = "login@unlam.edu.ar";
  private static final String LOGIN_PASSWORD = "secure-password";

  private static final String ATTR_NEW_USER_DATA = "newUserData";
  private static final String BINDING_RESULT_NEW_USER =
    "org.springframework.validation.BindingResult." + ATTR_NEW_USER_DATA;

  @Autowired
  private WebApplicationContext wac;

  @Autowired
  private LoginService loginService;

  @Autowired
  private UserRepository userRepository;

  private MockMvc mockMvc;

  @BeforeEach
  public void setUp() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
    if (userRepository.findByEmail(LOGIN_EMAIL).isEmpty()) {
      loginService.register(LOGIN_EMAIL, LOGIN_PASSWORD, "Login", "Test");
    }
  }

  @Test
  public void shouldShowLandingPage() throws Exception {
    this.mockMvc.perform(get("/"))
      .andExpect(status().isOk())
      .andExpect(view().name("pages/landing"));
  }

  @Test
  public void shouldReturnLoginPage() throws Exception {
    this.mockMvc.perform(get("/admin/login"))
      .andExpect(status().isOk())
      .andExpect(view().name("pages/auth/login"));
  }

  @Test
  @WithMockUser(username = "admin@unlam.edu.ar", roles = { "ADMIN" })
  public void shouldReturnNewUserPageWithNewUserRequest() throws Exception {
    this.mockMvc.perform(get("/admin/new-user"))
      .andExpect(status().isOk())
      .andExpect(view().name("pages/auth/new-user"))
      .andExpect(model().attributeExists(ATTR_NEW_USER_DATA));
  }

  @Test
  @WithMockUser(username = "admin@unlam.edu.ar", roles = { "ADMIN" })
  public void shouldRedirectToHomeWhenCredentialsAreCorrect() throws Exception {
    this.mockMvc.perform(
        post("/admin/validate-login")
          .with(csrf())
          .param("email", LOGIN_EMAIL)
          .param("password", LOGIN_PASSWORD)
      )
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/admin/home"));
  }

  @Test
  public void shouldReRenderLoginWithErrorWhenPasswordIsWrong() throws Exception {
    this.mockMvc.perform(
        post("/admin/validate-login")
          .with(csrf())
          .param("email", LOGIN_EMAIL)
          .param("password", "wrong-password")
      )
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/admin/login?error=true"));
  }

  @Test
  public void shouldRedirectToLoginFromHomeWhenNotAuthenticated() throws Exception {
    this.mockMvc.perform(get("/admin/home")).andExpect(status().is3xxRedirection());
  }

  @Test
  @WithMockUser(username = "admin@unlam.edu.ar", roles = { "ADMIN" })
  public void shouldRedirectToLoginWhenRegisteringWithANewEmail() throws Exception {
    this.mockMvc.perform(
        post("/admin/register")
          .with(csrf())
          .param("firstName", "New")
          .param("lastName", "User")
          .param("email", "new@unlam.edu.ar")
          .param("password", "new-password")
      )
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/admin/login"));
  }

  @Test
  @WithMockUser(username = "admin@unlam.edu.ar", roles = { "ADMIN" })
  public void shouldReRenderNewUserWithErrorWhenEmailAlreadyExists() throws Exception {
    String duplicateEmail = "duplicate@unlam.edu.ar";
    this.mockMvc.perform(
        post("/admin/register")
          .with(csrf())
          .param("firstName", "Dup")
          .param("lastName", "User")
          .param("email", duplicateEmail)
          .param("password", LOGIN_PASSWORD)
      )
      .andExpect(status().is3xxRedirection());

    this.mockMvc.perform(
        post("/admin/register")
          .with(csrf())
          .param("firstName", "Dup")
          .param("lastName", "User")
          .param("email", duplicateEmail)
          .param("password", "another-password")
      )
      .andExpect(status().isOk())
      .andExpect(view().name("pages/auth/new-user"))
      .andExpect(model().attribute("error", "Email is already registered"));
  }

  @Test
  @WithMockUser(username = "admin@unlam.edu.ar", roles = { "ADMIN" })
  public void shouldReRenderNewUserWithValidationErrorsWhenInputIsInvalid() throws Exception {
    this.mockMvc.perform(
        post("/admin/register")
          .with(csrf())
          .param("firstName", "")
          .param("lastName", "")
          .param("email", "not-an-email")
          .param("password", "123")
      )
      .andExpect(status().isOk())
      .andExpect(view().name("pages/auth/new-user"))
      .andExpect(model().attribute("error", "Invalid registration data"))
      .andExpect(model().attributeExists(BINDING_RESULT_NEW_USER));
  }

  @Test
  @WithMockUser(username = "admin@unlam.edu.ar", roles = { "ADMIN" })
  public void shouldShowHomeWhenAuthenticated() throws Exception {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(
      SessionInterceptor.USER_SESSION,
      new UserSession(LOGIN_EMAIL, "ADMIN", "Login", "Test")
    );
    session.setAttribute("loginTime", System.currentTimeMillis());

    this.mockMvc.perform(get("/admin/home").session(session))
      .andExpect(status().isOk())
      .andExpect(view().name("pages/home"))
      .andExpect(model().attributeExists("user"))
      .andExpect(model().attributeExists("loginTime"));
  }

  @Test
  public void shouldRejectPostWithoutCsrf() throws Exception {
    this.mockMvc.perform(
        post("/admin/validate-login").param("email", LOGIN_EMAIL).param("password", LOGIN_PASSWORD)
      )
      .andExpect(status().isForbidden());
  }
}
