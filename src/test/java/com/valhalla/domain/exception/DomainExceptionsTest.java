package com.valhalla.domain.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DomainExceptionsTest {

  @Test
  void sePuedenInstanciarLasExcepcionesDeDominio() {
    assertNotNull(new PlaceInUseException());
    assertNotNull(new PlaceNotFoundException());
    assertNotNull(new PlanNotFoundException());
    assertNotNull(new UserAlreadyExists());
    assertNotNull(new UserNotFoundException());
    assertNotNull(new PlanBannedException());
  }

  @Test
  void planBudgetExceededConservaElMensaje() {
    PlanBudgetExceededException ex = new PlanBudgetExceededException("limite");
    assertEquals("limite", ex.getMessage());
    assertTrue(ex instanceof IllegalArgumentException);
  }
}
