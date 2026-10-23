package com.valhalla.presentation.place;

import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * La vista de /explore. El listado lo arma el JS contra /api/places, asi que aca solo se sirve el
 * template: ni el catalogo ni los filtros viajan en el modelo.
 */
@Controller
@RequestMapping("/explore")
public class PlaceController {

  private static final String VIEW_LIST = "pages/places/list";

  @GetMapping
  public ModelAndView listPlaces() {
    Map<String, Object> model = new ModelMap();
    model.put("defaultPlanSettings", new com.valhalla.domain.plan.PlanSettings());
    // [WEA] El selector de fecha del plan no acepta días ya pasados.
    model.put("today", LocalDate.now().toString());
    return new ModelAndView(VIEW_LIST, model);
  }
}
