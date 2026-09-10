# [APL-BE] Agregar Lugares al Plan (Backend)

> Trello: https://trello.com/c/zHj13Gy6/6-apl-agregar-lugares-al-plan
> **Note:** This is an SDD proposal. Implementation may change based on team decisions.

## Objective

Backend for adding places to plans: entity, repository, service, REST endpoints, and PlaceController update.

## Prerequisites

- [PLC] completed (Place entity)
- [PLN] completed (Plan entity)

## Steps

### 1. Create PlanPlace entity

File: `src/main/java/com/valhalla/domain/planplace/PlanPlace.java`

```java
package com.valhalla.domain.planplace;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.plan.Plan;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "plan_places", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"plan_id", "place_id"})
})
public class PlanPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "visit_date")
    private LocalDate visitDate;

    @Column(name = "visit_time")
    private LocalTime visitTime;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    // Getters and setters
}
```

### 2. Create PlanPlaceRepository

File: `src/main/java/com/valhalla/domain/planplace/PlanPlaceRepository.java`

```java
package com.valhalla.domain.planplace;

import java.util.List;
import java.util.Optional;

public interface PlanPlaceRepository {
    PlanPlace save(PlanPlace planPlace);
    List<PlanPlace> findByPlanId(Long planId);
    Optional<PlanPlace> findById(Long id);
    boolean existsByPlanIdAndPlaceId(Long planId, Long placeId);
    void deleteById(Long id);
}
```

### 3. Create PlanPlaceService

File: `src/main/java/com/valhalla/domain/planplace/PlanPlaceService.java`

```java
package com.valhalla.domain.planplace;

import java.util.List;

public interface PlanPlaceService {
    PlanPlace addPlaceToPlan(Long planId, Long placeId);
    List<PlanPlace> getItinerary(Long planId);
    PlanPlace updatePlanPlace(PlanPlace planPlace);
    void removePlaceFromPlan(Long planPlaceId);
    void reorderPlaces(Long planId, List<Long> placeIds);
    boolean isPlaceInPlan(Long planId, Long placeId);
}
```

File: `src/main/java/com/valhalla/domain/planplace/PlanPlaceServiceImpl.java`

```java
package com.valhalla.domain.planplace;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.place.PlaceRepository;
import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanRepository;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanPlaceServiceImpl implements PlanPlaceService {

    private final PlanPlaceRepository planPlaceRepository;
    private final PlanRepository planRepository;
    private final PlaceRepository placeRepository;

    @Autowired
    public PlanPlaceServiceImpl(
        PlanPlaceRepository planPlaceRepository,
        PlanRepository planRepository,
        PlaceRepository placeRepository
    ) {
        this.planPlaceRepository = planPlaceRepository;
        this.planRepository = planRepository;
        this.placeRepository = placeRepository;
    }

    @Override
    @Transactional
    public PlanPlace addPlaceToPlan(Long planId, Long placeId) {
        if (isPlaceInPlan(planId, placeId)) {
            throw new IllegalStateException("Place already in plan");
        }

        Plan plan = planRepository.findById(planId)
            .orElseThrow(() -> new RuntimeException("Plan not found"));
        Place place = placeRepository.findById(placeId)
            .orElseThrow(() -> new RuntimeException("Place not found"));

        List<PlanPlace> existing = planPlaceRepository.findByPlanId(planId);
        int nextOrder = existing.stream()
            .mapToInt(PlanPlace::getSortOrder)
            .max()
            .orElse(0) + 1;

        PlanPlace planPlace = new PlanPlace();
        planPlace.setPlan(plan);
        planPlace.setPlace(place);
        planPlace.setSortOrder(nextOrder);

        return planPlaceRepository.save(planPlace);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanPlace> getItinerary(Long planId) {
        return planPlaceRepository.findByPlanId(planId);
    }

    @Override
    public PlanPlace updatePlanPlace(PlanPlace planPlace) {
        return planPlaceRepository.save(planPlace);
    }

    @Override
    public void removePlaceFromPlan(Long planPlaceId) {
        planPlaceRepository.deleteById(planPlaceId);
    }

    @Override
    @Transactional
    public void reorderPlaces(Long planId, List<Long> placeIds) {
        AtomicInteger order = new AtomicInteger(1);
        placeIds.forEach(placeId -> {
            planPlaceRepository.findByPlanId(planId).stream()
                .filter(pp -> pp.getPlace().getId().equals(placeId))
                .findFirst()
                .ifPresent(pp -> {
                    pp.setSortOrder(order.getAndIncrement());
                    planPlaceRepository.save(pp);
                });
        });
    }

    @Override
    public boolean isPlaceInPlan(Long planId, Long placeId) {
        return planPlaceRepository.existsByPlanIdAndPlaceId(planId, placeId);
    }
}
```

### 4. Create JPA Repository

File: `src/main/java/com/valhalla/infrastructure/planplace/JpaPlanPlaceRepository.java`

```java
package com.valhalla.infrastructure.planplace;

import com.valhalla.domain.planplace.PlanPlace;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPlanPlaceRepository extends JpaRepository<PlanPlace, Long> {
    @EntityGraph(attributePaths = {"place"})
    List<PlanPlace> findByPlanIdOrderBySortOrder(Long planId);
    boolean existsByPlanIdAndPlaceId(Long planId, Long placeId);
}
```

### 5. Create PlanPlaceRepositoryImpl

File: `src/main/java/com/valhalla/infrastructure/planplace/PlanPlaceRepositoryImpl.java`

```java
package com.valhalla.infrastructure.planplace;

import com.valhalla.domain.planplace.PlanPlace;
import com.valhalla.domain.planplace.PlanPlaceRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class PlanPlaceRepositoryImpl implements PlanPlaceRepository {

    private final JpaPlanPlaceRepository jpa;

    @Autowired
    public PlanPlaceRepositoryImpl(JpaPlanPlaceRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public PlanPlace save(PlanPlace planPlace) {
        return jpa.save(planPlace);
    }

    @Override
    public List<PlanPlace> findByPlanId(Long planId) {
        return jpa.findByPlanIdOrderBySortOrder(planId);
    }

    @Override
    public Optional<PlanPlace> findById(Long id) {
        return jpa.findById(id);
    }

    @Override
    public boolean existsByPlanIdAndPlaceId(Long planId, Long placeId) {
        return jpa.existsByPlanIdAndPlaceId(planId, placeId);
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }
}
```

### 6. Create REST endpoints

File: `src/main/java/com/valhalla/presentation/plan/PlanPlaceRestController.java`

```java
package com.valhalla.presentation.plan;

import com.valhalla.domain.planplace.PlanPlace;
import com.valhalla.domain.planplace.PlanPlaceService;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plans/{planId}/places")
public class PlanPlaceRestController {

    private final PlanPlaceService planPlaceService;

    @Autowired
    public PlanPlaceRestController(PlanPlaceService planPlaceService) {
        this.planPlaceService = planPlaceService;
    }

    @GetMapping
    public List<PlanPlace> getItinerary(@PathVariable Long planId) {
        return planPlaceService.getItinerary(planId);
    }

    @PostMapping
    public PlanPlace addPlace(
        @PathVariable Long planId,
        @RequestParam Long placeId
    ) {
        return planPlaceService.addPlaceToPlan(planId, placeId);
    }

    @PutMapping("/{id}")
    public PlanPlace updatePlanPlace(
        @PathVariable Long planId,
        @PathVariable Long id,
        @RequestBody PlanPlace planPlace
    ) {
        planPlace.setId(id);
        return planPlaceService.updatePlanPlace(planPlace);
    }

    @DeleteMapping("/{id}")
    public Map<String, Boolean> removePlace(
        @PathVariable Long planId,
        @PathVariable Long id
    ) {
        planPlaceService.removePlaceFromPlan(id);
        return Map.of("success", true);
    }

    @PostMapping("/reorder")
    public Map<String, Boolean> reorder(
        @PathVariable Long planId,
        @RequestBody List<Long> placeIds
    ) {
        planPlaceService.reorderPlaces(planId, placeIds);
        return Map.of("success", true);
    }
}
```

### 7. Update PlaceController (add user plans to model)

File: `src/main/java/com/valhalla/presentation/place/PlaceController.java`

Update the `placeDetail` method to include user's plans:

```java
@GetMapping("/{id}")
public ModelAndView placeDetail(
    @PathVariable Long id,
    @AuthenticationPrincipal UserDetails userDetails
) {
    Map<String, Object> model = new ModelMap();
    placeService.getPlaceById(id).ifPresent(place -> model.put("place", place));

    if (userDetails != null) {
        List<Plan> userPlans = planService.getPlansByUserEmail(userDetails.getUsername());
        model.put("userPlans", userPlans);
    }

    return new ModelAndView("pages/places/detail", model);
}
```

Add required imports:
```java
import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
```

Add `PlanService` to constructor:
```java
private final PlaceService placeService;
private final PlanService planService;

@Autowired
public PlaceController(PlaceService placeService, PlanService planService) {
    this.placeService = placeService;
    this.planService = planService;
}
```

## Verification

1. `mvn test` — all tests pass
2. POST `/api/plans/{planId}/places?placeId=X` — adds place to plan
3. GET `/api/plans/{planId}/places` — returns itinerary ordered by sortOrder
4. PUT `/api/plans/{planId}/places/{id}` — updates visitDate/visitTime
5. DELETE `/api/plans/{planId}/places/{id}` — removes place from plan
6. POST `/api/plans/{planId}/places/reorder` — reorders places
7. Adding same place twice → error
8. PlaceController detail page shows user's plans in model

## Files to create/modify

| File | Action |
|------|--------|
| `domain/planplace/PlanPlace.java` | Create |
| `domain/planplace/PlanPlaceRepository.java` | Create |
| `domain/planplace/PlanPlaceService.java` | Create |
| `domain/planplace/PlanPlaceServiceImpl.java` | Create |
| `infrastructure/planplace/JpaPlanPlaceRepository.java` | Create |
| `infrastructure/planplace/PlanPlaceRepositoryImpl.java` | Create |
| `presentation/plan/PlanPlaceRestController.java` | Create |
| `presentation/place/PlaceController.java` | Update (add PlanService, userPlans) |
