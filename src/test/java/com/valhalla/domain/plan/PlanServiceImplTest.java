package com.valhalla.domain.plan;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.valhalla.infrastructure.plan.JpaPlanRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PlanServiceImplTest {

  private PlanServiceImpl planService;
  private JpaPlanRepository planRepositoryMock;

  @BeforeEach
  public void init() {
    this.planRepositoryMock = mock(JpaPlanRepository.class);
    this.planService = new PlanServiceImpl(this.planRepositoryMock);
  }

  @Test
  public void T_PLN_006_createPlan_generaCodigo() {
    Plan plan = new Plan();
    plan.setName("Viaje a Bariloche");
    when(this.planRepositoryMock.save(any(Plan.class))).thenReturn(plan);

    this.planService.createPlan(plan);

    assertThat(plan.getCodigo(), is(notNullValue()));
    assertThat(plan.getCodigo().length(), is(equalTo(8)));
    verify(this.planRepositoryMock, times(1)).save(plan);
  }

  @Test
  public void T_PLN_006_createPlan_respetaCodigoExistente() {
    Plan plan = new Plan();
    plan.setCodigo("MIOWN123");
    when(this.planRepositoryMock.save(any(Plan.class))).thenReturn(plan);

    this.planService.createPlan(plan);

    assertThat(plan.getCodigo(), is(equalTo("MIOWN123")));
  }

  @Test
  public void T_PLN_007_getPlansByUserEmail_devuelveLista() {
    List<Plan> resultado = this.planService.getPlansByUserEmail("user@test.com");

    assertThat(resultado, is(empty()));
  }

  @Test
  public void T_PLN_007_getPlanById_existente() {
    Plan plan = new Plan();
    when(this.planRepositoryMock.findById(1L)).thenReturn(Optional.of(plan));

    Optional<Plan> resultado = this.planService.getPlanById(1L);

    assertThat(resultado.isPresent(), is(true));
    assertThat(resultado.get(), is(equalTo(plan)));
  }

  @Test
  public void T_PLN_007_getPlanById_inexistente() {
    when(this.planRepositoryMock.findById(999L)).thenReturn(Optional.empty());

    Optional<Plan> resultado = this.planService.getPlanById(999L);

    assertThat(resultado.isPresent(), is(false));
  }

  @Test
  public void T_PLN_008_updatePlan_guarda() {
    Plan plan = new Plan();
    plan.setIdPlan(1L);

    this.planService.updatePlan(plan);

    verify(this.planRepositoryMock, times(1)).save(plan);
  }

  @Test
  public void T_PLN_008_deletePlan_borraPorId() {
    this.planService.deletePlan(1L);

    verify(this.planRepositoryMock, times(1)).deleteById(1L);
  }
}