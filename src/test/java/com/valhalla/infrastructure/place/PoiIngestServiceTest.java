package com.valhalla.infrastructure.place;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.valhalla.domain.place.BoundingBox;
import com.valhalla.domain.place.MapPoi;
import com.valhalla.domain.place.MapPoiRepository;
import com.valhalla.domain.place.OsmPoi;
import com.valhalla.domain.place.PlaceCategory;
import com.valhalla.domain.place.PoiStatus;
import com.valhalla.domain.place.PoiZone;
import com.valhalla.domain.place.PoiZoneRepository;
import com.valhalla.domain.place.ZonePois;
import com.valhalla.infrastructure.place.PoiIngestService.IngestResult;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * El diff de la ingesta: es la pieza que decide si recargar una zona escribe o no, asi que se prueba
 * con los dos puertos mockeados. Nada aca toca Overpass ni la base.
 */
public class PoiIngestServiceTest {

  private static final BoundingBox VIEW = new BoundingBox(-34.62, -58.45, -34.58, -58.37);
  private static final BoundingBox ZONE = new BoundingBox(-34.62, -58.45, -34.58, -58.37);

  private static final OsmPoi PARRILLA = new OsmPoi(
    "Parrilla del Puerto",
    "Av. Corrientes 1234",
    -34.6037,
    -58.3816,
    null,
    null,
    null,
    "amenity",
    "restaurant",
    "way",
    1L
  );

  private MapPoiRepository mapPoiRepository;
  private PoiZoneRepository poiZoneRepository;
  private PoiIngestService service;

  @BeforeEach
  public void setUp() {
    this.mapPoiRepository = mock(MapPoiRepository.class);
    this.poiZoneRepository = mock(PoiZoneRepository.class);
    this.service = new PoiIngestService(mapPoiRepository, poiZoneRepository);
    when(poiZoneRepository.find(any(), any(), any())).thenReturn(Optional.empty());
  }

  @Test
  public void shouldInsertAPoiThatIsNotStoredYet() {
    when(mapPoiRepository.findInBox("amenity", "restaurant", ZONE)).thenReturn(List.of());

    IngestResult result = service.ingest("amenity", "restaurant", ZONE, List.of(PARRILLA));

    assertThat(result.created(), is(1));
    assertThat(result.updated(), is(0));
    MapPoi saved = saved().getFirst();
    assertThat(saved.getName(), is("Parrilla del Puerto"));
    assertThat(saved.getStatus(), is(PoiStatus.ACTIVE));
    assertThat(saved.getCategory(), is(PlaceCategory.RESTAURANT));
    assertThat(saved.getSourceKey(), is("osm:way/1"));
  }

  @Test
  public void shouldWriteNothingWhenTheSameDataComesBack() {
    when(mapPoiRepository.findInBox("amenity", "restaurant", ZONE)).thenReturn(List.of(stored()));

    IngestResult result = service.ingest("amenity", "restaurant", ZONE, List.of(PARRILLA));

    assertThat(result.created(), is(0));
    assertThat(result.updated(), is(0));
    assertThat(result.removed(), is(0));
    assertThat(saved(), is(empty()));
  }

  @Test
  public void shouldUpdateOnlyWhenADisplayedFieldChanged() {
    when(mapPoiRepository.findInBox("amenity", "restaurant", ZONE)).thenReturn(List.of(stored()));
    OsmPoi renamed = new OsmPoi(
      "Parrilla Nueva",
      PARRILLA.address(),
      PARRILLA.latitude(),
      PARRILLA.longitude(),
      null,
      null,
      null,
      "amenity",
      "restaurant",
      "way",
      1L
    );

    IngestResult result = service.ingest("amenity", "restaurant", ZONE, List.of(renamed));

    assertThat(result.updated(), is(1));
    assertThat(saved().getFirst().getName(), is("Parrilla Nueva"));
  }

  @Test
  public void shouldMarkAsRemovedWhatOverpassNoLongerReturns() {
    MapPoi stored = stored();
    when(mapPoiRepository.findInBox("amenity", "restaurant", ZONE)).thenReturn(List.of(stored));

    IngestResult result = service.ingest("amenity", "restaurant", ZONE, List.of());

    assertThat(result.removed(), is(1));
    assertThat(stored.getStatus(), is(PoiStatus.REMOVED));
    assertThat(saved(), contains(stored));
  }

  @Test
  public void shouldNotCountAnAlreadyRemovedPoiTwice() {
    MapPoi stored = stored();
    stored.setStatus(PoiStatus.REMOVED);
    when(mapPoiRepository.findInBox("amenity", "restaurant", ZONE)).thenReturn(List.of(stored));

    IngestResult result = service.ingest("amenity", "restaurant", ZONE, List.of());

    assertThat(result.removed(), is(0));
    assertThat(saved(), is(empty()));
  }

  @Test
  public void shouldReactivateAPoiThatComesBack() {
    MapPoi stored = stored();
    stored.setStatus(PoiStatus.REMOVED);
    when(mapPoiRepository.findInBox("amenity", "restaurant", ZONE)).thenReturn(List.of(stored));

    IngestResult result = service.ingest("amenity", "restaurant", ZONE, List.of(PARRILLA));

    assertThat(result.reactivated(), is(1));
    assertThat(stored.getStatus(), is(PoiStatus.ACTIVE));
  }

  @Test
  public void shouldSkipAPoiWithoutCoordinates() {
    when(mapPoiRepository.findInBox("amenity", "restaurant", ZONE)).thenReturn(List.of());
    OsmPoi nowhere = new OsmPoi(
      "Sin ubicacion",
      null,
      null,
      null,
      null,
      null,
      null,
      "amenity",
      "restaurant",
      "node",
      7L
    );

    IngestResult result = service.ingest("amenity", "restaurant", ZONE, List.of(nowhere));

    assertThat(result.created(), is(0));
    assertThat(saved(), is(empty()));
  }

  @Test
  public void shouldCountARepeatedElementOnce() {
    when(mapPoiRepository.findInBox("amenity", "restaurant", ZONE)).thenReturn(List.of());

    IngestResult result = service.ingest(
      "amenity",
      "restaurant",
      ZONE,
      List.of(PARRILLA, PARRILLA)
    );

    assertThat(result.created(), is(1));
    assertThat(saved().size(), is(1));
  }

  @Test
  public void shouldStampTheZoneOnEveryIngest() {
    when(mapPoiRepository.findInBox("amenity", "restaurant", ZONE)).thenReturn(List.of());

    service.ingest("amenity", "restaurant", ZONE, List.of());

    ArgumentCaptor<PoiZone> zone = ArgumentCaptor.forClass(PoiZone.class);
    verify(poiZoneRepository).save(zone.capture());
    assertThat(zone.getValue().getZoneKey(), is("-34.62,-58.45,-34.58,-58.37"));
    assertThat(zone.getValue().getFetchedAt(), is(notNullValue()));
  }

  @Test
  public void shouldMoveTheTimestampOfAnExistingZoneInsteadOfDuplicatingIt() {
    PoiZone existing = new PoiZone(
      "-34.62,-58.45,-34.58,-58.37",
      "amenity",
      "restaurant",
      Instant.parse("2026-01-01T10:00:00Z")
    );
    when(poiZoneRepository.find(any(), eq("amenity"), eq("restaurant")))
      .thenReturn(Optional.of(existing));
    when(mapPoiRepository.findInBox("amenity", "restaurant", ZONE)).thenReturn(List.of());

    service.ingest("amenity", "restaurant", ZONE, List.of());

    assertThat(existing.getFetchedAt().isAfter(Instant.parse("2026-01-01T10:00:00Z")), is(true));
  }

  @Test
  public void shouldReadOnlyTheActivePoisInsideTheBoxWithoutTouchingOverpass() {
    MapPoi active = stored();
    MapPoi removed = stored();
    removed.setSourceKey("osm:way/2");
    removed.setStatus(PoiStatus.REMOVED);
    when(mapPoiRepository.findInBox("amenity", "restaurant", VIEW))
      .thenReturn(List.of(active, removed));
    when(poiZoneRepository.find(any(), any(), any()))
      .thenReturn(
        Optional.of(
          new PoiZone("k", "amenity", "restaurant", Instant.parse("2026-01-01T10:00:00Z"))
        )
      );

    ZonePois zonePois = service.read("amenity", "restaurant", VIEW);

    assertThat(zonePois.pois().size(), is(1));
    assertThat(zonePois.fetchedAt(), is(Instant.parse("2026-01-01T10:00:00Z")));
    verify(mapPoiRepository, never()).saveAll(any());
  }

  @Test
  public void shouldReportNoTimestampForAZoneThatWasNeverLoaded() {
    when(mapPoiRepository.findInBox("amenity", "restaurant", VIEW)).thenReturn(List.of());

    ZonePois zonePois = service.read("amenity", "restaurant", VIEW);

    assertThat(zonePois.fetchedAt(), is(nullValue()));
  }

  @Test
  public void shouldRoundTheZoneOutwardSoItContainsTheView() {
    BoundingBox zone = PoiIngestService.zoneOf(
      new BoundingBox(-34.6037, -58.3816, -34.5987, -58.3766)
    );

    assertThat(zone.south() <= -34.6037, is(true));
    assertThat(zone.north() >= -34.5987, is(true));
    assertThat(zone.west() <= -58.3816, is(true));
    assertThat(zone.east() >= -58.3766, is(true));
    assertThat(PoiIngestService.zoneKey(zone), is("-34.61,-58.39,-34.59,-58.37"));
  }

  @Test
  public void shouldKeepTheRequestedBoxWhenTheZoneFallsOutsideCaba() {
    BoundingBox outside = new BoundingBox(-40, -70, -39.99, -69.99);

    assertThat(PoiIngestService.zoneOf(outside), is(outside));
  }

  private static MapPoi stored() {
    return MapPoi.from(PARRILLA, PlaceCategory.RESTAURANT, "amenity", "restaurant");
  }

  @SuppressWarnings("unchecked")
  private List<MapPoi> saved() {
    ArgumentCaptor<List<MapPoi>> captor = ArgumentCaptor.forClass(List.class);
    verify(mapPoiRepository).saveAll(captor.capture());
    return captor.getValue();
  }
}
