package com.valhalla.domain.place;

import java.util.List;

public interface MapPoiRepository {
  /**
   * Todos los POIs guardados de ese tag cuya posicion cae en la caja, sin mirar el estado: el
   * importador necesita ver tambien los {@code REMOVED} para reactivarlos, y filtrar los activos es
   * una decision de la capa que lee.
   */
  List<MapPoi> findInBox(String tagKey, String tagValue, BoundingBox box);

  void saveAll(List<MapPoi> pois);
}
