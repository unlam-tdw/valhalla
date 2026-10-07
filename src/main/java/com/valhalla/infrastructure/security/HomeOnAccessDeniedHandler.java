package com.valhalla.infrastructure.security;

import com.valhalla.presentation.shared.LoginRedirects;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

/**
 * Cuando un principal con sesión cruza la línea entre las dos superficies (/admin/** para un ADMIN,
 * /auth/** para un USER), lo manda a su propio home en vez de mostrarle un 403.
 *
 * <p>Un 403 con la página de error genérica le decía al admin "no tenés permiso" sin decirle por
 * dónde seguir, y en el caso inverso le mostraba el mismo error por haber tipeado mal una URL.
 *
 * <p>La excepción es el filtro de CSRF: {@code CsrfFilter} corre antes que los filtros de
 * autorización, así que un POST sin token nunca pasa por {@code ExceptionTranslationFilter}: va
 * directo a este handler. Acá tiene que seguir siendo 403, o un login sin token se contestaría con un
 * 302 y el AC-06 de 08-AUT quedaría reescrito por un redirect.
 */
@Component
public class HomeOnAccessDeniedHandler implements AccessDeniedHandler {

  private static final String PUBLIC_LANDING = "/";

  // El handler por defecto de Spring, que responde sendError(403) y deja que la página de error lo
  // renderice. Se reutiliza en vez de reimplementar sendError.
  private static final AccessDeniedHandler FORBIDDEN = new AccessDeniedHandlerImpl();

  @Override
  public void handle(
    HttpServletRequest request,
    HttpServletResponse response,
    AccessDeniedException exception
  ) throws IOException, ServletException {
    if (exception instanceof CsrfException) {
      FORBIDDEN.handle(request, response, exception);
      return;
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String target = LoginRedirects.isSignedIn(authentication)
      ? LoginRedirects.landingFor(authentication)
      : PUBLIC_LANDING;
    response.sendRedirect(request.getContextPath() + target);
  }
}
