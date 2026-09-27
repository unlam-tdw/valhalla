package com.valhalla.infrastructure;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.place.PlaceCategory;
import com.valhalla.domain.place.PlaceRepository;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class PlaceDataSeeder implements ApplicationListener<ContextRefreshedEvent> {

  private static final Logger LOGGER = Logger.getLogger(PlaceDataSeeder.class.getName());
  private final PlaceRepository placeRepository;
  private boolean seeded;

  @Autowired
  public PlaceDataSeeder(PlaceRepository placeRepository) {
    this.placeRepository = placeRepository;
  }

  @Override
  public synchronized void onApplicationEvent(ContextRefreshedEvent event) {
    if (seeded || placeRepository.count() > 0) {
      return;
    }
    seeded = true;
    savePlace(
      "El Sanjuanino",
      "Empanadas portenas clasicas y comida del norte",
      PlaceCategory.RESTAURANT,
      "Av. del Libertador 1234",
      -34.5895,
      -58.4095
    );
    savePlace(
      "La Viruta",
      "Milonga de tango y escuela de baile en Palermo",
      PlaceCategory.NIGHTLIFE,
      "Armenia 1366",
      -34.6025,
      -58.4185
    );
    savePlace(
      "MALBA",
      "Museo de Arte Latinoamericano de Buenos Aires",
      PlaceCategory.MUSEUM,
      "Av. Figueroa Alcorta 3415",
      -34.5833,
      -58.3928
    );
    savePlace(
      "Plaza Serrano",
      "Plaza con feria de artesanos los domingos",
      PlaceCategory.SHOPPING,
      "Plaza Cortazar, Palermo",
      -34.5818,
      -58.4187
    );
    savePlace(
      "Cafe Tortoni",
      "El cafe mas antiguo de Buenos Aires (1858)",
      PlaceCategory.CAFE,
      "Av. de Mayo 825",
      -34.6083,
      -58.3722
    );
    savePlace(
      "Parque Tres de Febrero",
      "Gran parque con lagos y areas verdes",
      PlaceCategory.PARK,
      "Palermo",
      -34.5747,
      -58.4103
    );
    savePlace(
      "El Ateneo Grand Splendid",
      "Libreria en un antiguo teatro",
      PlaceCategory.CULTURE,
      "Av. Santa Fe 1860",
      -34.5967,
      -58.3817
    );
    savePlace(
      "Cerveceria General San Martin",
      "Cerveceria artesanal con shows en vivo",
      PlaceCategory.BAR,
      "Av. Corrientes 1475",
      -34.6033,
      -58.3817
    );
    savePlace(
      "Planetario Galileo Galilei",
      "Planetario ubicado en el Parque Tres de Febrero",
      PlaceCategory.CULTURE,
      "Av. Sarmiento s/n",
      -34.5725,
      -58.4172
    );
    savePlace(
      "Parrilla Don Julio",
      "Parrilla galardonada a nivel internacional",
      PlaceCategory.RESTAURANT,
      "Guatemala 4699",
      -34.5892,
      -58.4262
    );
    LOGGER.info("Place seed data loaded");
  }

  private void savePlace(
    String name,
    String description,
    PlaceCategory category,
    String address,
    double latitude,
    double longitude
  ) {
    Place place = new Place();
    place.setName(name);
    place.setDescription(description);
    place.setCategory(category);
    place.setAddress(address);
    place.setLatitude(latitude);
    place.setLongitude(longitude);
    placeRepository.save(place);
  }
}
