package com.valhalla.domain.place;

import java.time.Instant;
import java.util.List;

/**
 * Lo que el importador le devuelve al navegador: los POIs que hay en el area visible y cuando se
 * cargo por ultima vez la zona que los cubre.
 *
 * <p>{@code fetchedAt} en {@code null} no significa "no hay puntos": significa que esa zona nunca se
 * cargo, que es distinto de haberse cargado y no haber traido nada.
 */
public record ZonePois(List<OsmPoi> pois, Instant fetchedAt) {}
