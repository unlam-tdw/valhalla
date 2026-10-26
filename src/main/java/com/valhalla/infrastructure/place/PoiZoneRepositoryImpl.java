package com.valhalla.infrastructure.place;

import com.valhalla.domain.place.PoiZone;
import com.valhalla.domain.place.PoiZoneRepository;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class PoiZoneRepositoryImpl implements PoiZoneRepository {

  private final JpaPoiZoneRepository jpaPoiZoneRepository;

  @Autowired
  public PoiZoneRepositoryImpl(JpaPoiZoneRepository jpaPoiZoneRepository) {
    this.jpaPoiZoneRepository = jpaPoiZoneRepository;
  }

  @Override
  public Optional<PoiZone> find(String zoneKey, String tagKey, String tagValue) {
    return jpaPoiZoneRepository.findByZoneKeyAndTagKeyAndTagValue(zoneKey, tagKey, tagValue);
  }

  @Override
  public void save(PoiZone zone) {
    jpaPoiZoneRepository.save(zone);
  }
}
