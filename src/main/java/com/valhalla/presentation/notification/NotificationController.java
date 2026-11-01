package com.valhalla.presentation.notification;

import com.valhalla.domain.notification.NotificationService;
import com.valhalla.domain.user.User;
import com.valhalla.domain.user.UserService;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * [NOTIF] Centro de notificaciones in-app: listado paginado, marcar una leida y marcar todas.
 *
 * <p>Mismo contrato de identidad que el perfil (AC-09): el destinatario sale siempre de la sesion,
 * el path solo nombra a la notificacion. La busqueda cruza id con destinatario en la capa de datos,
 * asi marcar una fila ajena es un no-op silencioso, no una filtracion.
 */
@Controller
@RequestMapping("/notifications")
public class NotificationController {

  /** Ventana fija: suficientemente grande para el scroll manual, acotada para la consulta. */
  static final int PAGE_SIZE = 10;

  private static final String VIEW_LIST = "pages/notifications/list";
  private static final String REDIRECT_LIST = "redirect:/notifications";

  private final NotificationService notificationService;
  private final UserService userService;

  @Autowired
  public NotificationController(NotificationService notificationService, UserService userService) {
    this.notificationService = notificationService;
    this.userService = userService;
  }

  @GetMapping
  public ModelAndView list(
    @RequestParam(name = "page", defaultValue = "0") int page,
    Authentication authentication
  ) {
    User me = userService.getByEmail(authentication.getName());
    int currentPage = Math.max(0, page);
    long total = notificationService.count(me.getId());
    int totalPages = (int) ((total + PAGE_SIZE - 1) / PAGE_SIZE);
    if (currentPage >= totalPages) {
      currentPage = Math.max(0, totalPages - 1);
    }
    List<?> notifications = notificationService.list(me.getId(), currentPage, PAGE_SIZE);
    ModelAndView model = new ModelAndView(VIEW_LIST);
    model.addObject("notifications", notifications);
    model.addObject("page", currentPage);
    model.addObject("hasPrev", currentPage > 0);
    model.addObject("hasNext", currentPage + 1 < totalPages);
    model.addObject("pageTitle", "Notificaciones - PlanIt");
    return model;
  }

  /**
   * [NOTIF] AC-06: marcar una leida conserva la pagina desde la que se la acciono: el form de la
   * fila lleva el numero de pagina como hidden, porque el usuario no puede caer en otra ventana.
   */
  @PostMapping("/{id}/read")
  public ModelAndView markRead(
    @PathVariable UUID id,
    @RequestParam(name = "page", defaultValue = "0") int page,
    Authentication authentication
  ) {
    User me = userService.getByEmail(authentication.getName());
    notificationService.markRead(me.getId(), id);
    return new ModelAndView(REDIRECT_LIST + "?page=" + Math.max(0, page));
  }

  /** [NOTIF] AC-06: vaciar el badge de una accion. */
  @PostMapping("/read-all")
  public ModelAndView markAllRead(Authentication authentication) {
    User me = userService.getByEmail(authentication.getName());
    notificationService.markAllRead(me.getId());
    return new ModelAndView(REDIRECT_LIST);
  }
}
