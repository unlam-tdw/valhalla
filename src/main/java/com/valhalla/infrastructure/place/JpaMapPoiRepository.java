package com.valhalla.infrastructure.place;

import com.valhalla.domain.place.MapPoi;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaMapPoiRepository extends JpaRepository<MapPoi, UUID> {
  /**
   * Lee por caja. {@code between} compara con {@code >= / <=} y los cuatro bordes son inclusivos:
   * un POI justo en el borde de la zona no se pierde al mover el mapa un pixel.
   */
  @Query(
    "select poi from MapPoi poi where poi.tagKey = :tagKey and poi.tagValue = :tagValue" +
    " and poi.latitude between :south and :north" +
    " and poi.longitude between :west and :east"
  )
  List<MapPoi> findInBox(
    @Param("tagKey") String tagKey,
    @Param("tagValue") String tagValue,
    @Param("south") double south,
    @Param("west") double west,
    @Param("north") double north,
    @Param("east") double east
  );
}
