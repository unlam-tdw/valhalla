package com.valhalla.domain.place;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

/**
 * Identidad y diff del POI guardado: es la logica que decide si una recarga toca la base o no, asi
 * que se prueba sin Spring.
 */
public class MapPoiTest {

  private static final OsmPoi PARRILLA = new OsmPoi(
    "Parrilla del Puerto",
    "Av. Corrientes 1234",
    -34.6037,
    -58.3816,
    "Mo-Su 12:00-23:00",
    "+54 11 4444-5555",
    "https://parrilla.example.com",
    "amenity",
    "restaurant",
    "way",
    1L
  );

  @Test
  public void shouldKeyThePoiByItsOsmElement() {
    assertThat(MapPoi.sourceKeyOf(PARRILLA), is("osm:way/1"));
  }

  @Test
  public void shouldFallBackToNameAndCoordinatesWithoutAnOsmId() {
    OsmPoi anonymous = new OsmPoi(
      "Sin id",
      null,
      -34.6,
      -58.38,
      null,
      null,
      null,
      "amenity",
      "cafe",
      null,
      null
    );

    assertThat(MapPoi.sourceKeyOf(anonymous), is("fallback:Sin id@-34.6,-58.38"));
  }

  /** Media identidad no alcanza: sin id no hay clave estable, aunque el tipo venga. */
  @Test
  public void shouldFallBackWhenTheElementHasATypeButNoId() {
    OsmPoi halfIdentified = new OsmPoi(
      "Sin id",
      null,
      -34.6,
      -58.38,
      null,
      null,
      null,
      "amenity",
      "cafe",
      "node",
      null
    );

    assertThat(MapPoi.sourceKeyOf(halfIdentified), is("fallback:Sin id@-34.6,-58.38"));
  }

  @Test
  public void shouldStartAsActiveWithTheRequestedTag() {
    MapPoi poi = MapPoi.from(PARRILLA, PlaceCategory.RESTAURANT, "amenity", "restaurant");

    assertThat(poi.getStatus(), is(PoiStatus.ACTIVE));
    assertThat(poi.getSourceKey(), is("osm:way/1"));
    assertThat(poi.getCategory(), is(PlaceCategory.RESTAURANT));
    assertThat(poi.getOsmId(), is(1L));
  }

  @Test
  public void shouldReportNoChangeWhenTheSamePoiComesBack() {
    MapPoi poi = MapPoi.from(PARRILLA, PlaceCategory.RESTAURANT, "amenity", "restaurant");

    assertThat(poi.apply(PARRILLA, PlaceCategory.RESTAURANT), is(false));
  }

  @Test
  public void shouldCopyTheNewValuesWhenAFieldChanged() {
    MapPoi poi = MapPoi.from(PARRILLA, PlaceCategory.RESTAURANT, "amenity", "restaurant");
    OsmPoi renamed = new OsmPoi(
      "Parrilla Nueva",
      PARRILLA.address(),
      PARRILLA.latitude(),
      PARRILLA.longitude(),
      PARRILLA.openingHours(),
      PARRILLA.phone(),
      PARRILLA.website(),
      PARRILLA.osmKey(),
      PARRILLA.osmValue(),
      PARRILLA.osmType(),
      PARRILLA.osmId()
    );

    assertThat(poi.apply(renamed, PlaceCategory.RESTAURANT), is(true));
    assertThat(poi.getName(), is("Parrilla Nueva"));
  }

  @Test
  public void shouldWriteOnlyTheCategoryWhenThatIsTheOnlyDifference() {
    MapPoi poi = MapPoi.from(PARRILLA, PlaceCategory.OTHER, "amenity", "restaurant");

    assertThat(poi.apply(PARRILLA, PlaceCategory.RESTAURANT), is(true));
    assertThat(poi.getCategory(), is(PlaceCategory.RESTAURANT));
  }

  @Test
  public void shouldExposeTheOsmShapeByItsStoredTag() {
    MapPoi poi = MapPoi.from(PARRILLA, PlaceCategory.RESTAURANT, "amenity", "restaurant");

    OsmPoi shape = poi.toOsmPoi();

    assertThat(shape.osmKey(), is("amenity"));
    assertThat(shape.osmValue(), is("restaurant"));
    assertThat(shape.name(), is("Parrilla del Puerto"));
    assertThat(shape.website(), notNullValue());
  }

  @Test
  public void shouldKeepTheOptionalFieldsNullWhenThePoiHasNone() {
    OsmPoi bare = new OsmPoi(
      "Kiosco",
      null,
      -34.6,
      -58.38,
      null,
      null,
      null,
      "amenity",
      "cafe",
      "node",
      9L
    );

    MapPoi poi = MapPoi.from(bare, PlaceCategory.CAFE, "amenity", "cafe");

    assertThat(poi.getAddress(), is(nullValue()));
    assertThat(poi.getWebsite(), is(nullValue()));
    assertThat(poi.getPhone(), is(nullValue()));
  }
}
