# [APL-FE] Agregar Lugares al Plan (Frontend)

> Trello: https://trello.com/c/K7XqvGWZ/9-apl-fe-agregar-lugares-al-plan-frontend
> **Note:** This is an SDD proposal. Implementation may change based on team decisions.

## Objetivo

Frontend para agregar lugares a planes: boton "Add to plan" en el panel lateral de `/places`, itinerario con Vue.js, mapa con rutas y marcadores numerados en el detalle del plan.

## Pre-requisitos

- [APL-BE] completed (PlanPlace entity, REST endpoints, `userPlans` en el model de `GET /places`)
- [PLC] completed (panel lateral de detalles del lugar dentro de `/places`)

## Criterios de Aceptacion

| # | Criterio |
|---|----------|
| AC-01 | El panel lateral de `/places` muestra un dropdown con los planes del usuario logueado para el lugar seleccionado |
| AC-02 | Se puede agregar el lugar seleccionado a un plan desde el panel lateral, sin salir de `/places` |
| AC-03 | Al agregar un lugar se muestra feedback visual (redirect o mensaje) |
| AC-04 | El detalle del plan (/plans/{id}) muestra el itinerario con los lugares en orden |
| AC-05 | Cada lugar del itinerario muestra numero de orden, nombre, categoria, fecha y hora |
| AC-06 | Se puede editar fecha y hora de visita desde el itinerario |
| AC-07 | Se puede eliminar un lugar del itinerario con boton X |
| AC-08 | El mapa muestra markers numerados por cada lugar del itinerario |
| AC-09 | El mapa dibuja una linea de ruta entre los lugares en orden |
| AC-10 | El mapa hace fitBounds para mostrar todos los markers |
| AC-11 | Los forms usan CSRF token para proteger POST/PUT/DELETE |

## Escenarios de Test

### Tests Unitarios (Vue.js logic, si aplica)

| # | Test | AC que cubre |
|---|------|-------------|
| U-01 | El panel lateral de `/places` muestra el select con los planes del usuario y el boton "Add to plan" | AC-01, AC-02 |

### Tests de Integracion (`presentation/plan/PlanControllerTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| I-01 | `GET /plans/{id}` con sesion retorna 200 y vista con itinerary | AC-04 |
| I-02 | `GET /plans/{id}` sin sesion redirige a login | AC-04 |

### E2E (minimos)

| # | Test | Flujo | AC que cubre |
|---|------|-------|-------------|
| E-01 | `AddToPlanE2E` | Ir a /places, seleccionar un lugar, agregar a plan desde el panel, verificar en /plans/{id} | AC-01, AC-02, AC-04, AC-05 |

## Notas / decisiones de diseño

- **El "Add to plan" vive en el panel lateral, no en una ficha aparte** (D1). No existe
  `pages/places/detail.html` ni ruta `/places/{id}`: la ficha se rediseñó en Sprint 1 (PR #6) como
  `<aside aria-label="Details for ...">` dentro de `/places`. El panel ya tiene nombre, categoría,
  dirección, descripción e imagen del lugar, así que el dropdown de planes se agrega ahí y el
  usuario no pierde el mapa, el filtro aplicado ni la posición del scroll.
- **El panel ya existe en `places/list.html`** y su botón "Add to plan" hoy es un no-op
  (`PlacesViewE2E` solo verifica que no navegue y que el panel siga montado). Esta spec lo
  conecta al endpoint real de APL-BE.
- **CSRF (AC-11)**: APL-BE reduce el ignore list a las lecturas, así que el `POST` desde `/places`
  necesita el token. Un form de Thymeleaf lo trae con `th:name="${_csrf.parameterName}"`; si la
  llamada se hace con `fetch`, el token va en el header que indique `${_csrf.parameterName}`.

## Referencia de Implementacion

> Los pasos a continuación son guía de implementación, no reemplazan los acceptance criteria de arriba.

### 1. Update the places panel (add "Add to plan" form)

File: `src/main/webapp/WEB-INF/templates/pages/places/list.html`

Agregar dentro del `<aside>` de detalles, después del bloque de imagen/dirección/descripción. No
hay vista nueva que crear:

```html
<div sec:authorize="isAuthenticated()" class="mt-6" th:if="${userPlans}">
    <h3 class="font-medium mb-2">Add to plan</h3>
    <form th:action="@{/api/plans/{planId}/places(planId=${selectedPlace.id})}" method="post" class="flex gap-2">
        <input type="hidden" name="placeId" th:value="${selectedPlace.id}">
        <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">
        <select name="planId" class="px-3 py-2 border rounded focus:outline-none">
            <option value="">Select a plan...</option>
            <option th:each="plan : ${userPlans}" th:value="${plan.id}" th:text="${plan.name}"></option>
        </select>
        <button type="submit" class="bg-green-500 hover:bg-green-600 text-white px-4 py-2 rounded">
            Add
        </button>
    </form>
</div>
```

### 2. Update plan detail template with Vue.js

File: `src/main/webapp/WEB-INF/templates/pages/plans/detail.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title th:text="${plan?.name}">Plan Detail</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script src="https://unpkg.com/vue@3/dist/vue.global.js"></script>
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <meta name="_csrf" th:content="${_csrf.token}">
    <meta name="_csrf_header" th:content="${_csrf.parameterName}">
</head>
<body class="bg-gray-100 min-h-screen">
    <div th:replace="~{fragments/header :: header}"></div>

    <div id="app" class="container mx-auto px-4 py-8" th:if="${plan}">
        <div class="flex justify-between items-start mb-8">
            <div>
                <h1 class="text-3xl font-bold" th:text="${plan.name}"></h1>
                <span class="inline-block mt-2 px-2 py-0.5 text-xs text-white rounded"
                      th:classappend="${plan.visibility.name() == 'PUBLIC'} ? 'bg-green-500' : 'bg-gray-500'"
                      th:text="${plan.visibility}"></span>
            </div>
            <div class="flex gap-2">
                <a th:href="@{/places}" class="bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded">
                    Add Places
                </a>
                <button @click="sharePlan" class="bg-purple-500 hover:bg-purple-600 text-white px-4 py-2 rounded">
                    Share
                </button>
            </div>
        </div>

        <p class="text-gray-600 mb-4" th:text="${plan.description}"></p>
        <p class="text-gray-700 mb-8" th:if="${plan.visitDate}">
            <strong>Date:</strong> <span th:text="${plan.visitDate}"></span>
        </p>

        <div class="flex gap-8">
            <div class="flex-1">
                <div class="bg-white p-6 rounded-lg shadow">
                    <h2 class="text-xl font-bold mb-4">Itinerary</h2>

                    <div v-if="itinerary.length === 0" class="text-gray-500 text-center py-8">
                        No places added yet.
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
                                <div class="mt-1 flex gap-2">
                                    <input type="date" :value="item.visitDate"
                                           @change="updateDateTime(item.id, 'visitDate', $event.target.value)"
                                           class="px-2 py-1 border rounded text-sm">
                                    <input type="time" :value="item.visitTime"
                                           @change="updateDateTime(item.id, 'visitTime', $event.target.value)"
                                           class="px-2 py-1 border rounded text-sm">
                                </div>
                            </div>
                            <button @click="removePlace(item.id)"
                                    class="text-red-500 hover:text-red-700 font-bold">
                                X
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            <div class="flex-1">
                <div class="bg-white p-6 rounded-lg shadow">
                    <h2 class="text-xl font-bold mb-4">Route Map</h2>
                    <div id="plan-map" class="h-96 rounded-lg"></div>
                </div>
            </div>
        </div>
    </div>

    <script th:inline="javascript">
        const planId = [[${plan?.id}]];

        const { createApp, ref, onMounted } = Vue;

        createApp({
            setup() {
                const itinerary = ref([]);
                let map = null;
                let markers = [];
                let routeLine = null;

                function getCsrfToken() {
                    const meta = document.querySelector('meta[name="_csrf"]');
                    return meta ? meta.getAttribute('content') : '';
                }
                function getCsrfHeader() {
                    const meta = document.querySelector('meta[name="_csrf_header"]');
                    return meta ? meta.getAttribute('content') : 'X-CSRF-TOKEN';
                }

                onMounted(async () => {
                    await loadItinerary();
                    initMap();
                });

                async function loadItinerary() {
                    const response = await fetch('/api/plans/' + planId + '/places');
                    itinerary.value = await response.json();
                    updateMap();
                }

                function initMap() {
                    map = L.map('plan-map').setView([-34.6037, -58.3816], 13);
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        attribution: '&copy; OpenStreetMap contributors'
                    }).addTo(map);
                }

                function updateMap() {
                    markers.forEach(m => map.removeLayer(m));
                    markers = [];
                    if (routeLine) map.removeLayer(routeLine);

                    const coordinates = [];

                    itinerary.value.forEach((item, index) => {
                        if (item.place.latitude && item.place.longitude) {
                            coordinates.push([item.place.latitude, item.place.longitude]);

                            const marker = L.marker([item.place.latitude, item.place.longitude])
                                .addTo(map)
                                .bindPopup('<strong>' + (index + 1) + '. ' + item.place.name + '</strong>');
                            markers.push(marker);
                        }
                    });

                    if (coordinates.length > 1) {
                        routeLine = L.polyline(coordinates, {
                            color: '#3498db', weight: 3, dashArray: '10, 5'
                        }).addTo(map);
                    }

                    if (coordinates.length > 0) {
                        map.fitBounds(coordinates, { padding: [50, 50] });
                    }
                }

                async function removePlace(planPlaceId) {
                    if (confirm('Remove this place?')) {
                        await fetch('/api/plans/' + planId + '/places/' + planPlaceId, {
                            method: 'DELETE',
                            headers: { [getCsrfHeader()]: getCsrfToken() }
                        });
                        await loadItinerary();
                    }
                }

                async function updateDateTime(planPlaceId, field, value) {
                    await fetch('/api/plans/' + planId + '/places/' + planPlaceId, {
                        method: 'PUT',
                        headers: {
                            'Content-Type': 'application/json',
                            [getCsrfHeader()]: getCsrfToken()
                        },
                        body: JSON.stringify({ [field]: value })
                    });
                }

                function sharePlan() {
                    fetch('/plans/' + planId + '/share', {
                        method: 'POST',
                        headers: { [getCsrfHeader()]: getCsrfToken() }
                    })
                        .then(r => r.json())
                        .then(data => {
                            const url = window.location.origin + data.url;
                            prompt('Share this link:', url);
                        });
                }

                return { itinerary, removePlace, updateDateTime, sharePlan };
            }
        }).mount('#app');
    </script>
</body>
</html>
```

## Archivos a crear/modificar

| File | Action |
|------|--------|
| `templates/pages/places/list.html` | Update (add "Add to plan" form al panel lateral, con CSRF) |
| `templates/pages/plans/detail.html` | Update (add Vue.js itinerary with CSRF helpers) |
