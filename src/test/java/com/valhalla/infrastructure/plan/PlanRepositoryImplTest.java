package com.valhalla.infrastructure.plan;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.valhalla.domain.plan.Plan;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link PlanRepositoryImpl}: every port method must reach {@link
 * JpaPlanRepository} with the same arguments. The delegation is the whole behaviour of the
 * adapter, so each test asserts the interaction on the mocked JPA repository as well as the value
 * coming back.
 */
public class PlanRepositoryImplTest {

  private JpaPlanRepository jpaPlanRepositoryMock;
  private PlanRepositoryImpl planRepository;

  @BeforeEach
  public void init() {
    this.jpaPlanRepositoryMock = mock(JpaPlanRepository.class);
    this.planRepository = new PlanRepositoryImpl(this.jpaPlanRepositoryMock);
  }

  @Test
  public void T_PLN_040_findAll_delegaEnJpaPlanRepository() {
    // given
    List<Plan> planes = List.of(plan(1L), plan(2L));
    when(this.jpaPlanRepositoryMock.findAll()).thenReturn(planes);

    // when
    List<Plan> result = this.planRepository.findAll();

    // then
    assertThat(result, is(sameInstance(planes)));
    verify(this.jpaPlanRepositoryMock, times(1)).findAll();
  }

  @Test
  public void T_PLN_041_findById_delegaEnJpaPlanRepository() {
    // given
    Plan plan = plan(1L);
    when(this.jpaPlanRepositoryMock.findById(1L)).thenReturn(Optional.of(plan));

    // when
    Optional<Plan> result = this.planRepository.findById(1L);

    // then
    assertThat(result.orElseThrow(), is(sameInstance(plan)));
    verify(this.jpaPlanRepositoryMock, times(1)).findById(1L);
  }

  @Test
  public void T_PLN_042_findByAdministratorId_delegaEnJpaPlanRepository() {
    // given
    List<Plan> planes = List.of(plan(1L));
    when(this.jpaPlanRepositoryMock.findByAdministratorId(7L)).thenReturn(planes);

    // when
    List<Plan> result = this.planRepository.findByAdministratorId(7L);

    // then
    assertThat(result, is(sameInstance(planes)));
    verify(this.jpaPlanRepositoryMock, times(1)).findByAdministratorId(7L);
  }

  @Test
  public void T_PLN_043_findByShortCode_delegaEnJpaPlanRepository() {
    // given
    Plan plan = plan(1L);
    when(this.jpaPlanRepositoryMock.findByShortCode("ABCD1234")).thenReturn(Optional.of(plan));

    // when
    Optional<Plan> result = this.planRepository.findByShortCode("ABCD1234");

    // then
    assertThat(result.orElseThrow(), is(sameInstance(plan)));
    verify(this.jpaPlanRepositoryMock, times(1)).findByShortCode("ABCD1234");
  }

  @Test
  public void T_PLN_044_existsByShortCode_delegaEnJpaPlanRepository() {
    // given
    when(this.jpaPlanRepositoryMock.existsByShortCode("ABCD1234")).thenReturn(true);

    // when
    boolean result = this.planRepository.existsByShortCode("ABCD1234");

    // then
    assertTrue(result);
    verify(this.jpaPlanRepositoryMock, times(1)).existsByShortCode("ABCD1234");
  }

  @Test
  public void T_PLN_045_save_devuelveLoQueJpaPlanRepositoryGuardo() {
    // given
    Plan plan = plan(1L);
    Plan managed = plan(1L);
    when(this.jpaPlanRepositoryMock.save(plan)).thenReturn(managed);

    // when
    Plan result = this.planRepository.save(plan);

    // then
    assertThat(result, is(sameInstance(managed)));
    verify(this.jpaPlanRepositoryMock, times(1)).save(plan);
  }

  @Test
  public void T_PLN_046_deleteById_delegaEnJpaPlanRepository() {
    // when
    this.planRepository.deleteById(1L);

    // then
    verify(this.jpaPlanRepositoryMock, times(1)).deleteById(1L);
  }

  private Plan plan(Long id) {
    Plan plan = new Plan();
    plan.setId(id);
    plan.setName("Viaje a Bariloche");
    return plan;
  }
}
