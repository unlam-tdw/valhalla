package com.valhalla.presentation.plan;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.place.PlaceCategory;
import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanScheduleCalculator;
import com.valhalla.domain.plan.TransportMode;
import com.valhalla.domain.planplace.PlanPlace;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * [PRINT] Modelo de la hoja imprimible del itinerario: los dias, las paradas de cada dia y el
 * tramo de traslado hacia la parada siguiente.
 *
 * <p>Se arma en Java y no reciclando el DOM del detalle: ahi el recorrido lo pinta Vue y la vista
 * publica lo renderiza server-side con otro markup. Un unico modelo deja las dos vistas con el
 * mismo papel y se puede probar sin levantar el navegador. La agrupacion por dia copia la del JS
 * (fechas consecutivas iguales forman un grupo; una fecha que reaparece arranca un dia nuevo) y el
 * numero de parada es global, el mismo de los marcadores del mapa.
 */
public record PrintSheet(List<Day> days) {
  /** Horario sin hora de inicio: la parada todavia no tiene lugar en el reloj. */
  public static final String NO_SCHEDULE = "Horario por definir";

  private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
  private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("dd/MM");
  private static final int MINUTES_PER_HOUR = 60;

  private static final Map<PlaceCategory, String> CATEGORY_LABELS = Map.ofEntries(
    Map.entry(PlaceCategory.RESTAURANT, "Restaurante"),
    Map.entry(PlaceCategory.BAR, "Bar"),
    Map.entry(PlaceCategory.CAFE, "Café"),
    Map.entry(PlaceCategory.MUSEUM, "Museo"),
    Map.entry(PlaceCategory.PARK, "Parque"),
    Map.entry(PlaceCategory.SHOPPING, "Compras"),
    Map.entry(PlaceCategory.NIGHTLIFE, "Vida nocturna"),
    Map.entry(PlaceCategory.CULTURE, "Cultura"),
    Map.entry(PlaceCategory.SPORT, "Deporte"),
    Map.entry(PlaceCategory.OTHER, "Otro")
  );

  private static final Map<TransportMode, String> MODE_LABELS = Map.of(
    TransportMode.WALK,
    "a pie",
    TransportMode.BIKE,
    "en bici",
    TransportMode.BUS,
    "en colectivo",
    TransportMode.CAR,
    "en auto"
  );

  /**
   * Un dia del itinerario impreso.
   *
   * @param label encabezado impreso, con el numero de jornada y la fecha corta cuando la hay
   * @param stops paradas de ese dia, en el orden del itinerario
   */
  public record Day(String label, List<Stop> stops) {}

  /**
   * Una parada del itinerario impreso.
   *
   * @param number numero global de la parada, el mismo de los marcadores del mapa
   * @param name nombre del lugar
   * @param category categoria traducida al espanol
   * @param schedule rango horario ({@code 09:00 – 10:30}) o {@link #NO_SCHEDULE}
   * @param duration duracion de la visita ({@code 45m}, {@code 1h 30m})
   * @param address direccion del lugar, vacia si no tiene
   * @param leg tramo hacia la parada siguiente del mismo dia, o {@code null} si es la ultima
   */
  public record Stop(
    int number,
    String name,
    String category,
    String schedule,
    String duration,
    String address,
    String leg
  ) {}

  /** Arma la hoja de un plan; un plan nulo o sin paradas devuelve una hoja sin dias. */
  public static PrintSheet of(Plan plan) {
    if (plan == null || plan.getPlanPlaces() == null || plan.getPlanPlaces().isEmpty()) {
      return new PrintSheet(List.of());
    }
    List<PlanPlace> places = plan.getPlanPlaces();
    List<Day> days = new ArrayList<>();
    List<Stop> currentStops = null;
    LocalDate currentDate = null;
    for (int index = 0; index < places.size(); index++) {
      PlanPlace entry = places.get(index);
      LocalDate date = entry.getVisitDate();
      if (currentStops == null || !Objects.equals(currentDate, date)) {
        currentStops = new ArrayList<>();
        days.add(new Day(dayLabel(days.size() + 1, date), currentStops));
        currentDate = date;
      }
      PlanPlace next = index + 1 < places.size() ? places.get(index + 1) : null;
      currentStops.add(stop(plan, entry, index + 1, next, date));
    }
    return new PrintSheet(
      days.stream().map(day -> new Day(day.label(), List.copyOf(day.stops()))).toList()
    );
  }

  private static Stop stop(Plan plan, PlanPlace entry, int number, PlanPlace next, LocalDate date) {
    Place place = entry.getPlace();
    int duration = PlanScheduleCalculator.effectiveDuration(
      entry.getDurationMinutes(),
      place == null ? null : place.getDefaultDurationMinutes(),
      place == null ? null : place.getCategory()
    );
    return new Stop(
      number,
      place == null ? "" : blankToEmpty(place.getName()),
      categoryLabel(place == null ? null : place.getCategory()),
      schedule(entry.getVisitTime(), duration),
      formatDuration(duration),
      place == null ? "" : blankToEmpty(place.getAddress()),
      leg(plan, entry, next, date)
    );
  }

  private static String dayLabel(int number, LocalDate date) {
    String label = "Día " + number;
    return date == null ? label : label + " · " + SHORT_DATE.format(date);
  }

  private static String schedule(LocalTime start, int duration) {
    if (start == null) {
      return NO_SCHEDULE;
    }
    return CLOCK.format(start) + " – " + CLOCK.format(start.plusMinutes(duration));
  }

  /** Misma cuenta que el chip del detalle: horas enteras sin minutos, y minutos solos bajo la hora. */
  static String formatDuration(int minutes) {
    int total = Math.max(0, minutes);
    int hours = total / MINUTES_PER_HOUR;
    int rest = total % MINUTES_PER_HOUR;
    if (hours == 0) {
      return rest + "m";
    }
    return rest == 0 ? hours + "h" : hours + "h " + rest + "m";
  }

  private static String categoryLabel(PlaceCategory category) {
    return category == null
      ? CATEGORY_LABELS.get(PlaceCategory.OTHER)
      : CATEGORY_LABELS.getOrDefault(category, CATEGORY_LABELS.get(PlaceCategory.OTHER));
  }

  /**
   * Tramo hacia la parada siguiente, solo dentro del mismo dia: cruzar la medianoche es una jornada
   * nueva, no un viaje. Los minutos fijados a mano mandan; sin ellos se usa la estimacion del
   * planificador con el modo del tramo o el del plan.
   */
  private static String leg(Plan plan, PlanPlace entry, PlanPlace next, LocalDate date) {
    if (next == null || !Objects.equals(date, next.getVisitDate())) {
      return null;
    }
    // `PlanSchedule#getTransportMode` ya normaliza el nulo a WALK, asi que el modo nunca llega
    // nulo aca; el fallback del mapa de etiquetas cubre un enum nuevo sin traducir.
    TransportMode mode = entry.getTransportModeToNext() == null
      ? plan.getTransportMode()
      : entry.getTransportModeToNext();
    int minutes;
    if (entry.getTravelMinutesToNext() != null) {
      minutes = Math.max(1, entry.getTravelMinutesToNext());
    } else {
      minutes =
        PlanScheduleCalculator.travelMinutes(calculatorStop(entry), calculatorStop(next), mode);
    }
    return minutes + " min " + MODE_LABELS.getOrDefault(mode, MODE_LABELS.get(TransportMode.WALK));
  }

  private static PlanScheduleCalculator.Stop calculatorStop(PlanPlace entry) {
    Place place = entry.getPlace();
    return new PlanScheduleCalculator.Stop(
      entry.getVisitDate(),
      entry.getVisitTime(),
      entry.getDurationMinutes(),
      place == null ? null : place.getDefaultDurationMinutes(),
      place == null ? null : place.getCategory(),
      place == null ? null : place.getLatitude(),
      place == null ? null : place.getLongitude(),
      entry.getTransportModeToNext(),
      entry.getTravelMinutesToNext()
    );
  }

  private static String blankToEmpty(String value) {
    return value == null ? "" : value.trim();
  }
}
