package com.valhalla.domain.exception;

public class PlanBudgetExceededException extends IllegalArgumentException {

  private static final long serialVersionUID = 1L;

  public PlanBudgetExceededException(String message) {
    super(message);
  }
}
