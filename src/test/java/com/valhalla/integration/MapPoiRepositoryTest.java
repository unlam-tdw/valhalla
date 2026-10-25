package com.valhalla.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import com.valhalla.domain.place.BoundingBox;
import com.valhalla.domain.place.MapPoi;
import com.valhalla.domain.place.MapPoiRepository;
import com.valhalla.domain.place.OsmPoi;
import com.valhalla.domain.place.PlaceCategory;
import com.valhalla.domain.place.PoiStatus;
import com.valhalla.domain.place.PoiZone;
import com.valhalla.domain.place.PoiZoneRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * A-1: los dos adaptadores del POI. Se prueba la lectura por caja (que es la que corre en cada
 * movimiento del mapa) y que la cobertura de zona se guarda y se relee por su clave natural.
 */
@WebIntegrationTest
@Transactional
public class MapPoiRepositoryTest {

  private static final BoundingBox CENTRO = new BoundingBox(-34.62, -58.45, -34.58, -58.37);
  private static final BoundingBox LEJOS = new BoundingBox(-34.70, -58.53, -34.69, -58.52);

  @Autowired
  private MapPoiRepository mapPoiRepository;

  @Autowired
  private PoiZoneRepository poiZoneRepository;

  @Test
  public void shouldReadAPoiBackWhenTheBoxContainsIt() {
    mapPoiRepository.saveAll(
      List.of(poi("way", 1L, -34.6037, -58.3816, "amenity", "restaurant", "Parrilla"))
    );

    assertThat(names(CENTRO), contains("Parrilla"));
    assertThat(names(LEJOS), is(empty()));
  }

  @Test
  public void shouldNotReturnAPoiOfAnotherTag() {
    mapPoiRepository.saveAll(
      List.of(poi("way", 1L, -34.6037, -58.3816, "amenity", "restaurant", "Parrilla"))
    );

    assertThat(mapPoiRepository.findInBox("shop", "clothes", CENTRO), is(empty()));
  }

  @Test
  public void shouldFilterOnBothCoordinates() {
    // Misma longitud, latitud fuera de la caja: cae por el filtro de latitude, no por el de longitude.
    mapPoiRepository.saveAll(List.of(poi("node", 2L, -34.70, -58.3816, "amenity", "bar", "Bar")));

    assertThat(names(CENTRO), is(empty()));
  }

  @Test
  public void shouldReadRemovedRowsTooSoTheyCanBeReactivated() {
    MapPoi removed = poi("node", 3L, -34.6037, -58.3816, "amenity", "cafe", "Cafe");
    removed.setStatus(PoiStatus.REMOVED);
    mapPoiRepository.saveAll(List.of(removed));

    assertThat(
      mapPoiRepository.findInBox("amenity", "cafe", CENTRO).stream().map(MapPoi::getName).toList(),
      contains("Cafe")
    );
  }

  @Test
  public void shouldFindTheZoneByItsNaturalKey() {
    assertThat(poiZoneRepository.find("zona", "amenity", "restaurant"), is(Optional.empty()));

    PoiZone zone = new PoiZone(
      "zona",
      "amenity",
      "restaurant",
      Instant.parse("2026-01-01T10:00:00Z")
    );
    poiZoneRepository.save(zone);

    assertThat(
      poiZoneRepository.find("zona", "amenity", "restaurant").map(PoiZone::getFetchedAt),
      is(Optional.of(Instant.parse("2026-01-01T10:00:00Z")))
    );
  }

  private List<String> names(BoundingBox box) {
    return mapPoiRepository
      .findInBox("amenity", "restaurant", box)
      .stream()
      .map(MapPoi::getName)
      .toList();
  }

  private static MapPoi poi(
    String type,
    Long id,
    double latitude,
    double longitude,
    String tagKey,
    String tagValue,
    String name
  ) {
    OsmPoi poi = new OsmPoi(
      name,
      null,
      latitude,
      longitude,
      null,
      null,
      null,
      tagKey,
      tagValue,
      type,
      id
    );
    return MapPoi.from(poi, PlaceCategory.fromOsm(tagKey, tagValue), tagKey, tagValue);
  }
}
