package com.valhalla.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.place.PlaceCategory;
import com.valhalla.domain.place.PlaceRepository;
import com.valhalla.domain.place.PlaceService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * El cache del catalogo se prueba por el puerto: dos lecturas seguidas no pueden volver al
 * repositorio, y toda escritura que pase por el puerto tiene que tirar la lista cacheada. Cada
 * lugar de prueba se crea y se borra por el mismo puerto, asi que la clase no deja filas.
 */
@WebIntegrationTest
public class PlaceCatalogueCacheTest {

  @Autowired
  private PlaceService placeService;

  @MockitoSpyBean
  private PlaceRepository placeRepository;

  @Test
  public void shouldServeTwoConsecutiveReadsFromTheCache() {
    // La escritura deja el cache vacio: la primera lectura de abajo es un miss seguro.
    Place scratch = createScratchPlace("Cache hit");

    placeService.getAllPlaces();
    placeService.getAllPlaces();

    verify(placeRepository, times(1)).findAll();
    deleteScratchPlace(scratch);
  }

  @Test
  public void shouldShowANewPlaceAfterSavingThroughThePort() {
    placeService.getAllPlaces();

    Place created = createScratchPlace("Cache eviction on save");

    assertThat(names(placeService.getAllPlaces()), hasItem("Cache eviction on save"));
    deleteScratchPlace(created);
  }

  @Test
  public void shouldHideADeletedPlaceAfterDeletingThroughThePort() {
    Place created = createScratchPlace("Cache eviction on delete");
    assertThat(names(placeService.getAllPlaces()), hasItem("Cache eviction on delete"));

    deleteScratchPlace(created);

    assertThat(names(placeService.getAllPlaces()), is(not(hasItem("Cache eviction on delete"))));
  }

  private Place createScratchPlace(String name) {
    Place place = new Place();
    place.setName(name);
    place.setCategory(PlaceCategory.OTHER);
    placeRepository.save(place);
    return place;
  }

  private void deleteScratchPlace(Place place) {
    placeRepository.deleteById(place.getId());
  }

  private List<String> names(List<Place> places) {
    return places.stream().map(Place::getName).toList();
  }
}
