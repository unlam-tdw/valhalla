package com.valhalla.infrastructure;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.place.PlaceCategory;
import com.valhalla.domain.place.PlaceRepository;
import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanRepository;
import com.valhalla.domain.plan.PlanService;
import com.valhalla.domain.planplace.PlanPlace;
import com.valhalla.domain.user.User;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests of {@link PlanItShowcaseSeeder} against mocked ports.
 *
 * <p>Sin {@code seed.demoPlans} no toca nada. Con un plan a nombre de la plataforma ya sembrado, la
 * corrida tambien se va. Con las guardas abiertas, el camino feliz publica la agenda por el mismo
 * alta que usa el ABM de admin.
 */
public class PlanItShowcaseSeederTest {

  private static final String APP_EMAIL = "app@planit.com";
  private static final String PLAN_NAME = "Agenda PlanIt: clásicos porteños";

  private PublicPlanDataSeeder publicPlanSeederMock;
  private PlanService planServiceMock;
  private PlanRepository planRepositoryMock;
  private PlaceRepository placeRepositoryMock;
  private PlanItShowcaseSeeder seeder;

  @BeforeEach
  public void init() {
    this.publicPlanSeederMock = mock(PublicPlanDataSeeder.class);
    this.planServiceMock = mock(PlanService.class);
    this.planRepositoryMock = mock(PlanRepository.class);
    this.placeRepositoryMock = mock(PlaceRepository.class);
    this.seeder =
      new PlanItShowcaseSeeder(
        this.publicPlanSeederMock,
        this.planServiceMock,
        this.planRepositoryMock,
        this.placeRepositoryMock
      );
    when(this.planRepositoryMock.findAll()).thenReturn(new ArrayList<>());
  }

  @Test
  public void sinLaLlaveDeDemoNoTocaNada() {
    // given: el flag apagado (el default)

    // when
    this.seeder.onApplicationEvent(null);

    // then: solo se consulto la llave; no se pidio catalogo ni se publico nada
    verify(this.publicPlanSeederMock, never()).ensureSeeded();
    verify(this.planServiceMock, never()).createAdminPlan(any(Plan.class));
    verifyNoInteractions(this.placeRepositoryMock);
  }

  @Test
  public void conLaAgendaYaPublicadaNoSiembraNada() {
    // given: el flag prendido y la plataforma ya tiene su plan
    when(this.publicPlanSeederMock.isEnabled()).thenReturn(true);
    when(this.planRepositoryMock.findAll()).thenReturn(List.of(planOwnedBy(APP_EMAIL)));

    // when
    this.seeder.onApplicationEvent(null);

    // then
    verify(this.publicPlanSeederMock).ensureSeeded();
    verify(this.planServiceMock, never()).createAdminPlan(any(Plan.class));
  }

  @Test
  public void publicaLaAgendaConElAltaDeAdmin() {
    // given
    givenEnabledSeeder();

    // when
    this.seeder.onApplicationEvent(null);

    // then: un solo plan, por el camino real del ABM (el servicio asigna dueno y shortCode)
    ArgumentCaptor<Plan> published = ArgumentCaptor.forClass(Plan.class);
    verify(this.planServiceMock, times(1)).createAdminPlan(published.capture());
    Plan plan = published.getValue();
    assertThat(plan.getName(), is(PLAN_NAME));
    assertThat(plan.getIsPublic(), is(true));
    assertThat(plan.getBudget().isRange(), is(true));
    assertThat(plan.getEventTime(), is(LocalTime.of(11, 0)));
    assertThat(plan.getPlanPlaces(), hasSize(4));
    assertThat(
      plan.getPlanPlaces().stream().map(PlanPlace::getPlace).map(Place::getName).toList(),
      contains(
        "Cafe Tortoni",
        "El Ateneo Grand Splendid",
        "Obelisco de Buenos Aires",
        "Parrilla Don Julio"
      )
    );
  }

  @Test
  public void lasParadasSalenDelCatalogoConSuDuracionPorCategoria() {
    // given
    givenEnabledSeeder();

    // when
    this.seeder.onApplicationEvent(null);

    // then: el orden es el del itinerario y cada parada trae la duracion de su categoria
    ArgumentCaptor<Plan> published = ArgumentCaptor.forClass(Plan.class);
    verify(this.planServiceMock).createAdminPlan(published.capture());
    List<PlanPlace> stops = published.getValue().getPlanPlaces();
    assertThat(stops.get(0).getSortOrder(), is(1));
    assertThat(stops.get(3).getSortOrder(), is(4));
    assertThat(stops.get(0).getDurationMinutes(), is(90));
  }

  @Test
  public void salteaLasParadasQueNoEstanEnElCatalogo() {
    // given: el catalogo se quedo corto
    when(this.publicPlanSeederMock.isEnabled()).thenReturn(true);
    when(this.placeRepositoryMock.findAll())
      .thenReturn(placesNamed("Cafe Tortoni", "Obelisco de Buenos Aires"));

    // when
    this.seeder.onApplicationEvent(null);

    // then: publica las que existen, sin romper la agenda
    ArgumentCaptor<Plan> published = ArgumentCaptor.forClass(Plan.class);
    verify(this.planServiceMock).createAdminPlan(published.capture());
    assertThat(published.getValue().getPlanPlaces(), hasSize(2));
  }

  @Test
  public void elSegundoEventoNoVuelveASembrar() {
    // given
    givenEnabledSeeder();

    // when: un refresh del contexto dispara el evento dos veces
    this.seeder.onApplicationEvent(null);
    this.seeder.onApplicationEvent(null);

    // then: el flag en memoria corta la segunda corrida
    verify(this.publicPlanSeederMock, times(1)).ensureSeeded();
    verify(this.planServiceMock, times(1)).createAdminPlan(any(Plan.class));
  }

  // --- fixtures ---

  private void givenEnabledSeeder() {
    when(this.publicPlanSeederMock.isEnabled()).thenReturn(true);
    when(this.placeRepositoryMock.findAll())
      .thenReturn(
        placesNamed(
          "Cafe Tortoni",
          "El Ateneo Grand Splendid",
          "Obelisco de Buenos Aires",
          "Parrilla Don Julio"
        )
      );
  }

  private static Plan planOwnedBy(String email) {
    User owner = new User();
    owner.setId(UUID.randomUUID());
    owner.setEmail(email);
    Plan plan = new Plan();
    plan.setId(UUID.randomUUID());
    plan.setAdministrator(owner);
    return plan;
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
}
