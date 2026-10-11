package com.valhalla.infrastructure.security;

import com.valhalla.presentation.shared.LoginRedirects;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
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

    // Un USER ya firmó su sesión y no vuelve a una landing estática: aterriza en el feed de
    // planes públicos (/plans/public), lo primero que quiere ver. La regla es compartida con las
    // dos páginas de login (LoginRedirects), así que las tres coinciden en el mismo destino.
    response.sendRedirect(request.getContextPath() + LoginRedirects.landingFor(authentication));
  }
}
