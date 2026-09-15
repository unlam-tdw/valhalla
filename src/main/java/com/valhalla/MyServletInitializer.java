package com.valhalla;

import com.valhalla.config.DevClassReloader;
import com.valhalla.config.JpaConfig;
import com.valhalla.config.SpringWebConfig;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import java.nio.file.Files;
import java.nio.file.Path;
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
    return new Class<?>[] { SpringWebConfig.class, JpaConfig.class };
  }

  @Override
  protected String[] getServletMappings() {
    return new String[] { "/" };
  }

  @Override
  public void onStartup(jakarta.servlet.ServletContext servletContext)
    throws jakarta.servlet.ServletException {
    super.onStartup(servletContext);

    // Dev-only class hot-reload: watches target/classes for .class changes
    // and triggers a context reload after a debounce period. Only active
    // when running via mvn jetty:run (target/classes exists).
    Path classesDir = Path.of("target/classes");
    if (Files.exists(classesDir)) {
      DevClassReloader reloader = new DevClassReloader(classesDir);
      servletContext.addListener(
        new ServletContextListener() {
          @Override
          public void contextInitialized(ServletContextEvent sce) {
            reloader.start(sce.getServletContext());
          }

          @Override
          public void contextDestroyed(ServletContextEvent sce) {
            reloader.stop();
          }
        }
      );
    }
  }
}
