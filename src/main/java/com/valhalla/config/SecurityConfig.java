package com.valhalla.config;

import com.valhalla.infrastructure.security.CustomAuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Autowired
  private CustomAuthenticationSuccessHandler successHandler;

  // 1. Cadena para Usuarios Comunes (/auth/**) [AUT-01]

  @Bean
  @Order(1)
  public SecurityFilterChain authFilterChain(HttpSecurity http, SessionRegistry sessionRegistry)
    throws Exception {
    http
      .securityMatcher("/auth/**")
      .authorizeHttpRequests(auth ->
        // AC-10: todo el namespace /auth/** es público (form, registro y recuperación).
        // Un endpoint nuevo bajo /auth/ nace público, como pide la spec, en vez de
        // aparecer bloqueado en silencio por un anyRequest() restrictivo.
        auth.anyRequest().permitAll()
      )
      .formLogin(form ->
        form
          .loginPage("/auth/login")
          .loginProcessingUrl("/auth/validate-login")
          .usernameParameter("username")
          .passwordParameter("password")
          .successHandler(successHandler)
          .failureUrl("/auth/login?error=true")
          .permitAll()
      )
      .logout(logout ->
        logout
          .logoutUrl("/auth/logout")
          .logoutSuccessUrl("/auth/login?logout=true")
          .invalidateHttpSession(true)
          .deleteCookies("JSESSIONID")
          .permitAll()
      )
      .sessionManagement(session ->
        session.maximumSessions(1).sessionRegistry(sessionRegistry).maxSessionsPreventsLogin(false)
      );

    return http.build();
  }

  // 2. Cadena para Administradores

  @Bean
  @Order(2)
  public SecurityFilterChain adminFilterChain(HttpSecurity http, SessionRegistry sessionRegistry)
    throws Exception {
    http
      .securityMatcher("/admin/**")
      .authorizeHttpRequests(auth ->
        auth
          .requestMatchers("/admin/login", "/admin/validate-login")
          .permitAll()
          .requestMatchers("/admin/home")
          .authenticated()
          .requestMatchers("/admin/**")
          .hasRole("ADMIN")
          .anyRequest()
          .authenticated()
      )
      .formLogin(form ->
        form
          .loginPage("/admin/login")
          .loginProcessingUrl("/admin/validate-login")
          .usernameParameter("email")
          .passwordParameter("password")
          .successHandler(successHandler)
          .failureUrl("/admin/login?error=true")
          .permitAll()
      )
      .logout(logout ->
        logout
          .logoutUrl("/admin/logout")
          // Mirrors the /auth chain above: the flag is what lets the login view confirm the
          // session ended, instead of the user landing on a form that says nothing about it.
          .logoutSuccessUrl("/admin/login?logout=true")
          .invalidateHttpSession(true)
          .deleteCookies("JSESSIONID")
      )
      .sessionManagement(session ->
        session.maximumSessions(1).sessionRegistry(sessionRegistry).maxSessionsPreventsLogin(false)
      );

    return http.build();
  }

  // 3. Cadena Default para el resto del sitio

  @Bean
  @Order(3)
  public SecurityFilterChain defaultFilterChain(HttpSecurity http, SessionRegistry sessionRegistry)
    throws Exception {
    http
      .authorizeHttpRequests(auth ->
        auth
          .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/**", "/places")
          .permitAll()
          .requestMatchers("/api/**")
          .authenticated()
          .requestMatchers("/", "/share/**", "/reload/**")
          .permitAll()
          .requestMatchers("/css/**", "/js/**", "/images/**")
          .permitAll()
          .anyRequest()
          .authenticated()
      )
      .formLogin(form ->
        form
          .loginPage("/auth/login")
          .loginProcessingUrl("/auth/validate-login")
          .usernameParameter("username")
          .passwordParameter("password")
          .successHandler(successHandler)
          .failureUrl("/auth/login?error=true")
          .permitAll()
      )
      .exceptionHandling(ex ->
        ex.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/auth/login"))
      )
      .sessionManagement(session ->
        session.maximumSessions(1).sessionRegistry(sessionRegistry).maxSessionsPreventsLogin(false)
      );

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SessionRegistry sessionRegistry() {
    return new SessionRegistryImpl();
  }
}
