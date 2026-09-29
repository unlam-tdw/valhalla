package com.valhalla.presentation.plan;

import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(PlanController.ROUTE_PLANS)
public class PlanController {

  public static final String ROUTE_PLANS = "/plans";
  public static final String ROUTE_NEW = "/new";
  public static final String ROUTE_ID = "/{id}";
  private static final String MODEL_PLAN = "plan";
  private static final String VIEW_PLANS_LIST = "pages/plans/list";
  private static final String VIEW_PLANS_CREATE = "pages/plans";
  private static final String VIEW_PLANS_DETAIL = "pages/plans/detail";
  private static final String REDIRECT_PLANS = "redirect:/plans/";

  private final PlanService planService;

  public PlanController(PlanService planService) {
    this.planService = planService;
  }

  @GetMapping
  public String planList(Model model) {
    model.addAttribute("plans", planService.getAllPlans());
    return VIEW_PLANS_LIST;
  }

  @GetMapping(ROUTE_NEW)
  public String planCreate(Model model) {
    model.addAttribute(MODEL_PLAN, new Plan());
    return VIEW_PLANS_CREATE;
  }

  @GetMapping(ROUTE_ID)
  public String planDetail(@PathVariable Long id, Model model) {
    planService.getPlanById(id).ifPresent(plan -> model.addAttribute(MODEL_PLAN, plan));
    return VIEW_PLANS_DETAIL;
  }

  @PostMapping
  public String planGenerate(
          @Valid @ModelAttribute(MODEL_PLAN) Plan plan,
          BindingResult bindingResult) {

    if (bindingResult.hasErrors()) {
      return VIEW_PLANS_CREATE;
    }

    planService.createPlan(plan);
    return REDIRECT_PLANS + plan.getIdPlan();
  }

  @PutMapping(ROUTE_ID)
  public String planUpdate(
          @PathVariable Long id,
          @Valid @ModelAttribute(MODEL_PLAN) Plan plan,
          BindingResult bindingResult) {

    if (bindingResult.hasErrors()) {
      return VIEW_PLANS_DETAIL;
    }

    plan.setIdPlan(id);
    planService.updatePlan(plan);
    return REDIRECT_PLANS + id;
  }

  @DeleteMapping(ROUTE_ID)
  public String planDelete(@PathVariable Long id) {
    planService.deletePlan(id);
    return "redirect:/plans";
  }
}