package com.valhalla.presentation.plan;

import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class CalendarController {

  private final PlanService planService;

  public CalendarController(PlanService planService) {
    this.planService = planService;
  }

  @GetMapping("/plans/calendar")
  public ModelAndView calendar(
    Authentication authentication,
    @RequestParam(required = false) String month,
    @RequestParam(name = "date", required = false) String requestedDay
  ) {
    YearMonth selected = YearMonth.now();
    if (month != null) {
      try {
        YearMonth requested = YearMonth.parse(month);
        if (requested.getYear() >= 1900 && requested.getYear() <= 9998) {
          selected = requested;
        }
      } catch (java.time.format.DateTimeParseException ignored) {
        selected = YearMonth.now();
      }
    }
    String email = authentication.getName();
    List<Plan> plans = Stream
      .concat(
        planService.getPlansByUserEmail(email).stream(),
        planService.getParticipantPlans(email).stream()
      )
      .distinct()
      .sorted(Comparator.comparing(Plan::getName))
      .toList();
    LocalDate first = selected.atDay(1);
    LocalDate start = first.minusDays(first.getDayOfWeek().getValue() - 1L);
    int cells = ((first.getDayOfWeek().getValue() - 1 + selected.lengthOfMonth() + 6) / 7) * 7;
    YearMonth visibleMonth = selected;
    List<CalendarDay> days = IntStream
      .range(0, cells)
      .mapToObj(offset -> {
        LocalDate date = start.plusDays(offset);
        return new CalendarDay(
          date,
          YearMonth.from(date).equals(visibleMonth),
          date.equals(LocalDate.now()),
          plans.stream().filter(plan -> date.equals(plan.getEventDate())).toList()
        );
      })
      .toList();
    ModelMap model = new ModelMap();
    LocalDate selectedDate = selected.equals(YearMonth.now()) ? LocalDate.now() : first;
    if (requestedDay != null) {
      try {
        LocalDate requestedDate = LocalDate.parse(requestedDay);
        if (YearMonth.from(requestedDate).equals(selected)) {
          selectedDate = requestedDate;
        }
      } catch (java.time.format.DateTimeParseException ignored) {
        selectedDate = selected.equals(YearMonth.now()) ? LocalDate.now() : first;
      }
    }
    LocalDate activeDate = selectedDate;
    model.put("selectedDate", activeDate);
    model.put(
      "selectedDateLabel",
      activeDate.format(
        DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-AR"))
      )
    );
    model.put(
      "selectedPlans",
      plans.stream().filter(plan -> activeDate.equals(plan.getEventDate())).toList()
    );
    model.put("selectedMonth", selected.toString());
    model.put("days", days);
    model.put(
      "monthLabel",
      selected.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-AR")))
    );
    model.put("previousMonth", selected.minusMonths(1).toString());
    model.put("nextMonth", selected.plusMonths(1).toString());
    model.put("hasPlans", !plans.isEmpty());
    model.put("undatedPlans", plans.stream().filter(plan -> plan.getEventDate() == null).toList());
    return new ModelAndView("pages/plans/calendar", model);
  }

  public record CalendarDay(
    LocalDate date,
    boolean currentMonth,
    boolean today,
    List<Plan> plans
  ) {}
}
