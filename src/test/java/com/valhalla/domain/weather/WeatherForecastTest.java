package com.valhalla.domain.weather;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

public class WeatherForecastTest {

  @Test
  public void exponeSusComponentesYTieneIdentidadDeValor() {
    WeatherForecast forecast = sample();

    // Una fila de la tira horaria: hora, temperatura, viento y condición.
    assertThat(forecast.date(), is(LocalDate.of(2026, 10, 29)));
    assertThat(forecast.time(), is(LocalTime.of(18, 30)));
    assertThat(forecast.temperatureC(), is(21.4));
    assertThat(forecast.apparentTemperatureC(), is(20.1));
    assertThat(forecast.precipitationProbability(), is(10));
    assertThat(forecast.windSpeedKmh(), is(12.0));
    assertThat(forecast.condition(), is(WeatherCondition.CLEAR));

    // Como es un valor, dos filas con los mismos datos son la misma fila.
    assertThat(forecast, is(equalTo(sample())));
    assertThat(forecast.hashCode(), is(equalTo(sample().hashCode())));
    assertThat(forecast.toString(), is(notNullValue()));
    assertThat(forecast.toString(), containsString("CLEAR"));
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
