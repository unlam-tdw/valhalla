package com.valhalla.infrastructure;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.place.PlaceRepository;
import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanBudget;
import com.valhalla.domain.plan.PlanRepository;
import com.valhalla.domain.plan.PlanScheduleCalculator;
import com.valhalla.domain.plan.PlanService;
import com.valhalla.domain.plan.TransportMode;
import com.valhalla.domain.planplace.PlanPlace;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * [DEMO] Un plan publicado por la propia plataforma: la agenda que PlanIt recomienda.
 *
 * <p>Es el mismo tipo de plan que da de alta un admin desde {@code /admin/plans/new}, creado por el
 * camino real ({@link PlanService#createAdminPlan}), asi que el dueño es el usuario de sistema
 * {@code app@planit.com} y el plan aparece primero en los listados, como cualquier plan de PlanIt
 * ({@code PlanDiscovery.planItFirst}).
 *
 * <p>Va en su propio seeder y no dentro de {@link DemoShowcaseSeeder} por la guarda de idempotencia:
 * aquel sale apenas existe {@code DEMO0001}, y entonces una base ya sembrada nunca recibiria este
 * plan. Aca el marcador es "ya hay un plan a nombre de la app".
 */
@Component
public class PlanItShowcaseSeeder implements ApplicationListener<ContextRefreshedEvent> {

  private static final Logger LOGGER = Logger.getLogger(PlanItShowcaseSeeder.class.getName());

  /** Mismo dueño que asigna el alta de admin; lo crea el servicio si no existe. */
  private static final String APP_USER_EMAIL = "app@planit.com";

  private static final String PLAN_NAME = "Agenda PlanIt: clásicos porteños";
  private static final String PLAN_DESCRIPTION =
    "La agenda que armamos desde PlanIt: café con historia, una librería que era un teatro, " +
    "una vuelta por el centro y una parrilla para cerrar la tarde.";
  private static final int DAYS_FROM_NOW = 10;
  private static final String START_HOUR = "11:00";
  private static final String DAY_END = "23:00";

  private static final List<String> STOPS = List.of(
    "Cafe Tortoni",
    "El Ateneo Grand Splendid",
    "Obelisco de Buenos Aires",
    "Parrilla Don Julio"
  );

  private final PublicPlanDataSeeder publicPlanSeeder;
  private final PlanService planService;
  private final PlanRepository planRepository;
  private final PlaceRepository placeRepository;

  private boolean seeded;

  public PlanItShowcaseSeeder(
    PublicPlanDataSeeder publicPlanSeeder,
    PlanService planService,
    PlanRepository planRepository,
    PlaceRepository placeRepository
  ) {
    this.publicPlanSeeder = publicPlanSeeder;
    this.planService = planService;
    this.planRepository = planRepository;
    this.placeRepository = placeRepository;
  }

  @Override
  public synchronized void onApplicationEvent(ContextRefreshedEvent event) {
    if (seeded || !publicPlanSeeder.isEnabled()) {
      return;
    }
    seeded = true;
    // El orden de los listeners no esta garantizado: el catalogo de lugares se pide a mano.
    publicPlanSeeder.ensureSeeded();
    if (appPlanPresent()) {
      return;
    }
    seed();
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info(
        "PlanIt showcase seed loaded: 1 public plan owned by the platform (" + APP_USER_EMAIL + ")"
      );
    }
  }

  /** Marcador de idempotencia: la plataforma ya tiene un plan publicado. */
  private boolean appPlanPresent() {
    return planRepository
      .findAll()
      .stream()
      .anyMatch(plan ->
        plan.getAdministrator() != null &&
        APP_USER_EMAIL.equalsIgnoreCase(plan.getAdministrator().getEmail())
      );
  }

  private void seed() {
    Map<String, Place> catalogue = placesByName();
    Plan plan = new Plan();
    plan.setName(PLAN_NAME);
    plan.setDescription(PLAN_DESCRIPTION);
    plan.setEventDate(LocalDate.now().plusDays(DAYS_FROM_NOW));
    plan.setEventTime(LocalTime.parse(START_HOUR));
    plan.setDayEndTime(LocalTime.parse(DAY_END));
    plan.setTransportMode(TransportMode.BUS);
    plan.setIsPublic(true);
    plan.setBudget(
      new PlanBudget(BigDecimal.valueOf(10000), BigDecimal.valueOf(25000), "Café, entradas y cena")
    );
    addStops(plan, catalogue);
    // El alta de admin asigna dueño, shortCode, horario derivado y estado activo.
    planService.createAdminPlan(plan);
  }

  private Map<String, Place> placesByName() {
    return placeRepository
      .findAll()
      .stream()
      .collect(
        Collectors.toMap(Place::getName, Function.identity(), (a, b) -> a, LinkedHashMap::new)
      );
  }

  /** Paradas del catalogo con su duracion por categoria; los nombres que no existan se saltean. */
  private void addStops(Plan plan, Map<String, Place> catalogue) {
    int order = 1;
    for (String placeName : STOPS) {
      Place place = catalogue.get(placeName);
      if (place == null) {
        // Un nombre roto no puede pasar en silencio: la agenda quedaria incompleta.
        if (LOGGER.isLoggable(Level.WARNING)) {
          LOGGER.warning("PlanIt showcase stop missing from catalogue, skipped: " + placeName);
        }
        continue;
      }
      PlanPlace stop = new PlanPlace();
      stop.setPlace(place);
      stop.setSortOrder(order);
      stop.setDurationMinutes(PlanScheduleCalculator.defaultDurationMinutes(place.getCategory()));
      plan.addPlanPlace(stop);
      order++;
    }
  }
}
