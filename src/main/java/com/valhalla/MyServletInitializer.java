package com.valhalla;

import com.valhalla.config.DevClassReloader;
import com.valhalla.config.JpaConfig;
import com.valhalla.config.SpringWebConfig;
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

    // Dev-only: watches target/classes for .class changes and restarts the
    // container (System.exit(1)) so Docker picks up new classes on restart.
    Path classesDir = Path.of("target/classes");
    if (Files.exists(classesDir)) {
      DevClassReloader reloader = new DevClassReloader(classesDir);
      reloader.start();
    }
  }
}
