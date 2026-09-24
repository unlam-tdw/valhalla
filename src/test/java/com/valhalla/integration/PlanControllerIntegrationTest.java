package com.valhalla.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@WebIntegrationTest
@Transactional
public class PlanControllerIntegrationTest {

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  @BeforeEach
  public void setUp() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
  }

  @Test
  @WithMockUser(username = "user@test.com")
  public void T_PLN_009_getPlanes_muestraLista() throws Exception {
    this.mockMvc.perform(get("/planes"))
      .andExpect(status().isOk())
      .andExpect(view().name("pages/plans/list"))
      .andExpect(model().attributeExists("plans"));
  }

  @Test
  @WithMockUser(username = "user@test.com")
  public void T_PLN_009_getPlanesCrear_muestraFormulario() throws Exception {
    this.mockMvc.perform(get("/planes/crear"))
      .andExpect(status().isOk())
      .andExpect(view().name("pages/plans/create"))
      .andExpect(model().attributeExists("plan"));
  }

  @Test
  @WithMockUser(username = "user@test.com")
  public void T_PLN_010_postPlanesCrear_creaYRedirige() throws Exception {
    this.mockMvc.perform(post("/planes/crear").with(csrf()).param("name", "Viaje de integración"))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/planes"));
  }

  @Test
  @WithMockUser(username = "user@test.com")
  public void T_PLN_011_getPlanesId_muestraDetalle() throws Exception {
    this.mockMvc.perform(post("/planes/crear").with(csrf()).param("name", "Plan para detalle"))
      .andExpect(status().is3xxRedirection());

    this.mockMvc.perform(get("/planes/1"))
      .andExpect(status().isOk())
      .andExpect(view().name("pages/plans/detail"));
  }

  @Test
  @WithMockUser(username = "user@test.com")
  public void T_PLN_011_getPlanesId_idInexistente() throws Exception {
    this.mockMvc.perform(get("/planes/999999"))
      .andExpect(status().isOk())
      .andExpect(view().name("pages/error"));
  }

  @Test
  @WithMockUser(username = "user@test.com")
  public void T_PLN_012_postPlanesDelete_borraYRedirige() throws Exception {
    this.mockMvc.perform(post("/planes/crear").with(csrf()).param("name", "Plan a borrar"))
      .andExpect(status().is3xxRedirection());

    this.mockMvc.perform(post("/planes/delete/1").with(csrf()))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/planes"));

    this.mockMvc.perform(get("/planes/1")).andExpect(view().name("pages/error"));
  }
}