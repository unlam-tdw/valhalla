package com.valhalla.domain.place;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PlaceServiceImplTest {

  private PlaceRepository placeRepositoryMock;
  private PlaceService placeService;

  @BeforeEach
  public void init() {
    placeRepositoryMock = mock(PlaceRepository.class);
    placeService = new PlaceServiceImpl(placeRepositoryMock);
  }

  @Test
  public void shouldReturnAllPlaces() {
    List<Place> places = List.of(place("Cafe Tortoni", "CAFE"));
    when(placeRepositoryMock.findAll()).thenReturn(places);

    List<Place> result = placeService.getAllPlaces();

    assertThat(result, is(sameInstance(places)));
    verify(placeRepositoryMock).findAll();
  }

  @Test
  public void shouldReturnPlacesForCategory() {
    List<Place> places = List.of(place("Cafe Tortoni", "CAFE"));
    when(placeRepositoryMock.findByCategory("CAFE")).thenReturn(places);

    List<Place> result = placeService.getPlacesByCategory("CAFE");

    assertThat(result, is(sameInstance(places)));
    verify(placeRepositoryMock).findByCategory("CAFE");
  }

  @Test
  public void shouldSearchPlacesByName() {
    List<Place> places = List.of(place("Parrilla Don Julio", "RESTAURANT"));
    when(placeRepositoryMock.findByNameContainingIgnoreCase("Don")).thenReturn(places);

    List<Place> result = placeService.searchPlaces("Don");

    assertThat(result, is(sameInstance(places)));
    verify(placeRepositoryMock).findByNameContainingIgnoreCase("Don");
  }

  @Test
  public void shouldReturnPlaceWhenIdExists() {
    Place expectedPlace = place("MALBA", "MUSEUM");
    expectedPlace.setId(1L);
    when(placeRepositoryMock.findById(1L)).thenReturn(Optional.of(expectedPlace));

    Optional<Place> result = placeService.getPlaceById(1L);

    assertThat(result.orElseThrow(), is(equalTo(expectedPlace)));
    verify(placeRepositoryMock).findById(1L);
  }

  @Test
  public void shouldReturnEmptyWhenIdDoesNotExist() {
    when(placeRepositoryMock.findById(99L)).thenReturn(Optional.empty());

    Optional<Place> result = placeService.getPlaceById(99L);

    assertThat(result.isEmpty(), is(true));
    verify(placeRepositoryMock).findById(99L);
  }

  private Place place(String name, String category) {
    Place place = new Place();
    place.setName(name);
    place.setCategory(category);
    return place;
  }
}
