package com.valhalla.presentation.login;

import com.valhalla.domain.exception.UserAlreadyExists;
import com.valhalla.domain.login.LoginService;
import com.valhalla.domain.user.User;
import com.valhalla.presentation.shared.NewUserRequest;
import com.valhalla.presentation.shared.SessionInterceptor;
import com.valhalla.presentation.shared.UserSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class LoginController {

  private static final String VIEW_LOGIN = "pages/auth/login";
  private static final String VIEW_NEW_USER = "pages/auth/new-user";
  private static final String VIEW_HOME = "pages/home";
  private static final String REDIRECT_LOGIN = "redirect:/admin/login";
  private static final String ATTR_LOGIN_DATA = "loginData";
  private static final String ATTR_NEW_USER_DATA = "newUserData";
  private static final String ATTR_USER = "user";
  private static final String ATTR_LOGIN_TIME = "loginTime";

  private final LoginService loginService;

  @Autowired
  public LoginController(LoginService loginService) {
    this.loginService = loginService;
  }

  @RequestMapping("/admin/login")
  public ModelAndView showLogin() {
    Map<String, Object> model = new ModelMap();
    model.put(ATTR_LOGIN_DATA, new LoginRequest());
    return new ModelAndView(VIEW_LOGIN, model);
  }

  @RequestMapping(path = "/admin/validate-login", method = RequestMethod.POST)
  public ModelAndView validateLogin(
    @Valid @ModelAttribute(ATTR_LOGIN_DATA) LoginRequest loginData,
    BindingResult bindingResult,
    HttpServletRequest request
  ) {
    if (bindingResult.hasErrors()) {
      return renderLoginWithError(loginData);
    }
    User foundUser = loginService.findUser(loginData.getEmail(), loginData.getPassword());
    if (foundUser != null) {
      UserSession userSession = new UserSession(
        foundUser.getEmail(),
        foundUser.getRole(),
        foundUser.getFirstName(),
        foundUser.getLastName()
      );
      request.getSession().setAttribute(SessionInterceptor.USER_SESSION, userSession);
      request.getSession().setAttribute(ATTR_LOGIN_TIME, System.currentTimeMillis());
      return new ModelAndView("redirect:/admin/home");
    }
    return renderLoginWithError(loginData);
  }

  @RequestMapping(path = "/admin/register", method = RequestMethod.POST)
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

  @RequestMapping(path = "/admin/new-user", method = RequestMethod.GET)
  public ModelAndView showNewUser() {
    Map<String, Object> model = new ModelMap();
    model.put(ATTR_NEW_USER_DATA, new NewUserRequest());
    return new ModelAndView(VIEW_NEW_USER, model);
  }

  @RequestMapping(path = "/admin/home", method = RequestMethod.GET)
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

  @RequestMapping(path = "/admin/logout", method = RequestMethod.POST)
  public ModelAndView logout(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
    return new ModelAndView(REDIRECT_LOGIN);
  }

  private ModelAndView renderLoginWithError(LoginRequest loginData) {
    Map<String, Object> model = new ModelMap();
    model.put(ATTR_LOGIN_DATA, loginData);
    model.put("error", "Invalid email or password");
    return new ModelAndView(VIEW_LOGIN, model);
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
