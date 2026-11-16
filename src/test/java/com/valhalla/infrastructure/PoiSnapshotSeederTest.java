package com.valhalla.infrastructure;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.valhalla.domain.place.BoundingBox;
import com.valhalla.domain.place.OsmPoi;
import com.valhalla.domain.place.PlaceCategory;
import com.valhalla.domain.place.PoiZone;
import com.valhalla.domain.place.PoiZoneRepository;
import com.valhalla.infrastructure.place.PoiIngestService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Tests de {@link PoiSnapshotSeeder}: el flag, la idempotencia y el contenido del snapshot.
 *
 * <p>El ultimo grupo es el que mas vale: cada categoria del archivo tiene que caer, por
 * {@link PlaceCategory#fromOsm}, en la categoria que dice la tabla. Si alguien cambia el mapeo o
 * regenera el snapshot con otro tag, el test lo grita antes de llegar a la demo.
 */
public class PoiSnapshotSeederTest {

  /** Tag del snapshot -> categoria esperada. Son las siete que la zona de Palermo tiene. */
  private static final Map<String, PlaceCategory> EXPECTED = Map.of(
    "amenity=restaurant",
    PlaceCategory.RESTAURANT,
    "amenity=bar",
    PlaceCategory.BAR,
    "amenity=cafe",
    PlaceCategory.CAFE,
    "amenity=nightclub",
    PlaceCategory.NIGHTLIFE,
    "amenity=theatre",
    PlaceCategory.CULTURE,
    "amenity=pharmacy",
    PlaceCategory.OTHER,
    "shop=clothes",
    PlaceCategory.SHOPPING
  );

  private static final BoundingBox ZONE = new BoundingBox(-34.60, -58.44, -34.58, -58.42);

  private PublicPlanDataSeeder publicPlanSeederMock;
  private PoiIngestService poiIngestServiceMock;
  private PoiZoneRepository poiZoneRepositoryMock;
  private PoiSnapshotSeeder seeder;

  @BeforeEach
  public void init() {
    this.publicPlanSeederMock = mock(PublicPlanDataSeeder.class);
    this.poiIngestServiceMock = mock(PoiIngestService.class);
    this.poiZoneRepositoryMock = mock(PoiZoneRepository.class);
    this.seeder =
      new PoiSnapshotSeeder(
        this.publicPlanSeederMock,
        this.poiIngestServiceMock,
        this.poiZoneRepositoryMock
      );
  }

  @Test
  public void sinLaLlaveDeDemoNoIngiereNada() {
    // given: el flag apagado (el default)

    // when
    this.seeder.onApplicationEvent(null);

    // then
    verifyNoInteractions(this.poiIngestServiceMock, this.poiZoneRepositoryMock);
  }

  @Test
  public void cargaCadaCategoriaDelSnapshotLaPrimeraVez() {
    // given: el flag prendido y ninguna cobertura todavia
    when(this.publicPlanSeederMock.isEnabled()).thenReturn(true);
    when(this.poiZoneRepositoryMock.find(anyString(), anyString(), anyString()))
      .thenReturn(Optional.empty());

    // when
    this.seeder.onApplicationEvent(null);

    // then: una ingesta por tag, sin tocar la red, con la zona del snapshot
    ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> values = ArgumentCaptor.forClass(String.class);
    verify(this.poiIngestServiceMock, times(EXPECTED.size()))
      .ingest(keys.capture(), values.capture(), any(BoundingBox.class), anyList());
    for (int i = 0; i < keys.getAllValues().size(); i++) {
      assertThat(
        EXPECTED.containsKey(keys.getAllValues().get(i) + "=" + values.getAllValues().get(i)),
        is(true)
      );
    }
  }

  @Test
  public void unaCategoriaYaCargadaNoSeVuelveAIngerir() {
    // given: la cobertura ya existe para todas las categorias (un reinicio)
    when(this.publicPlanSeederMock.isEnabled()).thenReturn(true);
    when(this.poiZoneRepositoryMock.find(anyString(), anyString(), anyString()))
      .thenReturn(Optional.of(mock(PoiZone.class)));

    // when
    this.seeder.onApplicationEvent(null);

    // then: no se reescribe fetched_at ni se toca una sola fila
    verify(this.poiIngestServiceMock, never()).ingest(anyString(), anyString(), any(), anyList());
  }

  @Test
  public void elSegundoEventoNoVuelveAIngerir() {
    // given
    when(this.publicPlanSeederMock.isEnabled()).thenReturn(true);
    when(this.poiZoneRepositoryMock.find(anyString(), anyString(), anyString()))
      .thenReturn(Optional.empty());

    // when
    this.seeder.onApplicationEvent(null);
    this.seeder.onApplicationEvent(null);

    // then
    verify(this.poiIngestServiceMock, times(EXPECTED.size()))
      .ingest(anyString(), anyString(), any(), anyList());
  }

  @Test
  public void elSnapshotTieneLaZonaDePalermoYSieteCategoriasConDatos() {
    // when
    PoiSnapshotSeeder.Snapshot snapshot = this.seeder.load();

    // then: la zona es la de la grilla de la app, y cada categoria trae puntos
    assertThat(snapshot.zone(), is(ZONE));
    assertThat(PoiIngestService.zoneOf(snapshot.zone()), is(ZONE));
    assertThat(snapshot.categories(), hasSize(EXPECTED.size()));
    for (PoiSnapshotSeeder.Category category : snapshot.categories()) {
      assertThat(category.pois(), is(not(List.of())));
      assertThat(category.pois().size(), is(greaterThan(0)));
    }
  }

  @Test
  public void cadaCategoriaCaeEnLaCategoriaEsperadaPorElMapeoReal() {
    // when
    PoiSnapshotSeeder.Snapshot snapshot = this.seeder.load();

    // then: el tag del bloque mapea, por el mapeo real, a la categoria de la tabla
    for (PoiSnapshotSeeder.Category category : snapshot.categories()) {
      String tag = category.tagKey() + "=" + category.tagValue();
      assertThat(tag, EXPECTED.containsKey(tag), is(true));
      assertThat(
        PlaceCategory.fromOsm(category.tagKey(), category.tagValue()),
        is(EXPECTED.get(tag))
      );
    }
  }

  @Test
  public void cadaPoiTieneNombreCoordenadasYSuTag() {
    // when
    PoiSnapshotSeeder.Snapshot snapshot = this.seeder.load();

    // then: nada sin nombre o sin coordenadas, y cada POI lleva el tag de su bloque
    for (PoiSnapshotSeeder.Category category : snapshot.categories()) {
      for (OsmPoi poi : category.pois()) {
        assertThat(poi.name(), notNullValue());
        assertThat(poi.latitude(), notNullValue());
        assertThat(poi.longitude(), notNullValue());
        assertThat(poi.osmKey(), is(category.tagKey()));
        assertThat(poi.osmValue(), is(category.tagValue()));
      }
    }
    assertThat(
      snapshot.categories().stream().flatMap(c -> c.pois().stream()).map(OsmPoi::name).toList(),
      everyItem(not(""))
    );
  }
}
