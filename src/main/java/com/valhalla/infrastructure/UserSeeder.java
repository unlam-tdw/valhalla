package com.valhalla.infrastructure;

import com.valhalla.domain.exception.UserAlreadyExists;
import com.valhalla.domain.user.UserService;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class UserSeeder implements ApplicationListener<ContextRefreshedEvent> {

  private static final Logger LOGGER = Logger.getLogger(UserSeeder.class.getName());

  @Autowired(required = false)
  private UserService userService;

  private boolean seeded = false;

  @Override
  public synchronized void onApplicationEvent(ContextRefreshedEvent event) {
    if (seeded || userService == null) return;
    seeded = true;
    try {
      userService.create("test@unlam.edu.ar", "password", "ADMIN", "Admin", "Test");
      LOGGER.info("Test admin user created: test@unlam.edu.ar");
    } catch (UserAlreadyExists e) {
      LOGGER.info("Test admin user already exists, skipping seed");
    }
  }
}
