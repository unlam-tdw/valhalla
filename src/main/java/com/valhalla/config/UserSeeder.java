package com.valhalla.config;

import com.valhalla.domain.exception.UserAlreadyExists;
import com.valhalla.domain.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class UserSeeder implements ApplicationListener<ContextRefreshedEvent> {

  private final UserService userService;

  @Autowired
  public UserSeeder(UserService userService) {
    this.userService = userService;
  }

  private boolean seeded = false;

  @Override
  public synchronized void onApplicationEvent(ContextRefreshedEvent event) {
    if (seeded) return;
    seeded = true;
    try {
      userService.create("test@unlam.edu.ar", "password", "ADMIN");
      System.out.println("Test admin user created: test@unlam.edu.ar");
    } catch (UserAlreadyExists e) {
      System.out.println("Test admin user already exists, skipping seed");
    }
  }
}
