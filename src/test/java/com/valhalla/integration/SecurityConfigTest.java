package com.valhalla.integration;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
  public void shouldRedirectToLoginWhenAccessingPlacesWithoutSession() throws Exception {
    this.mockMvc.perform(get("/places")).andExpect(status().is3xxRedirection());
  }

  @Test
  public void shouldRedirectToLoginWhenAccessingPlansWithoutSession() throws Exception {
    this.mockMvc.perform(get("/plans")).andExpect(status().is3xxRedirection());
  }

  @Test
  public void shouldAllowPublicAccessToSharePage() throws Exception {
    this.mockMvc.perform(get("/share/abc123")).andExpect(status().isOk());
  }

  @Test
  public void shouldAllowPublicAccessToLandingPage() throws Exception {
    this.mockMvc.perform(get("/")).andExpect(status().isOk());
  }
}
