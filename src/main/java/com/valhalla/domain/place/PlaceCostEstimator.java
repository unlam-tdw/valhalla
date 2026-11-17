package com.valhalla.domain.place;

import com.valhalla.domain.plan.TransportMode;
import com.valhalla.domain.planplace.PlanPlace;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Estimated ARS cost per place and per transport leg, always per person. */
public final class PlaceCostEstimator {

  public static final BigDecimal DEFAULT_PRICE = BigDecimal.ZERO;

  private static final Map<PlaceCategory, BigDecimal> ESTIMATED_PRICES = Map.ofEntries(
    Map.entry(PlaceCategory.RESTAURANT, BigDecimal.valueOf(12000)),
    Map.entry(PlaceCategory.BAR, BigDecimal.valueOf(8000)),
    Map.entry(PlaceCategory.CAFE, BigDecimal.valueOf(4000)),
    Map.entry(PlaceCategory.MUSEUM, BigDecimal.valueOf(5000)),
    // Los parques y los monumentos se visitan gratis: lo que cuesta del dia es el traslado.
    Map.entry(PlaceCategory.PARK, BigDecimal.ZERO),
    Map.entry(PlaceCategory.SHOPPING, BigDecimal.valueOf(6000)),
    Map.entry(PlaceCategory.NIGHTLIFE, BigDecimal.valueOf(10000)),
    Map.entry(PlaceCategory.CULTURE, BigDecimal.valueOf(4000)),
    Map.entry(PlaceCategory.SPORT, BigDecimal.valueOf(7000)),
    Map.entry(PlaceCategory.OTHER, BigDecimal.ZERO)
  );

  /** Costo estimado por persona de un traslado entre paradas, segun el modo del tramo. */
  private static final Map<TransportMode, BigDecimal> TRAVEL_COSTS = Map.of(
    TransportMode.WALK,
    BigDecimal.ZERO,
    TransportMode.BIKE,
    BigDecimal.ZERO,
    TransportMode.BUS,
    BigDecimal.valueOf(750),
    TransportMode.CAR,
    BigDecimal.valueOf(2000)
  );

  private PlaceCostEstimator() {}

  public static BigDecimal estimate(Place place) {
    if (place == null || place.getCategory() == null) {
      return DEFAULT_PRICE;
    }
    return ESTIMATED_PRICES.getOrDefault(place.getCategory(), DEFAULT_PRICE);
  }

  /** Traslado por persona de un tramo; null o WALK salen gratis, que es lo esperado a pie. */
  public static BigDecimal travelCost(TransportMode mode) {
    if (mode == null) {
      return BigDecimal.ZERO;
    }
    return TRAVEL_COSTS.getOrDefault(mode, BigDecimal.ZERO);
  }

  /**
   * Costo por persona del recorrido completo: el precio de cada lugar mas el traslado de cada
   * tramo hacia la parada siguiente. El ultimo tramo no existe, asi que no aporta. La lista viaja
   * en el orden del itinerario.
   */
  public static BigDecimal tripCost(List<PlanPlace> entries) {
    BigDecimal total = BigDecimal.ZERO;
    for (int i = 0; i < entries.size(); i++) {
      PlanPlace entry = entries.get(i);
      if (entry != null && entry.getPlace() != null) {
        total = total.add(estimate(entry.getPlace()));
      }
      if (i + 1 < entries.size()) {
        total = total.add(travelCost(entry.getTransportModeToNext()));
      }
    }
    return total;
  }

  public static Map<String, BigDecimal> pricesByCategory() {
    Map<String, BigDecimal> prices = new LinkedHashMap<>();
    for (PlaceCategory category : PlaceCategory.values()) {
      prices.put(category.name(), ESTIMATED_PRICES.getOrDefault(category, DEFAULT_PRICE));
    }
    prices.put("DEFAULT", DEFAULT_PRICE);
    return prices;
  }
}
