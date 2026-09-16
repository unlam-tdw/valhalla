package com.valhalla.domain.login;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.valhalla.domain.exception.UserAlreadyExists;
import com.valhalla.domain.user.User;
import com.valhalla.domain.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class LoginServiceTest {

  private LoginService loginService;
  private UserRepository userRepositoryMock;
  private PasswordEncoder passwordEncoder;

  @BeforeEach
  public void init() {
    this.userRepositoryMock = mock(UserRepository.class);
    this.passwordEncoder = new BCryptPasswordEncoder();
    this.loginService = new LoginServiceImpl(this.userRepositoryMock, this.passwordEncoder);
  }

  @Test
  public void shouldReturnUserWhenPasswordMatches() {
    // given
    String email = "test@test.com";
    String password = "password123";
    User expectedUser = new User();
    expectedUser.setEmail(email);
    expectedUser.setActive(true);
    expectedUser.setPassword(passwordEncoder.encode(password));
    when(this.userRepositoryMock.findByEmail(email)).thenReturn(Optional.of(expectedUser));

    // when
    User actualUser = this.loginService.findUser(email, password);

    // then
    assertThat(actualUser, equalTo(expectedUser));
    verify(this.userRepositoryMock, times(1)).findByEmail(email);
  }

  @Test
  public void shouldReturnNullWhenPasswordDoesNotMatch() {
    // given
    String email = "test@test.com";
    User expectedUser = new User();
    expectedUser.setEmail(email);
    expectedUser.setActive(true);
    expectedUser.setPassword(passwordEncoder.encode("correctPassword"));
    when(this.userRepositoryMock.findByEmail(email)).thenReturn(Optional.of(expectedUser));

    // when
    User actualUser = this.loginService.findUser(email, "wrongPassword");

    // then
    assertThat(actualUser, is(nullValue()));
  }

  @Test
  public void shouldReturnNullWhenUserIsNotActive() {
    // given
    String email = "test@test.com";
    User inactiveUser = new User();
    inactiveUser.setEmail(email);
    inactiveUser.setActive(false);
    inactiveUser.setPassword(passwordEncoder.encode("password123"));
    when(this.userRepositoryMock.findByEmail(email)).thenReturn(Optional.of(inactiveUser));

    // when
    User actualUser = this.loginService.findUser(email, "password123");

    // then
    assertThat(actualUser, is(nullValue()));
  }

  @Test
  public void shouldReturnNullWhenEmailDoesNotExist() {
    // given
    String email = "test@test.com";
    when(this.userRepositoryMock.findByEmail(email)).thenReturn(Optional.empty());

    // when
    User actualUser = this.loginService.findUser(email, "password123");

    // then
    assertThat(actualUser, is(nullValue()));
  }

  @Test
  public void shouldSaveNewUserWithDefaultsAndEncryptedPasswordWhenNotRegistered() {
    // given
    String email = "new@test.com";
    String password = "password123";
    when(this.userRepositoryMock.findByEmail(email)).thenReturn(Optional.empty());

    // when
    this.loginService.register(email, password, "John", "Doe");

    // then
    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(this.userRepositoryMock, times(1)).save(captor.capture());
    User saved = captor.getValue();
    assertThat(saved.getEmail(), is(equalTo(email)));
    assertThat(saved.getFirstName(), is(equalTo("John")));
    assertThat(saved.getLastName(), is(equalTo("Doe")));
    assertThat(saved.getRole(), is(equalTo("USER")));
    assertThat(saved.getActive(), is(true));
    assertThat(saved.getPassword(), not(equalTo(password)));
    assertThat(this.passwordEncoder.matches(password, saved.getPassword()), is(true));
  }

  @Test
  public void shouldThrowWhenUserAlreadyExists() {
    // given
    String email = "exists@test.com";
    when(this.userRepositoryMock.findByEmail(email)).thenReturn(Optional.of(new User()));

    // when and then
    assertThrows(
      UserAlreadyExists.class,
      () -> this.loginService.register(email, "password123", "John", "Doe")
    );
    verify(this.userRepositoryMock, times(0)).save(any(User.class));
  }

  @Test
  public void shouldThrowWhenEmailExistsEvenWithDifferentPassword() {
    // given: a user already exists with that email and another password
    String email = "exists@test.com";
    User existing = new User();
    existing.setEmail(email);
    existing.setPassword(passwordEncoder.encode("existingPassword"));
    when(this.userRepositoryMock.findByEmail(email)).thenReturn(Optional.of(existing));

    // when and then: signup is rejected because the email already exists
    assertThrows(
      UserAlreadyExists.class,
      () -> this.loginService.register(email, "differentPassword", "John", "Doe")
    );
    verify(this.userRepositoryMock, times(0)).save(any(User.class));
  }
}
