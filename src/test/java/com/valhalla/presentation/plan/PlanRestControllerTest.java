package com.valhalla.presentation.plan;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.place.PlaceCategory;
import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanService;
import com.valhalla.domain.planplace.PlanPlace;
import com.valhalla.presentation.plan.PlanRestController.PlanSummary;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * Unit tests of {@link PlanRestController}. The service is mocked, so what is under test is the
 * mapping of {@code GET /api/plans} and its two branches on {@code Principal}: the endpoint the
 * "Add to plan" modal of the places list calls before it can offer anything.
 */
public class PlanRestControllerTest {

  private static final String OWNER_EMAIL = "dueno@test.com";
  private static final Long PLAN_ID = 7L;

  private PlanRestController controller;
  private PlanService planServiceMock;

  @BeforeEach
  public void init() {
    planServiceMock = mock(PlanService.class);
    controller = new PlanRestController(planServiceMock);
  }

  @Test
  public void shouldReturnAnEmptyListWithoutAskingTheServiceWhenThereIsNoPrincipal() {
    List<PlanSummary> plans = controller.getUserPlans(null);

    assertThat(plans, is(empty()));
    verifyNoInteractions(planServiceMock);
  }

  @Test
  public void shouldMapEveryPlanOfTheAuthenticatedUser() {
    Plan plan = planWithOnePlace();
    when(planServiceMock.getPlansByUserEmail(OWNER_EMAIL)).thenReturn(List.of(plan));

    List<PlanSummary> plans = controller.getUserPlans(principal());

    assertThat(plans, hasSize(1));
    PlanSummary summary = plans.get(0);
    assertThat(summary.id(), is(PLAN_ID));
    assertThat(summary.name(), is("Viaje a Bariloche"));
    assertThat(summary.eventDate(), is("2026-12-31"));
    assertThat(summary.isPublic(), is(true));
    assertThat(summary.planPlaces(), hasSize(1));
    assertThat(summary.planPlaces().get(0).id(), is(11L));
    assertThat(summary.planPlaces().get(0).placeId(), is(4L));
    assertThat(summary.planPlaces().get(0).name(), is("MALBA"));
    assertThat(summary.planPlaces().get(0).category(), is("MUSEUM"));
    assertThat(summary.planPlaces().get(0).latitude(), is(-34.6098));
    assertThat(summary.planPlaces().get(0).longitude(), is(-58.3711));
    verify(planServiceMock).getPlansByUserEmail(OWNER_EMAIL);
  }

  @Test
  public void shouldLeaveTheEventDateNullWhenThePlanHasNone() {
    Plan plan = new Plan();
    plan.setId(PLAN_ID);
    plan.setName("Sin fecha");
    when(planServiceMock.getPlansByUserEmail(OWNER_EMAIL)).thenReturn(List.of(plan));

    List<PlanSummary> plans = controller.getUserPlans(principal());

    assertThat(plans, hasSize(1));
    assertThat(plans.get(0).eventDate(), is(nullValue()));
    assertThat(plans.get(0).planPlaces(), is(empty()));
  }

  // --- helpers ---

  private static Principal principal() {
    return new UsernamePasswordAuthenticationToken(OWNER_EMAIL, null);
  }

  private static Plan planWithOnePlace() {
    Place place = new Place();
    place.setId(4L);
    place.setName("MALBA");
    place.setCategory(PlaceCategory.MUSEUM);
    place.setLatitude(-34.6098);
    place.setLongitude(-58.3711);

    PlanPlace planPlace = new PlanPlace();
    planPlace.setId(11L);
    planPlace.setPlace(place);

    Plan plan = new Plan();
    plan.setId(PLAN_ID);
    plan.setName("Viaje a Bariloche");
    plan.setEventDate(LocalDate.of(2026, 12, 31));
    plan.setIsPublic(true);
    plan.getPlanPlaces().add(planPlace);
    return plan;
  }
}
