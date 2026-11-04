package com.valhalla.domain.planplace;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PlanPlaceTimePinnedTest {

  @Test
  void newEntryHasNoPinnedTime() {
    assertFalse(new PlanPlace().isTimePinned());
  }

  @Test
  void legacyNullIsTreatedAsUnpinned() {
    PlanPlace entry = new PlanPlace();
    ReflectionTestUtils.setField(entry, "timePinned", null);
    assertFalse(entry.isTimePinned());
  }

  @Test
  void timeCanBePinnedAndUnpinned() {
    PlanPlace entry = new PlanPlace();
    entry.setTimePinned(true);
    assertTrue(entry.isTimePinned());
    entry.setTimePinned(false);
    assertFalse(entry.isTimePinned());
  }
}
