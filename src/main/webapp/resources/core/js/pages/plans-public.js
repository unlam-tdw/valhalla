/**
 * The public plan view: one Leaflet map with the itinerary and its route. Extracted from
 * WEB-INF/templates/pages/plans/public.html (docs/guide-js.md R4).
 *
 * The stops come from the page as a seed object, because only Thymeleaf can serialize the
 * model. Nothing here fetches: a shared plan is read-only.
 */

const stops = (window.PUBLIC_PLAN_SEED || {}).stops || [];

function renderMap() {
  const container = document.getElementById('plan-map');
  if (!container || stops.length === 0) {
    return;
  }

  const map = L.map('plan-map');
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap'
  }).addTo(map);

  const bounds = [];
  stops.forEach(function (stop, index) {
    if (!stop.latitude || !stop.longitude) {
      return;
    }
    // Mismo marcador que pages/plans/detail.html: circulo por categoria (cat-*), numerado
    // por parada y con el borde amarillo de "en el itinerario" (todas las paradas lo son
    // aca). El numero va en un nodo con textContent, no en un string con el nombre.
    const badge = document.createElement('div');
    badge.className =
      'custom-map-icon cat-' + (stop.category || 'DEFAULT') + ' in-itinerary';
    const number = document.createElement('span');
    number.textContent = String(index + 1);
    badge.appendChild(number);
    const icon = L.divIcon({
      html: badge,
      className: 'custom-div-icon',
      iconSize: [30, 30],
      iconAnchor: [15, 15]
    });
    // El popup se arma con DOM, no con string concatenado: un nombre de lugar con
    // "<script>" tiene que renderizarse como texto.
    L.marker([stop.latitude, stop.longitude], { icon: icon })
      .addTo(map)
      .bindPopup(function () {
        const content = document.createElement('div');
        const title = document.createElement('strong');
        title.textContent = stop.name;
        content.appendChild(title);
        if (stop.category) {
          const lineBreak = document.createElement('br');
          const label = document.createElement('small');
          label.textContent = stop.category;
          lineBreak.appendChild(label);
          content.appendChild(lineBreak);
        }
        // [MAPS] Como llegar desde el popup, igual que en el detalle y en explore. Se
        // arma con DOM y no con string concatenado, por la misma razon que el popup.
        // Usa la direccion de texto (no la coordenada) y el nombre delante para
        // desambiguar.
        const directions = document.createElement('a');
        directions.href =
          'https://www.google.com/maps/dir/?api=1&destination=' +
          encodeURIComponent(((stop.name || '') + ' ' + (stop.address || '')).trim());
        directions.target = '_blank';
        directions.rel = 'noopener';
        directions.textContent = 'Cómo llegar';
        directions.className = 'text-primary fw-medium d-inline-block mt-1';
        content.appendChild(document.createElement('br'));
        content.appendChild(directions);
        return content;
      });
    bounds.push([stop.latitude, stop.longitude]);
  });

  // Linea que une las paradas en orden, con las mismas opciones que el detalle. Como alla,
  // solo con 2+ paradas: una sola parada no tiene trayecto que dibujar.
  if (stops.length > 1) {
    const routeLatLngs = [];
    stops.forEach(function (stop) {
      if (stop.latitude && stop.longitude) {
        routeLatLngs.push([stop.latitude, stop.longitude]);
      }
    });
    L.polyline(routeLatLngs, {
      color: '#0d6efd',
      weight: 3,
      opacity: 0.8,
      dashArray: '6, 6'
    }).addTo(map);
  }

  if (bounds.length === 1) {
    map.setView(bounds[0], 14);
  } else if (bounds.length > 1) {
    map.fitBounds(bounds, { padding: [30, 30] });
  } else {
    // Ningun lugar trae coordenadas: el mapa queda en un default y la lista de abajo
    // sigue siendo la fuente de verdad del itinerario.
    map.setView([-34.59, -58.40], 12);
  }
}

renderMap();
