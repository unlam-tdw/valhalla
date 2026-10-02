package com.valhalla.domain.place;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.Optional;
import org.junit.jupiter.api.Test;

public class PlaceCategoryTest {

  @Test
  public void shouldResolveAKnownCode() {
    Optional<PlaceCategory> result = PlaceCategory.fromCode("CAFE");

    assertThat(result.orElseThrow(), is(equalTo(PlaceCategory.CAFE)));
  }

  @Test
  public void shouldReturnEmptyForAnUnknownCode() {
    assertThat(PlaceCategory.fromCode("NO_SUCH_CATEGORY").isEmpty(), is(true));
  }

  @Test
  public void shouldReturnEmptyForBlankOrNullCode() {
    assertThat(PlaceCategory.fromCode("  ").isEmpty(), is(true));
    assertThat(PlaceCategory.fromCode(null).isEmpty(), is(true));
  }
}
