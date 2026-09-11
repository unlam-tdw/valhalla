# [PLC] Explorar Lugares + Ficha

> Trello: https://trello.com/c/mPp6mZml/3-plc-explorar-lugares
> **Note:** This is an SDD proposal. Implementation may change based on team decisions.

## Objective

Show an interactive map of Buenos Aires with markers for each place. Synchronized sidebar. Category filters. Complete place detail with image, description, embedded map.

## Prerequisites

- [LOG] completed (Spring Security configured)
- Leaflet.js loaded (CDN)
- Vue.js loaded (CDN)

## Steps

### 1. Create Place entity

File: `src/main/java/com/valhalla/domain/place/Place.java`

```java
package com.valhalla.domain.place;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "places")
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotBlank(message = "Category is required")
    @Column(nullable = false)
    private String category;

    private String address;

    @Column(name = "image_url")
    private String imageUrl;

    private Double latitude;
    private Double longitude;

    // Getters and setters
}
```

### 2. Create PlaceRepository

File: `src/main/java/com/valhalla/domain/place/PlaceRepository.java`

```java
package com.valhalla.domain.place;

import java.util.List;
import java.util.Optional;

public interface PlaceRepository {
    List<Place> findAll();
    Optional<Place> findById(Long id);
    List<Place> findByCategory(String category);
    List<Place> findByNameContainingIgnoreCase(String name);
}
```

### 3. Create PlaceService

File: `src/main/java/com/valhalla/domain/place/PlaceService.java`

```java
package com.valhalla.domain.place;

import java.util.List;
import java.util.Optional;

public interface PlaceService {
    List<Place> getAllPlaces();
    Optional<Place> getPlaceById(Long id);
    List<Place> getPlacesByCategory(String category);
    List<Place> searchPlaces(String query);
}
```

File: `src/main/java/com/valhalla/domain/place/PlaceServiceImpl.java`

```java
package com.valhalla.domain.place;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PlaceServiceImpl implements PlaceService {

    private final PlaceRepository placeRepository;

    @Autowired
    public PlaceServiceImpl(PlaceRepository placeRepository) {
        this.placeRepository = placeRepository;
    }

    @Override
    public List<Place> getAllPlaces() {
        return placeRepository.findAll();
    }

    @Override
    public Optional<Place> getPlaceById(Long id) {
        return placeRepository.findById(id);
    }

    @Override
    public List<Place> getPlacesByCategory(String category) {
        return placeRepository.findByCategory(category);
    }

    @Override
    public List<Place> searchPlaces(String query) {
        return placeRepository.findByNameContainingIgnoreCase(query);
    }
}
```

### 4. Create JPA Repository

File: `src/main/java/com/valhalla/infrastructure/place/JpaPlaceRepository.java`

```java
package com.valhalla.infrastructure.place;

import com.valhalla.domain.place.Place;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPlaceRepository extends JpaRepository<Place, Long> {
    List<Place> findByCategory(String category);
    List<Place> findByNameContainingIgnoreCase(String name);
}
```

### 5. Create PlaceRepositoryImpl

File: `src/main/java/com/valhalla/infrastructure/place/PlaceRepositoryImpl.java`

```java
package com.valhalla.infrastructure.place;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.place.PlaceRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

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
    public Optional<Place> findById(Long id) {
        return jpaPlaceRepository.findById(id);
    }

    @Override
    public List<Place> findByCategory(String category) {
        return jpaPlaceRepository.findByCategory(category);
    }

    @Override
    public List<Place> findByNameContainingIgnoreCase(String name) {
        return jpaPlaceRepository.findByNameContainingIgnoreCase(name);
    }
}
```

### 6. Create PlaceController

File: `src/main/java/com/valhalla/presentation/place/PlaceController.java`

```java
package com.valhalla.presentation.place;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.place.PlaceService;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/places")
public class PlaceController {

    private final PlaceService placeService;

    @Autowired
    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @GetMapping
    public ModelAndView listPlaces(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String search
    ) {
        Map<String, Object> model = new ModelMap();
        List<Place> places;

        if (category != null && !category.isEmpty()) {
            places = placeService.getPlacesByCategory(category);
        } else if (search != null && !search.isEmpty()) {
            places = placeService.searchPlaces(search);
        } else {
            places = placeService.getAllPlaces();
        }

        model.put("places", places);
        return new ModelAndView("pages/places/list", model);
    }

    @GetMapping("/{id}")
    public ModelAndView placeDetail(@PathVariable Long id) {
        Map<String, Object> model = new ModelMap();
        placeService.getPlaceById(id).ifPresent(place -> model.put("place", place));
        return new ModelAndView("pages/places/detail", model);
    }
}
```

### 7. Create Place REST endpoint (for Vue.js)

File: `src/main/java/com/valhalla/presentation/place/PlaceRestController.java`

```java
package com.valhalla.presentation.place;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.place.PlaceService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/places")
public class PlaceRestController {

    private final PlaceService placeService;

    @Autowired
    public PlaceRestController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @GetMapping
    public List<Place> listPlaces(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String search
    ) {
        if (category != null && !category.isEmpty()) {
            return placeService.getPlacesByCategory(category);
        } else if (search != null && !search.isEmpty()) {
            return placeService.searchPlaces(search);
        }
        return placeService.getAllPlaces();
    }
}
```

### 8. Create Seeder

File: `src/main/java/com/valhalla/infrastructure/PlaceDataSeeder.java`

```java
package com.valhalla.infrastructure;

import com.valhalla.domain.place.Place;
import com.valhalla.domain.place.PlaceRepository;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class PlaceDataSeeder implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger LOGGER = Logger.getLogger(PlaceDataSeeder.class.getName());
    private final PlaceRepository placeRepository;
    private boolean seeded = false;

    @Autowired
    public PlaceDataSeeder(PlaceRepository placeRepository) {
        this.placeRepository = placeRepository;
    }

    @Override
    public synchronized void onApplicationEvent(ContextRefreshedEvent event) {
        if (seeded || placeRepository.count() > 0) return;
        seeded = true;
        savePlace("El Sanjuanino", "Classic porteno empanadas and northern food",
            "RESTAURANT", "Av. del Libertador 1234", -34.5895, -58.4095);
        savePlace("La Viruta", "Tango milonga and dance school in Palermo",
            "NIGHTLIFE", "Armenia 1366", -34.6025, -58.4185);
        savePlace("MALBA", "Museum of Latin American Art of Buenos Aires",
            "MUSEUM", "Av. Figueroa Alcorta 3415", -34.5833, -58.3928);
        savePlace("Plaza Serrano", "Plaza with artisan market on Sundays",
            "SHOPPING", "Plaza Cortazar, Palermo", -34.5818, -58.4187);
        savePlace("Cafe Tortoni", "Oldest cafe in Buenos Aires (1858)",
            "CAFE", "Av. de Mayo 825", -34.6083, -58.3722);
        savePlace("Parque Tres de Febrero", "Large park with lakes and green areas",
            "PARK", "Palermo", -34.5747, -58.4103);
        savePlace("El Ateneo Grand Splendid", "Bookstore in a former theater",
            "CULTURE", "Av. Santa Fe 1860", -34.5967, -58.3817);
        savePlace("Cerveceria General San Martin", "Craft brewery with live shows",
            "BAR", "Av. Corrientes 1475", -34.6033, -58.3817);
        savePlace("Planetario Galileo Galilei", "Planetarium in Parque Tres de Febrero",
            "CULTURE", "Av. Sarmiento s/n", -34.5725, -58.4172);
        savePlace("Parrilla Don Julio", "Internationally award-winning grill",
            "RESTAURANT", "Guatemala 4699", -34.5892, -58.4262);
        LOGGER.info("Place seed data loaded");
    }

    private void savePlace(String name, String description, String category,
                           String address, double lat, double lng) {
        Place place = new Place();
        place.setName(name);
        place.setDescription(description);
        place.setCategory(category);
        place.setAddress(address);
        place.setLatitude(lat);
        place.setLongitude(lng);
        placeRepository.save(place);
    }
}
```

### 9. Create list template with Vue.js + Leaflet

File: `src/main/webapp/WEB-INF/templates/pages/places/list.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Explore Places</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script src="https://unpkg.com/vue@3/dist/vue.global.js"></script>
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
</head>
<body class="bg-gray-100">
    <div th:replace="~{fragments/header :: header}"></div>

    <div id="app" class="flex h-[calc(100vh-64px)]">
        <aside class="w-96 overflow-y-auto bg-white border-r">
            <div class="p-4">
                <h2 class="text-xl font-bold mb-4">Places</h2>

                <div class="space-y-2 mb-4">
                    <select v-model="selectedCategory" @change="filterPlaces"
                            class="w-full px-3 py-2 border rounded focus:outline-none focus:border-blue-500">
                        <option value="">All categories</option>
                        <option value="RESTAURANT">Restaurant</option>
                        <option value="BAR">Bar</option>
                        <option value="CAFE">Cafe</option>
                        <option value="MUSEUM">Museum</option>
                        <option value="PARK">Park</option>
                        <option value="SHOPPING">Shopping</option>
                        <option value="NIGHTLIFE">Nightlife</option>
                        <option value="CULTURE">Culture</option>
                    </select>

                    <input v-model="searchQuery" @input="filterPlaces" type="text"
                           placeholder="Search..."
                           class="w-full px-3 py-2 border rounded focus:outline-none focus:border-blue-500">
                </div>

                <div class="space-y-2">
                    <div v-for="place in filteredPlaces" :key="place.id"
                         @click="focusPlace(place)"
                         class="p-3 border rounded cursor-pointer hover:bg-gray-50">
                        <h3 class="font-medium">{{ place.name }}</h3>
                        <span class="inline-block px-2 py-0.5 text-xs text-white bg-blue-500 rounded">
                            {{ place.category }}
                        </span>
                        <p class="text-sm text-gray-600 mt-1">{{ place.description }}</p>
                        <a :href="'/places/' + place.id" class="text-blue-500 text-sm hover:underline">
                            View details
                        </a>
                    </div>
                </div>
            </div>
        </aside>

        <div id="map" class="flex-1"></div>
    </div>

    <script>
        const { createApp, ref, onMounted, computed } = Vue;

        createApp({
            setup() {
                const places = ref([]);
                const selectedCategory = ref('');
                const searchQuery = ref('');
                let map = null;
                let markers = [];

                const categoryColors = {
                    'RESTAURANT': '#e74c3c',
                    'BAR': '#9b59b6',
                    'CAFE': '#e67e22',
                    'MUSEUM': '#3498db',
                    'PARK': '#2ecc71',
                    'SHOPPING': '#f39c12',
                    'NIGHTLIFE': '#1abc9c',
                    'CULTURE': '#34495e'
                };

                const filteredPlaces = computed(() => {
                    return places.value;
                });

                onMounted(async () => {
                    map = L.map('map').setView([-34.6037, -58.3816], 13);
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        attribution: '&copy; OpenStreetMap contributors'
                    }).addTo(map);

                    const response = await fetch('/api/places');
                    places.value = await response.json();
                    addMarkers();
                });

                function addMarkers() {
                    markers.forEach(m => map.removeLayer(m));
                    markers = [];

                    places.value.forEach(place => {
                        if (place.latitude && place.longitude) {
                            const color = categoryColors[place.category] || '#3498db';
                            const marker = L.circleMarker([place.latitude, place.longitude], {
                                radius: 8, fillColor: color, color: '#fff',
                                weight: 2, fillOpacity: 0.8
                            }).addTo(map);

                            marker.bindPopup(
                                '<strong>' + place.name + '</strong><br>' + place.category + '<br>' +
                                '<a href="/places/' + place.id + '">View details</a>'
                            );
                            marker.placeData = place;
                            markers.push(marker);
                        }
                    });
                }

                async function filterPlaces() {
                    let url = '/api/places?';
                    if (selectedCategory.value) url += 'category=' + selectedCategory.value + '&';
                    if (searchQuery.value) url += 'search=' + searchQuery.value;

                    const response = await fetch(url);
                    places.value = await response.json();
                    addMarkers();
                }

                function focusPlace(place) {
                    if (place.latitude && place.longitude) {
                        map.setView([place.latitude, place.longitude], 16);
                    }
                }

                return { places, selectedCategory, searchQuery, filteredPlaces,
                         filterPlaces, focusPlace };
            }
        }).mount('#app');
    </script>
</body>
</html>
```

### 10. Create detail template

File: `src/main/webapp/WEB-INF/templates/pages/places/detail.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
    <meta charset="UTF-8">
    <title th:text="${place?.name}">Place Detail</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
</head>
<body class="bg-gray-100 min-h-screen">
    <div th:replace="~{fragments/header :: header}"></div>

    <main class="container mx-auto px-4 py-8" th:if="${place}">
        <div class="flex gap-8">
            <div class="flex-1">
                <div th:if="${place.imageUrl}" class="rounded-lg overflow-hidden">
                    <img th:src="${place.imageUrl}" th:alt="${place.name}" class="w-full h-80 object-cover">
                </div>
                <div th:unless="${place.imageUrl}" class="w-full h-80 bg-gray-200 rounded-lg flex items-center justify-center">
                    <span class="text-gray-500">No image</span>
                </div>
            </div>

            <div class="flex-1">
                <h1 class="text-3xl font-bold mb-2" th:text="${place.name}"></h1>
                <span class="inline-block px-3 py-1 text-sm text-white bg-blue-500 rounded mb-4"
                      th:text="${place.category}"></span>

                <p class="text-gray-600 mb-2" th:if="${place.address}">
                    <strong>Address:</strong> <span th:text="${place.address}"></span>
                </p>
                <p class="text-gray-700 mb-6" th:text="${place.description}"></p>

                <!-- "Add to plan" button is implemented in [APL-FE] spec -->
                <div sec:authorize="isAuthenticated()" class="mb-6" id="add-to-plan-placeholder">
                    <p class="text-sm text-gray-500 italic">Add to plan feature coming soon.</p>
                </div>

                <a th:href="@{/places}" class="bg-gray-500 hover:bg-gray-600 text-white px-4 py-2 rounded inline-block">
                    Back to list
                </a>
            </div>
        </div>

        <div class="mt-8">
            <h2 class="text-xl font-bold mb-4">Location</h2>
            <div id="detail-map" class="h-96 rounded-lg"></div>
        </div>
    </main>

    <div th:unless="${place}" class="container mx-auto px-4 py-8 text-center">
        <h1 class="text-2xl font-bold text-red-500">Place not found</h1>
        <a th:href="@{/places}" class="text-blue-500 hover:underline">Back to list</a>
    </div>

    <script th:inline="javascript">
        const place = [[${place}]];
        if (place && place.latitude && place.longitude) {
            const map = L.map('detail-map').setView([place.latitude, place.longitude], 16);
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; OpenStreetMap contributors'
            }).addTo(map);

            L.circleMarker([place.latitude, place.longitude], {
                radius: 12, fillColor: '#e74c3c', color: '#fff',
                weight: 3, fillOpacity: 1
            }).addTo(map).bindPopup('<strong>' + place.name + '</strong>').openPopup();
        }
    </script>
</body>
</html>
```

**Note:** The "Add to plan" dropdown is implemented in [APL-FE] spec (05-APL-FE.md) which has the PlanPlace entity and PlanService. This spec only shows the place detail with image, description, and map.

## Verification

1. `mvn test` — all tests pass
2. Go to `/places` — map loads with markers
3. Select category — markers filter
4. Search name — markers filter
5. Click marker — popup with info
6. Click card in sidebar — map centers
7. Click "View details" — goes to `/places/{id}`
8. Detail shows image (or placeholder), name, category, address, description
9. Detail shows map with highlighted marker
10. "Add to plan" section visible for logged-in users (full functionality in [APL-FE])

## Files to create

| File | Action |
|------|--------|
| `domain/place/Place.java` | Create (with @NotBlank validation) |
| `domain/place/PlaceRepository.java` | Create |
| `domain/place/PlaceService.java` | Create |
| `domain/place/PlaceServiceImpl.java` | Create |
| `infrastructure/place/JpaPlaceRepository.java` | Create |
| `infrastructure/place/PlaceRepositoryImpl.java` | Create |
| `presentation/place/PlaceController.java` | Create |
| `presentation/place/PlaceRestController.java` | Create |
| `config/PlaceDataSeeder.java` | Create |
| `templates/pages/places/list.html` | Create |
| `templates/pages/places/detail.html` | Create (with image handling + map) |
