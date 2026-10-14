package com.valhalla.domain.plan;

import java.util.List;
import java.util.UUID;

public interface PlanService {
  Plan createPlan(Plan plan, String ownerEmail);
  List<Plan> getPlansByUserEmail(String ownerEmail);
  List<Plan> getPublicPlans();
  Plan getOwnedPlan(UUID id, String ownerEmail);
  Plan getParticipatingPlan(UUID id, String userEmail);
  Plan getPublicPlan(UUID id);
  List<Plan> getParticipantPlans(String userEmail);
  void leavePlan(UUID id, String userEmail);
  Plan joinPlan(String shortCode, String userEmail);
  Plan updatePlan(UUID id, Plan changes, String ownerEmail);
  void deleteOwnedPlan(UUID id, String ownerEmail);
  /** [CLO] Copia propia y editable de un plan accesible: nuevo dueño, nuevo shortCode. */
  Plan clonePlan(UUID id, String clonerEmail);
}
