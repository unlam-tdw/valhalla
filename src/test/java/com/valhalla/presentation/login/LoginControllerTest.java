package com.valhalla.presentation.login;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.valhalla.domain.exception.UserAlreadyExists;
import com.valhalla.domain.login.LoginService;
import com.valhalla.presentation.shared.NewUserRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.ModelAndView;

public class LoginControllerTest {

  private LoginController controller;
  private HttpSession sessionMock;
  private LoginService loginServiceMock;
  private NewUserRequest newUserData;

  @BeforeEach
  public void init() {
    sessionMock = mock(HttpSession.class);
    loginServiceMock = mock(LoginService.class);
    controller = new LoginController(loginServiceMock);
    newUserData = new NewUserRequest("Dami", "Test", "dami@unlam.com", "123456");
  }

  @Test
  public void shouldCreateUserAndReturnToLoginWhenEmailIsAvailable() {
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
      newUserData,
      "newUserData"
    );
    ModelAndView modelAndView = controller.register(newUserData, bindingResult);

    assertThat(modelAndView.getViewName(), equalToIgnoringCase("redirect:/admin/login"));
    verify(loginServiceMock, times(1)).register("dami@unlam.com", "123456", "Dami", "Test");
  }

  @Test
  public void shouldReRenderRegistrationFormWhenInputIsInvalid() {
    NewUserRequest invalidData = new NewUserRequest("", "", "", "");
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
      invalidData,
      "newUserData"
    );
    bindingResult.addError(new FieldError("newUserData", "email", "Email is required"));

    ModelAndView modelAndView = controller.register(invalidData, bindingResult);

    assertThat(modelAndView.getViewName(), equalToIgnoringCase("pages/auth/new-user"));
    assertThat(
      modelAndView.getModel().get("error").toString(),
      equalToIgnoringCase("Invalid registration data")
    );
  }

  @Test
  public void shouldReturnNewUserFormWithErrorWhenEmailAlreadyExists() {
    doThrow(UserAlreadyExists.class)
      .when(loginServiceMock)
      .register(anyString(), anyString(), anyString(), anyString());
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
      newUserData,
      "newUserData"
    );
    ModelAndView modelAndView = controller.register(newUserData, bindingResult);
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("pages/auth/new-user"));
    assertThat(
      modelAndView.getModel().get("error").toString(),
      equalToIgnoringCase("Email is already registered")
    );
  }

  @Test
  public void shouldPropagateExceptionOnUnexpectedRegistrationError() {
    doThrow(new RuntimeException())
      .when(loginServiceMock)
      .register(anyString(), anyString(), anyString(), anyString());
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
      newUserData,
      "newUserData"
    );
    assertThrows(RuntimeException.class, () -> controller.register(newUserData, bindingResult));
  }

  @Test
  public void shouldReturnLoginPage() {
    ModelAndView modelAndView = controller.showLogin(null);
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("pages/auth/login"));
  }

  @Test
  public void shouldAddErrorToModelWhenLoginFails() {
    ModelAndView modelAndView = controller.showLogin("true");
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("pages/auth/login"));
    assertThat(
      (String) modelAndView.getModel().get("error"),
      equalToIgnoringCase("Invalid email or password")
    );
  }

  @Test
  public void shouldReturnNewUserPageWithEmptyData() {
    ModelAndView modelAndView = controller.showNewUser();
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("pages/auth/new-user"));
    assertThat(modelAndView.getModel().get("newUserData"), instanceOf(NewUserRequest.class));
  }

  @Test
  public void shouldReturnHomeViewWithLoginTime() {
    when(sessionMock.getAttribute("loginTime")).thenReturn(System.currentTimeMillis());
    ModelAndView modelAndView = controller.showHome(sessionMock);
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("pages/home"));
    assertThat(modelAndView.getModel().get("loginTime"), instanceOf(Long.class));
  }

  @Test
  public void shouldReturnHomeViewWhenNoLoginTime() {
    when(sessionMock.getAttribute("loginTime")).thenReturn(null);
    ModelAndView modelAndView = controller.showHome(sessionMock);
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("pages/home"));
  }

  @Test
  public void shouldRedirectToAdminLoginFromAdminIndex() {
    ModelAndView modelAndView = controller.adminIndex();
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("redirect:/admin/login"));
  }
}
