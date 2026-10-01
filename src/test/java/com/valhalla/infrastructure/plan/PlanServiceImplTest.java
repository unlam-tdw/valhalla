package com.valhalla.infrastructure.plan;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.valhalla.domain.exception.PlanNotFoundException;
import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanRepository;
import com.valhalla.domain.user.User;
import com.valhalla.domain.user.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PlanServiceImplTest {

  private static final String OWNER_EMAIL = "user@test.com";

  private PlanServiceImpl planService;
  private PlanRepository planRepositoryMock;
  private UserRepository userRepositoryMock;

  @BeforeEach
  public void init() {
    this.planRepositoryMock = mock(PlanRepository.class);
    this.userRepositoryMock = mock(UserRepository.class);
    this.planService = new PlanServiceImpl(this.planRepositoryMock, this.userRepositoryMock);
  }

  @Test
  public void T_PLN_006_createPlan_generaShortCode() {
    when(this.userRepositoryMock.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner()));
    Plan plan = new Plan();
    plan.setName("Viaje a Bariloche");
    when(this.planRepositoryMock.save(any(Plan.class))).thenReturn(plan);

    this.planService.createPlan(plan, OWNER_EMAIL);

    assertThat(plan.getShortCode(), is(notNullValue()));
    assertThat(plan.getShortCode().length(), is(equalTo(8)));
    verify(this.planRepositoryMock, times(1)).save(plan);
  }

  @Test
  public void T_PLN_006_createPlan_respetaShortCodeExistente() {
    when(this.userRepositoryMock.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner()));
    Plan plan = new Plan();
    plan.setShortCode("MIOWN123");
    when(this.planRepositoryMock.save(any(Plan.class))).thenReturn(plan);

    this.planService.createPlan(plan, OWNER_EMAIL);

    assertThat(plan.getShortCode(), is(notNullValue()));
  }

  @Test
  public void T_PLN_007_getPlansByUserEmail_devuelveLista() {
    List<Plan> resultado = this.planService.getPlansByUserEmail(OWNER_EMAIL);

    assertThat(resultado, is(empty()));
  }

  @Test
  public void T_PLN_007_getOwnedPlan_existente() {
    Plan plan = ownedPlan();
    when(this.planRepositoryMock.findById(1L)).thenReturn(Optional.of(plan));

    Plan resultado = this.planService.getOwnedPlan(1L, OWNER_EMAIL);

    assertThat(resultado, is(equalTo(plan)));
  }

  @Test
  public void T_PLN_007_getOwnedPlan_inexistente() {
    when(this.planRepositoryMock.findById(999L)).thenReturn(Optional.empty());

    assertThrows(
      PlanNotFoundException.class,
      () -> this.planService.getOwnedPlan(999L, OWNER_EMAIL)
    );
  }

  @Test
  public void T_PLN_008_updatePlan_guarda() {
    Plan plan = ownedPlan();
    when(this.planRepositoryMock.findById(1L)).thenReturn(Optional.of(plan));

    this.planService.updatePlan(1L, new Plan(), OWNER_EMAIL);

    verify(this.planRepositoryMock, times(1)).save(plan);
  }

  @Test
  public void T_PLN_008_deleteOwnedPlan_borraPorId() {
    when(this.planRepositoryMock.findById(1L)).thenReturn(Optional.of(ownedPlan()));

    this.planService.deleteOwnedPlan(1L, OWNER_EMAIL);

    verify(this.planRepositoryMock, times(1)).deleteById(1L);
  }

  private User owner() {
    User user = new User();
    user.setEmail(OWNER_EMAIL);
    return user;
  }

  private Plan ownedPlan() {
    Plan plan = new Plan();
    plan.setId(1L);
    plan.setAdministrator(owner());
    return plan;
  }
}
