package com.valhalla.domain.login;

import com.valhalla.domain.user.User;

public interface LoginService {
  User findUser(String email, String password);
  void register(String email, String password, String firstName, String lastName);
}
