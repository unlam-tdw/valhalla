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

const { createApp, ref, computed, onMounted, nextTick } = Vue;
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

/** An itinerary entry and a catalogue place arrive in different shapes; both end here. */
const parsePlace = (p) => {
    if (!p) return null;
    const target = p.place ? p.place : p;
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
        id: target.id,
        name: target.name || 'Sin nombre',
        category: target.category || 'DEFAULT',
        // [MAPS] La direccion viaja en el modelo para armar el "Como llegar" de la ficha,
        // el popup y la lista del itinerario (el dataset trae direccion y coordenada
        // desfasadas, asi que el link usa el texto).
        address: target.address,
        latitude: target.latitude || target.lat,
        longitude: target.longitude || target.lng
    };
};

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
            try {
                const response = await apiFetch(itineraryUrl + path, options);
                const text = await response.text();
                return text ? JSON.parse(text) : null;
            } catch (error) {
                // The session-expiry sentence is the only one worth passing through;
                // this page owns its copy for everything else.
                if (error.status === 401) throw error;
                console.error(error);
                throw new Error('No se pudo actualizar o consultar el lugar. Intenta nuevamente.');
            }
        };

        /* El servidor recalcula el plan entero después de cada cambio de horario, de un reorden o
           de una baja: copiar solo los cuatro campos del horario sobre las paradas ya presentes
           evita reemplazar el array y perder el orden optimista de la UI. */
        const refreshSchedule = async () => {
            const entries = await itineraryRequest();
            entries.forEach((entry) => {
                const item = planPlaces.value.find(p => p.entryId === entry.id);
                if (!item) return;
                item.visitDate = entry.visitDate;
                item.visitTime = entry.visitTime;
                item.durationMinutes = entry.durationMinutes;
                item.timePinned = entry.timePinned;
            });
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
                // Sin duración no hay fin que calcular, pero la hora de inicio sí existe:
                // mostrarla vale más que declarar el horario "por definir".
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
                        // La posición desambigua: un pin puede volver a una fecha ya usada un
                        // grupo antes (1 → 2 → 1) y Vue necesita claves únicas dentro del v-for.
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

        /* [REO] D-9: resumen del recorrido en vivo mientras se arrastra, con la misma cuenta
           del itinerario: paradas, minutos totales (duraciones + tramos) y km de ruta con coma
           decimal. Es efímero: al soltar gana lo que el servidor recalcula y refresca. */
        const dragSummary = computed(() => {
            if (draggingIndex.value === null || hoverSlot.value === null) return null;
            const stops = displayPlaces.value.map((pair) => scheduleStopOf(pair.item));
            const schedule = scheduleStops({
                planDate: planDate.value,
                startTime: eventTime,
                dayEnd: dayEndTime,
                mode: transportMode,
                stops
            });
            let totalMinutes = 0;
            schedule.forEach((slot) => { totalMinutes += slot.minutes + slot.legMinutes; });
            if (!schedule.length) {
                // Sin seed del planificador (plan viejo sin hora de inicio) solo quedan las
                // duraciones: el resumen no miente con un total que no puede derivar.
                stops.forEach((stop) => { totalMinutes += effectiveMinutes(stop); });
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
                kmLabel: totalKm.toFixed(1).replace('.', ',') + ' km'
            };
        });

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

        const renderMarkers = (focusPlaceId = null) => {
            markers.forEach(m => map.removeLayer(m));
            markers = [];
            if (routeLine) {
                map.removeLayer(routeLine);
                routeLine = null;
            }

            const isSinglePlace = planPlaces.value.length === 1;
            const routeLatLngs = [];
            const bounds = [];

            if (allPlaces.value.length > 0) {
                allPlaces.value.forEach((place) => {
                    if (place.latitude && place.longitude) {
                        const itineraryIndex = planPlaces.value.findIndex(p => p.id === place.id);
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

                        const marker = buildMarker(place, { badge })
                            .addTo(map)
                            .bindPopup(popup);

                        markers.push(marker);
                        bounds.push([place.latitude, place.longitude]);

                        if ((focusPlaceId && place.id === focusPlaceId) || (isSinglePlace && isInPlan && focusPlaceId)) {
                            map.flyTo([place.latitude, place.longitude], 13, { duration: 1.0 });
                            setTimeout(() => marker.openPopup(), 1000);
                        }
                    }
                });
            }

            if (planPlaces.value.length > 1) {
                planPlaces.value.forEach(place => {
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
            } catch (error) { requestError.value = error.message; }
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
            } catch (error) { requestError.value = error.message; }
            finally { saving.value = false; }
        };

        const addPlace = async () => {
            if (saving.value) return;
            saving.value = true;
            requestError.value = '';
            const placeObj = allPlaces.value.find(p => p.id === selectedPlaceId.value);
            try {
                if (placeObj) {
                    const entry = await itineraryRequest('?placeId=' + placeObj.id, { method: 'POST' });
                    const addedId = placeObj.id;
                    planPlaces.value.push({
                        ...placeObj,
                        entryId: entry.id,
                        description: entry.description,
                        visitDate: entry.visitDate,
                        visitTime: entry.visitTime,
                        durationMinutes: entry.durationMinutes,
                        timePinned: entry.timePinned,
                        transportModeToNext: entry.transportModeToNext || '',
                        travelMinutesToNext: entry.travelMinutesToNext ?? null
                    });
                    selectedPlaceId.value = null;
                    renderMarkers(addedId);
                }
            } catch (error) {
                requestError.value = error.message;
            } finally {
                saving.value = false;
            }
        };

        const removePlace = async (index) => {
            if (saving.value) return;
            saving.value = true;
            requestError.value = '';
            try {
                await itineraryRequest('/' + planPlaces.value[index].entryId, { method: 'DELETE' });
                planPlaces.value.splice(index, 1);
                renderMarkers();
                // El servidor recalcula el plan sin la parada borrada: las que quedan cambian de
                // hora y el refresco las trae.
                await refreshSchedule();
            } catch (error) { requestError.value = error.message; }
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
            if (saving.value || event.button > 0) return;
            event.preventDefault();
            draggingIndex.value = index;
            hoverSlot.value = index;
            const handle = event.currentTarget;
            handle.setPointerCapture(event.pointerId);
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
            } catch (error) { planPlaces.value = previous; renderMarkers(); requestError.value = error.message; }
            finally { saving.value = false; }
            // [SCH] Reordenar vuelve a derivar el horario: el refresco trae las horas nuevas y
            // deja intacto el orden optimista que ya está pintado. Va fuera del try del
            // reorder para que un fallo acá no revierta un orden que el servidor sí guardó.
            if (reordered) {
                try { await refreshSchedule(); } catch (error) { requestError.value = error.message; }
            }
        };

        onMounted(async () => {
            if (configDialog.value?.dataset.budgetInvalid === 'true') {
                openConfig();
                document.getElementById('config-presupuesto')?.scrollIntoView({ block: 'nearest' });
            }

            try {
                const response = await apiFetch(catalogUrl);
                allPlaces.value = await response.json();
            } catch (error) {
                allPlaces.value = [];
                requestError.value = error.status === 401
                    ? error.message
                    : 'No se pudo cargar el listado de lugares.';
            }
            initMap();
        });

        return {
            allPlaces, planPlaces, selectedPlaceId, selectedEntry, requestError, saving,
            inviteOpen, addPlaceOpen,
            configDialog, openConfig, bannedDialog, bannedTrigger, openBanned,
            participantsDialog, openParticipants, requestsDialog, openRequests,
            placeDialog, availablePlaces, dayGroups, dayCount, directionsUrl, itineraryList,
            dragSummary,
            dragAnnouncement,
            getCategoryLabel, iconHtml, categoryIconHtml, showPlace, updateEntry, addPlace,
            removePlace, onHandleDown, onHandleKeydown
        };
    }
}).mount('#plan-app');
initBudgetDescription();
