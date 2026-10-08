package com.valhalla.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;

public class PlanPlaceDetailE2E {

  @Test
  void addsPersistsAndDisplaysVisitDetails() {
    String baseUrl = System.getProperty("e2e.baseUrl");
    assertNotNull(baseUrl);
    try (
      Playwright playwright = Playwright.create();
      Browser browser = playwright.chromium().launch();
      BrowserContext context = browser.newContext()
    ) {
      Page page = context.newPage();
      page.navigate(baseUrl + "/auth/login");
      page.locator("input[name='username']").fill("test@unlam.edu.ar");
      page.locator("input[name='password']").fill("password");
      page.locator("#btn-login").click();
      // Ya no existe /plans/new: el plan se crea desde el modal de /explore, que publica
      // a POST /plans. El catálogo del paso 2 lo alimenta fetch('/api/places') del onMounted.
      page.navigate(baseUrl + "/explore");
      page.waitForURL(java.util.regex.Pattern.compile(".*/explore"));
      page.locator("aside[aria-label='Plans'] button:has-text('Create plan')").click();
      page.locator("#plan-name").fill("Detalle E2E " + System.nanoTime());
      page.locator("#plan-description").fill("");
      page.locator("#plan-date").fill("2026-12-01");
      // El modal no expone la hora del plan, pero POST /plans bindea eventTime si el campo
      // viaja en el form: se inyecta para conservar la hora que el detalle sí muestra.
      page
        .locator("#plan-name")
        .evaluate(
          "el => { const i = document.createElement('input'); i.type = 'time'; i.name = 'eventTime'; i.value = '18:30'; el.form.appendChild(i); }"
        );
      page.locator("button:has-text('Siguiente')").click();
      // Paso 2: el primer lugar del catálogo entra al borrador y le ponemos la hora de visita.
      page.locator("ol li button[aria-label^='Add ']").first().click();
      page.locator("ol li input[aria-label='Hora de visita']").fill("18:30");
      page.locator("button:has-text('Crear plan')").click();
      page.waitForURL(java.util.regex.Pattern.compile(".*/plans/\\d+"));
      String detailUrl = page.url();
      try {
        assertThat(page.locator("input[name='eventDate']")).hasValue("2026-12-01");
        assertThat(page.locator("input[name='eventTime']")).hasValue("18:30");
        page
          .locator("select.form-select")
          .selectOption(new com.microsoft.playwright.options.SelectOption().setIndex(1));
        page.getByText("+ Agregar", new Page.GetByTextOptions().setExact(true)).click();
        Locator more = page.getByText("Ver más", new Page.GetByTextOptions().setExact(true));
        assertThat(more).hasCount(1);
        more.click();
        assertThat(page.locator("dialog")).isVisible();
        assertThat(page.locator("dialog")).containsText("Sin fecha");
        assertThat(page.locator("dialog")).containsText("Posición en el itinerario");
        page.locator("dialog .btn-close").click();
        page.reload();
        assertThat(more).hasCount(1);
        Object updated = page.evaluate(
          """
          async () => {
            const url = document.querySelector('form[action$="/delete"]').action.replace('/plans/', '/api/plans/').replace('/delete', '/places');
            const entries = await (await fetch(url)).json();
            const response = await fetch(url + '/' + entries[0].id, {
              method: 'PUT', headers: {'Content-Type': 'application/json', 'X-CSRF-TOKEN': document.querySelector('meta[name="csrf-token"]').content},
              body: JSON.stringify({visitDate: '2026-12-01', visitTime: '10:30'})
            });
            return response.ok;
          }
          """
        );
        assertEquals(Boolean.TRUE, updated);
        more.click();
        assertThat(page.locator("dialog")).containsText("2026-12-01");
        assertThat(page.locator("dialog")).containsText("10:30");
        page.locator("dialog .btn-close").click();
        page.reload();
        page.getByText("Guardar cambios", new Page.GetByTextOptions().setExact(true)).click();
        more.click();
        assertThat(page.locator("dialog")).containsText("2026-12-01");
        page.locator("dialog .btn-close").click();
        assertThat(page.locator(".stop-schedule")).containsText("01/12/2026");
        assertThat(page.locator(".stop-schedule")).containsText("10:30");
        page
          .locator("select.form-select")
          .selectOption(new com.microsoft.playwright.options.SelectOption().setIndex(1));
        page.getByText("+ Agregar", new Page.GetByTextOptions().setExact(true)).click();
        assertThat(more).hasCount(2);
        String firstName = page.locator(".stop-title").nth(0).innerText();
        String secondName = page.locator(".stop-title").nth(1).innerText();
        page.locator(".place-item").nth(0).dragTo(page.locator(".place-item").nth(1));
        assertThat(page.locator(".stop-title").nth(0)).hasText(secondName);
        assertThat(page.locator(".stop-title").nth(1)).hasText(firstName);
        page.waitForCondition(() -> !page.locator(".stop-remove").nth(0).isDisabled());
        page.reload();
        assertThat(page.locator(".stop-title").nth(0)).hasText(secondName);
        page.screenshot(
          new Page.ScreenshotOptions()
            .setPath(java.nio.file.Path.of("target/itinerary-desktop.png"))
            .setFullPage(true)
        );
        page.setViewportSize(390, 844);
        page.locator(".itinerary-panel").scrollIntoViewIfNeeded();
        page.screenshot(
          new Page.ScreenshotOptions()
            .setPath(java.nio.file.Path.of("target/itinerary-mobile.png"))
            .setFullPage(true)
        );
        assertTrue(
          (Boolean) page.evaluate(
            "document.querySelector('.itinerary-panel').scrollWidth <= document.querySelector('.itinerary-panel').clientWidth"
          )
        );
        page.locator(".stop-remove").nth(0).click();
        assertThat(more).hasCount(1);
        page.getByText("Eliminar", new Page.GetByTextOptions().setExact(true)).click();
        assertThat(more).hasCount(0);
        page.reload();
        assertThat(more).hasCount(0);
      } finally {
        page.navigate(detailUrl);
        page.getByText("Eliminar plan", new Page.GetByTextOptions().setExact(true)).click();
      }
    }
  }
}
