package com.valhalla.domain.plan;

import java.util.List;
import java.util.Optional;

public interface PlanRepository {
  Plan save(Plan plan);
  Optional<Plan> findById(Long id);
  Optional<Plan> findByShortCode(String shortCode);
  List<Plan> findByUserId(Long userId);
  void deleteById(Long id);
}
