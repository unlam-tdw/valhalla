package com.valhalla.domain.user;

import com.valhalla.domain.exception.UserAlreadyExists;
import com.valhalla.domain.exception.UserNotFoundException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Autowired
  public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional(readOnly = true)
  public List<User> findAll() {
    return userRepository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public User findById(Long id) {
    return userRepository.findById(id).orElseThrow(UserNotFoundException::new);
  }

  @Override
  public void create(
    String email,
    String password,
    String role,
    String firstName,
    String lastName
  ) {
    if (userRepository.existsByEmail(email)) {
      throw new UserAlreadyExists();
    }
    User user = new User();
    user.setEmail(email);
    user.setPassword(passwordEncoder.encode(password));
    user.setRole(role);
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setActive(true);
    userRepository.save(user);
  }

  @Override
  public void update(Long id, String email, String role, String firstName, String lastName) {
    User user = findById(id);
    user.setEmail(email);
    user.setRole(role);
    user.setFirstName(firstName);
    user.setLastName(lastName);
    userRepository.update(user);
  }

  @Override
  public void deactivate(Long id) {
    User user = findById(id);
    user.setActive(false);
    userRepository.update(user);
  }

  @Override
  public void activate(Long id) {
    User user = findById(id);
    user.setActive(true);
    userRepository.update(user);
  }

  @Override
  public void delete(Long id) {
    if (!userRepository.existsById(id)) {
      throw new UserNotFoundException();
    }
    userRepository.deleteById(id);
  }

  @Override
  public String rotatePassword(Long id) {
    User user = findById(id);
    String newPassword = generatePassword();
    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.update(user);
    return newPassword;
  }

  @Override
  public String generatePassword() {
    return java.util.UUID.randomUUID().toString().substring(0, 8);
  }
}
