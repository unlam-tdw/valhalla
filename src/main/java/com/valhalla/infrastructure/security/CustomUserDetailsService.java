package com.valhalla.infrastructure.security;

import com.valhalla.domain.user.User;
import com.valhalla.domain.user.UserRepository;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  @Autowired
  public CustomUserDetailsService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    User user = userRepository
      .findByEmail(email)
      .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

    return new org.springframework.security.core.userdetails.User(
      user.getEmail(),
      user.getPassword(),
      Boolean.TRUE.equals(user.getActive()),
      true,
      true,
      true,
      Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
    );
  }
}
