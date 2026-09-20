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
    return "plans/list";
  }

  @GetMapping("/{id}")
  public String planDetail(@PathVariable Long id, Model model) {
    model.addAttribute("plan", planService.getPlanById(id));
    return "plans/detail";
  }

  @GetMapping
  public String planCreate(Model model) {
    model.addAttribute("plan", new Plan());
    return "plans/create";
  }

  @PostMapping("/crear")
  public String planGenerate(@ModelAttribute Plan plan) {
    planService.createPlan(plan);
    return "redirect:/planes";
  }
}
