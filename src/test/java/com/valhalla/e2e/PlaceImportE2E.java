package com.valhalla.e2e;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

import com.microsoft.playwright.Route;
import com.valhalla.e2e.views.PlaceFormPage;
import com.valhalla.e2e.views.PlaceImportPage;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * E-07..E-10: el importador visto desde el navegador. Los dos endpoints se stubbean con {@code
 * page.route}, y el test que prueba la lectura desde la base siembra la fila por JDBC: el navegador
 * nunca sale a internet (AC-10). El stub tambien es el unico modo de ejercitar el error sin
 * depender de que Overpass este saturado justo en la corrida, y de contar cuantas consultas se
 * hicieron.
 *
 * <p>La distincion que estos tests cuidan es la del cambio: entrar, mover el mapa y recargar leen de
 * la base ({@code /pois}); solo el boton consulta Overpass ({@code /refresh}).
 */
public class PlaceImportE2E extends E2eBase {

  private static final String PARRILLA = "Parrilla del Puerto";

  /** Id con el que el backend marca que un POI ya es una fila de places. */
  private static final String SAVED_PLACE_ID = "0192f0c4-7b3a-7c2e-9f10-2a1b3c4d5e6f";

  /**
   * Tres POIs, cada uno con su categoria: uno completo, uno sin datos opcionales y uno sin
   * coordenadas (no se dibuja). Ninguno es todavia un lugar guardado.
   */
  private static final String FIXTURE_POIS = fixturePois("null");

  /** El mismo fixture, con la parrilla ya convertida en lugar: el importador tiene que marcarlo. */
  private static final String FIXTURE_POIS_SAVED = fixturePois("\"" + SAVED_PLACE_ID + "\"");

  /** Una zona ya cargada: fecha vieja a proposito, para que "hace X" siempre sea pasado. */
  private static final String ENVELOPE_LOADED = envelope(FIXTURE_POIS, "\"2020-01-01T10:00:00Z\"");

  /** La misma zona, con un punto que ya existe en el catalogo. */
  private static final String ENVELOPE_SAVED = envelope(
    FIXTURE_POIS_SAVED,
    "\"2020-01-01T10:00:00Z\""
  );

  /** Zona nunca cargada: hay que distinguirla de una zona cargada sin resultados. */
  private static final String ENVELOPE_EMPTY = envelope("[]", "null");

  @Test
  void shouldReadTheStoredPoisOnEntryWithoutQueryingOverpass() {
    signInAsAdmin();
    List<String> refreshes = new ArrayList<>();
    stubStored(ENVELOPE_LOADED);
    stubRefresh(ENVELOPE_LOADED, refreshes);

    PlaceImportPage view = new PlaceImportPage(page);
    view.navigateToImport();
    view.waitForPois(3);

    assertThat("al entrar no se consulta Overpass", refreshes, hasSize(0));
    assertThat("con resultados aparece el buscador", view.hasSearchInput(), is(true));
    view.waitForFreshnessNotice();
    assertThat(view.freshnessText(), containsString("hace"));
    assertThat(
      "la zona ya cargada se puede actualizar",
      view.buttonLabel(),
      is("Actualizar puntos de esta zona")
    );
  }

  @Test
  void shouldQueryOnlyWhenTheLoadButtonIsPressed() {
    signInAsAdmin();
    List<String> refreshes = new ArrayList<>();
    stubStored(ENVELOPE_EMPTY);
    stubRefresh(ENVELOPE_LOADED, refreshes);

    PlaceImportPage view = new PlaceImportPage(page);
    view.navigateToImport();
    view.waitForNeverLoadedNotice();

    assertThat("al entrar no se consulta nada", refreshes, hasSize(0));
    assertThat("sin puntos no hay buscador", view.hasSearchInput(), is(false));
    assertThat(
      "la zona sin cargar se anuncia",
      view.buttonLabel(),
      is("Cargar puntos de esta zona")
    );

    view.clickLoad();
    view.waitForPois(3);

    assertThat("una sola consulta por click", refreshes, hasSize(1));
    assertThat("con resultados aparece el buscador", view.hasSearchInput(), is(true));
    assertThat(
      "despues de cargar, el boton actualiza",
      view.buttonLabel(),
      is("Actualizar puntos de esta zona")
    );
  }

  @Test
  void shouldListTheStubbedPoisAndOpenThePrefilledForm() {
    signInAsAdmin();
    stubStored(ENVELOPE_EMPTY);
    stubRefresh(ENVELOPE_LOADED);

    PlaceImportPage view = new PlaceImportPage(page);
    view.navigateToImport();
    view.waitForLoadButton();
    view.clickLoad();
    view.waitForPois(3);

    assertThat(view.statusText(), is("3 punto(s) encontrados"));
    assertThat("el POI sin coordenadas no tiene marker", view.markerCount(), is(2));

    view.selectPoi(PARRILLA);
    view.waitForPreview();

    assertThat(
      "la fila abre la vista previa, no el alta",
      page.url(),
      containsString("/admin/places/import")
    );
    assertThat("el panel es el del punto elegido", view.previewName(), is(PARRILLA));
    assertThat(view.previewAddress(), is("Av. Corrientes 1234"));
    assertThat(view.previewHours(), is("Mo-Su 12:00-23:00"));
    assertThat(view.previewPhone(), is("+54 11 4444-5555"));
    assertThat(view.previewWebsite(), is("https://parrilla.example.com"));
    assertThat(view.previewCoordinates(), is("-34.6037, -58.3816"));
    // El color sale del token de RESTAURANT, el mismo que pinta el marker de /explore.
    assertThat(
      "el punto de categoria pinta con su token",
      view.previewCategoryColor(),
      is("rgb(231, 76, 60)")
    );
    assertThat("el punto todavia no es un lugar", view.hasCreateAction(), is(true));

    view.clickCreateFromPreview();
    view.waitForPath("/admin/places/new");
    page.waitForFunction("() => document.querySelector('#name')?.value === '" + PARRILLA + "'");

    PlaceFormPage form = new PlaceFormPage(page);
    assertThat(form.getName(), is(PARRILLA));
    assertThat("la categoria sugiere la del punto", form.getCategory(), is("RESTAURANT"));
    assertThat(form.getAddress(), is("Av. Corrientes 1234"));
    assertThat(form.getLatitude(), is("-34.6037"));
    assertThat(form.getLongitude(), is("-58.3816"));
    assertThat(form.getOpeningHours(), is("Mo-Su 12:00-23:00"));
    assertThat(form.getPhone(), is("+54 11 4444-5555"));
    assertThat(form.getWebsite(), is("https://parrilla.example.com"));
    assertThat("la identidad del POI viaja al alta", form.getExternalId(), is("osm:way/1"));
  }

  /** El marker abre el mismo panel que la fila: el mapa tampoco navega al alta. */
  @Test
  void shouldPreviewAMarkerWithoutLeavingThePage() {
    signInAsAdmin();
    stubStored(ENVELOPE_EMPTY);
    stubRefresh(ENVELOPE_LOADED);

    PlaceImportPage view = new PlaceImportPage(page);
    view.navigateToImport();
    view.waitForLoadButton();
    view.clickLoad();
    view.waitForPois(3);
    assertThat(view.markerCount(), is(2));

    view.clickFirstMarker();
    view.waitForPreview();

    assertThat("el marker abre la vista previa", view.previewName(), is(PARRILLA));
    assertThat(
      "el click no navega: la pagina sigue en el importador",
      page.url(),
      containsString("/admin/places/import")
    );

    view.closePreview();
    assertThat("el panel se puede cerrar sin dejar nada abierto", view.hasPreview(), is(false));
  }

  /**
   * El punto que ya es un lugar del catalogo se marca en la fila y en el panel, y no ofrece un
   * alta que duplicaria la fila.
   */
  @Test
  void shouldMarkThePoiThatIsAlreadyAPlace() {
    signInAsAdmin();
    stubStored(ENVELOPE_EMPTY);
    stubRefresh(ENVELOPE_SAVED);

    PlaceImportPage view = new PlaceImportPage(page);
    view.navigateToImport();
    view.waitForLoadButton();
    view.clickLoad();
    view.waitForPois(3);

    assertThat("solo la parrilla ya es un lugar", view.savedBadgeCount(), is(1));

    view.selectPoi(PARRILLA);
    view.waitForPreview();

    assertThat(view.previewName(), is(PARRILLA));
    assertThat(view.previewText(), containsString("Ya está guardado"));
    assertThat(
      "el link lleva al lugar que ya existe",
      view.savedPlaceHref(),
      endsWith("/admin/places/" + SAVED_PLACE_ID + "/edit")
    );
    assertThat("no se ofrece crear un lugar que ya existe", view.hasCreateAction(), is(false));
  }

  @Test
  void shouldFilterThePoisByName() {
    signInAsAdmin();
    stubStored(ENVELOPE_EMPTY);
    stubRefresh(ENVELOPE_LOADED);

    PlaceImportPage view = new PlaceImportPage(page);
    view.navigateToImport();
    view.waitForLoadButton();
    view.clickLoad();
    view.waitForPois(3);

    view.filterByName("PARRILLA");
    view.waitForPois(1);
    assertThat("el filtro no distingue mayusculas", view.poiCount(), is(1));
    assertThat("el mapa muestra solo el punto filtrado", view.markerCount(), is(1));
    assertThat(view.statusText(), is("1 de 3 punto(s)"));

    view.filterByName("cafe");
    view.waitForPois(1);
    assertThat("el filtro no distingue acentos", view.poiCount(), is(1));
    assertThat("el cafe no tiene coordenadas", view.markerCount(), is(0));

    view.filterByName("zzz");
    view.waitForPois(0);
    assertThat(view.markerCount(), is(0));

    view.filterByName("");
    view.waitForPois(3);
    assertThat("al limpiar el filtro vuelven todos", view.markerCount(), is(2));
  }

  /**
   * Mover el mapa relee la zona visible, pero de la base: el unico camino a Overpass es el boton.
   * Antes cada arrastre era una consulta de 30 s.
   */
  @Test
  void shouldReadTheAreaOnScreenAndOnlyQueryOverpassOnDemand() {
    signInAsAdmin();
    List<String> reads = new ArrayList<>();
    List<String> refreshes = new ArrayList<>();
    stubStored(ENVELOPE_LOADED, reads);
    stubRefresh(ENVELOPE_LOADED, refreshes);

    PlaceImportPage view = new PlaceImportPage(page);
    view.navigateToImport();
    view.waitForPois(3);
    page.waitForCondition(() -> !reads.isEmpty());

    String first = reads.getLast();
    assertThat("la lectura lleva el area visible", hasAllBorders(first), is(true));
    double citySpan = latitudeSpan(first);

    int before = reads.size();
    view.zoomTo(17);
    page.waitForCondition(() -> reads.size() > before);
    assertThat(
      "acercarse relee una zona mas chica",
      latitudeSpan(reads.getLast()),
      lessThan(citySpan)
    );

    int afterZoom = reads.size();
    view.dragMap(600);
    view.dragMap(-600);
    page.waitForCondition(() -> reads.size() > afterZoom);

    assertThat("la lista se mantiene al mover el mapa", view.poiCount(), is(3));
    assertThat("mover el mapa no consulta Overpass", refreshes, hasSize(0));

    view.clickLoad();
    page.waitForCondition(() -> refreshes.size() == 1);
  }

  @Test
  void shouldShowTheErrorMessageWhenTheProxyFails() {
    signInAsAdmin();
    stubStored(ENVELOPE_EMPTY);
    stubRefresh(502, "{\"error\":\"Overpass is not available right now\"}");

    PlaceImportPage view = new PlaceImportPage(page);
    view.navigateToImport();
    view.waitForLoadButton();
    view.clickLoad();
    view.waitForError();

    assertThat(view.hasError(), is(true));
    assertThat(
      view.errorText(),
      containsString("No se pudieron traer los puntos de OpenStreetMap")
    );
    assertThat("no queda ningun POI en la lista", view.poiCount(), is(0));
  }

  /**
   * Lo guardado se pinta al entrar y vuelve a aparecer al recargar la pagina, sin una sola
   * consulta: es el punto del cambio. La fila se siembra por JDBC porque llegar a Overpass desde un
   * test esta prohibido (AC-10).
   */
  @Test
  void shouldRenderTheStoredPoisAgainAfterAReload() {
    signInAsAdmin();
    ResetDatabase.seedPoi(PARRILLA, -34.6037, -58.3816, "amenity", "restaurant");
    List<String> requested = new ArrayList<>();
    page.onRequest(request -> requested.add(request.url()));

    PlaceImportPage view = new PlaceImportPage(page);
    view.navigateToImport();
    view.waitForPois(1);

    assertThat(view.poiCount(), is(1));
    assertThat("el punto guardado se dibuja", view.markerCount(), is(1));
    assertThat(
      "la lista vino de la base",
      requested.stream().anyMatch(url -> url.contains("/admin/places/import/pois")),
      is(true)
    );

    view.navigateToImport();
    view.waitForPois(1);

    assertThat("recargar pinta lo mismo", view.poiCount(), is(1));
    assertThat("el punto sigue en el mapa", view.markerCount(), is(1));
    assertThat(
      "recargar no consulta Overpass",
      requested.stream().anyMatch(url -> url.contains("/admin/places/import/refresh")),
      is(false)
    );
  }

  /** El sobre que devuelven los dos endpoints: `pois` y `fetchedAt` (null = zona nunca cargada). */
  private static String envelope(String pois, String fetchedAt) {
    return "{\"pois\":" + pois + ",\"fetchedAt\":" + fetchedAt + "}";
  }

  /**
   * Los tres POIs del stub, con la forma que manda el backend: categoria del enum e identidad del
   * POI en {@code externalId}, sin los viejos {@code osmType}/{@code osmId}.
   *
   * @param savedPlaceId id del lugar que ya representa a la parrilla, o {@code null} en JSON
   */
  private static String fixturePois(String savedPlaceId) {
    return """
    [
      {
        "name": "Parrilla del Puerto",
        "address": "Av. Corrientes 1234",
        "latitude": -34.6037,
        "longitude": -58.3816,
        "openingHours": "Mo-Su 12:00-23:00",
        "phone": "+54 11 4444-5555",
        "website": "https://parrilla.example.com",
        "category": "RESTAURANT",
        "externalId": "osm:way/1",
        "savedPlaceId": %s
      },
      {
        "name": "Bar Sin Datos",
        "address": null,
        "latitude": -34.61,
        "longitude": -58.37,
        "openingHours": null,
        "phone": null,
        "website": null,
        "category": "BAR",
        "externalId": "osm:node/2",
        "savedPlaceId": null
      },
      {
        "name": "Café Sin Coordenadas",
        "address": "Av. Rivadavia 100",
        "latitude": null,
        "longitude": null,
        "openingHours": null,
        "phone": null,
        "website": null,
        "category": "CAFE",
        "externalId": "osm:node/3",
        "savedPlaceId": null
      }
    ]
    """.formatted(savedPlaceId);
  }

  private boolean hasAllBorders(String url) {
    return Stream
      .of("south", "west", "north", "east")
      .allMatch(name -> urlParam(url, name) != null && !urlParam(url, name).isBlank());
  }

  private double latitudeSpan(String url) {
    return Math.abs(
      Double.parseDouble(urlParam(url, "north")) - Double.parseDouble(urlParam(url, "south"))
    );
  }

  /** Valor crudo de un query param: los numeros no necesitan decodificacion. */
  private String urlParam(String url, String name) {
    for (String pair : url.substring(url.indexOf('?') + 1).split("&")) {
      int separator = pair.indexOf('=');
      if (separator > 0 && pair.substring(0, separator).equals(name)) {
        return pair.substring(separator + 1);
      }
    }
    return null;
  }

  /** Stub de la lectura: la vista pinta lo que le contesten, sin pasar por la base. */
  private void stubStored(String body) {
    stubStored(body, new ArrayList<>());
  }

  /** Igual que {@link #stubStored}, guardando cada URL pedida. */
  private void stubStored(String body, List<String> requested) {
    stub("**/admin/places/import/pois**", 200, body, requested);
  }

  private void stubRefresh(String body) {
    stubRefresh(200, body, new ArrayList<>());
  }

  private void stubRefresh(String body, List<String> requested) {
    stubRefresh(200, body, requested);
  }

  /** Stub de la ingesta: es el unico endpoint que en produccion habla con Overpass. */
  private void stubRefresh(int status, String body) {
    stubRefresh(status, body, new ArrayList<>());
  }

  private void stubRefresh(int status, String body, List<String> requested) {
    stub("**/admin/places/import/refresh**", status, body, requested);
  }

  private void stub(String pattern, int status, String body, List<String> requested) {
    page.route(
      pattern,
      route -> {
        requested.add(route.request().url());
        route.fulfill(
          new Route.FulfillOptions()
            .setStatus(status)
            .setContentType("application/json; charset=utf-8")
            .setBody(body)
        );
      }
    );
  }
}
