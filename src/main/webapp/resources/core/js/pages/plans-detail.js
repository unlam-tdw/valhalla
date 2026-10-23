/**
 * The plan detail view: map, itinerary and catalogue, extracted from
 * WEB-INF/templates/pages/plans/detail.html (docs/guide-js.md R4).
 *
 * The page hands this module four values through `window.PLAN_DETAIL_SEED` (see the
 * bootstrap script in the template): a module cannot read Thymeleaf, and the two lists are
 * object graphs that a data-* attribute cannot carry without a serializer.
 */
import { apiFetch } from '../lib/api.js';

const { createApp, ref, computed, onMounted } = Vue;
const seed = window.PLAN_DETAIL_SEED || {};
const itineraryUrl = seed.itineraryUrl || '';
const catalogUrl = seed.catalogUrl || '';

/** An itinerary entry and a catalogue place arrive in different shapes; both end here. */
const parsePlace = (p) => {
    if (!p) return null;
    const target = p.place ? p.place : p;
    return {
        entryId: p.entryId || null,
        description: p.description,
        visitDate: p.visitDate,
        visitTime: p.visitTime,
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

        const selectedPlaceId = ref(null);
        const selectedEntry = ref(null);
        const requestError = ref('');
        const saving = ref(false);
        const placeDialog = ref(null);

        // Leaflet owns these three and the template never reads them, so they are not refs.
        let map = null;
        let markers = [];
        let routeLine = null;
        let draggedIndex = null;

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

        const formatVisitDate = (value) => {
            const [year, month, day] = value.split('-');
            return `${day}/${month}/${year}`;
        };

        // [MAPS] Direccion de Google Maps para "Como llegar": usa la DIRECCION de texto del
        // lugar (el dataset trae direccion y coordenada desfasadas), con el nombre delante
        // para desambiguar las vagas. Mismo criterio que el mapa y el resto de las vistas.
        const directionsUrl = (item) => {
            const query = `${item.name || ''} ${item.address || ''}`.trim();
            return `https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(query)}`;
        };

        const getCategoryLabel = (category) => {
            const labels = { RESTAURANT: 'Restaurante', BAR: 'Bar', CAFE: 'Café', MUSEUM: 'Museo', PARK: 'Parque', SHOPPING: 'Compras', NIGHTLIFE: 'Vida nocturna', CULTURE: 'Cultura', SPORT: 'Deporte', OTHER: 'Otro' };
            return labels[category] || 'Lugar';
        };

        const getCategoryIcon = (category) => {
            const icons = {
                RESTAURANT: 'fa-utensils',
                NIGHTLIFE:  'fa-wine-glass-alt',
                MUSEUM:     'fa-landmark',
                SHOPPING:   'fa-shopping-bag',
                CAFE:       'fa-coffee',
                PARK:       'fa-tree',
                CULTURE:    'fa-book',
                BAR:        'fa-beer-mug-empty',
                SPORT:      'fa-futbol',
                DEFAULT:    'fa-map-marker-alt'
            };
            return icons[category] || icons.DEFAULT;
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
                        const iconClass = getCategoryIcon(place.category);

                        let activeClass = '';
                        let innerContent = '<i class="fa-solid ' + iconClass + '"></i>';

                        if (isInPlan) {
                            if (isSinglePlace) {
                                activeClass = 'single-place-pulse';
                                innerContent = '<i class="fa-solid ' + iconClass + '"></i>';
                            } else {
                                activeClass = 'in-itinerary';
                                innerContent = '<span>' + (itineraryIndex + 1) + '</span>';
                            }
                        }

                        const customHtml = '<div class="custom-map-icon ' + categoryClass + ' ' + activeClass + '">' + innerContent + '</div>';

                        const customIcon = L.divIcon({
                            html: customHtml,
                            className: 'custom-div-icon',
                            iconSize: [30, 30],
                            iconAnchor: [15, 15]
                        });

                        let popupText = '<b>' + place.name + '</b><br><small>' + place.category + '</small>';
                        if (isInPlan) {
                            if (isSinglePlace) {
                                popupText = '<b>📍 Único lugar seleccionado:</b><br><strong>' + place.name + '</strong>';
                            } else {
                                popupText = '<b>Paso ' + (itineraryIndex + 1) + ': ' + place.name + '</b><br><small class="text-success">✔ En el itinerario</small>';
                            }
                        }
                        // [MAPS] Como llegar desde el propio popup del mapa: usa la direccion
                        // de texto (no la coordenada, que el dataset trae desfasada), con el
                        // nombre delante para desambiguar; mismo criterio que el resto.
                        popupText =
                            '<div>' + popupText + '</div>' +
                            '<a class="text-primary underline-offset-4 text-decoration-none fw-medium" ' +
                            'href="https://www.google.com/maps/dir/?api=1&destination=' +
                            encodeURIComponent(((place.name || '') + ' ' + (place.address || '')).trim()) + '" ' +
                            'target="_blank" rel="noopener">Cómo llegar</a>';

                        const marker = L.marker([place.latitude, place.longitude], { icon: customIcon })
                            .addTo(map)
                            .bindPopup(popupText);

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

            renderMarkers();
        };

        const showPlace = async (item) => {
            requestError.value = '';
            try {
                const entries = await itineraryRequest();
                selectedEntry.value = entries.find(entry => entry.id === item.entryId);
                if (!selectedEntry.value) throw new Error('Este lugar ya no pertenece al plan. Actualiza la página.');
                placeDialog.value.showModal();
            } catch (error) { requestError.value = error.message; }
        };

        const updateDateTime = async (entry, field, value) => {
            if (saving.value || !entry) return;
            saving.value = true;
            requestError.value = '';
            // El PUT reemplaza fecha y hora juntas, así que se manda el par completo: el campo
            // cambiado y el otro tal como estaba (si no, cambiar uno borra el otro).
            const payload = {
                visitDate: field === 'visitDate' ? (value || null) : (entry.visitDate || null),
                visitTime: field === 'visitTime' ? (value || null) : (entry.visitTime || null)
            };
            try {
                const updated = await itineraryRequest('/' + entry.id, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });
                entry.visitDate = updated.visitDate;
                entry.visitTime = updated.visitTime;
                const item = planPlaces.value.find(p => p.entryId === entry.id);
                if (item) {
                    item.visitDate = updated.visitDate;
                    item.visitTime = updated.visitTime;
                }
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
                    planPlaces.value.push({ ...placeObj, entryId: entry.id, description: entry.description, visitDate: entry.visitDate, visitTime: entry.visitTime });
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
            } catch (error) { requestError.value = error.message; }
            finally { saving.value = false; }
        };

        const onDragStart = (index) => {
            draggedIndex = index;
        };

        const onDrop = async (dropIndex) => {
            if (saving.value) return;
            if (draggedIndex !== null && draggedIndex !== dropIndex) {
                saving.value = true;
                requestError.value = '';
                const previous = [...planPlaces.value];
                const movedItem = planPlaces.value.splice(draggedIndex, 1)[0];
                planPlaces.value.splice(dropIndex, 0, movedItem);
                draggedIndex = null;
                renderMarkers();
                try {
                    await itineraryRequest('/reorder', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(planPlaces.value.map(item => item.entryId)) });
                } catch (error) { planPlaces.value = previous; renderMarkers(); requestError.value = error.message; }
                finally { saving.value = false; }
            }
        };

        onMounted(async () => {
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
            placeDialog, availablePlaces, formatVisitDate, directionsUrl,
            getCategoryLabel, getCategoryIcon, showPlace, updateDateTime, addPlace,
            removePlace, onDragStart, onDrop
        };
    }
}).mount('#plan-app');
