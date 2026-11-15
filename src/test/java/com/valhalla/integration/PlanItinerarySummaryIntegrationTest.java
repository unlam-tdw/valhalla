package com.valhalla.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.place.PlaceCategory;
import com.valhalla.domain.place.PlaceRepository;
import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanRepository;
import com.valhalla.domain.plan.TransportMode;
import com.valhalla.domain.planplace.PlanPlace;
import com.valhalla.domain.user.User;
import com.valhalla.domain.user.UserRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * [SCH] Integration del chip de resumen del itinerario (paradas · tiempo · km): sale del detalle
 * del dueno y de la vista publica con el mismo modelo, calculado con la misma calculadora que
 * deriva el horario, y desaparece cuando el plan no tiene paradas todavia.
 */
@WebIntegrationTest
@Transactional
public class PlanItinerarySummaryIntegrationTest {

  private static final String OWNER_EMAIL = "summary.owner@test.com";
  private static final String PLAN_NAME = "Plan con resumen";
  private static final String SHORT_CODE = "SUMM1234";
  private static final LocalDate EVENT_DATE = LocalDate.of(2026, 11, 12);

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
    User owner = new User();
    owner.setEmail(OWNER_EMAIL);
    owner.setPassword("password123");
    owner.setRole("USER");
    owner.setFirstName("Ada");
    owner.setLastName("Lovelace");
    owner.activate();
    this.userRepository.save(owner);
  }

  @Test
  public void theDetailHeaderShowsTheSummaryChip() throws Exception {
    UUID planId = givenPlanWithTwoStops(false);

    String html = html("/plans/" + planId);

    // 90 + 60 de visitas y 40 de traslado a pie entre dos lugares a 2,9 km con rodeo
    assertThat(html, containsString("ITINERARIO"));
    assertThat(html, containsString("2 paradas · 3 h 10 min · 2,9 km"));
  }

  @Test
  public void aPlanWithoutStopsHidesTheSummaryChip() throws Exception {
    UUID planId = givenPlanWithTwoStops(false);
    Plan plan = this.planRepository.findById(planId).orElseThrow();
    plan.getPlanPlaces().clear();
    this.planRepository.save(plan);

    String html = html("/plans/" + planId);

    assertThat(html, not(containsString("ITINERARIO")));
  }

  private String html(String url) throws Exception {
    return this.mockMvc.perform(get(url).with(user(OWNER_EMAIL)))
      .andExpect(status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString();
  }

  private UUID givenPlanWithTwoStops(boolean isPublic) {
    Plan plan = new Plan();
    plan.setName(PLAN_NAME);
    plan.setShortCode(SHORT_CODE);
    plan.setEventDate(EVENT_DATE);
    plan.setEventTime(LocalTime.of(9, 0));
    plan.setTransportMode(TransportMode.WALK);
    plan.setIsPublic(isPublic);
    plan.setAdministrator(this.userRepository.findByEmail(OWNER_EMAIL).orElseThrow());
    plan.addPlanPlace(stop(place("Museo del resumen", PlaceCategory.MUSEUM, -34.6, -58.4), 90));
    plan.addPlanPlace(stop(place("Bar del resumen", PlaceCategory.BAR, -34.62, -58.4), 60));
    return this.planRepository.save(plan).getId();
  }

  private Place place(String name, PlaceCategory category, double latitude, double longitude) {
    Place place = new Place();
    place.setName(name);
    place.setCategory(category);
    place.setLatitude(latitude);
    place.setLongitude(longitude);
    this.placeRepository.save(place);
    return place;
  }

  private static PlanPlace stop(Place place, Integer durationMinutes) {
    PlanPlace entry = new PlanPlace();
    entry.setPlace(place);
    entry.setDurationMinutes(durationMinutes);
    entry.setSortOrder(1);
    return entry;
  }
}
