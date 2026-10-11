package com.valhalla.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.valhalla.domain.place.PlaceRepository;
import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanRepository;
import com.valhalla.domain.planplace.PlanPlace;
import com.valhalla.domain.user.User;
import com.valhalla.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * [PPV] Contrato de seguridad del feed de planes publicos contra la cadena real de
 * {@code SecurityConfig}: el feed exige sesion (S-01/S-02) y, ya adentro, no filtra informacion
 * privada de los planes (S-03).
 */
@WebIntegrationTest
@Transactional
public class PlanPublicSecurityTest {

  private static final String OWNER_EMAIL = "dueno.ppv@test.com";
  private static final String PARTICIPANT_EMAIL = "participante.ppv@test.com";
  private static final String VISITOR_EMAIL = "visitante.ppv@test.com";
  private static final String PUBLIC_PLAN_NAME = "Plan publico visible";
  private static final String PRIVATE_PLAN_NAME = "Plan privado oculto";

  @Autowired
  private WebApplicationContext wac;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PlanRepository planRepository;

  @Autowired
  private PlaceRepository placeRepository;

  private MockMvc mockMvc;

  @BeforeEach
  public void setUp() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
  }

  // S-01: el feed es para usuarios con sesion. El visitante aterriza en /auth/login.
  @Test
  public void S01_getPlansPublicAnonimoRedirigeALogin() throws Exception {
    givenAPublicPlan();

    this.mockMvc.perform(get("/plans/public"))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrlPattern("**/auth/login"));
  }

  // S-01b: con sesion el mismo GET responde 200 y rinde el listado.
  @Test
  public void S01b_getPlansPublicConSesionResponde200() throws Exception {
    givenAPublicPlan();

    this.mockMvc.perform(get("/plans/public").with(user(VISITOR_EMAIL))).andExpect(status().isOk());
  }

  // S-02: sin sesion no entra ni el resto de verbos de la ruta. Con token CSRF la request llega
  // al filtro de autorizacion, asi que el redirect a login prueba la regla y no un 403 de CSRF.
  @Test
  public void S02_postPlansPublicAnonimoRedirigeALogin() throws Exception {
    this.mockMvc.perform(post("/plans/public").with(csrf()))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrlPattern("**/auth/login"));
  }

  // S-03: ni shortCode, ni emails de participantes ni datos de planes privados.
  @Test
  public void S03_elListadoNoExponeInformacionPrivada() throws Exception {
    Plan publicPlan = givenAPublicPlan();
    givenAPrivatePlan();

    String html =
      this.mockMvc.perform(get("/plans/public").with(user(VISITOR_EMAIL)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    assertThat(html, containsString(PUBLIC_PLAN_NAME));
    assertThat(html, not(containsString(publicPlan.getShortCode())));
    assertThat(html, not(containsString(PARTICIPANT_EMAIL)));
    assertThat(html, not(containsString(OWNER_EMAIL)));
    assertThat(html, not(containsString(PRIVATE_PLAN_NAME)));
  }

  // --- fixtures ---

  private Plan givenAPublicPlan() {
    User owner = givenUser(OWNER_EMAIL);
    Plan plan = new Plan();
    plan.setName(PUBLIC_PLAN_NAME);
    plan.setAdministrator(owner);
    plan.setShortCode("PPVSECRE");
    plan.setIsPublic(true);
    User participant = givenUser(PARTICIPANT_EMAIL);
    plan.getParticipants().add(participant);
    PlanPlace entry = new PlanPlace();
    entry.setPlace(this.placeRepository.findAll().get(0));
    plan.addPlanPlace(entry);
    return this.planRepository.save(plan);
  }

  private void givenAPrivatePlan() {
    Plan plan = new Plan();
    plan.setName(PRIVATE_PLAN_NAME);
    plan.setAdministrator(this.userRepository.findByEmail(OWNER_EMAIL).orElseThrow());
    this.planRepository.save(plan);
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
