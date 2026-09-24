package com.valhalla.domain.plan;

import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

public interface PlanService {
  Plan createPlan(Plan plan);

  Optional<Plan> getPlanById(Long id);

  Optional<Plan> getPlanByShortCode(String shortCode);

  @Transactional(readOnly = true)
  List<Plan> getAllPlans();

  List<Plan> getPlansByUserEmail(String email);

  Plan updatePlan(Plan plan);

  void deletePlan(Long id);

  String generateShortCode();
}
