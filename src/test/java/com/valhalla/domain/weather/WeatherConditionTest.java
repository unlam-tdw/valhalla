package com.valhalla.domain.weather;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

public class WeatherConditionTest {

  @Test
  public void fromCode_convierteLosCodigosDeOpenMeteoYElRestoCaeEnUnknown() {
    // Los códigos WMO del proveedor caen en su condición.
    assertThat(WeatherCondition.fromCode(0), is(WeatherCondition.CLEAR));
    assertThat(WeatherCondition.fromCode(1), is(WeatherCondition.PARTLY_CLOUDY));
    assertThat(WeatherCondition.fromCode(2), is(WeatherCondition.PARTLY_CLOUDY));
    assertThat(WeatherCondition.fromCode(3), is(WeatherCondition.OVERCAST));
    assertThat(WeatherCondition.fromCode(45), is(WeatherCondition.FOG));
    assertThat(WeatherCondition.fromCode(48), is(WeatherCondition.FOG));
    assertThat(WeatherCondition.fromCode(51), is(WeatherCondition.DRIZZLE));
    assertThat(WeatherCondition.fromCode(53), is(WeatherCondition.DRIZZLE));
    assertThat(WeatherCondition.fromCode(55), is(WeatherCondition.DRIZZLE));
    assertThat(WeatherCondition.fromCode(56), is(WeatherCondition.FREEZING_RAIN));
    assertThat(WeatherCondition.fromCode(57), is(WeatherCondition.FREEZING_RAIN));
    assertThat(WeatherCondition.fromCode(61), is(WeatherCondition.RAIN));
    assertThat(WeatherCondition.fromCode(63), is(WeatherCondition.RAIN));
    assertThat(WeatherCondition.fromCode(65), is(WeatherCondition.RAIN));
    assertThat(WeatherCondition.fromCode(66), is(WeatherCondition.FREEZING_RAIN));
    assertThat(WeatherCondition.fromCode(67), is(WeatherCondition.FREEZING_RAIN));
    assertThat(WeatherCondition.fromCode(71), is(WeatherCondition.SNOW));
    assertThat(WeatherCondition.fromCode(73), is(WeatherCondition.SNOW));
    assertThat(WeatherCondition.fromCode(75), is(WeatherCondition.SNOW));
    assertThat(WeatherCondition.fromCode(77), is(WeatherCondition.SNOW));
    assertThat(WeatherCondition.fromCode(80), is(WeatherCondition.RAIN_SHOWERS));
    assertThat(WeatherCondition.fromCode(81), is(WeatherCondition.RAIN_SHOWERS));
    assertThat(WeatherCondition.fromCode(82), is(WeatherCondition.RAIN_SHOWERS));
    assertThat(WeatherCondition.fromCode(85), is(WeatherCondition.SNOW_SHOWERS));
    assertThat(WeatherCondition.fromCode(86), is(WeatherCondition.SNOW_SHOWERS));
    assertThat(WeatherCondition.fromCode(95), is(WeatherCondition.THUNDERSTORM));
    assertThat(WeatherCondition.fromCode(96), is(WeatherCondition.THUNDERSTORM));
    assertThat(WeatherCondition.fromCode(99), is(WeatherCondition.THUNDERSTORM));
    assertThat(WeatherCondition.fromCode(999), is(WeatherCondition.UNKNOWN));

    // Y de cada condición la vista usa el texto, el ícono y el aviso.
    assertThat(WeatherCondition.RAIN.getDescription(), is("Lluvia"));
    assertThat(WeatherCondition.RAIN.getIcon(), is("fa-cloud-showers-heavy"));
    assertThat(WeatherCondition.RAIN.getSeverity(), is(WeatherSeverity.WARNING));
    assertThat(WeatherCondition.RAIN.isAlert(), is(true));
    assertThat(WeatherCondition.RAIN.getAlertMessage(), containsString("paraguas"));
    assertThat(WeatherCondition.CLEAR.isAlert(), is(false));
    assertThat(WeatherCondition.CLEAR.getAlertMessage(), is(nullValue()));
  }
}
