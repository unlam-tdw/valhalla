package com.valhalla.domain.place;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlaceRepository {
  List<Place> findAll();

  Optional<Place> findById(UUID id);

  /**
   * Fila que representa al POI de origen, si ya se importo alguna vez.
   *
   * @param externalId identidad del POI de origen, con la forma {@code osm:<tipo>/<id>}
   * @return el lugar ya guardado con esa identidad, o vacio si nunca se importo
   */
  Optional<Place> findByExternalId(String externalId);

  List<Place> findByCategory(PlaceCategory category);

  List<Place> findByNameContainingIgnoreCase(String name);

  List<Place> findByCategoryAndNameContainingIgnoreCase(PlaceCategory category, String name);

  void save(Place place);

  void deleteById(UUID id);

  long count();
}
