# [PRINT] Imprimir / exportar PDF del itinerario

> Trello: tarjeta "Imprimir / exportar PDF del itinerario" (3 pts)
> **Estimación:** 3 pts Fibonacci

## Objetivo

Que quien armó un plan pueda imprimir el itinerario o guardarlo como PDF desde el navegador, para
llevarlo a la salida sin depender del celular ni de la conexión. La hoja impresa es una vista
propia: no es una captura del tablero. Muestra la cabecera del plan (nombre, fecha, hora de
inicio, visibilidad, autor y código corto), el recorrido agrupado por día —con horario, nombre,
categoría, dirección y duración de cada parada— y el tramo de traslado entre paradas consecutivas
(minutos y modo). El pie lleva la URL pública del plan y el código corto para volver desde el papel.

La impresión está disponible en el **detalle del dueño** (`/plans/{id}`) y en la **vista pública**
(`/plans/{id}/public`); no así en los listados (`/plans`, `/for-you`), donde no hay itinerario que
imprimir.

## Pre-requisitos

- [STD] y [SCH] completos: el detalle ya agrupa el recorrido por día, muestra el rango horario y la
  duración de cada parada, y el planificador [SCH] conoce el modo de traslado y los minutos entre
  paradas (`PlanScheduleCalculator`).
- [PVP] completo: la vista pública renderiza el itinerario server-side.
- `brand.css` se carga en todas las páginas desde `layouts/base.html`; la hoja de impresión vive
  ahí, no en un `<style>` de cada plantilla.

## Criterios de Aceptación

| # | Criterio |
|---|----------|
| AC-01 | El botón **Imprimir** (`[data-print-plan]`) está en el detalle del dueño y en la vista pública, llama a `window.print()` y no se imprime: en `@media print` queda oculto junto con el resto del chrome. No se renderiza en `/plans` ni en `/for-you` |
| AC-02 | `@media print` oculta **navbar, footer, mapa, toasts, chips y botones de acción**; solo queda visible la hoja de impresión. Fuera de impresión la hoja no existe para el usuario ni para el lector de pantalla (`display: none`) |
| AC-03 | La impresión fuerza el tema claro: fondo blanco y texto negro aunque el usuario tenga modo oscuro activo (`data-theme="dark"` y `data-bs-theme="dark"`), incluidos los colores heredados de Bootstrap y de la marca |
| AC-04 | La hoja imprime una **cabecera del plan** con nombre, fecha (`dd/MM/yyyy`), hora de inicio (`HH:mm`), visibilidad (`Público`/`Privado`) y autor. El detalle agrega el código corto; la vista pública no lo imprime porque PVP AC-04 prohíbe exponerlo en su HTML (ver decisiones). La cabecera comparte estilos en las dos vistas |
| AC-05 | El itinerario impreso se **agrupa por día** con un encabezado `Día N · dd/mm` por jornada, en el orden del itinerario. Cada parada imprime horario (`HH:mm – HH:mm`), nombre, categoría en español, dirección y duración (`45m`, `1h 30m`) |
| AC-06 | Entre paradas consecutivas **del mismo día** se imprime el tramo de traslado: minutos y modo (`a pie`, `en bici`, `en colectivo`, `en auto`). Los minutos salen de `travelMinutesToNext` cuando está fijado a mano; si no, de la estimación de [SCH] (`PlanScheduleCalculator.travelMinutes`). La última parada de cada día no lleva tramo |
| AC-07 | Control de cortes: cada parada lleva `break-inside: avoid`, el encabezado de día `break-after: avoid` y cada jornada intenta mantenerse entera (`break-inside: avoid-page`), de modo que un día que no entra arranca en una página nueva en vez de partir una parada al medio |
| AC-08 | El pie de la hoja imprime la **URL pública absoluta** del plan (`/plans/{id}/public`, con el origen de `siteOrigin`: `PUBLIC_BASE_URL`/`APP_BASE_URL` o el de la request). El código corto se imprime solo en la hoja del detalle del dueño, por la misma restricción de PVP AC-04 |
| AC-09 | Sin paradas, la hoja imprime la cabecera y el texto `Este plan todavía no tiene lugares.` |
| AC-10 | `pwsh -File scripts/gate.ps1 all` en verde |

## Decisiones / aclaraciones

- **La hoja es server-side, no una captura del tablero.** En el detalle el recorrido lo pinta Vue;
  imprimir ese DOM depende de que el módulo haya montado y de que el navegador no reordene columnas.
  `PrintSheet` arma días y paradas en Java y `components/print-sheet.html` los renderiza en ambas
  vistas. Así la vista pública (estática) y el detalle comparten markup y datos.
- **"Exportar PDF" no es una librería.** El botón abre el diálogo del navegador, que ya ofrece
  "Guardar como PDF" en todas las plataformas. Agregar un generador de PDF en el servidor sería otra
  tarjeta y otro costo.
- **Los traslados se estiman con el planificador, no con la diferencia de horarios.** Un pin manual
  puede dejar un hueco que no es viaje. `PlanScheduleCalculator.travelMinutes` expone la misma
  cuenta que usa el horario derivado (override manual, si no distancia con rodeo y modo).
- **El modo del tramo es el de la parada de origen** (`transportModeToNext`), con el modo del plan
  como fallback; es el mismo criterio que el planificador.
- **La agrupación por día copia la del JS:** fechas consecutivas iguales forman un grupo, y una
  fecha que reaparece después de otra arranca un día nuevo (`1 → 2 → 1`). El número de parada es
  global, el mismo de los marcadores del mapa.
- **La hoja no reemplaza la vista del tablero.** En pantalla sigue mandando el tablero de [STD]; la
  hoja solo aparece en el medio impreso.
- **La vista pública no imprime el código corto.** El código sirve para unirse al plan, y PVP AC-04
  exige que la vista pública no lo exponga en el HTML (lo verifica `PlanPublicSecurityTest`). La
  hoja pública imprime la URL absoluta, que ya alcanza para volver desde el papel; el código queda
  en la hoja del dueño, que es la que se comparte en mano.
- **El tema oscuro no se "resetea" en el documento.** La hoja fija sus colores con `!important` en
  `@media print` para no depender del orden de carga de Bootstrap (que en el detalle va antes que
  `brand.css`).
- **Las acciones del dueño van agrupadas y compactas.** Imprimir, Compartir y Eliminar comparten
  fila en la columna de 360 px; con el tamaño de botón anterior, "Eliminar plan" caía a una segunda
  línea. En desktop `.plan-primary-actions` no se parte (si la tipografía del sistema es más ancha,
  como en CI Linux, se corre unos píxeles sobre el gap de 16 px); en mobile envuelve entero si el
  ancho no alcanza.

## Escenarios de Test

### Tests Unitarios (`presentation/plan/PrintSheetTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| U-PRINT-01 | Agrupa paradas consecutivas por fecha y numera globalmente, con etiqueta `Día N · dd/mm` | AC-05 |
| U-PRINT-02 | Una fecha que reaparece después de otra arranca un grupo nuevo (1 → 2 → 1) | AC-05 |
| U-PRINT-03 | Formatea horario y duración (`09:00 – 10:30`, `1h 30m`) y cae a `Horario por definir` sin hora | AC-05 |
| U-PRINT-04 | Traduce la categoría al español y deja el resto como `Otro` | AC-05 |
| U-PRINT-05 | El tramo usa los minutos fijados a mano y el modo del tramo | AC-06 |
| U-PRINT-06 | Sin minutos fijados estima con la distancia y el modo del plan; sin coordenadas usa el default de [SCH] | AC-06 |
| U-PRINT-07 | La última parada de cada día no lleva tramo, aunque haya una parada en el día siguiente | AC-06 |
| U-PRINT-08 | Un plan sin paradas devuelve una hoja sin días | AC-09 |
| U-PRINT-09 | Sin modo en la parada ni en el plan, el tramo asume caminar | AC-06 |

### Tests de Integración (`integration/PrintSheetIntegrationTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| I-PRINT-01 | El detalle del dueño renderiza el botón Imprimir y la hoja con cabecera, días, paradas y tramo | AC-01, AC-04, AC-05, AC-06 |
| I-PRINT-02 | La vista pública renderiza el botón Imprimir y la misma hoja, con la URL pública en el pie y sin el código corto (PVP AC-04) | AC-01, AC-04, AC-08 |
| I-PRINT-03 | `/plans` y `/for-you` no renderizan el botón Imprimir | AC-01 |
| I-PRINT-04 | Sin paradas la hoja muestra el estado vacío y conserva la cabecera | AC-09 |

### E2E (`e2e/PrintItineraryE2E.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| E-PRINT-01 | Con `media print` emulada, navbar, mapa y botones no se renderizan; el nombre del plan y la primera parada sí | AC-02 |
| E-PRINT-02 | Un plan de más de un día imprime un encabezado por día y el orden coincide con el itinerario | AC-05 |
| E-PRINT-03 | Con tema oscuro activo, el texto impreso es negro sobre blanco | AC-03 |

## Archivos a crear/modificar

| Archivo | Acción |
|---------|--------|
| `presentation/plan/PrintSheet.java` | Crear (modelo de días/paradas/tramos, en Java y testeable) |
| `domain/plan/PlanScheduleCalculator.java` | Actualizar (exponer `travelMinutes` para el tramo impreso) |
| `presentation/plan/PlanController.java` | Actualizar (`printDays` en detalle y vista pública; `administratorName` y `shortCode` en el detalle) |
| `templates/components/print-sheet.html` | Crear (cabecera, días, paradas, pie; compartido por ambas vistas) |
| `templates/pages/plans/detail.html` | Actualizar (botón Imprimir + hoja de impresión) |
| `templates/pages/plans/public.html` | Actualizar (botón Imprimir + hoja de impresión) |
| `resources/css/brand.css` | Actualizar (`@media print` y estilos de la hoja) |
| `test/.../PrintSheetTest.java` | Crear (U-PRINT-01..08) |
| `test/.../PrintSheetIntegrationTest.java` | Crear (I-PRINT-01..04) |
| `test/.../PrintItineraryE2E.java` | Crear (E-PRINT-01..03) |
