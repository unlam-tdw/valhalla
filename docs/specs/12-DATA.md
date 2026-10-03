# [DATA] Contenido de lugares

> Trello: (pendiente — se crea en el paso 3)
> **Estimación:** 3 pts Fibonacci
> **Note:** This is an SDD proposal. Implementation may change based on team decisions.

## Objetivo

Que `/places` tenga suficiente contenido para que el mapa sea creíble. Hoy hay 10 lugares de
Buenos Aires y **los 10 muestran el mismo `place-placeholder.svg`**: `PlaceDataSeeder` nunca setea
`imageUrl`, así que el campo queda en null y la vista cae al placeholder. Peor: el E2E codifica ese
placeholder como comportamiento esperado, así que agregar contenido no es cambiar un número sino
recalibrar un archivo de test completo.

## Pre-requisitos

- [AUT] completed (el E2E de places necesita sesión de admin)
- [BRD] completado (los íconos/imágenes de categoría usan los tokens `--category-*`; si los colores
  de categoría se movieron a tokens, esta spec no debe introducir hex nuevos)
- `PlaceDataSeeder` es el seeder: ya siembra solo si `placeRepository.count() == 0`.
- `places/list.html` y `PlacesViewE2E` fijan el balance de categorías, ver AC-05.

## Criterios de Aceptacion

| # | Criterio |
|---|----------|
| AC-01 | El dataset pasa de 10 a al menos 25 lugares de Buenos Aires, repartidos entre las categorías existentes |
| AC-02 | Todo lugar nuevo tiene `imageUrl` o cae al placeholder. El placeholder queda como **default**, no como excepción única |
| AC-03 | Un lugar con `imageUrl` muestra su imagen en el panel lateral; uno sin imagen muestra `/images/place-placeholder.svg`. Ambos casos tienen test |
| AC-04 | Cada categoría de `PlaceCategory` tiene al menos un lugar en el dataset (hoy `SPORT` y `OTHER` no tienen ninguno) |
| AC-05 | **Recalibrar `PlacesViewE2E` completo.** El archivo fija el dataset en 5 puntos y no se trata de cambiar un `10`: hay que decidir el nuevo conteo y el nuevo balance de categorías |
| AC-06 | El filtro por categoría sigue funcionando: elegir una categoría deja exactamente los lugares de esa categoría |
| AC-07 | La búsqueda por nombre sigue funcionando sobre el dataset nuevo |

## Escenarios de Test

### Tests Unitarios (`infrastructure/PlaceDataSeederTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| U-01 | El seeder no corre si ya hay lugares | AC-01 |
| U-02 | El seeder deja al menos 25 lugares | AC-01 |
| U-03 | Cada categoría de `PlaceCategory` tiene al menos un lugar | AC-04 |
| U-04 | Los lugares con imagen tienen `imageUrl`; los demás, `null` | AC-02 |

### Tests de Integracion (`integration/PlaceRestControllerTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| I-01 | `GET /api/places` retorna el dataset completo | AC-01 |
| I-02 | `GET /api/places?category=SPORT` retorna al menos 1 (hoy 0) | AC-04 |
| I-03 | `GET /api/places?category=OTHER` retorna al menos 1 (hoy 0) | AC-04 |
| I-04 | `GET /api/places?search=...` filtra por nombre sobre el dataset nuevo | AC-07 |
| I-05 | `GET /api/places` incluye `imageUrl` en algunos registros y `null` en otros | AC-02 |

### Tests de Integracion (`integration/PlaceImageTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| I-06 | Un lugar con `imageUrl` renderiza esa URL en el panel lateral | AC-03 |
| I-07 | Un lugar sin `imageUrl` renderiza `/images/place-placeholder.svg` | AC-02, AC-03 |

### E2E (minimos)

| # | Test | Flujo | AC que cubre |
|---|------|-------|-------------|
| E-01 | `PlacesViewE2E` recalibrado | Verificar mapa, cards y balance de categorías contra el nuevo dataset | AC-01, AC-05, AC-06 |
| E-02 | `PlacesViewE2E` | Filtrar por categoría y por nombre sobre el dataset nuevo | AC-06, AC-07 |

## Notas / decisiones de diseño

- **`PlacesViewE2E` fija el dataset en 5 puntos.** No alcanza con cambiar el `10`:

  | Punto | Aserción actual |
  |-------|-----------------|
  | `signInAndOpenPlaces` (setup) | `waitForPlaceCount(10)` |
  | `shouldRenderSeededPlacesAndKeepMarkersInSyncWithFilters` | `markers().count() == 10` |
  | el mismo test | `placeCards().count() == 10` |
  | filtro por categoría | `#map .leaflet-marker-icon div[style*='background-color:#e74c3c']` == **2** (`#e74c3c` es el color de `RESTAURANT`, y hay exactamente 2 restaurantes en el seed) |
  | filtro por categoría / búsqueda | `waitForPlaceCount(2)` → `waitForPlaceCount(1)` |
  | fin del test | `waitForPlaceCount(10)` |

  `waitForPlaceCount(n)` es un helper: una sola constante nueva lo resuelve para los tres
  `waitForPlaceCount(10)`, pero `markers() == 10` y `placeCards() == 10` están escritos a mano, y
  los conteos del filtro por categoría dependen de **cuántos lugares hay por categoría**, no del
  total. Hay que decidir el nuevo balance antes de tocar el archivo.
- **El assert del color de categoría es el más frágil.** El selector
  `div[style*='background-color:#e74c3c']` ata el test al hex en el `style` inline que escribe
  Leaflet. Si [BRD] mueve los colores a tokens CSS, ese selector tiene que cambiar con él: mejor
  buscar el marcador por su categoría (`div[data-category='BAR']` o el `title` del marker) que por
  su color. Es el cambio que evita que BRD y DATA rompan el mismo archivo en el mismo sprint.
- **`imageUrl` no existe en los datos actuales.** `Place.imageUrl` está declarado y con getter y
  setter, pero `PlaceDataSeeder` no lo setea nunca: los 10 lugares devuelven `null` y la vista cae
  al placeholder. Agregar imágenes reales requiere decidir el origen (assets versionados vs URL
  externa) — si son assets, van en `resources/images/` y hay que garantizar que `/images/**`
  siga `permitAll`.
- **`SPORT` y `OTHER` están en el enum y en `categoryColors` pero no tienen datos.** Un filtro por
  esas categorías devuelve cero lugares, que es indistinguible de un filtro roto. Con AC-04 cada
  categoría tiene al menos un lugar y el filtro se puede probar de punta a punta.
- **Cuidado con el `count() == 0` del seeder.** `PlaceDataSeeder` solo corre si
  `placeRepository.count() == 0`. En una base ya sembrada, cambiar el seeder no cambia nada hasta
  que se resetee la tabla; el E2E resetea con `ResetDatabase`, que **no** borra `places` hoy.
  Hay que decidir si el reset de la E2E incluye `DELETE FROM places`.
- **Sin clustering.** [CONTEXT.md] ya no define `Cluster` (D7): con 25–30 lugares en Buenos Aires
  los pines nunca se agrupan, así que no hay que agregar el plugin.

## Referencia de Implementacion

> Los pasos a continuación son guía de implementación, no reemplazan los acceptance criteria de arriba.

### 1. Ampliar el dataset en `PlaceDataSeeder`

File: `src/main/java/com/valhalla/infrastructure/PlaceDataSeeder.java`

El seeder ya tiene la lista `PLACES` y `savePlace(...)`. Se agregan los lugares nuevos con sus
coordenadas reales, cubriendo las 10 categorías de `PlaceCategory` yemate al menos 2 en las que
hoy hay 1.

`savePlace(...)` tiene que aceptar la imagen como parámetro opcional, para que el placeholder
quede como default en vez de ser el único resultado:

```java
private void savePlace(PlaceData data) {
    Place place = new Place();
    place.setName(data.name());
    place.setDescription(data.description());
    place.setCategory(data.category());
    place.setAddress(data.address());
    place.setLatitude(data.latitude());
    place.setLongitude(data.longitude());
    place.setImageUrl(data.imageUrl());   // null cuando el lugar no tiene imagen
    placeRepository.save(place);
}
```

La vista resuelve `imageUrl` null con `/images/place-placeholder.svg`, que es exactamente lo que
ya hace hoy el panel lateral.

### 2. Recalibrar `PlacesViewE2E`

File: `src/test/java/com/valhalla/e2e/PlacesViewE2E.java`

Antes de editar, definir el balance nuevo y dejarlo escrito. La recalibración toca:

| Qué | Dónde | Acción |
|-----|-------|--------|
| Total de lugares | `waitForPlaceCount(10)` ×2, `markers().count() == 10`, `placeCards().count() == 10` | Una sola constante (el helper ya parametrizado) + los dos asserts literales |
| Balance por categoría | filtro `RESTAURANT` → `waitForPlaceCount(2)`; búsqueda `"Don"` → `waitForPlaceCount(1)` | Recalcular contra el dataset nuevo |
| Selector por color | `div[style*='background-color:#e74c3c']` == 2 | Dejar de atar el test al hex: identificar el marker por su categoría |
| Placeholder | `assertThat(panel.locator("img").getAttribute("src"), equalTo("/images/place-placeholder.svg"))` | Dejar de codificar el placeholder como resultado **único**: pasar a "algún lugar con imagen real" + "un lugar sin imagen cae al placeholder" |

Recomendar `data-category` en el marker: hace el assert legible y sobrevive a [BRD].

### 3. `ResetDatabase`: decidir si `places` entra en el reset

File: `src/test/java/com/valhalla/e2e/ResetDatabase.java`

Hoy el reset solo limpia `plans` y `users`. Si `places` no se borra, el dataset nuevo no llega a
la base de E2E: el seeder ve `count() > 0` y no corre. Agregar `"DELETE FROM places"` al principio
del array, más `"ALTER SEQUENCE places_id_seq RESTART WITH 1"`, **antes** de `DELETE FROM plans`
por el orden de FK.

## Archivos a crear/modificar

| Archivo | Accion |
|---------|--------|
| `infrastructure/PlaceDataSeeder.java` | Actualizar (≥25 lugares, cobertura de las 10 categorías, `imageUrl` opcional) |
| `templates/pages/places/list.html` | Actualizar (placeholder como default; `data-category` en el marker) |
| `e2e/PlacesViewE2E.java` | Actualizar (recalibración completa: total, balance, selector, placeholder) |
| `e2e/ResetDatabase.java` | Actualizar (`DELETE FROM places` antes de `plans` + `ALTER SEQUENCE`) |
| `resources/images/**` | Crear/Actualizar (imágenes de los lugares nuevos, si la opción es asset local) |