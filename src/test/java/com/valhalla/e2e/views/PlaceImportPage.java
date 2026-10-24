package com.valhalla.e2e.views;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Mouse;
import com.microsoft.playwright.Page;

/**
 * Page object for {@code /admin/places/import}: la lista de POIs, los markers del mapa y el mensaje
 * de error. La lista es la via de texto para elegir un punto, igual que el click en el marker.
 */
public class PlaceImportPage extends WebPage {

  public PlaceImportPage(Page page) {
    super(page);
  }

  public void navigateToImport() {
    page.navigate(baseUrl() + "/admin/places/import");
  }

  /** Espera a que la lista tenga exactamente {@code count} POIs: el fetch resuelve despues del load. */
  public void waitForPois(int count) {
    page.waitForFunction(
      "(n) => document.querySelectorAll('#place-import-app > aside ul li').length === n",
      count
    );
  }

  /** El boton se habilita cuando el mapa ya tiene un area (y esa area toca CABA). */
  public void waitForLoadButton() {
    page.waitForSelector("#load-pois:not([disabled])");
  }

  public void clickLoad() {
    this.clickElement("#load-pois");
  }

  /** El buscador existe recien cuando hay una lista que filtrar. */
  public boolean hasSearchInput() {
    return page.locator("#poi-search").count() > 0;
  }

  public String hintText() {
    return this.getElementText("#zone-never-loaded").trim();
  }

  /** Lo que el boton va a hacer: la primera vez trae, despues actualiza la zona. */
  public String buttonLabel() {
    return this.getElementText("#load-pois").trim();
  }

  /** Aviso de que la zona (y el tipo) que se mira nunca se cargo. */
  public void waitForNeverLoadedNotice() {
    page.waitForSelector("#zone-never-loaded");
  }

  public boolean hasNeverLoadedNotice() {
    return page.locator("#zone-never-loaded").count() > 0;
  }

  /** Frescura de lo guardado: solo aparece cuando la zona ya se cargo alguna vez. */
  public void waitForFreshnessNotice() {
    page.waitForSelector("#zone-freshness");
  }

  public boolean hasFreshnessNotice() {
    return page.locator("#zone-freshness").count() > 0;
  }

  public String freshnessText() {
    return this.getElementText("#zone-freshness").trim();
  }

  public void waitForError() {
    page.waitForSelector("#place-import-app > aside p[role='alert']");
  }

  public int poiCount() {
    return page.locator("#place-import-app > aside ul li").count();
  }

  public String statusText() {
    return this.getElementText("#place-import-app > aside p[aria-live='polite']").trim();
  }

  /** Un POI sin coordenadas no se dibuja, asi que el total puede ser menor que el de la lista. */
  public int markerCount() {
    return page.locator("#map path.leaflet-interactive").count();
  }

  public boolean hasError() {
    return page.locator("#place-import-app > aside p[role='alert']").count() > 0;
  }

  public String errorText() {
    return this.getElementText("#place-import-app > aside p[role='alert']").trim();
  }

  public void selectPoi(String name) {
    page
      .locator("#place-import-app > aside ul li a")
      .filter(new Locator.FilterOptions().setHasText(name))
      .click();
  }

  /** Filtro local por nombre: escribe en el input y Vue re-renderiza la lista. */
  public void filterByName(String term) {
    this.typeIntoElement("#poi-search", term);
  }

  /**
   * Arrastra el mapa {@code dx} px desde el centro: es el gesto real de panear. Espera a que Leaflet
   * cierre el movimiento (inclusive la inercia) antes de devolver, asi dos arrastres seguidos no se
   * pisan.
   */
  public void dragMap(int dx) {
    int before = moveCount();
    var map = page.locator("#map").boundingBox();
    double y = map.y + map.height / 2;
    double from = map.x + map.width / 2;
    page.mouse().move(from, y);
    page.mouse().down();
    page.mouse().move(from + dx, y, new Mouse.MoveOptions().setSteps(10));
    page.mouse().up();
    page.waitForFunction("(n) => Number(document.querySelector('#map').dataset.moves) > n", before);
  }

  private int moveCount() {
    return Integer.parseInt(page.locator("#map").getAttribute("data-moves"));
  }

  /**
   * Acerca el mapa hasta {@code level} con el control de zoom de Leaflet. Espera a que el zoom
   * nuevo se refleje en {@code #map[data-zoom]} antes del click siguiente: con animacion en curso
   * Leaflet ignora clicks y el test se volveria flaky.
   */
  public void zoomTo(int level) {
    while (true) {
      int current = Integer.parseInt(page.locator("#map").getAttribute("data-zoom"));
      if (current >= level) {
        return;
      }
      this.clickElement("a.leaflet-control-zoom-in");
      page.waitForFunction(
        "(zoom) => document.querySelector('#map').dataset.zoom === String(zoom)",
        current + 1
      );
    }
  }
}
