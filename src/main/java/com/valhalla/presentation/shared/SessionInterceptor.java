package com.valhalla.presentation.shared;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.web.servlet.HandlerInterceptor;

public class SessionInterceptor implements HandlerInterceptor {

  public static final String USER_SESSION = "userSession";

  @Override
  public boolean preHandle(
    HttpServletRequest request,
    HttpServletResponse response,
    Object handler
  ) throws IOException {
    HttpSession session = request.getSession(false);
    if (session == null || session.getAttribute(USER_SESSION) == null) {
      response.sendRedirect(request.getContextPath() + "/admin/login");
      return false;
    }
    UserSession userSession = (UserSession) session.getAttribute(USER_SESSION);
    if (!"ADMIN".equals(userSession.getRole())) {
      response.sendRedirect(request.getContextPath() + "/admin/login");
      return false;
    }
    return true;
  }
}
