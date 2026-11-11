package com.valhalla.presentation.plan;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.place.PlaceCategory;
import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.TransportMode;
import com.valhalla.domain.planplace.PlanPlace;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link PrintSheet}: la hoja impresa agrupa por dia, numera globalmente y describe
 * cada parada con horario, categoria, direccion, duracion y el tramo de traslado hacia la siguiente
 * (docs/specs/27-PRINT.md, AC-05 y AC-06).
 */
public class PrintSheetTest {

  private static final LocalDate DAY_ONE = LocalDate.of(2026, 11, 12);
  private static final LocalDate DAY_TWO = LocalDate.of(2026, 11, 13);

  @Test
  public void groupsConsecutiveStopsByDayAndNumbersThemGlobally() {
    Plan plan = plan();
    plan.addPlanPlace(stop(place("Museo X", PlaceCategory.MUSEUM), DAY_ONE, "09:00", 90));
    plan.addPlanPlace(stop(place("Bar Y", PlaceCategory.BAR), DAY_ONE, "11:35", 60));
    plan.addPlanPlace(stop(place("Parque Z", PlaceCategory.PARK), DAY_TWO, "10:00", 60));

    PrintSheet sheet = PrintSheet.of(plan);

    assertThat(sheet.days(), hasSize(2));
    assertThat(sheet.days().get(0).label(), is(equalTo("Día 1 · 12/11")));
    assertThat(sheet.days().get(0).stops(), hasSize(2));
    assertThat(sheet.days().get(1).label(), is(equalTo("Día 2 · 13/11")));
    assertThat(sheet.days().get(1).stops(), hasSize(1));
    assertThat(sheet.days().get(0).stops().get(0).number(), is(equalTo(1)));
    assertThat(sheet.days().get(0).stops().get(1).number(), is(equalTo(2)));
    assertThat(sheet.days().get(1).stops().get(0).number(), is(equalTo(3)));
  }

  @Test
  public void aDateThatComesBackAfterAnotherDayStartsANewGroup() {
    Plan plan = plan();
    plan.addPlanPlace(stop(place("Uno", PlaceCategory.BAR), DAY_ONE, "09:00", 60));
    plan.addPlanPlace(stop(place("Dos", PlaceCategory.BAR), DAY_TWO, "10:00", 60));
    plan.addPlanPlace(stop(place("Tres", PlaceCategory.BAR), DAY_ONE, "11:00", 60));

    PrintSheet sheet = PrintSheet.of(plan);

    assertThat(sheet.days(), hasSize(3));
    assertThat(sheet.days().get(0).label(), is(equalTo("Día 1 · 12/11")));
    assertThat(sheet.days().get(1).label(), is(equalTo("Día 2 · 13/11")));
    assertThat(sheet.days().get(2).label(), is(equalTo("Día 3 · 12/11")));
  }

  @Test
  public void formatsTheScheduleAndTheDuration() {
    Plan plan = plan();
    plan.addPlanPlace(stop(place("Museo X", PlaceCategory.MUSEUM), DAY_ONE, "09:00", 90));
    plan.addPlanPlace(stop(place("Cafe Y", PlaceCategory.CAFE), DAY_ONE, "10:45", 45));
    plan.addPlanPlace(stop(place("Bar Z", PlaceCategory.BAR), DAY_ONE, "11:30", 120));
    plan.addPlanPlace(stop(place("Sin hora", PlaceCategory.OTHER), DAY_ONE, null, null));

    PrintSheet sheet = PrintSheet.of(plan);

    assertThat(sheet.days().get(0).stops().get(0).schedule(), is(equalTo("09:00 – 10:30")));
    assertThat(sheet.days().get(0).stops().get(0).duration(), is(equalTo("1h 30m")));
    assertThat(sheet.days().get(0).stops().get(1).duration(), is(equalTo("45m")));
    assertThat(sheet.days().get(0).stops().get(2).duration(), is(equalTo("2h")));
    assertThat(sheet.days().get(0).stops().get(3).schedule(), is(equalTo("Horario por definir")));
  }

  @Test
  public void translatesTheCategoryToSpanish() {
    Plan plan = plan();
    plan.addPlanPlace(stop(place("Uno", PlaceCategory.RESTAURANT), DAY_ONE, "09:00", 60));
    plan.addPlanPlace(stop(place("Dos", PlaceCategory.NIGHTLIFE), DAY_ONE, "10:00", 60));
    plan.addPlanPlace(stop(place("Tres", PlaceCategory.CAFE), DAY_ONE, "11:00", 60));

    PrintSheet sheet = PrintSheet.of(plan);

    assertThat(sheet.days().get(0).stops().get(0).category(), is(equalTo("Restaurante")));
    assertThat(sheet.days().get(0).stops().get(1).category(), is(equalTo("Vida nocturna")));
    assertThat(sheet.days().get(0).stops().get(2).category(), is(equalTo("Café")));
    assertThat(
      PrintSheet.of(planWithCategory(PlaceCategory.SPORT)).days().get(0).stops().get(0).category(),
      is(equalTo("Deporte"))
    );
  }

  private static Plan planWithCategory(PlaceCategory category) {
    Plan plan = plan();
    plan.addPlanPlace(stop(place("Uno", category), DAY_ONE, "09:00", 60));
    return plan;
  }

  @Test
  public void theLegUsesThePinnedMinutesAndTheStopMode() {
    Plan plan = plan();
    PlanPlace first = stop(place("Uno", PlaceCategory.BAR), DAY_ONE, "09:00", 60);
    first.setTravelMinutesToNext(25);
    first.setTransportModeToNext(TransportMode.BUS);
    PlanPlace second = stop(place("Dos", PlaceCategory.BAR), DAY_ONE, "10:25", 60);
    second.setTravelMinutesToNext(10);
    second.setTransportModeToNext(TransportMode.CAR);
    plan.addPlanPlace(first);
    plan.addPlanPlace(second);

    PrintSheet sheet = PrintSheet.of(plan);

    assertThat(sheet.days().get(0).stops().get(0).leg(), is(equalTo("25 min en colectivo")));
    assertThat(sheet.days().get(0).stops().get(1).leg(), is(nullValue()));
  }

  @Test
  public void withoutPinnedMinutesItEstimatesWithDistanceAndPlanMode() {
    Plan plan = plan();
    plan.setTransportMode(TransportMode.BIKE);
    PlanPlace first = stop(place("Uno", PlaceCategory.BAR), DAY_ONE, "09:00", 60);
    PlanPlace second = stop(place("Dos", PlaceCategory.BAR), DAY_ONE, "10:10", 60);
    plan.addPlanPlace(first);
    plan.addPlanPlace(second);

    PrintSheet sheet = PrintSheet.of(plan);

    // Las dos paradas comparten coordenadas: distancia 0, piso de 5 minutos del planificador.
    assertThat(sheet.days().get(0).stops().get(0).leg(), is(equalTo("5 min en bici")));
  }

  @Test
  public void withoutAnyModeTheLegWalks() {
    Plan plan = plan();
    plan.setTransportMode(null);
    plan.addPlanPlace(stop(place("Uno", PlaceCategory.BAR), DAY_ONE, "09:00", 60));
    plan.addPlanPlace(stop(place("Dos", PlaceCategory.BAR), DAY_ONE, "10:05", 60));

    PrintSheet sheet = PrintSheet.of(plan);

    assertThat(sheet.days().get(0).stops().get(0).leg(), is(equalTo("5 min a pie")));
  }

  @Test
  public void withoutCoordinatesItFallsBackToThePlannerDefault() {
    Plan plan = plan();
    PlanPlace first = stop(placeWithoutCoordinates("Uno"), DAY_ONE, "09:00", 60);
    PlanPlace second = stop(placeWithoutCoordinates("Dos"), DAY_ONE, "10:15", 60);
    plan.addPlanPlace(first);
    plan.addPlanPlace(second);

    PrintSheet sheet = PrintSheet.of(plan);

    assertThat(sheet.days().get(0).stops().get(0).leg(), is(equalTo("15 min a pie")));
  }

  @Test
  public void theLastStopOfADayNeverCarriesALeg() {
    Plan plan = plan();
    plan.addPlanPlace(stop(place("Uno", PlaceCategory.BAR), DAY_ONE, "09:00", 60));
    plan.addPlanPlace(stop(place("Dos", PlaceCategory.BAR), DAY_TWO, "10:00", 60));

    PrintSheet sheet = PrintSheet.of(plan);

    assertThat(sheet.days().get(0).stops().get(0).leg(), is(nullValue()));
    assertThat(sheet.days().get(1).stops().get(0).leg(), is(nullValue()));
  }

  @Test
  public void aPlanWithoutStopsHasNoDays() {
    PrintSheet sheet = PrintSheet.of(plan());

    assertThat(sheet.days(), hasSize(0));
  }

  private static Plan plan() {
    Plan plan = new Plan();
    plan.setName("Plan impreso");
    plan.setEventDate(DAY_ONE);
    plan.setEventTime(LocalTime.of(9, 0));
    plan.setTransportMode(TransportMode.WALK);
    return plan;
  }

  private static PlanPlace stop(Place place, LocalDate date, String time, Integer durationMinutes) {
    PlanPlace entry = new PlanPlace();
    entry.setPlace(place);
    entry.setVisitDate(date);
    entry.setVisitTime(time == null ? null : LocalTime.parse(time));
    entry.setDurationMinutes(durationMinutes);
    entry.setSortOrder(1);
    return entry;
  }

  private static Place place(String name, PlaceCategory category) {
    Place place = placeWithoutCoordinates(name);
    place.setCategory(category);
    place.setLatitude(-34.6);
    place.setLongitude(-58.4);
    return place;
  }

  private static Place placeWithoutCoordinates(String name) {
    Place place = new Place();
    place.setName(name);
    place.setCategory(PlaceCategory.OTHER);
    place.setAddress("Calle 1");
    return place;
  }
}
