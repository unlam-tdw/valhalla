package com.valhalla.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;

import com.microsoft.playwright.Locator;
import com.valhalla.e2e.views.PlansPage;
import com.valhalla.e2e.views.WebPage;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * [PAGE] T-PAGE-004: "Cargar mas" agrega cards al feed sin recargar y conserva los filtros.
 *
 * <p>El arrange crea nueve planes publicos desde el admin y usa {@code size=3}: cada pagina llena
 * una fila completa en la grilla de escritorio. El observador es un visitante sin sesion. El primer
 * click agrega la pagina 2 y el segundo la 3, asi el test cubre cargas sucesivas: el boton sigue
 * disponible mientras haya mas y desaparece recien en la ultima pagina.
 */
public class ForYouPaginationE2E extends E2eBase {

  private static final List<String> PLAN_NAMES = List.of(
    "Paginacion 01 E2E",
    "Paginacion 02 E2E",
    "Paginacion 03 E2E",
    "Paginacion 04 E2E",
    "Paginacion 05 E2E",
    "Paginacion 06 E2E",
    "Paginacion 07 E2E",
    "Paginacion 08 E2E",
    "Paginacion 09 E2E"
  );
  private static final String QUERY = "Paginacion";

  @Test
  void T_PAGE_004_cargarMasAgregaCardsYConservaFiltros() {
    givenNinePublicPlans();

    newContext();
    WebPage web = new WebPage(page);
    web.navigate(web.baseUrl() + "/for-you?q=" + QUERY + "&sort=name&size=3");
    web.waitForPath("/for-you");

    // Página 1: fila completa de tres cards, el botón con fallback y los filtros intactos.
    Locator cards = page.locator("main article");
    Locator loadMore = page.locator("[data-load-more]");
    assertThat(cards).hasCount(3);
    assertThat(cards.first()).containsText(PLAN_NAMES.get(0));
    assertThat(cards.nth(2)).containsText(PLAN_NAMES.get(2));
    assertThat(loadMore).hasText("Cargar más");
    assertThat(page.locator("input[name='q']")).hasValue(QUERY);
    assertThat(page.locator("select[name='sort']")).hasValue("name");
    MatcherAssert.assertThat(page.url(), containsString("q=" + QUERY));
    MatcherAssert.assertThat(page.url(), containsString("sort=name"));

    // Primer click: se agrega la pagina 2 completa sin navegar ni perder la fila anterior.
    loadMore.click();
    assertThat(cards).hasCount(6);
    assertThat(cards.nth(3)).containsText(PLAN_NAMES.get(3));
    assertThat(cards.nth(5)).containsText(PLAN_NAMES.get(5));
    assertThat(loadMore).hasText("Cargar más");
    MatcherAssert.assertThat(page.url(), containsString("q=" + QUERY));
    MatcherAssert.assertThat(page.url(), containsString("sort=name"));
    assertThat(page.locator("input[name='q']")).hasValue(QUERY);

    // Segundo click: pagina 3 completa; hasMore false retira el botón.
    loadMore.click();
    assertThat(cards).hasCount(9);
    assertThat(page.locator("[data-load-more]")).hasCount(0);

    List<String> names = page.locator("main article h3").allInnerTexts();
    MatcherAssert.assertThat(
      "el orden del ranking se conserva al agregar",
      names,
      contains(PLAN_NAMES.toArray(String[]::new))
    );
    MatcherAssert.assertThat(page.url(), containsString("q=" + QUERY));
    assertThat(page.locator("select[name='sort']")).hasValue("name");
  }

  private void givenNinePublicPlans() {
    signInAsAdmin();
    PlansPage plans = new PlansPage(page);
    for (int index = 0; index < PLAN_NAMES.size(); index++) {
      plans.createPublicPlanViaExplore(
        PLAN_NAMES.get(index),
        "Plan paginado " + (index + 1),
        isoDateIn(index + 1)
      );
    }
  }
}
