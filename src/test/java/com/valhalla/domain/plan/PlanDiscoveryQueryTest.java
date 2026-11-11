package com.valhalla.domain.plan;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link PlanDiscoveryQuery}: el parseo tolerante de parametros crudos y
 * {@code hasFilters()}. Sin Spring y sin base de datos.
 */
public class PlanDiscoveryQueryTest {

  @Test
  public void U14_sortDesconocidoCaeAPopular() {
    PlanDiscoveryQuery query = PlanDiscoveryQuery.parse(null, null, null, "nope");

    assertThat(query.sort(), is(PlanDiscoveryQuery.Sort.POPULAR));
  }

  @Test
  public void U15_categoriaDesconocidaSeIgnora() {
    PlanDiscoveryQuery query = PlanDiscoveryQuery.parse(null, "NOPE", null, null);

    assertThat(query.category(), is(nullValue()));
  }

  @Test
  public void U16_whenDesconocidoCaeAAll() {
    PlanDiscoveryQuery query = PlanDiscoveryQuery.parse(null, null, "nope", null);

    assertThat(query.when(), is(PlanDiscoveryQuery.When.ALL));
  }

  @Test
  public void parseToleraNullYBlancoEnTodosLosParametros() {
    PlanDiscoveryQuery empty = PlanDiscoveryQuery.parse(null, null, null, null);
    assertThat(empty.text(), is(""));
    assertThat(empty.category(), is(nullValue()));
    assertThat(empty.when(), is(PlanDiscoveryQuery.When.ALL));
    assertThat(empty.sort(), is(PlanDiscoveryQuery.Sort.POPULAR));

    PlanDiscoveryQuery blank = PlanDiscoveryQuery.parse("   ", "   ", "   ", "   ");
    assertThat(blank.text(), is(""));
    assertThat(blank.category(), is(nullValue()));
    assertThat(blank.when(), is(PlanDiscoveryQuery.When.ALL));
    assertThat(blank.sort(), is(PlanDiscoveryQuery.Sort.POPULAR));
  }

  @Test
  public void U17_hasFiltersEsFalsoSinTextoNiCategoriaYConWhenAll() {
    PlanDiscoveryQuery query = PlanDiscoveryQuery.parse("   ", null, "all", "date");

    assertThat(query.hasFilters(), is(false));
  }

  @Test
  public void U18_hasFiltersEsVerdaderoConCualquieraDeLosTresFiltros() {
    assertThat(PlanDiscoveryQuery.parse("museo", null, null, null).hasFilters(), is(true));
    assertThat(PlanDiscoveryQuery.parse(null, "BAR", null, null).hasFilters(), is(true));
    assertThat(PlanDiscoveryQuery.parse(null, null, "upcoming", null).hasFilters(), is(true));
  }

  @Test
  public void T_PAGE_001_pageYSizeInvalidosCaelAlDefault() {
    PlanDiscoveryQuery empty = PlanDiscoveryQuery.parse(null, null, null, null, null, null);
    assertThat(empty.page(), is(PlanDiscoveryQuery.DEFAULT_PAGE));
    assertThat(empty.size(), is(PlanDiscoveryQuery.DEFAULT_SIZE));

    PlanDiscoveryQuery invalid = PlanDiscoveryQuery.parse(null, null, null, null, "0", "-4");
    assertThat(invalid.page(), is(PlanDiscoveryQuery.DEFAULT_PAGE));
    assertThat(invalid.size(), is(PlanDiscoveryQuery.DEFAULT_SIZE));

    PlanDiscoveryQuery garbage = PlanDiscoveryQuery.parse(null, null, null, null, "dos", "muchos");
    assertThat(garbage.page(), is(PlanDiscoveryQuery.DEFAULT_PAGE));
    assertThat(garbage.size(), is(PlanDiscoveryQuery.DEFAULT_SIZE));

    PlanDiscoveryQuery blank = PlanDiscoveryQuery.parse(null, null, null, null, "   ", "   ");
    assertThat(blank.page(), is(PlanDiscoveryQuery.DEFAULT_PAGE));
    assertThat(blank.size(), is(PlanDiscoveryQuery.DEFAULT_SIZE));
  }

  @Test
  public void T_PAGE_001_unRecordConstruidoAManoNormalizaLaVentana() {
    PlanDiscoveryQuery invalid = new PlanDiscoveryQuery(
      "",
      null,
      PlanDiscoveryQuery.When.ALL,
      PlanDiscoveryQuery.Sort.POPULAR,
      0,
      0
    );

    assertThat(invalid.page(), is(PlanDiscoveryQuery.DEFAULT_PAGE));
    assertThat(invalid.size(), is(PlanDiscoveryQuery.DEFAULT_SIZE));
  }

  @Test
  public void T_PAGE_001_pageYSizeValidosSeConservan() {
    PlanDiscoveryQuery query = PlanDiscoveryQuery.parse(null, null, null, null, " 3 ", " 5 ");

    assertThat(query.page(), is(3));
    assertThat(query.size(), is(5));
  }
}
