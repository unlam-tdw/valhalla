# [VPC] View Shared Plan Without Account

> Trello: https://trello.com/c/hr1n4EwA/8-vpc-ver-plan-compartido-sin-cuenta
> **Note:** This is an SDD proposal. Implementation may change based on team decisions.

## Objective

Anyone can view a shared plan via URL, without login. Map with complete itinerary.

## Prerequisites

- [CMP] completed (shortCode and share working)

## Steps

### 1. Create ShareController

File: `src/main/java/com/valhalla/presentation/share/ShareController.java`

```java
package com.valhalla.presentation.share;

import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanService;
import com.valhalla.domain.planplace.PlanPlace;
import com.valhalla.domain.planplace.PlanPlaceService;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/share")
public class ShareController {

    private final PlanService planService;
    private final PlanPlaceService planPlaceService;

    @Autowired
    public ShareController(PlanService planService, PlanPlaceService planPlaceService) {
        this.planService = planService;
        this.planPlaceService = planPlaceService;
    }

    @GetMapping("/{shortCode}")
    public ModelAndView viewSharedPlan(@PathVariable String shortCode) {
        Map<String, Object> model = new ModelMap();

        Plan plan = planService.getPlanByShortCode(shortCode).orElse(null);

        if (plan == null) {
            model.put("error", "Plan not found");
            return new ModelAndView("pages/share/view", model);
        }

        if (plan.getVisibility() == Plan.Visibility.PRIVATE) {
            model.put("error", "This plan is private");
            return new ModelAndView("pages/share/view", model);
        }

        List<PlanPlace> itinerary = planPlaceService.getItinerary(plan.getId());

        model.put("plan", plan);
        model.put("itinerary", itinerary);

        return new ModelAndView("pages/share/view", model);
    }
}
```

### 2. Create public view template

File: `src/main/webapp/WEB-INF/templates/pages/share/view.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title th:text="${plan != null ? plan.name : 'Plan not found'}">Shared Plan</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
</head>
<body class="bg-gray-100 min-h-screen">
    <!-- Error state -->
    <div th:if="${error}" class="flex items-center justify-center h-screen">
        <div class="text-center">
            <h1 class="text-2xl font-bold text-red-500 mb-4" th:text="${error}"></h1>
            <p class="text-gray-600 mb-4">The link may be expired or the plan may have been deleted.</p>
            <a href="/" class="text-blue-500 hover:underline">Go to PlanIt</a>
        </div>
    </div>

    <!-- Success state -->
    <div th:if="${plan}" class="flex h-screen">
        <aside class="w-96 overflow-y-auto bg-white border-r p-6">
            <div class="mb-6">
                <h1 class="text-2xl font-bold" th:text="${plan.name}"></h1>
                <span class="inline-block mt-2 px-2 py-0.5 text-xs text-white bg-green-500 rounded">
                    PUBLIC
                </span>
            </div>

            <p class="text-gray-600 mb-4" th:text="${plan.description}" th:if="${plan.description}"></p>
            <p class="text-gray-700 font-medium mb-6" th:if="${plan.visitDate}">
                Date: <span th:text="${plan.visitDate}"></span>
            </p>

            <h2 class="text-lg font-bold mb-4">Itinerary</h2>
            <ul class="space-y-3">
                <li th:each="planPlace, iter : ${itinerary}" class="flex gap-3 p-3 border rounded-lg bg-gray-50">
                    <div class="w-8 h-8 bg-blue-500 text-white rounded-full flex items-center justify-center font-bold flex-shrink-0">
                        <span th:text="${iter.index + 1}"></span>
                    </div>
                    <div class="flex-1">
                        <strong th:text="${planPlace.place.name}"></strong>
                        <span class="ml-2 px-2 py-0.5 text-xs text-white bg-blue-500 rounded"
                              th:text="${planPlace.place.category}"></span>
                        <p th:text="${planPlace.place.description}" th:if="${planPlace.place.description}"
                           class="text-sm text-gray-600 mt-1"></p>
                        <div class="text-sm text-gray-500 mt-1" th:if="${planPlace.visitDate || planPlace.visitTime}">
                            <span th:if="${planPlace.visitDate}" th:text="${planPlace.visitDate}"></span>
                            <span th:if="${planPlace.visitTime}" th:text="${planPlace.visitTime}"></span>
                        </div>
                    </div>
                </li>
            </ul>

            <div class="mt-8 pt-4 border-t text-sm text-gray-500">
                <p>Created with <a href="/" class="text-blue-500 hover:underline">PlanIt</a></p>
            </div>
        </aside>

        <div id="share-map" class="flex-1"></div>
    </div>

    <script th:inline="javascript">
        const itinerary = [[${itinerary}]];

        if (itinerary && itinerary.length > 0) {
            const map = L.map('share-map').setView([-34.6037, -58.3816], 13);
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; OpenStreetMap contributors'
            }).addTo(map);

            const coordinates = [];

            itinerary.forEach(function(item, index) {
                const place = item.place;
                if (place && place.latitude && place.longitude) {
                    coordinates.push([place.latitude, place.longitude]);

                    const icon = L.divIcon({
                        className: 'bg-red-500 text-white rounded-full w-8 h-8 flex items-center justify-center font-bold border-2 border-white shadow-md',
                        html: (index + 1).toString(),
                        iconSize: [32, 32],
                        iconAnchor: [16, 16]
                    });

                    L.marker([place.latitude, place.longitude], { icon: icon }).addTo(map)
                        .bindPopup(
                            '<strong>' + (index + 1) + '. ' + place.name + '</strong><br>' +
                            place.category + '<br>' +
                            (place.description || '')
                        );
                }
            });

            if (coordinates.length > 1) {
                L.polyline(coordinates, {
                    color: '#3498db', weight: 3, opacity: 0.7, dashArray: '10, 5'
                }).addTo(map);
            }

            if (coordinates.length > 0) {
                map.fitBounds(coordinates, { padding: [50, 50] });
            }
        }
    </script>
</body>
</html>
```

### 3. Verify SecurityConfig (public route)

The `/share/**` route is already configured as public in SecurityConfig (created in [LOG]):

```java
.requestMatchers("/share/**").permitAll()
```

No additional configuration needed.

## Verification

1. Create plan, add places, set to PUBLIC
2. Copy share link
3. Open in incognito window (no login)
4. Plan shows completely
5. Map shows numbered markers and route
6. If plan is PRIVATE → error "This plan is private"
7. If shortCode invalid → error "Plan not found"
8. Without session → no redirect to login

## Files to create

| File | Action |
|------|--------|
| `presentation/share/ShareController.java` | Create |
| `templates/pages/share/view.html` | Create |
