package com.valhalla.presentation.plan;

import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanService;
import com.valhalla.domain.planplace.PlanPlace;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plans")
public class PlanRestController {

  private final PlanService planService;

  public PlanRestController(PlanService planService) {
    this.planService = planService;
  }

  @GetMapping
  public List<PlanSummary> getUserPlans(Principal principal) {
    if (principal == null) {
      return List.of();
    }
    return planService
      .getPlansByUserEmail(principal.getName())
      .stream()
      .map(PlanSummary::from)
      .toList();
  }

  public record PlanSummary(
    Long id,
    String name,
    String eventDate,
    Boolean isPublic,
    List<PlanPlaceSummary> planPlaces
  ) {
    static PlanSummary from(Plan plan) {
      return new PlanSummary(
        plan.getId(),
        plan.getName(),
        plan.getEventDate() == null ? null : plan.getEventDate().toString(),
        plan.getIsPublic(),
        plan.getPlanPlaces().stream().map(PlanPlaceSummary::from).toList()
      );
    }
  }

  public record PlanPlaceSummary(
    Long id,
    Long placeId,
    String name,
    String category,
    Double latitude,
    Double longitude
  ) {
    static PlanPlaceSummary from(PlanPlace pp) {
      return new PlanPlaceSummary(
        pp.getId(),
        pp.getPlace().getId(),
        pp.getPlace().getName(),
        pp.getPlace().getCategory() == null ? null : pp.getPlace().getCategory().name(),
        pp.getPlace().getLatitude(),
        pp.getPlace().getLongitude()
      );
    }
  }
}
