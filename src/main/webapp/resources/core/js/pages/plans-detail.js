/**
 * The plan detail view: map, itinerary and catalogue, extracted from
 * WEB-INF/templates/pages/plans/detail.html (docs/guide-js.md R4).
 *
 * The page hands this module four values through `window.PLAN_DETAIL_SEED` (see the
 * bootstrap script in the template): a module cannot read Thymeleaf, and the two lists are
 * object graphs that a data-* attribute cannot carry without a serializer.
 */
import { apiFetch } from '../lib/api.js';

const { createApp } = Vue;
const seed = window.PLAN_DETAIL_SEED || {};
const itineraryUrl = seed.itineraryUrl || '';
const catalogUrl = seed.catalogUrl || '';

createApp({
    data() {
        const seederPlaces = [
            { id: 1, name: "El Sanjuanino", category: "RESTAURANT", address: "Av. del Libertador 1234", latitude: -34.5895, longitude: -58.4095 },
            { id: 2, name: "La Viruta", category: "NIGHTLIFE", address: "Armenia 1366", latitude: -34.6025, longitude: -58.4185 },
            { id: 3, name: "MALBA", category: "MUSEUM", address: "Av. Figueroa Alcorta 3415", latitude: -34.5833, longitude: -58.3928 },
            { id: 4, name: "Plaza Serrano", category: "SHOPPING", address: "Plaza Cortazar, Palermo", latitude: -34.5818, longitude: -58.4187 },
            { id: 5, name: "Cafe Tortoni", category: "CAFE", address: "Av. de Mayo 825", latitude: -34.6083, longitude: -58.3722 },
            { id: 6, name: "Parque Tres de Febrero", category: "PARK", address: "Palermo", latitude: -34.5747, longitude: -58.4103 },
            { id: 7, name: "El Ateneo Grand Splendid", category: "CULTURE", address: "Av. Santa Fe 1860", latitude: -34.5967, longitude: -58.3817 },
            { id: 8, name: "Cerveceria General San Martin", category: "BAR", address: "Av. Corrientes 1475", latitude: -34.6033, longitude: -58.3817 },
            { id: 9, name: "Planetario Galileo Galilei", category: "CULTURE", address: "Av. Sarmiento s/n", latitude: -34.5725, longitude: -58.4172 },
            { id: 10, name: "Parrilla Don Julio", category: "RESTAURANT", address: "Guatemala 4699", latitude: -34.5892, longitude: -58.4262 }
        ];

        const backendAllPlaces = /*[[${allPlaces}]]*/ [] || [];
        const backendPlanPlaces = /*[[${itineraryPlaces}]]*/ [] || [];

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

        const normalizedAllPlaces = (backendAllPlaces && backendAllPlaces.length > 0)
            ? backendAllPlaces.map(parsePlace).filter(p => p && p.id)
            : seederPlaces;

        const normalizedPlanPlaces = backendPlanPlaces.map(parsePlace).filter(p => p && p.id);

        return {
            map: null,
            markers: [],
            routeLine: null,
            selectedPlaceId: null,
            draggedIndex: null,
            selectedEntry: null,
            requestError: '',
            saving: false,
            allPlaces: normalizedAllPlaces,
            planPlaces: normalizedPlanPlaces
        };
    },
    computed: {
        availablePlaces() {
            const addedIds = this.planPlaces.map(p => p.id);
            return this.allPlaces.filter(p => !addedIds.includes(p.id));
        }
    },
    async mounted() {
        try {
            const response = await apiFetch(catalogUrl);
            this.allPlaces = await response.json();
        } catch (error) {
            this.allPlaces = [];
            this.requestError = error.status === 401
                ? error.message
                : 'No se pudo cargar el listado de lugares.';
        }
        this.initMap();
    },
    methods: {
        async itineraryRequest(path = '', options = {}) {
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
        },
        async showPlace(item) {
            this.requestError = '';
            try {
                const entries = await this.itineraryRequest();
                this.selectedEntry = entries.find(entry => entry.id === item.entryId);
                if (!this.selectedEntry) throw new Error('Este lugar ya no pertenece al plan. Actualiza la página.');
                this.$refs.placeDialog.showModal();
            } catch (error) { this.requestError = error.message; }
        },

        async updateDateTime(entry, field, value) {
            if (this.saving || !entry) return;
            this.saving = true;
            this.requestError = '';
            // El PUT reemplaza fecha y hora juntas, así que se manda el par completo: el campo
            // cambiado y el otro tal como estaba (si no, cambiar uno borra el otro).
            const payload = {
                visitDate: field === 'visitDate' ? (value || null) : (entry.visitDate || null),
                visitTime: field === 'visitTime' ? (value || null) : (entry.visitTime || null)
            };
            try {
                const updated = await this.itineraryRequest('/' + entry.id, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });
                entry.visitDate = updated.visitDate;
                entry.visitTime = updated.visitTime;
                const item = this.planPlaces.find(p => p.entryId === entry.id);
                if (item) {
                    item.visitDate = updated.visitDate;
                    item.visitTime = updated.visitTime;
                }
            } catch (error) { this.requestError = error.message; }
            finally { this.saving = false; }
        },
        formatVisitDate(value) {
            const [year, month, day] = value.split('-');
            return `${day}/${month}/${year}`;
        },
        // [MAPS] Direccion de Google Maps para "Como llegar": usa la DIRECCION de texto del
        // lugar (el dataset trae direccion y coordenada desfasadas), con el nombre delante
        // para desambiguar las vagas. Mismo criterio que el mapa y el resto de las vistas.
        directionsUrl(item) {
            const query = `${item.name || ''} ${item.address || ''}`.trim();
            return `https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(query)}`;
        },
        getCategoryLabel(category) {
            const labels = { RESTAURANT: 'Restaurante', BAR: 'Bar', CAFE: 'Café', MUSEUM: 'Museo', PARK: 'Parque', SHOPPING: 'Compras', NIGHTLIFE: 'Vida nocturna', CULTURE: 'Cultura', SPORT: 'Deporte', OTHER: 'Otro' };
            return labels[category] || 'Lugar';
        },
        getCategoryIcon(category) {
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
        },

        initMap() {
            this.map = L.map('map').setView([-34.59, -58.40], 12);

            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; OpenStreetMap'
            }).addTo(this.map);

            this.renderMarkers();
        },

        renderMarkers(focusPlaceId = null) {
            this.markers.forEach(m => this.map.removeLayer(m));
            this.markers = [];
            if (this.routeLine) {
                this.map.removeLayer(this.routeLine);
                this.routeLine = null;
            }

            const isSinglePlace = this.planPlaces.length === 1;
            const routeLatLngs = [];
            const bounds = [];

            if (this.allPlaces.length > 0) {
                this.allPlaces.forEach((place) => {
                    if (place.latitude && place.longitude) {
                        const itineraryIndex = this.planPlaces.findIndex(p => p.id === place.id);
                        const isInPlan = itineraryIndex !== -1;

                        const categoryClass = 'cat-' + place.category;
                        const iconClass = this.getCategoryIcon(place.category);

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
                            .addTo(this.map)
                            .bindPopup(popupText);

                        this.markers.push(marker);
                        bounds.push([place.latitude, place.longitude]);

                        if ((focusPlaceId && place.id === focusPlaceId) || (isSinglePlace && isInPlan && focusPlaceId)) {
                            this.map.flyTo([place.latitude, place.longitude], 13, { duration: 1.0 });
                            setTimeout(() => marker.openPopup(), 1000);
                        }
                    }
                });
            }

            if (this.planPlaces.length > 1) {
                this.planPlaces.forEach(place => {
                    if (place.latitude && place.longitude) {
                        routeLatLngs.push([place.latitude, place.longitude]);
                    }
                });

                this.routeLine = L.polyline(routeLatLngs, {
                    color: '#0d6efd',
                    weight: 3,
                    opacity: 0.8,
                    dashArray: '6, 6'
                }).addTo(this.map);
            }

            if (!focusPlaceId && bounds.length > 0) {
                this.map.fitBounds(bounds, { padding: [60, 60] });
            }
        },

        async addPlace() {
            if (this.saving) return;
            this.saving = true;
            this.requestError = '';
            const placeObj = this.allPlaces.find(p => p.id === this.selectedPlaceId);
            try {
                if (placeObj) {
                    const entry = await this.itineraryRequest('?placeId=' + placeObj.id, { method: 'POST' });
                    const addedId = placeObj.id;
                    this.planPlaces.push({ ...placeObj, entryId: entry.id, description: entry.description, visitDate: entry.visitDate, visitTime: entry.visitTime });
                    this.selectedPlaceId = null;
                    this.renderMarkers(addedId);
                }
            } catch (error) {
                this.requestError = error.message;
            } finally {
                this.saving = false;
            }
        },

        async removePlace(index) {
            if (this.saving) return;
            this.saving = true;
            this.requestError = '';
            try {
                await this.itineraryRequest('/' + this.planPlaces[index].entryId, { method: 'DELETE' });
                this.planPlaces.splice(index, 1);
                this.renderMarkers();
            } catch (error) { this.requestError = error.message; }
            finally { this.saving = false; }
        },

        onDragStart(index) {
            this.draggedIndex = index;
        },

        async onDrop(dropIndex) {
            if (this.saving) return;
            if (this.draggedIndex !== null && this.draggedIndex !== dropIndex) {
                this.saving = true;
                this.requestError = '';
                const previous = [...this.planPlaces];
                const movedItem = this.planPlaces.splice(this.draggedIndex, 1)[0];
                this.planPlaces.splice(dropIndex, 0, movedItem);
                this.draggedIndex = null;
                this.renderMarkers();
                try {
                    await this.itineraryRequest('/reorder', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(this.planPlaces.map(item => item.entryId)) });
                } catch (error) { this.planPlaces = previous; this.renderMarkers(); this.requestError = error.message; }
                finally { this.saving = false; }
            }
        }
    }
}).mount('#plan-app');
