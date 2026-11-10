package com.valhalla.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanRepository;
import com.valhalla.domain.user.User;
import com.valhalla.domain.user.UserRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * [PAGE] Paginacion del feed: el recorte ocurre despues del ranking, las paginas no se repiten y
 * el fragmento parcial reutiliza el markup de la card.
 */
@WebIntegrationTest
@Transactional
public class ForYouPaginationIntegrationTest {

  private static final String AUTHOR_EMAIL = "for.you.pagination@test.com";
  private static final String QUERY = "PAGINACION";
  private static final String FIRST = "Paginacion Alfa";
  private static final String SECOND = "Paginacion Beta";
  private static final String THIRD = "Paginacion Gamma";

  @Autowired
  private WebApplicationContext wac;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PlanRepository planRepository;

  private MockMvc mockMvc;
  private UUID authorId;

  @BeforeEach
  public void setUp() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
    this.authorId = givenUser();
  }

  @Test
  public void T_PAGE_002_laPagina1YLa2NoRepitenPlanesYConservanElRanking() throws Exception {
    // given: tres planes con fechas ascendentes; sin votos el ranking popular desempata por fecha
    givenThreePlans();

    // when / then: la primera ventana respeta ese orden y avisa que hay mas
    String page1 =
      this.mockMvc.perform(get("/for-you?q=" + QUERY + "&size=2&page=1"))
        .andExpect(status().isOk())
        .andExpect(view().name("pages/plans/public-list"))
        .andExpect(model().attribute("page", 1))
        .andExpect(model().attribute("size", 2))
        .andExpect(model().attribute("total", 3))
        .andExpect(model().attribute("hasMore", true))
        .andReturn()
        .getResponse()
        .getContentAsString();
    assertThat(page1, containsString(FIRST));
    assertThat(page1, containsString(SECOND));
    assertThat(page1, not(containsString(THIRD)));
    assertThat(page1, containsString("data-load-more"));

    String page2 =
      this.mockMvc.perform(get("/for-you?q=" + QUERY + "&size=2&page=2"))
        .andExpect(status().isOk())
        .andExpect(view().name("pages/plans/public-list"))
        .andExpect(model().attribute("page", 2))
        .andExpect(model().attribute("size", 2))
        .andExpect(model().attribute("total", 3))
        .andExpect(model().attribute("hasMore", false))
        .andReturn()
        .getResponse()
        .getContentAsString();
    assertThat(page2, containsString(THIRD));
    assertThat(page2, not(containsString(FIRST)));
    assertThat(page2, not(containsString(SECOND)));
  }

  @Test
  public void T_PAGE_003_hasMoreEsFalsoEnLaUltimaPagina() throws Exception {
    givenThreePlans();

    this.mockMvc.perform(get("/for-you?q=" + QUERY + "&size=2&page=2"))
      .andExpect(status().isOk())
      .andExpect(model().attribute("hasMore", false));
  }

  @Test
  public void T_PAGE_005_sinJsLaUrlPage2RenderizaLaSegundaPaginaCompleta() throws Exception {
    givenThreePlans();

    String html =
      this.mockMvc.perform(get("/for-you?q=" + QUERY + "&size=2&page=2"))
        .andExpect(status().isOk())
        .andExpect(view().name("pages/plans/public-list"))
        .andReturn()
        .getResponse()
        .getContentAsString();

    assertThat(html, containsString("<html"));
    assertThat(html, containsString(THIRD));
    assertThat(html, not(containsString(FIRST)));
  }

  @Test
  public void T_PAGE_005_elEndpointParcialDevuelveSoloLasCardsDeLaVentana() throws Exception {
    givenThreePlans();

    String html =
      this.mockMvc.perform(get("/for-you?partial=&q=" + QUERY + "&size=2&page=2"))
        .andExpect(status().isOk())
        .andExpect(view().name("components/feed-cards :: feedPage"))
        .andExpect(model().attribute("page", 2))
        .andReturn()
        .getResponse()
        .getContentAsString();

    assertThat(html, containsString(THIRD));
    assertThat(html, containsString("data-feed-page"));
    assertThat(html, not(containsString("<html")));
  }

  private UUID givenUser() {
    User user = new User();
    user.setEmail(AUTHOR_EMAIL);
    user.setPassword("password123");
    user.setRole("USER");
    user.setFirstName("Paginador");
    user.setLastName("Test");
    user.activate();
    this.userRepository.save(user);
    return this.userRepository.findByEmail(AUTHOR_EMAIL).orElseThrow().getId();
  }

  private void givenThreePlans() {
    LocalDate today = LocalDate.now();
    givenPlan(FIRST, today.plusDays(1));
    givenPlan(SECOND, today.plusDays(2));
    givenPlan(THIRD, today.plusDays(3));
  }

  private void givenPlan(String name, LocalDate eventDate) {
    Plan plan = new Plan();
    plan.setName(name);
    plan.setDescription(name);
    plan.setEventDate(eventDate);
    plan.setIsPublic(true);
    plan.setAdministrator(this.userRepository.findById(this.authorId).orElseThrow());
    this.planRepository.save(plan);
  }
}
