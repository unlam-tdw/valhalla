package com.valhalla.integration;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebIntegrationTest
public class SecurityConfigTest {

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  @BeforeEach
  public void setUp() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
  }

  @Test
  public void shouldAllowPlacesWithoutSession() throws Exception {
    this.mockMvc.perform(get("/places"))
      .andExpect(status().isOk())
      .andExpect(model().attributeDoesNotExist("userPlans"));
  }

  @Test
  public void shouldRedirectToLoginWhenAccessingPlansWithoutSession() throws Exception {
    this.mockMvc.perform(get("/plans"))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrlPattern("**/auth/login"));
  }

  // --- S-03: a permitted path that no controller maps answers 404, not 403 and not a 200 page ---

  @Test
  public void shouldReturnNotFoundForUnmappedPublicPath() throws Exception {
    this.mockMvc.perform(get("/share/abc123"))
      .andExpect(status().isNotFound())
      // Proves the 404 came from GlobalExceptionHandler and not from the servlet container's own
      // error page, which would answer 404 with an empty body.
      .andExpect(model().attribute("error", "Page not found"));
  }

  @Test
  public void shouldAllowPublicAccessToLandingPage() throws Exception {
    this.mockMvc.perform(get("/")).andExpect(status().isOk());
  }

  @Test
  public void shouldAllowPublicAccessToReloadEndpoint() throws Exception {
    this.mockMvc.perform(get("/reload/version")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "USER")
  public void shouldAllowUserRoleAccessToHome() throws Exception {
    this.mockMvc.perform(get("/admin/home")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "USER")
  public void shouldDenyUserRoleAccessToAdminUsers() throws Exception {
    this.mockMvc.perform(get("/admin/users")).andExpect(status().isForbidden());
  }

  // --- S-01: the whole /auth/** namespace is public, so a form never redirects to login ---

  @Test
  public void shouldAllowPublicAccessToTheAuthForms() throws Exception {
    this.mockMvc.perform(get("/auth/login")).andExpect(status().isOk());
    this.mockMvc.perform(get("/auth/register")).andExpect(status().isOk());
    this.mockMvc.perform(get("/auth/forgot-password")).andExpect(status().isOk());
  }

  // --- S-05: a register post without a token is refused ---

  @Test
  public void shouldRejectRegisterPostWithoutCsrf() throws Exception {
    this.mockMvc.perform(
        post("/auth/register").param("email", "s01@unlam.edu.ar").param("password", "secret123")
      )
      .andExpect(status().isForbidden());
  }
}
