package com.valhalla.presentation.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.ModelAndView;

class CalendarControllerTest {

  private static final String EMAIL = "calendar@test.com";
  private PlanService service;
  private Authentication authentication;
  private CalendarController controller;

  @BeforeEach
  void setUp() {
    service = mock(PlanService.class);
    authentication = mock(Authentication.class);
    when(authentication.getName()).thenReturn(EMAIL);
    controller = new CalendarController(service);
  }

  @Test
  void defaultsToTodayAndQueriesOnlyTheAuthenticatedUser() {
    ModelAndView view = controller.calendar(authentication, null, null);
    assertEquals("pages/plans/calendar", view.getViewName());
    assertEquals(LocalDate.now(), view.getModel().get("selectedDate"));
    assertEquals(YearMonth.now().toString(), view.getModel().get("selectedMonth"));
    assertEquals(false, view.getModel().get("hasPlans"));
    assertEquals(List.of(), view.getModel().get("undatedPlans"));
    verify(service).getPlansByUserEmail(EMAIL);
    verify(service).getParticipantPlans(EMAIL);
    assertEquals(1, days(view).stream().filter(CalendarController.CalendarDay::today).count());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
    strings = { "invalid", "2026-13", "2026-00", "2026-10-01", "1899-12", "+10000-01", "9999-01" }
  )
  void invalidMonthsFallBackToCurrentMonth(String month) {
    ModelAndView view = controller.calendar(authentication, month, null);
    assertEquals(YearMonth.now().toString(), view.getModel().get("selectedMonth"));
    assertEquals(LocalDate.now(), view.getModel().get("selectedDate"));
  }

  @ParameterizedTest
  @CsvSource(
    {
      "2024-02,35,2024-01-29,2024-03-03,29",
      "2021-02,28,2021-02-01,2021-02-28,28",
      "2026-08,42,2026-07-27,2026-09-06,31",
    }
  )
  void rendersCompleteMondayToSundayWeeks(
    String month,
    int cells,
    LocalDate start,
    LocalDate end,
    int monthDays
  ) {
    ModelAndView view = controller.calendar(authentication, month, null);
    List<CalendarController.CalendarDay> days = days(view);
    assertEquals(cells, days.size());
    assertEquals(start, days.getFirst().date());
    assertEquals(end, days.getLast().date());
    assertEquals(DayOfWeek.MONDAY, days.getFirst().date().getDayOfWeek());
    assertEquals(DayOfWeek.SUNDAY, days.getLast().date().getDayOfWeek());
    assertEquals(
      monthDays,
      days.stream().filter(CalendarController.CalendarDay::currentMonth).count()
    );
    for (int index = 1; index < days.size(); index++) {
      assertEquals(days.get(index - 1).date().plusDays(1), days.get(index).date());
    }
  }

  @ParameterizedTest
  @CsvSource(
    {
      "2026-01,2025-12,2026-02",
      "2026-12,2026-11,2027-01",
      "1900-01,1899-12,1900-02",
      "9998-12,9998-11,9999-01",
    }
  )
  void navigationHandlesYearChangesAndSupportedBoundaries(
    String month,
    String previous,
    String next
  ) {
    ModelAndView view = controller.calendar(authentication, month, null);
    assertEquals(previous, view.getModel().get("previousMonth"));
    assertEquals(next, view.getModel().get("nextMonth"));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = { "invalid", "2024-02-30", "2024-03-01", "2024-01-31" })
  void invalidOrOutsideDatesFallBackToFirstDay(String date) {
    ModelAndView view = controller.calendar(authentication, "2024-02", date);
    assertEquals(LocalDate.of(2024, 2, 1), view.getModel().get("selectedDate"));
  }

  @Test
  void invalidCurrentMonthDateFallsBackToToday() {
    ModelAndView view = controller.calendar(authentication, YearMonth.now().toString(), "invalid");
    assertEquals(LocalDate.now(), view.getModel().get("selectedDate"));
  }

  @Test
  void acceptsLeapDayAndFormatsSpanishLabels() {
    ModelAndView view = controller.calendar(authentication, "2024-02", "2024-02-29");
    assertEquals(LocalDate.of(2024, 2, 29), view.getModel().get("selectedDate"));
    assertEquals("febrero 2024", view.getModel().get("monthLabel"));
    assertEquals("jueves 29 de febrero", view.getModel().get("selectedDateLabel"));
  }

  @Test
  void combinesOwnedAndParticipatingPlansWithoutDuplicatingAndSeparatesUndatedPlans() {
    Plan owned = plan("Zoológico", "2024-02-29");
    Plan shared = plan("Museo", "2024-02-29");
    Plan undated = plan("Sin fecha", null);
    Plan otherDay = plan("Otro día", "2024-02-15");
    Plan outside = plan("Otro mes", "2024-04-01");
    when(service.getPlansByUserEmail(EMAIL)).thenReturn(List.of(owned, undated, otherDay, outside));
    when(service.getParticipantPlans(EMAIL)).thenReturn(List.of(shared, owned));
    ModelAndView view = controller.calendar(authentication, "2024-02", "2024-02-29");
    assertEquals(true, view.getModel().get("hasPlans"));
    assertEquals(List.of(shared, owned), view.getModel().get("selectedPlans"));
    assertEquals(List.of(undated), view.getModel().get("undatedPlans"));
    assertEquals(
      List.of(shared, owned),
      days(view)
        .stream()
        .filter(day -> day.date().equals(LocalDate.of(2024, 2, 29)))
        .findFirst()
        .orElseThrow()
        .plans()
    );
    assertFalse(
      days(view)
        .stream()
        .flatMap(day -> day.plans().stream())
        .anyMatch(plan -> plan == undated || plan == outside)
    );
    assertTrue(
      days(view).stream().flatMap(day -> day.plans().stream()).anyMatch(plan -> plan == otherDay)
    );
  }

  @SuppressWarnings("unchecked")
  private List<CalendarController.CalendarDay> days(ModelAndView view) {
    return (List<CalendarController.CalendarDay>) view.getModel().get("days");
  }

  private Plan plan(String name, String date) {
    Plan plan = new Plan();
    plan.setName(name);
    plan.setEventDate(date == null ? null : LocalDate.parse(date));
    return plan;
  }
}
