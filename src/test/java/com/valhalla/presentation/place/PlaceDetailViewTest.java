package com.valhalla.presentation.place;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

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
  public void T_APL_FE_001_vistaLugares_contieneBotonAddToPlan() {
    // 1- Verifica que exista el botón "Add to plan".
    assertThat(html, containsString("Add to plan"));

    // 2- Habria que verificar que sea un botón submit pero se decidió poner en list.html
    // figura como un type="button".
    assertThat(html, containsString("type=\"button\""));
  }

  @Test
  public void T_APL_FE_001_vistaLugares_contieneSelectDeFiltro() {
    // 3- Verifica la presencia del elemento <select> como pide en Trello pero este select es del
    // filtro de categorías del sidebar.
    assertThat(html, containsString("<select"));
  }
}
