package com.valhalla.e2e;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.valhalla.e2e.views.PlansPage;
import com.valhalla.e2e.views.UserFormPage;
import java.net.URI;
import org.junit.jupiter.api.Test;

/**
 * [CAL] Happy path completo de las calificaciones (16-CAL.md, E-01) contra el Jetty real:
 *
 * <ol>
 *   <li>Un plan publico calificado muestra el agregado a un anonimo, y su widget nunca existe
 *       (AC-05): el CTA de calificar es el link al login con la vuelta a la vista.
 *   <li>Tras loguearse, el visitante vota 5 estrellas: el widget marca "Tu calificacion" (AC-03) y
 *       el promedio se refleja en la capa de relleno (AC-08).
 *   <li>Recalificar reemplaza (AC-04): la cantidad de votos no crece.
 * </ol>
 *
 * <p>E2eBase limpia la base antes de cada test. El arrange crea el plan publico como admin por UI
 * (mismo fixture que PublicPlanViewE2E, via createPublicPlanViaExplore) y UNE usuario normal que
 * sera la sesion de la fase de voto; la calificacion semilla la deja la propia sesion admin.
 */
public class RatingE2E extends E2eBase {

  private static final String PLAN_NAME = "Plan calificable E2E";
  private static final String PLAN_DESCRIPTION = "Un plan para calificar por link";
  private static final String PLAN_DATE = "2026-12-31";
  private static final String VOTER_EMAIL = "votante@unlam.edu.ar";

  @Test
  void T_CAL_021_elAnonimoVeElPromedioYElLogueadoVotaYRecalifica() {
    // --- arrange: plan publico por UI (admin) + usuario normal para la fase de voto.
    // El fixture devuelve la URL publica que capturo cuando el detalle estaba abierto: la fase
    // 2 necesita esa URL y para entonces la sesion ya no esta en el detalle del plan.
    Fixture fixture = givenPublicPlanAndUser();

    // --- fase 1: la sesion admin califica 4 en la vista publica (el admin es usuario igual)
    rateFromThisPage(4);
    assertThat(page.locator("body").innerText(), containsString("Tu calificación: 4"));
    assertThat(page.locator("body").innerText(), containsString("1 calificación"));
    // el indicador pinta el promedio en proporcion: 4/5 = 80% de la capa amarilla (AC-08)
    assertThat(fillPercent(), is(80.0));

    // --- fase 2: un anonimo ve el agregado pero nunca el widget de voto (AC-05)
    newContext();
    page.navigate(fixture.planPublicUrl());
    assertThat(page.locator("body").innerText(), containsString("1 calificación"));
    assertThat(page.locator("button:has-text('Calificar')").count(), is(0));

    // el CTA de calificar del anonimo lleva al login conservando la vuelta al plan
    page.locator("a:has-text('Iniciá sesión para calificar')").click();
    page.waitForSelector("input[type='email']");
    assertThat(page.url(), containsString("/auth/login?redirect="));

    // --- fase 3: logueado, vota 5 estrellas; el agregado pasa a 4.5 con 2 votos
    page.fill("#email", VOTER_EMAIL);
    page.fill("#password", fixture.voterPassword());
    page.click("#btn-login");
    // el redirect devolvio el flujo a la vista publica del plan (mismo patron que [PVP])
    page.waitForSelector("button:has-text('Calificar')");
    rateFromThisPage(5);
    assertThat(page.locator("body").innerText(), containsString("Tu calificación: 5"));
    assertThat(page.locator("body").innerText(), containsString("2 calificaciones"));

    // --- fase 4: recalificar reemplaza la propia (AC-04): misma cantidad, otro promedio
    rateFromThisPage(4);
    assertThat(page.locator("body").innerText(), containsString("Tu calificación: 4"));
    assertThat(page.locator("body").innerText(), containsString("2 calificaciones"));
    // 4 (admin) + 4 (votante) = 4.0 → el relleno vuelve al 80%
    assertThat(fillPercent(), is(80.0));
  }

  /** Selecciona la estrella n y envia el form de calificar. */
  private void rateFromThisPage(int stars) {
    page.locator(".rating-star-option").nth(stars - 1).click();
    page.locator("button:has-text('Calificar')").click();
    // El POST redirige a la misma vista: el widget re-renderizado confirma el voto (AC-03).
    Locator ownRating = page.locator("text=Tu calificación:");
    ownRating.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
  }

  /** El porcentaje de la capa amarilla parseado del inline style (AC-08). */
  private double fillPercent() {
    String style = page.locator("span.rating-stars-fill").getAttribute("style");
    int at = style.indexOf("width:");
    String value = style.substring(at + "width:".length()).replace("%", "").trim();
    return Double.parseDouble(value);
  }

  /**
   * Crea el plan publico (admin), crea el votante por el ABM y deja la sesion admin abierta en la
   * vista publica, lista para la fase 1. Devuelve la URL publica (capturada mientras el detalle
   * estaba abierto: despues la sesion navega por el ABM y el landing) y la contrasena generada del
   * votante.
   */
  private Fixture givenPublicPlanAndUser() {
    signInAsAdmin();
    PlansPage plans = new PlansPage(page);
    plans.createPublicPlanViaExplore(PLAN_NAME, PLAN_DESCRIPTION, PLAN_DATE);
    plans.waitForDetailPath();
    String planPublicUrl = publicPlanUrl();

    // El ABM se navega con la sesion admin ya abierta: /admin/login nos rebotaria al landing.
    page.navigate(plans.baseUrl() + "/admin/users/new");
    String password = givenANormalUser(VOTER_EMAIL);

    // Contexto y sesion limpios: la fase 1 califica con el admin desde la vista publica.
    newContext();
    signInAsAdmin();
    page.navigate(planPublicUrl);
    return new Fixture(planPublicUrl, password);
  }

  /** Lo que la fase de voto necesita del arrange: donde esta el plan y como entra el votante. */
  private record Fixture(String planPublicUrl, String voterPassword) {}

  /** El id del plan sale de la URL de detalle abierta, no de la vista publica. */
  private String publicPlanUrl() {
    String planId = URI.create(page.url()).getPath().replaceAll(".*/", "");
    return (
      "http://localhost:" + Integer.getInteger("jetty.port", 8080) + "/plans/" + planId + "/public"
    );
  }

  /**
   * Crea el votante por el ABM con la sesion admin abierta (igual que PublicPlanViewE2E) y devuelve
   * la contrasena que el backend asigno, que aparece en la lista.
   */
  private String givenANormalUser(String email) {
    UserFormPage form = new UserFormPage(page);
    form.typeFirstName("Votante");
    form.typeLastName("E2e");
    form.typeEmail(email);
    form.selectRole("USER");
    form.clickCreate();
    form.waitForPath("/admin/users");
    return page.locator("code").textContent();
  }
}
