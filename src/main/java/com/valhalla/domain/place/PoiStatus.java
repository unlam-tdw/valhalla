package com.valhalla.domain.place;

/**
 * Estado de un POI guardado desde Overpass.
 *
 * <p>La baja es logica a proposito: si un hipo de Overpass deja de devolver un punto, marcarlo
 * {@link #REMOVED} lo saca del mapa sin destruir la fila. Cuando vuelve, se reactiva y conserva lo
 * que ya se sabia de el.
 */
public enum PoiStatus {
  ACTIVE,
  REMOVED,
}
