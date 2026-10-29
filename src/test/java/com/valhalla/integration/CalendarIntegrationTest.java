package com.valhalla.integration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanInvitation;
import com.valhalla.domain.plan.PlanRepository;
import com.valhalla.domain.user.User;
import com.valhalla.domain.user.UserRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@WebIntegrationTest
@Transactional
class CalendarIntegrationTest {

  private static final String EMAIL = "calendar.integration@test.com";
  private static final String URL = "/plans/calendar";

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private UserRepository users;

  @Autowired
  private PlanRepository plans;

  private MockMvc mvc;
  private User viewer;
  private User other;

  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    viewer = saveUser(EMAIL);
    other = saveUser("calendar.other@test.com");
  }

  @Test
  void anonymousCalendarRequiresLogin() throws Exception {
    mvc
      .perform(get(URL))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrlPattern("**/auth/login"));
  }

  @Test
  void rendersOwnedAndParticipatingPrivatePlansWithTimesAndDetailLinks() throws Exception {
    Plan owned = savePlan("Salida propia", viewer, "2024-02-29");
    Plan participating = savePlan("Salida compartida privada", other, "2024-02-29");
    participating.getParticipants().add(viewer);
    plans.save(participating);
    mvc
      .perform(get(URL).param("month", "2024-02").param("date", "2024-02-29").with(user(EMAIL)))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString(owned.getName())))
      .andExpect(content().string(containsString(participating.getName())))
      .andExpect(content().string(containsString("/plans/" + owned.getId())))
      .andExpect(content().string(containsString("/plans/" + participating.getId())))
      .andExpect(content().string(containsString("10:30")))
      .andExpect(content().string(containsString("aria-current=\"date\"")));
  }

  @Test
  void excludesOtherUsersPublicPrivatePendingAndBannedPlansEvenWithEmailParameter()
    throws Exception {
    savePlan("Privado ajeno secreto", other, "2024-02-29");
    Plan publicPlan = savePlan("Publico ajeno descubierto", other, "2024-02-29");
    publicPlan.setIsPublic(true);
    plans.save(publicPlan);
    Plan invited = savePlan("Invitacion pendiente oculta", other, "2024-02-29");
    invited.getInvitations().add(new PlanInvitation(invited, viewer, other, "PARTICIPANT"));
    plans.save(invited);
    Plan banned = savePlan("Plan con acceso prohibido", other, "2024-02-29");
    banned.getBannedUsers().add(viewer);
    plans.save(banned);
    mvc
      .perform(
        get(URL)
          .param("month", "2024-02")
          .param("date", "2024-02-29")
          .param("email", other.getEmail())
          .with(user(EMAIL))
      )
      .andExpect(status().isOk())
      .andExpect(model().attribute("hasPlans", false))
      .andExpect(content().string(not(containsString("Privado ajeno secreto"))))
      .andExpect(content().string(not(containsString(publicPlan.getName()))))
      .andExpect(content().string(not(containsString(invited.getName()))))
      .andExpect(content().string(not(containsString(banned.getName()))));
  }

  @Test
  void selectingAnotherDayChangesTheAgenda() throws Exception {
    savePlan("Plan del primer dia", viewer, "2024-02-01");
    Plan selected = savePlan("Plan del dia elegido", viewer, "2024-02-29");
    mvc
      .perform(get(URL).param("month", "2024-02").param("date", "2024-02-29").with(user(EMAIL)))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString(selected.getName())))
      .andExpect(content().string(not(containsString("Plan del primer dia"))))
      .andExpect(content().string(containsString("month=2024-03")))
      .andExpect(content().string(containsString("month=2024-01")))
      .andExpect(content().string(containsString("date=2024-02-29")));
  }

  @Test
  void undatedPlanWithoutTimeRemainsReachable() throws Exception {
    Plan undated = savePlan("Plan sin fecha", viewer, null);
    undated.setEventTime(null);
    plans.save(undated);
    mvc
      .perform(get(URL).with(user(EMAIL)))
      .andExpect(status().isOk())
      .andExpect(model().attribute("hasPlans", true))
      .andExpect(content().string(containsString("Planes sin fecha")))
      .andExpect(content().string(containsString(undated.getName())))
      .andExpect(content().string(containsString("/plans/" + undated.getId())));
  }

  @Test
  void emptyAgendaAndCalendarRenderHelpfulMessagesAndNavbarEntry() throws Exception {
    mvc
      .perform(get(URL).param("month", "2024-02").with(user(EMAIL)))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("No tenés planes para este día.")))
      .andExpect(content().string(containsString("Todavía no tenés planes.")))
      .andExpect(content().string(containsString("id=\"menu-calendar\"")))
      .andExpect(content().string(containsString("href=\"/plans/calendar\"")));
  }

  @Test
  void escapesPlanNamesAndSupportsMissingTimes() throws Exception {
    Plan plan = savePlan("<script>alert('calendar')</script>", viewer, "2024-02-29");
    plan.setEventTime(null);
    plans.save(plan);
    mvc
      .perform(get(URL).param("month", "2024-02").param("date", "2024-02-29").with(user(EMAIL)))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("&lt;script&gt;")))
      .andExpect(content().string(not(containsString(plan.getName()))));
  }

  @Test
  void invalidParametersRenderInsteadOfFailing() throws Exception {
    mvc
      .perform(get(URL).param("month", "invalid").param("date", "2024-02-30").with(user(EMAIL)))
      .andExpect(status().isOk())
      .andExpect(model().attribute("selectedDate", LocalDate.now()));
  }

  private User saveUser(String email) {
    User user = new User();
    user.setEmail(email);
    user.setPassword("password");
    user.setRole("USER");
    users.save(user);
    return users.findByEmail(email).orElseThrow();
  }

  private Plan savePlan(String name, User owner, String date) {
    Plan plan = new Plan();
    plan.setName(name);
    plan.setAdministrator(owner);
    plan.setEventDate(date == null ? null : LocalDate.parse(date));
    plan.setEventTime(LocalTime.of(10, 30));
    return plans.save(plan);
  }
}
