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
nuevas ni JavaScript adicional.

Este primer alcance es de consulta: no permite crear, editar ni arrastrar
eventos, y no desglosa los días del itinerario de los planes de varios días.
No se agregaron tests automatizados para este prototipo, según lo solicitado.

Verificación básica: compilación de Java con `mvn -Pdev compile -DskipTests`.
La revisión visual en navegador queda pendiente. La actualización de la tarjeta
de Trello queda pendiente porque sus herramientas no están disponibles en esta sesión.

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
