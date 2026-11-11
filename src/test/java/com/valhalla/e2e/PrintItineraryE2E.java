package com.valhalla.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.Media;
import com.valhalla.e2e.views.PlansPage;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * E2E del itinerario imprimible (docs/specs/27-PRINT.md, E-PRINT-01..03): con la media de impresion
 * emulada, la pagina se reduce a la hoja del plan —sin navbar, mapa ni botones—, agrupa por dia y
 * fuerza el tema claro aunque el usuario tenga el oscuro activo.
 */
public class PrintItineraryE2E extends E2eBase {

  private static final String START_TIME = "09:00";

  private PlansPage plans;
  private String planName;

  @BeforeEach
  void signInAndPreparePlanPage() {
    signInAsAdmin();
    this.plans = new PlansPage(page);
    this.planName = "Itinerario impreso E2E " + System.nanoTime();
  }

  @Test
  void E_PRINT_01_printMediaHidesTheChromeAndKeepsTheItinerary() {
    createPlanWithTwoStops();
    String firstStop = page.locator(".stop-title").nth(0).innerText();

    page.emulateMedia(new Page.EmulateMediaOptions().setMedia(Media.PRINT));

    assertThat(page.locator("body > nav")).isHidden();
    assertThat(page.locator("#plan-map")).isHidden();
    assertThat(page.locator("#plan-app")).isHidden();
    assertThat(page.locator("[data-print-plan]")).isHidden();
    assertThat(page.locator("[data-print-sheet]")).isVisible();
    assertThat(page.locator("[data-print-sheet]")).containsText(planName);
    assertThat(page.locator("[data-print-sheet]")).containsText(firstStop);
  }

  @Test
  void E_PRINT_02_aMultiDayPlanPrintsOneHeadingPerDayInItineraryOrder() {
    createPlanWithTwoStops();
    String nextDay = isoDateIn(31);
    pinStopToNextDay(1, nextDay, "10:00");
    page.reload();
    page.waitForLoadState(LoadState.NETWORKIDLE);
    assertThat(page.locator("li.place-item")).hasCount(2);

    page.emulateMedia(new Page.EmulateMediaOptions().setMedia(Media.PRINT));

    assertThat(page.locator("[data-print-sheet] .print-day")).hasCount(2);
    assertThat(page.locator("[data-print-sheet] .print-day-heading")).hasCount(2);
    assertThat(page.locator("[data-print-sheet] .print-day-heading").nth(0)).containsText("Día 1");
    assertThat(page.locator("[data-print-sheet] .print-day-heading").nth(1)).containsText("Día 2");

    List<String> itineraryOrder = page.locator(".stop-title").allInnerTexts();
    List<String> printedOrder = page.locator("[data-print-sheet] .print-stop-name").allInnerTexts();
    assertEquals(itineraryOrder, printedOrder, "el papel respeta el orden del itinerario");
  }

  @Test
  void E_PRINT_03_darkThemeStillPrintsBlackTextOnWhite() {
    createPlanWithTwoStops();
    page.click("body > nav button[data-theme-toggle]");
    page.waitForLoadState(LoadState.NETWORKIDLE);
    assertThat(page.locator("html")).hasAttribute("data-theme", "dark");

    page.emulateMedia(new Page.EmulateMediaOptions().setMedia(Media.PRINT));

    assertEquals(
      "rgb(0, 0, 0)",
      computedColor(".print-sheet-title"),
      "el titulo impreso sale negro"
    );
    assertEquals(
      "rgb(0, 0, 0)",
      computedColor(".print-stop-name"),
      "las paradas impresas salen negras"
    );
    assertEquals(
      "rgb(255, 255, 255)",
      (String) page.evaluate("() => getComputedStyle(document.body).backgroundColor"),
      "el fondo impreso sale blanco"
    );
    assertEquals(
      "rgb(255, 255, 255)",
      (String) page.evaluate(
        "() => getComputedStyle(document.querySelector('[data-print-sheet]')).backgroundColor"
      ),
      "la hoja impresa sale blanca"
    );
  }

  private String computedColor(String selector) {
    return (String) page.evaluate(
      "selector => getComputedStyle(document.querySelector(selector)).color",
      selector
    );
  }

  private void createPlanWithTwoStops() {
    plans.navigateToCreatePlanModal();
    plans.typeName(planName);
    plans.typeDescription("Dos paradas para imprimir");
    plans.typeEventDate(isoDateIn(30));
    plans.typeStartTime(START_TIME);
    plans.selectTransportMode("WALK");
    plans.clickNextStep();
    plans.addDraftPlace(0);
    plans.addDraftPlace(1);
    plans.clickNextStep();
    plans.clickCreate();
    plans.waitForDetailPath();
    assertThat(page.locator("li.place-item")).hasCount(2);
  }

  /**
   * Mueve una parada al dia siguiente usando el mismo endpoint del editor de horario. El detalle no
   * expone un input de fecha en la lista; el pin viaja por la API, como en PlanPlaceDetailE2E.
   */
  private void pinStopToNextDay(int index, String isoDate, String time) {
    Object updated = page.evaluate(
      """
      async args => {
        const planId = location.pathname.split('/').pop();
        const url = '/api/plans/' + planId + '/places';
        const entries = await (await fetch(url)).json();
        const header = document.querySelector('meta[name="_csrf_header"]');
        const token = document.querySelector('meta[name="_csrf"]');
        if (!header || !token) {
          throw new Error('Missing CSRF metas');
        }
        const response = await fetch(url + '/' + entries[args.index].id, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json', [header.content]: token.content },
          body: JSON.stringify({ visitDate: args.date, visitTime: args.time })
        });
        return response.ok;
      }
      """,
      Map.of("index", index, "date", isoDate, "time", time)
    );
    assertTrue(Boolean.TRUE.equals(updated), "el endpoint del itinerario tiene que aceptar el pin");
  }
}
