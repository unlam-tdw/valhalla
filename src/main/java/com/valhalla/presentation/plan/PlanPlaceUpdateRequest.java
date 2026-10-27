package com.valhalla.presentation.plan;

// Strings avoid introducing a JavaTime Jackson module absent from the existing MVC configuration.
// transportModeToNext arrives as the enum name and the controller converts it.
public record PlanPlaceUpdateRequest(
  String visitDate,
  String visitTime,
  Integer durationMinutes,
  String transportModeToNext
) {}
