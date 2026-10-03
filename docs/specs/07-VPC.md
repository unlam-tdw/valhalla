# [VPC] Vista Publica de Plan Compartido

> Trello: https://trello.com/c/hr1n4EwA/8-vpc-ver-plan-compartido-sin-cuenta
> **Estimación:** 5 pts Fibonacci
> **Note:** This is an SDD proposal. Implementation may change based on team decisions.

## Objetivo

Cualquier persona puede ver un plan compartido via URL, sin necesidad de login. Vista completa con mapa, itinerario numerado y ruta.

## Pre-requisitos

- [CMP] el modal de compartir produce la URL `/share/{shortCode}`. [VPC] **recibe de CMP**
  `ShareController` y `pages/share/view.html`: antes vivían en 06-CMP.md y los dos equipos los
  escribían en el mismo sprint sobre `integration/ShareControllerTest.java`.

## Criterios de Aceptacion

| # | Criterio |
|---|----------|
| AC-01 | /share/{shortCode} es accesible sin login |
| AC-02 | Si el plan es publico (`isPublic == true`), muestra nombre, descripcion, fecha, itinerario y mapa |
| AC-03 | Los markers del mapa estan numerados en orden de visita |
| AC-04 | El mapa dibuja una polyline de ruta entre los lugares |
| AC-05 | El itinerario muestra numero de orden, nombre, categoria, fecha y hora |
| AC-06 | Si el plan es privado, muestra error "This plan is private" |
| AC-07 | Si el shortCode es invalido, muestra error "Plan not found" |
| AC-08 | La ruta /share/** esta configurada como publica en SecurityConfig. **Ya esta**: es de [LOG] (T-LOG-012), verificado por los tests de seguridad de 08-AUT (S-03, S-04) |

## Escenarios de Test

### Tests de Integracion (`integration/ShareControllerIntegrationTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| I-01 | `GET /share/{shortCode}` con plan publico retorna 200 con plan | AC-01, AC-02 |
| I-02 | `GET /share/{shortCode}` con plan privado retorna 200 con error | AC-06 |
| I-03 | `GET /share/{shortCode}` con shortCode invalido retorna 200 con error | AC-07 |
| I-04 | `GET /share/{shortCode}` sin sesion no redirige a login | AC-01 |

### Tests de Seguridad (`integration/ShareSecurityTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| S-01 | `/share/test` es accesible sin autenticacion | AC-08 |
| S-02 | `/share/test` no retorna 302 a login | AC-08 |

### E2E (minimos)

| # | Test | Flujo | AC que cubre |
|---|------|-------|-------------|
| E-01 | `PublicShareE2E` | Crear un `BrowserContext` nuevo de Playwright (sin cookies), abrir el link, verificar plan y mapa | AC-01, AC-02, AC-03, AC-04 |

## Notas / decisiones de diseño

- **Alcance recibido de CMP**: `ShareController` y `pages/share/view.html` salen de 06-CMP.md
  y entran acá. Con la separación, CMP toca `plans/detail.html` y VPC crea `share/*`: los dos
  equipos dejan de escribir el mismo archivo de test en el mismo sprint.
- **"Ventana incógnito" no existe en el harness**. El `E2eBase` de Playwright reusa un solo
  `BrowserContext` con la cookie de sesión de admin: abrir `/share/...` ahí no prueba nada. E-01
  crea un `BrowserContext` nuevo con `browser.newContext()`, sin storage, navega a la URL
  compartida y recién ahí verifica el plan y el mapa. Es el equivalente programático de la
  ventana incógnito.
- **`/share/**` ya es `permitAll` desde Sprint 1**, en la cadena @Order(3) de `SecurityConfig`:
  `.requestMatchers("/", "/share/**", "/api/**", "/reload/**").permitAll()`. La vista pública
  **no necesita tocar `SecurityConfig`**. Solo verifica que nadie la rompa.
- **`Boolean isPublic`, no enum** (D4). La condición es `Boolean.TRUE.equals(plan.getIsPublic())`.
  El test de Integration es el que fija el criterio: plan con `isPublic == false` responde
  "This plan is private" con **200**, no con 403 ni 404 — el link es público y el404 confirmaría
  que ese shortCode existe.

## Referencia de Implementacion

> Los pasos a continuación son guía de implementación, no reemplazan los acceptance criteria de arriba.

### 1. ShareController (recibe de CMP)

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
            if (Boolean.TRUE.equals(plan.getIsPublic())) {
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

### 2. Share view template

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
    <div th:replace="~{components/navbar}"></div>

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
                                <p class="text-sm text-gray-500 mt-1">
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

### 3. SecurityConfig (verificar, no cambiar)

`/share/**` ya es `permitAll` en la cadena @Order(3) de `config/SecurityConfig.java` (creado en
[LOG], verificado por los tests de seguridad de 08-AUT). No hace falta configuración adicional:
la vista pública no escribe nada. Su `fetch` a `/api/plans/{id}/places` es lectura, y [APL-BE]
deja esa ruta disponible sin sesión **para planes públicos** (S-08 de 04-APL-BE.md) — es la
condición que hace que esta vista funcione sin login.

## Archivos a crear/modificar

| Archivo | Accion |
|---------|--------|
| `presentation/share/ShareController.java` | Crear (recibe de CMP; vista pública por shortCode) |
| `templates/pages/share/view.html` | Crear (plan público con itinerario y mapa) |
| `e2e/views/SharedPlanPage.java` | Crear (page object para `PublicShareE2E`) |
| `config/SecurityConfig.java` | Verificar que `/share/**` siga `permitAll` (sin cambios) |