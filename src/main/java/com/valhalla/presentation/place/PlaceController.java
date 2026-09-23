package com.valhalla.presentation.place;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.place.PlaceService;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/places")
public class PlaceController {

  private static final String VIEW_LIST = "pages/places/list";
  private static final String VIEW_DETAIL = "pages/places/detail";
  private static final String ATTR_PLACES = "places";
  private static final String ATTR_PLACE = "place";

  private final PlaceService placeService;

  @Autowired
  public PlaceController(PlaceService placeService) {
    this.placeService = placeService;
  }

  @GetMapping
  public ModelAndView listPlaces(
    @RequestParam(required = false) String category,
    @RequestParam(required = false) String search
  ) {
    Map<String, Object> model = new ModelMap();
    model.put(ATTR_PLACES, getPlaces(category, search));
    return new ModelAndView(VIEW_LIST, model);
  }

  @GetMapping("/{id}")
  public ModelAndView placeDetail(@PathVariable Long id) {
    Map<String, Object> model = new ModelMap();
    placeService.getPlaceById(id).ifPresent(place -> model.put(ATTR_PLACE, place));
    return new ModelAndView(VIEW_DETAIL, model);
  }

  private List<Place> getPlaces(String category, String search) {
    if (category != null && !category.isBlank()) {
      return placeService.getPlacesByCategory(category);
    }
    if (search != null && !search.isBlank()) {
      return placeService.searchPlaces(search);
    }
    return placeService.getAllPlaces();
  }
}
