package com.valhalla.config;

import com.valhalla.domain.exception.UserAlreadyExists;
import com.valhalla.domain.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class UserSeeder implements CommandLineRunner {

  private final UserService userService;

  @Autowired
  public UserSeeder(UserService userService) {
    this.userService = userService;
  }

  @Override
  public void run(String... args) {
    try {
      userService.create("test@unlam.edu.ar", "password", "ADMIN");
      System.out.println("Test admin user created: test@unlam.edu.ar");
    } catch (UserAlreadyExists e) {
      System.out.println("Test admin user already exists, skipping seed");
    }
  }
}
