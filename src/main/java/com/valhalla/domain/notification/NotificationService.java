package com.valhalla.domain.notification;

import com.valhalla.domain.user.User;
import java.util.List;
import java.util.UUID;

/**
 * [NOTIF] Escritura y lectura del centro de notificaciones in-app.
 *
 * <p>La creacion es un side-effect del evento que la genera (seguir, sumarse a un plan, publicar):
 * el que dispara ya tiene los usuarios resueltos y los pasa directo, sin requeryar. Nunca falla la
 * accion original: si el evento no corresponde a una notificacion (uno se sigue a si mismo no
 * aplica, es un no-op del follow), no se escribe nada.
 */
public interface NotificationService {
  /** [NOTIF] "X te empezó a seguir": el destinatario es el seguido. */
  void notifyNewFollower(User follower, User followed);

  /** "X se unió a tu plan": el destinatario es el dueño; sin dueño no hay a quién avisar. */
  void notifyNewParticipant(User participant, User owner, UUID planId, String planName);

  /** "X publicó un plan nuevo": una fila por cada seguidor del autor. */
  void notifyNewPlan(User owner, UUID planId, String planName);

  List<Notification> list(UUID recipientId, int page, int size);

  long count(UUID recipientId);

  long unreadCount(UUID recipientId);

  /** Marcar una leida: id inexistente o de otro destinatario es un no-op silencioso (AC-09). */
  void markRead(UUID recipientId, UUID notificationId);

  void markAllRead(UUID recipientId);
}
