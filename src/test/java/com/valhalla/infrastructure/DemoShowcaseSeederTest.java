package com.valhalla.infrastructure;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.valhalla.domain.bookmark.PlanBookmarkService;
import com.valhalla.domain.follow.Follow;
import com.valhalla.domain.follow.FollowRepository;
import com.valhalla.domain.notification.NotificationService;
import com.valhalla.domain.place.Place;
import com.valhalla.domain.place.PlaceCategory;
import com.valhalla.domain.place.PlaceRepository;
import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanRepository;
import com.valhalla.domain.plan.PlanService;
import com.valhalla.domain.rating.PlanRating;
import com.valhalla.domain.rating.PlanRatingRepository;
import com.valhalla.domain.user.User;
import com.valhalla.domain.user.UserService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/**
 * Unit tests of {@link DemoShowcaseSeeder} against mocked ports.
 *
 * <p>Sin {@code seed.demoPlans} no toca nada. Con el marcador {@code DEMO0001} ya sembrado, la
 * corrida tambien se va. Con las guardas abiertas, el camino feliz verifica la cuenta demo, sus
 * dos planes propios, la invitacion, las relaciones sociales, los guardados y las calificaciones.
 */
public class DemoShowcaseSeederTest {

  private static final String DEMO_EMAIL = "demo@planit.demo";
  private static final String LUCIA_EMAIL = "lucia.peralta@planit.demo";
  private static final String MATEO_EMAIL = "mateo.rios@planit.demo";
  private static final String SOFIA_EMAIL = "sofia.cabrera@planit.demo";
  private static final String TOMAS_EMAIL = "tomas.ledesma@planit.demo";
  private static final String MARKER = "DEMO0001";
  private static final String SHARED = "DEMO0002";
  private static final String INVITE = "DEMO0003";

  private PublicPlanDataSeeder publicPlanSeederMock;
  private UserService userServiceMock;
  private PlanRepository planRepositoryMock;
  private PlaceRepository placeRepositoryMock;
  private PlanService planServiceMock;
  private PlanBookmarkService planBookmarkServiceMock;
  private PlanRatingRepository planRatingRepositoryMock;
  private FollowRepository followRepositoryMock;
  private NotificationService notificationServiceMock;
  private DemoShowcaseSeeder seeder;
  private List<Plan> savedPlans;

  @BeforeEach
  public void init() {
    this.publicPlanSeederMock = mock(PublicPlanDataSeeder.class);
    this.userServiceMock = mock(UserService.class);
    this.planRepositoryMock = mock(PlanRepository.class);
    this.placeRepositoryMock = mock(PlaceRepository.class);
    this.planServiceMock = mock(PlanService.class);
    this.planBookmarkServiceMock = mock(PlanBookmarkService.class);
    this.planRatingRepositoryMock = mock(PlanRatingRepository.class);
    this.followRepositoryMock = mock(FollowRepository.class);
    this.notificationServiceMock = mock(NotificationService.class);
    this.savedPlans = new ArrayList<>();
    this.seeder =
      new DemoShowcaseSeeder(
        this.publicPlanSeederMock,
        this.userServiceMock,
        this.planRepositoryMock,
        this.placeRepositoryMock,
        this.planServiceMock,
        this.planBookmarkServiceMock,
        this.planRatingRepositoryMock,
        this.followRepositoryMock,
        this.notificationServiceMock
      );
    // Spring Data devuelve la misma instancia con el id asignado: el mock hace lo mismo.
    when(this.planRepositoryMock.save(any(Plan.class)))
      .thenAnswer(invocation -> {
        Plan plan = invocation.getArgument(0, Plan.class);
        if (plan.getId() == null) {
          plan.setId(UUID.randomUUID());
        }
        this.savedPlans.add(plan);
        return plan;
      });
  }

  @Test
  public void sinLaLlaveDeDemoNoTocaNada() {
    // given: el flag apagado (el default)

    // when
    this.seeder.onApplicationEvent(null);

    // then
    verify(this.publicPlanSeederMock, never()).ensureSeeded();
    verifyNoInteractions(
      this.userServiceMock,
      this.planRepositoryMock,
      this.planServiceMock,
      this.planBookmarkServiceMock,
      this.planRatingRepositoryMock,
      this.followRepositoryMock,
      this.notificationServiceMock
    );
  }

  @Test
  public void conElMarcadorSembradoNoSiembraNada() {
    // given: el flag prendido y una base que ya paso por este seeder
    when(this.publicPlanSeederMock.isEnabled()).thenReturn(true);
    when(this.userServiceMock.findOptionalByEmail(LUCIA_EMAIL))
      .thenReturn(Optional.of(userWith(LUCIA_EMAIL)));
    when(this.planRepositoryMock.existsByShortCode(MARKER)).thenReturn(true);

    // when
    this.seeder.onApplicationEvent(null);

    // then
    verify(this.publicPlanSeederMock).ensureSeeded();
    verify(this.planRepositoryMock, never()).save(any(Plan.class));
    verifyNoInteractions(
      this.planServiceMock,
      this.planBookmarkServiceMock,
      this.planRatingRepositoryMock,
      this.followRepositoryMock,
      this.notificationServiceMock
    );
  }

  @Test
  public void sinFantasmasNoSiembraNada() {
    // given: el flag prendido pero el feed base no dejo fantasmas
    when(this.publicPlanSeederMock.isEnabled()).thenReturn(true);
    when(this.userServiceMock.findOptionalByEmail(LUCIA_EMAIL)).thenReturn(Optional.empty());

    // when
    this.seeder.onApplicationEvent(null);

    // then
    verifyNoInteractions(this.planServiceMock, this.planBookmarkServiceMock);
  }

  @Test
  public void creaLaCuentaDemoConSusDosPlanesPropios() {
    // given
    givenEnabledSeederWithGhosts();

    // when
    this.seeder.onApplicationEvent(null);

    // then: la cuenta se crea una vez y los tres planes demo se guardan
    verify(this.userServiceMock, times(1))
      .create(eq(DEMO_EMAIL), anyString(), eq("USER"), anyString(), anyString());
    assertThat(this.savedPlans, hasSize(3));

    Plan weekend = savedPlan(MARKER);
    assertThat(weekend.getIsPublic(), is(true));
    assertThat(weekend.getBudget().isRange(), is(true));
    assertThat(weekend.getPlanPlaces(), hasSize(6));
    assertThat(weekend.getDayEndTime(), is(java.time.LocalTime.of(23, 0)));

    Plan shared = savedPlan(SHARED);
    assertThat(shared.getIsPublic(), is(false));
    assertThat(shared.getParticipants(), hasSize(3));
  }

  @Test
  public void usaLosServiciosRealesParaRolesSolicitudesBaneosEInvitaciones() {
    // given
    givenEnabledSeederWithGhosts();

    // when
    this.seeder.onApplicationEvent(null);

    // then: cada accion pasa por el servicio, asi las notificaciones salen del flujo real
    Plan shared = savedPlan(SHARED);
    verify(this.planServiceMock, times(1))
      .changeParticipantRole(shared.getId(), LUCIA_EMAIL, "editor", DEMO_EMAIL);
    verify(this.planServiceMock, times(1)).requestToJoin(shared.getId(), TOMAS_EMAIL);
    verify(this.planServiceMock, times(1)).banParticipant(shared.getId(), SOFIA_EMAIL, DEMO_EMAIL);
    verify(this.planServiceMock, times(1))
      .inviteParticipant(savedPlan(INVITE).getId(), DEMO_EMAIL, "guest", "app", LUCIA_EMAIL);
    verify(this.notificationServiceMock, times(2))
      .notifyNewParticipant(any(User.class), any(User.class), any(UUID.class), anyString());
  }

  @Test
  public void laSolicitudPendienteSeHaceSobreUnPlanPrivadoYAntesDelBaneo() {
    // given
    givenEnabledSeederWithGhosts();

    // when
    this.seeder.onApplicationEvent(null);

    // then: requestToJoin exige plan privado; el baneo exige que Sofia ya sea participante
    Plan shared = savedPlan(SHARED);
    assertThat(shared.getIsPublic(), is(false));
    InOrder order = inOrder(this.planRepositoryMock, this.planServiceMock);
    order.verify(this.planRepositoryMock).save(shared);
    order.verify(this.planServiceMock).requestToJoin(shared.getId(), TOMAS_EMAIL);
    order.verify(this.planServiceMock).banParticipant(shared.getId(), SOFIA_EMAIL, DEMO_EMAIL);
  }

  @Test
  public void sembraGuardadosCalificacionesYRelacionesSociales() {
    // given
    givenEnabledSeederWithGhosts();

    // when
    this.seeder.onApplicationEvent(null);

    // then: dos guardados disponibles y dos calificaciones propias
    verify(this.planBookmarkServiceMock, times(2)).save(any(UUID.class), eq(DEMO_EMAIL));
    ArgumentCaptor<PlanRating> ratings = ArgumentCaptor.forClass(PlanRating.class);
    verify(this.planRatingRepositoryMock, times(2)).save(ratings.capture());
    assertThat(ratings.getAllValues().get(0).getScore(), is(4));
    assertThat(ratings.getAllValues().get(1).getScore(), is(5));

    // Demo sigue a tres fantasmas y dos fantasmas lo siguen a el (con su notificacion)
    ArgumentCaptor<Follow> follows = ArgumentCaptor.forClass(Follow.class);
    verify(this.followRepositoryMock, times(5)).save(follows.capture());
    verify(this.notificationServiceMock, times(2))
      .notifyNewFollower(any(User.class), any(User.class));
    assertThat(follows.getAllValues().get(0).getFollower().getEmail(), is(DEMO_EMAIL));
  }

  @Test
  public void elSegundoEventoNoVuelveASembrar() {
    // given
    givenEnabledSeederWithGhosts();

    // when: un refresh del contexto dispara el evento dos veces
    this.seeder.onApplicationEvent(null);
    this.seeder.onApplicationEvent(null);

    // then: el flag en memoria corta la segunda corrida
    verify(this.publicPlanSeederMock, times(1)).ensureSeeded();
    assertThat(this.savedPlans, hasSize(3));
  }

  // --- fixtures ---

  private void givenEnabledSeederWithGhosts() {
    when(this.publicPlanSeederMock.isEnabled()).thenReturn(true);
    when(this.userServiceMock.findOptionalByEmail(anyString()))
      .thenAnswer(invocation -> Optional.of(userWith(invocation.getArgument(0, String.class))));
    // La cuenta demo no existe al principio y existe despues de crearla.
    when(this.userServiceMock.findOptionalByEmail(DEMO_EMAIL))
      .thenReturn(Optional.empty(), Optional.of(userWith(DEMO_EMAIL)));
    when(this.planRepositoryMock.findByShortCode(anyString()))
      .thenAnswer(invocation -> {
        Plan plan = new Plan();
        plan.setId(UUID.randomUUID());
        plan.setShortCode(invocation.getArgument(0, String.class));
        plan.setIsPublic(true);
        return Optional.of(plan);
      });
    when(this.placeRepositoryMock.findAll())
      .thenReturn(
        placesNamed(
          "MALBA",
          "Plaza Serrano",
          "Parque Tres de Febrero",
          "Planetario Galileo Galilei",
          "La Viruta",
          "El Sanjuanino"
        )
      );
  }

  /** El plan demo guardado con ese shortCode, con el id que le asigno el repositorio mockeado. */
  private Plan savedPlan(String shortCode) {
    return this.savedPlans.stream()
      .filter(plan -> shortCode.equals(plan.getShortCode()))
      .findFirst()
      .orElseThrow();
  }

  private static List<Place> placesNamed(String... names) {
    List<Place> places = new ArrayList<>();
    for (String name : names) {
      Place place = new Place();
      place.setId(UUID.randomUUID());
      place.setName(name);
      // El horario deriva distancias y categorias: un lugar real siempre las trae.
      place.setCategory(PlaceCategory.CULTURE);
      place.setLatitude(-34.6037);
      place.setLongitude(-58.3816);
      places.add(place);
    }
    return places;
  }

  private User userWith(String email) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setEmail(email);
    return user;
  }
}
