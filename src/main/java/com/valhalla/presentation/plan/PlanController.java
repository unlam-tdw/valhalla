package com.valhalla.presentation.plan;

import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/planes")
public class PlanController {

  private final PlanService planService;

  public PlanController(PlanService planService) {
    this.planService = planService;
  }

  @GetMapping
  public String planList(Model model) {
    model.addAttribute("plans", planService.getAllPlans());
    return "pages/plans/list";
  }

  @GetMapping("/{id}")
  public String planDetail(@PathVariable Long id, Model model) {
    Plan plan = planService
      .getPlanById(id)
      .orElseThrow(() -> new IllegalArgumentException("Plan no encontrado"));
    model.addAttribute("plan", plan);
    return "pages/plans/detail";
  }

  @GetMapping("/crear")
  public String planCreate(Model model) {
    model.addAttribute("plan", new Plan());
    return "pages/plans/create";
  }

  @PostMapping("/crear")
  public String planGenerate(@ModelAttribute Plan plan) {
    planService.createPlan(plan);
    return "redirect:/planes";
  }

  @PostMapping("/delete/{id}")
  public String planDelete(@PathVariable Long id) {
    planService.deletePlan(id);
    return "redirect:/planes";
  }
}
