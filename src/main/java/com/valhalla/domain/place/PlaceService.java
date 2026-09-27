package com.valhalla.domain.place;

import java.util.List;
import java.util.Optional;

public interface PlaceService {
  List<Place> getAllPlaces();

  Optional<Place> getPlaceById(Long id);

  List<Place> getPlacesByCategory(PlaceCategory category);

  List<Place> searchPlaces(String query);
}
