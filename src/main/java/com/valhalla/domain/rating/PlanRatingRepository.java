package com.valhalla.domain.rating;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** [CAL] Acceso a las calificaciones de los planes. */
public interface PlanRatingRepository {
  Optional<PlanRating> findByPlanIdAndUserEmail(UUID planId, String email);

  List<RatingSummary> findSummaries();

  /** [PAGE] Agregados solo de los planes de la pagina que se va a pintar. */
  List<RatingSummary> findSummariesByPlanIds(Collection<UUID> planIds);

  PlanRating save(PlanRating rating);

  void deleteAllByPlanId(UUID planId);
}
