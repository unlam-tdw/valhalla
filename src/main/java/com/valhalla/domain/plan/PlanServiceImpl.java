package com.valhalla.domain.plan;

import com.valhalla.domain.user.UserRepository;
import com.valhalla.infrastructure.plan.JpaPlanRepository;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PlanServiceImpl implements PlanService {

  private final JpaPlanRepository planRepository;
  private final UserRepository userRepository;

  public PlanServiceImpl(JpaPlanRepository planRepository, UserRepository userRepository) {
    this.planRepository = planRepository;
    this.userRepository = userRepository;
  }

  @Override
  public Plan createPlan(Plan plan) {
    if (plan.getCodigo() == null || plan.getCodigo().isBlank()) {
      plan.setCodigo(generateShortCode());
    }

    return planRepository.save(plan);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Plan> getPlanById(Long id) {
    return planRepository.findById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Plan> getPlanByShortCode(String shortCode) {
    return Optional.ofNullable(planRepository.findByCodigo(shortCode));
  }

  @Transactional(readOnly = true)
  @Override
  public List<Plan> getAllPlans() {
    return planRepository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Plan> getPlansByUserEmail(String email) {
    return userRepository.findByEmail(email)
            .map(user -> planRepository.findByAdministratorId(user.getId()))
            .orElse(List.of());
  }

  @Override
  public Plan updatePlan(Plan plan) {
    return planRepository.save(plan);
  }

  @Override
  public void deletePlan(Long id) {
    planRepository.deleteById(id);
  }

  @Override
  public String generateShortCode() {
    return UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
  }
}