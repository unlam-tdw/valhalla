package com.valhalla.infrastructure.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  @Override
  public void onAuthenticationSuccess(
    HttpServletRequest request,
    HttpServletResponse response,
    Authentication authentication
  ) throws IOException, ServletException {
    request.getSession().setAttribute("loginTime", System.currentTimeMillis());

    boolean isAdmin = authentication
      .getAuthorities()
      .stream()
      .map(GrantedAuthority::getAuthority)
      .anyMatch(role -> "ROLE_ADMIN".equals(role) || "ADMIN".equals(role));

    if (isAdmin) {
      response.sendRedirect(request.getContextPath() + "/admin/home");
    } else {
      // LandingController serves "/" and the landing page is public. It replaces "/plans", which
      // no controller maps: the redirect used to land on a page that only looked broken because a
      // missing route rendered the error view with HTTP 200.
      response.sendRedirect(request.getContextPath() + "/");
    }
  }
}
