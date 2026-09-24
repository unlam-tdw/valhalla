package com.valhalla.presentation.plan;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

public class PlanControllerTest {

  private PlanController controller;
  private PlanService planServiceMock;
  private Model model;

  @BeforeEach
  public void init() {
    this.planServiceMock = mock(PlanService.class);
    this.controller = new PlanController(this.planServiceMock);
    this.model = new ExtendedModelMap();
  }

  @Test
  public void T_PLN_001_planList_retornaPlanes() {
    Plan plan1 = new Plan();
    Plan plan2 = new Plan();
    List<Plan> planesFalsos = List.of(plan1, plan2);
    when(this.planServiceMock.getAllPlans()).thenReturn(planesFalsos);

    String vista = this.controller.planList(this.model);

    assertThat(vista, is(equalTo("pages/plans/list")));
    assertThat(this.model.getAttribute("plans"), is(equalTo(planesFalsos)));
  }

  @Test
  public void T_PLN_002_planCreate_retornaFormulario() {
    String vista = this.controller.planCreate(this.model);

    assertThat(vista, is(equalTo("pages/plans/create")));
    assertThat(this.model.getAttribute("plan"), is(instanceOf(Plan.class)));
  }

  @Test
  public void T_PLN_003_planGenerate_creaYRedirige() {
    Plan plan = new Plan();
    plan.setName("Viaje a Bariloche");

    String vista = this.controller.planGenerate(plan);

    verify(this.planServiceMock, times(1)).createPlan(plan);
    assertThat(vista, is(equalTo("redirect:/planes")));
  }

  @Test
  public void T_PLN_004_planDetail_idValido_muestraDetalle() {
    Plan plan = new Plan();
    plan.setName("Viaje a Bariloche");
    when(this.planServiceMock.getPlanById(1L)).thenReturn(Optional.of(plan));

    String vista = this.controller.planDetail(1L, this.model);

    assertThat(vista, is(equalTo("pages/plans/detail")));
    assertThat(this.model.getAttribute("plan"), is(equalTo(plan)));
  }

  @Test
  public void T_PLN_004_planDetail_idInvalido_noEncontrado() {
    when(this.planServiceMock.getPlanById(999L)).thenReturn(Optional.empty());

    assertThrows(IllegalArgumentException.class, () -> this.controller.planDetail(999L, this.model));
  }

  @Test
  public void T_PLN_005_planDelete_borraYRedirige() {
    String vista = this.controller.planDelete(1L);
    verify(this.planServiceMock, times(1)).deletePlan(1L);
    assertThat(vista, is(equalTo("redirect:/planes")));
  }
}