package com.valhalla.presentation.place;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class PlaceDetailViewTest {

  private static final Path TEMPLATE_PATH = Path.of(
    "src/main/webapp/WEB-INF/templates/pages/places/list.html"
  );

  private static String html;

  @BeforeAll
  static void readTemplate() throws IOException {
    html = Files.readString(TEMPLATE_PATH);
  }

  @Test
  public void T_APL_FE_001_vistaLugares_noContieneBotonAddToPlan() {
    // El boton "Add to plan" del panel de detalles y su modal se quitaron de la vista:
    // el alta de lugares vive en el wizard de creacion y en el detalle del plan.
    assertThat(html, not(containsString("Add to plan")));
    assertThat(html, not(containsString("Agregar a plan")));

    // Queda el boton de las tarjetas que agrega al borrador mientras el wizard esta abierto.
    assertThat(html, containsString("type=\"button\""));
  }

  @Test
  public void T_APL_FE_001_vistaLugares_contieneSelectDeFiltro() {
    // 3- Verifica la presencia del elemento <select> como pide en Trello pero este select es del
    // filtro de categorías del sidebar.
    assertThat(html, containsString("<select"));
  }
}
