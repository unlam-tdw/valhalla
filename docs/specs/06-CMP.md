# [CMP] Compartir Plan

> Trello: https://trello.com/c/vZuil82c/7-cmp-compartir-plan
> **Note:** This is an SDD proposal. Implementation may change based on team decisions.

## Objetivo

El usuario puede compartir su plan via URL unica con shortCode. Copiar link con feedback visual. Control de visibilidad publico/privado.

## Pre-requisitos

- [PLN] completed (Plan entity with shortCode)
- [APL-FE] completed (itinerary working)

## Criterios de Aceptacion

| # | Criterio |
|---|----------|
| AC-01 | Al hacer click en "Share" se genera una URL con shortCode del plan |
| AC-02 | La URL tiene formato /share/{shortCode} |
| AC-03 | Se puede copiar el link al portapapeles con feedback "Copied!" |
| AC-04 | Se puede cambiar la visibilidad del plan (PUBLIC/PRIVATE) |
| AC-05 | Si el plan es PRIVATE y alguien abre el link, se muestra error |
| AC-06 | Si el plan no tiene shortCode, se genera automaticamente al compartir |

## Escenarios de Test

### Tests Unitarios (`presentation/plan/PlanControllerTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| U-01 | `sharePlan()` genera shortCode si es null y retorna URL | AC-01, AC-02, AC-06 |
| U-02 | `sharePlan()` con shortCode existente retorna URL | AC-01, AC-02 |
| U-03 | `updateVisibility()` cambia a PUBLIC | AC-04 |
| U-04 | `updateVisibility()` cambia a PRIVATE | AC-04 |

### Tests Unitarios (`domain/plan/PlanServiceImplTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| U-05 | `generateShortCode()` retorna string de 8 caracteres | AC-02 |

### Tests de Integracion (`integration/ShareControllerTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| I-01 | `GET /share/{shortCode}` con plan PUBLIC retorna 200 | AC-05 |
| I-02 | `GET /share/{shortCode}` con plan PRIVATE retorna 200 con error de acceso | AC-05 |
| I-03 | `GET /share/{shortCode}` con shortCode invalido retorna 200 sin plan | AC-05 |

### Tests de Integracion (`integration/PlanControllerTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| I-04 | `POST /plans/{id}/share` retorna JSON con URL | AC-01 |
| I-05 | `POST /plans/{id}/visibility?visibility=PUBLIC` retorna JSON con visibility | AC-04 |

### E2E (minimos)

| # | Test | Flujo | AC que cubre |
|---|------|-------|-------------|
| E-01 | `SharePlanE2E` | Compartir plan, copiar link, abrir en otra ventana | AC-01, AC-03 |

## Referencia de Implementacion

> Los pasos a continuación son guía de implementación, no reemplazan los acceptance criteria de arriba.

### 1. Verify Plan.shortCode

The Plan entity already has `shortCode` (String, unique) created in [PLN].

### 2. Verify PlanService.generateShortCode()

Already exists in [PLN]. Generates 8-character UUID.

### 3. Update PlanController (add share endpoints)

File: `src/main/java/com/valhalla/presentation/plan/PlanController.java`

Add these methods:

```java
@PostMapping("/{id}/share")
@ResponseBody
public Map<String, String> sharePlan(@PathVariable Long id) {
    Plan plan = planService.getPlanById(id)
        .orElseThrow(() -> new RuntimeException("Plan not found"));

    if (plan.getShortCode() == null) {
        plan.setShortCode(planService.generateShortCode());
        planService.updatePlan(plan);
    }

    String shareUrl = "/share/" + plan.getShortCode();
    return Map.of("url", shareUrl);
}

@PostMapping("/{id}/visibility")
@ResponseBody
public Map<String, String> updateVisibility(
    @PathVariable Long id,
    @RequestParam String visibility
) {
    Plan plan = planService.getPlanById(id)
        .orElseThrow(() -> new RuntimeException("Plan not found"));
    plan.setVisibility(Plan.Visibility.valueOf(visibility));
    planService.updatePlan(plan);
    return Map.of("visibility", visibility);
}
```

Add required imports:
```java
import org.springframework.web.bind.annotation.ResponseBody;
```

### 4. Create ShareController (public view)

File: `src/main/java/com/valhalla/presentation/share/ShareController.java`

```java
package com.valhalla.presentation.share;

import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanService;
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

    @Autowired
    public ShareController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping("/{shortCode}")
    public ModelAndView sharedPlan(@PathVariable String shortCode) {
        Map<String, Object> model = new ModelMap();
        planService.getPlanByShortCode(shortCode).ifPresent(plan -> {
            if (plan.getVisibility() == Plan.Visibility.PUBLIC) {
                model.put("plan", plan);
            } else {
                model.put("error", "This plan is private");
            }
        });
        if (!model.containsKey("plan") && !model.containsKey("error")) {
            model.put("error", "Plan not found");
        }
        return new ModelAndView("pages/share/view", model);
    }
}
```

### 5. Create share view template

File: `src/main/webapp/WEB-INF/templates/pages/share/view.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title th:text="${plan?.name ?: 'Shared Plan'}">Shared Plan</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script src="https://unpkg.com/vue@3/dist/vue.global.js"></script>
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
</head>
<body class="bg-gray-100 min-h-screen">
    <nav class="bg-white shadow p-4">
        <div class="container mx-auto">
            <h1 class="text-xl font-bold text-blue-600">PlanIt</h1>
        </div>
    </nav>

    <div id="app" class="container mx-auto px-4 py-8" th:if="${plan}">
        <h1 class="text-3xl font-bold mb-2" th:text="${plan.name}"></h1>
        <p class="text-gray-600 mb-4" th:text="${plan.description}"></p>
        <p class="text-gray-700 mb-8" th:if="${plan.visitDate}">
            <strong>Date:</strong> <span th:text="${plan.visitDate}"></span>
        </p>

        <div class="flex gap-8">
            <div class="flex-1">
                <div class="bg-white p-6 rounded-lg shadow">
                    <h2 class="text-xl font-bold mb-4">Itinerary</h2>
                    <div v-if="itinerary.length === 0" class="text-gray-500 text-center py-8">
                        No places in this plan.
                    </div>
                    <div v-else class="space-y-3">
                        <div v-for="(item, index) in itinerary" :key="item.id"
                             class="flex items-center gap-4 p-3 border rounded-lg">
                            <div class="w-8 h-8 bg-blue-500 text-white rounded-full flex items-center justify-center font-bold">
                                {{ index + 1 }}
                            </div>
                            <div class="flex-1">
                                <strong>{{ item.place.name }}</strong>
                                <span class="ml-2 px-2 py-0.5 text-xs text-white bg-blue-500 rounded">
                                    {{ item.place.category }}
                                </span>
                                <p class="text-sm text-gray-500 mt-1" th:if="${plan.visitDate}">
                                    {{ item.visitDate }} {{ item.visitTime }}
                                </p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="flex-1">
                <div class="bg-white p-6 rounded-lg shadow">
                    <h2 class="text-xl font-bold mb-4">Route Map</h2>
                    <div id="share-map" class="h-96 rounded-lg"></div>
                </div>
            </div>
        </div>
    </div>

    <div th:if="${error}" class="container mx-auto px-4 py-16 text-center">
        <h1 class="text-3xl font-bold text-red-500 mb-4" th:text="${error}"></h1>
        <a href="/" class="bg-blue-500 hover:bg-blue-600 text-white px-6 py-3 rounded inline-block">
            Go to PlanIt
        </a>
    </div>

    <script th:inline="javascript">
        const planId = [[${plan?.id}]];
        if (planId) {
            const { createApp, ref, onMounted } = Vue;
            createApp({
                setup() {
                    const itinerary = ref([]);
                    let map = null;

                    onMounted(async () => {
                        const response = await fetch('/api/plans/' + planId + '/places');
                        itinerary.value = await response.json();
                        initMap();
                    });

                    function initMap() {
                        map = L.map('share-map').setView([-34.6037, -58.3816], 13);
                        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                            attribution: '&copy; OpenStreetMap contributors'
                        }).addTo(map);

                        const coordinates = [];
                        itinerary.value.forEach((item, index) => {
                            if (item.place.latitude && item.place.longitude) {
                                coordinates.push([item.place.latitude, item.place.longitude]);
                                L.marker([item.place.latitude, item.place.longitude])
                                    .addTo(map)
                                    .bindPopup('<strong>' + (index + 1) + '. ' + item.place.name + '</strong>');
                            }
                        });

                        if (coordinates.length > 1) {
                            L.polyline(coordinates, { color: '#3498db', weight: 3, dashArray: '10, 5' }).addTo(map);
                        }
                        if (coordinates.length > 0) {
                            map.fitBounds(coordinates, { padding: [50, 50] });
                        }
                    }

                    return { itinerary };
                }
            }).mount('#app');
        }
    </script>
</body>
</html>
```

### 6. Update plan detail template (add share modal)

File: `src/main/webapp/WEB-INF/templates/pages/plans/detail.html`

Add within the Vue app div (after the main content):

```html
<!-- Share Modal -->
<div v-if="showShareModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
    <div class="bg-white p-6 rounded-lg shadow-lg max-w-md w-full mx-4">
        <h3 class="text-xl font-bold mb-4">Share Plan</h3>
        <p class="text-gray-600 mb-4">Copy this link to share your plan:</p>
        <div class="flex gap-2 mb-4">
            <input type="text" :value="shareUrl" readonly
                   class="flex-1 px-3 py-2 border rounded focus:outline-none">
            <button @click="copyShareLink"
                    class="bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded">
                Copy
            </button>
        </div>
        <p v-if="copied" class="text-green-500 text-sm mb-4">Copied!</p>
        <button @click="showShareModal = false"
                class="w-full bg-gray-500 hover:bg-gray-600 text-white px-4 py-2 rounded">
            Close
        </button>
    </div>
</div>
```

Add to the Vue setup:

```javascript
const showShareModal = ref(false);
const shareUrl = ref('');
const copied = ref(false);

async function sharePlan() {
    const response = await fetch('/plans/' + planId + '/share', {
        method: 'POST',
        headers: { [getCsrfHeader()]: getCsrfToken() }
    });
    const data = await response.json();
    shareUrl.value = window.location.origin + data.url;
    showShareModal.value = true;
    copied.value = false;
}

function copyShareLink() {
    navigator.clipboard.writeText(shareUrl.value);
    copied.value = true;
    setTimeout(() => { copied.value = false; }, 2000);
}
```

## Archivos a crear/modificar

| File | Action |
|------|--------|
| `presentation/plan/PlanController.java` | Update (add share + visibility endpoints) |
| `presentation/share/ShareController.java` | Create (public view by shortCode) |
| `templates/pages/share/view.html` | Create (public plan view with map) |
| `templates/pages/plans/detail.html` | Update (add share modal) |
