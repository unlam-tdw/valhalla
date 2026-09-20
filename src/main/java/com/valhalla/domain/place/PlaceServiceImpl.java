package com.valhalla.domain.place;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PlaceServiceImpl implements PlaceService {

  private final PlaceRepository placeRepository;

  @Autowired
  public PlaceServiceImpl(PlaceRepository placeRepository) {
    this.placeRepository = placeRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Place> getAllPlaces() {
    return placeRepository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Place> getPlaceById(Long id) {
    return placeRepository.findById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Place> getPlacesByCategory(String category) {
    return placeRepository.findByCategory(category);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Place> searchPlaces(String query) {
    return placeRepository.findByNameContainingIgnoreCase(query);
  }
}
