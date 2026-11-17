package com.valhalla.presentation.plan;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.place.PlaceCategory;
import com.valhalla.domain.place.PlaceRepository;
import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.ModelAndView;

/** [SURPRISE] Tests del botÃ³n "Sorprendeme" del feed. */
public class SurprisePlanControllerTest {

  private static final String OWNER_EMAIL = "dueno@test.com";
  private static final UUID PLAN_ID = UUID.fromString("0192f0a1-0000-7000-8000-000000000007");
  private static final LocalDate PLAN_DATE = LocalDate.of(2026, 12, 20);

  private PlanService planServiceMock;
  private PlaceRepository placeRepositoryMock;
  private SurprisePlanController controller;
  private Authentication authentication;

  @BeforeEach
  public void init() {
    this.planServiceMock = mock(PlanService.class);
    this.placeRepositoryMock = mock(PlaceRepository.class);
    when(this.placeRepositoryMock.findAll()).thenReturn(places());
    this.controller = new SurprisePlanController(this.planServiceMock, this.placeRepositoryMock);
    this.authentication = new UsernamePasswordAuthenticationToken(OWNER_EMAIL, null);
  }

  @Test
  public void SURPRISE_GET_muestraElFormularioConValoresPorDefecto() {
    ModelAndView view = this.controller.surpriseForm();

    assertThat(view.getViewName(), is("pages/plans/surprise"));
    SurpriseRequest form = (SurpriseRequest) view.getModel().get("surpriseForm");
    assertThat(form.getDate(), is(notNullValue()));
    assertThat(form.getStyle(), is("mixto"));
  }

  @Test
  public void SURPRISE_POST_creaPlanPrivadoConLugaresYPrecioEstimado() {
    SurpriseRequest form = form(new BigDecimal("20000"), "gastronomia", 2);
    when(this.planServiceMock.createPlan(any(Plan.class), eq(OWNER_EMAIL)))
      .thenReturn(plan(PLAN_ID, "Sorpresa"));

    ModelAndView view =
      this.controller.createSurprise(
          form,
          new BeanPropertyBindingResult(form, "surpriseForm"),
          this.authentication
        );

    ArgumentCaptor<Plan> captor = ArgumentCaptor.forClass(Plan.class);
    verify(this.planServiceMock).createPlan(captor.capture(), eq(OWNER_EMAIL));
    Plan draft = captor.getValue();
    assertThat(view.getViewName(), is("redirect:/plans/" + PLAN_ID));
    assertThat(draft.getName(), containsString("Sabores sorpresa"));
    assertThat(draft.getEventDate(), is(PLAN_DATE));
    assertThat(draft.getEventTime(), is(LocalTime.of(19, 0)));
    assertThat(draft.getIsPublic(), is(false));
    assertThat(draft.getBudget().getMinimum(), is(new BigDecimal("20000")));
    assertThat(draft.getBudget().getMaximum(), is(new BigDecimal("20000")));
    assertThat(draft.getBudget().getPeople(), is(2));
    assertThat(draft.getBudget().getNotes(), containsString("Sorprendeme"));
    assertThat(draft.getBudget().getNotes(), containsString("gastronomia"));
    assertThat(draft.getPlanPlaces().isEmpty(), is(false));
    java.util.Set<PlaceCategory> allowed = java.util.EnumSet.of(
      PlaceCategory.RESTAURANT,
      PlaceCategory.CAFE,
      PlaceCategory.BAR,
      PlaceCategory.NIGHTLIFE
    );
    java.math.BigDecimal total = java.math.BigDecimal.ZERO;
    java.util.Set<java.util.UUID> ids = new java.util.HashSet<>();
    for (int i = 0; i < draft.getPlanPlaces().size(); i++) {
      assertThat(draft.getPlanPlaces().get(i).getSortOrder(), is(i + 1));
      PlaceCategory cat = draft.getPlanPlaces().get(i).getPlace().getCategory();
      assertThat(allowed.contains(cat), is(true));
      ids.add(draft.getPlanPlaces().get(i).getPlace().getId());
      total =
        total.add(
          com.valhalla.domain.place.PlaceCostEstimator.estimate(
            draft.getPlanPlaces().get(i).getPlace()
          )
        );
    }
    assertThat(ids.size(), is(draft.getPlanPlaces().size()));
    assertThat(total.compareTo(new BigDecimal("20000")) <= 0, is(true));
    assertThat(draft.getDescription(), containsString("Plan sorpresa"));
    assertThat(draft.getDescription(), containsString("gastronom"));
  }

  @Test
  public void SURPRISE_POST_mixtoConPresupuestoAltoEligeTresParadas() {
    SurpriseRequest form = form(new BigDecimal("50000"), "mixto", 2);
    when(this.planServiceMock.createPlan(any(Plan.class), eq(OWNER_EMAIL)))
      .thenReturn(plan(PLAN_ID, "Sorpresa"));

    this.controller.createSurprise(
        form,
        new BeanPropertyBindingResult(form, "surpriseForm"),
        this.authentication
      );

    ArgumentCaptor<Plan> captor = ArgumentCaptor.forClass(Plan.class);
    verify(this.planServiceMock).createPlan(captor.capture(), eq(OWNER_EMAIL));
    Plan draft = captor.getValue();
    assertThat(draft.getName(), containsString("Sorpresa completa"));
    assertThat(draft.getEventTime(), is(LocalTime.of(11, 0)));
    assertThat(draft.getPlanPlaces().size(), is(3));
    java.math.BigDecimal total = java.math.BigDecimal.ZERO;
    java.util.Set<java.util.UUID> ids = new java.util.HashSet<>();
    for (int i = 0; i < draft.getPlanPlaces().size(); i++) {
      assertThat(draft.getPlanPlaces().get(i).getSortOrder(), is(i + 1));
      ids.add(draft.getPlanPlaces().get(i).getPlace().getId());
      total =
        total.add(
          com.valhalla.domain.place.PlaceCostEstimator.estimate(
            draft.getPlanPlaces().get(i).getPlace()
          )
        );
    }
    assertThat(ids.size(), is(3));
    assertThat(total.compareTo(new BigDecimal("50000")) <= 0, is(true));
  }

  @Test
  public void SURPRISE_POST_sinEstiloUsaNombreGenerico() {
    SurpriseRequest form = form(new BigDecimal("20000"), null, 2);
    when(this.planServiceMock.createPlan(any(Plan.class), eq(OWNER_EMAIL)))
      .thenReturn(plan(PLAN_ID, "Sorpresa"));

    this.controller.createSurprise(
        form,
        new BeanPropertyBindingResult(form, "surpriseForm"),
        this.authentication
      );

    ArgumentCaptor<Plan> captor = ArgumentCaptor.forClass(Plan.class);
    verify(this.planServiceMock).createPlan(captor.capture(), eq(OWNER_EMAIL));
    assertThat(captor.getValue().getName(), containsString("Plan sorpresa"));
    assertThat(captor.getValue().getDescription(), containsString("Plan sorpresa"));
    assertThat(captor.getValue().getDescription(), containsString("sorpresa"));
    assertThat(captor.getValue().getBudget().getMinimum(), is(new BigDecimal("20000")));
  }

  @Test
  public void SURPRISE_POST_conCeroAvisaQueNoAlcanza() {
    SurpriseRequest form = form(BigDecimal.ZERO, "cultura", 1);
    BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "surpriseForm");

    ModelAndView view = this.controller.createSurprise(form, errors, this.authentication);

    assertThat(view.getViewName(), is("pages/plans/surprise"));
    FieldError error = errors.getFieldError("budget");
    assertThat(error, is(notNullValue()));
    assertThat(error.getDefaultMessage(), containsString("no te alcanza"));
    assertThat(error.getDefaultMessage(), containsString("0"));
    verify(this.planServiceMock, never()).createPlan(any(Plan.class), anyString());
  }

  @Test
  public void SURPRISE_POST_conPresupuestoBajoAvisaElMinimoDelEstilo() {
    SurpriseRequest form = form(new BigDecimal("3000"), "cultura", 1);
    BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "surpriseForm");

    ModelAndView view = this.controller.createSurprise(form, errors, this.authentication);

    assertThat(view.getViewName(), is("pages/plans/surprise"));
    FieldError error = errors.getFieldError("budget");
    assertThat(error, is(notNullValue()));
    assertThat(error.getDefaultMessage(), containsString("nimo estimado"));
    assertThat(error.getDefaultMessage(), containsString("4000"));
    verify(this.planServiceMock, never()).createPlan(any(Plan.class), anyString());
  }

  @Test
  public void SURPRISE_POST_sinLugaresAvisaQueNoHayCatalogo() {
    when(this.placeRepositoryMock.findAll()).thenReturn(List.of());
    SurpriseRequest form = form(new BigDecimal("20000"), "mixto", 1);
    BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "surpriseForm");

    ModelAndView view = this.controller.createSurprise(form, errors, this.authentication);

    assertThat(view.getViewName(), is("pages/plans/surprise"));
    FieldError error = errors.getFieldError("budget");
    assertThat(error, is(notNullValue()));
    assertThat(error.getDefaultMessage(), containsString("No hay lugares"));
    verify(this.planServiceMock, never()).createPlan(any(Plan.class), anyString());
  }

  @Test
  public void SURPRISE_POST_conErroresDeValidacionNoCreaPlan() {
    SurpriseRequest form = new SurpriseRequest();
    BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "surpriseForm");
    errors.rejectValue("budget", "budget", "El presupuesto es obligatorio");

    ModelAndView view = this.controller.createSurprise(form, errors, this.authentication);

    assertThat(view.getViewName(), is("pages/plans/surprise"));
    verify(this.planServiceMock, never()).createPlan(any(Plan.class), anyString());
  }

  private static SurpriseRequest form(BigDecimal budget, String style, Integer people) {
    SurpriseRequest form = new SurpriseRequest();
    form.setBudget(budget);
    form.setDate(PLAN_DATE);
    form.setPeople(people);
    form.setStyle(style);
    return form;
  }

  private static List<Place> places() {
    return List.of(
      place("Parrilla Don Julio", PlaceCategory.RESTAURANT),
      place("Zzz Parrilla", PlaceCategory.RESTAURANT),
      place("Cafe Tortoni", PlaceCategory.CAFE),
      place("Cerveceria San Martin", PlaceCategory.BAR),
      place("La Viruta", PlaceCategory.NIGHTLIFE),
      place("MALBA", PlaceCategory.MUSEUM),
      place("El Ateneo", PlaceCategory.CULTURE),
      place("Plaza Serrano", PlaceCategory.SHOPPING),
      place("Parque Tres de Febrero", PlaceCategory.PARK),
      place("Obelisco", PlaceCategory.OTHER),
      place("Estadio Monumental", PlaceCategory.SPORT)
    );
  }

  private static Place place(String name, PlaceCategory category) {
    Place place = new Place();
    place.setId(UUID.randomUUID());
    place.setName(name);
    place.setCategory(category);
    return place;
  }

  private static Plan plan(UUID id, String name) {
    Plan plan = new Plan();
    plan.setId(id);
    plan.setName(name);
    return plan;
  }
}
