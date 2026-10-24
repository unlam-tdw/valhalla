package com.valhalla.domain.place;

import com.valhalla.domain.plan.UuidVersion7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

/**
 * Cobertura de una zona: cuando se trajo por ultima vez ese tag en esa caja. [POI]
 *
 * <p>Existe para distinguir "esta zona nunca se cargo" de "se cargo y Overpass no devolvio nada",
 * que sin esta fila se ven iguales, y para mostrar la antiguedad de lo que hay en el mapa sin
 * guardar un timestamp en cada POI.
 *
 * <p>Accessors minimos, igual que {@link MapPoi}: Hibernate entra por campo y la cobertura de
 * {@code domain} esta pineada al 100%.
 */
@Entity
@Table(
  name = "poi_zone",
  uniqueConstraints = @UniqueConstraint(columnNames = { "zone_key", "tag_key", "tag_value" })
)
public class PoiZone {

  @Id
  @UuidGenerator(algorithm = UuidVersion7Generator.class)
  private UUID id;

  @Column(name = "zone_key", nullable = false, length = 128)
  private String zoneKey;

  @Column(name = "tag_key", nullable = false, length = 32)
  private String tagKey;

  @Column(name = "tag_value", nullable = false, length = 128)
  private String tagValue;

  @Column(name = "fetched_at", nullable = false)
  private Instant fetchedAt;

  public PoiZone() {}

  public PoiZone(String zoneKey, String tagKey, String tagValue, Instant fetchedAt) {
    this.zoneKey = zoneKey;
    this.tagKey = tagKey;
    this.tagValue = tagValue;
    this.fetchedAt = fetchedAt;
  }

  public String getZoneKey() {
    return zoneKey;
  }

  public Instant getFetchedAt() {
    return fetchedAt;
  }

  public void setFetchedAt(Instant fetchedAt) {
    this.fetchedAt = fetchedAt;
  }
}
