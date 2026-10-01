package com.valhalla.presentation.plan;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.valhalla.domain.exception.PlanNotFoundException;
import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanService;
import com.valhalla.presentation.shared.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

public class PlanControllerTest {

  private static final String OWNER_EMAIL = "user@test.com";

  private PlanController controller;
  private PlanService planServiceMock;
  private Authentication authentication;
  private MockMvc mockMvc;

  @BeforeEach
  public void init() {
    this.planServiceMock = mock(PlanService.class);
    this.controller = new PlanController(this.planServiceMock);
    this.authentication = new UsernamePasswordAuthenticationToken(OWNER_EMAIL, null);
    this.mockMvc =
      MockMvcBuilders
        .standaloneSetup(this.controller)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  public void T_PLN_001_planList_retornaPlanes() {
    Plan plan1 = new Plan();
    Plan plan2 = new Plan();
    List<Plan> planesFalsos = List.of(plan1, plan2);
    when(this.planServiceMock.getPlansByUserEmail(OWNER_EMAIL)).thenReturn(planesFalsos);

    ModelAndView vista = this.controller.listPlans(this.authentication);

    assertThat(vista.getViewName(), is(equalTo("pages/plans/list")));
    assertThat(vista.getModel().get("plans"), is(equalTo(planesFalsos)));
  }

  @Test
  public void T_PLN_002_planCreate_retornaFormulario() {
    ModelAndView vista = this.controller.showNewPlanForm();

    assertThat(vista.getViewName(), is(equalTo("pages/plans/new")));
    assertThat(vista.getModel().get("plan"), is(instanceOf(PlanRequest.class)));
  }

  @Test
  public void T_PLN_003_planGenerate_creaYRedirige() throws Exception {
    this.mockMvc.perform(post("/plans").param("name", "Viaje a Bariloche"))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrlPattern("/plans/*"));

    verify(this.planServiceMock, times(1)).createPlan(any(Plan.class), anyString());
  }

  @Test
  public void T_PLN_004_planDetail_idValido_muestraDetalle() {
    Plan plan = new Plan();
    plan.setName("Viaje a Bariloche");
    when(this.planServiceMock.getOwnedPlan(1L, OWNER_EMAIL)).thenReturn(plan);

    ModelAndView vista = this.controller.showPlan(1L, this.authentication);

    assertThat(vista.getViewName(), is(equalTo("pages/plans/detail")));
    assertThat(vista.getModel().get("plan"), is(equalTo(plan)));
  }

  @Test
  public void T_PLN_004_planDetail_idInvalido_noEncontrado() {
    when(this.planServiceMock.getOwnedPlan(999L, OWNER_EMAIL))
      .thenThrow(new PlanNotFoundException());

    assertThrows(
      PlanNotFoundException.class,
      () -> this.controller.showPlan(999L, this.authentication)
    );
  }

  @Test
  public void T_PLN_005_planDelete_borraYRedirige() {
    ModelAndView vista = this.controller.deletePlan(1L, this.authentication);

    verify(this.planServiceMock, times(1)).deleteOwnedPlan(anyLong(), anyString());
    assertThat(vista.getViewName(), is(equalTo("redirect:/plans")));
  }

  @Test
  public void T_PLN_006_planUpdate_actualizaYRedirige() {
    PlanRequest form = new PlanRequest();
    BindingResult errores = new BeanPropertyBindingResult(form, "plan");

    ModelAndView vista = this.controller.updatePlan(1L, form, errores, this.authentication);

    verify(this.planServiceMock, times(1)).updatePlan(anyLong(), any(Plan.class), anyString());
    assertThat(vista.getViewName(), is(equalTo("redirect:/plans/1")));
  }

  @Test
  public void T_PLN_006_planUpdate_conErrores_muestraForm() {
    PlanRequest form = new PlanRequest();
    BindingResult errores = new BeanPropertyBindingResult(form, "plan");
    errores.reject("invalido");

    ModelAndView vista = this.controller.updatePlan(1L, form, errores, this.authentication);

    verify(this.planServiceMock, never()).updatePlan(anyLong(), any(Plan.class), anyString());
    assertThat(vista.getViewName(), is(equalTo("pages/plans/new")));
  }
}
