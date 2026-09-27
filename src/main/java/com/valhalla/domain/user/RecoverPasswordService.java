package com.valhalla.domain.user;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RecoverPasswordService {

  private final UserRepository userRepository;
  private final UserService userService;

  @Autowired
  public RecoverPasswordService(UserRepository userRepository, UserService userService) {
    this.userRepository = userRepository;
    this.userService = userService;
  }

  /**
   * Busca al usuario por email y rota su contraseña.
   * Si no existe, lanza una excepción de dominio con el mensaje requerido.
   *
   * @param email correo del usuario a recuperar
   * @return la nueva contraseña provisoria generada
   */
  public String recoverPassword(String email) {
    Optional<User> userOptional = userRepository.findByEmail(email);

    if (userOptional.isEmpty()) {
      throw new IllegalArgumentException("Email no encontrado");
    }

    User user = userOptional.get();
    return userService.rotatePassword(user.getId());
  }
}
