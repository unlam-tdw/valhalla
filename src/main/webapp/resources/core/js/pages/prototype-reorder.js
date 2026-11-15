/**
 * [PROTOTYPE] Descartable — responde: ¿cómo se siente reordenar el itinerario con drag & drop?
 *
 * Tres variantes de interacción sobre la MISMA lista de paradas, elegibles por ?variant=a|b|c
 * o con la barra flotante inferior (las flechas ←/→ del teclado también ciclan):
 *
 *   A — Arrastrar la fila: HTML5 drag & drop nativo, la fila entera es el handle, con
 *       indicador de inserción en la fila destino.
 *   B — Manija + reflow en vivo: se arrastra por una manija; mientras se arrastra, la lista
 *       reacomoda en vivo (animación FLIP) y los horarios derivados de TODAS las paradas se
 *       recalculan al vuelo. Es la candidata al "momento mágico" del producto.
 *   C — Toque para mover: nada de arrastre. Se levanta una parada con un tap y se suelta
 *       tocando la posición destino (mobile-first); flechas ↑/↓ por fila y teclado como
 *       alternativa accesible.
 *
 * Todo es mock en memoria: lugares reales de Buenos Aires, sin backend ni persistencia. El
 * cronograma se deriva con la misma idea que la app real (duración de la parada + viaje al
 * siguiente, tope 23:00, sigue al día siguiente) pero simplificado: lo que no entra en el
 * día arranca a las 10:00 del día siguiente. El viaje sale de la distancia real entre
 * coordenadas y modo a pie fijo, así reordenar cambia las piernas de verdad. El mini-mapa
 * dibuja la ruta punteada en el orden actual, igual que el mapa real del detalle.
 */

import { categoryColor } from '../lib/category-colors.js';

const { createApp, reactive, computed, ref, nextTick } = Vue;

// ---------------------------------------------------------------------------
// Datos mock y derivación del cronograma
// ---------------------------------------------------------------------------

const PLACES = [
  { id: 'tortoni', name: 'Café Tortoni', category: 'CAFE', duration: 60, lat: -34.6089, lng: -58.3798 },
  { id: 'san-telmo', name: 'Mercado de San Telmo', category: 'SHOPPING', duration: 90, lat: -34.6212, lng: -58.3723 },
  { id: 'floralis', name: 'Floralis Genérica', category: 'PARK', duration: 30, lat: -34.6037, lng: -58.4062 },
  { id: 'malba', name: 'MALBA', category: 'MUSEUM', duration: 120, lat: -34.6119, lng: -58.4107 },
  { id: 'ateneo', name: 'El Ateneo Grand Splendid', category: 'CULTURE', duration: 60, lat: -34.5983, lng: -58.3860 }
];

const BY_ID = Object.fromEntries(PLACES.map(function (p) { return [p.id, p]; }));

const DAY_START = 10 * 60;
const DAY_END = 23 * 60;
const WALK_KMH = 4.5;

function kmBetween(a, b) {
  const R = 6371;
  const rad = Math.PI / 180;
  const dLat = (b.lat - a.lat) * rad;
  const dLng = (b.lng - a.lng) * rad;
  const s =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(a.lat * rad) * Math.cos(b.lat * rad) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
  return 2 * R * Math.asin(Math.sqrt(s));
}

function legMinutes(a, b) {
  return Math.max(1, Math.round((kmBetween(a, b) / WALK_KMH) * 60));
}

// La derivación del prototipo: misma idea que la app real, simplificada a propósito.
function deriveSchedule(orderIds) {
  let day = 1;
  let cursor = DAY_START;
  let totalMinutes = 0;
  let totalKm = 0;
  const stops = [];
  orderIds.forEach(function (id, i) {
    const place = BY_ID[id];
    if (cursor >= DAY_END || cursor + place.duration > DAY_END) {
      day += 1;
      cursor = DAY_START;
    }
    const arrival = cursor;
    const departure = arrival + place.duration;
    let leg = 0;
    if (i < orderIds.length - 1) {
      leg = legMinutes(place, BY_ID[orderIds[i + 1]]);
      totalKm += kmBetween(place, BY_ID[orderIds[i + 1]]);
    }
    stops.push(Object.assign({}, place, { day: day, arrival: arrival, departure: departure, leg: leg }));
    totalMinutes += place.duration + leg;
    cursor = departure + leg;
  });
  return { stops: stops, totalMinutes: totalMinutes, totalKm: totalKm };
}

function fmtTime(minutes) {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return String(h).padStart(2, '0') + ':' + String(m).padStart(2, '0');
}

function fmtDuration(minutes) {
  if (minutes < 60) return minutes + ' min';
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return m === 0 ? h + ' h' : h + ' h ' + m + ' min';
}

function fmtKm(km) {
  return km.toFixed(1).replace('.', ',') + ' km';
}

// ---------------------------------------------------------------------------
// Estado compartido por las variantes
// ---------------------------------------------------------------------------

function initialVariant() {
  const value = (new URLSearchParams(window.location.search).get('variant') || 'a').toLowerCase();
  return VARIANTS.some(function (v) { return v.key === value; }) ? value : 'a';
}

const store = reactive({
  variant: null, // se setea tras definir VARIANTS
  order: PLACES.map(function (p) { return p.id; })
});

const itinerary = computed(function () {
  return deriveSchedule(store.order);
});

function move(from, to) {
  const next = store.order.slice();
  const [id] = next.splice(from, 1);
  next.splice(to, 0, id);
  store.order = next;
}

// Animación FLIP barata: mide los tops antes de mutar, muta, y anima el delta al revertir.
async function flip(container, mutate) {
  const before = new Map();
  container.querySelectorAll('[data-stop-row]').forEach(function (row) {
    before.set(row.dataset.stopRow, row.getBoundingClientRect().top);
  });
  mutate();
  await nextTick();
  container.querySelectorAll('[data-stop-row]').forEach(function (row) {
    const prev = before.get(row.dataset.stopRow);
    if (prev == null) return;
    const delta = prev - row.getBoundingClientRect().top;
    if (!delta) return;
    row.style.transition = 'none';
    row.style.transform = 'translateY(' + delta + 'px)';
    requestAnimationFrame(function () {
      row.style.transition = 'transform 180ms ease';
      row.style.transform = '';
    });
  });
}

// ---------------------------------------------------------------------------
// Variante A — arrastrar la fila (HTML5 drag & drop nativo)
// ---------------------------------------------------------------------------

const VariantA = {
  name: 'VariantA',
  setup() {
    const dragging = ref(null);
    const hoverIndex = ref(null);

    function onDragStart(index, event) {
      dragging.value = index;
      event.dataTransfer.effectAllowed = 'move';
      try { event.dataTransfer.setData('text/plain', String(index)); } catch (e) { /* sin storage */ }
    }

    function onDragOver(index) {
      if (dragging.value === null) return;
      hoverIndex.value = index;
    }

    function onDrop(index) {
      if (dragging.value === null) return;
      move(dragging.value, index);
      dragging.value = null;
      hoverIndex.value = null;
    }

    function onDragEnd() {
      dragging.value = null;
      hoverIndex.value = null;
    }

    return { store, itinerary, dragging, hoverIndex, onDragStart, onDragOver, onDrop, onDragEnd, fmtTime, fmtDuration, categoryColor };
  },
  template: `
    <p class="mb-3 text-sm text-gray-500">Agarrá cualquier fila y arrastrala. La fila destino se marca mientras pasás por encima; los horarios se recalculan al soltar.</p>
    <ul class="space-y-2" @dragover.prevent>
      <li v-for="(id, index) in store.order" :key="id" :data-stop-row="id"
        draggable="true"
        @dragstart="onDragStart(index, $event)"
        @dragover.prevent="onDragOver(index)"
        @drop.prevent="onDrop(index)"
        @dragend="onDragEnd"
        class="flex cursor-grab items-center gap-3 rounded-lg border bg-white p-3 shadow-sm transition active:cursor-grabbing"
        :class="dragging === index ? 'opacity-40' : (hoverIndex === index && dragging !== null && dragging !== index ? 'outline outline-2 outline-primary' : '')">
        <span class="grid h-8 w-8 shrink-0 place-items-center rounded-full text-sm font-bold text-white" :style="{ backgroundColor: categoryColor(itinerary.stops[index].category) }">{{ index + 1 }}</span>
        <div class="min-w-0 flex-1">
          <p class="truncate text-sm font-semibold text-gray-900">{{ itinerary.stops[index].name }}</p>
          <p class="text-xs text-gray-500">
            Día {{ itinerary.stops[index].day }} · {{ fmtTime(itinerary.stops[index].arrival) }} → {{ fmtTime(itinerary.stops[index].departure) }}
            · {{ fmtDuration(itinerary.stops[index].duration) }} en el lugar
          </p>
        </div>
        <span v-if="itinerary.stops[index].leg" class="shrink-0 text-xs text-gray-400">↓ {{ itinerary.stops[index].leg }} min a pie</span>
      </li>
    </ul>
  `
};

// ---------------------------------------------------------------------------
// Variante B — manija + reflow en vivo con FLIP
// ---------------------------------------------------------------------------

const VariantB = {
  name: 'VariantB',
  setup() {
    const listEl = ref(null);
    const dragging = ref(null);
    const hoverSlot = ref(null);

    // displayOrder es el orden que se ve: con la manija agarrada, es el orden con la
    // parada ya insertada en la ranura hover. De él deriva el cronograma, así los
    // horarios de todas las paradas se recalculan mientras se arrastra.
    const displayOrder = computed(function () {
      if (dragging.value === null || hoverSlot.value === null) return store.order;
      const next = store.order.slice();
      const [id] = next.splice(dragging.value, 1);
      next.splice(hoverSlot.value, 0, id);
      return next;
    });

    const displayItinerary = computed(function () {
      return deriveSchedule(displayOrder.value);
    });

    function slotFromPointer(event) {
      const rows = Array.from(listEl.value.querySelectorAll('[data-stop-row]'));
      for (let i = 0; i < rows.length; i++) {
        const rect = rows[i].getBoundingClientRect();
        if (event.clientY < rect.top + rect.height / 2) return i;
      }
      return rows.length - 1;
    }

    function onHandleDown(event, index) {
      dragging.value = index;
      hoverSlot.value = index;
      event.target.setPointerCapture(event.pointerId);
      const onMove = function (e) {
        hoverSlot.value = slotFromPointer(e);
      };
      const onUp = function () {
        event.target.removeEventListener('pointermove', onMove);
        event.target.removeEventListener('pointerup', onUp);
        event.target.removeEventListener('pointercancel', onUp);
        if (dragging.value !== null && hoverSlot.value !== null && hoverSlot.value !== dragging.value) {
          flip(listEl.value, function () {
            store.order = displayOrder.value;
          });
        }
        dragging.value = null;
        hoverSlot.value = null;
      };
      event.target.addEventListener('pointermove', onMove);
      event.target.addEventListener('pointerup', onUp);
      event.target.addEventListener('pointercancel', onUp);
    }

    return {
      store, dragging, hoverSlot, displayOrder, displayItinerary, listEl, onHandleDown, fmtTime, fmtDuration, categoryColor
    };
  },
  template: `
    <p class="mb-3 text-sm text-gray-500">Agarrá la manija ⟷ de una fila. Mientras arrastrás, la lista reacomoda en vivo y <strong>todos los horarios se recalculan al vuelo</strong>; soltás y queda.</p>
    <ul ref="listEl" class="space-y-2">
      <li v-for="(id, index) in displayOrder" :key="id" :data-stop-row="id"
        class="flex items-center gap-3 rounded-lg border bg-white p-3 shadow-sm"
        :class="dragging !== null ? (id === displayOrder[hoverSlot] ? 'ring-2 ring-primary shadow-lg' : 'opacity-60') : ''">
        <button type="button" aria-label="Arrastrar para reordenar"
          class="grid h-9 w-7 shrink-0 cursor-grab touch-none place-items-center rounded text-gray-400 hover:bg-gray-100 hover:text-gray-600 active:cursor-grabbing"
          :class="dragging === store.order.indexOf(id) ? 'text-primary' : ''"
          @pointerdown="onHandleDown($event, store.order.indexOf(id))">
          <svg width="10" height="18" viewBox="0 0 10 18" fill="currentColor" aria-hidden="true">
            <circle cx="2.5" cy="3" r="1.5"/><circle cx="7.5" cy="3" r="1.5"/>
            <circle cx="2.5" cy="9" r="1.5"/><circle cx="7.5" cy="9" r="1.5"/>
            <circle cx="2.5" cy="15" r="1.5"/><circle cx="7.5" cy="15" r="1.5"/>
          </svg>
        </button>
        <span class="grid h-8 w-8 shrink-0 place-items-center rounded-full text-sm font-bold text-white" :style="{ backgroundColor: categoryColor(displayItinerary.stops[index].category) }">{{ index + 1 }}</span>
        <div class="min-w-0 flex-1">
          <p class="truncate text-sm font-semibold text-gray-900">{{ displayItinerary.stops[index].name }}</p>
          <p class="text-xs text-gray-500">
            Día {{ displayItinerary.stops[index].day }} · {{ fmtTime(displayItinerary.stops[index].arrival) }} → {{ fmtTime(displayItinerary.stops[index].departure) }}
            · {{ fmtDuration(displayItinerary.stops[index].duration) }} en el lugar
          </p>
        </div>
        <span v-if="displayItinerary.stops[index].leg" class="shrink-0 text-xs text-gray-400">↓ {{ displayItinerary.stops[index].leg }} min a pie</span>
      </li>
    </ul>
  `
};

// ---------------------------------------------------------------------------
// Variante C — toque para mover (mobile-first, sin arrastre)
// ---------------------------------------------------------------------------

const VariantC = {
  name: 'VariantC',
  setup() {
    const lifted = ref(null);

    function lift(index) {
      lifted.value = lifted.value === index ? null : index;
    }

    function dropAt(index) {
      if (lifted.value === null) return;
      move(lifted.value, index);
      lifted.value = null;
    }

    function nudge(index, delta) {
      const target = index + delta;
      if (target < 0 || target >= store.order.length) return;
      move(index, target);
      if (lifted.value === index) lifted.value = target;
    }

    function onKeydown(event) {
      if (lifted.value === null) return;
      if (event.key === 'ArrowUp') { nudge(lifted.value, -1); event.preventDefault(); }
      if (event.key === 'ArrowDown') { nudge(lifted.value, 1); event.preventDefault(); }
      if (event.key === 'Escape') lifted.value = null;
    }

    return { store, itinerary, lifted, lift, dropAt, nudge, onKeydown, fmtTime, fmtDuration, categoryColor };
  },
  template: `
    <p class="mb-3 text-sm text-gray-500">Sin arrastre: tocá <strong>Mover</strong> para levantar una parada y tocá la posición destino para soltarla ahí. También andan las flechas ↑/↓ por fila y el teclado (↑/↓/Esc con una parada levantada).</p>
    <ul class="space-y-2" tabindex="0" @keydown="onKeydown">
      <li v-for="(id, index) in store.order" :key="id" :data-stop-row="id">
        <div v-if="lifted !== null && lifted !== index" class="mb-2">
          <button type="button" class="w-full rounded-lg border-2 border-dashed border-primary bg-primary/5 px-3 py-2 text-xs font-semibold text-primary hover:bg-primary/10"
            @click="dropAt(index)">Soltar acá (posición {{ index + 1 }})</button>
        </div>
        <div class="flex items-center gap-3 rounded-lg border bg-white p-3 shadow-sm"
          :class="lifted === index ? 'ring-2 ring-primary shadow-lg' : ''">
          <span class="grid h-8 w-8 shrink-0 place-items-center rounded-full text-sm font-bold text-white" :style="{ backgroundColor: categoryColor(itinerary.stops[index].category) }">{{ index + 1 }}</span>
          <div class="min-w-0 flex-1">
            <p class="truncate text-sm font-semibold text-gray-900">{{ itinerary.stops[index].name }}</p>
            <p class="text-xs text-gray-500">
              Día {{ itinerary.stops[index].day }} · {{ fmtTime(itinerary.stops[index].arrival) }} → {{ fmtTime(itinerary.stops[index].departure) }}
              · {{ fmtDuration(itinerary.stops[index].duration) }} en el lugar
            </p>
          </div>
          <div class="flex shrink-0 items-center gap-1">
            <button type="button" aria-label="Subir" class="grid h-8 w-8 place-items-center rounded-md text-gray-500 hover:bg-gray-100 disabled:opacity-30"
              :disabled="index === 0" @click="nudge(index, -1)">↑</button>
            <button type="button" aria-label="Bajar" class="grid h-8 w-8 place-items-center rounded-md text-gray-500 hover:bg-gray-100 disabled:opacity-30"
              :disabled="index === store.order.length - 1" @click="nudge(index, 1)">↓</button>
            <button type="button" class="rounded-md bg-gray-900 px-2.5 py-1.5 text-xs font-semibold text-white hover:bg-gray-700"
              @click="lift(index)">{{ lifted === index ? 'Cancelo' : 'Mover' }}</button>
          </div>
        </div>
      </li>
      <li v-if="lifted !== null">
        <button type="button" class="w-full rounded-lg border-2 border-dashed border-primary bg-primary/5 px-3 py-2 text-xs font-semibold text-primary hover:bg-primary/10"
          @click="dropAt(store.order.length)">Soltar al final (posición {{ store.order.length }})</button>
      </li>
    </ul>
  `
};

// ---------------------------------------------------------------------------
// Panel lateral compartido: resumen derivado + mini-mapa de la ruta
// ---------------------------------------------------------------------------

const RouteMiniMap = {
  name: 'RouteMiniMap',
  props: { stops: { type: Array, required: true } },
  setup(props) {
    const W = 220;
    const H = 150;
    const P = 16;

    const points = computed(function () {
      const lats = props.stops.map(function (s) { return s.lat; });
      const lngs = props.stops.map(function (s) { return s.lng; });
      const minLat = Math.min.apply(null, lats);
      const maxLat = Math.max.apply(null, lats);
      const minLng = Math.min.apply(null, lngs);
      const maxLng = Math.max.apply(null, lngs);
      const spanLat = Math.max(maxLat - minLat, 0.0001);
      const spanLng = Math.max(maxLng - minLng, 0.0001);
      return props.stops.map(function (s, i) {
        return {
          x: P + ((s.lng - minLng) / spanLng) * (W - 2 * P),
          y: P + ((maxLat - s.lat) / spanLat) * (H - 2 * P),
          n: i + 1,
          color: categoryColor(s.category)
        };
      });
    });

    return { W, H, points };
  },
  template: `
    <svg :viewBox="'0 0 ' + W + ' ' + H" class="w-full rounded-lg border bg-gray-50">
      <polyline :points="points.map(p => p.x + ',' + p.y).join(' ')"
        fill="none" stroke="#2563eb" stroke-width="2" stroke-dasharray="5 4" />
      <g v-for="p in points" :key="p.n">
        <circle :cx="p.x" :cy="p.y" r="9" :fill="p.color" stroke="#fff" stroke-width="2" />
        <text :x="p.x" :y="p.y + 3.5" text-anchor="middle" font-size="9" font-weight="bold" fill="#fff">{{ p.n }}</text>
      </g>
    </svg>
  `
};

// ---------------------------------------------------------------------------
// Barra flotante de variantes + raíz
// ---------------------------------------------------------------------------

const VARIANTS = [
  { key: 'a', name: 'Arrastrar la fila', component: VariantA },
  { key: 'b', name: 'Manija + reflow en vivo', component: VariantB },
  { key: 'c', name: 'Toque para mover', component: VariantC }
];
store.variant = initialVariant();

const PrototypeBar = {
  name: 'PrototypeBar',
  setup() {
    function cycle(delta) {
      const i = VARIANTS.findIndex(function (v) { return v.key === store.variant; });
      const next = VARIANTS[(i + delta + VARIANTS.length) % VARIANTS.length];
      store.variant = next.key;
      window.history.replaceState(null, '', '?variant=' + next.key);
    }

    window.addEventListener('keydown', function (event) {
      if (event.target.closest && event.target.closest('input, textarea, [contenteditable]')) return;
      if (event.key === 'ArrowLeft') cycle(-1);
      if (event.key === 'ArrowRight') cycle(1);
    });

    const current = computed(function () {
      return VARIANTS.find(function (v) { return v.key === store.variant; });
    });

    return { cycle, current };
  },
  template: `
    <div class="fixed bottom-4 left-1/2 z-50 flex -translate-x-1/2 items-center gap-3 rounded-full bg-gray-900 px-4 py-2.5 text-white shadow-2xl">
      <button type="button" aria-label="Variante anterior" class="grid h-8 w-8 place-items-center rounded-full bg-white/10 hover:bg-white/20" @click="cycle(-1)">←</button>
      <span class="text-xs font-semibold tracking-wide">{{ current.key.toUpperCase() }} · {{ current.name }}</span>
      <button type="button" aria-label="Variante siguiente" class="grid h-8 w-8 place-items-center rounded-full bg-white/10 hover:bg-white/20" @click="cycle(1)">→</button>
    </div>
  `
};

const app = createApp({
  components: { RouteMiniMap, PrototypeBar },
  setup() {
    const current = computed(function () {
      return VARIANTS.find(function (v) { return v.key === store.variant; });
    });
    const summary = computed(function () {
      const it = itinerary.value;
      return {
        stopCount: it.stops.length,
        duration: fmtDuration(it.totalMinutes),
        km: fmtKm(it.totalKm)
      };
    });
    return { current, summary, itinerary, RouteMiniMap };
  },
  template: `
    <div class="grid gap-6 lg:grid-cols-[minmax(0,1fr)_280px]">
      <section class="min-w-0 rounded-xl border bg-gray-50 p-4">
        <h2 class="mb-1 text-sm font-bold uppercase tracking-wide text-gray-500">Itinerario</h2>
        <component :is="current.component" :key="current.key" />
      </section>
      <aside class="space-y-4">
        <div class="rounded-xl border bg-white p-4">
          <h2 class="mb-2 text-sm font-bold uppercase tracking-wide text-gray-500">Resumen derivado</h2>
          <p class="text-sm font-semibold text-gray-900">{{ summary.stopCount }} paradas · {{ summary.duration }} · {{ summary.km }}</p>
          <p class="mt-1 text-xs text-gray-500">Se recalcula con cada movimiento (misma idea que la derivación real del cronograma).</p>
        </div>
        <div class="rounded-xl border bg-white p-4">
          <h2 class="mb-2 text-sm font-bold uppercase tracking-wide text-gray-500">Ruta en el orden actual</h2>
          <RouteMiniMap :stops="itinerary.stops" />
        </div>
      </aside>
    </div>
    <PrototypeBar />
  `
});

const root = document.getElementById('prototype-reorder-root');
app.mount(root);
root.removeAttribute('v-cloak');
