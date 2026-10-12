package com.valhalla.infrastructure.plan;

import com.valhalla.domain.plan.Plan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaPlanRepository extends JpaRepository<Plan, Long> {
  List<Plan> findByAdministratorId(Long administratorId);

  @Query(
    "select distinct plan from Plan plan join plan.planManager.participants participant where participant.email = :email"
  )
  List<Plan> findByParticipantsEmail(@Param("email") String email);

  @Query(
    "select distinct plan from Plan plan join plan.planManager.invitations invitation where invitation.recipient.email = :email"
  )
  List<Plan> findByInvitationsRecipientEmail(@Param("email") String email);

  Optional<Plan> findByShortCode(String shortCode);
  boolean existsByShortCode(String shortCode);
}
