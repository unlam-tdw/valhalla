package com.valhalla.presentation.shared;

import com.valhalla.domain.exception.UserAlreadyExists;
import com.valhalla.domain.exception.UserNotFoundException;
import com.valhalla.presentation.user.EditUserRequest;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger LOGGER = Logger.getLogger(GlobalExceptionHandler.class.getName());

  @ExceptionHandler(UserAlreadyExists.class)
  public ModelAndView handleUserAlreadyExists() {
    Map<String, Object> model = new ModelMap();
    model.put("userForm", new EditUserRequest());
    model.put("isEdit", false);
    model.put("error", "Email is already registered");
    return new ModelAndView("pages/admin/user-form", model);
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ModelAndView handleUserNotFound() {
    Map<String, Object> model = new ModelMap();
    model.put("error", "User not found");
    return new ModelAndView("redirect:/admin/users", model);
  }

  @ExceptionHandler(Exception.class)
  public ModelAndView handleUnexpectedError(Exception ex) {
    LOGGER.log(Level.SEVERE, "Unhandled error", ex);
    Map<String, Object> model = new ModelMap();
    model.put("error", "An unexpected error occurred");
    return new ModelAndView("pages/error", model);
  }
}
