package com.valhalla.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebIntegrationTest
public class PlaceControllerTest {

  @Autowired
  private WebApplicationContext webApplicationContext;

  private MockMvc mockMvc;

  @BeforeEach
  public void setUp() {
    mockMvc =
      MockMvcBuilders
        .webAppContextSetup(webApplicationContext)
        .apply(SecurityMockMvcConfigurers.springSecurity())
        .build();
  }

  @Test
  @WithMockUser
  public void shouldReturnPlacesList() throws Exception {
    mockMvc
      .perform(get("/places"))
      .andExpect(status().isOk())
      .andExpect(view().name("pages/places/list"))
      .andExpect(model().attributeExists("places"));
  }

  @Test
  @WithMockUser
  public void shouldFilterPlacesByCategory() throws Exception {
    mockMvc
      .perform(get("/places").param("category", "RESTAURANT"))
      .andExpect(status().isOk())
      .andExpect(view().name("pages/places/list"))
      .andExpect(model().attributeExists("places"));
  }

  @Test
  @WithMockUser
  public void shouldSearchPlacesByName() throws Exception {
    mockMvc
      .perform(get("/places").param("search", "Don"))
      .andExpect(status().isOk())
      .andExpect(view().name("pages/places/list"))
      .andExpect(model().attributeExists("places"));
  }

  @Test
  @WithMockUser
  public void shouldReturnPlaceDetailWhenIdExists() throws Exception {
    mockMvc
      .perform(get("/places/1"))
      .andExpect(status().isOk())
      .andExpect(view().name("pages/places/detail"))
      .andExpect(model().attributeExists("place"));
  }

  @Test
  @WithMockUser
  public void shouldReturnPlaceDetailWithoutPlaceWhenIdDoesNotExist() throws Exception {
    mockMvc
      .perform(get("/places/999"))
      .andExpect(status().isOk())
      .andExpect(view().name("pages/places/detail"));
  }
}
