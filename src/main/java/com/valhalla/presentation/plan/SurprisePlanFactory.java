package com.valhalla.presentation.plan;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.place.PlaceCategory;
import com.valhalla.domain.place.PlaceCostEstimator;
import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanBudget;
import com.valhalla.domain.plan.TransportMode;
import com.valhalla.domain.planplace.PlanPlace;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * [SURPRISE] Arma el plan privado del botÃƒÂ³n "Sorprendeme" con lugares reales del catÃƒÂ¡logo.
 *
 * <p>El catÃƒÂ¡logo de lugares no guarda un precio por lugar, asÃƒÂ­ que el costo se estima por categorÃƒÂ­a
 * (un cafÃƒÂ© no cuesta lo mismo que una parrilla). Esa estimaciÃƒÂ³n es la que decide si el presupuesto
 * alcanza y cuÃƒÂ¡ntas paradas entran: hasta tres, una por categorÃƒÂ­a, sin repetir rubro.
 */
final class SurprisePlanFactory {

  private static final int MAX_STOPS = 3;
  private static final LocalTime DEFAULT_START_TIME = LocalTime.of(10, 0);
  private static final LocalTime DAY_END = LocalTime.of(23, 0);
  private static final String STYLE_GASTRONOMIA = "gastronomia";
  private static final String STYLE_CULTURA = "cultura";
  private static final String STYLE_AIRE_LIBRE = "aire_libre";
  private static final String STYLE_MIXTO = "mixto";

  private static final List<PlaceCategory> MIXED_CATEGORIES = List.of(
    PlaceCategory.RESTAURANT,
    PlaceCategory.MUSEUM,
    PlaceCategory.PARK,
    PlaceCategory.CAFE,
    PlaceCategory.CULTURE,
    PlaceCategory.BAR,
    PlaceCategory.SHOPPING,
    PlaceCategory.OTHER,
    PlaceCategory.SPORT,
    PlaceCategory.NIGHTLIFE
  );

  private static final Map<String, List<PlaceCategory>> STYLE_CATEGORIES = Map.of(
    STYLE_GASTRONOMIA,
    List.of(
      PlaceCategory.RESTAURANT,
      PlaceCategory.CAFE,
      PlaceCategory.BAR,
      PlaceCategory.NIGHTLIFE
    ),
    STYLE_CULTURA,
    List.of(PlaceCategory.MUSEUM, PlaceCategory.CULTURE, PlaceCategory.SHOPPING),
    STYLE_AIRE_LIBRE,
    List.of(PlaceCategory.PARK, PlaceCategory.OTHER, PlaceCategory.SPORT),
    STYLE_MIXTO,
    MIXED_CATEGORIES
  );

  private static final Map<String, String> STYLE_LABELS = Map.of(
    STYLE_GASTRONOMIA,
    "gastronomÃƒÂ­a",
    STYLE_CULTURA,
    "cultura",
    STYLE_AIRE_LIBRE,
    "aire libre",
    STYLE_MIXTO,
    STYLE_MIXTO
  );

  private static final Map<String, String> STYLE_TITLES = Map.of(
    STYLE_GASTRONOMIA,
    "Sabores sorpresa",
    STYLE_CULTURA,
    "Cultura sorpresa",
    STYLE_AIRE_LIBRE,
    "Aire libre sorpresa",
    STYLE_MIXTO,
    "Sorpresa completa"
  );

  private static final Map<String, LocalTime> STYLE_START_TIMES = Map.of(
    STYLE_GASTRONOMIA,
    LocalTime.of(19, 0),
    STYLE_CULTURA,
    LocalTime.of(10, 0),
    STYLE_AIRE_LIBRE,
    LocalTime.of(10, 0),
    STYLE_MIXTO,
    LocalTime.of(11, 0)
  );

  private SurprisePlanFactory() {}

  /**
   * Construye el borrador del plan. No persiste nada: el controlador lo entrega a {@code
   * PlanService}, que resuelve los lugares y deriva el horario.
   *
   * @param form respuestas del formulario
   * @param places catÃƒÂ¡logo completo de lugares
   * @return plan privado con las paradas elegidas
   * @throws IllegalArgumentException si no hay lugares o el presupuesto no alcanza para el mÃƒÂ­nimo
   */
  static Plan build(SurpriseRequest form, List<Place> places) {
    List<Place> candidates = candidates(form.getStyle(), places);
    if (candidates.isEmpty()) {
      throw new IllegalArgumentException("No hay lugares cargados para armar el plan.");
    }
    BigDecimal minimum = candidates
      .stream()
      .map(PlaceCostEstimator::estimate)
      .min(Comparator.naturalOrder())
      .orElse(PlaceCostEstimator.DEFAULT_PRICE);
    BigDecimal budget = form.getBudget();
    if (budget.compareTo(minimum) < 0) {
      throw new IllegalArgumentException(
        "Con $" +
        formatAmount(budget) +
        " no te alcanza para ningÃƒÂºn lugar de " +
        styleLabel(form.getStyle()) +
        ". El mÃƒÂ­nimo estimado es $" +
        formatAmount(minimum) +
        "."
      );
    }
    List<Place> selected = select(candidates, budget);

    Plan plan = new Plan();
    plan.setName(titleFor(form.getStyle()));
    plan.setDescription(description(form));
    plan.setEventDate(form.getDate());
    plan.setEventTime(startTimeFor(form.getStyle()));
    plan.setDayEndTime(DAY_END);
    plan.setTransportMode(TransportMode.WALK);
    plan.setIsPublic(false);
    plan.setBudget(new PlanBudget(budget, budget, notesFor(form), form.getPeople()));
    int order = 1;
    for (Place place : selected) {
      PlanPlace stop = new PlanPlace();
      stop.setPlace(place);
      stop.setSortOrder(order);
      order++;
      plan.addPlanPlace(stop);
    }
    return plan;
  }

  private static String notesFor(SurpriseRequest form) {
    return (
      "Sorprendeme: estilo " +
      (form.getStyle() == null ? STYLE_MIXTO : form.getStyle()) +
      ", presupuesto $" +
      formatAmount(form.getBudget()) +
      " por persona"
    );
  }

  private static List<Place> candidates(String style, List<Place> places) {
    List<PlaceCategory> categories = categoriesFor(style);
    return places
      .stream()
      .filter(place -> place.getCategory() != null)
      .filter(place -> categories.contains(place.getCategory()))
      .sorted(
        Comparator
          .comparingInt((Place place) -> categories.indexOf(place.getCategory()))
          .thenComparing(Place::getName, String.CASE_INSENSITIVE_ORDER)
      )
      .toList();
  }

  /**
   * Elige hasta tres paradas variando categorÃ¯Â¿Â½as y lugares para que no salga siempre lo mismo.
   * Considera el costo estimado por lugar y un margen simple para traslados.
   */
  private static List<Place> select(List<Place> candidates, BigDecimal budget) {
    List<Map.Entry<PlaceCategory, List<Place>>> groups = shuffledGroups(candidates);
    BigDecimal available = availableBudget(budget);
    List<Place> selected = new ArrayList<>();
    BigDecimal total = BigDecimal.ZERO;
    for (Map.Entry<PlaceCategory, List<Place>> group : groups) {
      if (selected.size() >= MAX_STOPS) {
        break;
      }
      Place picked = pickAffordable(group.getValue(), total, available, budget);
      if (picked != null) {
        selected.add(picked);
        total = total.add(PlaceCostEstimator.estimate(picked));
      }
    }
    fillRemaining(selected, candidates, budget);
    return selected;
  }

  private static List<Map.Entry<PlaceCategory, List<Place>>> shuffledGroups(
    List<Place> candidates
  ) {
    Map<PlaceCategory, List<Place>> byCategory = new LinkedHashMap<>();
    for (Place place : candidates) {
      byCategory.computeIfAbsent(place.getCategory(), category -> new ArrayList<>()).add(place);
    }
    List<Map.Entry<PlaceCategory, List<Place>>> groups = new ArrayList<>(byCategory.entrySet());
    java.util.Collections.shuffle(groups, java.util.concurrent.ThreadLocalRandom.current());
    for (Map.Entry<PlaceCategory, List<Place>> group : groups) {
      java.util.Collections.shuffle(
        group.getValue(),
        java.util.concurrent.ThreadLocalRandom.current()
      );
    }
    return groups;
  }

  private static BigDecimal availableBudget(BigDecimal budget) {
    BigDecimal travelReserve = BigDecimal.valueOf(1500L * Math.max(0, MAX_STOPS - 1));
    BigDecimal available = budget.subtract(travelReserve);
    return available.signum() < 0 ? budget : available;
  }

  private static void fillRemaining(
    List<Place> selected,
    List<Place> candidates,
    BigDecimal budget
  ) {
    BigDecimal total = selected
      .stream()
      .map(PlaceCostEstimator::estimate)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    List<Place> all = new ArrayList<>(candidates);
    java.util.Collections.shuffle(all, java.util.concurrent.ThreadLocalRandom.current());
    for (Place place : all) {
      if (selected.size() >= MAX_STOPS) {
        break;
      }
      if (selected.contains(place)) {
        continue;
      }
      BigDecimal price = PlaceCostEstimator.estimate(place);
      if (total.add(price).compareTo(budget) <= 0) {
        selected.add(place);
        total = total.add(price);
      }
    }
  }

  // cheapestOf eliminado: build garantiza que al menos el mÃƒÂ­nimo entra en presupuesto.

  private static Place pickAffordable(
    List<Place> pool,
    BigDecimal total,
    BigDecimal preferredLimit,
    BigDecimal hardLimit
  ) {
    for (Place place : pool) {
      BigDecimal price = PlaceCostEstimator.estimate(place);
      if (total.add(price).compareTo(preferredLimit) <= 0) {
        return place;
      }
    }
    return pool
      .stream()
      .filter(place -> total.add(PlaceCostEstimator.estimate(place)).compareTo(hardLimit) <= 0)
      .min(Comparator.comparing(PlaceCostEstimator::estimate))
      .orElse(null);
  }

  private static String description(SurpriseRequest form) {
    return "Plan sorpresa · " + styleLabel(form.getStyle());
  }

  private static List<PlaceCategory> categoriesFor(String style) {
    return style == null
      ? MIXED_CATEGORIES
      : STYLE_CATEGORIES.getOrDefault(style, MIXED_CATEGORIES);
  }

  private static String styleLabel(String style) {
    return style == null ? "sorpresa" : STYLE_LABELS.getOrDefault(style, "sorpresa");
  }

  private static String titleFor(String style) {
    return style == null ? "Plan sorpresa" : STYLE_TITLES.getOrDefault(style, "Plan sorpresa");
  }

  private static LocalTime startTimeFor(String style) {
    return style == null
      ? DEFAULT_START_TIME
      : STYLE_START_TIMES.getOrDefault(style, DEFAULT_START_TIME);
  }

  private static String formatAmount(BigDecimal amount) {
    return amount.stripTrailingZeros().toPlainString();
  }

  /** Params para regenerar el plan con la misma idea (otra sugerencia). */
  static Map<String, String> againParams(Plan plan) {
    Map<String, String> params = new LinkedHashMap<>();
    if (plan == null || plan.getBudget() == null) {
      params.put("style", STYLE_MIXTO);
      return params;
    }
    String notes = plan.getBudget().getNotes() == null ? "" : plan.getBudget().getNotes();
    java.util.regex.Matcher matcher = java.util.regex.Pattern
      .compile("estilo\\s+([^,]+)")
      .matcher(notes);
    params.put("style", matcher.find() ? matcher.group(1).trim() : STYLE_MIXTO);
    BigDecimal amount = plan.getBudget().getMaximum() != null
      ? plan.getBudget().getMaximum()
      : plan.getBudget().getMinimum();
    if (amount != null) {
      params.put("budget", amount.stripTrailingZeros().toPlainString());
    }
    if (plan.getEventDate() != null) {
      params.put("date", plan.getEventDate().toString());
    }
    if (plan.getBudget().getPeople() != null) {
      params.put("people", String.valueOf(plan.getBudget().getPeople()));
    }
    return params;
  }
}
