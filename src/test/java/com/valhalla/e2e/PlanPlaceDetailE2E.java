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
      page.navigate(baseUrl + "/plans/new");
      page.locator("input[name='name']").fill("Detalle E2E " + System.nanoTime());
      page.locator("main button[type='submit']").click();
      page.waitForURL(java.util.regex.Pattern.compile(".*/plans/\\d+"));
      String detailUrl = page.url();
      try {
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
