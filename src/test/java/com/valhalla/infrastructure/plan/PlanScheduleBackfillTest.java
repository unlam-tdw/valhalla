package com.valhalla.infrastructure.plan;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.place.PlaceCategory;
import com.valhalla.domain.place.PlaceRepository;
import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanRepository;
import com.valhalla.domain.plan.TransportMode;
import com.valhalla.domain.planplace.PlanPlace;
import com.valhalla.domain.user.User;
import com.valhalla.domain.user.UserRepository;
import com.valhalla.integration.JpaIntegrationTest;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * [SCH] Tests de integracion del backfill de horarios: un plan viejo, con paradas sin duracion ni
 * horas, queda con el itinerario armado como si se hubiera creado con el planificador.
 *
 * <p>Las dos paradas comparten coordenadas a proposito: el traslado cae al piso de cinco minutos y
 * el horario esperado no depende de ninguna ciudad. El contexto HSQLDB se comparte entre suites, asi
 * que cada test verifica su propio plan y limpia lo que creo.
 */
@JpaIntegrationTest
public class PlanScheduleBackfillTest {

  private static final LocalDate EVENT_DATE = LocalDate.of(2026, 6, 13);
  private static final LocalTime EVENT_TIME = LocalTime.of(10, 0);
  private static final LocalTime DAY_END = LocalTime.of(23, 0);
  private static final LocalTime SECOND_STOP_START = LocalTime.of(11, 35);
  private static final int MUSEUM_DURATION_MINUTES = 90;
  private static final double LATITUDE = -34.6;
  private static final double LONGITUDE = -58.4;
  private static final String OWNER_EMAIL = "backfill@test.com";

  @Autowired
  private PlanScheduleBackfill backfill;

  @Autowired
  private PlanRepository planRepository;

  @Autowired
  private PlaceRepository placeRepository;

  @Autowired
  private UserRepository userRepository;

  private UUID planId;
  private UUID ownerId;
  private UUID firstPlaceId;
  private UUID secondPlaceId;

  @BeforeEach
  public void setUp() {
    User owner = new User();
    owner.setEmail(OWNER_EMAIL);
    this.userRepository.save(owner);
    this.ownerId = this.userRepository.findByEmail(OWNER_EMAIL).orElseThrow().getId();

    Place first = saveMuseum("Museo Backfill Uno");
    Place second = saveMuseum("Museo Backfill Dos");
    this.firstPlaceId = first.getId();
    this.secondPlaceId = second.getId();

    Plan plan = new Plan();
    plan.setName("Dos museos sin horario");
    plan.setEventDate(EVENT_DATE);
    plan.setEventTime(EVENT_TIME);
    plan.setDayEndTime(DAY_END);
    plan.setTransportMode(TransportMode.WALK);
    plan.setAdministrator(this.userRepository.findById(this.ownerId).orElseThrow());
    plan.addPlanPlace(stop(first, 1));
    plan.addPlanPlace(stop(second, 2));
    this.planId = this.planRepository.save(plan).getId();
  }

  @AfterEach
  public void cleanUp() {
    this.planRepository.deleteById(this.planId);
    this.placeRepository.deleteById(this.firstPlaceId);
    this.placeRepository.deleteById(this.secondPlaceId);
    this.userRepository.deleteById(this.ownerId);
  }

  @Test
  public void I10_completaDuracionesYDerivaElHorarioDeLasParadas() {
    // when
    int recalculados = this.backfill.backfill();

    // then: el plan propio entra en la cuenta (el contexto es compartido, no se afirma el total)
    assertThat(recalculados, is(greaterThanOrEqualTo(1)));

    // primera pasada: el lugar nace sin duracion y queda con la de su categoria
    Place place = this.placeRepository.findById(this.firstPlaceId).orElseThrow();
    assertThat(place.getDefaultDurationMinutes(), is(equalTo(MUSEUM_DURATION_MINUTES)));

    // segunda pasada: duraciones completas y horario derivado desde la hora del plan
    Plan plan = this.planRepository.findById(this.planId).orElseThrow();
    assertThat(plan.getPlanPlaces(), hasSize(2));
    PlanPlace first = plan.getPlanPlaces().get(0);
    PlanPlace second = plan.getPlanPlaces().get(1);
    assertThat(first.getDurationMinutes(), is(equalTo(MUSEUM_DURATION_MINUTES)));
    assertThat(second.getDurationMinutes(), is(equalTo(MUSEUM_DURATION_MINUTES)));
    assertThat(first.getVisitDate(), is(equalTo(EVENT_DATE)));
    assertThat(first.getVisitTime(), is(equalTo(EVENT_TIME)));
    // 90 minutos de visita + 5 de traslado entre dos paradas pegadas
    assertThat(second.getVisitDate(), is(equalTo(EVENT_DATE)));
    assertThat(second.getVisitTime(), is(equalTo(SECOND_STOP_START)));
  }

  @Test
  public void I11_laSegundaCorridaNoEncuentraNadaYNoMueveLasHoras() {
    // given: la primera corrida ya dejo todo completo
    this.backfill.backfill();
    Plan seeded = this.planRepository.findById(this.planId).orElseThrow();
    List<LocalDate> dates = seeded.getPlanPlaces().stream().map(PlanPlace::getVisitDate).toList();
    List<LocalTime> times = seeded.getPlanPlaces().stream().map(PlanPlace::getVisitTime).toList();

    // when
    int recalculados = this.backfill.backfill();

    // then: la guarda de idempotencia manda, no hay ningun plan con duraciones nulas
    assertThat(recalculados, is(equalTo(0)));
    Plan again = this.planRepository.findById(this.planId).orElseThrow();
    assertThat(
      again.getPlanPlaces().stream().map(PlanPlace::getVisitDate).toList(),
      is(equalTo(dates))
    );
    assertThat(
      again.getPlanPlaces().stream().map(PlanPlace::getVisitTime).toList(),
      is(equalTo(times))
    );
  }

  @Test
  public void laDuracionQueYaTeniaLaParadaNoSePisa() {
    // given: una parada con override propio y otra sin duracion
    Plan plan = this.planRepository.findById(this.planId).orElseThrow();
    plan.getPlanPlaces().get(0).setDurationMinutes(45);
    this.planRepository.save(plan);

    // when
    this.backfill.backfill();

    // then: el backfill solo completa los nulos
    Plan updated = this.planRepository.findById(this.planId).orElseThrow();
    assertThat(updated.getPlanPlaces().get(0).getDurationMinutes(), is(equalTo(45)));
    assertThat(
      updated.getPlanPlaces().get(1).getDurationMinutes(),
      is(equalTo(MUSEUM_DURATION_MINUTES))
    );
  }

  @Test
  public void sinHoraDeInicioCompletaLasDuracionesYNoInventaHoras() {
    // given: un plan viejo al que le falta la hora de arranque
    Plan plan = this.planRepository.findById(this.planId).orElseThrow();
    plan.setEventTime(null);
    this.planRepository.save(plan);

    // when
    this.backfill.backfill();

    // then: las duraciones quedan completas, las horas no se tocan
    Plan updated = this.planRepository.findById(this.planId).orElseThrow();
    for (PlanPlace stop : updated.getPlanPlaces()) {
      assertThat(stop.getDurationMinutes(), is(equalTo(MUSEUM_DURATION_MINUTES)));
      assertThat(stop.getVisitDate(), is(nullValue()));
      assertThat(stop.getVisitTime(), is(nullValue()));
    }
  }

  private Place saveMuseum(String name) {
    Place place = new Place();
    place.setName(name);
    place.setCategory(PlaceCategory.MUSEUM);
    place.setLatitude(LATITUDE);
    place.setLongitude(LONGITUDE);
    this.placeRepository.save(place);
    return place;
  }

  private static PlanPlace stop(Place place, int order) {
    PlanPlace stop = new PlanPlace();
    stop.setPlace(place);
    stop.setSortOrder(order);
    return stop;
  }
}
