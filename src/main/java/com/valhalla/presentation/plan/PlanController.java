package com.valhalla.presentation.plan;

import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/plans")
public class PlanController {

  private static final String VIEW_PLANS_LIST = "pages/plans/list";
  private static final String VIEW_PLAN_FORM = "pages/plans/new";
  private static final String VIEW_PLAN_DETAIL = "pages/plans/detail";
  private static final String REDIRECT_PLANS = "redirect:/plans";
  private static final String REDIRECT_PLAN_DETAIL = "redirect:/plans/";
  private static final String ATTR_PLANS = "plans";
  private static final String ATTR_PLAN = "plan";
  private static final String ATTR_PLAN_ID = "planId";

  private final PlanService planService;

  @Autowired
  public PlanController(PlanService planService) {
    this.planService = planService;
  }

  @GetMapping
  public ModelAndView listPlans(Authentication authentication) {
    Map<String, Object> model = new ModelMap();
    model.put(ATTR_PLANS, planService.getPlansByUserEmail(authentication.getName()));
    return new ModelAndView(VIEW_PLANS_LIST, model);
  }

  @GetMapping("/new")
  public ModelAndView showNewPlanForm() {
    Map<String, Object> model = new ModelMap();
    model.put(ATTR_PLAN, new PlanRequest());
    return new ModelAndView(VIEW_PLAN_FORM, model);
  }

  @GetMapping("/{id}")
  public ModelAndView showPlan(@PathVariable Long id, Authentication authentication) {
    Map<String, Object> model = new ModelMap();
    model.put(ATTR_PLAN, planService.getOwnedPlan(id, authentication.getName()));
    return new ModelAndView(VIEW_PLAN_DETAIL, model);
  }

  @PostMapping
  public ModelAndView createPlan(
    @Valid @ModelAttribute(ATTR_PLAN) PlanRequest planForm,
    BindingResult bindingResult,
    Authentication authentication
  ) {
    if (bindingResult.hasErrors()) {
      return renderFormWithError(planForm, null);
    }
    Plan created = planService.createPlan(toPlan(planForm), authentication.getName());
    return new ModelAndView(REDIRECT_PLAN_DETAIL + created.getId());
  }

  @RequestMapping(value = "/{id}", method = { RequestMethod.POST, RequestMethod.PUT })
  public ModelAndView updatePlan(
    @PathVariable Long id,
    @Valid @ModelAttribute(ATTR_PLAN) PlanRequest planForm,
    BindingResult bindingResult,
    Authentication authentication
  ) {
    if (bindingResult.hasErrors()) {
      return renderFormWithError(planForm, id);
    }
    planService.updatePlan(id, toPlan(planForm), authentication.getName());
    return new ModelAndView(REDIRECT_PLAN_DETAIL + id);
  }

  /**
   * Delete lives on its own sub-path and accepts POST, the same way UserController's
   * /admin/users/{id}/delete does. The reason is not cosmetic: the app is wired through
   * MyServletInitializer, a plain AbstractAnnotationConfigDispatcherServletInitializer, so the
   * HiddenHttpMethodFilter bean is never registered with the servlet container and {@code _method}
   * is never honoured at runtime. It only appears to work under MockMvc, because
   * webAppContextSetup picks Filter beans up from the context. A delete form posted to /{id}
   * therefore arrived as a POST and landed on the update handler above, which rejected the empty
   * name and re-rendered the form. Registering POST here is what makes the button work for real.
   */
  @RequestMapping(value = "/{id}/delete", method = { RequestMethod.POST, RequestMethod.DELETE })
  public ModelAndView deletePlan(@PathVariable Long id, Authentication authentication) {
    planService.deleteOwnedPlan(id, authentication.getName());
    return new ModelAndView(REDIRECT_PLANS);
  }

  /** The form owns these four fields only: id, shortCode and administrator never come from a post. */
  private Plan toPlan(PlanRequest planForm) {
    Plan plan = new Plan();
    plan.setName(planForm.getName());
    plan.setDescription(planForm.getDescription());
    plan.setEventDate(planForm.getEventDate());
    plan.setIsPublic(planForm.getIsPublic());
    return plan;
  }

  private ModelAndView renderFormWithError(PlanRequest planForm, Long id) {
    Map<String, Object> model = new ModelMap();
    model.put(ATTR_PLAN, planForm);
    if (id != null) {
      model.put(ATTR_PLAN_ID, id);
    }
    model.put("error", "Invalid plan data");
    return new ModelAndView(VIEW_PLAN_FORM, model);
  }
}
