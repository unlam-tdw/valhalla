/**
 * The plan detail view: map, itinerary and catalogue, extracted from
 * WEB-INF/templates/pages/plans/detail.html (docs/guide-js.md R4).
 *
 * The page hands this module four values through `window.PLAN_DETAIL_SEED` (see the
 * bootstrap script in the template): a module cannot read Thymeleaf, and the two lists are
 * object graphs that a data-* attribute cannot carry without a serializer.
 */
import { apiFetch } from '../lib/api.js';
import { initBudgetDescription } from '../lib/plan-budget.js';
import { el } from '../lib/dom.js';
import {
    effectiveMinutes,
    formatClock,
    formatDate,
    formatDuration,
    haversineKm,
    scheduleStops
} from '../lib/plan-schedule.js';
import {
    buildMarker,
    categoryIconHtml,
    categoryIconNode,
    directionsLink,
    directionsUrl
} from '../lib/places-map.js';

const { createApp, ref, computed, onMounted, nextTick, watch } = Vue;
const seed = window.PLAN_DETAIL_SEED || {};
const itineraryUrl = seed.itineraryUrl || '';
const catalogUrl = seed.catalogUrl || '';
// [REO] Los cuatro valores del planificador (29-REO D-3): el preview del horario durante el
// drag los usa en el espejo `scheduleStops`. Sin hora de inicio no hay cadena que derivar,
// y `autoSchedule` apagado manda el mismo mensaje que en el servidor: no se recalcula nada.
const eventTime = seed.eventTime || '';
const dayEndTime = seed.dayEndTime || '';
const transportMode = seed.transportMode || '';
const autoScheduleOn = seed.autoSchedule !== false;
const estimatedPrices = seed.estimatedPrices || {};
const budgetEstimated = seed.budgetEstimated === true;
const budgetMaximum = seed.budgetMaximum == null ? null : Number(seed.budgetMaximum);
// [SURPRISE] Cuantas personas se estimaron: los precios son por persona y la vista los
// multiplica por este valor (null = no se indico, se muestra el precio por persona solo).
const budgetPeople = Number(seed.budgetPeople) || null;
const canReorderStops = seed.editable === true;

// [SURPRISE] Costo por persona de cada tramo de traslado, el espejo de PlaceCostEstimator:
// a pie y en bici no se gasta; colectivo y auto, si. El modo viaja como string en el seed y
// en las entradas de la API.
const TRAVEL_COSTS = { WALK: 0, BIKE: 0, BUS: 750, CAR: 2000 };
const travelCostOf = (mode) => TRAVEL_COSTS[mode] || 0;

/** An itinerary entry and a catalogue place arrive in different shapes; both end here. */
const parsePlace = (p) => {
    if (!p) return null;
    const target = p.place ? p.place : p;
    const category = target.category || 'DEFAULT';
    const estimatedCost = Number(
        p.estimatedCost ?? target.estimatedCost ?? estimatedPrices[category] ?? estimatedPrices.DEFAULT ?? 2500
    );
    return {
        entryId: p.entryId || null,
        description: p.description,
        visitDate: p.visitDate,
        visitTime: p.visitTime,
        // [SCH] El detalle dibuja el rango con la duración y marca con el pin lo que el usuario
        // fijó a mano. El catálogo (/api/places) manda un Place y no trae ninguno de los tres:
        // quedan neutros para que una parada del catálogo no herede un horario que no tiene.
        durationMinutes: p.durationMinutes ?? null,
        transportModeToNext: p.transportModeToNext || '',
        travelMinutesToNext: p.travelMinutesToNext ?? null,
        timePinned: p.timePinned === true,
        estimatedCost,
        id: target.id || p.placeId,
        name: target.name || 'Sin nombre',
        category,
        // [MAPS] La direccion viaja en el modelo para armar el "Como llegar" de la ficha,
        // el popup y la lista del itinerario (el dataset trae direccion y coordenada
        // desfasadas, asi que el link usa el texto).
        address: target.address,
        latitude: target.latitude ?? target.lat,
        longitude: target.longitude ?? target.lng
    };
};

const money = (value) => Number(value || 0).toLocaleString('es-AR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
});

/* Precio visible al usuario: los lugares gratuitos dicen "Gratis" y el resto viaja con el
   simbolo. Cuando se indico gente, multiplica: "$12.000 × 2". El select de alta y las paradas
   del itinerario usan la misma etiqueta. */
const costLabel = (value) => {
    const cost = Number(value) || 0;
    const label = cost === 0 ? 'Gratis' : '$' + money(cost);
    return budgetPeople > 1 ? label + ' × ' + budgetPeople : label;
};

/* Fallback del traslado cuando no hay ventana del día para derivar el horario: misma cuenta
   que el espejo `travelMinutes` de plan-schedule (override, piso de 5, techo de 90). */
function travelMinutesOf(from, to) {
    const override = parseInt(from?.travelMinutesToNext, 10);
    if (Number.isFinite(override)) return Math.max(1, override);
    if (from?.place?.latitude == null || from?.place?.longitude == null
        || to?.place?.latitude == null || to?.place?.longitude == null) return 15;
    const mode = from?.transportModeToNext || transportMode || 'WALK';
    const speeds = { WALK: 4.5, BIKE: 14, BUS: 16, CAR: 22 };
    const speed = speeds[mode] || 4.5;
    const distanceKm = haversineKm(from, to) * 1.4;
    const minutes = Math.round(((distanceKm / speed) * 60) / 5) * 5;
    return Math.max(5, Math.min(90, minutes));
}

createApp({
    setup() {
        const allPlaces = ref((seed.allPlaces || []).map(parsePlace).filter(p => p && p.id));
        const planPlaces = ref((seed.itineraryPlaces || []).map(parsePlace).filter(p => p && p.id));
        // [SCH] Fecha de la jornada inicial: una parada que cae en ella no repite la fecha en la
        // lista, y las que se corren a otro día sí la muestran.
        const planDate = ref(seed.planDate || '');

        const selectedPlaceId = ref(null);
        const selectedEntry = ref(null);
        const requestError = ref('');
        const requestNotice = ref('');
        const saving = ref(false);
        const placeDialog = ref(null);
        // [STD] La configuración del plan y los miembros baneados son cards centradas: se abren
        // desde la cabecera y desde Participantes, sin navegar ni sumar scroll a la página.
        const configDialog = ref(null);
        const openConfig = () => configDialog.value.showModal();
        const bannedDialog = ref(null);
        // [STD-009] bannedTrigger devuelve el foco al botón de Participantes al cerrar el diálogo.
        const bannedTrigger = ref(null);
        const openBanned = () => bannedDialog.value.showModal();
        // [STD] Solicitudes y Participantes son cards resumen: el contenido completo se abre en
        // una card centrada (la notificación de solicitudes abre la misma card).
        const participantsDialog = ref(null);
        const openParticipants = () => participantsDialog.value.showModal();
        const requestsDialog = ref(null);
        const openRequests = () => requestsDialog.value.showModal();
        // [STD] 25-STD: los dos paneles plegables (invitar, agregar lugar) arrancan cerrados
        // para que la vista base no pinte formularios ni consuma scroll.
        const inviteOpen = ref(false);
        const addPlaceOpen = ref(false);

        // Leaflet owns these three and the template never reads them, so they are not refs.
        let map = null;
        let markers = [];
        let routeLine = null;

        // [REO] Estado del reorden por manija (29-REO D-1): refs porque la plantilla marca la
        // fila levantada y la ranura de destino mientras el drag vive. Los dos índices son
        // planos (posiciones de planPlaces); los grupos de día son presentación.
        const draggingIndex = ref(null);
        const hoverSlot = ref(null);
        const itineraryList = ref(null);
        // [REO] D-12: el texto que la plantilla lee en el aria-live off-screen; vacío no anuncia.
        const dragAnnouncement = ref('');
        // [REO] D-7: un drag por teclado necesita devolver el foco a la manija al soltar, porque
        // Vue pudo remontar la fila si cambió de grupo de día (D-11).
        let keyboardDrag = false;

        const availablePlaces = computed(() => {
            const addedIds = planPlaces.value.map(p => p.id);
            return allPlaces.value.filter(p => !addedIds.includes(p.id));
        });

        const itineraryRequest = async (path = '', options = {}) => {
            const response = await apiFetch(itineraryUrl + path, options);
            const text = await response.text();
            return text ? JSON.parse(text) : null;
        };

        /* El servidor recalcula el plan entero después de cada cambio de horario, de un reorden o
           de una baja: copiar solo los cuatro campos del horario sobre las paradas ya presentes
           evita reemplazar el array y perder el orden optimista de la UI. */
        const refreshSchedule = async () => {
            const entries = await itineraryRequest();
            if (!Array.isArray(entries)) return;
            const byId = new Map(planPlaces.value.map(p => [p.entryId, p]));
            const ordered = [];
            entries.forEach((entry) => {
                const item = byId.get(entry.id);
                if (!item) return;
                item.visitDate = entry.visitDate;
                item.visitTime = entry.visitTime;
                item.durationMinutes = entry.durationMinutes;
                item.timePinned = entry.timePinned === true;
                item.travelMinutesToNext = entry.travelMinutesToNext ?? null;
                item.transportModeToNext = entry.transportModeToNext || '';
                if (entry.estimatedCost != null) item.estimatedCost = Number(entry.estimatedCost);
                ordered.push(item);
            });
            // Conserva el orden devuelto por el servidor y agrega cualquier entrada local no presente.
            if (ordered.length === planPlaces.value.length) {
                planPlaces.value = ordered;
            } else if (ordered.length > 0) {
                const seen = new Set(ordered.map(p => p.entryId));
                planPlaces.value = ordered.concat(planPlaces.value.filter(p => !seen.has(p.entryId)));
            }
        };

        /** `dd/mm` para el encabezado del día, igual que el creador. */
        const shortDate = (isoDate) => isoDate.slice(8, 10) + '/' + isoDate.slice(5, 7);

        /** Minutos del día de un `HH:mm`; `null` cuando el valor no es una hora. */
        const clockMinutes = (value) => {
            if (!value) return null;
            const [hours, minutes] = value.split(':').map(Number);
            return hours * 60 + minutes;
        };

        /** Minutos del día en que termina la visita; `null` sin hora o sin duración. */
        const endMinutes = (entry) => {
            const start = clockMinutes(entry.visitTime);
            if (start === null || !entry.durationMinutes) return null;
            return start + entry.durationMinutes;
        };

        /* Lo que la parada muestra de horario: rango, duración y fecha, ya formateados. La ficha
           del lugar y la lista usan la misma cuenta, así que la comparten. */
        const describeStop = (item) => {
            const start = clockMinutes(item.visitTime);
            const end = endMinutes(item);
            return {
                timeRange: start === null
                    ? ''
                    : end !== null
                        ? formatClock(start) + ' – ' + formatClock(end)
                        : formatClock(start),
                durationLabel: item.durationMinutes ? formatDuration(item.durationMinutes) : '',
                dateLabel: item.visitDate && item.visitDate !== planDate.value
                    ? formatDate(item.visitDate)
                    : ''
            };
        };

        /* [REO] La parada con la forma que el espejo del planificador espera (29-REO D-3): las
           coordenadas y la categoría anidadas en `place`, la duración y el traslado propio
           arriba y el pin del horario tal como viaja en el seed. */
        const scheduleStopOf = (item) => ({
            place: { latitude: item.latitude, longitude: item.longitude, category: item.category },
            durationMinutes: item.durationMinutes,
            transportModeToNext: item.transportModeToNext,
            travelMinutesToNext: item.travelMinutesToNext,
            timePinned: item.timePinned,
            visitTime: item.visitTime,
            visitDate: item.visitDate
        });

        /* [REO] D-2: el orden visible es el de planPlaces con la parada levantada ya insertada
           en la ranura bajo el puntero. Devuelve pares (item, índice plano) porque la
           numeración visible es la posición del reacomodo, mientras que eliminar sigue
           necesitando el índice del array. */
        const displayPlaces = computed(() => {
            const pairs = planPlaces.value.map((item, index) => ({ item, index }));
            if (draggingIndex.value === null || hoverSlot.value === null) return pairs;
            const next = pairs.slice();
            const [moved] = next.splice(draggingIndex.value, 1);
            next.splice(hoverSlot.value, 0, moved);
            return next;
        });

        /* Resumen del recorrido en vivo (paradas · minutos · km), el mismo que leía el chip del
           servidor: ahora se recalcula también al agregar, sacar o reordenar paradas, y el drag
           lo muestra como preview mientras arrastra. */
        const itineraryStats = computed(() => {
            const stops = displayPlaces.value.map((pair) => scheduleStopOf(pair.item));
            let totalMinutes = 0;
            let schedule = [];
            if (autoScheduleOn && eventTime && planDate.value) {
                schedule = scheduleStops({
                    planDate: planDate.value,
                    startTime: eventTime,
                    dayEnd: dayEndTime,
                    mode: transportMode,
                    stops
                });
            }
            if (schedule.length) {
                schedule.forEach((slot) => { totalMinutes += slot.minutes + slot.legMinutes; });
            } else {
                stops.forEach((stop, index) => {
                    totalMinutes += effectiveMinutes(stop);
                    if (index + 1 < stops.length) {
                        totalMinutes += travelMinutesOf(stop, stops[index + 1]);
                    }
                });
            }
            let totalKm = 0;
            for (let i = 0; i < stops.length - 1; i++) {
                const from = stops[i];
                const to = stops[i + 1];
                if (from.place.latitude != null && from.place.longitude != null
                    && to.place.latitude != null && to.place.longitude != null) {
                    totalKm += haversineKm(from, to);
                }
            }
            return {
                count: stops.length,
                timeLabel: formatDuration(totalMinutes),
                kmLabel: totalKm > 0 ? totalKm.toFixed(1).replace('.', ',') + ' km' : ''
            };
        });

        const itineraryHeadline = computed(() => {
            const stats = itineraryStats.value;
            if (!stats.count) return '';
            return stats.count + (stats.count === 1 ? ' parada' : ' paradas')
                + ' · ' + stats.timeLabel
                + (stats.kmLabel ? ' · ' + stats.kmLabel : '');
        });

        const dragSummary = computed(() => {
            if (draggingIndex.value === null || hoverSlot.value === null) return null;
            return itineraryStats.value;
        });

        /* [REO] D-3/D-5: el preview del horario vive solo durante el arrastre y solo cuando
           autoSchedule está activo (con él apagado el servidor tampoco recalcula: las filas se
           reacomodan y las horas quedan como están). Es computed sobre displayPlaces y nunca
           muta planPlaces; con el espejo divergente gana el servidor al refrescar (D-4). */
        const previewSchedule = computed(() => {
            const previewing = draggingIndex.value !== null && hoverSlot.value !== null;
            if (!previewing || !autoScheduleOn || !eventTime || !planDate.value) return null;
            return scheduleStops({
                planDate: planDate.value,
                startTime: eventTime,
                dayEnd: dayEndTime,
                mode: transportMode,
                stops: displayPlaces.value.map((pair) => scheduleStopOf(pair.item))
            });
        });

        /* [REO] Las paradas del reacomodo: el item del preview cuando lo hay (horas derivadas
           del espejo), si no el item real del servidor. Cada una lleva su índice plano (para
           eliminar), su posición visible (numeración) y las marcas del drag. */
        const displayStops = computed(() => {
            const schedule = previewSchedule.value;
            return displayPlaces.value.map((pair, position) => {
                const slot = schedule ? schedule[position] : null;
                const item = slot
                    ? { ...pair.item, visitDate: slot.date, visitTime: formatClock(slot.startMinutes) }
                    : pair.item;
                const isDragged = draggingIndex.value !== null && pair.index === draggingIndex.value;
                return {
                    item,
                    planIndex: pair.index,
                    position,
                    isDragged,
                    dimmed: draggingIndex.value !== null && !isDragged,
                    ...describeStop(item)
                };
            });
        });

        /* Las paradas agrupadas por la fecha que les tocó, en el orden del itinerario. [REO]
           Deriva del reacomodo: durante el drag el agrupado por día sale del preview (D-11 y
           AC-12), así que mover una parada a otra jornada la re-agrupa en vivo. La numeración
           es global (la misma que los marcadores del mapa) y viaja como posición visible. */
        const dayGroups = computed(() => {
            const groups = [];
            displayStops.value.forEach((stop) => {
                const date = stop.item.visitDate || null;
                let group = groups[groups.length - 1];
                if (!group || group.date !== date) {
                    group = {
                        key: 'day-' + groups.length + (date || ''),
                        date,
                        label: 'Día ' + (groups.length + 1) + (date ? ' · ' + shortDate(date) : ''),
                        stops: []
                    };
                    groups.push(group);
                }
                group.stops.push(stop);
            });
            return groups;
        });

        const dayCount = computed(() => dayGroups.value.length);

        /* [SURPRISE] Costo por persona de todo el recorrido en vivo: el precio de cada lugar
           mas el traslado de cada tramo hacia la parada siguiente (el ultimo no tiene destino,
           asi que no suma). El servidor valida el tope al agregar con la misma cuenta; acá se
           muestra y se avisa cuando queda por encima del presupuesto. */
        const estimatedTotal = computed(() => {
            const stops = planPlaces.value;
            let total = 0;
            for (let i = 0; i < stops.length; i++) {
                total += Number(stops[i].estimatedCost) || 0;
                if (i + 1 < stops.length) {
                    total += travelCostOf(stops[i].transportModeToNext);
                }
            }
            return total;
        });

        const overBudgetAmount = computed(() => {
            if (!budgetEstimated || budgetMaximum == null) return 0;
            return Math.max(0, estimatedTotal.value - budgetMaximum);
        });

        /* El chip ITINERARIO arranca server-rendered y Vue lo refresca escribiendo el texto
           sobre el mismo nodo (no v-text: el th:text deja hijos y el template no compila). */
        const syncItineraryHeadline = () => {
            const chip = document.querySelector('.plan-chip--wide .plan-chip__value');
            if (chip) chip.textContent = itineraryHeadline.value;
        };

        const syncEstimatedCost = () => {
            const line = document.getElementById('itinerary-estimate');
            const cost = document.getElementById('itinerary-estimated-cost');
            const over = document.getElementById('itinerary-over-budget');
            const overAmount = document.getElementById('itinerary-over-amount');
            if (!line || !cost) return;
            if (planPlaces.value.length === 0) {
                line.hidden = true;
                return;
            }
            line.hidden = false;
            cost.textContent = money(estimatedTotal.value);
            // El multiplicador de personas solo viaja si el server lo renderizo (gente > 1).
            const peopleTotal = document.getElementById('itinerary-estimated-total');
            if (peopleTotal) peopleTotal.textContent = money(estimatedTotal.value * (budgetPeople || 1));
            syncItineraryHeadline();
            const amount = overBudgetAmount.value;
            if (over) over.hidden = !(amount > 0);
            if (overAmount) overAmount.textContent = money(amount);
        };

        const getCategoryLabel = (category) => {
            const labels = { RESTAURANT: 'Restaurante', BAR: 'Bar', CAFE: 'Café', MUSEUM: 'Museo', PARK: 'Parque', SHOPPING: 'Compras', NIGHTLIFE: 'Vida nocturna', CULTURE: 'Cultura', SPORT: 'Deporte', OTHER: 'Otro' };
            return labels[category] || 'Lugar';
        };

        /* A Lucide icon as markup for the template's v-html slots. Trusted: our own SVG.
           The template names icons the way HTML does ("map-pinned", "trash-2"), while
           `lucide.icons` keys are PascalCase: normalise before the lookup, or Lucide builds
           an empty <svg> and the icon is silently blank. */
        const iconHtml = (name, size = 16) => {
            const key = name.replace(/(?:^|-)([a-z0-9])/g, (_, letter) => letter.toUpperCase());
            return lucide.createElement(lucide.icons[key] || lucide.icons.MapPin, {
                width: size,
                height: size,
                'aria-hidden': 'true'
            }).outerHTML;
        };

        const renderMarkers = (focusPlaceId = null, orderedPlaces = null) => {
            markers.forEach(m => map.removeLayer(m));
            markers = [];
            if (routeLine) {
                map.removeLayer(routeLine);
                routeLine = null;
            }

            const ordered = Array.isArray(orderedPlaces) ? orderedPlaces : planPlaces.value;
            const isSinglePlace = ordered.length === 1;
            const routeLatLngs = [];
            const bounds = [];

            if (allPlaces.value.length > 0) {
                allPlaces.value.forEach((place) => {
                    if (place.latitude && place.longitude) {
                        const itineraryIndex = ordered.findIndex(p => p.id === place.id);
                        const isInPlan = itineraryIndex !== -1;

                        const categoryClass = 'cat-' + place.category;
                        const isNumbered = isInPlan && !isSinglePlace;

                        let activeClass = '';
                        if (isInPlan) {
                            activeClass = isSinglePlace ? 'single-place-pulse' : 'in-itinerary';
                        }

                        const badge = el(
                            'div',
                            { class: 'custom-map-icon ' + categoryClass + ' ' + activeClass },
                            [isNumbered
                                ? el('span', { text: String(itineraryIndex + 1) })
                                : categoryIconNode(place.category, 15)]
                        );

                        /* Leaflet parses a String popup as HTML, and both the name and the
                           category come from the catalogue: the popup is a node, never markup.
                           R7, and the reason lib/dom.js exists. */
                        let head;
                        if (isInPlan && isSinglePlace) {
                            head = [
                                el('b', { text: '📍 Único lugar seleccionado:' }),
                                el('br'),
                                el('strong', { text: place.name })
                            ];
                        } else if (isInPlan) {
                            head = [
                                el('b', { text: 'Paso ' + (itineraryIndex + 1) + ': ' + place.name }),
                                el('br'),
                                el('small', { class: 'text-success', text: '✔ En el itinerario' })
                            ];
                        } else {
                            head = [el('b', { text: place.name }), el('br'),
                                el('small', { text: place.category })];
                        }

                        const popup = el('div', {}, [
                            ...head,
                            // [MAPS] Como llegar desde el propio popup del mapa: usa la
                            // direccion de texto (no la coordenada, que el dataset trae
                            // desfasada), con el nombre delante para desambiguar; mismo
                            // criterio que el resto.
                            el('br'),
                            directionsLink(place, { class: 'text-primary underline-offset-4 text-decoration-none fw-medium' })
                        ]);

                        const marker = buildMarker(place, { badge, size: isNumbered ? 32 : 30 })
                            .addTo(map)
                            .bindPopup(popup);
                        if (isNumbered) {
                            marker.setZIndexOffset(500 + itineraryIndex);
                        }

                        markers.push(marker);
                        bounds.push([place.latitude, place.longitude]);

                        if ((focusPlaceId && place.id === focusPlaceId) || (isSinglePlace && isInPlan && focusPlaceId)) {
                            map.flyTo([place.latitude, place.longitude], 13, { duration: 1.0 });
                            setTimeout(() => marker.openPopup(), 1000);
                        }
                    }
                });
            }

            if (ordered.length > 1) {
                ordered.forEach(place => {
                    if (place.latitude && place.longitude) {
                        routeLatLngs.push([place.latitude, place.longitude]);
                    }
                });

                routeLine = L.polyline(routeLatLngs, {
                    color: '#0d6efd',
                    weight: 3,
                    opacity: 0.8,
                    dashArray: '6, 6'
                }).addTo(map);
            }

            if (!focusPlaceId && bounds.length > 0) {
                map.fitBounds(bounds, { padding: [60, 60] });
            }
        };

        const initMap = () => {
            map = L.map('map').setView([-34.59, -58.40], 12);

            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; OpenStreetMap'
            }).addTo(map);

            // [STD-VB] El mapa ya no mide 480 px fijos: llena el stage, cuya altura cambia con
            // el viewport. Sin invalidateSize Leaflet conserva el tamaño viejo y las tiles
            // quedan corridas (mismo patrón que places-list.js).
            new ResizeObserver(() => map.invalidateSize({ pan: false })).observe(map.getContainer());
            requestAnimationFrame(() => map.invalidateSize({ pan: false }));

            renderMarkers();
        };

        const showPlace = async (item) => {
            requestError.value = '';
            requestNotice.value = '';
            try {
                const entries = await itineraryRequest();
                const entry = entries.find(candidate => candidate.id === item.entryId);
                if (!entry) throw new Error('Este lugar ya no pertenece al plan. Actualiza la página.');
                // La ficha muestra el horario ya formateado: el rango, la duración y el día se
                // calculan acá, igual que en la lista, y no en el template.
                selectedEntry.value = { ...entry, ...describeStop(entry) };
                // [MERGE] El spread de describeStop (feature) y el nextTick de main (DIS-B04) no
                // se pisan: el nextTick va después de montar selectedEntry, que es lo que el
                // <template v-if> necesita para renderizar antes de showModal.
                await nextTick();
                placeDialog.value.showModal();
            } catch (error) { requestError.value = error.message || 'No se pudo consultar el lugar.'; }
        };

        /* El PUT reemplaza los campos del horario, así que el payload viaja completo: el
           campo editado y los otros tal como están. Los dos de fecha y hora solo se repiten
           si la parada está fijada a mano o si se está editando justamente uno de ellos; si no,
           van en null para que cambiar solo la duración no convierta una parada derivada en una
           fijada. El traslado fijo no tiene pin: viaja como está, y vaciarlo lo devuelve al
           cálculo (null en el servidor). */
        const updateEntry = async (entry, field, value) => {
            if (saving.value || !entry) return;
            saving.value = true;
            requestError.value = '';
            requestNotice.value = '';
            const editsSchedule = field === 'visitDate' || field === 'visitTime';
            const keepsSchedule = entry.timePinned || editsSchedule;
            const payload = {
                durationMinutes: field === 'durationMinutes'
                    ? (value === '' ? null : Number(value))
                    : (entry.durationMinutes ?? null),
                transportModeToNext: entry.transportModeToNext || null,
                travelMinutesToNext: field === 'travelMinutesToNext'
                    ? (value === '' ? null : Number(value))
                    : (entry.travelMinutesToNext ?? null),
                visitDate: field === 'visitDate'
                    ? (value || null)
                    : (keepsSchedule ? (entry.visitDate || null) : null),
                visitTime: field === 'visitTime'
                    ? (value || null)
                    : (keepsSchedule ? (entry.visitTime || null) : null)
            };
            try {
                const updated = await itineraryRequest('/' + entry.id, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });
                Object.assign(entry, updated);
                // Fijar una parada corre las siguientes: sin este refresco la lista muestra las
                // horas viejas hasta recargar la página.
                await refreshSchedule();
                Object.assign(entry, describeStop(entry));
                syncEstimatedCost();
            } catch (error) { requestError.value = error.message || 'No se pudo actualizar el lugar.'; }
            finally { saving.value = false; }
        };

        const addPlace = async () => {
            if (saving.value || !selectedPlaceId.value) return;
            saving.value = true;
            requestError.value = '';
            requestNotice.value = '';
            const placeObj = allPlaces.value.find(p => p.id === selectedPlaceId.value);
            try {
                if (!placeObj) throw new Error('Elegí un lugar para agregar.');
                const entry = await itineraryRequest('?placeId=' + placeObj.id, { method: 'POST' });
                const addedId = placeObj.id;
                planPlaces.value.push({
                    ...placeObj,
                    entryId: entry.id,
                    description: entry.description,
                    visitDate: entry.visitDate,
                    visitTime: entry.visitTime,
                    durationMinutes: entry.durationMinutes,
                    timePinned: entry.timePinned === true,
                    transportModeToNext: entry.transportModeToNext || '',
                    travelMinutesToNext: entry.travelMinutesToNext ?? null,
                    estimatedCost: Number(entry.estimatedCost ?? placeObj.estimatedCost ?? 0)
                });
                selectedPlaceId.value = null;
                renderMarkers(addedId);
                await refreshSchedule();
                syncEstimatedCost();
                if (overBudgetAmount.value > 0) {
                    requestNotice.value = 'Se agregó «' + placeObj.name + '». El itinerario queda $'
                        + money(overBudgetAmount.value)
                        + ' sobre tu presupuesto de $' + money(budgetMaximum) + '.';
                }
            } catch (error) {
                // El servidor rechaza el alta cuando el nuevo total pasa el presupuesto por más
                // de $2.000: el plan queda como estaba y el mensaje viaja al alert rojo.
                requestError.value = error.message || 'No se pudo agregar el lugar.';
            } finally {
                saving.value = false;
            }
        };

        const removePlace = async (index) => {
            if (saving.value) return;
            saving.value = true;
            requestError.value = '';
            requestNotice.value = '';
            try {
                await itineraryRequest('/' + planPlaces.value[index].entryId, { method: 'DELETE' });
                planPlaces.value.splice(index, 1);
                renderMarkers();
                // El servidor recalcula el plan sin la parada borrada: las que quedan cambian de
                // hora y el refresco las trae.
                await refreshSchedule();
                syncEstimatedCost();
            } catch (error) { requestError.value = error.message || 'No se pudo eliminar el lugar.'; }
            finally { saving.value = false; }
        };

        /* [REO] Ranura de inserción bajo el puntero (29-REO, variante B): la primera fila cuyo
           punto medio queda debajo del puntero, o la última si el puntero pasa el final. Las
           filas de todos los grupos de día en orden del DOM son el orden del itinerario, y su
           data-stop-row (entryId) es solo la clave de la fila, no un índice. */
        const slotFromPointer = (clientY) => {
            const panel = itineraryList.value;
            const rows = panel ? panel.querySelectorAll('[data-stop-row]') : [];
            for (let i = 0; i < rows.length; i++) {
                const rect = rows[i].getBoundingClientRect();
                if (clientY < rect.top + rect.height / 2) return i;
            }
            return Math.max(0, rows.length - 1);
        };

        /* [REO] D-1: la manija es el único origen del arrastre. pointerdown sobre el botón más
           setPointerCapture encamina los movimientos a la manija aunque el puntero salga de la
           card, y es la base del táctil (D-8: touch-action none solo acá). saving vuelve la
           manija inerte: sin drag mientras hay un guardado en curso. pointercancel restaura
           como un drop cancelado. */
        const onHandleDown = (event, index) => {
            if (!canReorderStops || saving.value || event.button > 0) return;
            // Permite arrastrar desde la fila/manija, pero no al interactuar con controles.
            const interactive = event.target && event.target.closest ? event.target.closest('a, button, input, select, textarea, label') : null;
            const isHandle = event.target && event.target.closest ? event.target.closest('.drag-handle') : null;
            if (interactive && !isHandle) return;
            event.preventDefault();
            event.stopPropagation();
            draggingIndex.value = index;
            hoverSlot.value = index;
            const handle = event.currentTarget;
            /* [REO] La captura es lo que mantiene vivo el drag cuando el puntero sale de la
               card. Con un puntero real siempre está activa al momento del down; con el
               PointerEvent sintético del E2E no lo está y setPointerCapture rechaza, lo que
               mataría el pointerdown entero. Sin captura el drag igual funciona acá: los
               listeners viven en la manija y el dispatch va sobre ella. */
            try { handle.setPointerCapture(event.pointerId); } catch (error) { /* sin captura */ }
            const onMove = (moveEvent) => { hoverSlot.value = slotFromPointer(moveEvent.clientY); };
            const detach = () => {
                handle.removeEventListener('pointermove', onMove);
                handle.removeEventListener('pointerup', onUp);
                handle.removeEventListener('pointercancel', onCancel);
            };
            const onUp = () => { detach(); finishDrag(); };
            const onCancel = () => {
                detach();
                draggingIndex.value = null;
                hoverSlot.value = null;
            };
            handle.addEventListener('pointermove', onMove);
            handle.addEventListener('pointerup', onUp);
            handle.addEventListener('pointercancel', onCancel);
        };

        /* [REO] D-12: texto del aria-live mientras se arrastra: nombre de la parada (el lector
             lo nombra, la posición sola no dice qué se movió), ranura destino y total. El hint
             extra solo lo suma el teclado (instrucciones al levantar). */
        const announcePosition = (hint = '') => {
            const from = draggingIndex.value;
            if (from === null) return;
            const item = planPlaces.value[from];
            dragAnnouncement.value = 'Parada ' + (item ? item.name : '')
                + ', posición ' + (hoverSlot.value + 1) + ' de ' + planPlaces.value.length + '.' + hint;
        };

        /* [REO] D-7: al soltar por teclado el foco vuelve a la manija de la fila movida: sin
             esto queda en el body y el siguiente Enter no sabe de qué parada habla. Vue pudo
             remontar la fila (cambió de grupo de día, D-11), así que se busca por data-stop-row
             después del commit y del re-render. */
        const refocusHandle = async (entryId) => {
            if (!entryId) return;
            await nextTick();
            const panel = itineraryList.value;
            const handle = panel
                && panel.querySelector('[data-stop-row="' + entryId + '"] .drag-handle');
            if (handle) handle.focus();
        };

        /* [REO] D-2/D-7: el drop es el mismo commit para puntero y teclado; solo cambia el
             anuncio ("soltada en la posición N") y que el teclado devuelve el foco a la manija
             movida, también cuando no hubo movimiento (from === to). */
        const finishDrag = async () => {
            const from = draggingIndex.value;
            const to = hoverSlot.value;
            const movedEntryId = from !== null && planPlaces.value[from]
                ? planPlaces.value[from].entryId : null;
            const wasKeyboard = keyboardDrag;
            keyboardDrag = false;
            draggingIndex.value = null;
            hoverSlot.value = null;
            if (from === null || to === null || from === to) {
                if (wasKeyboard) await refocusHandle(movedEntryId);
                return;
            }
            dragAnnouncement.value = 'Parada soltada en la posición ' + (to + 1) + '.';
            if (wasKeyboard) {
                await commitReorder(from, to);
                await refocusHandle(movedEntryId);
                return;
            }
            commitReorder(from, to);
        };

        /* [REO] D-7: la manija es un botón real, así que el teclado vive en su keydown: Enter
             o Espacio levanta y suelta, flechas mueven la ranura (limitada al total de
             paradas) y Escape cancela. Sin drag en curso las flechas y Escape no hacen nada
             (la manija sigue siendo un botón normal), y saving ignora todo porque la manija
             ya está deshabilitada. */
        const onHandleKeydown = (event, index) => {
            if (saving.value) return;
            if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                if (draggingIndex.value === null) {
                    keyboardDrag = true;
                    draggingIndex.value = index;
                    hoverSlot.value = index;
                    announcePosition(' Usá las flechas para moverla y Enter para soltar.');
                } else {
                    finishDrag();
                }
                return;
            }
            if (draggingIndex.value === null) return;
            if (event.key === 'ArrowUp' || event.key === 'ArrowDown') {
                event.preventDefault();
                const next = Math.min(Math.max(hoverSlot.value + (event.key === 'ArrowUp' ? -1 : 1), 0), planPlaces.value.length - 1);
                if (next !== hoverSlot.value) {
                    hoverSlot.value = next;
                    announcePosition();
                }
                return;
            }
            if (event.key === 'Escape') {
                keyboardDrag = false;
                draggingIndex.value = null;
                hoverSlot.value = null;
                dragAnnouncement.value = 'Reorden cancelado.';
            }
        };

        /* [REO] D-2/D-11: FLIP barato para el commit del drop: mide los tops de las filas que
           persisten, muta el orden y anima el delta al volver. Una fila que cambia de grupo de
           día se remonta (clave nueva en otro <ol>) y no anima, aceptado por la spec.
           prefers-reduced-motion la apaga (D-12). */
        const flipReorder = async (mutate) => {
            const panel = itineraryList.value;
            const reduceMotion = window.matchMedia
                && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
            if (!panel || reduceMotion) { mutate(); return; }
            const before = new Map();
            panel.querySelectorAll('[data-stop-row]').forEach((row) => {
                before.set(row.dataset.stopRow, row.getBoundingClientRect().top);
            });
            mutate();
            await nextTick();
            panel.querySelectorAll('[data-stop-row]').forEach((row) => {
                const previousTop = before.get(row.dataset.stopRow);
                if (previousTop === null || previousTop === undefined) return;
                const delta = previousTop - row.getBoundingClientRect().top;
                if (!delta) return;
                row.style.transition = 'none';
                row.style.transform = 'translateY(' + delta + 'px)';
                requestAnimationFrame(() => {
                    row.style.transition = 'transform 180ms ease';
                    row.style.transform = '';
                });
            });
        };

        /* [REO] D-4: la persistencia es la de siempre: splice optimista (con FLIP, D-2), POST
           /reorder con los entryId en el orden nuevo y rollback si el servidor falla; el mapa
           se redibuja al soltar (D-10) y refreshSchedule trae después las horas derivadas. */
        const commitReorder = async (from, to) => {
            if (saving.value) return;
            saving.value = true;
            requestError.value = '';
            requestNotice.value = '';
            const previous = [...planPlaces.value];
            await flipReorder(() => {
                const movedItem = planPlaces.value.splice(from, 1)[0];
                planPlaces.value.splice(to, 0, movedItem);
            });
            renderMarkers();
            let reordered = false;
            try {
                await itineraryRequest('/reorder', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(planPlaces.value.map(item => item.entryId)) });
                reordered = true;
            } catch (error) { planPlaces.value = previous; renderMarkers(); requestError.value = error.message || 'No se pudo reordenar.'; }
            finally { saving.value = false; }
            // [SCH] Reordenar vuelve a derivar el horario: el refresco trae las horas nuevas y
            // deja intacto el orden optimista que ya está pintado. Va fuera del try del
            // reorder para que un fallo acá no revierta un orden que el servidor sí guardó.
            if (reordered) {
                try { await refreshSchedule(); } catch (error) { requestError.value = error.message || 'No se pudo actualizar el horario.'; }
                syncEstimatedCost();
            }
        };

        onMounted(async () => {
            if (configDialog.value?.dataset.budgetInvalid === 'true') {
                openConfig();
                document.getElementById('config-presupuesto')?.scrollIntoView({ block: 'nearest' });
            }

            try {
                const response = await apiFetch(catalogUrl);
                const catalog = await response.json();
                allPlaces.value = (catalog || []).map(parsePlace).filter(p => p && p.id);
            } catch (error) {
                allPlaces.value = [];
                requestError.value = error.status === 401
                    ? error.message
                    : 'No se pudo cargar el listado de lugares.';
            }
            initMap();
            syncEstimatedCost();
        });

        watch([estimatedTotal, () => planPlaces.value.length], syncEstimatedCost);
        // Recalcula marcadores y recorrido del mapa cuando cambia el itinerario (agregar,
        // quitar, reordenar o refrescar horarios) para que se vea siempre actualizado.
        watch(planPlaces, () => { if (map) renderMarkers(); }, { deep: true });
        // Durante el arrastre muestra en el mapa el orden de la vista previa; al soltar vuelve al real.
        watch([draggingIndex, hoverSlot], () => {
            if (!map) return;
            if (draggingIndex.value !== null && hoverSlot.value !== null) {
                renderMarkers(null, displayPlaces.value.map(pair => pair.item));
            } else {
                renderMarkers();
            }
        });
        watch(displayPlaces, () => {
            if (!map) return;
            if (draggingIndex.value !== null && hoverSlot.value !== null) {
                renderMarkers(null, displayPlaces.value.map(pair => pair.item));
            }
        }, { deep: true });

        return {
            allPlaces, planPlaces, selectedPlaceId, selectedEntry, requestError, requestNotice, saving,
            inviteOpen, addPlaceOpen,
            configDialog, openConfig, bannedDialog, bannedTrigger, openBanned,
            participantsDialog, openParticipants, requestsDialog, openRequests,
            placeDialog, availablePlaces, dayGroups, dayCount, directionsUrl, itineraryList,
            dragSummary, itineraryHeadline,
            dragAnnouncement, canReorderStops,
            getCategoryLabel, costLabel, iconHtml, categoryIconHtml, showPlace, updateEntry, addPlace,
            removePlace, onHandleDown, onHandleKeydown
        };
    }
}).mount('#plan-app');
initBudgetDescription();
