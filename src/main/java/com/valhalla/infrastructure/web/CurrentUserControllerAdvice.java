package com.valhalla.infrastructure.web;

import com.valhalla.infrastructure.security.AppUserDetails;
import java.security.Principal;
import java.util.Locale;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes the signed-in identity to every view: the shared navbar renders an avatar with the
 * first initial of the real name ({@code userInitial}) and needs the full name for its
 * accessible label ({@code userDisplayName}).
 *
 * <p>Defensive by design: the principal is whatever the security layer placed there — an
 * {@link AppUserDetails} after a real form login, a plain Spring {@code User} (or any object
 * with a name) for synthetic MockMvc principals such as {@code SecurityMockMvcRequestPostProcessors.user(...)},
 * and null or an anonymous token when there is no session. Every one of those must render the
 * navbar without throwing or producing an empty avatar.
 */
@ControllerAdvice
public class CurrentUserControllerAdvice {

  @ModelAttribute("userInitial")
  public String userInitial(Principal principal) {
    String name = displayNameOf(principal);
    if (name == null) {
      return null;
    }
    return name.substring(0, 1).toUpperCase(Locale.ROOT);
  }

  @ModelAttribute("userDisplayName")
  public String userDisplayName(Principal principal) {
    return displayNameOf(principal);
  }

  /**
   * Human-readable identity, or null when there is no session to speak for. Prefers the real
   * name carried by {@link AppUserDetails}; anything else falls back to the login name.
   */
  private static String displayNameOf(Principal principal) {
    if (principal == null || principal instanceof AnonymousAuthenticationToken) {
      return null;
    }
    if (principal instanceof AppUserDetails appUser) {
      String first = trimToNull(appUser.getFirstName());
      if (first != null) {
        String last = trimToNull(appUser.getLastName());
        return last == null ? first : first + " " + last;
      }
    }
    return trimToNull(principal.getName());
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
