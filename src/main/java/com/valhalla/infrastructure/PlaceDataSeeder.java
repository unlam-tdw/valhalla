package com.valhalla.infrastructure;

import com.valhalla.domain.place.Place;
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
      "Classic porteno empanadas and northern food",
      "RESTAURANT",
      "Av. del Libertador 1234",
      -34.5895,
      -58.4095
    );
    savePlace(
      "La Viruta",
      "Tango milonga and dance school in Palermo",
      "NIGHTLIFE",
      "Armenia 1366",
      -34.6025,
      -58.4185
    );
    savePlace(
      "MALBA",
      "Museum of Latin American Art of Buenos Aires",
      "MUSEUM",
      "Av. Figueroa Alcorta 3415",
      -34.5833,
      -58.3928
    );
    savePlace(
      "Plaza Serrano",
      "Plaza with artisan market on Sundays",
      "SHOPPING",
      "Plaza Cortazar, Palermo",
      -34.5818,
      -58.4187
    );
    savePlace(
      "Cafe Tortoni",
      "Oldest cafe in Buenos Aires (1858)",
      "CAFE",
      "Av. de Mayo 825",
      -34.6083,
      -58.3722
    );
    savePlace(
      "Parque Tres de Febrero",
      "Large park with lakes and green areas",
      "PARK",
      "Palermo",
      -34.5747,
      -58.4103
    );
    savePlace(
      "El Ateneo Grand Splendid",
      "Bookstore in a former theater",
      "CULTURE",
      "Av. Santa Fe 1860",
      -34.5967,
      -58.3817
    );
    savePlace(
      "Cerveceria General San Martin",
      "Craft brewery with live shows",
      "BAR",
      "Av. Corrientes 1475",
      -34.6033,
      -58.3817
    );
    savePlace(
      "Planetario Galileo Galilei",
      "Planetarium in Parque Tres de Febrero",
      "CULTURE",
      "Av. Sarmiento s/n",
      -34.5725,
      -58.4172
    );
    savePlace(
      "Parrilla Don Julio",
      "Internationally award-winning grill",
      "RESTAURANT",
      "Guatemala 4699",
      -34.5892,
      -58.4262
    );
    LOGGER.info("Place seed data loaded");
  }

  private void savePlace(
    String name,
    String description,
    String category,
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
