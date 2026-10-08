package com.valhalla.presentation.plan;

// Strings avoid introducing a JavaTime Jackson module absent from the existing MVC configuration.
public record PlanPlaceUpdateRequest(String visitDate, String visitTime) {}
