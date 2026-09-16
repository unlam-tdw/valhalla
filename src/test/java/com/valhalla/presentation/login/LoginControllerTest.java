package com.valhalla.presentation.login;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.valhalla.domain.exception.UserAlreadyExists;
import com.valhalla.domain.login.LoginService;
import com.valhalla.domain.user.User;
import com.valhalla.presentation.shared.NewUserRequest;
import com.valhalla.presentation.shared.SessionInterceptor;
import com.valhalla.presentation.shared.UserSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.ModelAndView;

public class LoginControllerTest {

  private LoginController controller;
  private HttpServletRequest requestMock;
  private HttpSession sessionMock;
  private LoginService loginServiceMock;
  private NewUserRequest newUserData;

  @BeforeEach
  public void init() {
    requestMock = mock(HttpServletRequest.class);
    sessionMock = mock(HttpSession.class);
    loginServiceMock = mock(LoginService.class);
    controller = new LoginController(loginServiceMock);
    newUserData = new NewUserRequest("Dami", "Test", "dami@unlam.com", "123456");
  }

  @Test
  public void shouldReturnToLoginWhenCredentialsAreWrong() {
    when(loginServiceMock.findUser(anyString(), anyString())).thenReturn(null);
    LoginRequest loginData = new LoginRequest("dami@unlam.com", "123456");
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(loginData, "loginData");

    ModelAndView modelAndView = controller.validateLogin(loginData, bindingResult, requestMock);

    assertThat(modelAndView.getViewName(), equalToIgnoringCase("pages/auth/login"));
    assertThat(
      modelAndView.getModel().get("error").toString(),
      equalToIgnoringCase("Invalid email or password")
    );
    verify(sessionMock, times(0)).setAttribute(anyString(), any());
  }

  @Test
  public void shouldGoToHomeAndStoreSessionWhenCredentialsAreCorrect() {
    User foundUserMock = mock(User.class);
    when(foundUserMock.getEmail()).thenReturn("dami@unlam.com");
    when(foundUserMock.getRole()).thenReturn("ADMIN");
    when(requestMock.getSession()).thenReturn(sessionMock);
    when(loginServiceMock.findUser(anyString(), anyString())).thenReturn(foundUserMock);
    LoginRequest loginData = new LoginRequest("dami@unlam.com", "123456");
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(loginData, "loginData");

    ModelAndView modelAndView = controller.validateLogin(loginData, bindingResult, requestMock);

    assertThat(modelAndView.getViewName(), equalToIgnoringCase("redirect:/admin/home"));
    ArgumentCaptor<UserSession> captor = ArgumentCaptor.forClass(UserSession.class);
    verify(sessionMock, times(1))
      .setAttribute(eq(SessionInterceptor.USER_SESSION), captor.capture());
    assertThat(captor.getValue().getEmail(), equalToIgnoringCase("dami@unlam.com"));
    assertThat(captor.getValue().getRole(), equalToIgnoringCase("ADMIN"));
    verify(sessionMock, times(1)).setAttribute(eq("loginTime"), any(Long.class));
  }

  @Test
  public void shouldReRenderLoginPageWhenInputIsInvalid() {
    LoginRequest loginData = new LoginRequest("", "");
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(loginData, "loginData");
    bindingResult.addError(new FieldError("loginData", "email", "Email is required"));

    ModelAndView modelAndView = controller.validateLogin(loginData, bindingResult, requestMock);

    assertThat(modelAndView.getViewName(), equalToIgnoringCase("pages/auth/login"));
    assertThat(
      modelAndView.getModel().get("error").toString(),
      equalToIgnoringCase("Invalid email or password")
    );
    verify(loginServiceMock, times(0)).findUser(anyString(), anyString());
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
    verify(loginServiceMock, times(0)).register(anyString(), anyString(), anyString(), anyString());
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
  public void shouldReturnLoginPageWithLoginRequest() {
    ModelAndView modelAndView = controller.showLogin();
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("pages/auth/login"));
    assertThat(modelAndView.getModel().get("loginData"), instanceOf(LoginRequest.class));
  }

  @Test
  public void shouldReturnNewUserPageWithEmptyData() {
    ModelAndView modelAndView = controller.showNewUser();
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("pages/auth/new-user"));
    assertThat(modelAndView.getModel().get("newUserData"), instanceOf(NewUserRequest.class));
  }

  @Test
  public void shouldReturnHomeViewWithUserWhenSessionExists() {
    UserSession sessionUser = new UserSession("dami@unlam.com", "ADMIN", "Dami", "Test");
    when(sessionMock.getAttribute(SessionInterceptor.USER_SESSION)).thenReturn(sessionUser);
    when(sessionMock.getAttribute("loginTime")).thenReturn(System.currentTimeMillis());
    ModelAndView modelAndView = controller.showHome(sessionMock);
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("pages/home"));
    assertThat(modelAndView.getModel().get("user"), equalTo(sessionUser));
    assertThat(modelAndView.getModel().get("loginTime"), instanceOf(Long.class));
  }

  @Test
  public void shouldRedirectToLoginWhenNoSessionUser() {
    when(sessionMock.getAttribute(SessionInterceptor.USER_SESSION)).thenReturn(null);
    ModelAndView modelAndView = controller.showHome(sessionMock);
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("redirect:/admin/login"));
  }

  @Test
  public void shouldInvalidateSessionAndRedirectToLoginOnLogout() {
    when(requestMock.getSession(false)).thenReturn(sessionMock);
    ModelAndView modelAndView = controller.logout(requestMock);
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("redirect:/admin/login"));
    verify(sessionMock, times(1)).invalidate();
  }

  @Test
  public void shouldRedirectToLoginOnLogoutWhenNoSession() {
    when(requestMock.getSession(false)).thenReturn(null);
    ModelAndView modelAndView = controller.logout(requestMock);
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("redirect:/admin/login"));
    verify(sessionMock, times(0)).invalidate();
  }
}
