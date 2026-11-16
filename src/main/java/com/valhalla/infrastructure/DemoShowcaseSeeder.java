package com.valhalla.infrastructure;

import com.valhalla.domain.bookmark.PlanBookmarkService;
import com.valhalla.domain.follow.Follow;
import com.valhalla.domain.follow.FollowRepository;
import com.valhalla.domain.notification.NotificationService;
import com.valhalla.domain.place.Place;
import com.valhalla.domain.place.PlaceRepository;
import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanBudget;
import com.valhalla.domain.plan.PlanRepository;
import com.valhalla.domain.plan.PlanScheduleCalculator;
import com.valhalla.domain.plan.PlanService;
import com.valhalla.domain.plan.TransportMode;
import com.valhalla.domain.planplace.PlanPlace;
import com.valhalla.domain.rating.PlanRating;
import com.valhalla.domain.rating.PlanRatingRepository;
import com.valhalla.domain.user.User;
import com.valhalla.domain.user.UserService;
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
 * [DEMO] Cuenta de presentacion para la demo final: {@code demo@planit.demo} con historia propia.
 *
 * <p>Los seeders anteriores dejan fantasmas y estados raros, pero la persona que presenta no tiene
 * nada: ni planes, ni solicitudes, ni invitaciones, ni notificaciones. Este seeder le arma el
 * recorrido de punta a punta usando los mismos servicios que la app, para que lo que se vea en la
 * demo sea lo que produce la propia aplicacion.
 *
 * <p>Cuelga del mismo flag que el resto ({@code -Dseed.demoPlans=true}) y usa una sola guarda de
 * idempotencia: el shortCode {@code DEMO0001}. Un seed a medias se repara con {@code reset-db}.
 */
@Component
public class DemoShowcaseSeeder implements ApplicationListener<ContextRefreshedEvent> {

  private static final Logger LOGGER = Logger.getLogger(DemoShowcaseSeeder.class.getName());

  private static final String MARKER_SHORT_CODE = "DEMO0001";
  private static final String SHARED_SHORT_CODE = "DEMO0002";
  private static final String INVITE_SHORT_CODE = "DEMO0003";

  private static final String DEMO_EMAIL = "demo@planit.demo";
  private static final String DEMO_PASSWORD = "password";
  private static final String DEMO_FIRST_NAME = "Demo";
  private static final String DEMO_LAST_NAME = "Planit";

  private static final String LUCIA = "lucia.peralta@planit.demo";
  private static final String MATEO = "mateo.rios@planit.demo";
  private static final String SOFIA = "sofia.cabrera@planit.demo";
  private static final String TOMAS = "tomas.ledesma@planit.demo";

  private static final String USER_ROLE = "USER";
  private static final String PARTICIPANT_ROLE = "guest";
  private static final String EDITOR_ROLE = "editor";
  private static final String APP_CHANNEL = "app";
  private static final String DAY_END = "23:00";

  private static final List<String> WEEKEND_STOPS = List.of(
    "MALBA",
    "Plaza Serrano",
    "Parque Tres de Febrero",
    "Planetario Galileo Galilei",
    "La Viruta",
    // Cierra la noche: sin ella el itinerario termina antes de las 23:00 y no cruza de dia.
    "El Sanjuanino"
  );

  private final PublicPlanDataSeeder publicPlanSeeder;
  private final UserService userService;
  private final PlanRepository planRepository;
  private final PlaceRepository placeRepository;
  private final PlanService planService;
  private final PlanBookmarkService planBookmarkService;
  private final PlanRatingRepository planRatingRepository;
  private final FollowRepository followRepository;
  private final NotificationService notificationService;

  private boolean seeded;

  public DemoShowcaseSeeder(
    PublicPlanDataSeeder publicPlanSeeder,
    UserService userService,
    PlanRepository planRepository,
    PlaceRepository placeRepository,
    PlanService planService,
    PlanBookmarkService planBookmarkService,
    PlanRatingRepository planRatingRepository,
    FollowRepository followRepository,
    NotificationService notificationService
  ) {
    this.publicPlanSeeder = publicPlanSeeder;
    this.userService = userService;
    this.planRepository = planRepository;
    this.placeRepository = placeRepository;
    this.planService = planService;
    this.planBookmarkService = planBookmarkService;
    this.planRatingRepository = planRatingRepository;
    this.followRepository = followRepository;
    this.notificationService = notificationService;
  }

  @Override
  public synchronized void onApplicationEvent(ContextRefreshedEvent event) {
    if (seeded || !publicPlanSeeder.isEnabled()) {
      return;
    }
    seeded = true;
    // El orden de los listeners no esta garantizado: los fantasmas y el catalogo se piden a mano,
    // igual que en DemoInteractionSeeder, y los dos son idempotentes.
    publicPlanSeeder.ensureSeeded();
    if (!ghostsPresent() || planRepository.existsByShortCode(MARKER_SHORT_CODE)) {
      return;
    }
    seed();
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info(
        "Demo showcase seed loaded: " +
        DEMO_EMAIL +
        " with 2 own plans, 1 invitation, pending join request, banned user, " +
        "notifications, saves and ratings"
      );
    }
  }

  private void seed() {
    Map<String, User> ghosts = ghostsByEmail();
    User demo = ensureDemoUser();
    Map<String, Place> catalogue = placesByName();
    saveWeekendPlan(demo, catalogue);
    saveSharedPrivatePlan(demo, ghosts, catalogue);
    saveInvitationPlan(ghosts);
    seedSocialGraph(demo, ghosts);
    seedSavesAndRatings(demo);
  }

  private boolean ghostsPresent() {
    return userService.findOptionalByEmail(LUCIA).isPresent();
  }

  private Map<String, User> ghostsByEmail() {
    Map<String, User> ghosts = new LinkedHashMap<>();
    for (String email : List.of(LUCIA, MATEO, SOFIA, TOMAS)) {
      ghosts.put(email, userService.findOptionalByEmail(email).orElseThrow());
    }
    return ghosts;
  }

  /** La cuenta de demo: se crea si no existe; si ya esta, se reutiliza tal cual. */
  private User ensureDemoUser() {
    if (userService.findOptionalByEmail(DEMO_EMAIL).isEmpty()) {
      userService.create(DEMO_EMAIL, DEMO_PASSWORD, USER_ROLE, DEMO_FIRST_NAME, DEMO_LAST_NAME);
    }
    return userService.findOptionalByEmail(DEMO_EMAIL).orElseThrow();
  }

  private Map<String, Place> placesByName() {
    return placeRepository
      .findAll()
      .stream()
      .collect(
        Collectors.toMap(Place::getName, Function.identity(), (a, b) -> a, LinkedHashMap::new)
      );
  }

  /** Plan publico propio del demo: el recorrido que se muestra en el feed y en el itinerario. */
  private void saveWeekendPlan(User demo, Map<String, Place> catalogue) {
    Plan plan = newPlan(
      MARKER_SHORT_CODE,
      "Finde en Palermo",
      "Museos, parques y una noche de milonga. Itinerario largo, pasa de las 23:00.",
      6,
      "15:00",
      TransportMode.BUS,
      demo,
      true
    );
    plan.setBudget(
      new PlanBudget(BigDecimal.valueOf(8000), BigDecimal.valueOf(20000), "Entradas y comida")
    );
    addStops(plan, catalogue, WEEKEND_STOPS);
    // [SCH] El horario sale de la misma calculadora que usa la app, no de horas escritas a mano.
    PublicPlanDataSeeder.applyDerivedSchedule(plan);
    planRepository.save(plan);
  }

  /**
   * Plan privado del demo con roles, solicitud pendiente y un baneo. El orden es el de las reglas
   * de los servicios: primero los participantes (por entidad, como los fantasmas), despues el rol,
   * la solicitud (exige plan privado) y el baneo (exige que sea participante).
   */
  private void saveSharedPrivatePlan(
    User demo,
    Map<String, User> ghosts,
    Map<String, Place> catalogue
  ) {
    Plan plan = newPlan(
      SHARED_SHORT_CODE,
      "Asado en casa",
      "Todos traen algo para la parrilla. El que no trae nada, lava los platos.",
      12,
      "13:00",
      TransportMode.CAR,
      demo,
      false
    );
    plan.getParticipants().add(ghosts.get(LUCIA));
    plan.getParticipants().add(ghosts.get(MATEO));
    plan.getParticipants().add(ghosts.get(SOFIA));
    plan.setBudget(
      new PlanBudget(BigDecimal.valueOf(15000), BigDecimal.valueOf(30000), "Carne y bebidas")
    );
    addStops(plan, catalogue, List.of("Parque Tres de Febrero"));
    Plan saved = planRepository.save(plan);

    planService.changeParticipantRole(saved.getId(), LUCIA, EDITOR_ROLE, DEMO_EMAIL);
    planService.requestToJoin(saved.getId(), TOMAS);
    planService.banParticipant(saved.getId(), SOFIA, DEMO_EMAIL);
    // Las uniones se cargaron por entidad, sin pasar por el servicio: avisamos al dueno aparte.
    notificationService.notifyNewParticipant(
      ghosts.get(LUCIA),
      demo,
      saved.getId(),
      saved.getName()
    );
    notificationService.notifyNewParticipant(
      ghosts.get(MATEO),
      demo,
      saved.getId(),
      saved.getName()
    );
  }

  /** Plan privado de Lucia con una invitacion pendiente para el demo: se acepta en vivo. */
  private void saveInvitationPlan(Map<String, User> ghosts) {
    Plan plan = newPlan(
      INVITE_SHORT_CODE,
      "Noche de cine",
      "Ciclo de cine en el MALBA y despues cena. Vení si te copa.",
      20,
      "21:00",
      TransportMode.BUS,
      ghosts.get(LUCIA),
      false
    );
    Plan saved = planRepository.save(plan);
    planService.inviteParticipant(saved.getId(), DEMO_EMAIL, PARTICIPANT_ROLE, APP_CHANNEL, LUCIA);
  }

  /** Feed "Siguiendo" con contenido y seguidores que disparan la campanita. */
  private void seedSocialGraph(User demo, Map<String, User> ghosts) {
    for (String email : List.of(LUCIA, MATEO, SOFIA)) {
      followRepository.save(new Follow(demo, ghosts.get(email)));
    }
    for (String email : List.of(LUCIA, MATEO)) {
      followRepository.save(new Follow(ghosts.get(email), demo));
      notificationService.notifyNewFollower(ghosts.get(email), demo);
    }
  }

  /** Guardados disponibles y calificaciones propias, para que las vistas no esten vacias. */
  private void seedSavesAndRatings(User demo) {
    planBookmarkService.save(findPlan("GHOST004").getId(), DEMO_EMAIL);
    planBookmarkService.save(findPlan("GHOST006").getId(), DEMO_EMAIL);
    planRatingRepository.save(new PlanRating(findPlan("GHOST001"), demo, 4));
    planRatingRepository.save(new PlanRating(findPlan("GHOST003"), demo, 5));
  }

  private Plan findPlan(String shortCode) {
    return planRepository.findByShortCode(shortCode).orElseThrow();
  }

  private Plan newPlan(
    String shortCode,
    String name,
    String description,
    int daysFromNow,
    String hour,
    TransportMode transportMode,
    User owner,
    boolean isPublic
  ) {
    Plan plan = new Plan();
    plan.setName(name);
    plan.setDescription(description);
    plan.setEventDate(LocalDate.now().plusDays(daysFromNow));
    plan.setEventTime(LocalTime.parse(hour));
    plan.setDayEndTime(LocalTime.parse(DAY_END));
    plan.setTransportMode(transportMode);
    plan.setIsPublic(isPublic);
    plan.setShortCode(shortCode);
    plan.setAdministrator(owner);
    return plan;
  }

  /** Paradas del catalogo con su duracion por categoria; los nombres que no existan se saltean. */
  private void addStops(Plan plan, Map<String, Place> catalogue, List<String> placeNames) {
    int order = 1;
    for (String placeName : placeNames) {
      Place place = catalogue.get(placeName);
      if (place == null) {
        // Un nombre roto no puede pasar en silencio: la demo quedaria con menos paradas.
        if (LOGGER.isLoggable(Level.WARNING)) {
          LOGGER.warning("Demo showcase stop missing from catalogue, skipped: " + placeName);
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
