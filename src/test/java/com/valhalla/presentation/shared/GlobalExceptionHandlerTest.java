package com.valhalla.presentation.shared;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.mockito.Mockito.mock;

import com.valhalla.presentation.user.EditUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

public class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;

  @BeforeEach
  public void init() {
    handler = new GlobalExceptionHandler();
  }

  @Test
  public void shouldReRenderUserFormWithErrorWhenEmailAlreadyExists() {
    ModelAndView modelAndView = handler.handleUserAlreadyExists();
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("pages/admin/user-form"));
    assertThat(
      modelAndView.getModel().get("error").toString(),
      equalToIgnoringCase("Email is already registered")
    );
    assertThat(modelAndView.getModel().get("userForm"), instanceOf(EditUserRequest.class));
    assertThat(modelAndView.getModel().get("isEdit"), instanceOf(Boolean.class));
  }

  @Test
  public void shouldShowErrorViewWithGenericMessage() {
    ModelAndView modelAndView = handler.handleUnexpectedError(new RuntimeException("boom"));
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("pages/error"));
    assertThat(
      modelAndView.getModel().get("error").toString(),
      equalToIgnoringCase("An unexpected error occurred")
    );
  }

  @Test
  public void shouldRedirectToUsersWithErrorWhenUserNotFound() {
    ModelAndView modelAndView = handler.handleUserNotFound();
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("redirect:/admin/users"));
    assertThat(
      modelAndView.getModel().get("error").toString(),
      equalToIgnoringCase("User not found")
    );
  }
}
