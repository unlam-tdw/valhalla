package com.valhalla.domain.place;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

public class PlaceTest {

  @Test
  public void shouldStoreTheImageUrl() {
    Place place = new Place();

    place.setImageUrl("https://example.test/place.jpg");

    assertThat(place.getImageUrl(), is(equalTo("https://example.test/place.jpg")));
  }
}
