package com.valhalla.e2e;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitUntilState;
import com.valhalla.e2e.views.WebPage;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * [17-DIS] El modo oscuro no puede dejar texto invisible: los controles nativos y las etiquetas
 * de categoria tienen que conservar contraste sobre las superficies oscuras.
 */
public class ExploreDarkContrastE2E extends E2eBase {

  /**
   * Recorre los textos que se pintan con color propio (etiquetas de categoria y opciones de los
   * selects) y devuelve los que no llegan a AA. Tambien verifica que el navegador reciba
   * {@code color-scheme: dark}, que es lo que pinta el popup nativo del select en oscuro.
   */
  private static final String AUDIT =
    """
    () => {
      // Canvas normaliza cualquier notacion (rgb, oklch, color(srgb), ...) a pixeles sRGB:
      // parsear el string a mano fallaba con las utilidades oklch de Tailwind v4.
      const canvas = document.createElement('canvas');
      canvas.width = 1;
      canvas.height = 1;
      const ctx = canvas.getContext('2d');
      const parse = (value) => {
        ctx.clearRect(0, 0, 1, 1);
        ctx.fillStyle = value;
        ctx.fillRect(0, 0, 1, 1);
        const [r, g, b, alpha] = ctx.getImageData(0, 0, 1, 1).data;
        return { r, g, b, a: alpha / 255 };
      };
      const lum = ({ r, g, b }) => {
        const f = (v) => {
          v /= 255;
          return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
        };
        return 0.2126 * f(r) + 0.7152 * f(g) + 0.0722 * f(b);
      };
      const ratio = (a, b) => {
        const x = lum(a);
        const y = lum(b);
        return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05);
      };
      const backgroundOf = (el) => {
        let node = el;
        while (node) {
          const c = parse(getComputedStyle(node).backgroundColor);
          if (c && c.a === 1) return c;
          node = node.parentElement;
        }
        return parse(getComputedStyle(document.body).backgroundColor);
      };
      const failures = [];
      const scheme = getComputedStyle(document.documentElement).colorScheme;
      if (scheme !== 'dark') failures.push('color-scheme: ' + scheme);

      const labels = Array.from(document.querySelectorAll('.place-category-label'));
      if (labels.length < 10) failures.push('etiquetas de categoria: ' + labels.length);
      for (const label of labels) {
        const fg = parse(getComputedStyle(label).color);
        const bg = backgroundOf(label);
        const value = ratio(fg, bg);
        if (value < 4.5) {
          failures.push('categoria ' + label.textContent.trim() + ': ' + value.toFixed(2));
        }
      }

      const options = Array.from(document.querySelectorAll('option'));
      if (options.length < 10) failures.push('opciones: ' + options.length);
      for (const option of options) {
        const style = getComputedStyle(option);
        const fg = parse(style.color);
        const bg = parse(style.backgroundColor);
        if (!bg || bg.a < 1) {
          failures.push('opcion sin fondo: ' + option.textContent.trim());
          continue;
        }
        const value = ratio(fg, bg);
        if (value < 4.5) {
          failures.push('opcion ' + option.textContent.trim() + ': ' + value.toFixed(2));
        }
      }
      return failures;
    }
    """;

  @Test
  void keepsCategoryLabelsAndPermissionOptionsReadableInDarkMode() {
    signInAsAdmin();
    page.navigate(
      new WebPage(page).baseUrl() + "/explore",
      new Page.NavigateOptions().setWaitUntil(WaitUntilState.COMMIT)
    );
    page.waitForSelector("#map.leaflet-container");
    page.waitForFunction(
      "() => document.querySelectorAll('#explore-app > aside article').length >= 10"
    );

    // El toggle es un submit POST: el click espera la navegacion de vuelta.
    page.click("body > nav button[data-theme-toggle]");
    page.waitForFunction("() => document.documentElement.dataset.theme === 'dark'");
    assertThat(page.locator("html").getAttribute("data-theme"), is("dark"));
    // El toggle recarga la pagina: hay que esperar de nuevo a que Vue pinte las tarjetas.
    page.waitForFunction(
      "() => document.querySelectorAll('#explore-app > aside article').length >= 10"
    );

    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create plan")).click();
    page.waitForSelector("#plans-aside");

    @SuppressWarnings("unchecked")
    List<String> failures = (List<String>) page.evaluate(AUDIT);
    assertThat("contraste en oscuro: " + failures, failures, is(empty()));
  }
}
