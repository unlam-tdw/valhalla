package com.valhalla.presentation.landing;

import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Home pública de PlanIt. La ruta ya estaba expuesta en "/": lo único que aporta esta clase es
 * el modelo que la vista necesita para dibujar la landing (estado de sesión, metadatos de SEO y el
 * itinerario de ejemplo del hero).
 *
 * <p>El itinerario del hero es contenido editorial, no una consulta: son lugares del catálogo que
 * el seed ya carga, escritos acá para que la página no dependa de una base de datos levantada y
 * para que el copy se cambie en un solo lugar.
 */
@Controller
public class LandingController {

  private static final String VIEW_LANDING = "pages/landing";

  private static final String PAGE_TITLE =
    "PlanIt | Armá tu plan de un día y compartilo con un link";
  private static final String PAGE_DESCRIPTION =
    "PlanIt junta lugares de Buenos Aires en un itinerario con horarios y lo convierte en un link " +
    "público. Creá tu plan gratis y compartilo sin pedir cuentas.";

  /** Fila del itinerario ilustrativo: hora, nombre del lugar, categoría y dirección. */
  record HeroStop(String time, String place, String category, String address) {}

  private static final List<HeroStop> HERO_ITINERARY = List.of(
    new HeroStop("10:00", "Cafe Tortoni", "Café", "Av. de Mayo 825"),
    new HeroStop("13:30", "MALBA", "Museo", "Figueroa Alcorta 3415"),
    new HeroStop("17:30", "El Ateneo Grand Splendid", "Cultura", "Av. Santa Fe 1860")
  );

  @GetMapping("/")
  public ModelAndView landing(@AuthenticationPrincipal UserDetails userDetails) {
    Map<String, Object> model = new ModelMap();
    boolean authenticated = userDetails != null;
    model.put("authenticated", authenticated);
    model.put(
      "isAdmin",
      authenticated &&
      userDetails.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))
    );

    // La landing es la home pública y el mensaje de SEO va acá: un buscador arma el título y la
    // descripción desde el modelo, con el texto genérico del layout como respaldo.
    model.put("pageTitle", PAGE_TITLE);
    model.put("pageDescription", PAGE_DESCRIPTION);

    model.put("heroItinerary", HERO_ITINERARY);

    return new ModelAndView(VIEW_LANDING, model);
  }
}
