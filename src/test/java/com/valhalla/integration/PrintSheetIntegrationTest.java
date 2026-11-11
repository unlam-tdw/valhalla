package com.valhalla.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
 * Integration coverage of the printable itinerary (docs/specs/27-PRINT.md, I-PRINT-01..04): el boton
 * "Imprimir" existe en el detalle del dueno y en la vista publica y no en los listados, y la hoja
 * imprime cabecera, dias, paradas, tramos y pie sin depender del JS del detalle.
 */
@WebIntegrationTest
@Transactional
public class PrintSheetIntegrationTest {

  private static final String OWNER_EMAIL = "print.owner@test.com";
  private static final String OWNER_NAME = "Grace Hopper";
  private static final String PLAN_NAME = "Plan imprimible";
  private static final String SHORT_CODE = "PRNT1234";
  private static final LocalDate EVENT_DATE = LocalDate.of(2026, 11, 12);
  private static final LocalDate SECOND_DAY = LocalDate.of(2026, 11, 13);

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
    givenOwner();
  }

  @Test
  public void theOwnerDetailRendersThePrintButtonAndTheWholeSheet() throws Exception {
    UUID planId = givenPlanWithThreeStops(false);

    String html = html("/plans/" + planId);

    assertThat(html, containsString("data-print-plan"));
    String sheet = printSheet(html);
    assertThat(sheet, containsString(PLAN_NAME));
    assertThat(sheet, containsString("12/11/2026"));
    assertThat(sheet, containsString("09:00"));
    assertThat(sheet, containsString("Privado"));
    assertThat(sheet, containsString(OWNER_NAME));
    assertThat(sheet, containsString(SHORT_CODE));
    assertThat(sheet, containsString("Día 1 · 12/11"));
    assertThat(sheet, containsString("Día 2 · 13/11"));
    assertThat(sheet, containsString("Museo de impresion"));
    assertThat(sheet, containsString("Museo"));
    assertThat(sheet, containsString("Av. Siempre Viva 742"));
    assertThat(sheet, containsString("09:00 – 10:30"));
    assertThat(sheet, containsString("1h 30m"));
    assertThat(sheet, containsString("25 min en colectivo"));
  }

  @Test
  public void thePublicViewRendersTheSameSheetWithThePublicUrlAndTheShortCode() throws Exception {
    UUID planId = givenPlanWithThreeStops(true);

    String html = html("/plans/" + planId + "/public");

    assertThat(html, containsString("data-print-plan"));
    String sheet = printSheet(html);
    assertThat(sheet, containsString(PLAN_NAME));
    assertThat(sheet, containsString("Público"));
    assertThat(sheet, containsString(OWNER_NAME));
    assertThat(sheet, containsString("Día 1 · 12/11"));
    assertThat(sheet, containsString("/plans/" + planId + "/public"));
    // PVP AC-04: la vista publica no expone el codigo corto en el HTML. La hoja de esa vista
    // imprime la URL publica; el codigo solo viaja en la hoja del dueno.
    assertThat(sheet, not(containsString(SHORT_CODE)));
  }

  @Test
  public void theListingsDoNotRenderThePrintButton() throws Exception {
    givenPlanWithThreeStops(true);

    assertThat(html("/plans"), not(containsString("data-print-plan")));
    assertThat(html("/for-you"), not(containsString("data-print-plan")));
  }

  @Test
  public void aPlanWithoutStopsKeepsTheHeaderAndShowsTheEmptyState() throws Exception {
    UUID planId = givenPlanWithThreeStops(false);
    Plan plan = this.planRepository.findById(planId).orElseThrow();
    plan.getPlanPlaces().clear();
    this.planRepository.save(plan);

    String sheet = printSheet(html("/plans/" + planId));

    assertThat(sheet, containsString(PLAN_NAME));
    assertThat(sheet, containsString("Este plan todavía no tiene lugares."));
  }

  private String html(String url) throws Exception {
    return this.mockMvc.perform(get(url).with(user(OWNER_EMAIL)))
      .andExpect(status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString();
  }

  private static String printSheet(String html) {
    int start = html.indexOf("data-print-sheet");
    assertTrue(start >= 0, "la hoja de impresion tiene que estar en el HTML");
    return html.substring(start);
  }

  private void givenOwner() {
    User owner = new User();
    owner.setEmail(OWNER_EMAIL);
    owner.setPassword("password123");
    owner.setRole("USER");
    owner.setFirstName("Grace");
    owner.setLastName("Hopper");
    owner.activate();
    this.userRepository.save(owner);
  }

  private UUID givenPlanWithThreeStops(boolean isPublic) {
    Plan plan = new Plan();
    plan.setName(PLAN_NAME);
    plan.setShortCode(SHORT_CODE);
    plan.setEventDate(EVENT_DATE);
    plan.setEventTime(LocalTime.of(9, 0));
    plan.setTransportMode(TransportMode.WALK);
    plan.setIsPublic(isPublic);
    plan.setAdministrator(this.userRepository.findByEmail(OWNER_EMAIL).orElseThrow());
    plan.addPlanPlace(
      stop(
        place("Museo de impresion", PlaceCategory.MUSEUM, "Av. Siempre Viva 742"),
        EVENT_DATE,
        LocalTime.of(9, 0),
        90,
        25,
        TransportMode.BUS
      )
    );
    plan.addPlanPlace(
      stop(
        place("Bar de la esquina", PlaceCategory.BAR, "Calle Falsa 123"),
        EVENT_DATE,
        LocalTime.of(11, 35),
        60,
        null,
        null
      )
    );
    plan.addPlanPlace(
      stop(
        place("Parque de la Costa", PlaceCategory.PARK, "Costanera 1"),
        SECOND_DAY,
        LocalTime.of(10, 0),
        60,
        null,
        null
      )
    );
    return this.planRepository.save(plan).getId();
  }

  private Place place(String name, PlaceCategory category, String address) {
    Place place = new Place();
    place.setName(name);
    place.setCategory(category);
    place.setAddress(address);
    place.setLatitude(-34.6);
    place.setLongitude(-58.4);
    this.placeRepository.save(place);
    return place;
  }

  private static PlanPlace stop(
    Place place,
    LocalDate date,
    LocalTime time,
    Integer durationMinutes,
    Integer travelMinutesToNext,
    TransportMode transportModeToNext
  ) {
    PlanPlace entry = new PlanPlace();
    entry.setPlace(place);
    entry.setVisitDate(date);
    entry.setVisitTime(time);
    entry.setDurationMinutes(durationMinutes);
    entry.setTravelMinutesToNext(travelMinutesToNext);
    entry.setTransportModeToNext(transportModeToNext);
    entry.setSortOrder(1);
    return entry;
  }
}
