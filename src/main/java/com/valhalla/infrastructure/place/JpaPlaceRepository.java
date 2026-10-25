package com.valhalla.infrastructure.place;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.place.PlaceCategory;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPlaceRepository extends JpaRepository<Place, UUID> {
  List<Place> findByCategory(PlaceCategory category);

  List<Place> findByNameContainingIgnoreCase(String name);

  List<Place> findByCategoryAndNameContainingIgnoreCase(PlaceCategory category, String name);

  Optional<Place> findByExternalId(String externalId);

  List<Place> findByExternalIdIn(Collection<String> externalIds);
}
