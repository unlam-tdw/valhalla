package com.valhalla.config;

import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;

/**
 * Registers the {@code springSecurityFilterChain} with the servlet container.
 * Without this, Spring Security's filters (CSRF, authentication, etc.) never execute.
 */
public class SecurityInitializer extends AbstractSecurityWebApplicationInitializer {

  public SecurityInitializer() {
    super(SecurityConfig.class);
  }
}
