package com.valhalla;

import com.valhalla.config.JpaConfig;
import com.valhalla.config.SecurityConfig;
import com.valhalla.config.SpringWebConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class MyServletInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

  // services and data sources
  @Override
  protected Class<?>[] getRootConfigClasses() {
    return new Class<?>[0];
  }

  // controller, view resolver, handler mapping
  @Override
  protected Class<?>[] getServletConfigClasses() {
    return new Class<?>[] { SpringWebConfig.class, JpaConfig.class, SecurityConfig.class };
  }

  @Override
  public void onStartup(ServletContext servletContext) throws ServletException {
    super.onStartup(servletContext);
    // Track session create/destroy so Spring Security's SessionRegistry can
    // enforce maximumSessions(1) (AC-13). Without this listener, expired or
    // logged-out sessions are never removed from the registry.
    servletContext.addListener(new HttpSessionEventPublisher());
  }

  @Override
  protected String[] getServletMappings() {
    return new String[] { "/" };
  }
}
