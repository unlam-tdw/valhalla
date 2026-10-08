package com.valhalla.presentation.place;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.place.PlaceCategory;
import com.valhalla.domain.place.PlaceService;
import com.valhalla.domain.plan.PlanService;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/explore")
public class PlaceController {

  private static final String VIEW_LIST = "pages/places/list";
  private static final String ATTR_PLACES = "places";

  private final PlaceService placeService;

  private final PlanService planService;

  @Autowired
  public PlaceController(PlaceService placeService, PlanService planService) {
    this.placeService = placeService;
    this.planService = planService;
  }

  @GetMapping
  public ModelAndView listPlaces(
    @RequestParam(required = false) String category,
    @RequestParam(required = false) String search,
    Principal principal
  ) {
    Map<String, Object> model = new ModelMap();
    model.put(ATTR_PLACES, getPlaces(category, search));
    if (principal != null) {
      model.put(
        "userPlans",
        planService
          .getPlansByUserEmail(principal.getName())
          .stream()
          .sorted(java.util.Comparator.comparing(com.valhalla.domain.plan.Plan::getId).reversed())
          .toList()
      );
    }
    model.put("categories", PlaceCategory.values());
    model.put("category", category);
    model.put("search", search);
    return new ModelAndView(VIEW_LIST, model);
  }

  private List<Place> getPlaces(String category, String search) {
    if (category != null && !category.isBlank()) {
      return PlaceCategory
        .fromCode(category)
        .map(placeCategory ->
          search != null && !search.isBlank()
            ? placeService.searchPlaces(search, placeCategory)
            : placeService.getPlacesByCategory(placeCategory)
        )
        .orElseGet(List::of);
    }
    if (search != null && !search.isBlank()) {
      return placeService.searchPlaces(search);
    }
    return placeService.getAllPlaces();
  }
}
