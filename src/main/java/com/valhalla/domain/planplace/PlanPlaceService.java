package com.valhalla.domain.planplace;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface PlanPlaceService {
  PlanPlace addPlaceToPlan(Long planId, Long placeId, String ownerEmail);
  List<PlanPlace> getItinerary(Long planId, String ownerEmail);
  PlanPlace updatePlanPlace(
    Long planId,
    Long id,
    LocalDate date,
    LocalTime time,
    String ownerEmail
  );
  void removePlaceFromPlan(Long planId, Long id, String ownerEmail);
  void reorderPlaces(Long planId, List<Long> entryIds, String ownerEmail);
  boolean isPlaceInPlan(Long planId, Long placeId);
}
