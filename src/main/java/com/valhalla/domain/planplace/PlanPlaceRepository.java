package com.valhalla.domain.planplace;

import java.util.List;
import java.util.Optional;

public interface PlanPlaceRepository {
  PlanPlace save(PlanPlace entry);
  List<PlanPlace> findByPlanId(Long planId);
  Optional<PlanPlace> findById(Long id);
  boolean existsByPlanIdAndPlaceId(Long planId, Long placeId);
  void deleteById(Long id);
}
