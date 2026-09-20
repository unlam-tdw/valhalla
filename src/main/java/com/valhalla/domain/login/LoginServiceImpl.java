package com.valhalla.domain.login;

import com.valhalla.domain.exception.UserAlreadyExists;
import com.valhalla.domain.user.User;
import com.valhalla.domain.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LoginServiceImpl implements LoginService {

  private static final String DEFAULT_ROLE = "USER";

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Autowired
  public LoginServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public User findUser(String email, String password) {
    User user = userRepository.findByEmail(email).orElse(null);
    if (
      user == null ||
      !Boolean.TRUE.equals(user.getActive()) ||
      !passwordEncoder.matches(password, user.getPassword())
    ) {
      return null;
    }
    return user;
  }

  @Override
  public void register(String email, String password, String firstName, String lastName) {
    if (userRepository.findByEmail(email).isPresent()) {
      throw new UserAlreadyExists();
    }
    User user = new User();
    user.setEmail(email);
    user.setPassword(passwordEncoder.encode(password));
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setRole(DEFAULT_ROLE);
    user.setActive(true);
    userRepository.save(user);
  }
}
