package com.valhalla.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/**
 * Abre la sesión sin pasar por el form de login, para que quien crea la cuenta quede adentro en
 * la misma request en que se registró (AC-01 de 08-AUT).
 *
 * <p>El contexto no se persiste solo: con el guardado explícito que exige Spring Security 6,
 * {@code SecurityContextHolderFilter} sólo carga el contexto y limpia el holder al final de la
 * request; persistir es trabajo de cada mecanismo de autenticación (el form de login lo hace su
 * filtro). Sin la llamada a {@code saveContext}, el recién registrado volvería anónimo en el
 * request siguiente.
 *
 * <p>El {@code changeSessionId} replica la protección contra session fixation que el form de login
 * aplica vía {@code ChangeSessionIdAuthenticationStrategy}: sin él, la sesión anónima que trajo el
 * form de registro seguiría siendo la sesión autenticada.
 */
@Component
public class ProgrammaticSignIn {

  /** El mismo atributo que deja el CustomAuthenticationSuccessHandler en cada login. */
  private static final String LOGIN_TIME_ATTRIBUTE = "loginTime";

  private final UserDetailsService userDetailsService;
  private final SecurityContextRepository securityContextRepository;

  @Autowired
  public ProgrammaticSignIn(UserDetailsService userDetailsService) {
    this.userDetailsService = userDetailsService;
    // Sólo HttpSession: la carga de las cadenas la hace un DelegatingSecurityContextRepository
    // que termina en este mismo repositorio, así que guardar acá alcanza para que el request
    // siguiente encuentre el contexto.
    this.securityContextRepository = new HttpSessionSecurityContextRepository();
  }

  /**
   * Autentica a {@code email} en la request actual y devuelve la autenticación creada, para que
   * el caller la pase a {@code LoginRedirects} y el destino sea el mismo que el de un login
   * normal.
   */
  public Authentication signIn(
    String email,
    HttpServletRequest request,
    HttpServletResponse response
  ) {
    UserDetails user = userDetailsService.loadUserByUsername(email);
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
      UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities())
    );

    if (request.getSession(false) != null) {
      request.changeSessionId();
    }
    securityContextRepository.saveContext(context, request, response);
    request.getSession().setAttribute(LOGIN_TIME_ATTRIBUTE, System.currentTimeMillis());
    return context.getAuthentication();
  }
}
