package com.valhalla.domain.place;

import static org.junit.jupiter.api.Assertions.*;

import com.valhalla.domain.plan.TransportMode;
import com.valhalla.domain.planplace.PlanPlace;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlaceCostEstimatorTest {

  @Test
  void estimatePorCategoria() {
    Place restaurante = place(PlaceCategory.RESTAURANT);
    assertEquals(new BigDecimal("12000"), PlaceCostEstimator.estimate(restaurante));
    assertEquals(BigDecimal.ZERO, PlaceCostEstimator.estimate(place(PlaceCategory.PARK)));
  }

  @Test
  void sinCategoriaCaeAlPrecioPorDefecto() {
    assertEquals(PlaceCostEstimator.DEFAULT_PRICE, PlaceCostEstimator.estimate(new Place()));
    assertEquals(PlaceCostEstimator.DEFAULT_PRICE, PlaceCostEstimator.estimate(null));
  }

  @Test
  void pricesByCategoryCubreTodosLosRubrosYElDefault() {
    var prices = PlaceCostEstimator.pricesByCategory();
    for (PlaceCategory category : PlaceCategory.values()) {
      assertTrue(prices.containsKey(category.name()));
    }
    assertEquals(PlaceCostEstimator.DEFAULT_PRICE, prices.get("DEFAULT"));
    assertEquals(PlaceCategory.values().length + 1, prices.size());
  }

  @Test
  void travelCostPorModo() {
    assertEquals(BigDecimal.ZERO, PlaceCostEstimator.travelCost(null));
    assertEquals(BigDecimal.ZERO, PlaceCostEstimator.travelCost(TransportMode.WALK));
    assertEquals(BigDecimal.valueOf(750), PlaceCostEstimator.travelCost(TransportMode.BUS));
    assertEquals(BigDecimal.valueOf(2000), PlaceCostEstimator.travelCost(TransportMode.CAR));
  }

  @Test
  void tripCostSumaLugaresYTrasladosSinContarElUltimoTramo() {
    PlanPlace first = new PlanPlace();
    first.setPlace(place(PlaceCategory.RESTAURANT)); // 12000
    first.setTransportModeToNext(TransportMode.BUS); // 750
    PlanPlace second = new PlanPlace();
    second.setPlace(place(PlaceCategory.CAFE)); // 4000
    second.setTransportModeToNext(TransportMode.CAR); // no suma (último)

    assertEquals(new BigDecimal("16750"), PlaceCostEstimator.tripCost(List.of(first, second)));
  }

  @Test
  void tripCostToleraEntradasIncompletas() {
    PlanPlace nullPlace = new PlanPlace();
    PlanPlace withPlace = new PlanPlace();
    withPlace.setPlace(place(PlaceCategory.PARK));

    assertEquals(BigDecimal.ZERO, PlaceCostEstimator.tripCost(List.of(nullPlace, withPlace)));
    assertEquals(BigDecimal.ZERO, PlaceCostEstimator.tripCost(List.of()));
    assertEquals(
      BigDecimal.ZERO,
      PlaceCostEstimator.tripCost(java.util.Collections.singletonList(null))
    );
  }

  private static Place place(PlaceCategory category) {
    Place place = new Place();
    place.setId(UUID.randomUUID());
    place.setName("Lugar");
    place.setCategory(category);
    return place;
  }
}
