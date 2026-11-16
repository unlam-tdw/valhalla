package com.valhalla.presentation.plan;

import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanBudget;
import com.valhalla.domain.plan.PlanService;
import com.valhalla.domain.plan.TransportMode;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalTime;
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

  private final PlanService planService;

  @Autowired
  public SurprisePlanController(PlanService planService) {
    this.planService = planService;
  }

  @GetMapping
  public ModelAndView surpriseForm() {
    SurpriseRequest form = new SurpriseRequest();
    form.setDate(java.time.LocalDate.now().plusDays(7));
    form.setStyle("gastronomia");
    return new ModelAndView(VIEW_SURPRISE, Map.of("surpriseForm", form));
  }

  @PostMapping
  public ModelAndView createSurprise(
    @Valid @ModelAttribute("surpriseForm") SurpriseRequest form,
    BindingResult bindingResult,
    Authentication authentication
  ) {
    if (bindingResult.hasErrors()) {
      return new ModelAndView(VIEW_SURPRISE, Map.of("surpriseForm", form));
    }
    Plan created = planService.createPlan(surprisePlan(form), authentication.getName());
    return new ModelAndView(REDIRECT_PLAN_DETAIL + created.getId());
  }

  private static Plan surprisePlan(SurpriseRequest form) {
    Plan plan = new Plan();
    plan.setName(surpriseName(form));
    plan.setDescription(surpriseDescription(form));
    plan.setEventDate(form.getDate());
    plan.setEventTime(LocalTime.of(10, 0));
    plan.setDayEndTime(LocalTime.of(23, 0));
    plan.setTransportMode(TransportMode.WALK);
    plan.setIsPublic(false);
    plan.setBudget(
      new PlanBudget(BigDecimal.ZERO, form.getBudget(), "Presupuesto estimado por persona")
    );
    return plan;
  }

  private static String surpriseName(SurpriseRequest form) {
    return "Sorpresa: " + styleLabel(form.getStyle()) + " para " + form.getDate();
  }

  private static String surpriseDescription(SurpriseRequest form) {
    StringBuilder description = new StringBuilder();
    description
      .append("Plan generado con Sorprendeme para ")
      .append(form.getDate())
      .append(". Presupuesto estimado: $")
      .append(form.getBudget())
      .append(" por persona.");
    if (form.getPeople() != null && form.getPeople() > 0) {
      description.append(" Pensado para ").append(form.getPeople()).append(" personas.");
    }
    description.append(" Estilo: ").append(styleLabel(form.getStyle())).append(".");
    return description.toString();
  }

  private static String styleLabel(String style) {
    return switch (style == null ? "" : style) {
      case "gastronomia" -> "gastronomía";
      case "cultura" -> "cultura";
      case "aire_libre" -> "aire libre";
      case "mixto" -> "mixto";
      default -> "sorpresa";
    };
  }
}
