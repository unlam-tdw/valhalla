package com.valhalla.presentation.plan;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.ModelAndView;

/** [SURPRISE] Tests del botón "Sorprendeme" del feed. */
public class SurprisePlanControllerTest {

  private static final String OWNER_EMAIL = "dueno@test.com";
  private static final UUID PLAN_ID = UUID.fromString("0192f0a1-0000-7000-8000-000000000007");

  private PlanService planServiceMock;
  private SurprisePlanController controller;
  private Authentication authentication;

  @BeforeEach
  public void init() {
    this.planServiceMock = mock(PlanService.class);
    this.controller = new SurprisePlanController(this.planServiceMock);
    this.authentication = new UsernamePasswordAuthenticationToken(OWNER_EMAIL, null);
  }

  @Test
  public void SURPRISE_GET_muestraElFormularioConValoresPorDefecto() {
    ModelAndView view = this.controller.surpriseForm();

    assertThat(view.getViewName(), is("pages/plans/surprise"));
    SurpriseRequest form = (SurpriseRequest) view.getModel().get("surpriseForm");
    assertThat(form.getDate(), is(notNullValue()));
    assertThat(form.getStyle(), is("gastronomia"));
  }

  @Test
  public void SURPRISE_POST_creaPlanPrivadoConPresupuestoEFecha() {
    SurpriseRequest form = new SurpriseRequest();
    form.setBudget(new BigDecimal("20000"));
    form.setDate(LocalDate.of(2026, 12, 20));
    form.setPeople(2);
    form.setStyle("cultura");
    when(this.planServiceMock.createPlan(any(Plan.class), eq(OWNER_EMAIL)))
      .thenReturn(plan(PLAN_ID, "Sorpresa"));

    ModelAndView view =
      this.controller.createSurprise(
          form,
          new BeanPropertyBindingResult(form, "surpriseForm"),
          this.authentication
        );

    ArgumentCaptor<Plan> captor = ArgumentCaptor.forClass(Plan.class);
    verify(this.planServiceMock).createPlan(captor.capture(), eq(OWNER_EMAIL));
    Plan draft = captor.getValue();
    assertThat(draft.getName(), containsString("cultura"));
    assertThat(draft.getEventDate(), is(LocalDate.of(2026, 12, 20)));
    assertThat(draft.getIsPublic(), is(false));
    assertThat(draft.getBudget().getMaximum(), is(new BigDecimal("20000")));
    assertThat(view.getViewName(), is("redirect:/plans/" + PLAN_ID));
  }

  @Test
  public void SURPRISE_POST_conErroresNoCreaPlan() {
    SurpriseRequest form = new SurpriseRequest();
    BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "surpriseForm");
    errors.reject("budget", "El presupuesto es obligatorio");

    ModelAndView view = this.controller.createSurprise(form, errors, this.authentication);

    assertThat(view.getViewName(), is("pages/plans/surprise"));
    verify(this.planServiceMock, never()).createPlan(any(Plan.class), anyString());
  }

  private Plan plan(UUID id, String name) {
    Plan plan = new Plan();
    plan.setId(id);
    plan.setName(name);
    return plan;
  }
}
