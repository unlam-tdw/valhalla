package com.valhalla.infrastructure.place;

import com.valhalla.domain.place.PoiZone;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPoiZoneRepository extends JpaRepository<PoiZone, UUID> {
  Optional<PoiZone> findByZoneKeyAndTagKeyAndTagValue(
    String zoneKey,
    String tagKey,
    String tagValue
  );
}
