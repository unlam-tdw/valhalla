package com.valhalla.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanInvitation;
import com.valhalla.domain.plan.PlanRepository;
import com.valhalla.domain.plan.PlanService;
import com.valhalla.domain.user.User;
import com.valhalla.domain.user.UserRepository;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@WebIntegrationTest
@Transactional
public class PlanBanSecurityTest {

  private static final String OWNER = "owner.ban@test.com";
  private static final String EDITOR = "editor.ban@test.com";
  private static final String MEMBER = "member.ban@test.com";
  private static final String PLAN_NAME = "Plan bloqueado";

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private PlanRepository plans;

  @Autowired
  private UserRepository users;

  @Autowired
  private PlanService service;

  @Autowired
  private EntityManager entityManager;

  private MockMvc mvc;
  private UUID planId;

  @BeforeEach
  public void setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    User owner = createUser(OWNER);
    User editor = createUser(EDITOR);
    User member = createUser(MEMBER);
    Plan plan = new Plan();
    plan.setName(PLAN_NAME);
    plan.setDescription("Contenido que no debe exponerse");
    plan.setShortCode("BAN12345");
    plan.setIsPublic(true);
    plan.setAdministrator(owner);
    plan.getParticipants().addAll(List.of(editor, member));
    plan.getEditors().add(editor);
    planId = plans.save(plan).getId();
  }

  @Test
  public void editorCannotRemoveBanDemoteOwnerOrDeletePlan() throws Exception {
    for (String action : List.of("remove", "ban", "role")) {
      mvc
        .perform(
          post("/plans/{id}/participants/" + action, planId)
            .with(user(EDITOR))
            .with(csrf())
            .param("participantEmail", OWNER)
            .param("role", "guest")
        )
        .andExpect(status().is3xxRedirection());
    }
    mvc
      .perform(post("/plans/{id}/delete", planId).with(user(EDITOR)).with(csrf()))
      .andExpect(status().is3xxRedirection());
    entityManager.flush();
    entityManager.clear();
    Plan plan = plans.findById(planId).orElseThrow();
    assertThat(plan.isAdministrator(OWNER), is(true));
    assertThat(plan.isBanned(OWNER), is(false));
    assertThat(plan.isEditor(EDITOR), is(true));
  }

  @Test
  public void banBlocksDetailPublicViewItineraryJoinAndClone() throws Exception {
    service.banParticipant(planId, MEMBER, OWNER);
    entityManager.flush();
    entityManager.clear();
    for (boolean publicPlan : List.of(true, false)) {
      Plan plan = plans.findById(planId).orElseThrow();
      plan.setIsPublic(publicPlan);
      plans.save(plan);
      for (String path : publicPlan
        ? List.of("/plans/" + planId, "/plans/" + planId + "/public")
        : List.of("/plans/" + planId)) {
        mvc
          .perform(get(path).with(user(MEMBER)))
          .andExpect(status().isForbidden())
          .andExpect(content().string(containsString("Fuiste baneado")))
          .andExpect(content().string(not(containsString(plan.getDescription()))));
      }
      if (!publicPlan) {
        mvc
          .perform(get("/plans/{id}/public", planId).with(user(MEMBER)))
          .andExpect(status().is3xxRedirection());
      }
      mvc
        .perform(get("/api/plans/{id}/places", planId).with(user(MEMBER)))
        .andExpect(status().isNotFound());
      mvc
        .perform(post("/plans/join").with(user(MEMBER)).with(csrf()).param("shortCode", "ban12345"))
        .andExpect(status().isForbidden());
      mvc
        .perform(post("/plans/{id}/clone", planId).with(user(MEMBER)).with(csrf()))
        .andExpect(status().isForbidden());
      assertThat(plan.isParticipant(MEMBER), is(false));
    }
  }

  @Test
  public void bannedPlanShowsNoticeWithoutDetailAndDisappearsFromFeeds() throws Exception {
    service.banParticipant(planId, MEMBER, OWNER);
    entityManager.flush();
    entityManager.clear();
    String html = mvc
      .perform(get("/plans").with(user(MEMBER)))
      .andExpect(status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString(StandardCharsets.UTF_8);
    assertThat(html, containsString(PLAN_NAME));
    assertThat(html, containsString("Fuiste baneado"));
    assertThat(html, not(containsString("/plans/" + planId)));
    for (String path : List.of("/for-you", "/api/plans/public")) {
      mvc
        .perform(get(path).with(user(MEMBER)))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString(PLAN_NAME))));
    }
    mvc.perform(get("/plans/{id}/public", planId)).andExpect(status().isOk());
    mvc.perform(get("/plans/{id}", planId).with(user(OWNER))).andExpect(status().isOk());
  }

  @Test
  public void banRejectsPendingInvitationAcceptance() throws Exception {
    User member = users.findByEmail(MEMBER).orElseThrow();
    service.banParticipant(planId, MEMBER, OWNER);
    Plan plan = plans.findById(planId).orElseThrow();
    PlanInvitation invitation = new PlanInvitation(plan, member, plan.getAdministrator(), "editor");
    plan.getInvitations().add(invitation);
    Plan saved = plans.save(plan);
    entityManager.flush();
    Long invitationId = saved.getInvitations().getFirst().getId();
    mvc
      .perform(
        post("/plans/{id}/invitations/{invitationId}/respond", planId, invitationId)
          .with(user(MEMBER))
          .with(csrf())
          .param("accept", "true")
      )
      .andExpect(status().isForbidden());
    assertThat(plan.isParticipant(MEMBER), is(false));
    assertThat(plan.isEditor(MEMBER), is(false));
  }

  private User createUser(String email) {
    User user = new User();
    user.setEmail(email);
    user.setPassword("password123");
    user.setRole("USER");
    users.save(user);
    return user;
  }
}
