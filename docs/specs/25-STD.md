# [STD] Vista del plan (dueño): sin datos repetidos y con menos scroll

> Trello: pendiente de crear la card — `[DIS][STD] Vista del plan (dueño): sin datos repetidos y
> con menos scroll`, lista `BACKLOG` (spec 1:1 con la card)
> **Estimacion:** 3 pts Fibonacci
> **Note:** This is an SDD proposal. Implementation may change based on team decisions.
> **Estado (2026-10-28):** spec inicial para revisión, sin implementar. Decisiones de diseño
> cerradas con la usuaria (ver Notas). Card pendiente de pegar en Trello.

## Objetivo

La vista de dueño de `/plans/{id}` —la que se ve al crear un plan o al clonar uno— debe mostrar
cada dato del plan **una sola vez** y caber en **menos pantallas**, sin perder ninguna acción que
hoy ofrece. Es deuda de diseño: **el alcance es visual, no funcional** (ninguna ruta,
controlador, servicio, permiso ni regla de negocio cambia).

Hoy es la página más larga del sitio: 2.145 px en desktop y 3.589 px en un celular. Casi la mitad
de la altura la ocupan dos bloques que se pueden plegar (edición de metadatos e invitación) y seis
datos que se ven repetidos entre la ficha y el form de edición.

## Problema actual (baseline medido el 2026-10-28)

Medido con Playwright sobre el plan clonado en dev (`/plans/01a184dd-1546-7de9-b7a7-94fa8903b5c5`
con arranque de `test@`); capturas baseline en el hand-off de la card.

| Ancho | Alto total (`scrollHeight`) | Pantallas |
|-------|-----------------------------|-----------|
| 1440×900 (desktop) | 2.145 px | 2,4 |
| 390×844 (mobile) | 3.589 px | 4,3 |

Bloques por alto (desktop): editar plan 731 px (34 % de la página) · Invitar 545 px · mapa 514 px ·
ficha 426 px · recorrido 372 px · solicitudes con estado vacío 106 px · agregar lugar 128 px.

Orden vertical real en 390 (medido, no inferido): `solicitudes → mapa → agregar lugar → recorrido →
ficha → participantes → editar` — el resumen del plan queda **después** del recorrido.

Duplicados en la vista de dueño (texto visible + `value` de los inputs, sin abrir nada):

| Dato | Veces | Dónde se ve |
|------|-------|-------------|
| nombre | 2 | cabecera (`h2`) + input del form |
| descripción | 2 | ficha + textarea |
| fecha | 2 | ficha + input date |
| hora | 2 | ficha + input time |
| visibilidad | 2 | ficha + switch del form |
| administrador | 2 | ficha + badge "Dueño" en Participantes |

La hora/fecha de las tarjetas del recorrido son dato de parada, no duplicados.

## Pre-requisitos

- [DIS] (17-DIS) mergeada: navbar compartido, banda de cabecera (AC-14) y el modal "Ver más" con el
  patrón de diálogo a11y (`aria-labelledby`, foco, Escape) que este spec replica para la edición.
- [SCH] (24-SCH) mergeada: la ficha FECHA ya muestra dd/mm/yyyy y el form de edición lleva "Fin del
  día" y "Cómo se trasladan" (ambos campos viajan al modal).
- [MOBILE] mergeada: el responsive ≤575 px, donde aplica el nuevo orden de secciones.
- El model ya trae todo lo necesario (`canEditPlan`, `canInvite`, `canHandleRequests`,
  `joinRequests`, `participants`): **no hay backend nuevo ni migraciones**.
- Sin clases de test nuevas: los métodos E2E nuevos van dentro de la clase existente
  `PlanPlaceDetailE2E`; la verificación es gate completo + checks visuales.

## Criterios de Aceptacion

| # | Criterio |
|---|----------|
| AC-01 | En la vista base de dueño cada dato del plan se ve **una sola vez**: nombre (cabecera), descripción, fecha, hora y visibilidad viven en la ficha y ya no en un form en línea. La ficha es compacta —chips `FECHA · HORA · VISIBILIDAD · CÓDIGO` y descripción con clamp de 3 líneas— y **no muestra el tile ADMINISTRADOR**: la fuente única es el badge "Dueño" de Participantes |
| AC-02 | La edición de metadatos (nombre, descripción, fecha, hora, fin del día, traslado, visibilidad) se abre desde un botón "Editar" de la ficha en un `<dialog>`: conserva `canEditPlan`, el mismo form (`POST /plans/{id}` con `th:object="${plan}"`) y el guardado actual. El diálogo declara `aria-labelledby` apuntando a su título "Editar plan", recibe el foco al abrirse y se cierra con Escape; queda en el DOM después de Participantes y antes de `banned-participants`, y solo se renderiza con permiso de edición |
| AC-03 | Sin solicitudes pendientes **no se renderiza la card de Solicitudes** (desaparece el mensaje "No hay solicitudes pendientes."). Con ≥ 1 pendiente la card aparece con la lista de Aceptar/Rechazar y la fila de cabecera muestra un badge ancla con el conteo (también solo con ≥ 1) |
| AC-04 | "Invitar a alguien" deja de ser una card permanente y pasa a un **accordion dentro de Participantes**: la lista de participantes siempre visible; un botón "Invitar a alguien" con `aria-expanded` expande y colapsa el form actual completo (mismo action, mismos campos, mismo `th:if="${canInvite}"`) |
| AC-05 | "Agregar lugar al plan" deja de ser una card propia y pasa a un **botón en la cabecera de "Tu recorrido"** (`th:if="${canEditPlan}"`): al presionarlo se despliega el mismo picker (select de lugares disponibles + "+ Agregar") debajo del título, sin recargar la página |
| AC-06 | En 390×844 el orden vertical es: **cabecera → ficha (resumen) → Tu recorrido (+ Agregar lugar) → Mapa → Solicitudes (con ≥ 1) → Participantes (+ accordion Invitar)**. En desktop se conserva la composición de dos columnas actual (mapa/recorrido a la izquierda, ficha/participantes a la derecha). El reordenamiento se hace con utilities CSS `order`, sin mover el DOM |
| AC-07 | El alto total queda en **≤ 1.500 px en 1440×900** (−30 % vs 2.145) y **≤ 2.800 px en 390×844** (−22 % vs 3.589), sin perder ninguna acción actual. Se mide con `document.documentElement.scrollHeight` sobre el arrange estándar de los E2E |

## Escenarios de Test

Es una spec de diseño: no hay lógica nueva, así que no hay clases de test U/I/S nuevas. La
verificación son los checks V, tres métodos E2E nuevos dentro de `PlanPlaceDetailE2E` y la puerta
completa.

### Checks visuales (V-XX, manuales o asistidos por los E2E)

| # | Check | AC que cubre |
|---|-------|-------------|
| V-01 | Vista de dueño sin abrir nada: nombre, descripción, fecha, hora y visibilidad ocurren una vez cada uno en texto visible + `value` de inputs; `#plan-summary` muestra los 4 chips y una descripción de ≤ 3 líneas, y no el tile ADMINISTRADOR; Participantes sí muestra "Dueño" | AC-01 |
| V-02 | Como dueño/editor: el botón "Editar" de la ficha abre el diálogo (`aria-labelledby` hacia "Editar plan", `document.activeElement` dentro, Escape lo cierra); guardar un nombre nuevo actualiza cabecera y ficha tras el redirect. Sin permiso de edición no hay ni botón ni diálogo | AC-02 |
| V-03 | Con 0 solicitudes: no hay card, no hay mensaje de vacío ni badge. Con ≥ 1 (otra sesión se une al plan): aparece la card con Aceptar/Rechazar y el badge con el conteo en la cabecera | AC-03 |
| V-04 | Participantes: la lista siempre visible; "Invitar a alguien" colapsado con `aria-expanded="false"`, al expandir muestra el form completo (email, rol, canal) y al colapsar lo oculta | AC-04 |
| V-05 | "Tu recorrido": el botón "Agregar lugar al plan" vive en su cabecera y no existe card propia de agregar; el picker se despliega y agrega una parada al recorrido | AC-05 |
| V-06 | A 390×844 el `y` de las secciones cumple ficha < recorrido < mapa < participantes; a 1440×900 las dos columnas quedan como hoy | AC-06 |
| V-07 | `scrollHeight` ≤ 1.500 px @1440×900 y ≤ 2.800 px @390×844, medido antes/después con Playwright | AC-07 |

Verificar como administrador y como editor, en 1440×900 y 390×844, en **claro y oscuro**.

### E2E

Tres métodos nuevos en `e2e/PlanPlaceDetailE2E`, sobre el arranque existente (crea un plan con una
visita y abre el detalle):

- `keepsTheOwnerViewCompactAndWithoutRepeatedData` → AC-01, AC-03, AC-04 y AC-05: cada dato
  ocurre una vez (texto visible + `value`, sin abrir el modal), chips en `#plan-summary`, ausencia
  de la card de solicitudes con la lista vacía, accordion de Invitar colapsado que se expande, y el
  botón "Agregar lugar al plan" dentro de la cabecera de "Tu recorrido" que muestra el select.
- `editsPlanMetadataFromAModalWithDialogA11y` → AC-02: abrir con foco dentro del diálogo, cierre
  con Escape, round-trip de guardado (cambia la hora → el chip de la ficha refleja el valor nuevo) y
  que el form no existe fuera del diálogo.
- `followsTheSectionOrderAndScrollBudget` → AC-06 y AC-07: posiciones `y` a 390 y `scrollHeight`
  bajo ambos techos.

La suite E2E existente debe seguir en verde: si un selector se rompe por el markup nuevo se
actualiza el page object, no la lógica del test; cuando el **flujo** cambia (abrir el modal antes
de editar) se actualiza el test y queda listado abajo.

> Como ya hay otros `<dialog>` en la página (`banned-participants` va primero en el DOM), todos
> los selectores nuevos se anclan por `aria-labelledby`, nunca por `querySelector('dialog')`.

### Impactos previstos en la suite existente (se ajustan en la misma card)

- `PlanPlaceDetailE2E.addsPersistsAndDisplaysVisitDetails`: `input[name='eventTime']`.fill y el
  click en "Guardar cambios" ahora ocurren dentro del diálogo → abrir el modal antes (helper
  `openEditDialog()` en `PlansPage`). Los `hasValue` sobre inputs del diálogo cerrado leen
  propiedad y deberían pasar; si Playwright los rechaza, se abre el modal también para esas líneas.
- `PublicPlanCloneE2E.makePlanPublic` y `PublicPlanViewE2E.makePlanPublic`: `#isPublic`.check() +
  click "Guardar cambios" → abrir el modal antes con el mismo helper.
- `PlanParticipantsE2E`: `#invitation-email`.fill requiere expandir el accordion antes.
- `PlanScheduleE2E`: `hasValue` sobre `eventTime` / `dayEndTime` / `transportMode` con el diálogo
  cerrado (esperado OK; abrir solo si Playwright lo exige).
- `PlanControllerIntegrationTest` (**no se modifica**; sus anclajes están preservados por diseño y
  se verifica en el gate): el literal "Editar plan" vive solo en el `<dialog>` en la misma posición
  de DOM que hoy ocupa el bloque de edición (los `substring(participants-heading → "Editar plan")`
  siguen cortando bien y el trigger se llama "Editar", sin "plan"); "Agregar lugar al plan" e
  "Invitar a alguien" conservan sus literales con los mismos `th:if`; los chips mantienen
  `<span>valor</span>` crudo (`>31/12/2026</span>`); el diálogo sigue en el HTML con
  `input[name='eventDate']` en value ISO. Si igual algún ancla rompe, se ajusta la aserción
  conservando su semántica de permisos y se documenta en Notas de implementación.
- `ActionIconsTest`: los 5 glyphs mínimos de DETAIL se mantienen (SAVE vive en el diálogo de
  edición) y los nuevos botones suman íconos del set (Editar, Agregar lugar).
- `MobileLayoutE2E.T_MOB_008` (detalle legible a 320 px, sin scroll lateral) debe seguir en verde.
- `PlanBanE2E` y `PublicPlanJoinRequestE2E`: el form `join-requests` sigue existiendo cuando hay
  ≥ 1 solicitud (los tests crean la solicitud antes de aceptarla; al aceptar la última, la card
  desaparece — comportamiento nuevo esperado).

### Puerta

El cierre de la card es el gate completo en verde con los counts escritos en el item de la card:
`powershell.exe -File scripts/gate.ps1 all` (unit+integration y E2E). No se agregan suites.

## Notas / decisiones de diseño

- **Decisiones cerradas (2026-10-28, con la usuaria):** edición → modal; solicitudes → badge y
  card solo con ≥ 1; invitar → accordion; "Agregar lugar" → dentro de "Tu recorrido"; orden mobile
  resumen → recorrido → mapa → personas; techos de scroll ≤ 1.500 / ≤ 2.800 px.
- **Alternativas descartadas:** (B) edición en `/plans/{id}/settings` — esa página es de permisos,
  requeriría backend nuevo y saca al usuario del contexto del plan; (C) accordion en línea para
  todo — cambio mínimo pero mucho menor ahorro de scroll.
- **Reordenar sin mover el DOM.** El orden mobile se logra con utilities `order` (más un `order`
  interno entre mapa y recorrido); el DOM queda como hoy, y por eso los `substring()` de
  `PlanControllerIntegrationTest` (`plan-summary` → siguiente `<section>`; `participants-heading` →
  "Editar plan") siguen cortando regiones válidas.
- **Literales que no cambian** (los usan tests de permisos): "Editar plan" (título del diálogo),
  "Agregar lugar al plan" (del botón, con `th:if="${canEditPlan}"`), "Invitar a alguien" (del
  accordion, con `th:if="${canInvite}"`), las etiquetas FECHA/HORA de la ficha y el
  `<span>valor</span>` crudo dentro de los chips.
- **La ficha compacta reemplaza a la grilla de tiles** del AC-09 de 17-DIS (mismo motivo: altura).
  Al implementar, 17-DIS se anota con la actualización al estilo de "Actualización 24-SCH" en
  `05-APL-FE.md`. Si en revisión se prefiere conservar tiles, se reducen a 4 con clamp — la meta de
  alto del AC-07 no cambia.
- **Trampas heredadas:** `.bg-white` con `!important` de Bootstrap en modo oscuro (hallazgo de
  17-DIS) — verificar ambos temas; la hora/fecha de las tarjetas del recorrido es dato de parada,
  no un duplicado; el modal "Ver más" y su a11y son de 17-DIS y no se tocan.
- **Fuera de alcance:** migrar Bootstrap del detail a Tailwind (deuda aparte, decisión de 17-DIS),
  backend/permisos (`canEditPlan`, `canInvite`, `canHandleRequests`), `/plans/{id}/settings`
  (es de permisos), el flujo de clonado, la lógica de solicitudes/invitaciones y la configuración
  del mapa (Leaflet queda intacto).

## Notas de implementación (fase verde, 2026-10-28)

- **[STD-001..006]** Todo el cambio vive en `templates/pages/plans/detail.html` + `plans-detail.js`:
  - **Ficha:** 4 × `.plan-chip` (label chico sobre valor, `background: var(--bs-secondary-bg)` para
    que el modo oscuro no pida reglas extra) y `#plan-description` con
    `-webkit-line-clamp: 3` (`.plan-description-clamp`, el valor que clava el E2E vía
    `getComputedStyle`). El label va en `<small>` — no es capricho: `DetailPlanCloneE2E` lee el
    código con `small:has-text('CÓDIGO') + span` y con `<span>` en el label el selector se rompía.
    El tile ADMINISTRADOR se fue del todo; el badge "Dueño" de Participantes es la fuente.
  - **Edición:** `#plan-edit-dialog` conserva el mismo form y el mismo POST; `ref="editDialog"` +
    `openEdit()` replican la mecánica de `bannedParticipantsDialog`. El trigger es
    `#plan-summary button:has-text('Editar')` (texto exacto "Editar") y el literal completo
    vive solo en el `<h5 id="plan-edit-title">`. El diálogo sigue **inmediatamente después de
    Participantes** en el DOM porque los `substring()` de integración cortan de
    `participants-heading` hasta ese literal. **Trampa:** los comentarios HTML se sirven crudos —
    escribir "Editar plan" en un comentario de este template rompe los `not(containsString(...))`
    de integración y desordena los `substring()` (el comentario del trigger no lo contiene a
    propósito).
  - **Solicitudes:** badge ancla en la fila de cabecera y card con
    `th:if="${canHandleRequests and !#lists.isEmpty(joinRequests)}"`: con la lista vacía no hay
    card, no hay mensaje de vacío y no hay ancla (el heading desaparece).
  - **Invitar / agregar lugar:** `inviteOpen` y `addPlaceOpen` (refs Vue, arrancan en `false`) con
    `aria-expanded` + `aria-controls`; ambos paneles llevan además `th:if` de permiso, así la
    vista anónima no sirve markup de edición. El picker pasó a la cabecera de "Tu recorrido" y
    quedó colapsado: la card propia ya no existe.
  - **Orden mobile sin mover el DOM:** `.itinerary-col` y `.plan-cols` hacen `display: contents`
    en `max-width: 767.98px` y cada hijo promovido gana `order` (ficha 1 → recorrido 2 → mapa 3 →
    solicitudes 4 → participantes 5). Detalle que no es cosmético: los promovidos dejan de
    calificar como `.row > *`, así que el CSS les devuelve a mano el `width: 100%`, el gutter y el
    `margin-top` que Bootstrap les daba. En desktop no se toca nada: los wrappers vuelven a ser
    columnas y la composición (mapa+recorrido a la izquierda, ficha+participantes a la derecha) es
    la de siempre.
- **Presupuestos (V-07 / AC-07) medidos sobre un plan con datos** (capturas del 2026-10-28):
  desktop 1440×900 → **1.174 px** (techo 1.500; el baseline era 2.121-2.145) y 390 → **1.873 px**
  (techo 2.800; baseline 3.589). Orden mobile medido: ficha y=332 → recorrido y=628 → mapa
  y=1116 → participantes y=1689. Los dos temas verificados (claro/oscuro, desktop/móvil, diálogo
  abierto): los chips heredan `--bs-*` y no dejaron manchas blancas.
- **Tests existentes migrados en la fase verde:** `PlanPlaceDetailE2E.addsPersistsAndDisplaysVisitDetails`
  y los dos `makePlanPublic` abren el diálogo con `PlansPage.openEditDialog()` antes de tocar los
  inputs; `PlanParticipantsE2E` despliega el accordion antes de `#invitation-email`.
- **Nota de entorno (no es de la card):** `PlacesViewE2E.shouldShowDetailsFromMarkerWithoutLeavingPlacesPage`
  fallaba en local por el orden **físico** de la tabla `places` en `valhalla_e2e`: `findAll()`
  sin `ORDER BY` devuelve el orden de heap, y "Estadio Monumental" (noroeste, fuera de la vista
  fija del explore a zoom 13) quedó en la ranura (0,14) → era el `.first()` del test. El CI nunca
  lo vio porque su volumen de Postgres nace vacío y el seeder inserta en orden de catálogo.
  `TRUNCATE places CASCADE` + el arranque siguiente del Jetty resembraron `(0,1) El Sanjuanino…`
  y el test pasó. No había código roto.

## Plan de entrega

Items de la card (se van tildando con su commit, `(commit <hash>)` al landing) — **cerrados
2026-10-28**:

- [STD-001] Ficha compacta: chips (fecha · hora · visibilidad · código), clamp de descripción y
  tile ADMINISTRADOR fuera (AC-01). ✔ (commit e28b3e8)
- [STD-002] "Editar plan" → `<dialog>` con a11y + botón "Editar" en la ficha (AC-02). ✔ (commit e28b3e8)
- [STD-003] Solicitudes: badge en la cabecera y card solo con ≥ 1 (AC-03). ✔ (commit e28b3e8)
- [STD-004] "Invitar a alguien" → accordion dentro de Participantes (AC-04). ✔ (commit e28b3e8)
- [STD-005] "Agregar lugar al plan" → botón en la cabecera de "Tu recorrido" (AC-05). ✔ (commit e28b3e8)
- [STD-006] Orden mobile con `order`, sin mover el DOM (AC-06) y techos de scroll (AC-07). ✔ (commit e28b3e8)
- [STD-007] Spec y docs: nota de actualización en 17-DIS (AC-09) + hand-off de la card. ✔ (este commit)
- [T-STD-001] E2E: los tres métodos de "### E2E" en verde. ✔ (commit e28b3e8 — PlanPlaceDetailE2E 7/7)
- [T-STD-002] Gate: `gate.ps1 all` en verde con los counts al cierre. ✔ (exit 0: surefire
  998/0/0 · E2E 84/0/0 en 26 clases · checkstyle 0 · prettier/PMD/CPD · gate js -Strict 0 FAIL)

## Archivos a crear/modificar

| Archivo | Accion |
|---------|--------|
| `templates/pages/plans/detail.html` | Actualizar (chips de `#plan-summary` sin tile ADMINISTRADOR; botón "Editar" + `<dialog>` "Editar plan" después de Participantes y antes de `banned-participants`; badge de solicitudes en la cabecera y card solo con ≥ 1; accordion "Invitar a alguien" dentro de Participantes; botón "Agregar lugar al plan" en la cabecera de "Tu recorrido"; utilities `order` para el orden mobile) |
| `resources/core/js/pages/plans-detail.js` | Actualizar (refs del diálogo de edición: abrir con foco y cerrar con Escape, misma mecánica que `bannedParticipantsDialog`; toggles del accordion y del picker de lugares con los refs Vue que hagan falta) |
| `resources/css/brand.css` (o el `<style>` de `detail.html`) | Actualizar/crear (chips, badge, estados del accordion, clamp de la descripción, estilos del diálogo — claro y oscuro) |
| `test/java/com/valhalla/e2e/PlanPlaceDetailE2E.java` | Actualizar (3 métodos nuevos + abrir el modal en `addsPersistsAndDisplaysVisitDetails`) |
| `test/java/com/valhalla/e2e/views/PlansPage.java` | Actualizar (helper `openEditDialog()` compartido por los tests que editan metadatos) |
| `test/java/com/valhalla/e2e/PlanParticipantsE2E.java` | Actualizar (expandir el accordion antes de `#invitation-email`) |
| `test/java/com/valhalla/e2e/PublicPlanCloneE2E.java` | Actualizar (`makePlanPublic` abre el modal antes de `#isPublic`) |
| `test/java/com/valhalla/e2e/PublicPlanViewE2E.java` | Actualizar (`makePlanPublic` abre el modal antes de `#isPublic`) |
| `test/java/com/valhalla/e2e/PlanScheduleE2E.java` | Verificar (`hasValue` con el diálogo cerrado; abrir solo si Playwright lo exige) |
| `docs/specs/17-DIS.md` | Actualizar (nota de actualización AC-09 → ficha en chips, al implementar) |

No se tocan controladores, servicios, repos, SecurityConfig, datos de semilla, ni ningún test de
unidad, integración o seguridad (los de integración se **verifican** en el gate sin editar). No
hay backend nuevo: toda la inteligencia de la card es markup, estilos, un poco de JS de presentación
y tests.
