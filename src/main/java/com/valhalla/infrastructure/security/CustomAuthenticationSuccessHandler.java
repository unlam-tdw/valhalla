package com.valhalla.infrastructure.security;

import com.valhalla.domain.user.User;
import com.valhalla.domain.user.UserRepository;
import com.valhalla.presentation.shared.SessionInterceptor;
import com.valhalla.presentation.shared.UserSession;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  private final UserRepository userRepository;

  @Autowired
  public CustomAuthenticationSuccessHandler(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public void onAuthenticationSuccess(
    HttpServletRequest request,
    HttpServletResponse response,
    Authentication authentication
  ) throws IOException, ServletException {
    String email = authentication.getName();
    User user = userRepository.findByEmail(email).orElse(null);
    if (user != null) {
      UserSession userSession = new UserSession(
        user.getEmail(),
        user.getRole(),
        user.getFirstName(),
        user.getLastName()
      );
      request.getSession().setAttribute(SessionInterceptor.USER_SESSION, userSession);
      request.getSession().setAttribute("loginTime", System.currentTimeMillis());
    }
    response.sendRedirect(request.getContextPath() + "/admin/home");
  }
}
