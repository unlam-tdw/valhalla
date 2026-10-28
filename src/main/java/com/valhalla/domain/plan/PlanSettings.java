package com.valhalla.domain.plan;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.Objects;

@Embeddable
public class PlanSettings {

  @Enumerated(EnumType.STRING)
  @Column(name = "handle_requests_permission")
  private PlanPermission handleRequests = PlanPermission.OWNER;

  public PlanPermission getHandleRequests() {
    return handleRequests == null ? PlanPermission.OWNER : handleRequests;
  }

  public void setHandleRequests(PlanPermission handleRequests) {
    if (handleRequests == PlanPermission.PARTICIPANTS) {
      throw new IllegalArgumentException(
        "Solo el dueño o los editores pueden gestionar solicitudes."
      );
    }
    this.handleRequests = Objects.requireNonNull(handleRequests);
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "invite_internal_permission")
  private PlanPermission inviteInternal = PlanPermission.EDITORS;

  @Enumerated(EnumType.STRING)
  @Column(name = "invite_email_permission")
  private PlanPermission inviteByEmail = PlanPermission.OWNER;

  public PlanPermission getInviteInternal() {
    return inviteInternal == null ? PlanPermission.EDITORS : inviteInternal;
  }

  public void setInviteInternal(PlanPermission permission) {
    inviteInternal = Objects.requireNonNull(permission);
  }

  public PlanPermission getInviteByEmail() {
    return inviteByEmail == null ? PlanPermission.OWNER : inviteByEmail;
  }

  public void setInviteByEmail(PlanPermission permission) {
    inviteByEmail = Objects.requireNonNull(permission);
  }

  // [SCH] Si esta activo, reordenar el itinerario recalcula los horarios de las paradas. Apagado,
  // los horarios cargados a mano se respetan tal cual.
  @Column(name = "auto_schedule")
  private Boolean autoSchedule = true;

  public Boolean getAutoSchedule() {
    return autoSchedule != null ? autoSchedule : true;
  }

  public void setAutoSchedule(Boolean autoSchedule) {
    this.autoSchedule = autoSchedule;
  }
}
