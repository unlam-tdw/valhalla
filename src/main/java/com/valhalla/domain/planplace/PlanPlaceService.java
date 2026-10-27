package com.valhalla.domain.planplace;

import java.util.List;
import java.util.UUID;

public interface PlanPlaceService {
  PlanPlace addPlaceToPlan(UUID planId, UUID placeId, String ownerEmail);
  List<PlanPlace> getItinerary(UUID planId, String ownerEmail);
  PlanPlace updatePlanPlace(UUID planId, UUID id, PlanPlaceUpdate changes, String ownerEmail);
  void removePlaceFromPlan(UUID planId, UUID id, String ownerEmail);
  void reorderPlaces(UUID planId, List<UUID> entryIds, String ownerEmail);
  boolean isPlaceInPlan(UUID planId, UUID placeId);
}
