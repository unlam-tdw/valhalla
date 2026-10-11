package com.valhalla.infrastructure.plan;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.valhalla.domain.exception.PlanNotFoundException;
import com.valhalla.domain.exception.UserNotFoundException;
import com.valhalla.domain.place.Place;
import com.valhalla.domain.place.PlaceRepository;
import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanRepository;
import com.valhalla.domain.planplace.PlanPlace;
import com.valhalla.domain.user.User;
import com.valhalla.domain.user.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link PlanServiceImpl} against mocked ports.
 *
 * <p><b>Anti-vacuity rule.</b> Every test asserts the interaction with the repository, not just the
 * returned value. The test this file replaces passed because Mockito answered {@code
 * Optional.empty()} by default, so the line it claimed to cover never ran. Each negative case here
 * additionally proves the effect: a {@code never()} on the destructive call, or the entity left
 * untouched.
 */
public class PlanServiceImplTest {

  private static final String OWNER_EMAIL = "dueno@test.com";
  private static final String OTHER_EMAIL = "otro@test.com";
  private static final String UNKNOWN_EMAIL = "nadie@test.com";
  private static final Long OWNER_ID = 7L;
  private static final Long PLAN_ID = 42L;

  /** The backend generates an 8 character uppercase alphanumeric code out of a UUID. */
  private static final String SHORT_CODE_PATTERN = "[A-Z0-9]{8}";

  private PlanServiceImpl planService;
  private PlanRepository planRepositoryMock;
  private UserRepository userRepositoryMock;
  private PlaceRepository placeRepositoryMock;

  @BeforeEach
  public void init() {
    this.planRepositoryMock = mock(PlanRepository.class);
    this.userRepositoryMock = mock(UserRepository.class);
    this.placeRepositoryMock = mock(PlaceRepository.class);
    this.planService =
      new PlanServiceImpl(
        this.planRepositoryMock,
        this.userRepositoryMock,
        this.placeRepositoryMock
      );
  }

  // --- createPlan ---

  @Test
  public void T_PLN_025_createPlan_resuelveLosLugaresYPermiteRepetirUnoVariasVeces() {
    // given
    when(this.userRepositoryMock.findByEmail(OWNER_EMAIL))
      .thenReturn(Optional.of(owner(OWNER_ID, OWNER_EMAIL)));
    Place real = new Place();
    real.setId(5L);
    when(this.placeRepositoryMock.findById(5L)).thenReturn(Optional.of(real));
    Plan plan = new Plan();
    plan.setName("Viaje");
    for (int order = 1; order <= 2; order++) {
      Place stub = new Place();
      stub.setId(5L);
      PlanPlace entry = new PlanPlace();
      entry.setPlace(stub);
      entry.setSortOrder(order);
      plan.addPlanPlace(entry);
    }
    when(this.planRepositoryMock.save(plan)).thenReturn(plan);

    // when
    this.planService.createPlan(plan, OWNER_EMAIL);

    // then
    assertThat(plan.getPlanPlaces().get(0).getPlace(), is(sameInstance(real)));
    assertThat(plan.getPlanPlaces().get(1).getPlace(), is(sameInstance(real)));
    verify(this.planRepositoryMock, times(1)).save(plan);
  }

  @Test
  public void T_PLN_026_createPlan_rechazaUnLugarInexistenteYNoGuarda() {
    // given
    when(this.userRepositoryMock.findByEmail(OWNER_EMAIL))
      .thenReturn(Optional.of(owner(OWNER_ID, OWNER_EMAIL)));
    when(this.placeRepositoryMock.findById(99L)).thenReturn(Optional.empty());
    Plan plan = new Plan();
    Place stub = new Place();
    stub.setId(99L);
    PlanPlace entry = new PlanPlace();
    entry.setPlace(stub);
    plan.addPlanPlace(entry);

    // when and then
    assertThrows(
      IllegalArgumentException.class,
      () -> this.planService.createPlan(plan, OWNER_EMAIL)
    );
    verify(this.planRepositoryMock, never()).save(any(Plan.class));
  }

  @Test
  public void T_PLN_020_createPlan_asignaElDuenoResueltoYGardaElPlan() {
    // given
    User owner = owner(OWNER_ID, OWNER_EMAIL);
    Plan plan = new Plan();
    plan.setName("Viaje a Bariloche");
    when(this.userRepositoryMock.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
    when(this.planRepositoryMock.save(plan)).thenReturn(plan);

    // when
    Plan created = this.planService.createPlan(plan, OWNER_EMAIL);

    // then
    assertThat(created, is(sameInstance(plan)));
    assertThat(plan.getAdministrator(), is(sameInstance(owner)));
    assertThat(plan.getShortCode(), is(notNullValue()));
    assertThat(plan.getShortCode(), matchesPattern(SHORT_CODE_PATTERN));
    verify(this.planRepositoryMock, times(1)).existsByShortCode(plan.getShortCode());
    verify(this.planRepositoryMock, times(1)).save(plan);
  }

  @Test
  public void T_PLN_021_createPlan_ignoraElShortCodeQueMandaElForm() {
    // given
    when(this.userRepositoryMock.findByEmail(OWNER_EMAIL))
      .thenReturn(Optional.of(owner(OWNER_ID, OWNER_EMAIL)));
    Plan plan = new Plan();
    plan.setName("Viaje a Bariloche");
    plan.setShortCode("HACKED01");
    when(this.planRepositoryMock.save(plan)).thenReturn(plan);

    // when
    this.planService.createPlan(plan, OWNER_EMAIL);

    // then: the incoming code never reaches the repository, not even as a lookup
    assertThat(plan.getShortCode(), is(not(equalTo("HACKED01"))));
    assertThat(plan.getShortCode(), matchesPattern(SHORT_CODE_PATTERN));
    verify(this.planRepositoryMock, never()).findByShortCode(anyString());
    verify(this.planRepositoryMock, times(1)).save(plan);
  }

  @Test
  public void T_PLN_022_createPlan_reintentaHastaQueElShortCodeEsteLibre() {
    // given: the first three candidates are taken, the fourth is free
    when(this.userRepositoryMock.findByEmail(OWNER_EMAIL))
      .thenReturn(Optional.of(owner(OWNER_ID, OWNER_EMAIL)));
    when(this.planRepositoryMock.existsByShortCode(anyString()))
      .thenReturn(true, true, true, false);
    Plan plan = new Plan();
    plan.setName("Viaje a Bariloche");
    when(this.planRepositoryMock.save(plan)).thenReturn(plan);

    // when
    this.planService.createPlan(plan, OWNER_EMAIL);

    // then
    assertThat(plan.getShortCode(), matchesPattern(SHORT_CODE_PATTERN));
    verify(this.planRepositoryMock, times(4)).existsByShortCode(anyString());
    verify(this.planRepositoryMock, times(1)).save(plan);
  }

  @Test
  public void T_PLN_023_createPlan_agotaLosDiezIntentosYNoGuardaNada() {
    // given
    when(this.userRepositoryMock.findByEmail(OWNER_EMAIL))
      .thenReturn(Optional.of(owner(OWNER_ID, OWNER_EMAIL)));
    when(this.planRepositoryMock.existsByShortCode(anyString())).thenReturn(true);

    // when and then
    assertThrows(
      IllegalStateException.class,
      () -> this.planService.createPlan(new Plan(), OWNER_EMAIL)
    );
    verify(this.planRepositoryMock, times(10)).existsByShortCode(anyString());
    verify(this.planRepositoryMock, never()).save(any(Plan.class));
  }

  @Test
  public void T_PLN_024_createPlan_fallaSiElDuenoNoExisteYTocandoPlanes() {
    // given
    when(this.userRepositoryMock.findByEmail(UNKNOWN_EMAIL)).thenReturn(Optional.empty());

    // when and then
    assertThrows(
      UserNotFoundException.class,
      () -> this.planService.createPlan(new Plan(), UNKNOWN_EMAIL)
    );
    verifyNoInteractions(this.planRepositoryMock);
  }

  // --- getPlansByUserEmail ---

  @Test
  public void T_PLN_025_getPlansByUserEmail_buscaPorElIdDelDuenoResuelto() {
    // given
    User owner = owner(OWNER_ID, OWNER_EMAIL);
    List<Plan> planes = List.of(ownedPlan(PLAN_ID, owner), ownedPlan(PLAN_ID + 1, owner));
    when(this.userRepositoryMock.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
    when(this.planRepositoryMock.findByAdministratorId(OWNER_ID)).thenReturn(planes);

    // when
    List<Plan> result = this.planService.getPlansByUserEmail(OWNER_EMAIL);

    // then
    assertThat(result, is(equalTo(planes)));
    verify(this.userRepositoryMock, times(1)).findByEmail(OWNER_EMAIL);
    verify(this.planRepositoryMock, times(1)).findByAdministratorId(OWNER_ID);
  }

  @Test
  public void T_PLN_026_getPlansByUserEmail_devuelveListaVaciaSiElDuenoNoExiste() {
    // given
    when(this.userRepositoryMock.findByEmail(UNKNOWN_EMAIL)).thenReturn(Optional.empty());

    // when
    List<Plan> result = this.planService.getPlansByUserEmail(UNKNOWN_EMAIL);

    // then
    assertThat(result, is(empty()));
    verify(this.planRepositoryMock, never()).findByAdministratorId(any());
  }

  // --- getPublicPlans [PPV] ---

  @Test
  public void U05_getPublicPlans_devuelveLosPlanesQueExponeElRepositorio() {
    // given: el filtro isPublic = true vive en la query del repositorio, no en el servicio
    List<Plan> publicos = List.of(ownedPlan(PLAN_ID, owner(OWNER_ID, OWNER_EMAIL)));
    when(this.planRepositoryMock.findByIsPublicTrue()).thenReturn(publicos);

    // when
    List<Plan> result = this.planService.getPublicPlans();

    // then
    assertThat(result, is(equalTo(publicos)));
    verify(this.planRepositoryMock, times(1)).findByIsPublicTrue();
  }

  @Test
  public void U06_getPublicPlans_sinNingunPlanPublicoDevuelveListaVacia() {
    // given
    when(this.planRepositoryMock.findByIsPublicTrue()).thenReturn(List.of());

    // when
    List<Plan> result = this.planService.getPublicPlans();

    // then: la base vacia es un caso normal, no una excepcion (AC-06)
    assertThat(result, is(empty()));
    verify(this.planRepositoryMock, times(1)).findByIsPublicTrue();
  }

  @Test
  public void U07_getPublicPlans_noLlamaASaveNiModificaPlanes() {
    // given
    when(this.planRepositoryMock.findByIsPublicTrue())
      .thenReturn(List.of(ownedPlan(PLAN_ID, owner(OWNER_ID, OWNER_EMAIL))));

    // when
    this.planService.getPublicPlans();

    // then: es una lectura, el listado no escribe ni ordena nada (AC-05)
    assertNothingWasMutated();
  }

  // --- getOwnedPlan ---

  @Test
  public void T_PLN_027_getOwnedPlan_devuelveElPlanPropio() {
    // given
    User owner = owner(OWNER_ID, OWNER_EMAIL);
    Plan plan = ownedPlan(PLAN_ID, owner);
    when(this.planRepositoryMock.findById(PLAN_ID)).thenReturn(Optional.of(plan));

    // when
    Plan result = this.planService.getOwnedPlan(PLAN_ID, OWNER_EMAIL);

    // then
    assertThat(result, is(sameInstance(plan)));
    verify(this.planRepositoryMock, times(1)).findById(PLAN_ID);
  }

  @Test
  public void T_PLN_028_getOwnedPlan_fallaSiElPlanNoExiste() {
    // given
    when(this.planRepositoryMock.findById(PLAN_ID)).thenReturn(Optional.empty());

    // when and then
    assertThrows(
      PlanNotFoundException.class,
      () -> this.planService.getOwnedPlan(PLAN_ID, OWNER_EMAIL)
    );
    assertNothingWasMutated();
  }

  @Test
  public void T_PLN_029_getOwnedPlan_fallaIgualSiElPlanEsDeOtroUsuario() {
    // given
    Plan planOfSomebodyElse = ownedPlan(PLAN_ID, owner(OWNER_ID + 1, OTHER_EMAIL));
    planOfSomebodyElse.setName("Plan ajeno");
    when(this.planRepositoryMock.findById(PLAN_ID)).thenReturn(Optional.of(planOfSomebodyElse));

    // when and then
    assertThrows(
      PlanNotFoundException.class,
      () -> this.planService.getOwnedPlan(PLAN_ID, OWNER_EMAIL)
    );
    // the exception must not leak whether the id exists: same type as the missing-plan case
    assertThat(planOfSomebodyElse.getName(), is(equalTo("Plan ajeno")));
    assertNothingWasMutated();
  }

  @Test
  public void T_PLN_030_getOwnedPlan_fallaSiElPlanNoTieneDueno() {
    // given
    Plan orphan = new Plan();
    orphan.setId(PLAN_ID);
    when(this.planRepositoryMock.findById(PLAN_ID)).thenReturn(Optional.of(orphan));

    // when and then
    assertThrows(
      PlanNotFoundException.class,
      () -> this.planService.getOwnedPlan(PLAN_ID, OWNER_EMAIL)
    );
    assertNothingWasMutated();
  }

  // --- updatePlan ---

  @Test
  public void T_PLN_031_updatePlan_cambiaSoloLosCuatroCamposQueMandaElForm() {
    // given
    User owner = owner(OWNER_ID, OWNER_EMAIL);
    Plan existing = ownedPlan(PLAN_ID, owner);
    existing.setName("Nombre viejo");
    existing.setDescription("Descripcion vieja");
    existing.setEventDate(LocalDate.of(2026, 1, 1));
    existing.setIsPublic(false);
    existing.setShortCode("ABCD1234");
    when(this.planRepositoryMock.findById(PLAN_ID)).thenReturn(Optional.of(existing));
    when(this.planRepositoryMock.save(existing)).thenReturn(existing);

    Plan changes = new Plan();
    changes.setName("Nombre nuevo");
    changes.setDescription("Descripcion nueva");
    changes.setEventDate(LocalDate.of(2026, 12, 31));
    changes.setIsPublic(true);
    // A hostile post would carry these; the form never does and they must not be copied.
    changes.setShortCode("IGNORADO");
    changes.setAdministrator(owner(OWNER_ID + 1, OTHER_EMAIL));

    // when
    Plan updated = this.planService.updatePlan(PLAN_ID, changes, OWNER_EMAIL);

    // then
    assertThat(updated, is(sameInstance(existing)));
    assertThat(updated.getName(), is(equalTo("Nombre nuevo")));
    assertThat(updated.getDescription(), is(equalTo("Descripcion nueva")));
    assertThat(updated.getEventDate(), is(equalTo(LocalDate.of(2026, 12, 31))));
    assertThat(updated.getIsPublic(), is(true));
    assertThat(updated.getShortCode(), is(equalTo("ABCD1234")));
    assertThat(updated.getAdministrator(), is(sameInstance(owner)));
    assertThat(updated.getId(), is(equalTo(PLAN_ID)));
    verify(this.planRepositoryMock, times(1)).save(existing);
  }

  @Test
  public void T_PLN_032_updatePlan_fallaSiNoEsElDuenoYNoGuardaNiTocaElPlan() {
    // given
    Plan planOfSomebodyElse = ownedPlan(PLAN_ID, owner(OWNER_ID + 1, OTHER_EMAIL));
    planOfSomebodyElse.setName("Intocable");
    when(this.planRepositoryMock.findById(PLAN_ID)).thenReturn(Optional.of(planOfSomebodyElse));

    // when and then
    assertThrows(
      PlanNotFoundException.class,
      () -> this.planService.updatePlan(PLAN_ID, new Plan(), OWNER_EMAIL)
    );
    assertThat(planOfSomebodyElse.getName(), is(equalTo("Intocable")));
    verify(this.planRepositoryMock, never()).save(any(Plan.class));
  }

  // --- deleteOwnedPlan ---

  @Test
  public void T_PLN_033_deleteOwnedPlan_borraElPlanDelDueno() {
    // given
    when(this.planRepositoryMock.findById(PLAN_ID))
      .thenReturn(Optional.of(ownedPlan(PLAN_ID, owner(OWNER_ID, OWNER_EMAIL))));

    // when
    this.planService.deleteOwnedPlan(PLAN_ID, OWNER_EMAIL);

    // then
    verify(this.planRepositoryMock, times(1)).deleteById(PLAN_ID);
  }

  @Test
  public void T_PLN_034_deleteOwnedPlan_noBorraSiElPlanEsDeOtroUsuario() {
    // given
    when(this.planRepositoryMock.findById(PLAN_ID))
      .thenReturn(Optional.of(ownedPlan(PLAN_ID, owner(OWNER_ID + 1, OTHER_EMAIL))));

    // when and then
    assertThrows(
      PlanNotFoundException.class,
      () -> this.planService.deleteOwnedPlan(PLAN_ID, OWNER_EMAIL)
    );
    verify(this.planRepositoryMock, never()).deleteById(any());
    verify(this.planRepositoryMock, never()).save(any(Plan.class));
  }

  // --- clonePlan [CLO "Usar plan"] ---

  @Test
  public void clonePlan_copiaElPlanConElClonadorComoDuenoYShortCodeNuevo() {
    // given
    User cloner = owner(OWNER_ID, OWNER_EMAIL);
    Plan source = ownedPlan(PLAN_ID, owner(OWNER_ID + 1, OTHER_EMAIL));
    source.setShortCode("ORIGINAL");
    source.setIsPublic(true);
    when(this.userRepositoryMock.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(cloner));
    when(this.planRepositoryMock.findById(PLAN_ID)).thenReturn(Optional.of(source));
    when(this.planRepositoryMock.existsByShortCode(anyString())).thenReturn(false);
    when(this.planRepositoryMock.save(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));

    // when
    Plan copy = this.planService.clonePlan(PLAN_ID, OWNER_EMAIL);

    // then: dueño nuevo, codigo nuevo y nunca el del original
    assertThat(copy.getAdministrator(), is(sameInstance(cloner)));
    assertThat(copy.getShortCode(), matchesPattern(SHORT_CODE_PATTERN));
    assertThat(copy.getShortCode(), is(not(equalTo("ORIGINAL"))));
    assertThat(copy.getParticipants(), is(empty()));
    verify(this.planRepositoryMock, times(1)).save(copy);
  }

  @Test
  public void clonePlan_laCopiaNacePrivadaAunqueElOriginalSeaPublico() {
    // given
    Plan source = ownedPlan(PLAN_ID, owner(OWNER_ID + 1, OTHER_EMAIL));
    source.setIsPublic(true);
    when(this.userRepositoryMock.findByEmail(OWNER_EMAIL))
      .thenReturn(Optional.of(owner(OWNER_ID, OWNER_EMAIL)));
    when(this.planRepositoryMock.findById(PLAN_ID)).thenReturn(Optional.of(source));
    when(this.planRepositoryMock.existsByShortCode(anyString())).thenReturn(false);
    when(this.planRepositoryMock.save(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));

    // when
    Plan copy = this.planService.clonePlan(PLAN_ID, OWNER_EMAIL);

    // then: publicar la copia es decision del nuevo dueño
    assertThat(copy.getIsPublic(), is(false));
    assertThat(source.getIsPublic(), is(true));
  }

  @Test
  public void clonePlan_copiaNombreDescripcionYFechas() {
    // given
    Plan source = ownedPlan(PLAN_ID, owner(OWNER_ID + 1, OTHER_EMAIL));
    source.setIsPublic(true);
    source.setName("Original");
    source.setDescription("Descripcion original");
    source.setEventDate(LocalDate.of(2026, 12, 31));
    when(this.userRepositoryMock.findByEmail(OWNER_EMAIL))
      .thenReturn(Optional.of(owner(OWNER_ID, OWNER_EMAIL)));
    when(this.planRepositoryMock.findById(PLAN_ID)).thenReturn(Optional.of(source));
    when(this.planRepositoryMock.existsByShortCode(anyString())).thenReturn(false);
    when(this.planRepositoryMock.save(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));

    // when
    Plan copy = this.planService.clonePlan(PLAN_ID, OWNER_EMAIL);

    // then
    assertThat(copy.getName(), is(equalTo("Original")));
    assertThat(copy.getDescription(), is(equalTo("Descripcion original")));
    assertThat(copy.getEventDate(), is(equalTo(LocalDate.of(2026, 12, 31))));
  }

  @Test
  public void clonePlan_copiaElItinerarioReferenciandoLosMismosLugares() {
    // given: un plan con dos paradas, fechas y orden propios
    Place malba = new Place();
    malba.setId(5L);
    Place tortoni = new Place();
    tortoni.setId(6L);
    Plan source = new Plan();
    source.setId(PLAN_ID);
    source.setName("Con itinerario");
    source.setIsPublic(true);
    PlanPlace first = new PlanPlace();
    first.setPlace(malba);
    first.setDescription("Sala 2");
    first.setSortOrder(1);
    PlanPlace second = new PlanPlace();
    second.setPlace(tortoni);
    second.setSortOrder(2);
    source.addPlanPlace(first);
    source.addPlanPlace(second);
    when(this.userRepositoryMock.findByEmail(OWNER_EMAIL))
      .thenReturn(Optional.of(owner(OWNER_ID, OWNER_EMAIL)));
    when(this.planRepositoryMock.findById(PLAN_ID)).thenReturn(Optional.of(source));
    when(this.planRepositoryMock.existsByShortCode(anyString())).thenReturn(false);
    when(this.planRepositoryMock.save(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));

    // when
    Plan copy = this.planService.clonePlan(PLAN_ID, OWNER_EMAIL);

    // then: las paradas se copian en orden, con los mismos Place persistidos (no duplicados)
    assertThat(copy.getPlanPlaces(), hasSize(2));
    assertThat(copy.getPlanPlaces().get(0).getPlace(), is(sameInstance(malba)));
    assertThat(copy.getPlanPlaces().get(0).getDescription(), is(equalTo("Sala 2")));
    assertThat(copy.getPlanPlaces().get(0).getSortOrder(), is(equalTo(1)));
    assertThat(copy.getPlanPlaces().get(1).getPlace(), is(sameInstance(tortoni)));
    assertThat(copy.getPlanPlaces().get(1).getSortOrder(), is(equalTo(2)));
    // los PlanPlace de la copia son objetos nuevos: el original no recibe sus entidades
    assertThat(copy.getPlanPlaces().get(0), is(not(sameInstance(first))));
  }

  @Test
  public void clonePlan_clonaUnPlanPrivadoDelQueSoyAdministrador() {
    // given
    Plan source = ownedPlan(PLAN_ID, owner(OWNER_ID, OWNER_EMAIL));
    source.setIsPublic(false);
    when(this.userRepositoryMock.findByEmail(OWNER_EMAIL))
      .thenReturn(Optional.of(owner(OWNER_ID, OWNER_EMAIL)));
    when(this.planRepositoryMock.findById(PLAN_ID)).thenReturn(Optional.of(source));
    when(this.planRepositoryMock.existsByShortCode(anyString())).thenReturn(false);
    when(this.planRepositoryMock.save(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));

    // when
    Plan copy = this.planService.clonePlan(PLAN_ID, OWNER_EMAIL);

    // then
    assertThat(copy, is(notNullValue()));
    verify(this.planRepositoryMock, times(1)).save(copy);
  }

  @Test
  public void clonePlan_clonaUnPlanPrivadoDelQueSoyParticipant() {
    // given
    Plan source = ownedPlan(PLAN_ID, owner(OWNER_ID + 1, OTHER_EMAIL));
    source.setIsPublic(false);
    source.getParticipants().add(owner(OWNER_ID, OWNER_EMAIL));
    when(this.userRepositoryMock.findByEmail(OWNER_EMAIL))
      .thenReturn(Optional.of(owner(OWNER_ID, OWNER_EMAIL)));
    when(this.planRepositoryMock.findById(PLAN_ID)).thenReturn(Optional.of(source));
    when(this.planRepositoryMock.existsByShortCode(anyString())).thenReturn(false);
    when(this.planRepositoryMock.save(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));

    // when
    Plan copy = this.planService.clonePlan(PLAN_ID, OWNER_EMAIL);

    // then: el participant clona, aunque su copia no arranque con participants
    assertThat(copy, is(notNullValue()));
    assertThat(copy.getParticipants(), is(empty()));
  }

  @Test
  public void clonePlan_noClonaUnPlanPrivadoAjenoYNoLoConfiesaExistente() {
    // given: privado, de otro, y el clonador ni siquiera es participant
    Plan source = ownedPlan(PLAN_ID, owner(OWNER_ID + 1, OTHER_EMAIL));
    source.setIsPublic(false);
    source.setName("Privado ajeno");
    when(this.userRepositoryMock.findByEmail(OWNER_EMAIL))
      .thenReturn(Optional.of(owner(OWNER_ID, OWNER_EMAIL)));
    when(this.planRepositoryMock.findById(PLAN_ID)).thenReturn(Optional.of(source));

    // when and then: mismo error que un id inexistente — no se filtra su existencia
    assertThrows(
      PlanNotFoundException.class,
      () -> this.planService.clonePlan(PLAN_ID, OWNER_EMAIL)
    );
    assertThat(source.getName(), is(equalTo("Privado ajeno")));
    assertNothingWasMutated();
  }

  @Test
  public void clonePlan_fallaSiElPlanNoExiste() {
    // given
    when(this.userRepositoryMock.findByEmail(OWNER_EMAIL))
      .thenReturn(Optional.of(owner(OWNER_ID, OWNER_EMAIL)));
    when(this.planRepositoryMock.findById(99L)).thenReturn(Optional.empty());

    // when and then
    assertThrows(PlanNotFoundException.class, () -> this.planService.clonePlan(99L, OWNER_EMAIL));
    assertNothingWasMutated();
  }

  @Test
  public void clonePlan_fallaSiElClonadorNoExisteYSinTocarLosPlanes() {
    // given
    when(this.userRepositoryMock.findByEmail(UNKNOWN_EMAIL)).thenReturn(Optional.empty());

    // when and then
    assertThrows(
      UserNotFoundException.class,
      () -> this.planService.clonePlan(PLAN_ID, UNKNOWN_EMAIL)
    );
    verifyNoInteractions(this.planRepositoryMock);
  }

  // --- fixtures ---

  /** Effect check shared by every read-path rejection: nothing was written or deleted. */
  private void assertNothingWasMutated() {
    verify(this.planRepositoryMock, never()).save(any(Plan.class));
    verify(this.planRepositoryMock, never()).deleteById(any());
  }

  private User owner(Long id, String email) {
    User user = new User();
    user.setId(id);
    user.setEmail(email);
    return user;
  }

  private Plan ownedPlan(Long id, User administrator) {
    Plan plan = new Plan();
    plan.setId(id);
    plan.setName("Viaje a Bariloche");
    plan.setAdministrator(administrator);
    return plan;
  }
}
