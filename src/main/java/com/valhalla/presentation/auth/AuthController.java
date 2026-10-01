package com.valhalla.presentation.auth;

import com.valhalla.domain.exception.UserAlreadyExists;
import com.valhalla.domain.login.LoginService;
import com.valhalla.infrastructure.user.RecoverPasswordService;
import com.valhalla.presentation.shared.RecoverPasswordRequest;
import com.valhalla.presentation.shared.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/auth")
public class AuthController {

  private final LoginService loginService;
  private final RecoverPasswordService recoverPasswordService;

  @Autowired
  public AuthController(LoginService loginService, RecoverPasswordService recoverPasswordService) {
    this.loginService = loginService;
    this.recoverPasswordService = recoverPasswordService;
  }

  // --- LOGIN ---
  @GetMapping("/login")
  public String showLoginForm(
    @RequestParam(value = "error", required = false) String error,
    Model model
  ) {
    model.addAttribute("error", "true".equals(error));
    return "pages/auth/user/login";
  }

  // --- REGISTRO ---
  @GetMapping("/register")
  public String showRegisterForm(Model model) {
    model.addAttribute("registerRequest", new RegisterRequest());
    return "pages/auth/user/register";
  }

  @PostMapping("/register")
  public String handleRegister(
    @Valid @ModelAttribute("registerRequest") RegisterRequest request,
    BindingResult bindingResult,
    Model model
  ) {
    if (bindingResult.hasErrors()) {
      return "pages/auth/user/register";
    }

    try {
      loginService.register(request.getEmail(), request.getPassword());
      return "redirect:/auth/login";
    } catch (UserAlreadyExists e) {
      model.addAttribute("errorMessage", "Ese email ya está registrado");
      return "pages/auth/user/register";
    }
  }

  // --- RECUPERACIÓN DE CONTRASEÑA ---
  @GetMapping("/forgot-password")
  public String showForgotPasswordForm(Model model) {
    model.addAttribute("recoverPasswordRequest", new RecoverPasswordRequest());
    return "pages/auth/user/forgot-password";
  }

  @PostMapping("/recover")
  public String handleRecoverPassword(
    @Valid @ModelAttribute("recoverPasswordRequest") RecoverPasswordRequest request,
    BindingResult bindingResult,
    Model model
  ) {
    if (bindingResult.hasErrors()) {
      return "pages/auth/user/forgot-password";
    }

    try {
      String tempPassword = recoverPasswordService.recoverPassword(request.getEmail());
      model.addAttribute("tempPassword", tempPassword);
      return "pages/auth/user/recovered";
    } catch (IllegalArgumentException e) {
      model.addAttribute("errorMessage", e.getMessage());
      return "pages/auth/user/forgot-password";
    }
  }
}
