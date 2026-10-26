package com.valhalla.presentation.place;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.valhalla.domain.plan.PlanSettings;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

public class PlaceControllerTest {

  private PlaceController controller;

  @BeforeEach
  public void init() {
    controller = new PlaceController();
  }

  @Test
  public void shouldReturnTheExploreView() {
    ModelAndView result = controller.listPlaces();

    assertThat(result.getViewName(), is(equalTo("pages/places/list")));
  }

  /**
   * El listado de /explore lo arma el JS pidiendo /api/places: el catalogo en el modelo es una
   * lectura que ningun template consume.
   */
  @Test
  public void shouldNotPutTheCatalogueInTheModel() {
    ModelAndView result = controller.listPlaces();

    assertThat(result.getModel().get("places"), is(nullValue()));
    assertThat(result.getModel().get("categories"), is(nullValue()));
  }

  @Test
  public void shouldBuildTheCreatePlanPanelForTheView() {
    ModelAndView result = controller.listPlaces();

    assertThat(result.getModel().get("defaultPlanSettings"), is(instanceOf(PlanSettings.class)));
    assertThat(result.getModel().get("today"), is(equalTo(LocalDate.now().toString())));
  }
}
