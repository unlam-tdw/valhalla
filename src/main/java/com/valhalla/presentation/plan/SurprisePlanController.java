package com.valhalla.presentation.plan;

import com.valhalla.domain.place.PlaceRepository;
import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/** [SURPRISE] Botón "Sorprendeme" del feed: preguntas mínimas y plan privado generado. */
@Controller
@RequestMapping("/plans/surprise")
public class SurprisePlanController {

  private static final String VIEW_SURPRISE = "pages/plans/surprise";
  private static final String REDIRECT_PLAN_DETAIL = "redirect:/plans/";
  private static final String FORM_ATTR = "surpriseForm";

  private final PlanService planService;
  private final PlaceRepository placeRepository;

  @Autowired
  public SurprisePlanController(PlanService planService, PlaceRepository placeRepository) {
    this.planService = planService;
    this.placeRepository = placeRepository;
  }

  @GetMapping
  public ModelAndView surpriseForm() {
    SurpriseRequest form = new SurpriseRequest();
    form.setDate(java.time.LocalDate.now().plusDays(7));
    form.setStyle("mixto");
    ModelAndView view = new ModelAndView(VIEW_SURPRISE, Map.of(FORM_ATTR, form));
    view.addObject("today", java.time.LocalDate.now().toString());
    return view;
  }

  @PostMapping
  public ModelAndView createSurprise(
    @Valid @ModelAttribute(FORM_ATTR) SurpriseRequest form,
    BindingResult bindingResult,
    Authentication authentication
  ) {
    if (bindingResult.hasErrors()) {
      return surpriseView(form, bindingResult);
    }
    try {
      Plan draft = SurprisePlanFactory.build(form, placeRepository.findAll());
      Plan created = planService.createPlan(draft, authentication.getName());
      return new ModelAndView(REDIRECT_PLAN_DETAIL + created.getId());
    } catch (IllegalArgumentException e) {
      bindingResult.rejectValue("budget", "budget", e.getMessage());
      return surpriseView(form, bindingResult);
    }
  }

  private ModelAndView surpriseView(SurpriseRequest form, BindingResult bindingResult) {
    ModelAndView view = new ModelAndView(VIEW_SURPRISE);
    view.addObject(FORM_ATTR, form);
    view.addObject(
      org.springframework.validation.BindingResult.MODEL_KEY_PREFIX + FORM_ATTR,
      bindingResult
    );
    view.addObject("today", java.time.LocalDate.now().toString());
    return view;
  }
}
