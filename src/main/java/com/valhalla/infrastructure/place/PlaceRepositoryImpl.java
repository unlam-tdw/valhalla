package com.valhalla.infrastructure.place;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.place.PlaceCategory;
import com.valhalla.domain.place.PlaceRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Repository;

/**
 * Cache: la lista de {@code findAll()} se guarda en el servicio, pero se tira aca, en el punto
 * donde la fila cambia. Invalidar en el servicio dejaria afuera toda escritura que no pase por el
 * (los seeders y los tests siembran por este puerto), y una lista cacheada sobrevive a la fila que
 * la origino.
 */
@Repository
public class PlaceRepositoryImpl implements PlaceRepository {

  private final JpaPlaceRepository jpaPlaceRepository;

  @Autowired
  public PlaceRepositoryImpl(JpaPlaceRepository jpaPlaceRepository) {
    this.jpaPlaceRepository = jpaPlaceRepository;
  }

  @Override
  public List<Place> findAll() {
    return jpaPlaceRepository.findAll();
  }

  @Override
  public Optional<Place> findById(UUID id) {
    return jpaPlaceRepository.findById(id);
  }

  @Override
  public Optional<Place> findByExternalId(String externalId) {
    return jpaPlaceRepository.findByExternalId(externalId);
  }

  @Override
  public List<Place> findByCategory(PlaceCategory category) {
    return jpaPlaceRepository.findByCategory(category);
  }

  @Override
  public List<Place> findByNameContainingIgnoreCase(String name) {
    return jpaPlaceRepository.findByNameContainingIgnoreCase(name);
  }

  @Override
  public List<Place> findByCategoryAndNameContainingIgnoreCase(
    PlaceCategory category,
    String name
  ) {
    return jpaPlaceRepository.findByCategoryAndNameContainingIgnoreCase(category, name);
  }

  @Override
  @CacheEvict(cacheNames = "places", allEntries = true)
  public void save(Place place) {
    jpaPlaceRepository.save(place);
  }

  @Override
  @CacheEvict(cacheNames = "places", allEntries = true)
  public void deleteById(UUID id) {
    jpaPlaceRepository.deleteById(id);
  }

  @Override
  public long count() {
    return jpaPlaceRepository.count();
  }
}
