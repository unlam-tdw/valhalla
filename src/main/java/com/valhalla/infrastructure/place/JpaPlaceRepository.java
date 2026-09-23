package com.valhalla.infrastructure.place;

import com.valhalla.domain.place.Place;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPlaceRepository extends JpaRepository<Place, Long> {
  List<Place> findByCategory(String category);
  List<Place> findByNameContainingIgnoreCase(String name);
}
