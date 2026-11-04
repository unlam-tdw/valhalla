# Mi calendario — prototipo

Implementado en la rama `task-51-CAL`.

Se accede desde **Mi calendario** en el menú del avatar del navbar, tanto en
escritorio como en móvil. La ruta `/plans/calendar` requiere iniciar sesión.

La vista mensual muestra los planes propios y los planes en los que el usuario
participa. Cada plan aparece en su fecha de inicio (`eventDate`), con su hora
cuando está disponible, y permite abrir su detalle. Las invitaciones pendientes
no se incluyen. Los planes sin fecha se muestran en una sección aparte.

Las semanas comienzan el lunes. Se puede avanzar o retroceder un mes y volver
al mes actual con **Hoy**; el día actual queda destacado. La cuadrícula compacta
se adapta al ancho disponible sin desplazamiento horizontal, con celdas de altura
adaptada a la pantalla y controles táctiles de al menos 44 píxeles. Cada día muestra
la cantidad de planes; al seleccionarlo se muestran nombres completos, horarios y
enlaces en un panel lateral en escritorio y debajo de la cuadrícula en móvil.
La lista del día tiene altura limitada y desplazamiento propio cuando hay muchos
planes. Los planes sin fecha se consultan en una sección desplegable. Se reutilizan el layout,
las tipografías, los colores y el soporte de tema oscuro existentes.

El mes y el día se pueden enlazar con `/plans/calendar?month=2026-10&date=2026-10-29`.
Al cambiar de mes se selecciona su primer día; en el mes actual se selecciona hoy.
Una fecha inválida o ajena al mes utiliza esa selección inicial. Un mes inválido
vuelve al mes actual. El renderizado se hace con Thymeleaf, sin dependencias
nuevas de ejecución ni JavaScript adicional.

Este primer alcance es de consulta: no permite crear, editar ni arrastrar
eventos, y no desglosa los días del itinerario de los planes de varios días.
La actualización de la tarjeta de Trello queda pendiente porque sus herramientas
no están disponibles en esta sesión.

## Pruebas automatizadas

Se agregaron pruebas después del prototipo inicial:

| Clase | Cobertura |
| --- | --- |
| `CalendarControllerTest` | Mes actual, semanas completas de lunes a domingo, meses de cuatro a seis semanas, año bisiesto, cambios de año, límites, fechas inválidas, selección del día, etiquetas en español, combinación de planes y planes sin fecha. |
| `CalendarIntegrationTest` | Ruta con seguridad y persistencia reales sobre HSQLDB; sesión obligatoria, planes propios y participaciones privadas, exclusión de planes ajenos e invitaciones pendientes, aislamiento del usuario autenticado, navegación, estados vacíos, ausencia de hora y escape de HTML. |
| `PlanPlaceTimePinnedTest` | Regresión del error de inicio con `time_pinned` nulo y cambios de la marca de horario fijo. |
| `CalendarE2E` | Acceso desde el avatar, navegación mensual y diaria, apertura del detalle, viewports 320×568, 390×844 y 1280×720, controles táctiles, ausencia de desborde horizontal y agenda con muchos planes. |

Los casos parametrizados usan `junit-jupiter-params` con la misma versión de JUnit
del proyecto, únicamente en el alcance de tests.

```powershell
mvn test '-Dtest=CalendarControllerTest,CalendarIntegrationTest,PlanPlaceTimePinnedTest' '-Djacoco.gate.skip=true'
.\scripts\gate.ps1 e2e=CalendarE2E -Port 8081 -Keep
mvn clean verify --fail-at-end '-DskipITs'
.\scripts\gate.ps1 js -Strict
```

Si la aplicación local ya ocupa el puerto 8080, agregar `-Djetty.port=8082`
a la verificación Maven: el objetivo `verify` inicia Jetty incluso con `skipITs`.
Esto evita detener el servidor de desarrollo.

La ejecución parcial omite únicamente el umbral global de cobertura; la verificación
completa usa los mismos controles del job de backend de GitHub sin omitir umbrales.
Los E2E usan `valhalla_e2e`, separada de los datos locales de la aplicación.

Resultado de la verificación del 29 de octubre de 2026: 1035 tests de backend
sin fallos, incluidos 26 casos del controlador, 8 de integración del calendario
y 3 de regresión de `time_pinned`; los umbrales de cobertura se cumplen.
`CalendarController` tiene 100 % de cobertura de líneas y ramas. Los 3 E2E del
calendario pasan en Chromium. El contrato de JavaScript estricto pasa con 0 fallos.
La verificación Maven completa terminó con `BUILD SUCCESS` usando el puerto 8082.

## Revisión de GitHub Actions

El [PR #57](https://github.com/unlam-tdw/valhalla/pull/57) registró una
[ejecución fallida](https://github.com/unlam-tdw/valhalla/actions/runs/40927792866)
para el commit `a46cfc0`: el contrato de JavaScript pasó, falló el build de backend
y los E2E quedaron sin ejecutar por su dependencia del backend.

La API pública permitió leer los jobs y sus anotaciones. La descarga del log
completo devolvió HTTP 403 por requerir autenticación, no disponible en esta sesión.
La reproducción local de `mvn validate` encontró dos violaciones de PMD en
`CalendarController`: `CyclomaticComplexity` y `LooseCoupling`. Se separó la
validación de mes y fecha, y el modelo se declaró mediante `Map`, conservando
el comportamiento. No se cambiaron los umbrales ni se desactivaron reglas de CI.

## Recuperación del inicio con datos anteriores al planificador

Si el inicio falla con `Can not set boolean field PlanPlace.timePinned to null value`,
hay paradas anteriores al agregado de `time_pinned` que tienen esa columna nula.
El modelo interpreta esos valores como `false`. Para normalizar la base existente,
ejecutar desde PowerShell:

```powershell
Get-Content scripts/fix-time-pinned.sql | docker compose exec -T postgres sh -c 'exec psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
docker compose restart app
```

El script conserva los datos, completa únicamente los valores nulos y establece
`DEFAULT false` y `NOT NULL`. Puede ejecutarse nuevamente sin cambiar valores ya definidos.
