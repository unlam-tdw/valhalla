package com.valhalla.presentation.plan;

import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    YearMonth selected = resolveMonth(month);
    String email = authentication.getName();
    List<Plan> plans = Stream
      .concat(
        planService.getPlansByUserEmail(email).stream(),
        planService.getParticipantPlans(email).stream()
      )
      .distinct()
      .sorted(Comparator.comparing(Plan::getName))
      .toList();
    Map<String, Object> model = new ModelMap();
    LocalDate activeDate = resolveDate(requestedDay, selected);
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
    model.put("days", calendarDays(selected, plans));
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

  private static YearMonth resolveMonth(String month) {
    if (month != null) {
      try {
        YearMonth requested = YearMonth.parse(month);
        if (requested.getYear() >= 1900 && requested.getYear() <= 9998) {
          return requested;
        }
      } catch (java.time.format.DateTimeParseException ignored) {
        return YearMonth.now();
      }
    }
    return YearMonth.now();
  }

  private static LocalDate resolveDate(String requestedDay, YearMonth selected) {
    LocalDate fallback = selected.equals(YearMonth.now()) ? LocalDate.now() : selected.atDay(1);
    if (requestedDay != null) {
      try {
        LocalDate requestedDate = LocalDate.parse(requestedDay);
        if (YearMonth.from(requestedDate).equals(selected)) {
          return requestedDate;
        }
      } catch (java.time.format.DateTimeParseException ignored) {
        return fallback;
      }
    }
    return fallback;
  }

  private static List<CalendarDay> calendarDays(YearMonth selected, List<Plan> plans) {
    LocalDate first = selected.atDay(1);
    LocalDate start = first.minusDays(first.getDayOfWeek().getValue() - 1L);
    int cells = ((first.getDayOfWeek().getValue() - 1 + selected.lengthOfMonth() + 6) / 7) * 7;
    return IntStream
      .range(0, cells)
      .mapToObj(offset -> {
        LocalDate date = start.plusDays(offset);
        return new CalendarDay(
          date,
          YearMonth.from(date).equals(selected),
          date.equals(LocalDate.now()),
          plans.stream().filter(plan -> date.equals(plan.getEventDate())).toList()
        );
      })
      .toList();
  }

  public record CalendarDay(
    LocalDate date,
    boolean currentMonth,
    boolean today,
    List<Plan> plans
  ) {}
}
