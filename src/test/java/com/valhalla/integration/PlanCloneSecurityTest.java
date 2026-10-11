package com.valhalla.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanRepository;
import com.valhalla.domain.user.User;
import com.valhalla.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * [CLO] Seguridad de {@code POST /plans/{id}/clone} (el boton "Usar plan" del feed PPV): sin
 * sesion cae en login (S-01), el GET no es un alias del clonado (S-02) y sin CSRF llega un 403
 * sin crear la copia (S-03).
 */
@WebIntegrationTest
@Transactional
public class PlanCloneSecurityTest {

  private static final String OWNER_EMAIL = "dueno.clone@test.com";
  private static final String CLONER_EMAIL = "clonador.clone@test.com";

  @Autowired
  private WebApplicationContext wac;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PlanRepository planRepository;

  private MockMvc mockMvc;

  @BeforeEach
  public void setUp() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
  }

  @Test
  public void S01_postCloneAnonimoRedirigeALoginYSinCrearCopia() throws Exception {
    Long sourceId = givenAPublicPlan();

    this.mockMvc.perform(post("/plans/" + sourceId + "/clone").with(csrf()))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrlPattern("**/auth/login"));

    assertThat(this.planRepository.findAll(), hasSize(1));
  }

  @Test
  public void S02_getCloneNoEsElClonadoYNoCreaNada() throws Exception {
    Long sourceId = givenAPublicPlan();

    this.mockMvc.perform(get("/plans/" + sourceId + "/clone").with(user(CLONER_EMAIL)))
      .andExpect(status().isMethodNotAllowed());

    assertThat(this.planRepository.findAll(), hasSize(1));
  }

  @Test
  public void S03_postCloneSinCsrfResponde403YSinCrearCopia() throws Exception {
    Long sourceId = givenAPublicPlan();

    this.mockMvc.perform(post("/plans/" + sourceId + "/clone").with(user(CLONER_EMAIL)))
      .andExpect(status().isForbidden());

    assertThat(this.planRepository.findAll(), hasSize(1));
  }

  // ---

  private Long givenAPublicPlan() {
    User owner = givenUser(OWNER_EMAIL);
    Plan plan = new Plan();
    plan.setName("Plan publico para clonar");
    plan.setAdministrator(owner);
    plan.setIsPublic(true);
    plan.setShortCode("CLONE001");
    return this.planRepository.save(plan).getId();
  }

  private User givenUser(String email) {
    User user = new User();
    user.setEmail(email);
    user.setPassword("password123");
    user.setRole("USER");
    user.setFirstName("Nombre");
    user.setLastName("Apellido");
    user.activate();
    this.userRepository.save(user);
    return this.userRepository.findByEmail(email).orElseThrow();
  }
}
