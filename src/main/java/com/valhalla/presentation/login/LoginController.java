package com.valhalla.presentation.login;

import com.valhalla.domain.exception.UserAlreadyExists;
import com.valhalla.domain.login.LoginService;
import com.valhalla.presentation.shared.NewUserRequest;
import com.valhalla.presentation.shared.SessionInterceptor;
import com.valhalla.presentation.shared.UserSession;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class LoginController {

  private static final String VIEW_LOGIN = "pages/auth/login";
  private static final String VIEW_NEW_USER = "pages/auth/new-user";
  private static final String VIEW_HOME = "pages/home";
  private static final String REDIRECT_LOGIN = "redirect:/admin/login";
  private static final String ATTR_NEW_USER_DATA = "newUserData";
  private static final String ATTR_USER = "user";
  private static final String ATTR_LOGIN_TIME = "loginTime";

  private final LoginService loginService;

  @Autowired
  public LoginController(LoginService loginService) {
    this.loginService = loginService;
  }

  @RequestMapping("/admin")
  public ModelAndView adminIndex() {
    return new ModelAndView(REDIRECT_LOGIN);
  }

  @GetMapping("/admin/login")
  public ModelAndView showLogin() {
    return new ModelAndView(VIEW_LOGIN);
  }

  @GetMapping("/admin/new-user")
  public ModelAndView showNewUser() {
    Map<String, Object> model = new ModelMap();
    model.put(ATTR_NEW_USER_DATA, new NewUserRequest());
    return new ModelAndView(VIEW_NEW_USER, model);
  }

  @PostMapping("/admin/register")
  public ModelAndView register(
    @Valid @ModelAttribute(ATTR_NEW_USER_DATA) NewUserRequest newUserData,
    BindingResult bindingResult
  ) {
    if (bindingResult.hasErrors()) {
      return renderNewUserWithError(newUserData);
    }
    try {
      loginService.register(
        newUserData.getEmail(),
        newUserData.getPassword(),
        newUserData.getFirstName(),
        newUserData.getLastName()
      );
      return new ModelAndView(REDIRECT_LOGIN);
    } catch (UserAlreadyExists e) {
      return renderNewUserWithError(newUserData, "Email is already registered");
    }
  }

  @GetMapping("/admin/home")
  public ModelAndView showHome(HttpSession httpSession) {
    UserSession userSession = (UserSession) httpSession.getAttribute(
      SessionInterceptor.USER_SESSION
    );
    if (userSession == null) {
      return new ModelAndView(REDIRECT_LOGIN);
    }
    Map<String, Object> model = new ModelMap();
    model.put(ATTR_USER, userSession);
    model.put(ATTR_LOGIN_TIME, httpSession.getAttribute(ATTR_LOGIN_TIME));
    return new ModelAndView(VIEW_HOME, model);
  }

  private ModelAndView renderNewUserWithError(NewUserRequest newUserData) {
    return renderNewUserWithError(newUserData, "Invalid registration data");
  }

  private ModelAndView renderNewUserWithError(NewUserRequest newUserData, String message) {
    Map<String, Object> model = new ModelMap();
    model.put(ATTR_NEW_USER_DATA, newUserData);
    model.put("error", message);
    return new ModelAndView(VIEW_NEW_USER, model);
  }
}
