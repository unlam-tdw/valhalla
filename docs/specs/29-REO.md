# [REO] Reordenar el itinerario por drag & drop (variante B)

> Trello: https://trello.com/c/BE77vWw1
> **Note:** This is an SDD proposal. Implementation may change based on team decisions.

## Objetivo

El detalle del plan ya permite reordenar el itinerario arrastrando, pero la
interacción es la cruda de HTML5 drag & drop: nada se mueve hasta soltar, los
horarios solo se actualizan después de la ida y vuelta al servidor, y en
pantalla táctil no funciona en absoluto. Este spec lleva esa interacción al
nivel de la variante B del prototipo (`prototype/dnd-reorder`, commit `8b2db11`,
`/prototype/reorder?variant=b`), que fue la elegida: **manija de arrastre,
reacomodo en vivo con animación FLIP, horarios derivados recalculados al vuelo
mientras se arrastra, resumen en vivo y soporte de teclado y táctil.**

La persistencia no cambia: el orden se guarda con el mismo
`POST /api/plans/{id}/places/reorder` de siempre y el servidor sigue siendo la
única verdad del horario derivado.

## Problema actual

| # | Problema | Dónde |
|---|----------|-------|
| P-1 | La card entera es el origen del arrastre: cualquier gesto sobre la card (seleccionar texto, intentar desplazar) puede iniciar un drag | `detail.html:666` (`th:attr draggable`), `plans-detail.js:427` |
| P-2 | Sin feedback durante el arrastre: la lista no se reacomoda y los horarios no cambian hasta soltar y que el servidor responda | `plans-detail.js:431-454` |
| P-3 | HTML5 drag & drop no existe en pantalla táctil: en móvil y tablet el itinerario no se puede reordenar | mismo mecanismo |
| P-4 | Sin alternativa de teclado: la manija es un `<span>` decorativo `aria-hidden`, no enfocable | `detail.html:673-682` |
| P-5 | El chip de resumen del header no refleja el orden nuevo hasta recargar: es server-rendered y `refreshSchedule` solo copia horarios por parada | `plans-detail.js:114-124` |

## Decisiones de diseño

Estas decisiones están tomadas y son las que la implementación debe respetar.

| # | Decisión | Motivo |
|---|----------|--------|
| D-1 | La manija es el único origen del arrastre: `pointerdown` sobre un `<button>` con `setPointerCapture`; el `<li>` deja de ser `draggable` | Fin de los drags accidentales (P-1) y base de táctil (P-3) |
| D-2 | Mientras se arrastra, la lista se reacomoda en vivo con animación FLIP: el orden visible es el de la parada ya insertada en la ranura bajo el puntero | El "momento mágico" que ganó el prototipo |
| D-3 | El horario del preview se calcula con el espejo JS (`plan-schedule.js`), no con el servidor | Cero round-trips por frame; el espejo ya existe para el creador |
| D-4 | El servidor sigue siendo la única verdad persistida: al soltar corre el mismo `POST /reorder` + `refreshSchedule` de hoy, con rollback optimista si falla | El preview es efímero; la persistencia no cambia ni un byte |
| D-5 | Con `autoSchedule` desactivado el preview no recalcula horarios: las filas se reacomodan y las horas quedan como están | Espeja exactamente lo que el servidor hará con el mismo orden |
| D-6 | Una parada con horario fijado ancla el preview: muestra su hora fijada y las siguientes se recalculan desde ese pin | El espejo honra el pin como lo hace `PlanScheduleCalculator` (24-SCH D-6); sin esto el preview mentiría |
| D-7 | La manija es un botón real con teclado: Enter/Space levanta, ↑/↓ mueve la ranura, Enter/Space suelta, Escape cancela | Accesibilidad (P-4); el foco se queda en la manija |
| D-8 | `touch-action: none` solo en la manija | El drag funciona en táctil sin romper el scroll de la página |
| D-9 | El resumen del header se previsualiza en vivo durante el arrastre con la misma cuenta de `PlanItinerarySummary` (paradas, tiempo total, km) | Ver el costo del orden antes de soltar; el servidor lo confirma al refrescar |
| D-10 | El mapa no se actualiza durante el arrastre: marcadores y ruta se redibujan al soltar, como hoy | Actualizar Leaflet por `pointermove` es costoso y el prototipo ganó con el preview en lista + resumen |
| D-11 | Una fila que cambia de grupo de día durante el preview se monta en el grupo nuevo sin animación | Vue remonta la clave al cambiar de `<ol>`; FLIP anima las filas que persisten |
| D-12 | Anuncios de posición en un `aria-live` off-screen y `prefers-reduced-motion` desactiva la animación | Teclado y motion; convención [STD] de la página |

La ranura de inserción se detecta contra las filas de todos los grupos de día en
orden del DOM (es el orden del itinerario), y se mapea de vuelta al índice del
array plano `planPlaces`: los grupos de día son presentación, el orden es plano.

## Modelo de datos

Ninguno. No hay tablas, columnas ni endpoints nuevos. El único cambio de
servidor es el seed del detalle: `showPlan` agrega al `PLAN_DETAIL_SEED` los
cuatro valores que el espejo necesita para derivar en el cliente
(`eventTime`, `dayEndTime`, `transportMode`, `autoSchedule`), al lado del
`planDate` que ya viaja.

## Preview del horario

Mismo algoritmo que el servidor, corriendo en el cliente solo mientras el
arrastre está activo (y solo con `autoSchedule` activo, D-5):

```
displayOrder = orden de planPlaces con la parada arrastrada
               insertada en la ranura bajo el puntero
preview      = scheduleStops(planDate, eventTime, dayEndTime, transportMode, displayOrder)
```

- `plan-schedule.js` gana un caso: una parada con `timePinned` y hora fijada
  ancla su inicio en esa hora (y su día en `visitDate`); las siguientes siguen
  la cadena desde ahí. Es el reflejo exacto del pin de 24-SCH D-6, y hoy el
  espejo no lo conoce.
- Al soltar: el `POST /reorder` persiste y `refreshSchedule` trae las horas del
  servidor, que manda sobre el preview (D-4). Con `autoSchedule` off el
  servidor no recalcula y el preview tampoco (D-5), así que no hay salto.
- El preview nunca toca `planPlaces`: vive en computed sobre `displayOrder`.
  Solo el drop muta el array, con rollback si el POST falla (comportamiento
  actual, se conserva).

## UX por pantalla

### Detalle (`/plans/{id}`, panel Itinerario)

- La manija de 6 puntos pasa a ser el botón de arrastre: `cursor: grab`,
  `active: cursor: grabbing`, `touch-action: none`; la card deja de tener
  cursor de drag.
- Agarrar la manija destaca la fila (anillo + sombra, igual que el prototipo),
  baja la opacidad de las demás y la lista reacomoda en vivo mientras el
  puntero cruza ranuras.
- Al soltar queda el orden, el POST corre y el resumen/mapa se confirman con el
  servidor.
- Con teclado: la manija es enfocable; Enter/Space levanta la parada, ↑/↓
  mueven la ranura (con el mismo recálculo en vivo), Enter/Space suelta,
  Escape cancela. Un `aria-live` anuncia "Parada N, posición M".
- El hint del panel pasa de "Arrastrá las tarjetas para organizar tu recorrido"
  a mencionar la manija.
- Los botones de la fila (Ver más, Eliminar) siguen siendo botones normales:
  nada del arrastre interfiere con ellos.
- Manijas y drag solo con `canEditPlan`, igual que hoy.

## Criterios de Aceptacion

| # | Criterio |
|---|----------|
| AC-01 | La manija es el único origen del arrastre; un gesto sobre la card sin manija no inicia reorden |
| AC-02 | Mientras se arrastra, la lista reacomoda en vivo con animación y la parada arrastrada se destaca; suelta y queda |
| AC-03 | Los horarios derivados de todas las paradas se recalculan en vivo durante el arrastre cuando `autoSchedule` está activo |
| AC-04 | Con `autoSchedule` desactivado, reordenar no cambia horarios ni en preview ni tras soltar |
| AC-05 | Al soltar, el orden persiste con `POST /api/plans/{id}/places/reorder`; un fallo revierte el orden optimista y muestra el error |
| AC-06 | Una parada con horario fijado conserva su hora en el preview y las siguientes se recalculan desde ese pin |
| AC-07 | El resumen del header se previsualiza en vivo durante el arrastre y queda confirmado por el servidor tras soltar |
| AC-08 | El mapa y la ruta se actualizan al soltar, no durante el arrastre |
| AC-09 | La manija es enfocable y opera por teclado: Enter/Space levanta, ↑/↓ mueve, Enter/Space suelta, Escape cancela |
| AC-10 | El reorden funciona en pantalla táctil y el scroll de la página no se rompe (`touch-action` limitado a la manija) |
| AC-11 | Los cambios de posición se anuncian en un `aria-live` y `prefers-reduced-motion` desactiva la animación |
| AC-12 | La ranura de inserción funciona a través de los grupos de día: mover una parada a otro día reacomoda y re-agrupa en vivo |

## Escenarios de Test

### Tests de Integracion (`presentation/plan/PlanControllerTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| I-01 | El modelo del detalle expone hora de inicio, fin de día, modo y `autoSchedule` para el seed | AC-03, AC-04 |

### E2E (Playwright, Chromium)

| # | Test | Flujo | AC que cubre |
|---|------|-------|-------------|
| E-01 | `PlanReorderE2E` (arrastre) | Agarrar la manija de la parada 3, llevarla arriba de la 1 y soltar: la lista reacomoda en vivo, los horarios cambian durante el drag, se persiste y el servidor confirma | AC-01, AC-02, AC-03, AC-05 |
| E-02 | `PlanReorderE2E` (teclado) | Foco en la manija, Enter levanta, ↓ mueve, Enter suelta: el orden persiste | AC-09 |
| E-03 | `PlanReorderE2E` (táctil) | Drag táctil de la manija en viewport móvil: reordena y la página no scrollea durante el drag | AC-10 |
| E-04 | `PlanReorderE2E` (pin) | Plan con una parada fijada: al reordenar, el pin conserva su hora y las siguientes salen desde ahí | AC-06 |

### Convención de JS

No hay runner de JS: la lógica del drag se cubre por E2E y el cambio del espejo
(`plan-schedule.js`, pines) es un caso más del algoritmo que E-04 ejercita de
punta a punta, igual que el resto del espejo hoy.

## Plan de entrega

| # | Slice | Contenido | Cierra | Costo |
|---|-------|-----------|--------|-------|
| 1 | Manija | Botón de arrastre con pointer events, `setPointerCapture`, `touch-action`, drop persistente con rollback (reemplaza el HTML5 DnD) | AC-01, AC-05, AC-10 | ~0.5 día |
| 2 | Reflow en vivo | FLIP, `displayOrder`, preview de horario con el espejo (incluye pines en el espejo y los 4 valores nuevos del seed), `autoSchedule` off | AC-02, AC-03, AC-04, AC-06, AC-12 | ~1 día |
| 3 | Resumen y mapa | Resumen del header en vivo durante el drag; mapa y ruta al soltar | AC-07, AC-08 | ~0.5 día |
| 4 | Teclado y motion | Manija enfocable, teclado completo, `aria-live`, reduced-motion, hint | AC-09, AC-11 | ~0.5 día |

Total estimado: 2.5 días.

## Archivos a crear/modificar

| Archivo | Accion |
|---------|--------|
| `webapp/resources/core/js/pages/plans-detail.js` | Actualizar (drag por manija, FLIP, preview, teclado, resumen en vivo) |
| `webapp/WEB-INF/templates/pages/plans/detail.html` | Actualizar (manija como botón, sin `draggable`, hint, `aria-live`, estilos) |
| `webapp/resources/core/js/lib/plan-schedule.js` | Actualizar (pin como ancla de inicio en `scheduleStops`) |
| `presentation/plan/PlanController.java` | Actualizar (seed: hora de inicio, fin de día, modo, `autoSchedule`) |
| `docs/specs/29-REO.md` | Crear (este documento) |

## Riesgos

| Riesgo | Mitigación |
|--------|------------|
| El preview del espejo diverge del servidor | El espejo ya existe con ese rol en el creador; el servidor confirma al soltar y `refreshSchedule` reconcilia (D-4) |
| Pointer events rotos por diálogos o pérdida de foco | `pointercancel` restaura el estado como un drop cancelado; el drag solo vive dentro del panel |
| FLIP en filas que cambian de grupo de día | D-11: se remontan sin animación, el resto anima |
| Drag accidental al hacer scroll en táctil desde la card | D-1/D-8: solo la manija captura el gesto y solo ella desactiva el scroll |
| Prettier/Checkstyle sobre los JS tocados | El gate corre el check sobre `resources`; el subagente corre `gate.ps1 check` en cada slice |

Fuera de alcance: animar el mapa durante el drag (D-10), reordenar en la vista
pública (es de solo lectura), contenedores de drop por día como zonas
independientes (el orden es plano y así queda), y cambiar el endpoint de
persistencia.
