package com.valhalla.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import com.valhalla.domain.place.*;
import com.valhalla.domain.plan.*;
import com.valhalla.domain.planplace.*;
import com.valhalla.infrastructure.planplace.JpaPlanPlaceRepository;
import com.valhalla.integration.JpaIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@JpaIntegrationTest
@Transactional
class PlanPlaceRepositoryTest {

  @Autowired
  PlanPlaceRepository entries;

  @Autowired
  JpaPlanPlaceRepository jpa;

  @Autowired
  PlanRepository plans;

  @Autowired
  PlaceRepository places;

  @Autowired
  EntityManager em;

  Plan plan;
  Place first;

  @BeforeEach
  void setup() {
    plan = new Plan();
    plan.setName("Repository plan");
    plans.save(plan);
    first = places.findAll().get(0);
  }

  PlanPlace save(Place place, int order) {
    PlanPlace entry = new PlanPlace();
    entry.setPlan(plan);
    entry.setPlace(place);
    entry.setSortOrder(order);
    return entries.save(entry);
  }

  @Test
  void I07_ordersAndFetchesPlaces() {
    PlanPlace late = save(first, 8);
    PlanPlace early = save(places.findAll().get(1), 2);
    em.flush();
    em.clear();
    var result = entries.findByPlanId(plan.getId());
    assertEquals(early.getId(), result.get(0).getId());
    assertEquals(late.getId(), result.get(1).getId());
    assertTrue(
      em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(result.get(0), "place")
    );
  }

  @Test
  void I08_existingMembership() {
    save(first, 1);
    assertTrue(entries.existsByPlanIdAndPlaceId(plan.getId(), first.getId()));
  }

  @Test
  void I09_missingMembership() {
    assertFalse(entries.existsByPlanIdAndPlaceId(plan.getId(), first.getId()));
  }

  @Test
  void I10_uniqueConstraintRejectsDuplicate() {
    save(first, 1);
    em.flush();
    assertThrows(
      DataIntegrityViolationException.class,
      () -> {
        save(first, 2);
        jpa.flush();
      }
    );
  }

  @Test
  void findAndDelete() {
    Long id = save(first, 1).getId();
    assertTrue(entries.findById(id).isPresent());
    entries.deleteById(id);
    assertTrue(entries.findById(id).isEmpty());
  }
}
