package com.valhalla.config;

import com.valhalla.infrastructure.security.CustomAuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Autowired
  private CustomAuthenticationSuccessHandler successHandler;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http, SessionRegistry sessionRegistry)
    throws Exception {
    http
      .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
      .authorizeHttpRequests(auth ->
        auth
          .requestMatchers("/", "/share/**", "/api/**", "/reload/**")
          .permitAll()
          .requestMatchers("/css/**", "/js/**", "/images/**")
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
          .logoutSuccessUrl("/admin/login")
          .invalidateHttpSession(true)
          .deleteCookies("JSESSIONID")
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
    // Exposed as a bean so maximumSessions(1) (AC-13) tracks sessions in a
    // registry the app (and tests) can inspect; paired with
    // HttpSessionEventPublisher in MyServletInitializer for cleanup.
    return new SessionRegistryImpl();
  }
}
