# [SAVES] Contador de guardados en la card pública

> Trello: tarjeta "Contador de guardados en la card pública"
> **Estimación:** 2 pts Fibonacci
> **Note:** This is an SDD proposal. Implementation may change based on team decisions.

## Objetivo

Que quien recorre el feed pueda ver, en cada card, cuántas personas guardaron ese plan, para
decidir cuáles vale la pena abrir sin entrar al detalle. La cifra es global (todos los usuarios),
no el estado del visitante: el icono relleno o de contorno sigue diciendo si *él* lo guardó.

El contador acompaña al icono de Guardados de las cards de listado. Con cero guardados no se
renderiza la cifra: queda solo el icono, sin placeholders ni ceros.

## Pre-requisitos

- [MARK] completo: `PlanBookmark`, `PlanBookmarkService`, el toggle de Guardados en las cards del
  feed y del perfil público, y la regla AC-05 (sin sesión no se renderiza el toggle).
- [PPV] completo: el feed `/for-you` y el perfil público comparten `PlanFeedCards.planCard`.
- [PAGE] completo: el feed pagina y el endpoint parcial reutiliza el mismo fragmento de cards.

## Criterios de Aceptación

| # | Criterio |
|---|----------|
| AC-01 | `PlanBookmarkService.getBookmarkCounts(List<UUID> planIds)` devuelve un mapa `planId → cantidad` con **una sola** consulta agrupada. Los planes sin guardados no aparecen en el mapa; una lista nula o vacía devuelve `Map.of()` sin consultar la base |
| AC-02 | `PlanFeedCards.planCard` recibe `bookmarkCount` y lo expone como clave `bookmarkCount` del modelo Thymeleaf. El feed `/for-you` y el perfil público `/profile/{id}` pasan el conteo real de cada plan; la tarjeta JSON del sidebar de `/explore` no cambia su contrato |
| AC-03 | En las cards de listado, la cifra se muestra junto al icono de Guardados cuando es mayor que cero. Con sesión va dentro del toggle (que conserva su estado relleno/contorno y su comportamiento); sin sesión se pinta como dato de la card junto a un icono de Guardados no interactivo, sin montar el toggle (MARK AC-05) |
| AC-04 | Con cero guardados no se renderiza la cifra: con sesión queda solo el icono del toggle; sin sesión no hay cifra ni icono, sin `0`, sin badge vacío y sin espacio reservado |
| AC-05 | La cifra no se renderiza en el detalle del plan (`/plans/{id}`) ni en la vista pública (`/plans/{id}/public`): vive solo en las cards de listado |
| AC-06 | No hay N+1: el feed y el perfil público resuelven todos los conteos de la página en una sola llamada a `getBookmarkCounts`, y cada card toma su valor del mapa |
| AC-07 | El agregado no expone quién guardó el plan, solo la cantidad. No se agregan ids, emails ni nombres de los usuarios que guardaron |
| AC-08 | `pwsh -File scripts/gate.ps1 all` en verde |

## Decisiones / aclaraciones

- **La cifra es global, el icono es personal.** `bookmarkCount` cuenta filas de
  `plan_bookmarks` para el plan; `bookmarked` sigue siendo el estado del usuario de la sesión. Son
  datos distintos y no se mezclan: el contador no cambia el icono ni el `aria-pressed`.
- **Un solo agregado por página.** Se pide `getBookmarkCounts(pagePlanIds)` después de recortar la
  ventana visible, igual que los summaries de calificaciones. La consulta es
  `group by plan_id` y solo devuelve los planes con al menos un guardado; el servicio completa el
  cero en presentación (`getOrDefault`), sin una segunda query.
- **La cifra es pública en las dos sesiones.** Con sesión se suma al toggle (`bookmark-button`)
  como texto del mismo botón: es el lugar donde ya está el icono de Guardados. Sin sesión el toggle
  no se monta (MARK AC-05), así que el mismo dato se pinta en la metadata de la card con un icono de
  Guardados no interactivo. El anónimo ve la cifra, pero no un control que rebote al login.
- **El botón crece solo cuando hay cifra.** La base pasa de `w-9` a `min-w-9 px-2`: con cero el
  contenido mide lo mismo que antes (círculo de 36 px); con cifra se vuelve una píldora sin tocar
  el resto del layout.
- **`/plans` en la tarjeta original es la ruta vieja del feed.** El listado privado `/plans` es una
  tabla de planes propios y de participación, sin cards públicas ni icono de Guardados; el feed
  público vive hoy en `/for-you`. El agregado compartido se aplica a las dos superficies que
  muestran la card con icono: `/for-you` y `/profile/{id}`.
- **Sin JavaScript la cifra también está.** El número se renderiza server-side; el toggle optimista
  cambia el icono en el lugar y la cifra se actualiza al recargar, que es el alcance de esta
  tarjeta. No se inventa un delta local en el JS.
- **El detalle no recibe el conteo.** Las plantillas de detalle y vista pública llaman al fragmento
  con `0`, así que el contador nunca aparece ahí aunque el plan tenga guardados.

## Escenarios de Test

### Tests Unitarios (`infrastructure/bookmark/PlanBookmarkServiceImplTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| U-SAVES-01 | `getBookmarkCounts` agrupa por plan y omite los planes sin guardados | AC-01, AC-06 |
| U-SAVES-02 | `getBookmarkCounts` con lista nula o vacía devuelve un mapa vacío y no consulta el repositorio | AC-01 |

### Tests de Integración (`integration/BookmarkControllerIntegrationTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| I-SAVES-01 | El feed muestra la cifra cuando otro usuario guardó el plan | AC-02, AC-03, AC-07 |
| I-SAVES-02 | El feed no renderiza la cifra cuando el plan no tiene guardados | AC-04 |
| I-SAVES-03 | El perfil público muestra la cifra de sus planes públicos | AC-02, AC-03 |
| I-SAVES-04 | El detalle y la vista pública no renderizan la cifra | AC-05 |
| I-SAVES-05 | Sin sesión, el feed muestra la cifra como dato de la card y no monta el toggle | AC-03, AC-04 |
| I-SAVES-06 | Sin sesión, el perfil público muestra la cifra y no monta el toggle | AC-03, AC-04 |

### E2E (`e2e/BookmarkE2E.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| E-SAVES-01 | Guardar desde la card y recargar muestra la cifra `1`; quitarlo y recargar la oculta | AC-03, AC-04 |

## Archivos a crear/modificar

| Archivo | Acción |
|---------|--------|
| `domain/bookmark/PlanBookmarkService.java` | Actualizar (nuevo `getBookmarkCounts`) |
| `domain/bookmark/PlanBookmarkRepository.java` | Actualizar (conteo agrupado por plan) |
| `infrastructure/bookmark/JpaPlanBookmarkRepository.java` | Actualizar (query `group by` + proyección) |
| `infrastructure/bookmark/PlanBookmarkRepositoryImpl.java` | Actualizar (mapa `planId → total`) |
| `infrastructure/bookmark/PlanBookmarkServiceImpl.java` | Actualizar (servicio, casos vacíos) |
| `presentation/plan/PlanFeedCards.java` | Actualizar (`bookmarkCount` en `planCard`) |
| `presentation/plan/ForYouController.java` | Actualizar (un agregado para la página) |
| `presentation/profile/ProfileController.java` | Actualizar (un agregado para el perfil público) |
| `templates/components/bookmark-button.html` | Actualizar (cifra en el toggle) |
| `templates/components/feed-cards.html` | Actualizar (pasar el conteo) |
| `templates/pages/profile/view.html` | Actualizar (pasar el conteo) |
| `templates/pages/plans/detail.html` | Actualizar (fragmento con `0`) |
| `templates/pages/plans/public.html` | Actualizar (fragmento con `0`) |
| `test/.../PlanBookmarkServiceImplTest.java` | Actualizar (U-SAVES-01/02) |
| `test/.../BookmarkControllerIntegrationTest.java` | Actualizar (I-SAVES-01..06) |
| `test/.../ForYouControllerTest.java` | Actualizar (clave `bookmarkCount` en el contrato del feed) |
| `test/.../BookmarkE2E.java` | Actualizar (E-SAVES-01) |
