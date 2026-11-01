package com.valhalla.domain.notification;

/**
 * [NOTIF] Tipos de evento que el centro de notificaciones sabe contar. Cada tipo define la frase
 * que ve el usuario y el destino del link, no una lógica propia: las reglas viven en
 * {@link NotificationService}.
 */
public enum NotificationType {
  /** Un usuario empezó a seguir al destinatario. */
  NEW_FOLLOWER,

  /** Un usuario se sumó como participante a un plan cuyo dueño es el destinatario. */
  NEW_PARTICIPANT,

  /** Un usuario seguido publicó un plan nuevo. */
  NEW_PLAN,

  /** Un usuario invitó al destinatario a un plan (decisión de producto, ver 25-NOTIF). */
  PLAN_INVITATION,

  /** Un usuario pidió unirse al plan cuyo dueño es el destinatario. */
  JOIN_REQUESTED,

  /** El dueño aprobó la solicitud de unión del destinatario. */
  JOIN_REQUEST_ACCEPTED,

  /** El dueño rechazó la solicitud de unión del destinatario. */
  JOIN_REQUEST_REJECTED,
}
