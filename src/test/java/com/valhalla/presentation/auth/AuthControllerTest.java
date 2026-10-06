package com.valhalla.presentation.auth;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.valhalla.domain.exception.UserAlreadyExists;
import com.valhalla.domain.login.LoginService;
import com.valhalla.infrastructure.user.RecoverPasswordService;
import com.valhalla.presentation.shared.RecoverPasswordRequest;
import com.valhalla.presentation.shared.RegisterRequest;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/** Unit tests for {@link AuthController}. Scenario ids U-01..U-10 of docs/specs/08-AUT.md. */
public class AuthControllerTest {

  private static final String EMAIL = "nuevo@unlam.edu.ar";
  private static final String PASSWORD = "secret123";
  private static final String LOGIN_VIEW = "pages/auth/user/login";
  private static final String REGISTER_VIEW = "pages/auth/user/register";
  private static final String FORGOT_VIEW = "pages/auth/user/forgot-password";
  private static final String RECOVERED_VIEW = "pages/auth/user/recovered";

  private static final org.springframework.validation.Validator VALIDATOR = localValidator();

  private static LocalValidatorFactoryBean localValidator() {
    LocalValidatorFactoryBean factoryBean = new LocalValidatorFactoryBean();
    factoryBean.afterPropertiesSet();
    return factoryBean;
  }

  private LoginService loginService;
  private RecoverPasswordService recoverPasswordService;
  private AuthController controller;

  @BeforeEach
  public void init() {
    loginService = mock(LoginService.class);
    recoverPasswordService = mock(RecoverPasswordService.class);
    controller = new AuthController(loginService, recoverPasswordService);
  }

  // --- U-01, U-02 ---

  @Test
  public void shouldReturnTheLoginView() {
    assertThat(controller.showLoginForm(null, new ExtendedModelMap(), null), is(LOGIN_VIEW));
  }

  @Test
  public void shouldAddErrorToModelWhenLoginFails() {
    ExtendedModelMap model = new ExtendedModelMap();

    controller.showLoginForm("true", model, null);

    assertThat(model.get("error"), is(true));
  }

  @Test
  public void shouldNotAddErrorToModelWhenErrorParamIsNotTrue() {
    ExtendedModelMap falseModel = new ExtendedModelMap();
    ExtendedModelMap absentModel = new ExtendedModelMap();

    controller.showLoginForm("false", falseModel, null);
    controller.showLoginForm(null, absentModel, null);

    assertThat(falseModel.get("error"), is(false));
    assertThat(absentModel.get("error"), is(false));
  }

  /**
   * Regresión del bug reportado: con sesión activa, /auth/login renderizaba el formulario encima de
   * la sesión. El par de tests de abajo cubre las dos ramas del redirect; la integración cubre que
   * el destino sea el mismo que el de un login exitoso.
   */
  @Test
  public void shouldRedirectAwayFromTheLoginViewWhenAlreadySignedIn() {
    String view = controller.showLoginForm(
      null,
      new ExtendedModelMap(),
      new TestingAuthenticationToken(
        "admin@unlam.edu.ar",
        "n/a",
        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
      )
    );

    assertThat(view, is("redirect:/admin/home"));
  }

  @Test
  public void shouldRedirectACommonUserToTheLandingFromTheLoginView() {
    String view = controller.showLoginForm(
      null,
      new ExtendedModelMap(),
      new TestingAuthenticationToken(
        "user@unlam.edu.ar",
        "n/a",
        List.of(new SimpleGrantedAuthority("ROLE_USER"))
      )
    );

    assertThat(view, is("redirect:/"));
  }

  // --- U-03, U-04 ---

  @Test
  public void shouldReturnRegisterViewWithAnEmptyForm() {
    ExtendedModelMap model = new ExtendedModelMap();

    String view = controller.showRegisterForm(model);

    assertThat(view, is(REGISTER_VIEW));
    assertThat(
      model.get("registerRequest"),
      org.hamcrest.Matchers.instanceOf(RegisterRequest.class)
    );
    assertThat(((RegisterRequest) model.get("registerRequest")).getEmail(), is(nullValue()));
  }

  @Test
  public void shouldRedirectToLoginAfterSuccessfulRegistration() {
    RegisterRequest request = new RegisterRequest(EMAIL, PASSWORD);

    String view = controller.handleRegister(request, noErrors(request), new ExtendedModelMap());

    assertThat(view, is("redirect:/auth/login"));
    verify(loginService).register(EMAIL, PASSWORD);
  }

  // --- U-05, U-06 ---

  @Test
  public void shouldRejectAnInvalidEmail() {
    RegisterRequest request = new RegisterRequest("not-an-email", PASSWORD);
    BeanPropertyBindingResult bindingResult = errorsOf(request);

    String view = controller.handleRegister(request, bindingResult, new ExtendedModelMap());

    assertThat(view, is(REGISTER_VIEW));
    assertThat(fieldNames(bindingResult), hasItem("email"));
    verify(loginService, never()).register(EMAIL, PASSWORD);
  }

  @Test
  public void shouldRejectAShortPassword() {
    RegisterRequest request = new RegisterRequest(EMAIL, "123");
    BeanPropertyBindingResult bindingResult = errorsOf(request);

    String view = controller.handleRegister(request, bindingResult, new ExtendedModelMap());

    assertThat(view, is(REGISTER_VIEW));
    assertThat(fieldNames(bindingResult), hasItem("password"));
    verify(loginService, never()).register(EMAIL, PASSWORD);
  }

  // --- U-07 ---

  @Test
  public void shouldReRenderRegisterViewWhenEmailIsAlreadyRegistered() {
    RegisterRequest request = new RegisterRequest(EMAIL, PASSWORD);
    doThrow(new UserAlreadyExists()).when(loginService).register(EMAIL, PASSWORD);
    ExtendedModelMap model = new ExtendedModelMap();

    String view = controller.handleRegister(request, noErrors(request), model);

    assertThat(view, is(REGISTER_VIEW));
    assertThat((String) model.get("errorMessage"), is("Ese email ya está registrado"));
  }

  // --- U-08, U-09, U-10 ---

  @Test
  public void shouldReturnForgotPasswordViewWithAnEmptyForm() {
    ExtendedModelMap model = new ExtendedModelMap();

    String view = controller.showForgotPasswordForm(model);

    assertThat(view, is(FORGOT_VIEW));
    assertThat(
      model.get("recoverPasswordRequest"),
      org.hamcrest.Matchers.instanceOf(RecoverPasswordRequest.class)
    );
  }

  @Test
  public void shouldReturnTheTemporaryPasswordAfterRecovery() {
    RecoverPasswordRequest request = new RecoverPasswordRequest(EMAIL);
    when(recoverPasswordService.recoverPassword(EMAIL)).thenReturn("tmp-9876");
    ExtendedModelMap model = new ExtendedModelMap();

    String view = controller.handleRecoverPassword(request, noErrors(request), model);

    assertThat(view, is(RECOVERED_VIEW));
    assertThat((String) model.get("tempPassword"), is("tmp-9876"));
  }

  @Test
  public void shouldRejectAnInvalidEmailOnRecovery() {
    RecoverPasswordRequest request = new RecoverPasswordRequest("not-an-email");
    BeanPropertyBindingResult bindingResult = errorsOf(request);

    String view = controller.handleRecoverPassword(request, bindingResult, new ExtendedModelMap());

    assertThat(view, is(FORGOT_VIEW));
    assertThat(fieldNames(bindingResult), hasItem("email"));
  }

  @Test
  public void shouldTellTheUserWhenTheEmailIsUnknown() {
    RecoverPasswordRequest request = new RecoverPasswordRequest(EMAIL);
    when(recoverPasswordService.recoverPassword(EMAIL))
      .thenThrow(new IllegalArgumentException("Email no encontrado"));
    ExtendedModelMap model = new ExtendedModelMap();

    String view = controller.handleRecoverPassword(request, noErrors(request), model);

    assertThat(view, is(FORGOT_VIEW));
    assertThat((String) model.get("errorMessage"), is("Email no encontrado"));
    assertThat(model.get("tempPassword"), is(nullValue()));
  }

  // --- helpers ---

  private static BeanPropertyBindingResult noErrors(Object target) {
    return new BeanPropertyBindingResult(target, "target");
  }

  /** Runs the real bean validation so U-05/U-06 assert the DTO constraints, not a hand-made error. */
  private static BeanPropertyBindingResult errorsOf(Object target) {
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "target");
    VALIDATOR.validate(target, bindingResult);
    return bindingResult;
  }

  private static Set<String> fieldNames(BeanPropertyBindingResult bindingResult) {
    return bindingResult
      .getFieldErrors()
      .stream()
      .map(FieldError::getField)
      .collect(java.util.stream.Collectors.toSet());
  }

  /** Guard: the happy paths must not accidentally satisfy the "no errors" case. */
  @Test
  public void shouldValidateAWellFormedRegistration() {
    assertThat(errorsOf(new RegisterRequest(EMAIL, PASSWORD)).hasErrors(), is(false));
    assertThat(errorsOf(new RecoverPasswordRequest(EMAIL)).hasErrors(), is(false));
    assertThat(fieldNames(errorsOf(new RegisterRequest(EMAIL, "123"))), not(hasItem("email")));
  }
}
