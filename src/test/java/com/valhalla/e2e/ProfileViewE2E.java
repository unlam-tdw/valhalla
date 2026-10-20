package com.valhalla.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import com.valhalla.e2e.views.WebPage;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * [PROF] Browser coverage del perfil (docs/specs/11-PROF.md, E-01..E-04): el menú del navbar abre
 * el modal, "Ver perfil" llega a la URL con id, el editor persiste visible con el aviso, la vista
 * pública funciona con y sin sesión, y las entradas "Pronto" no navegan a ningún lado.
 *
 * <p>El admin seed ("Admin Test", inicial A) es la única cuenta garantizada: ResetDatabase corre
 * en cada test y la seed se recarga.
 */
public class ProfileViewE2E extends E2eBase {

  @BeforeEach
  void signInAsSeedAdmin() {
    signInAsAdmin();
  }

  // --- E-01: el avatar abre el menú y "Ver perfil" llega al perfil con id en la URL ---

  @Test
  void openingTheMenuAndClickingViewProfileLandsOnTheOwnProfileEditor() {
    WebPage landing = new WebPage(page);
    page.locator("#navbar-avatar").click();
    page.waitForSelector("#navbar-menu:not([hidden])");

    page.locator("#menu-view-profile").click();

    // La URL canónica siempre lleva el id (AC-12): /profile es solo el atajo.
    page.waitForURL(Pattern.compile(".*/profile/\\d+.*"));
    page.waitForSelector("main h1");
    assertTrue(page.textContent("main h1").contains("Mi perfil"));
  }

  // --- E-03: las entradas "Pronto" están deshabilitadas y NO navegan ---

  @Test
  void soonEntriesAreDisabledAndDoNotNavigate() {
    page.locator("#navbar-avatar").click();
    page.waitForSelector("#navbar-menu:not([hidden])");

    // AC-18: "Pronto" no es <a> ni <button> navegable: un span aria-disabled. El E2E no lo
    // clica (Playwright se niega sobre elementos deshabilitados y ahi esta el punto): fija en
    // el DOM que la entrada no linkea a nada y que las dos "Pronto" siguen inertes.
    var disabledEntries = page.locator("#navbar-menu [role='menuitem'][aria-disabled='true']");
    assertTrue(disabledEntries.count() >= 2);
    assertEquals(
      0,
      page.locator("#navbar-menu a", new Page.LocatorOptions().setHasText("Guardados")).count()
    );

    // El menú sigue abierto y la URL intacta: nada navegó.
    assertTrue(page.locator("#navbar-menu:not([hidden])").count() == 1);
    assertFalse(page.url().contains("/profile"));
  }

  // --- E-02: la vista pública funciona con sesión ajena y con anonimo (link compartible) ---

  @Test
  void theSharedProfileLinkWorksLoggedOut() {
    String profileUrl = currentOwnProfileUrl();

    // Contexto nuevo = sin sesión: el mismo /profile/{id} sigue mostrando el escaparate.
    newContext();
    page.navigate(profileUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.COMMIT));
    page.waitForSelector("main h1");

    assertTrue(page.textContent("main h1").contains("Admin Test"));
    assertTrue(page.textContent("main").contains("seguidores"));
    assertFalse(page.textContent("main").contains("Guardar perfil"));
  }

  // --- E-04: la edición persiste visible con el aviso de éxito ---

  @Test
  void profileEditRoundsTripWithTheSuccessNotice() {
    page.navigate(currentOwnProfileUrl());
    page.waitForSelector("#firstName");

    page.fill("#firstName", "Admin");
    page.fill("#lastName", "Seed");
    page.fill("#avatarUrl", "/images/avatars/avatar-1.svg");
    page.locator("main button[type='submit']").first().click();

    page.waitForSelector("p[role='status']");
    assertEquals("Perfil actualizado", page.textContent("p[role='status']").trim());
    assertEquals("/images/avatars/avatar-1.svg", page.inputValue("#avatarUrl"));
  }

  /** URL del perfil propio resolviendo el atajo /profile que redirige a /profile/{id}. */
  private String currentOwnProfileUrl() {
    WebPage nav = new WebPage(page);
    page.navigate(nav.baseUrl() + "/profile");
    page.waitForURL(Pattern.compile(".*/profile/\\d+.*"));
    return page.url();
  }
}
