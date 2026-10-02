package com.valhalla.domain.place;

import java.util.Optional;

public enum PlaceCategory {
  RESTAURANT,
  BAR,
  CAFE,
  MUSEUM,
  PARK,
  SHOPPING,
  NIGHTLIFE,
  CULTURE,
  SPORT,
  OTHER;

  public static Optional<PlaceCategory> fromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(valueOf(code));
    } catch (IllegalArgumentException exception) {
      return Optional.empty();
    }
  }
}
