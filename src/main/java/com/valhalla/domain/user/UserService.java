package com.valhalla.domain.user;

import java.util.List;

public interface UserService {
  List<User> findAll();
  User findById(Long id);
  void create(String email, String password, String role, String firstName, String lastName);
  void update(Long id, String email, String role, String firstName, String lastName);
  void activate(Long id);
  void deactivate(Long id);
  void delete(Long id);
  String rotatePassword(Long id);
  String generatePassword();
}
