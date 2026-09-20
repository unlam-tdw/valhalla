package com.valhalla.domain.place;

import java.util.List;
import java.util.Optional;

public interface PlaceRepository {
  List<Place> findAll();
  Optional<Place> findById(Long id);
  List<Place> findByCategory(String category);
  List<Place> findByNameContainingIgnoreCase(String name);
  void save(Place place);
  long count();
}
