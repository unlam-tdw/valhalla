package com.valhalla.presentation.place;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.place.PlaceCategory;
import com.valhalla.domain.place.PlaceService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PlaceRestControllerTest {

  private PlaceRestController controller;
  private PlaceService placeServiceMock;

  @BeforeEach
  public void init() {
    placeServiceMock = mock(PlaceService.class);
    controller = new PlaceRestController(placeServiceMock);
  }

  @Test
  public void shouldReturnAllPlacesWhenFiltersAreAbsent() {
    List<Place> places = List.of(place("MALBA", PlaceCategory.MUSEUM));
    when(placeServiceMock.getAllPlaces()).thenReturn(places);

    List<Place> result = controller.listPlaces(null, null);

    assertThat(result, is(sameInstance(places)));
    verify(placeServiceMock).getAllPlaces();
  }

  @Test
  public void shouldFilterPlacesByCategory() {
    List<Place> places = List.of(place("El Sanjuanino", PlaceCategory.RESTAURANT));
    when(placeServiceMock.getPlacesByCategory(PlaceCategory.RESTAURANT)).thenReturn(places);

    List<Place> result = controller.listPlaces("RESTAURANT", null);

    assertThat(result, is(sameInstance(places)));
    verify(placeServiceMock).getPlacesByCategory(PlaceCategory.RESTAURANT);
  }

  @Test
  public void shouldSearchPlacesByName() {
    List<Place> places = List.of(place("Parrilla Don Julio", PlaceCategory.RESTAURANT));
    when(placeServiceMock.searchPlaces("Don")).thenReturn(places);

    List<Place> result = controller.listPlaces(null, "Don");

    assertThat(result, is(sameInstance(places)));
    verify(placeServiceMock).searchPlaces("Don");
  }

  @Test
  public void shouldSearchPlacesByNameWithinCategory() {
    List<Place> places = List.of(place("Parrilla Don Julio", PlaceCategory.RESTAURANT));
    when(placeServiceMock.searchPlaces("Don", PlaceCategory.RESTAURANT)).thenReturn(places);

    List<Place> result = controller.listPlaces("RESTAURANT", "Don");

    assertThat(result, is(sameInstance(places)));
    verify(placeServiceMock).searchPlaces("Don", PlaceCategory.RESTAURANT);
  }

  @Test
  public void shouldReturnNoPlacesForUnknownCategory() {
    List<Place> result = controller.listPlaces("UNKNOWN", "Don");

    assertThat(result, is(equalTo(List.of())));
    verifyNoInteractions(placeServiceMock);
  }

  @Test
  public void shouldTreatBlankFiltersAsAbsent() {
    List<Place> places = List.of(place("MALBA", PlaceCategory.MUSEUM));
    when(placeServiceMock.getAllPlaces()).thenReturn(places);

    List<Place> result = controller.listPlaces(" ", " ");

    assertThat(result, is(sameInstance(places)));
    verify(placeServiceMock).getAllPlaces();
  }

  private Place place(String name, PlaceCategory category) {
    Place place = new Place();
    place.setName(name);
    place.setCategory(category);
    return place;
  }
}
