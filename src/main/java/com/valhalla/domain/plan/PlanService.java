package com.valhalla.domain.plan;

import java.util.List;
import java.util.Optional;

public interface PlanService {
    Plan createPlan(Plan plan);
    Optional<Plan> getPlanById(Long id);
    Optional<Plan> getPlanByShortCode(String shortCode);
    List<Plan> getPlansByUserEmail(String email);
    Plan updatePlan(Plan plan);
    void deletePlan(Long id);
    String generateShortCode();
}