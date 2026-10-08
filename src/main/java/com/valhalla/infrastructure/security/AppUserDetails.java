package com.valhalla.infrastructure.security;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Principal built by {@link CustomUserDetailsService} for real logins. Extends the contract of
 * Spring's plain {@code User} with the account's actual first/last name, so templates can greet
 * the owner by name (the navbar avatar shows the initial of the real firstName) instead of by
 * email.
 *
 * <p>{@code equals}/{@code hashCode} mirror Spring's {@code User}: the {@code SessionRegistry}
 * keys sessions by principal object, so two loads of the same account must compare equal
 * (LoginControllerTest#shouldKeepOnlyTheMostRecentSessionPerUser re-loads the principal and
 * expects the registry to find the session).
 */
public class AppUserDetails implements UserDetails {

  /* serialVersionUID for class serialization, required by PMD */
  private static final long serialVersionUID = 1L;

  private final String username;
  private final String password;
  private final boolean enabled;
  private final String firstName;
  private final String lastName;
  private final List<GrantedAuthority> authorities;

  public AppUserDetails(
    String username,
    String password,
    boolean enabled,
    String firstName,
    String lastName,
    Collection<? extends GrantedAuthority> authorities
  ) {
    this.username = username;
    this.password = password;
    this.enabled = enabled;
    this.firstName = firstName;
    this.lastName = lastName;
    this.authorities = List.copyOf(authorities);
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  @Override
  public boolean equals(Object rhs) {
    if (this == rhs) {
      return true;
    }
    if (!(rhs instanceof AppUserDetails other)) {
      return false;
    }
    return (
      enabled == other.enabled &&
      Objects.equals(username, other.username) &&
      Objects.equals(password, other.password) &&
      Objects.equals(authorities, other.authorities)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(username, password, enabled, authorities);
  }
}
