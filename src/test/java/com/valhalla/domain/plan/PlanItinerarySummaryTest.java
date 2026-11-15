package com.valhalla.domain.plan;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.place.PlaceCategory;
import com.valhalla.domain.planplace.PlanPlace;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

/**
 * [SCH] Tests del resumen del itinerario: paradas, tiempo total (visitas mas traslados) y
 * kilometros. Todo sale de la misma calculadora que deriva el horario, asi que los numeros de
 * estos tests son los mismos que los del horario derivado para el mismo plan.
 */
public class PlanItinerarySummaryTest {

  private static final LocalDate PLAN_DATE = LocalDate.of(2026, 6, 13);
  private static final LocalTime DAY_START = LocalTime.of(10, 0);
  private static final LocalTime DAY_END = LocalTime.of(23, 0);
  private static final Double LATITUDE = -34.6;
  private static final Double LONGITUDE = -58.4;

  @Test
  public void sumaDuracionesYTrasladosDeTresParadas() {
    // given: tres museos con duracion propia, pegados (el traslado calculado cae al piso de 5)
    Plan plan = plan(TransportMode.WALK);
    stop(plan, place(PlaceCategory.MUSEUM, 90), null, null);
    stop(plan, place(PlaceCategory.MUSEUM, 60), null, null);
    stop(plan, place(PlaceCategory.MUSEUM, 45), null, null);

    // when
    PlanItinerarySummary summary = PlanItinerarySummary.of(plan);

    // then: 90 + 60 + 45 de visitas y 5 + 5 de traslados
    assertThat(summary.stopCount(), is(equalTo(3)));
    assertThat(summary.totalMinutes(), is(equalTo(205)));
    assertThat(summary.durationLabel(), is(equalTo("3 h 25 min")));
  }

  @Test
  public void laCascadaDeDuracionesEsLaMismaQueLaDelHorario() {
    // given: override de visita, duracion del lugar y default de categoria, en ese orden
    Plan plan = plan(TransportMode.WALK);
    stop(plan, place(PlaceCategory.MUSEUM, 90), 30, null);
    stop(plan, place(PlaceCategory.MUSEUM, 120), null, null);
    stop(plan, place(PlaceCategory.CAFE, null), null, null);

    // when
    PlanItinerarySummary summary = PlanItinerarySummary.of(plan);

    // then: 30 + 120 + 45 (default de CAFE) de visitas y 5 + 5 de traslados
    assertThat(summary.totalMinutes(), is(equalTo(205)));
  }

  @Test
  public void elTrasladoFijoMandaYSinCoordenadasNoHayKilometros() {
    // given: dos paradas sin coordenadas, con el tramo de la primera fijado a mano en 20
    Plan plan = plan(TransportMode.WALK);
    Place origin = place(PlaceCategory.MUSEUM, 60);
    origin.setLatitude(null);
    origin.setLongitude(null);
    Place destination = place(PlaceCategory.MUSEUM, 60);
    destination.setLatitude(null);
    destination.setLongitude(null);
    stop(plan, origin, null, 20);
    stop(plan, destination, null, null);

    // when
    PlanItinerarySummary summary = PlanItinerarySummary.of(plan);

    // then: 60 + 60 de visitas y los 20 minutos fijados, no el fallback de 15
    assertThat(summary.totalMinutes(), is(equalTo(140)));
    assertThat(summary.distanceKm(), is(nullValue()));
    assertThat(summary.headline(), is(equalTo("2 paradas · 2 h 20 min")));
  }

  @Test
  public void laDistanciaEsLaMismaCuentaConRodeoDelEstimador() {
    // given: dos paradas separadas 0,027 grados de latitud, el mismo par que usa el test del
    // estimador: 3 km en linea recta, 3,9 con rodeo y 50 minutos a pie
    Plan plan = plan(TransportMode.WALK);
    Place origin = place(PlaceCategory.MUSEUM, 60);
    origin.setLatitude(0.0);
    origin.setLongitude(0.0);
    Place destination = place(PlaceCategory.MUSEUM, 60);
    destination.setLatitude(0.027);
    destination.setLongitude(0.0);
    stop(plan, origin, null, null);
    stop(plan, destination, null, null);

    // when
    PlanItinerarySummary summary = PlanItinerarySummary.of(plan);

    // then: 60 + 60 de visitas y el tramo de 50 que deriva el horario
    assertThat(summary.totalMinutes(), is(equalTo(170)));
    assertThat(summary.durationLabel(), is(equalTo("2 h 50 min")));
    assertThat(summary.distanceKm(), is(closeTo(3.9028, 0.001)));
    assertThat(summary.headline(), is(equalTo("2 paradas · 2 h 50 min · 3,9 km")));
  }

  @Test
  public void unaParadaUnicaNoSumaTraslado() {
    // given: un cafe sin duracion de lugar: manda el default de su categoria
    Plan plan = plan(TransportMode.WALK);
    stop(plan, place(PlaceCategory.CAFE, null), null, null);

    // when
    PlanItinerarySummary summary = PlanItinerarySummary.of(plan);

    // then
    assertThat(summary.stopCount(), is(equalTo(1)));
    assertThat(summary.totalMinutes(), is(equalTo(45)));
    assertThat(summary.distanceKm(), is(nullValue()));
    assertThat(summary.headline(), is(equalTo("1 parada · 45 min")));
  }

  @Test
  public void sinParadasOSinPlanElResumenQuedaVacio() {
    assertThat(PlanItinerarySummary.of(null).stopCount(), is(equalTo(0)));
    assertThat(PlanItinerarySummary.of(null).totalMinutes(), is(equalTo(0)));
    assertThat(PlanItinerarySummary.of(null).distanceKm(), is(nullValue()));
    assertThat(
      PlanItinerarySummary.of(plan(TransportMode.WALK)).headline(),
      is(equalTo("0 paradas · 0 min"))
    );
  }

  @Test
  public void lasEtiquetasSeFormateanEnEspanol() {
    assertThat(new PlanItinerarySummary(5, 380, 12.36).durationLabel(), is(equalTo("6 h 20 min")));
    assertThat(new PlanItinerarySummary(1, 45, null).durationLabel(), is(equalTo("45 min")));
    assertThat(new PlanItinerarySummary(2, 120, null).durationLabel(), is(equalTo("2 h")));
    assertThat(new PlanItinerarySummary(2, 61, null).durationLabel(), is(equalTo("1 h 1 min")));
    assertThat(new PlanItinerarySummary(2, 0, 12.36).distanceLabel(), is(equalTo("12,4 km")));
    assertThat(new PlanItinerarySummary(2, 0, null).distanceLabel(), is(equalTo("")));
  }

  private static Plan plan(TransportMode mode) {
    Plan plan = new Plan();
    plan.setName("Itinerario");
    plan.setEventDate(PLAN_DATE);
    plan.setEventTime(DAY_START);
    plan.setDayEndTime(DAY_END);
    plan.setTransportMode(mode);
    return plan;
  }

  private static Place place(PlaceCategory category, Integer durationMinutes) {
    Place place = new Place();
    place.setName("Lugar " + category);
    place.setCategory(category);
    place.setDefaultDurationMinutes(durationMinutes);
    place.setLatitude(LATITUDE);
    place.setLongitude(LONGITUDE);
    return place;
  }

  private static void stop(
    Plan plan,
    Place place,
    Integer durationMinutes,
    Integer travelMinutesToNext
  ) {
    PlanPlace stop = new PlanPlace();
    stop.setPlace(place);
    stop.setDurationMinutes(durationMinutes);
    stop.setTravelMinutesToNext(travelMinutesToNext);
    stop.setSortOrder(plan.getPlanPlaces().size() + 1);
    plan.addPlanPlace(stop);
  }
}
