package com.valhalla.infrastructure.plan;

import com.valhalla.domain.plan.Plan;
import java.util.List;
//import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPlanRepository extends JpaRepository<Plan, Long> {
  List<Plan> findByAdministratorId(Long administratorId);
  Plan findByCodigo(String codigo);
}
