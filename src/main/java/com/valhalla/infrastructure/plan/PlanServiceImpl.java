package com.valhalla.infrastructure.plan;

import com.valhalla.domain.exception.PlanNotFoundException;
import com.valhalla.domain.exception.UserNotFoundException;
import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanRepository;
import com.valhalla.domain.plan.PlanService;
import com.valhalla.domain.user.User;
import com.valhalla.domain.user.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lives in infrastructure, not domain: the domain package must not depend on the framework. */
@Service
@Transactional
public class PlanServiceImpl implements PlanService {

  private static final int SHORT_CODE_LENGTH = 8;
  private static final int SHORT_CODE_MAX_ATTEMPTS = 10;

  private final PlanRepository planRepository;
  private final UserRepository userRepository;

  @Autowired
  public PlanServiceImpl(PlanRepository planRepository, UserRepository userRepository) {
    this.planRepository = planRepository;
    this.userRepository = userRepository;
  }

  @Override
  public Plan createPlan(Plan plan, String ownerEmail) {
    User owner = userRepository.findByEmail(ownerEmail).orElseThrow(UserNotFoundException::new);
    plan.setAdministrator(owner);
    // Always generated: the share code is the backend's, the form never asks for it.
    plan.setShortCode(generateUniqueShortCode());
    return planRepository.save(plan);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Plan> getPlansByUserEmail(String ownerEmail) {
    return userRepository
      .findByEmail(ownerEmail)
      .map(user -> planRepository.findByAdministratorId(user.getId()))
      .orElse(List.of());
  }

  @Override
  @Transactional(readOnly = true)
  public Plan getOwnedPlan(Long id, String ownerEmail) {
    Plan plan = planRepository.findById(id).orElseThrow(PlanNotFoundException::new);
    if (plan.getAdministrator() == null || !ownerEmail.equals(plan.getAdministrator().getEmail())) {
      // A plan owned by somebody else answers as missing: "forbidden" would confirm the id exists.
      throw new PlanNotFoundException();
    }
    return plan;
  }

  @Override
  public Plan updatePlan(Long id, Plan changes, String ownerEmail) {
    Plan existing = getOwnedPlan(id, ownerEmail);
    existing.updateFrom(changes);
    return planRepository.save(existing);
  }

  @Override
  public void deleteOwnedPlan(Long id, String ownerEmail) {
    getOwnedPlan(id, ownerEmail);
    planRepository.deleteById(id);
  }

  /**
   * The unique constraint on {@code shortCode} is the real guard; this loop only narrows the race
   * window down to that constraint. Ten collisions on 8 characters are already far past the odds,
   * so running out of attempts means something is broken, not unlucky.
   */
  private String generateUniqueShortCode() {
    for (int attempt = 0; attempt < SHORT_CODE_MAX_ATTEMPTS; attempt++) {
      String candidate = UUID
        .randomUUID()
        .toString()
        .substring(0, SHORT_CODE_LENGTH)
        .toUpperCase(Locale.ROOT);
      if (!planRepository.existsByShortCode(candidate)) {
        return candidate;
      }
    }
    throw new IllegalStateException("Could not generate a unique plan short code");
  }
}
