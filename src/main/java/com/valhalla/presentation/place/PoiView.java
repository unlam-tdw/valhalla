package com.valhalla.presentation.place;

import com.valhalla.domain.place.MapPoi;
import com.valhalla.domain.place.PlaceCategory;
import java.util.UUID;

/**
 * Forma de cable del importador: lo que el navegador recibe por cada punto del sobre.
 *
 * <p>No reusa {@link com.valhalla.domain.place.OsmPoi} porque ese record es el fetch crudo de
 * Overpass y no tiene ni la categoria ni la identidad guardada: esos dos datos viven en la fila
 * {@link MapPoi}, que es la que decide de que color va el marcador y si el punto ya es un lugar.
 *
 * <p>{@code category} viaja como el nombre del enum (serializacion por defecto de Jackson), que es
 * lo que el JS del mapa usa para elegir el color. {@code externalId} es {@code null} cuando la fila
 * no trae id de OSM y su clave cayo al fallback: sin identidad no hay nada que mandarle al alta.
 *
 * @param name nombre del punto
 * @param address direccion, si OSM la trae
 * @param latitude latitud del centro del elemento
 * @param longitude longitud del centro del elemento
 * @param openingHours horarios, si OSM los trae
 * @param phone telefono, si OSM lo trae
 * @param website sitio web, si OSM lo trae
 * @param category categoria guardada: es de donde sale el color del marcador
 * @param externalId identidad del POI de origen ({@code osm:way/1}), o {@code null} si no la tiene
 * @param savedPlaceId id del lugar que ya representa a este punto, o {@code null} si no existe
 */
public record PoiView(
  String name,
  String address,
  Double latitude,
  Double longitude,
  String openingHours,
  String phone,
  String website,
  PlaceCategory category,
  String externalId,
  UUID savedPlaceId
) {
  /** Prefijo de las claves que si son una identidad de OSM; el resto es el fallback. */
  private static final String OSM_PREFIX = "osm:";

  /**
   * Arma la vista de una fila guardada.
   *
   * @param savedPlaceId lugar que ya representa a ese POI, o {@code null} si todavia no se guardo
   */
  public static PoiView from(MapPoi poi, UUID savedPlaceId) {
    return new PoiView(
      poi.getName(),
      poi.getAddress(),
      poi.getLatitude(),
      poi.getLongitude(),
      poi.getOpeningHours(),
      poi.getPhone(),
      poi.getWebsite(),
      poi.getCategory(),
      externalIdOf(poi),
      savedPlaceId
    );
  }

  /** La clave de la fila solo es identidad si vino de un elemento de OSM. */
  private static String externalIdOf(MapPoi poi) {
    String sourceKey = poi.getSourceKey();
    return sourceKey.startsWith(OSM_PREFIX) ? sourceKey : null;
  }
}
