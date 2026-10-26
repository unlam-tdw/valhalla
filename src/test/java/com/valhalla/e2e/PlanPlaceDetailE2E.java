package com.valhalla.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.SelectOption;
import com.valhalla.e2e.views.PlansPage;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Browser coverage of a plan's itinerary: a place added together with its visit details has to be
 * displayed, keep those details across reloads, and stay reorderable and removable afterwards.
 *
 * <p>{@link E2eBase} wipes the database before the test, so the seeded admin owns the only plan
 * around and every assertion below is about the plan this test creates. Screenshots land in
 * {@code target/itinerary-*.png}; failures keep their own screenshot and trace in {@code
 * target/e2e-artifacts/}.
 */
public class PlanPlaceDetailE2E extends E2eBase {

  private static final String EVENT_DATE = "2026-12-01";
  private static final String PLAN_TIME = "20:00";
  private static final String VISIT_TIME = "18:30";
  private static final String UPDATED_VISIT_TIME = "10:30";

  private PlansPage plans;
  private String planName;

  @BeforeEach
  void signInAndPreparePlanPage() {
    signInAsAdmin();
    this.plans = new PlansPage(page);
    this.planName = "Detalle E2E " + System.nanoTime();
  }

  @Test
  void addsPersistsAndDisplaysVisitDetails() {
    createPlanWithAVisit();

    // Creation: the stop and the visit details typed into the create form are on screen right
    // away, and the plan's own date reached the summary as well. The stop rides on the plan's own
    // date (the create form does not ask for one any more), so its schedule row carries the time
    // only and there is no per-stop date left to show.
    assertEquals(planName, plans.getDetailHeading());
    assertThat(page.locator(".stop-details")).hasCount(1);
    assertEquals(
      VISIT_TIME,
      page.locator(".stop-schedule").first().innerText(),
      "a stop shows its time only; its date is the plan's own"
    );
    assertThat(page.locator("#plan-summary")).containsText(EVENT_DATE);
    assertThat(page.locator("input[name='eventDate']")).hasValue(EVENT_DATE);

    // The create form has no time field, so the plan's own time goes through the edit form.
    assertThat(page.locator("#plan-summary")).containsText("Sin hora");
    page.locator("input[name='eventTime']").fill(PLAN_TIME);
    page.getByText("Guardar cambios", new Page.GetByTextOptions().setExact(true)).click();
    plans.waitForDetailPath();
    assertThat(page.locator("#plan-summary")).containsText(PLAN_TIME);

    // The details dialog fetches the itinerary instead of reading the rendered snapshot.
    Locator dialog = openStopDetails(0);
    assertThat(dialog).containsText(VISIT_TIME);
    assertThat(dialog.locator("dd").last()).hasText("1");
    closeStopDetails();

    // Nothing above is client state: a reload shows the same stop with the same schedule.
    page.reload();
    assertThat(page.locator(".stop-details")).hasCount(1);
    assertThat(page.locator(".stop-schedule").first()).containsText(VISIT_TIME);

    // The detail page has no editor for a visit, so the change goes through the very endpoint the
    // app calls, and both the dialog and the itinerary have to pick the new values up.
    updateVisitDetails(EVENT_DATE, UPDATED_VISIT_TIME);
    dialog = openStopDetails(0);
    assertThat(dialog).containsText(UPDATED_VISIT_TIME);
    closeStopDetails();
    page.reload();
    assertThat(page.locator(".stop-schedule").first()).containsText(UPDATED_VISIT_TIME);

    // A second stop added from the page's own form, this time with no visit details at all.
    addPlaceToPlan();
    assertThat(page.locator(".stop-details")).hasCount(2);
    dialog = openStopDetails(1);
    assertThat(dialog).containsText("Sin fecha");
    assertThat(dialog).containsText("Sin hora");
    assertThat(dialog.locator("dd").last()).hasText("2");
    closeStopDetails();

    // Reordering is drag and drop, and the new order survives a reload. The drop handler swaps
    // the list optimistically and saves afterwards, so the reload has to wait for that save:
    // reloading while the POST is still in flight renders the old order from the database.
    Locator titles = page.locator(".stop-title");
    String firstName = titles.nth(0).innerText();
    String secondName = titles.nth(1).innerText();
    Response reorder = page.waitForResponse(
      "**/reorder",
      () -> page.locator(".place-item").nth(0).dragTo(page.locator(".place-item").nth(1))
    );
    assertEquals(200, reorder.status(), "the reorder has to be saved by the server");
    assertThat(titles.nth(0)).hasText(secondName);
    assertThat(titles.nth(1)).hasText(firstName);
    page.reload();
    assertThat(titles.nth(0)).hasText(secondName);

    screenshotItinerary();

    // Removing both stops empties the itinerary, and it stays empty after a reload.
    page.locator(".stop-remove").nth(0).click();
    assertThat(page.locator(".stop-details")).hasCount(1);
    page.locator(".stop-remove").nth(0).click();
    assertThat(page.locator(".stop-details")).hasCount(0);
    assertThat(page.locator(".itinerary-empty")).isVisible();
    page.reload();
    assertThat(page.locator(".stop-details")).hasCount(0);

    // The plan built above can still be deleted from this same page.
    page.getByText("Eliminar plan", new Page.GetByTextOptions().setExact(true)).click();
    plans.waitForPath("/plans");
    assertEquals(0, plans.getPlanCount(), "the plan this test created has to be gone");
  }

  /**
   * [MAPS] The stop details dialog links out to Google Maps directions: the link is only rendered
   * when the stop has an address, and its href carries that address (URL-encoded), not raw
   * coordinates — the dataset stores address and coordinates out of sync, so the link follows the
   * address the dialog shows. The itinerary list offers the same link below the stop, so a stop can
   * be routed without opening the dialog.
   */
  @Test
  void showsTheMapsDirectionsLinkInTheStopDetails() {
    createPlanWithAVisit();

    Locator dialog = openStopDetails(0);
    Locator mapsLink = dialog.locator("a[href*='maps/dir']");
    assertThat(mapsLink).isVisible();
    // El destino es la direccion del lugar, url-encoded: al menos un caracter no vacio.
    assertThat(mapsLink)
      .hasAttribute("href", Pattern.compile(".*destination=([^&\\s]|%[0-9A-F]{2})+.*"));
    closeStopDetails();

    // El itinerario de abajo tambien ofrece el "Como llegar" del lugar.
    Locator itineraryLink = page.locator(".place-item").first().locator("a.stop-directions");
    assertThat(itineraryLink).isVisible();
    assertThat(itineraryLink).hasText("Cómo llegar");
    assertThat(itineraryLink)
      .hasAttribute("href", Pattern.compile(".*google\\.com/maps/dir/\\?api=1&destination=.+"));
  }

  /**
   * [DIS-B01] /plans/{id} carga Bootstrap en su propio &lt;head&gt; y las utilidades de Tailwind
   * del navbar compartido quedan por debajo. El navbar tiene que lucir igual que en el resto del
   * sitio: avatar redondo con la serif de la marca, fondo de token, gap real de gap-4 y wordmark
   * en text-xl.
   */
  @Test
  void keepsTheSharedNavbarOnThePlanDetailPage() {
    createPlanWithAVisit();

    Map<String, Object> nav = (Map<String, Object>) page.evaluate(
      """
      () => {
        const avatarStyle = getComputedStyle(document.querySelector('#navbar-avatar'));
        return {
          avatarRadius: parseFloat(avatarStyle.borderRadius),
          avatarFont: avatarStyle.fontFamily,
          navBg: getComputedStyle(document.querySelector('nav')).backgroundColor,
          menuGap: getComputedStyle(document.querySelector('nav .md\\\\:flex')).gap,
          brandSize: getComputedStyle(document.querySelector('nav .navbar-brand')).fontSize,
        };
      }
      """
    );

    assertTrue(
      ((Number) nav.get("avatarRadius")).doubleValue() > 1000,
      "el avatar del navbar tiene que seguir redondo (rounded-full), no un rectángulo de 6px"
    );
    assertTrue(
      nav.get("avatarFont").toString().contains("Bree Serif"),
      "el avatar hereda la serif de la marca, no la sans del body de Bootstrap"
    );
    assertEquals("rgb(17, 24, 39)", nav.get("navBg"), "fondo de token del nav, como en el resto");
    assertEquals(
      "16px",
      nav.get("menuGap"),
      "gap-4 es 1rem en Tailwind, no el 1.5rem de Bootstrap"
    );
    assertEquals("20px", nav.get("brandSize"), "wordmark en text-xl, como en el resto del sitio");
  }

  /**
   * [DIS-B02/B03/B04] El modal "Ver más" es una ficha con jerarquía: título + chip de categoría,
   * bloques con icono para dirección, descripciones, fecha/hora y posición; el editor de horario
   * sigue ahí para quien puede editar el plan (en este arrange, el administrador). Del lado del
   * a11y el diálogo declara aria-labelledby, recibe el foco al abrir y se cierra con Escape.
   */
  @Test
  void showsTheStopDetailsAsAStructuredFichaAndClosesWithEscape() {
    createPlanWithAVisit();

    Locator dialog = openStopDetails(0);

    // Título + chip de categoría con la clase cat-{categoría} que pinta el mapa.
    assertThat(dialog.locator("#place-detail-title")).isVisible();
    Locator chip = dialog.locator(".place-chip");
    assertThat(chip).isVisible();
    assertThat(chip).containsText(Pattern.compile("\\S"));
    assertThat(chip).hasAttribute("class", Pattern.compile("cat-\\S+"));

    // Un icono por bloque del dl: dirección, cómo llegar, 2 descripciones, fecha/hora y
    // posición. El lugar del arrange tiene dirección, así que los 6 bloques están.
    assertThat(dialog.locator(".place-facts dt svg")).hasCount(6);

    // Bloques de fecha/hora y descripciones con sus valores, posición al final del dl.
    assertThat(dialog).containsText("Fecha y hora de visita");
    assertThat(dialog).containsText("Descripción del lugar");
    assertThat(dialog).containsText("Descripción de la visita");
    assertThat(dialog).containsText(VISIT_TIME);
    assertThat(dialog.locator(".place-facts dd").last()).hasText("1");

    // [DIS-B03] owner/editor: el admin del arrange sigue viendo el editor de fecha/hora.
    assertThat(dialog.locator(".visit-editor input[type='date']")).isVisible();
    assertThat(dialog.locator(".visit-editor input[type='time']")).isVisible();

    // [DIS-B04] aria-labelledby y foco dentro del diálogo al abrir. El selector se ancla al
    // dialog de este detalle: desde el merge de main hay otro <dialog> (banned-participants)
    // antes en el DOM y document.querySelector('dialog') lo agarra a él.
    assertThat(dialog).hasAttribute("aria-labelledby", "place-detail-title");
    boolean focusInside = Boolean.TRUE.equals(
      page.evaluate(
        "() => document.querySelector('dialog[aria-labelledby=place-detail-title]').contains(document.activeElement)"
      )
    );
    assertTrue(focusInside, "abrir el diálogo tiene que dejar el foco adentro");

    // Escape cierra (comportamiento nativo del <dialog>).
    page.keyboard().press("Escape");
    page.waitForTimeout(150);
    boolean stillOpen = Boolean.TRUE.equals(
      page.evaluate(
        "() => document.querySelector('dialog[aria-labelledby=place-detail-title]').open"
      )
    );
    assertFalse(stillOpen, "Escape tiene que cerrar el diálogo");
  }

  /** The create panel of {@code /explore}, with the visit details the form is able to carry. */
  private void createPlanWithAVisit() {
    plans.navigateToCreatePlanModal();
    plans.typeName(planName);
    plans.typeDescription("Itinerario con horarios de visita");
    plans.typeEventDate(EVENT_DATE);
    plans.clickNextStep();
    plans.addFirstDraftPlace();
    page.locator("#plans-aside ol li input[aria-label='Hora de visita']").fill(VISIT_TIME);
    plans.clickNextStep();
    plans.clickCreate();
    plans.waitForDetailPath();
  }

  /** Detail page form: picks the first place the itinerary does not hold yet and adds it. */
  private void addPlaceToPlan() {
    page.getByLabel("Lugar para agregar al plan").selectOption(new SelectOption().setIndex(1));
    page.getByText("+ Agregar", new Page.GetByTextOptions().setExact(true)).click();
  }

  private Locator openStopDetails(int index) {
    Locator dialog = page.locator("dialog[aria-labelledby='place-detail-title']");
    page.locator(".stop-details").nth(index).click();
    assertThat(dialog).isVisible();
    return dialog;
  }

  private void closeStopDetails() {
    page.locator("dialog[aria-labelledby='place-detail-title'] .btn-close").click();
  }

  /**
   * The only way to change a visit after the plan exists: the detail page renders the visit date
   * and time read-only, so the change goes to {@code PUT /api/plans/{id}/places}/{@code {entryId}}
   * with the CSRF token the page carries, the same way the Vue app itself does.
   */
  private void updateVisitDetails(String isoDate, String time) {
    Object updated = page.evaluate(
      """
      async body => {
        const planId = location.pathname.split('/').pop();
        const url = '/api/plans/' + planId + '/places';
        const entries = await (await fetch(url)).json();
        const headerMeta = document.querySelector('meta[name="_csrf_header"]');
        const tokenMeta = document.querySelector('meta[name="_csrf"]');
        if (!headerMeta || !tokenMeta) {
          throw new Error('the plan detail page must carry the _csrf metas');
        }
        const header = headerMeta.content;
        const token = tokenMeta.content;
        const response = await fetch(url + '/' + entries[0].id, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json', [header]: token },
          body
        });
        return response.ok;
      }
      """,
      String.format("{\"visitDate\":\"%s\",\"visitTime\":\"%s\"}", isoDate, time)
    );
    assertEquals(
      Boolean.TRUE,
      updated,
      "the itinerary endpoint has to accept a new visit date and time"
    );
  }

  private void screenshotItinerary() {
    page.screenshot(
      new Page.ScreenshotOptions()
        .setPath(Path.of("target/itinerary-desktop.png"))
        .setFullPage(true)
    );
    page.setViewportSize(390, 844);
    page.locator(".itinerary-panel").scrollIntoViewIfNeeded();
    page.screenshot(
      new Page.ScreenshotOptions().setPath(Path.of("target/itinerary-mobile.png")).setFullPage(true)
    );
    Object fitsOnPhone = page.evaluate(
      "() => { const p = document.querySelector('.itinerary-panel');" +
      " return p.scrollWidth <= p.clientWidth; }"
    );
    assertEquals(Boolean.TRUE, fitsOnPhone, "the itinerary must not scroll sideways on a phone");
    page.setViewportSize(1280, 720);
  }
}
