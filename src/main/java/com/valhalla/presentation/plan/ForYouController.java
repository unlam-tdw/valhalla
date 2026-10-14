package com.valhalla.presentation.plan;

import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanService;
import com.valhalla.domain.user.User;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * [PPV] Feed "Para vos" en {@code GET /for-you}: planes publicos para descubrir, con o sin sesion.
 *
 * <p>Esta ruta vive fuera de {@code PlanController} porque ese controller esta amarrado a
 * {@code @RequestMapping("/plans")} y el feed ya no es una subruta de /plans. Sin sesion se muestran
 * todos los planes publicos (el filtro de propios no aplica: el anonimo no tiene planes propios);
 * con sesion los propios quedan fuera, porque el feed es para descubrir planes de otros.
 */
@Controller
public class ForYouController {

  private static final String VIEW_PUBLIC_LIST = "pages/plans/public-list";
  private static final String ATTR_PUBLIC_PLANS = "publicPlans";

  /** Cantidad de covers genericos en /images/plans: el id del plan elige uno, deterministico. */
  private static final int COVER_COUNT = 6;

  private final PlanService planService;

  @Autowired
  public ForYouController(PlanService planService) {
    this.planService = planService;
  }

  @GetMapping("/for-you")
  public ModelAndView forYou(Authentication authentication) {
    // Con anonymousAuthenticationFilter encendido el principal es un AnonymousAuthenticationToken
    // (nombre "anonymousUser"), no null: ningun plan tiene ese administrador, asi que el filtro de
    // propios deja pasar todo igual. El corte por null cubre el caso sin anonymous habilitado.
    String currentEmail = authentication == null ? null : authentication.getName();
    Map<String, Object> model = new ModelMap();
    model.put(
      ATTR_PUBLIC_PLANS,
      planService
        .getPublicPlans()
        .stream()
        .filter(plan -> currentEmail == null || !plan.isAdministrator(currentEmail))
        .map(ForYouController::planCard)
        .toList()
    );
    return new ModelAndView(VIEW_PUBLIC_LIST, model);
  }

  /**
   * [PPV, AC-03/AC-09] Solo campos publicos de la tarjeta: nombre, descripcion, fecha, cantidad de
   * lugares del itinerario y nombre del administrador. Ni shortCode, ni emails, ni participantes.
   * {@code cover} es la imagen generica del plan (hashCode del id % COVER_COUNT); a futuro, una
   * columna {@code imageUrl} en Plan la reemplaza.
   */
  private static Map<String, Object> planCard(Plan plan) {
    Map<String, Object> card = new LinkedHashMap<>();
    card.put("id", plan.getId());
    card.put("name", plan.getName());
    card.put("description", plan.getDescription());
    card.put("eventDate", plan.getEventDate() == null ? null : plan.getEventDate().toString());
    card.put("placeCount", plan.getPlanPlaces().size());
    card.put("administratorName", feedDisplayName(plan.getAdministrator()));
    card.put(
      "cover",
      "/images/plans/cover-" + (Math.floorMod(plan.getId().hashCode(), COVER_COUNT) + 1) + ".svg"
    );
    return card;
  }

  /**
   * [PPV] Nombre visible del administrador en el feed. Sin nombre registrado queda vacio y la vista
   * oculta la linea: AC-09 prohibe exponer emails en el listado, a diferencia del helper de la
   * vista publica del plan, que usa el email como ultimo recurso.
   */
  private static String feedDisplayName(User user) {
    if (user == null) {
      return "";
    }
    String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
    String last = user.getLastName() == null ? "" : user.getLastName().trim();
    return (first + " " + last).trim();
  }
}
