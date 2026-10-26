package com.valhalla.infrastructure.place;

import com.valhalla.domain.place.BoundingBox;
import com.valhalla.domain.place.MapPoi;
import com.valhalla.domain.place.MapPoiRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class MapPoiRepositoryImpl implements MapPoiRepository {

  private final JpaMapPoiRepository jpaMapPoiRepository;

  @Autowired
  public MapPoiRepositoryImpl(JpaMapPoiRepository jpaMapPoiRepository) {
    this.jpaMapPoiRepository = jpaMapPoiRepository;
  }

  @Override
  public List<MapPoi> findInBox(String tagKey, String tagValue, BoundingBox box) {
    return jpaMapPoiRepository.findInBox(
      tagKey,
      tagValue,
      box.south(),
      box.west(),
      box.north(),
      box.east()
    );
  }

  @Override
  public void saveAll(List<MapPoi> pois) {
    jpaMapPoiRepository.saveAll(pois);
  }
}
