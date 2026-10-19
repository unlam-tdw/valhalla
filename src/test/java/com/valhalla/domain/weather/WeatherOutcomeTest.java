package com.valhalla.domain.weather;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

public class WeatherOutcomeTest {

  @Test
  public void availablePendingYUnavailableTraenLaSerieQueCorresponde() {
    // available: con la serie horaria del día (la ve la tarjeta y la tira del detalle).
    List<WeatherForecast> hourly = List.of(sample());
    WeatherOutcome disponible = WeatherOutcome.available(hourly);
    assertThat(disponible.status(), is(WeatherStatus.AVAILABLE));
    assertThat(disponible.hourly(), is(equalTo(hourly)));
    assertThat(WeatherOutcome.available(List.of()).hourly().isEmpty(), is(true));

    // pending: la fecha está fuera del horizonte; unavailable: no hay nada que mostrar.
    WeatherOutcome pendiente = WeatherOutcome.pending();
    assertThat(pendiente.status(), is(WeatherStatus.PENDING));
    assertThat(pendiente.hourly().isEmpty(), is(true));

    WeatherOutcome sinDatos = WeatherOutcome.unavailable();
    assertThat(sinDatos.status(), is(WeatherStatus.UNAVAILABLE));
    assertThat(sinDatos.hourly().isEmpty(), is(true));

    // Como es un valor, dos resultados con lo mismo son el mismo.
    WeatherOutcome otro = WeatherOutcome.available(hourly);
    assertThat(disponible, is(equalTo(otro)));
    assertThat(disponible.hashCode(), is(equalTo(otro.hashCode())));
    assertThat(disponible.toString(), is(containsString("AVAILABLE")));
  }

  private static WeatherForecast sample() {
    return new WeatherForecast(
      LocalDate.of(2026, 10, 29),
      LocalTime.of(18, 30),
      21.4,
      20.1,
      10,
      12.0,
      WeatherCondition.CLEAR
    );
  }
}
