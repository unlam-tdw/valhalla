package com.valhalla.presentation.plan;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.place.PlaceCategory;
import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanBudget;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SurprisePlanFactoryTest {

  @Test
  void againParams_extraeDatosDelPlanSorpresa() {
    Plan plan = new Plan();
    plan.setEventDate(LocalDate.of(2026, 12, 20));
    plan.setBudget(
      new PlanBudget(
        new BigDecimal("10000"),
        new BigDecimal("15000"),
        "Sorprendeme: estilo cultura, presupuesto $15000 por persona",
        3
      )
    );

    Map<String, String> params = SurprisePlanFactory.againParams(plan);

    assertThat(params.get("style"), is("cultura"));
    assertThat(params.get("budget"), is("15000"));
    assertThat(params.get("date"), is("2026-12-20"));
    assertThat(params.get("people"), is("3"));
  }

  @Test
  void againParams_usaMinimoSiNoHayMaximoYDefaults() {
    Plan plan = new Plan();
    plan.setBudget(new PlanBudget(new BigDecimal("5000"), new BigDecimal("5000"), null, null));

    Map<String, String> params = SurprisePlanFactory.againParams(plan);

    assertThat(params.get("style"), is("mixto"));
    assertThat(params.get("budget"), is("5000"));
    assertThat(params.containsKey("date"), is(false));
    assertThat(params.containsKey("people"), is(false));
  }

  @Test
  void againParams_sinMontosNiNotasNoIncluyeBudget() {
    Plan plan = new Plan();
    plan.setBudget(new PlanBudget());

    Map<String, String> params = SurprisePlanFactory.againParams(plan);

    assertThat(params.get("style"), is("mixto"));
    assertThat(params.containsKey("budget"), is(false));
  }

  @Test
  void againParams_esDefensivoAntePlanOBudgetNulo() {
    assertThat(SurprisePlanFactory.againParams(null).get("style"), is("mixto"));

    Plan plan = new Plan();
    plan.setBudget(null);
    assertThat(SurprisePlanFactory.againParams(plan).get("style"), is("mixto"));
  }

  @Test
  void build_conPresupuestoMinimoYAireLibreCubreRamasDeSeleccion() {
    SurpriseRequest form = new SurpriseRequest();
    form.setBudget(BigDecimal.ZERO);
    form.setDate(LocalDate.of(2026, 12, 20));
    form.setStyle("aire_libre");
    form.setPeople(0);

    List<Place> places = new ArrayList<>();
    // Un lugar gratuito asegura mínimo 0 y fuerza availableBudget negativo -> usa budget.
    places.add(place("Parque", PlaceCategory.PARK));
    places.add(place("Otro", PlaceCategory.OTHER));

    Plan plan = SurprisePlanFactory.build(form, places);

    assertThat(plan.getPlanPlaces().isEmpty(), is(false));
    assertThat(plan.getBudget().isEstimated(), is(true));
    // people 0 no agrega texto de personas.
    assertThat(plan.getDescription().contains("personas"), is(false));
  }

  @Test
  void build_conPresupuestoAjustadoDejaEspacioParaCompletar() {
    SurpriseRequest form = new SurpriseRequest();
    // Restaurante 12000, cafe 4000, bar 8000: con 16000 pueden entrar 2 (12k+4k) y fillRemaining corre.
    form.setBudget(new BigDecimal("16000"));
    form.setDate(LocalDate.of(2026, 12, 20));
    form.setStyle("gastronomia");
    form.setPeople(2);

    Plan plan = SurprisePlanFactory.build(form, placesGastro());

    assertThat(plan.getPlanPlaces().size(), greaterThanOrEqualTo(1));
    assertThat(plan.getBudget().getPeople(), is(2));
  }

  @Test
  void build_mixtoAltoVaríaYSigueDentroDelPresupuesto() {
    SurpriseRequest form = new SurpriseRequest();
    form.setBudget(new BigDecimal("50000"));
    form.setDate(LocalDate.of(2026, 12, 20));
    form.setStyle("mixto");
    form.setPeople(1);

    Plan plan = SurprisePlanFactory.build(form, placesAll());

    assertThat(plan.getPlanPlaces().size(), greaterThanOrEqualTo(1));
    java.math.BigDecimal total = java.math.BigDecimal.ZERO;
    for (com.valhalla.domain.planplace.PlanPlace pp : plan.getPlanPlaces()) {
      total = total.add(com.valhalla.domain.place.PlaceCostEstimator.estimate(pp.getPlace()));
    }
    assertThat(total.compareTo(new BigDecimal("50000")) <= 0, is(true));
  }

  @Test
  void build_completaParadasEnFillRemainingCuandoHayMismaCategoria() {
    SurpriseRequest form = new SurpriseRequest();
    form.setBudget(new BigDecimal("24000"));
    form.setDate(LocalDate.of(2026, 12, 20));
    form.setStyle("gastronomia");

    // Misma categoría: first pass toma 1, fillRemaining puede agregar el segundo.
    List<Place> places = List.of(
      place("R1", PlaceCategory.RESTAURANT),
      place("R2", PlaceCategory.RESTAURANT)
    );

    Plan plan = SurprisePlanFactory.build(form, places);

    assertThat(plan.getPlanPlaces().size(), is(2));
  }

  private static List<Place> placesGastro() {
    return List.of(
      place("Parrilla", PlaceCategory.RESTAURANT),
      place("Cafe", PlaceCategory.CAFE),
      place("Bar", PlaceCategory.BAR),
      place("Boliche", PlaceCategory.NIGHTLIFE)
    );
  }

  private static List<Place> placesAll() {
    return List.of(
      place("R", PlaceCategory.RESTAURANT),
      place("C", PlaceCategory.CAFE),
      place("B", PlaceCategory.BAR),
      place("M", PlaceCategory.MUSEUM),
      place("P", PlaceCategory.PARK),
      place("S", PlaceCategory.SHOPPING),
      place("K", PlaceCategory.CULTURE),
      place("N", PlaceCategory.NIGHTLIFE),
      place("O", PlaceCategory.OTHER),
      place("SP", PlaceCategory.SPORT)
    );
  }

  private static Place place(String name, PlaceCategory category) {
    Place place = new Place();
    place.setId(UUID.randomUUID());
    place.setName(name);
    place.setCategory(category);
    return place;
  }
}
