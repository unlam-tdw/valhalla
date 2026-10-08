package com.valhalla.presentation.shared;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * A dónde va alguien que ya tiene sesión.
 *
 * <p>Las dos páginas de login la usan para no mostrarle un formulario a quien ya está adentro, y el
 * handler de login exitoso la usa para el mismo destino. La regla estaba escrita una vez por página
 * en vez de una vez por lugar, así que un cambio de home tenía que acordarse de los tres.
 *
 * <p>Sin proxy de scope y sin estado: son dos preguntas sobre el principal de la request.
 */
public final class LoginRedirects {

  private static final String ADMIN_LANDING = "/admin/home";
  private static final String USER_LANDING = "/explore";

  private LoginRedirects() {}

  /**
   * Si la request ya trae una sesión. El filtro de Spring Security devuelve null desde
   * {@code getUserPrincipal()} cuando el principal es anónimo, así que null es exactamente "no hay
   * nadie adentro" y alcanza con esto. Si alguna vez esa respuesta fuera un
   * {@code AnonymousAuthenticationToken} en vez de null, esta es la línea que hay que endurecer.
   *
   * @param authentication el principal de la request, o null si no hay sesión
   * @return true solo con una sesión real
   */
  public static boolean isSignedIn(Authentication authentication) {
    return authentication != null;
  }

  /**
   * El mismo destino que devuelve un login exitoso, para que la página de login no difiera de lo
   * que pasa después de loguearse.
   *
   * @param authentication un principal ya autenticado
   * @return {@code /admin/home} para un ADMIN, la vista de exploración (/explore) para el resto
   */
  public static String landingFor(Authentication authentication) {
    boolean isAdmin = authentication
      .getAuthorities()
      .stream()
      .map(GrantedAuthority::getAuthority)
      .anyMatch(role -> "ROLE_ADMIN".equals(role) || "ADMIN".equals(role));
    return isAdmin ? ADMIN_LANDING : USER_LANDING;
  }
}
