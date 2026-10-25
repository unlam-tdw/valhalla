package com.valhalla.infrastructure.plan;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.place.PlaceRepository;
import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanRepository;
import com.valhalla.domain.plan.PlanScheduleCalculator;
import com.valhalla.domain.planplace.PlanPlace;
import com.valhalla.infrastructure.PublicPlanDataSeeder;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * [SCH] Pone al dia la base que nacio antes que el planificador: completa la duracion tipica de
 * los lugares y las duraciones y horas de las paradas de los planes que ya existian.
 *
 * <p>Corre una sola vez por arranque, colgado del mismo evento que los seeders, y les pide el
 * sembrado base a mano ({@link PublicPlanDataSeeder#ensureSeeded()}) porque el orden de los
 * listeners no esta garantizado. Sobre una base vacia no hace nada; el sembrado del demo solo
 * ocurre con la llave puesta, y este backfill no la necesita para completar lo que ya exista.
 *
 * <p>[SCH] Descarta las horas viejas en vez de respetarlas (decision D-7): la base es contenido de
 * demo, y mezclar horas tipeadas a mano con las que calcula el planificador deja itinerarios
 * mixtos —una parada a las 19:00 porque alguien la movio, la siguiente encadenada desde ahi— que
 * se leen peor que un horario entero recalculado. Una vez que el form escribe el horario, el
 * backfill no vuelve a tocarlo.
 *
 * <p>Es idempotente por la guarda de la segunda pasada: solo entran los planes que tienen alguna
 * parada sin duracion. Despues de la primera corrida no queda ningun nulo, asi que la siguiente no
 * encuentra nada y no reescribe ni una fila.
 */
@Component
public class PlanScheduleBackfill implements ApplicationListener<ContextRefreshedEvent> {

  private static final Logger LOGGER = Logger.getLogger(PlanScheduleBackfill.class.getName());

  private final PublicPlanDataSeeder publicPlanDataSeeder;
  private final PlanRepository planRepository;
  private final PlaceRepository placeRepository;

  public PlanScheduleBackfill(
    PublicPlanDataSeeder publicPlanDataSeeder,
    PlanRepository planRepository,
    PlaceRepository placeRepository
  ) {
    this.publicPlanDataSeeder = publicPlanDataSeeder;
    this.planRepository = planRepository;
    this.placeRepository = placeRepository;
  }

  @Override
  public synchronized void onApplicationEvent(ContextRefreshedEvent event) {
    publicPlanDataSeeder.ensureSeeded();
    backfill();
  }

  /**
   * Completa las duraciones que falten y recalcula el horario de los planes que las necesitaban.
   *
   * @return cuantos planes se recalcularon
   */
  public synchronized int backfill() {
    int places = backfillPlaceDurations();
    int plans = backfillPlanSchedules();
    if ((places > 0 || plans > 0) && LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("Schedule backfill: " + places + " places and " + plans + " plans updated");
    }
    return plans;
  }

  /**
   * Primera pasada: el catalogo anterior al planificador no tiene duracion tipica, y sin ella la
   * cascada no tiene de donde sacar el segundo nivel.
   *
   * @return cuantos lugares se completaron
   */
  private int backfillPlaceDurations() {
    int updated = 0;
    for (Place place : placeRepository.findAll()) {
      if (place.getDefaultDurationMinutes() != null) {
        continue;
      }
      place.setDefaultDurationMinutes(
        PlanScheduleCalculator.defaultDurationMinutes(place.getCategory())
      );
      placeRepository.save(place);
      updated++;
    }
    return updated;
  }

  /**
   * Segunda pasada: planes con paradas sin duracion. Las horas se recalculan enteras, tambien las
   * de las paradas que ya tenian una.
   *
   * @return cuantos planes se recalcularon
   */
  private int backfillPlanSchedules() {
    int updated = 0;
    for (Plan plan : planRepository.findAll()) {
      if (!needsSchedule(plan)) {
        continue;
      }
      fillDurations(plan);
      applySchedule(plan);
      planRepository.save(plan);
      updated++;
    }
    return updated;
  }

  /** Guarda de idempotencia: sin paradas o sin fecha no hay itinerario que armar. */
  private boolean needsSchedule(Plan plan) {
    List<PlanPlace> stops = plan.getPlanPlaces();
    return (
      !stops.isEmpty() &&
      plan.getEventDate() != null &&
      stops.stream().anyMatch(stop -> stop.getDurationMinutes() == null)
    );
  }

  /** Completa solo las duraciones nulas: la del lugar si la tiene, si no la de la categoria. */
  private void fillDurations(Plan plan) {
    for (PlanPlace stop : plan.getPlanPlaces()) {
      if (stop.getDurationMinutes() != null) {
        continue;
      }
      Place place = stop.getPlace();
      stop.setDurationMinutes(
        PlanScheduleCalculator.effectiveDuration(
          null,
          place.getDefaultDurationMinutes(),
          place.getCategory()
        )
      );
    }
  }

  /**
   * Recalcula el horario del plan entero. Sin hora de inicio la calculadora devuelve una lista
   * vacia: en ese caso las duraciones quedan completas y las horas viejas intactas, en vez de
   * quedar a medio escribir.
   */
  private void applySchedule(Plan plan) {
    List<PlanPlace> stops = plan.getPlanPlaces();
    List<PlanScheduleCalculator.Slot> slots = PlanScheduleCalculator.schedule(
      plan.getEventDate(),
      plan.getEventTime(),
      plan.getDayEndTime(),
      plan.getTransportMode(),
      stops.stream().map(this::toStop).toList()
    );
    if (slots.size() != stops.size()) {
      return;
    }
    for (int index = 0; index < slots.size(); index++) {
      stops.get(index).setVisitDate(slots.get(index).date());
      stops.get(index).setVisitTime(slots.get(index).start());
    }
  }

  /** Una parada sin pines: el backfill no conoce horas fijadas por el usuario. */
  private PlanScheduleCalculator.Stop toStop(PlanPlace stop) {
    Place place = stop.getPlace();
    return new PlanScheduleCalculator.Stop(
      null,
      null,
      stop.getDurationMinutes(),
      place.getDefaultDurationMinutes(),
      place.getCategory(),
      place.getLatitude(),
      place.getLongitude(),
      stop.getTransportModeToNext()
    );
  }
}
