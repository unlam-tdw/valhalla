package com.valhalla.infrastructure.plan;

import com.valhalla.domain.plan.Plan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPlanRepository extends JpaRepository<Plan, Long> {
    List<Plan> findByUserId(Long userId);
    Plan findByShortCode(String shortCode);
}
