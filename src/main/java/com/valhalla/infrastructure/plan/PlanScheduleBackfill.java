package com.valhalla.infrastructure.plan;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.place.PlaceRepository;
import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanRepository;
import com.valhalla.domain.plan.PlanScheduleCalculator;
import com.valhalla.domain.plan.PlanScheduler;
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
 * <p>[SCH] La derivacion del horario no vive aca: la unica copia de la regla esta en
 * {@link PlanScheduler}, que tambien usan el alta y la edicion. Este backfill solo elige que planes
 * necesitan la pasada y persiste el resultado.
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
   * <p>[SCH] La derivacion vive en {@link PlanScheduler}: esta pasada solo decide que planes
   * entran y persiste el resultado.
   *
   * @return cuantos planes se recalcularon
   */
  private int backfillPlanSchedules() {
    int updated = 0;
    for (Plan plan : planRepository.findAll()) {
      if (!needsSchedule(plan)) {
        continue;
      }
      PlanScheduler.apply(plan);
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
}
