# [PLC] Explorar Lugares + Ficha

> Trello: https://trello.com/c/mPp6mZml/3-plc-explorar-lugares-ficha
> **Note:** This is an SDD proposal. Implementation may change based on team decisions.

## Objetivo

El sistema muestra un mapa interactivo de Buenos Aires con markers por cada lugar. Sidebar sincronizada. Filtros por categoria. Ficha de lugar completa con imagen, descripcion, direccion y mapa embebido.

## Pre-requisitos

- [LOG] completed (Spring Security configured)
- Leaflet.js loaded (CDN)
- Vue.js loaded (CDN)

## Criterios de Aceptacion

| # | Criterio |
|---|----------|
| AC-01 | El mapa carga centrado en Buenos Aires con tiles de OpenStreetMap |
| AC-02 | Todos los lugares se muestran como markers en el mapa |
| AC-03 | Cada marker tiene un color segun su categoria |
| AC-04 | Al hacer click en un marker se muestra un popup con nombre, categoria y link a detalle |
| AC-05 | La sidebar muestra las cards de lugares sincronizadas con el mapa |
| AC-06 | Filtrar por categoria actualiza los markers y la sidebar |
| AC-07 | Buscar por nombre filtra los markers y la sidebar |
| AC-08 | Al hacer click en una card de la sidebar, el mapa centra en ese lugar |
| AC-09 | La ficha de lugar muestra imagen (o placeholder), nombre, categoria, direccion, descripcion |
| AC-10 | La ficha de lugar muestra un mapa con el marker de ese lugar |
| AC-11 | El seeder carga 10 lugares de Buenos Aires al iniciar la app |
| AC-12 | El endpoint GET /api/places retorna todos los lugares en JSON |
| AC-13 | El endpoint GET /api/places?category=X filtra por categoria |
| AC-14 | GET /places retorna la vista con la lista de lugares |

## Escenarios de Test

### Tests Unitarios (`presentation/place/PlaceControllerTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| U-01 | `listPlaces()` sin filtros retorna todos los lugares | AC-14 |
| U-02 | `listPlaces()` con category retorna lugares filtrados | AC-06 |
| U-03 | `listPlaces()` con search retorna lugares filtrados | AC-07 |
| U-04 | `placeDetail()` con id valido retorna la vista con el lugar | AC-09 |
| U-05 | `placeDetail()` con id invalido retorna vista sin lugar | AC-09 |

### Tests Unitarios (`domain/place/PlaceServiceImplTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| U-06 | `getAllPlaces()` retorna todos los lugares | AC-02 |
| U-07 | `getPlacesByCategory()` retorna filtrados | AC-06 |
| U-08 | `searchPlaces()` retorna por nombre | AC-07 |
| U-09 | `getPlaceById()` con id existente retorna el lugar | AC-09 |
| U-10 | `getPlaceById()` con id inexistente retorna empty | AC-09 |

### Tests de Integracion (`integration/PlaceControllerTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| I-01 | `GET /places` retorna 200 y vista con lugares | AC-14 |
| I-02 | `GET /places?category=RESTAURANT` retorna 200 con filtrados | AC-06 |
| I-03 | `GET /places?search=Don` retorna 200 con filtrados | AC-07 |
| I-04 | `GET /places/{id}` con id valido retorna 200 | AC-09 |
| I-05 | `GET /places/{id}` con id inexistente retorna 200 sin lugar | AC-09 |

### Tests de Integracion (`integration/PlaceRestControllerTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| I-06 | `GET /api/places` retorna 200 y JSON array | AC-12 |
| I-07 | `GET /api/places?category=RESTAURANT` retorna filtrados | AC-13 |
| I-08 | `GET /api/places?search=Don` retorna filtrados | AC-13 |

### Tests de Integracion (`infrastructure/PlaceRepositoryTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| I-09 | `findAll()` retorna todos los lugares del seeder | AC-11 |
| I-10 | `findByCategory()` retorna solo los de esa categoria | AC-11 |
| I-11 | `findByNameContainingIgnoreCase()` retorna por nombre parcial | AC-11 |

### E2E (minimos)

| # | Test | Flujo | AC que cubre |
|---|------|-------|-------------|
| E-01 | `PlacesViewE2E` | Ir a /places, verificar mapa y markers, filtrar por categoria | AC-01, AC-02, AC-03, AC-06 |

## Referencia de Implementacion

> Los pasos a continuación son guía de implementación, no reemplazan los acceptance criteria de arriba.

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

### 7. Create Place REST endpoint

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

## Archivos a crear

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
| `infrastructure/PlaceDataSeeder.java` | Create |
| `templates/pages/places/list.html` | Create |
| `templates/pages/places/detail.html` | Create (with image handling + map) |
