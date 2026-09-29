package com.valhalla.presentation.plan;

import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/plans")
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

  @GetMapping("/new")
  public String planCreate(Model model) {
    model.addAttribute("plan", new Plan());
    return "pages/plans";
  }

  @GetMapping("/{id}")
  public String planDetail(@PathVariable Long id, Model model) {
    planService.getPlanById(id).ifPresent(plan -> model.addAttribute("plan", plan));
    return "pages/plans/detail";
  }

  @PostMapping
  public String planGenerate(
          @Valid @ModelAttribute("plan") Plan plan,
          BindingResult bindingResult) {

    if (bindingResult.hasErrors()) {
      return "pages/plans";
    }

    planService.createPlan(plan);
    return "redirect:/plans/" + plan.getIdPlan();
  }

  @PutMapping("/{id}")
  public String planUpdate(
          @PathVariable Long id,
          @Valid @ModelAttribute("plan") Plan plan,
          BindingResult bindingResult) {

    if (bindingResult.hasErrors()) {
      return "pages/plans/detail";
    }

    plan.setIdPlan(id);
    planService.updatePlan(plan);
    return "redirect:/plans/" + id;
  }

  @DeleteMapping("/{id}")
  public String planDelete(@PathVariable Long id) {
    planService.deletePlan(id);
    return "redirect:/plans";
  }
}