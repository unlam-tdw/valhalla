package com.valhalla.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valhalla.domain.place.BoundingBox;
import com.valhalla.domain.place.OsmPoi;
import com.valhalla.domain.place.PoiZoneRepository;
import com.valhalla.infrastructure.place.PoiIngestService;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * [POI] Carga en la tabla de POIs una zona de Palermo, una categoria por tag, desde un snapshot
 * versionado en {@code place-snapshot/palermo-pois.json}. Asi el panel de importacion muestra
 * puntos en la demo sin depender de Overpass ni de un clic previo.
 *
 * <p>Cuelga del mismo flag que el resto del demo ({@code -Dseed.demoPlans=true}). No llama a la
 * red: entra por {@link PoiIngestService#ingest}, el mismo camino que el boton "Cargar puntos de
 * esta zona", y por eso el diff, la baja logica y la cobertura quedan como en produccion.
 *
 * <p>Idempotente por categoria: si la cobertura de esa zona y tag ya existe, no vuelve a ingerir.
 * Asi un reinicio no reescribe {@code fetched_at} ni toca filas.
 */
@Component
public class PoiSnapshotSeeder implements ApplicationListener<ContextRefreshedEvent> {

  private static final Logger LOGGER = Logger.getLogger(PoiSnapshotSeeder.class.getName());
  private static final String SNAPSHOT_RESOURCE = "place-snapshot/palermo-pois.json";

  private final PublicPlanDataSeeder publicPlanSeeder;
  private final PoiIngestService poiIngestService;
  private final PoiZoneRepository poiZoneRepository;
  private final ObjectMapper objectMapper = new ObjectMapper();

  private boolean seeded;

  public PoiSnapshotSeeder(
    PublicPlanDataSeeder publicPlanSeeder,
    PoiIngestService poiIngestService,
    PoiZoneRepository poiZoneRepository
  ) {
    this.publicPlanSeeder = publicPlanSeeder;
    this.poiIngestService = poiIngestService;
    this.poiZoneRepository = poiZoneRepository;
  }

  @Override
  public synchronized void onApplicationEvent(ContextRefreshedEvent event) {
    if (seeded || !publicPlanSeeder.isEnabled()) {
      return;
    }
    seeded = true;
    Snapshot snapshot = load();
    BoundingBox zone = PoiIngestService.zoneOf(snapshot.zone());
    int loaded = 0;
    for (Category category : snapshot.categories()) {
      if (
        poiZoneRepository
          .find(PoiIngestService.zoneKey(zone), category.tagKey(), category.tagValue())
          .isPresent()
      ) {
        continue;
      }
      poiIngestService.ingest(category.tagKey(), category.tagValue(), zone, category.pois());
      loaded++;
    }
    if (loaded > 0 && LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("Map POI snapshot loaded: " + loaded + " categories for zone " + zone);
    }
  }

  /** Lee el snapshot del classpath. Un archivo roto debe fallar fuerte, no sembrar a medias. */
  Snapshot load() {
    try (InputStream stream = new ClassPathResource(SNAPSHOT_RESOURCE).getInputStream()) {
      return objectMapper.readValue(stream, Snapshot.class);
    } catch (IOException exception) {
      throw new IllegalStateException("Unreadable POI snapshot " + SNAPSHOT_RESOURCE, exception);
    }
  }

  /** Forma del archivo: una zona y un bloque por tag, con los POIs ya normalizados. */
  record Snapshot(BoundingBox zone, String source, List<Category> categories) {}

  record Category(String tagKey, String tagValue, List<OsmPoi> pois) {}
}
