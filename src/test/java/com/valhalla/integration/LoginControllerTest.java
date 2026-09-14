package com.valhalla.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebIntegrationTest
public class LoginControllerTest {

  private static final String LOGIN_EMAIL = "login@unlam.edu.ar";
  private static final String LOGIN_PASSWORD = "secure-password";

  private static final String ATTR_LOGIN_DATA = "loginData";
  private static final String ATTR_NEW_USER_DATA = "newUserData";
  private static final String BINDING_RESULT_LOGIN =
    "org.springframework.validation.BindingResult." + ATTR_LOGIN_DATA;
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
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    if (userRepository.findByEmail(LOGIN_EMAIL).isEmpty()) {
      loginService.register(LOGIN_EMAIL, LOGIN_PASSWORD, "Login", "Test");
    }
  }

  @Test
  public void shouldRedirectToLoginPageFromRoot() throws Exception {
    this.mockMvc.perform(get("/"))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/login"));
  }

  @Test
  public void shouldReturnLoginPageWithLoginRequest() throws Exception {
    this.mockMvc.perform(get("/login"))
      .andExpect(status().isOk())
      .andExpect(view().name("pages/auth/login"))
      .andExpect(model().attributeExists(ATTR_LOGIN_DATA));
  }

  @Test
  public void shouldReturnNewUserPageWithNewUserRequest() throws Exception {
    this.mockMvc.perform(get("/new-user"))
      .andExpect(status().isOk())
      .andExpect(view().name("pages/auth/new-user"))
      .andExpect(model().attributeExists(ATTR_NEW_USER_DATA));
  }

  @Test
  public void shouldRedirectToHomeWhenCredentialsAreCorrect() throws Exception {
    this.mockMvc.perform(
        post("/validate-login").param("email", LOGIN_EMAIL).param("password", LOGIN_PASSWORD)
      )
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/home"));
  }

  @Test
  public void shouldReRenderLoginWithErrorWhenPasswordIsWrong() throws Exception {
    this.mockMvc.perform(
        post("/validate-login").param("email", LOGIN_EMAIL).param("password", "wrong-password")
      )
      .andExpect(status().isOk())
      .andExpect(view().name("pages/auth/login"))
      .andExpect(model().attribute("error", "Invalid email or password"))
      .andExpect(model().attributeExists(ATTR_LOGIN_DATA));
  }

  @Test
  public void shouldReRenderLoginWithValidationErrorsWhenEmailIsMissing() throws Exception {
    this.mockMvc.perform(post("/validate-login").param("password", LOGIN_PASSWORD))
      .andExpect(status().isOk())
      .andExpect(view().name("pages/auth/login"))
      .andExpect(model().attribute("error", "Invalid email or password"))
      .andExpect(model().attributeExists(BINDING_RESULT_LOGIN));
  }

  @Test
  public void shouldReRenderLoginWithValidationErrorsWhenEmailIsInvalid() throws Exception {
    this.mockMvc.perform(
        post("/validate-login").param("email", "not-an-email").param("password", LOGIN_PASSWORD)
      )
      .andExpect(status().isOk())
      .andExpect(view().name("pages/auth/login"))
      .andExpect(model().attribute("error", "Invalid email or password"))
      .andExpect(model().attributeExists(BINDING_RESULT_LOGIN));
  }

  @Test
  public void shouldRedirectToLoginWhenRegisteringWithANewEmail() throws Exception {
    this.mockMvc.perform(
        post("/register")
          .param("firstName", "New")
          .param("lastName", "User")
          .param("email", "new@unlam.edu.ar")
          .param("password", "new-password")
      )
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/login"));
  }

  @Test
  public void shouldReRenderNewUserWithErrorWhenEmailAlreadyExists() throws Exception {
    String duplicateEmail = "duplicate@unlam.edu.ar";
    this.mockMvc.perform(
        post("/register")
          .param("firstName", "Dup")
          .param("lastName", "User")
          .param("email", duplicateEmail)
          .param("password", LOGIN_PASSWORD)
      )
      .andExpect(status().is3xxRedirection());

    this.mockMvc.perform(
        post("/register")
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
  public void shouldReRenderNewUserWithValidationErrorsWhenInputIsInvalid() throws Exception {
    this.mockMvc.perform(
        post("/register")
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
  public void shouldRedirectToLoginFromHomeWhenNotAuthenticated() throws Exception {
    this.mockMvc.perform(get("/home"))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/login"));
  }

  @Test
  public void shouldShowHomeWhenAuthenticated() throws Exception {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(
      SessionInterceptor.USER_SESSION,
      new UserSession(LOGIN_EMAIL, "USER", "Login", "Test")
    );
    session.setAttribute("loginTime", System.currentTimeMillis());

    this.mockMvc.perform(get("/home").session(session))
      .andExpect(status().isOk())
      .andExpect(view().name("pages/home"))
      .andExpect(model().attributeExists("user"))
      .andExpect(model().attributeExists("loginTime"));
  }

  @Test
  public void shouldInvalidateSessionAndRedirectToLoginOnLogout() throws Exception {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(
      SessionInterceptor.USER_SESSION,
      new UserSession(LOGIN_EMAIL, "USER", "Login", "Test")
    );

    this.mockMvc.perform(post("/logout").session(session))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/login"));

    assertThat(session.isInvalid(), is(true));
  }
}
