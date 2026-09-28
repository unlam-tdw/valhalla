package com.valhalla.domain.login;

import com.valhalla.domain.user.User;

public interface LoginService {
  User findUser(String email, String password);

  /** Self-service registration (AC-01): email + password only, no personal data collected. */
  void register(String email, String password);

  void register(String email, String password, String firstName, String lastName);
}
