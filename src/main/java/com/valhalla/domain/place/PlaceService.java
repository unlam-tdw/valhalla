package com.valhalla.domain.place;

import com.valhalla.presentation.place.EditPlaceRequest;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface PlaceService {
  List<Place> getAllPlaces();

  Optional<Place> getPlaceById(UUID id);

  List<Place> getPlacesByCategory(PlaceCategory category);

  List<Place> searchPlaces(String query);

  List<Place> searchPlaces(String query, PlaceCategory category);

  /**
   * Cuales de esos POIs ya son un lugar guardado.
   *
   * <p>Es la consulta que le dice al importador que puntos ya estan dados de alta. Existe para que
   * el sobre no dispare una consulta por punto: la vista puede mostrar hasta 300, y una por punto
   * seria 300 viajes a la base para pintar una sola pantalla.
   *
   * @param externalIds identidades de POI ({@code osm:<tipo>/<id>}); las vacias se ignoran
   * @return identidad a id del lugar guardado, solo para las que existen
   */
  Map<String, UUID> savedPlaceIds(Collection<String> externalIds);

  Place create(EditPlaceRequest request);

  Place update(UUID id, EditPlaceRequest request);

  void delete(UUID id);
}
