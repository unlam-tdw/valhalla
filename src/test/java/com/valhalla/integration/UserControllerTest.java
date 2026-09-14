package com.valhalla.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.valhalla.domain.user.UserRepository;
import com.valhalla.domain.user.UserService;
import com.valhalla.presentation.shared.SessionInterceptor;
import com.valhalla.presentation.shared.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.HiddenHttpMethodFilter;

@WebIntegrationTest
@Transactional
public class UserControllerTest {

  private static final String ADMIN_EMAIL = "admin@unlam.edu.ar";
  private static final String ADMIN_ROLE = "ADMIN";
  private static final String TEST_EMAIL = "test-user@unlam.edu.ar";
  private static final String TEST_PASSWORD = "password123";
  private static final String TEST_ROLE = "USER";

  @Autowired
  private WebApplicationContext wac;

  @Autowired
  private UserService userService;

  @Autowired
  private UserRepository userRepository;

  private MockMvc mockMvc;
  private MockHttpSession adminSession;

  @BeforeEach
  public void setUp() {
    this.mockMvc =
      MockMvcBuilders.webAppContextSetup(this.wac).addFilter(new HiddenHttpMethodFilter()).build();
    this.adminSession = new MockHttpSession();
    this.adminSession.setAttribute(
        SessionInterceptor.USER_SESSION,
        new UserSession(ADMIN_EMAIL, ADMIN_ROLE, "Admin", "Test")
      );
  }

  @Test
  public void shouldRedirectToLoginWhenNotAuthenticated() throws Exception {
    this.mockMvc.perform(get("/users"))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/login"));
  }

  @Test
  public void shouldShowUsersListWhenAuthenticated() throws Exception {
    this.mockMvc.perform(get("/users").session(adminSession))
      .andExpect(status().isOk())
      .andExpect(view().name("pages/admin/users"))
      .andExpect(model().attributeExists("users"))
      .andExpect(model().attributeExists("user"));
  }

  @Test
  public void shouldShowNewUserForm() throws Exception {
    this.mockMvc.perform(get("/users/new").session(adminSession))
      .andExpect(status().isOk())
      .andExpect(view().name("pages/admin/user-form"))
      .andExpect(model().attributeExists("userForm"))
      .andExpect(model().attribute("isEdit", false));
  }

  @Test
  public void shouldCreateUserAndRedirectToList() throws Exception {
    this.mockMvc.perform(
        post("/users")
          .session(adminSession)
          .param("firstName", "New")
          .param("lastName", "User")
          .param("email", "newuser@unlam.edu.ar")
          .param("role", "USER")
      )
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/users"));
  }

  @Test
  public void shouldReRenderFormWithErrorsWhenEmailIsInvalid() throws Exception {
    this.mockMvc.perform(
        post("/users")
          .session(adminSession)
          .param("firstName", "Test")
          .param("lastName", "User")
          .param("email", "not-an-email")
          .param("role", "USER")
      )
      .andExpect(status().isOk())
      .andExpect(view().name("pages/admin/user-form"))
      .andExpect(model().attribute("error", "Invalid user data"))
      .andExpect(model().attributeExists("org.springframework.validation.BindingResult.userForm"));
  }

  @Test
  public void shouldReRenderFormWithErrorsWhenEmailIsMissing() throws Exception {
    this.mockMvc.perform(
        post("/users")
          .session(adminSession)
          .param("firstName", "Test")
          .param("lastName", "User")
          .param("role", "USER")
      )
      .andExpect(status().isOk())
      .andExpect(view().name("pages/admin/user-form"))
      .andExpect(model().attribute("error", "Invalid user data"));
  }

  @Test
  public void shouldShowEditUserForm() throws Exception {
    userService.create("editable@unlam.edu.ar", TEST_PASSWORD, TEST_ROLE, "Editable", "User");
    Long userId = userRepository.findByEmail("editable@unlam.edu.ar").get().getId();

    this.mockMvc.perform(get("/users/" + userId + "/edit").session(adminSession))
      .andExpect(status().isOk())
      .andExpect(view().name("pages/admin/user-form"))
      .andExpect(model().attributeExists("userForm"))
      .andExpect(model().attribute("isEdit", true))
      .andExpect(model().attribute("userId", userId));
  }

  @Test
  public void shouldUpdateUserAndRedirectToList() throws Exception {
    userService.create("updatable@unlam.edu.ar", TEST_PASSWORD, TEST_ROLE, "Updatable", "User");
    Long userId = userRepository.findByEmail("updatable@unlam.edu.ar").get().getId();

    this.mockMvc.perform(
        post("/users/" + userId)
          .session(adminSession)
          .param("_method", "PUT")
          .param("firstName", "Updated")
          .param("lastName", "User")
          .param("email", "updated@unlam.edu.ar")
          .param("role", "ADMIN")
      )
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/users"));
  }

  @Test
  public void shouldDeactivateUserAndRedirectToList() throws Exception {
    userService.create(
      "deactivatable@unlam.edu.ar",
      TEST_PASSWORD,
      TEST_ROLE,
      "Deactivatable",
      "User"
    );
    Long userId = userRepository.findByEmail("deactivatable@unlam.edu.ar").get().getId();

    this.mockMvc.perform(
        post("/users/" + userId + "/deactivate").session(adminSession).param("_method", "PUT")
      )
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/users"));
  }

  @Test
  public void shouldDeleteUserAndRedirectToList() throws Exception {
    userService.create("deletable@unlam.edu.ar", TEST_PASSWORD, TEST_ROLE, "Deletable", "User");
    Long userId = userRepository.findByEmail("deletable@unlam.edu.ar").get().getId();

    this.mockMvc.perform(
        post("/users/" + userId + "/delete").session(adminSession).param("_method", "DELETE")
      )
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/users"));
  }

  @Test
  public void shouldReRenderEditFormWithErrorWhenUpdateInputIsInvalid() throws Exception {
    userService.create(
      "updatable-invalid@unlam.edu.ar",
      TEST_PASSWORD,
      TEST_ROLE,
      "Updatable",
      "Invalid"
    );
    Long userId = userRepository.findByEmail("updatable-invalid@unlam.edu.ar").get().getId();

    this.mockMvc.perform(
        post("/users/" + userId)
          .session(adminSession)
          .param("_method", "PUT")
          .param("firstName", "Updatable")
          .param("lastName", "Invalid")
          .param("email", "")
          .param("role", "ADMIN")
      )
      .andExpect(status().isOk())
      .andExpect(view().name("pages/admin/user-form"))
      .andExpect(model().attribute("isEdit", true))
      .andExpect(model().attribute("userId", userId));
  }

  @Test
  public void shouldActivateUserAndRedirectToList() throws Exception {
    userService.create("activatable@unlam.edu.ar", TEST_PASSWORD, TEST_ROLE, "Activatable", "User");
    Long userId = userRepository.findByEmail("activatable@unlam.edu.ar").get().getId();
    userService.deactivate(userId);

    this.mockMvc.perform(
        post("/users/" + userId + "/activate").session(adminSession).param("_method", "PUT")
      )
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/users"));
  }

  @Test
  public void shouldRotatePasswordAndRedirectToList() throws Exception {
    userService.create("rotatable@unlam.edu.ar", TEST_PASSWORD, TEST_ROLE, "Rotatable", "User");
    Long userId = userRepository.findByEmail("rotatable@unlam.edu.ar").get().getId();

    this.mockMvc.perform(
        post("/users/" + userId + "/rotate-password").session(adminSession).param("_method", "PUT")
      )
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/users"));
  }

  @Test
  public void shouldShowGeneratedPasswordAfterCreate() throws Exception {
    this.mockMvc.perform(
        get("/users").session(adminSession).flashAttr("generatedPassword", "abc12345")
      )
      .andExpect(status().isOk())
      .andExpect(view().name("pages/admin/users"))
      .andExpect(model().attribute("generatedPassword", "abc12345"));
  }
}
