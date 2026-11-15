package com.valhalla.domain.plan;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.planplace.PlanPlace;
import java.util.List;
import java.util.Locale;

/**
 * [SCH] Resumen de lectura del itinerario: cuantas paradas tiene el plan, cuánto tiempo total
 * lleva (visitas mas traslados) y cuántos kilometros de traslado acumula.
 *
 * <p>Es una funcion pura que se apoya en la misma calculadora que deriva el horario
 * ({@link PlanScheduleCalculator}): la cascada de duraciones y el estimador de tramos son los
 * mismos metodos, asi que el resumen nunca muestra un viaje distinto al que el plan ya asumio.
 * La distancia es la misma cuenta con rodeo del estimador; si a una parada le falta coordenada,
 * el tramo no aporta kilometros en vez de inventar una geometria nueva.
 *
 * <p>No toca persistencia ni Spring: el detalle y la vista publica la arman en el controller y
 * el template solo lee las etiquetas ya formateadas.
 *
 * @param stopCount cantidad de paradas del itinerario
 * @param totalMinutes tiempo total del recorrido: visitas mas traslados, en minutos
 * @param distanceKm kilometros de traslado estimados, o {@code null} si ningun tramo pudo
 *     medirse por falta de coordenadas
 */
public record PlanItinerarySummary(int stopCount, int totalMinutes, Double distanceKm) {
  private static final int MINUTES_PER_HOUR = 60;

  /**
   * Arma el resumen de {@code plan} a partir de sus paradas en orden.
   *
   * <p>El tiempo total es la suma de las duraciones efectivas de cada visita mas los traslados
   * entre paradas consecutivas (la ultima parada no suma tramo, igual que en la derivacion). Los
   * minutos fijados a mano ({@code travelMinutesToNext}) valen tal cual, con el mismo piso de 1
   * minuto de la calculadora y sin el techo de 90, porque son tramos que el usuario conocio.
   *
   * @param plan plan a resumir; nulo o sin paradas devuelve un resumen vacio
   * @return el resumen del itinerario, con la distancia en {@code null} si ningun tramo la pudo
   *     estimar por falta de coordenadas
   */
  public static PlanItinerarySummary of(Plan plan) {
    if (plan == null || plan.getPlanPlaces() == null || plan.getPlanPlaces().isEmpty()) {
      return new PlanItinerarySummary(0, 0, null);
    }
    List<PlanPlace> stops = plan.getPlanPlaces();
    int minutes = 0;
    double distanceKm = 0;
    boolean hasDistance = false;
    for (int index = 0; index < stops.size(); index++) {
      minutes += stopMinutes(stops.get(index));
      if (index + 1 < stops.size()) {
        PlanScheduleCalculator.Stop from = toCalculatorStop(stops.get(index));
        PlanScheduleCalculator.Stop to = toCalculatorStop(stops.get(index + 1));
        minutes += PlanScheduleCalculator.travelMinutes(from, to, plan.getTransportMode());
        Double legKm = PlanScheduleCalculator.roadDistanceKm(from, to);
        if (legKm != null) {
          distanceKm += legKm;
          hasDistance = true;
        }
      }
    }
    return new PlanItinerarySummary(stops.size(), minutes, hasDistance ? distanceKm : null);
  }

  /**
   * Duracion efectiva de una visita, resuelta con la cascada de la calculadora: override de la
   * visita, duracion del lugar y default de la categoria.
   */
  private static int stopMinutes(PlanPlace stop) {
    Place place = stop.getPlace();
    return PlanScheduleCalculator.effectiveDuration(
      stop.getDurationMinutes(),
      place == null ? null : place.getDefaultDurationMinutes(),
      place == null ? null : place.getCategory()
    );
  }

  /**
   * Duracion total ya formateada para el chip: {@code "6 h 20 min"}; bajo la hora queda
   * {@code "45 min"} y las horas exactas van sin minutos, {@code "2 h"}.
   */
  public String durationLabel() {
    int hours = totalMinutes / MINUTES_PER_HOUR;
    int rest = totalMinutes % MINUTES_PER_HOUR;
    if (hours == 0) {
      return rest + " min";
    }
    return rest == 0 ? hours + " h" : hours + " h " + rest + " min";
  }

  /**
   * Distancia total ya formateada con coma decimal, {@code "12,4 km"}; vacia cuando no se pudo
   * estimar, para que el chip la omita en vez de mostrar un cero falso.
   */
  public String distanceLabel() {
    if (distanceKm == null) {
      return "";
    }
    return String.format(Locale.forLanguageTag("es"), "%.1f", distanceKm) + " km";
  }

  /**
   * Linea completa del chip, en espanol y con separador de punto medio: {@code "5 paradas ·
   * 6 h 20 min · 12,4 km"}. El tramo de kilometros desaparece cuando no hay distancia estimable.
   */
  public String headline() {
    StringBuilder label = new StringBuilder();
    label.append(stopCount).append(stopCount == 1 ? " parada" : " paradas");
    label.append(" · ").append(durationLabel());
    if (distanceKm != null) {
      label.append(" · ").append(distanceLabel());
    }
    return label.toString();
  }

  /**
   * Una parada tal como la lee la calculadora. Los pines no viajan: ni la fecha ni la hora fijada
   * cambian la duracion de la visita ni el traslado del tramo, que es todo lo que este resumen
   * suma.
   */
  private static PlanScheduleCalculator.Stop toCalculatorStop(PlanPlace stop) {
    Place place = stop.getPlace();
    return new PlanScheduleCalculator.Stop(
      null,
      null,
      stop.getDurationMinutes(),
      place == null ? null : place.getDefaultDurationMinutes(),
      place == null ? null : place.getCategory(),
      place == null ? null : place.getLatitude(),
      place == null ? null : place.getLongitude(),
      stop.getTransportModeToNext(),
      stop.getTravelMinutesToNext()
    );
  }
}
