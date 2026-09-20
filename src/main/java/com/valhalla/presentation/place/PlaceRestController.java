package com.valhalla.presentation.place;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.place.PlaceService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/places")
public class PlaceRestController {

  private final PlaceService placeService;

  @Autowired
  public PlaceRestController(PlaceService placeService) {
    this.placeService = placeService;
  }

  @GetMapping
  public List<Place> listPlaces(
    @RequestParam(required = false) String category,
    @RequestParam(required = false) String search
  ) {
    if (category != null && !category.isBlank()) {
      return placeService.getPlacesByCategory(category);
    }
    if (search != null && !search.isBlank()) {
      return placeService.searchPlaces(search);
    }
    return placeService.getAllPlaces();
  }
}
