package com.valhalla.infrastructure.weather;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.valhalla.domain.weather.WeatherCondition;
import com.valhalla.domain.weather.WeatherForecast;
import com.valhalla.domain.weather.WeatherOutcome;
import com.valhalla.domain.weather.WeatherStatus;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class WeatherServiceImplTest {

  private static final String BASE_URL = "https://api.open-meteo.com";
  private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 17);
  private static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay(ZONE).toInstant(), ZONE);
  private static final LocalDate DATE = LocalDate.of(2026, 10, 29);

  private RestTemplate restTemplate;
  private WeatherServiceImpl service;

  @BeforeEach
  public void setUp() {
    this.restTemplate = mock(RestTemplate.class);
    this.service = new WeatherServiceImpl(this.restTemplate, BASE_URL, CLOCK);
  }

  @Test
  public void hourlyFor_parseaLaSerieDelDiaYUsaDefaultsParaLoQueElProveedorNoManda() {
    when(this.restTemplate.getForObject(any(URI.class), eq(OpenMeteoResponse.class)))
      .thenReturn(response(), conDatosFaltantes());

    // Serie completa: una fila por hora con todo el dato del proveedor.
    WeatherOutcome outcome = this.service.hourlyFor(-34.6037, -58.3816, DATE);

    assertThat(outcome.status(), is(WeatherStatus.AVAILABLE));
    assertThat(outcome.hourly(), hasSize(3));
    WeatherForecast at18 = outcome.hourly().get(1);
    assertThat(at18.time(), is(LocalTime.of(18, 0)));
    assertThat(at18.temperatureC(), is(21.4));
    assertThat(at18.apparentTemperatureC(), is(20.1));
    assertThat(at18.precipitationProbability(), is(10));
    assertThat(at18.windSpeedKmh(), is(12.0));
    assertThat(at18.condition(), is(WeatherCondition.RAIN));
    assertThat(outcome.hourly().get(0).condition(), is(WeatherCondition.PARTLY_CLOUDY));
    assertThat(outcome.hourly().get(2).condition(), is(WeatherCondition.THUNDERSTORM));

    // Datos opcionales vacíos: sensación = temperatura, lluvia 0 y viento 0.
    List<WeatherForecast> defaults = this.service.hourlyFor(-34.6, -58.4, DATE).hourly();

    assertThat(defaults, hasSize(1));
    assertThat(defaults.get(0).time(), is(LocalTime.NOON));
    assertThat(defaults.get(0).apparentTemperatureC(), is(18.0));
    assertThat(defaults.get(0).precipitationProbability(), is(0));
    assertThat(defaults.get(0).windSpeedKmh(), is(0.0));
    assertThat(defaults.get(0).condition(), is(WeatherCondition.OVERCAST));

    // Y la llamada sale al endpoint del proveedor con el día del plan.
    ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
    verify(this.restTemplate, times(2))
      .getForObject(uriCaptor.capture(), eq(OpenMeteoResponse.class));
    String uri = uriCaptor.getAllValues().get(0).toString();
    assertThat(uri, containsString("/v1/forecast"));
    assertThat(uri, containsString("latitude=-34.6037"));
    assertThat(uri, containsString("longitude=-58.3816"));
    assertThat(uri, containsString("start_date=2026-10-29"));
  }

  @Test
  public void hourlyFor_anteCualquierFalloODatoIncompletoDevuelveSinDatos() {
    // Respuestas rotas: sin datos no se muestra clima y la página no se rompe.
    OpenMeteoResponse[] rotas = {
      null,
      new OpenMeteoResponse(null),
      new OpenMeteoResponse(new OpenMeteoResponse.Hourly(null, null, null, null, null, null)),
      new OpenMeteoResponse(
        new OpenMeteoResponse.Hourly(
          List.of("2026-10-29T12:00"),
          List.of(),
          List.of(),
          List.of(),
          List.of(),
          List.of()
        )
      ),
    };

    for (OpenMeteoResponse respuesta : rotas) {
      when(this.restTemplate.getForObject(any(URI.class), eq(OpenMeteoResponse.class)))
        .thenReturn(respuesta);
      assertThat(
        this.service.hourlyFor(-34.6, -58.4, DATE).status(),
        is(WeatherStatus.UNAVAILABLE)
      );
    }

    // Y si el proveedor tira un error de red, también.
    when(this.restTemplate.getForObject(any(URI.class), eq(OpenMeteoResponse.class)))
      .thenThrow(new RestClientException("sin red"));
    assertThat(this.service.hourlyFor(-34.6, -58.4, DATE).status(), is(WeatherStatus.UNAVAILABLE));
  }

  @Test
  public void hourlyFor_soloPideElClimaConCoordenadasYUnaFechaDentroDelHorizonte() {
    // Sin coordenadas o sin fecha no hay que consultar a nadie.
    assertThat(this.service.hourlyFor(null, -58.4, DATE).status(), is(WeatherStatus.UNAVAILABLE));
    assertThat(this.service.hourlyFor(-34.6, null, DATE).status(), is(WeatherStatus.UNAVAILABLE));
    assertThat(this.service.hourlyFor(-34.6, -58.4, null).status(), is(WeatherStatus.UNAVAILABLE));
    // Fechas pasadas: no hay pronóstico de lo que ya pasó.
    assertThat(
      this.service.hourlyFor(-34.6, -58.4, TODAY.minusDays(1)).status(),
      is(WeatherStatus.UNAVAILABLE)
    );
    // Más allá del horizonte de 16 días: "todavía no hay datos" (pending), sin consultar.
    assertThat(
      this.service.hourlyFor(-34.6, -58.4, TODAY.plusDays(17)).status(),
      is(WeatherStatus.PENDING)
    );
    verify(this.restTemplate, never()).getForObject(any(URI.class), eq(OpenMeteoResponse.class));

    // En el último día del horizonte sí se le pregunta al proveedor.
    when(this.restTemplate.getForObject(any(URI.class), eq(OpenMeteoResponse.class)))
      .thenReturn(null);
    assertThat(
      this.service.hourlyFor(-34.6, -58.4, TODAY.plusDays(16)).status(),
      is(WeatherStatus.UNAVAILABLE)
    );
    verify(this.restTemplate, times(1)).getForObject(any(URI.class), eq(OpenMeteoResponse.class));
  }

  private static OpenMeteoResponse response() {
    return new OpenMeteoResponse(
      new OpenMeteoResponse.Hourly(
        List.of("2026-10-29T17:00", "2026-10-29T18:00", "2026-10-29T19:00"),
        List.of(20.0, 21.4, 22.0),
        List.of(19.0, 20.1, 21.0),
        List.of(5, 10, 15),
        List.of(2, 61, 95),
        List.of(10.0, 12.0, 14.0)
      )
    );
  }

  private static OpenMeteoResponse conDatosFaltantes() {
    return new OpenMeteoResponse(
      new OpenMeteoResponse.Hourly(
        List.of("2026-10-29T12:00"),
        List.of(18.0),
        List.of(),
        List.of(),
        List.of(3),
        List.of()
      )
    );
  }
}
