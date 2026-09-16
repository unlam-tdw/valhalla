package com.valhalla.config;

import com.valhalla.infrastructure.security.CustomAuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Autowired
  private CustomAuthenticationSuccessHandler successHandler;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
      .authorizeHttpRequests(auth ->
        auth
          .requestMatchers("/", "/share/**", "/api/**")
          .permitAll()
          .requestMatchers("/css/**", "/js/**", "/images/**")
          .permitAll()
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
      .sessionManagement(session -> session.maximumSessions(1).maxSessionsPreventsLogin(false));
    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
