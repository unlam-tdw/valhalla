package com.valhalla;

import com.valhalla.config.JpaConfig;
import com.valhalla.config.SecurityConfig;
import com.valhalla.config.SpringWebConfig;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import java.util.EnumSet;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class MyServletInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

  private static final String HIDDEN_HTTP_METHOD_FILTER = "hiddenHttpMethodFilter";

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
    registerHiddenHttpMethodFilter(servletContext);
    // Track session create/destroy so Spring Security's SessionRegistry can
    // enforce maximumSessions(1) (AC-13). Without this listener, expired or
    // logged-out sessions are never removed from the registry.
    servletContext.addListener(new HttpSessionEventPublisher());
  }

  /**
   * Installs the HiddenHttpMethodFilter in the servlet container. A {@code @Bean} Filter is
   * invisible here: Boot's auto-registration of Filter beans belongs to Spring Boot, and this WAR
   * is bootstrapped by {@link AbstractAnnotationConfigDispatcherServletInitializer}. Without this,
   * {@code _method} was ignored at runtime while MockMvc, which picks Filter beans up from the
   * context, pretended it worked.
   */
  private void registerHiddenHttpMethodFilter(ServletContext servletContext) {
    if (servletContext.getFilterRegistration(HIDDEN_HTTP_METHOD_FILTER) != null) {
      return;
    }
    servletContext
      .addFilter(HIDDEN_HTTP_METHOD_FILTER, new HiddenHttpMethodFilter())
      .addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false);
  }

  @Override
  protected String[] getServletMappings() {
    return new String[] { "/" };
  }
}
