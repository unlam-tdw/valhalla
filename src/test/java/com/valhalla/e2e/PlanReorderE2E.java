package com.valhalla.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.Route;
import com.valhalla.e2e.views.PlansPage;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * E-01 a E-04 de la spec de reorden por drag &amp; drop (29-REO): la manija es un botón de
 * pointer events y el reacomodo en vivo, el preview del horario, el teclado, el táctil y los
 * pines se ejercitan sobre el detalle del plan.
 *
 * <p>Los arrastres son la misma secuencia sintética de PointerEvent sobre el handle que usa
 * {@code PlanPlaceDetailE2E}: down al medio de la manija, move a la mitad superior de la fila
 * destino (la ranura de inserción es la primera fila cuyo punto medio queda debajo del puntero)
 * y up en el mismo lugar. El down y el move van en un {@code evaluate} y el up en otro para que
 * las aserciones de Java vean el estado a mitad del arrastre —reacomodo, preview y resumen en
 * vivo— antes de soltar.
 *
 * <p>{@link E2eBase} limpia la base antes de cada test, así que el plan de cada test es el único
 * alrededor y sus paradas salen del catálogo sembrado (lugares distintos, con duraciones y
 * coordenadas distintas).
 */
public class PlanReorderE2E extends E2eBase {

  private static final String START_TIME = "09:00";
  private static final String TRANSPORT_MODE = "BIKE";
  private static final String PINNED_TIME = "14:00";
  private static final int STOP_COUNT = 3;

  private PlansPage plans;
  private String planName;

  @BeforeEach
  void signInAndPreparePlanPage() {
    signInAsAdmin();
    this.plans = new PlansPage(page);
    this.planName = "Reorden manija E2E " + System.nanoTime();
  }

  /**
   * E-01 (AC-01, AC-02, AC-03, AC-05): agarrar la manija de la parada 3, llevarla arriba de la
   * parada 1 y soltar. En vivo la fila ya está primera y el preview del horario le derivó la
   * hora de inicio del plan; al soltar el servidor confirma el POST /reorder y, recargada la
   * página, el orden persiste.
   */
  @Test
  void E01_arrastreDeLaManijaReacomodaEnVivoYPersiste() {
    createPlanWithThreeStops();
    Locator titles = page.locator(".stop-title");
    Locator schedules = page.locator("li.place-item .stop-schedule");
    String thirdTitle = titles.nth(2).innerText();
    String thirdScheduleBefore = schedules.nth(2).innerText();

    // Manija de la parada 3 hacia la ranura de la parada 1: mitad superior de la fila 0.
    startHandleDrag(2, 0, "mouse");

    // Reacomodo en vivo antes de soltar: la fila levantada ya está primera (reacomodo) y su
    // horario cambió (preview derivado del espejo: arranca en la hora de inicio del plan,
    // no en la hora que le tocaba al final de la cadena). El orden de las aserciones importa:
    // las que reintentan garantizan que el reacomodo ya se pintó cuando se lee el innerText.
    assertThat(page.locator("li.place-item.is-dragged")).hasCount(1);
    assertThat(titles.nth(0)).hasText(thirdTitle);
    assertThat(schedules.nth(0)).containsText(START_TIME);
    assertNotEquals(thirdScheduleBefore, schedules.nth(0).innerText());
    assertThat(page.locator(".drag-summary")).isVisible();

    Response reorder = dropHandle();
    assertEquals(200, reorder.status(), "el reorden tiene que guardarlo el servidor");
    assertThat(titles.nth(0)).hasText(thirdTitle);

    // Persistencia: recargada la página, el servidor manda el orden nuevo.
    page.reload();
    assertThat(page.locator("li.place-item")).hasCount(STOP_COUNT);
    assertThat(page.locator(".stop-title").nth(0)).hasText(thirdTitle);
  }

  /**
   * E-02 (AC-09, AC-11): teclado en la manija. Enter levanta (el aria-live anuncia la parada y
   * su posición), ↓ mueve la ranura a la siguiente posición y Enter suelta; el orden persiste y
   * el foco vuelve a la manija de la fila movida para que el siguiente drag por teclado sepa de
   * qué parada habla.
   */
  @Test
  void E02_tecladoLevantaMueveYSueltaLaParada() {
    createPlanWithThreeStops();
    Locator titles = page.locator(".stop-title");
    String firstTitle = titles.nth(0).innerText();

    Locator handle = page.locator(".drag-handle").nth(0);
    handle.focus();

    // Enter levanta: el aria-live anuncia la parada con su posición y el resumen en vivo
    // aparece igual que con el puntero.
    handle.press("Enter");
    assertThat(page.locator("#reorder-live")).containsText("posición 1 de 3");
    assertThat(page.locator(".drag-summary")).isVisible();

    // ↓ mueve la ranura a la posición siguiente: el anuncio la sigue en vivo.
    handle.press("ArrowDown");
    assertThat(page.locator("#reorder-live")).containsText("posición 2 de 3");

    // Enter suelta: el drop persiste con el mismo POST /reorder del drag por puntero.
    Response reorder = page.waitForResponse("**/reorder", () -> handle.press("Enter"));
    assertEquals(200, reorder.status(), "el reorden por teclado también se guarda");
    assertThat(page.locator("#reorder-live")).containsText("soltada en la posición 2");

    // [REO] D-7: la manija de la fila movida recupera el foco (antes de recargar, porque el
    // navegador pierde el foco con la navegación).
    assertThat(page.locator("li.place-item").nth(1).locator(".drag-handle")).isFocused();

    // Persistencia: la primera parada quedó segunda.
    page.reload();
    assertThat(page.locator("li.place-item")).hasCount(STOP_COUNT);
    assertThat(page.locator(".stop-title").nth(1)).hasText(firstTitle);
  }

  /**
   * E-03 (AC-10): drag táctil de la manija en viewport de teléfono. El reorden funciona con un
   * puntero de tipo touch y la página no scrollea durante el drag, porque el {@code
   * touch-action: none} vive solo en la manija.
   */
  @Test
  void E03_dragTactilReordenaSinScrollearLaPagina() {
    // El drag táctil necesita un contexto móvil (isMobile + hasTouch): se reemplaza el
    // contexto default por uno de teléfono y se entra de nuevo, porque la sesión no cruza
    // contextos.
    openTouchContext();
    signInAsAdmin();
    this.plans = new PlansPage(page);
    createPlanWithThreeStops();
    Locator titles = page.locator(".stop-title");
    String thirdTitle = titles.nth(2).innerText();

    long scrollBefore = scrollY();
    startHandleDrag(2, 0, "touch");

    // Durante el drag la página no scrollea y el reacomodo en vivo es el mismo del mouse.
    assertEquals(scrollBefore, scrollY(), "el drag táctil no tiene que scrollear la página");
    assertThat(page.locator("li.place-item.is-dragged")).hasCount(1);
    assertThat(titles.nth(0)).hasText(thirdTitle);
    assertThat(page.locator(".drag-summary")).isVisible();

    Response reorder = dropHandle();
    assertEquals(200, reorder.status(), "el reorden táctil tiene que guardarlo el servidor");

    page.reload();
    assertThat(page.locator("li.place-item")).hasCount(STOP_COUNT);
    assertThat(page.locator(".stop-title").nth(0)).hasText(thirdTitle);
  }

  /**
   * E-04 (AC-06): plan con una parada fijada. La parada 3 se fija a las 14:00 por la API, se
   * reordena a la primera ranura y conserva su hora; la parada que le sigue deja de arrancar en
   * la hora de inicio del plan y sale desde el fin de la fijada, tanto en el preview en vivo
   * como en el horario que el servidor recalcula.
   */
  @Test
  void E04_paradaFijadaConservaSuHoraYElRestoSaleDesdeElla() {
    createPlanWithThreeStops();
    // La vista del detalle dibuja el horario read-only, así que el pin va por PUT al endpoint
    // del itinerario con el CSRF que la página lleva, igual que hace la propia app.
    pinVisitTime(2, PINNED_TIME);
    page.reload();
    assertThat(page.locator("li.place-item .stop-pinned")).hasCount(1);
    Locator titles = page.locator(".stop-title");
    Locator schedules = page.locator("li.place-item .stop-schedule");
    String thirdTitle = titles.nth(2).innerText();
    assertThat(schedules.nth(2)).containsText(PINNED_TIME);

    // La parada fijada pasa a la ranura de la primera: manija suya, move a la mitad superior
    // de la fila 0.
    startHandleDrag(2, 0, "mouse");

    // El preview del espejo respeta el pin: la fijada muestra 14:00 en su ranura nueva y la
    // parada que le sigue sale desde el fin de la fijada, no desde las 09:00.
    assertThat(titles.nth(0)).hasText(thirdTitle);
    assertThat(schedules.nth(0)).containsText(PINNED_TIME);
    assertThat(schedules.nth(1)).not().containsText(START_TIME);

    Response reorder = dropHandle();
    assertEquals(200, reorder.status(), "el reorden tiene que guardarlo el servidor");

    // Persistencia y recálculo del servidor: la fijada conserva 14:00 en la primera posición y
    // la cadena siguiente arranca desde ahí.
    page.reload();
    assertThat(page.locator(".stop-title").nth(0)).hasText(thirdTitle);
    assertThat(page.locator("li.place-item .stop-schedule").nth(0)).containsText(PINNED_TIME);
    assertThat(page.locator("li.place-item .stop-schedule").nth(1)).not().containsText(START_TIME);
  }

  /** Wizard completo: paso 1 con la ventana del día, paso 2 con tres paradas distintas. */
  private void createPlanWithThreeStops() {
    plans.navigateToCreatePlanModal();
    plans.typeName(planName);
    plans.typeDescription("Tres paradas para el reorden por manija");
    // Fecha dinámica: el #plan-date del wizard lleva min=${today} y un literal duro vence.
    plans.typeEventDate(isoDateIn(30));
    plans.typeStartTime(START_TIME);
    plans.selectTransportMode(TRANSPORT_MODE);
    plans.clickNextStep();
    for (int index = 0; index < STOP_COUNT; index++) {
      plans.addDraftPlace(index);
    }
    plans.clickNextStep();
    plans.clickCreate();
    plans.waitForDetailPath();
  }

  /**
   * [REO] Arranca el drag de la manija con eventos de puntero sintéticos y lo deja a mitad de
   * camino (down + move) para que las aserciones de Java vean el reacomodo en vivo antes del
   * drop. El move apunta a la mitad superior de la fila {@code slotRow}: la primera fila cuyo
   * punto medio queda debajo del puntero es esa fila, así que la ranura de inserción es
   * {@code slotRow}. La manija agarrada queda en {@code window.__reoHandle} para que el drop
   * dispare el up sobre el mismo elemento, aunque el reacomodo la haya movido de sitio.
   */
  private void startHandleDrag(int handleIndex, int slotRow, String pointerType) {
    page.evaluate(
      """
      ([handleIndex, slotRow, pointerType]) => {
        const handle = document.querySelectorAll('.drag-handle')[handleIndex];
        const rows = document.querySelectorAll('.place-item');
        const from = handle.getBoundingClientRect();
        const to = rows[slotRow].getBoundingClientRect();
        const y = to.top + to.height / 2 - to.height / 4;
        const fire = (type, clientY) => handle.dispatchEvent(new PointerEvent(type, {
          bubbles: true,
          cancelable: true,
          pointerId: 1,
          pointerType,
          button: 0,
          buttons: type === 'pointerup' ? 0 : 1,
          clientX: from.left,
          clientY
        }));
        window.__reoHandle = handle;
        fire('pointerdown', from.top + from.height / 2);
        fire('pointermove', y);
      }
      """,
      List.of(handleIndex, slotRow, pointerType)
    );
  }

  /** [REO] Suelta la manija agarrada por {@link #startHandleDrag} y espera el POST /reorder. */
  private Response dropHandle() {
    return page.waitForResponse(
      "**/reorder",
      () ->
        page.evaluate(
          """
          () => {
            const handle = window.__reoHandle;
            const rect = handle.getBoundingClientRect();
            handle.dispatchEvent(new PointerEvent('pointerup', {
              bubbles: true,
              cancelable: true,
              pointerId: 1,
              pointerType: 'mouse',
              button: 0,
              buttons: 0,
              clientX: rect.left,
              clientY: rect.top + rect.height / 2
            }));
            window.__reoHandle = null;
          }
          """
        )
    );
  }

  /** El scroll vertical de la página, para comparar antes y durante el drag táctil (E-03). */
  private long scrollY() {
    return ((Number) page.evaluate("() => window.scrollY")).longValue();
  }

  /**
   * Contexto táctil (390x844, isMobile y hasTouch) que reemplaza al default: el drag de la
   * manija de E-03 se ejercita como el de un dedo. El route de fuentes y el tracing repiten lo
   * que {@code newContext()} hace para el contexto común.
   */
  private void openTouchContext() {
    context.close();
    context =
      browser.newContext(
        new Browser.NewContextOptions()
          .setViewportSize(390, 844)
          .setIsMobile(true)
          .setHasTouch(true)
      );
    // Hermetic suite: same fonts route as the shared context, so the head scripts never wait
    // on a slow font CSS.
    context.route(
      "https://fonts.googleapis.com/**",
      route -> route.fulfill(new Route.FulfillOptions().setContentType("text/css").setBody(""))
    );
    context
      .tracing()
      .start(
        new com.microsoft.playwright.Tracing.StartOptions().setScreenshots(true).setSnapshots(true)
      );
    page = context.newPage();
  }

  /**
   * [REO] E-04: fijar la hora de una parada después de creado el plan. La vista del detalle
   * dibuja el horario read-only, así que el cambio va por PUT al endpoint del itinerario con el
   * token CSRF que la página lleva, tal como lo hace la propia app; el resto de los campos viaja
   * como está para que el PUT no los pise con null. Un PUT con hora deja la parada pinned (el
   * servidor re-deriva el resto de la cadena desde ahí).
   */
  private void pinVisitTime(int entryIndex, String time) {
    Object pinned = page.evaluate(
      """
      async ([entryIndex, time]) => {
        const planId = location.pathname.split('/').pop();
        const url = '/api/plans/' + planId + '/places';
        const entries = await (await fetch(url)).json();
        const headerMeta = document.querySelector('meta[name="_csrf_header"]');
        const tokenMeta = document.querySelector('meta[name="_csrf"]');
        if (!headerMeta || !tokenMeta) {
          throw new Error('the plan detail page must carry the _csrf metas');
        }
        const entry = entries[entryIndex];
        const response = await fetch(url + '/' + entry.id, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            [headerMeta.content]: tokenMeta.content
          },
          body: JSON.stringify({
            durationMinutes: entry.durationMinutes,
            transportModeToNext: entry.transportModeToNext || null,
            travelMinutesToNext: entry.travelMinutesToNext,
            visitDate: entry.visitDate,
            visitTime: time
          })
        });
        return response.ok;
      }
      """,
      List.of(entryIndex, time)
    );
    assertEquals(
      Boolean.TRUE,
      pinned,
      "el endpoint del itinerario tiene que aceptar el pin de hora"
    );
  }
}
