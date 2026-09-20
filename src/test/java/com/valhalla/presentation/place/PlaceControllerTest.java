package com.valhalla.presentation.place;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.place.PlaceService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

public class PlaceControllerTest {

  private PlaceController controller;
  private PlaceService placeServiceMock;

  @BeforeEach
  public void init() {
    placeServiceMock = mock(PlaceService.class);
    controller = new PlaceController(placeServiceMock);
  }

  @Test
  public void shouldReturnAllPlacesWhenListHasNoFilters() {
    List<Place> places = List.of(place(1L, "MALBA", "MUSEUM"));
    when(placeServiceMock.getAllPlaces()).thenReturn(places);

    ModelAndView result = controller.listPlaces(null, null);

    assertThat(result.getViewName(), is(equalTo("pages/places/list")));
    assertThat(result.getModel().get("places"), is(sameInstance(places)));
    verify(placeServiceMock).getAllPlaces();
  }

  @Test
  public void shouldReturnCategoryFilteredPlaces() {
    List<Place> places = List.of(place(1L, "Cafe Tortoni", "CAFE"));
    when(placeServiceMock.getPlacesByCategory("CAFE")).thenReturn(places);

    ModelAndView result = controller.listPlaces("CAFE", null);

    assertThat(result.getModel().get("places"), is(sameInstance(places)));
    verify(placeServiceMock).getPlacesByCategory("CAFE");
  }

  @Test
  public void shouldReturnNameFilteredPlaces() {
    List<Place> places = List.of(place(1L, "Parrilla Don Julio", "RESTAURANT"));
    when(placeServiceMock.searchPlaces("Don")).thenReturn(places);

    ModelAndView result = controller.listPlaces(null, "Don");

    assertThat(result.getModel().get("places"), is(sameInstance(places)));
    verify(placeServiceMock).searchPlaces("Don");
  }

  @Test
  public void shouldReturnPlaceInDetailWhenIdExists() {
    Place expectedPlace = place(1L, "MALBA", "MUSEUM");
    when(placeServiceMock.getPlaceById(1L)).thenReturn(Optional.of(expectedPlace));

    ModelAndView result = controller.placeDetail(1L);

    assertThat(result.getViewName(), is(equalTo("pages/places/detail")));
    assertThat(result.getModel().get("place"), is(sameInstance(expectedPlace)));
  }

  @Test
  public void shouldReturnDetailWithoutPlaceWhenIdDoesNotExist() {
    when(placeServiceMock.getPlaceById(99L)).thenReturn(Optional.empty());

    ModelAndView result = controller.placeDetail(99L);

    assertThat(result.getViewName(), is(equalTo("pages/places/detail")));
    assertThat(result.getModel().containsKey("place"), is(false));
  }

  private Place place(Long id, String name, String category) {
    Place place = new Place();
    place.setId(id);
    place.setName(name);
    place.setCategory(category);
    return place;
  }
}
