package com.valhalla.presentation.prototype;

import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * [PROTOTYPE] Ruta tiradera del prototipo de UI "¿cómo se siente reordenar el itinerario con
 * drag & drop?". Tres variantes de interaccion conviven en esta unica ruta, elegibles con
 * ?variant=a|b|c (ver pages/prototype/reorder.html y resources/core/js/pages/prototype-reorder.js).
 *
 * <p>Descartable por diseno: cuando haya veredicto, el ganador se reimplementa bien dentro de la
 * vista real del detalle del plan y esta ruta, su template y su modulo JS se borran. Nunca debe
 * llegar a main.
 */
@Controller
public class PrototypeReorderController {

  @GetMapping("/prototype/reorder")
  public ModelAndView reorder() {
    Map<String, Object> model = new ModelMap();
    model.put("pageTitle", "Prototype · Reordenar el itinerario");
    return new ModelAndView("pages/prototype/reorder", model);
  }
}
