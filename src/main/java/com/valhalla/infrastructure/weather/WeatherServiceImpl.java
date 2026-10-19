package com.valhalla.infrastructure.weather;

import com.valhalla.config.EnvironmentConfig;
import com.valhalla.domain.weather.WeatherCondition;
import com.valhalla.domain.weather.WeatherForecast;
import com.valhalla.domain.weather.WeatherOutcome;
import com.valhalla.domain.weather.WeatherService;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Cliente del pronóstico horario de Open-Meteo (sin API key). Pide la serie horaria completa del
 * día del plan y arma un {@link WeatherForecast} por hora, así la tarjeta puede mostrar cómo
 * evoluciona el clima a lo largo del recorrido (a las 5 puede estar nublado y a las 8 llover).
 * Es best-effort: las fechas pasadas y las que caen más allá del horizonte del proveedor
 * devuelven {@link WeatherOutcome#unavailable()} o {@link WeatherOutcome#pending()} y ante
 * cualquier falla de red o de datos se devuelve {@link WeatherOutcome#unavailable()} para que el
 * detalle no muestre el clima.
 */
@Service
public class WeatherServiceImpl implements WeatherService {

  private static final Logger LOGGER = LoggerFactory.getLogger(WeatherServiceImpl.class);
  private static final String FORECAST_PATH = "/v1/forecast";
  private static final String HOURLY_FIELDS =
    "temperature_2m,apparent_temperature,precipitation_probability,weather_code,wind_speed_10m";
  private static final String TIME_ZONE = "America/Argentina/Buenos_Aires";
  private static final int FORECAST_HORIZON_DAYS = 16;

  private final RestTemplate restTemplate;
  private final String baseUrl;
  private final Clock clock;

  @Autowired
  public WeatherServiceImpl(RestTemplate weatherRestTemplate) {
    this(weatherRestTemplate, EnvironmentConfig.weatherApiBaseUrl(), systemClock());
  }

  WeatherServiceImpl(RestTemplate restTemplate, String baseUrl, Clock clock) {
    this.restTemplate = restTemplate;
    this.baseUrl = baseUrl;
    this.clock = clock;
  }

  private static Clock systemClock() {
    return Clock.system(ZoneId.of(TIME_ZONE));
  }

  @Override
  public WeatherOutcome hourlyFor(Double latitude, Double longitude, LocalDate date) {
    if (latitude == null || longitude == null || date == null) {
      return WeatherOutcome.unavailable();
    }
    LocalDate today = LocalDate.now(this.clock);
    if (date.isBefore(today)) {
      return WeatherOutcome.unavailable();
    }
    if (date.isAfter(today.plusDays(FORECAST_HORIZON_DAYS))) {
      return WeatherOutcome.pending();
    }
    URI uri = UriComponentsBuilder
      .fromHttpUrl(this.baseUrl)
      .path(FORECAST_PATH)
      .queryParam("latitude", latitude)
      .queryParam("longitude", longitude)
      .queryParam("hourly", HOURLY_FIELDS)
      .queryParam("timezone", TIME_ZONE)
      .queryParam("start_date", date)
      .queryParam("end_date", date)
      .build()
      .encode()
      .toUri();
    try {
      OpenMeteoResponse response = this.restTemplate.getForObject(uri, OpenMeteoResponse.class);
      return this.toHourly(date, response);
    } catch (RestClientException exception) {
      LOGGER.warn(
        "No se pudo obtener el pronóstico para lat={} lon={} fecha={}",
        latitude,
        longitude,
        date,
        exception
      );
      return WeatherOutcome.unavailable();
    }
  }

  private WeatherOutcome toHourly(LocalDate date, OpenMeteoResponse response) {
    if (response == null || response.hourly() == null) {
      return WeatherOutcome.unavailable();
    }
    OpenMeteoResponse.Hourly hourly = response.hourly();
    List<String> times = hourly.time();
    if (times == null) {
      return WeatherOutcome.unavailable();
    }
    List<WeatherForecast> forecasts = new ArrayList<>();
    for (int index = 0; index < times.size(); index++) {
      Double temperature = valueAt(hourly.temperature(), index);
      Integer code = intAt(hourly.weatherCode(), index);
      if (temperature == null || code == null) {
        continue;
      }
      Integer precipitation = intAt(hourly.precipitationProbability(), index);
      forecasts.add(
        new WeatherForecast(
          date,
          LocalTime.of(Integer.parseInt(times.get(index).substring(11, 13)), 0),
          temperature,
          orDefault(valueAt(hourly.apparentTemperature(), index), temperature),
          precipitation == null ? 0 : precipitation,
          orDefault(valueAt(hourly.windSpeed(), index), 0d),
          WeatherCondition.fromCode(code)
        )
      );
    }
    return forecasts.isEmpty() ? WeatherOutcome.unavailable() : WeatherOutcome.available(forecasts);
  }

  private static Double valueAt(List<Double> values, int index) {
    return values == null || index >= values.size() ? null : values.get(index);
  }

  private static Integer intAt(List<Integer> values, int index) {
    return values == null || index >= values.size() ? null : values.get(index);
  }

  private static double orDefault(Double value, double fallback) {
    return value == null ? fallback : value;
  }
}
