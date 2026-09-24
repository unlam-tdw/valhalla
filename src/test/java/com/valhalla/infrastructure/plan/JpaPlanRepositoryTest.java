package com.valhalla.infrastructure.plan;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.user.User;
import com.valhalla.infrastructure.user.JpaUserRepository;
import com.valhalla.integration.JpaIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@JpaIntegrationTest
public class JpaPlanRepositoryTest {

  @Autowired
  private JpaPlanRepository planRepository;

  @Autowired
  private JpaUserRepository userRepository;

  @Test
  @Transactional
  @Rollback
  public void T_PLN_013_save_guardaYAsignaId() {
    Plan plan = new Plan();
    plan.setName("Viaje a Bariloche");
    plan.setCodigo("ABC12345");

    Plan guardado = this.planRepository.save(plan);

    assertThat(guardado.getIdPlan(), is(notNullValue()));
  }

  @Test
  @Transactional
  @Rollback
  public void T_PLN_013_findByCodigo_existente() {
    Plan plan = new Plan();
    plan.setName("Viaje a Bariloche");
    plan.setCodigo("XYZ78901");
    this.planRepository.save(plan);

    Plan encontrado = this.planRepository.findByCodigo("XYZ78901");

    assertThat(encontrado, is(notNullValue()));
    assertThat(encontrado.getName(), is(equalTo("Viaje a Bariloche")));
  }

  @Test
  @Transactional
  public void T_PLN_013_findByCodigo_inexistente() {
    Plan encontrado = this.planRepository.findByCodigo("NOEXISTE");

    assertThat(encontrado, is(nullValue()));
  }

  @Test
  @Transactional
  @Rollback
  public void T_PLN_013_delete_borraElPlan() {
    Plan plan = new Plan();
    plan.setName("Plan a borrar");
    plan.setCodigo("DEL12345");
    Plan guardado = this.planRepository.save(plan);

    this.planRepository.deleteById(guardado.getIdPlan());

    assertThat(this.planRepository.findById(guardado.getIdPlan()).isPresent(), is(false));
  }

  @Test
  @Transactional
  @Rollback
  public void T_PLN_013_findByAdministratorId_devuelveSoloLosDeEseUsuario() {
    User user1 = new User();
    user1.setEmail("user1@test.com");
    user1 = this.userRepository.save(user1);

    User user2 = new User();
    user2.setEmail("user2@test.com");
    user2 = this.userRepository.save(user2);

    Plan planDeUser1 = new Plan();
    planDeUser1.setName("Plan de user1");
    planDeUser1.setCodigo("USR00001");
    planDeUser1.setAdministrator(user1);
    this.planRepository.save(planDeUser1);

    Plan planDeUser2 = new Plan();
    planDeUser2.setName("Plan de user2");
    planDeUser2.setCodigo("USR00002");
    planDeUser2.setAdministrator(user2);
    this.planRepository.save(planDeUser2);

    List<Plan> resultado = this.planRepository.findByAdministratorId(user1.getId());

    assertThat(resultado.size(), is(equalTo(1)));
    assertThat(resultado.get(0).getName(), is(equalTo("Plan de user1")));
  }
}