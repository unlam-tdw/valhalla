package com.valhalla.domain.place;

import java.util.Optional;

public interface PoiZoneRepository {
  Optional<PoiZone> find(String zoneKey, String tagKey, String tagValue);

  void save(PoiZone zone);
}
