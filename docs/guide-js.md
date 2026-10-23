# Guide: JavaScript in the View Layer

The contract for every line of JavaScript in this project, whether it lives in a `.html`
template or in a `.js` file. It exists because the view layer accumulated four
conventions at once, and one of them broke a page in production-ish conditions: an inline
`[[${...}]]` inside a plain `<script>` emitted `&amp;` into a JS string and a raw newline
that ended the literal, so the page's whole Vue app died with a `SyntaxError` and nobody
could save a place.

That is not a style preference. R3 and R7 each prevent a defect that already happened here;
the rest exist so the next one is not invisible.

**Enforced by:** `.\scripts\gate.ps1 js` -- see [the gate](#the-gate).

Related: [guide-vue.md](guide-vue.md) for the Thymeleaf + Vue view structure,
[guide-css-js.md](guide-css-js.md) for the no-framework alternative. Those two describe
*what a view looks like*; this one describes *how its JavaScript is written*.

## Scope

| Path | Status |
| :--- | :--- |
| `src/main/webapp/WEB-INF/templates/**/*.html` | covered, inline `<script>` included |
| `src/main/webapp/resources/core/js/**/*.js` | covered, `pages/` and `lib/` included |
| `vue.global.prod.js`, `tailwind-browser.js`, `lucide.min.js` | vendored, excluded |

## The rules

### R1 -- One API: Composition, via `setup()`

All new page code uses `setup()` with `ref`/`computed`/`watch`. The Options API pages that
remain are migration targets, not a second style to copy.

```js
createApp({
  setup() {
    const places = ref([]);
    return { places };
  }
}).mount('#explore-app');
```

Why: the two largest views in the tree -- `pages/places-list.js` and
`pages/place-import.js` -- already use it, and it is the only form whose state can be
tested without mounting a component.

### R2 -- One root per page, named after the view

The mount target is `#<view>-app`: `#place-form-app`, `#users-app`, `#explore-app`. Not
`#app`.

Mount exactly one app per page. A fragment that mounts is a bug: the host page owns the
lifecycle of the subtree.

R2 is not satisfied by the four extracted views yet -- `pages/places-list.js` and
`pages/place-import.js` still mount `#app`. The rename is phase 4, because `#app` is a
structural selector (`#app > aside`) in ten E2E page objects: renaming the root without
those edits turns every mobile layout test into a timeout.

### R3 -- Server data enters through `data-*`, never as an inline expression

**Never** `[[${...}]]` or `[(${...})]` inside `<script>`. Thymeleaf renders those as
escaped HTML text, not as a JavaScript literal: `&` becomes `&amp;`, and a newline in the
value ends the string and kills the page's script.

```html
<!-- no: placeForm.description comes from a <textarea>, so newlines are normal -->
<script>
  description: '[[${placeForm.description}]]'
</script>

<!-- yes: the attribute is HTML-escaped by the browser's own parser and read back decoded -->
<div id="place-form-app" th:attr="data-description=${placeForm.description}"></div>
<script>
  const data = document.getElementById('place-form-app').dataset;
  const description = data.description;
</script>
```

`th:inline="javascript"` with the comment form is the accepted alternative when the value
really must sit in a literal -- it is what `layouts/base.html` does for the dev-reload
token. Prefer `data-*`: it keeps the template free of JavaScript syntax.

There is one case `data-*` cannot carry: an **object graph**. A `data-*` attribute holds a
string, so a list of places reaches a module only through a serializer the controller does
not need to own. The accepted form is one seed object on `window`, assigned by a
`th:inline` literal, with the module reading it and defaulting every field:

```html
<script th:inline="javascript">
  window.PUBLIC_PLAN_SEED = { stops: /*[[${itineraryPlaces}]]*/ [] };
</script>
<script type="module" th:src="@{/js/pages/plans-public.js}"></script>
```

```js
const stops = (window.PUBLIC_PLAN_SEED || {}).stops || [];
```

Three pages use it (`PLAN_DETAIL_SEED`, `PLACE_IMPORT_SEED`, `PUBLIC_PLAN_SEED`). It is the
same handoff as `data-*`, one scope wider, and it stays confined to the template: the
module never reads Thymeleaf.

Gate: **JS01**.

### R4 -- Inline scripts stop at 20 lines

Under 20 lines, inline is right: the behaviour belongs to the markup that uses it. Over
that, the template has become a page whose JavaScript happens to live in HTML, and it
moves to `/js/pages/<view>.js`:

```html
<script type="module" th:src="@{/js/pages/place-import.js}"></script>
```

Modules are native ES modules. No bundler, no npm, no build step -- the project vendors
its libraries and compiles Tailwind in the browser on purpose, and none of that changes.

Gate: **JS06**.

### R5 -- `const`, or `let` when it is reassigned

No `var`. Code targets ES2020: arrow functions, optional chaining, `async`/`await`. Vue 3
already sets the browser floor, so nothing here needs transpiling.

Gate: **JS02**.

### R6 -- One request helper

`/js/lib/api.js` is the only place that writes `fetch`. It adds the CSRF header from the
`_csrf` metas, turns an expired session into a sentence instead of a JSON parse error, and
raises one error type that carries the status:

```js
export class ApiError extends Error { /* message, status */ }
export async function apiFetch(url, { method, body, headers, signal } = {}) { /* ... */ }
```

`apiFetch` returns the `Response`, so the caller keeps its own `.json()` / `.text()`. It
throws `ApiError` in two cases:

- the response is a redirect or carries `text/html` -- an expired session answers `/api/*`
  with the login page and **status 200**, which is what used to reach the user as a parse
  error;
- `!response.ok`, with the API's `message` when the body has one, or the status otherwise.

A failed request -- network, abort -- is **not** wrapped. `apiFetch` awaits `fetch()`
directly, so an `AbortError` arrives with its own name and a caller that cancels on purpose
can still tell a cancel from a failure:

```js
try {
  const response = await apiFetch(url, { signal: controller.signal });
} catch (failure) {
  if (failure.name === 'AbortError') return;   // superseded by a newer request
  error.value = failure.status === 401 ? failure.message : 'No se pudo cargar.';
}
```

That is why `pages/place-import.js` uses it too, `AbortController` and all. The one direct
`fetch` left in the tree is not API traffic: `layouts/base.html` polls `/reload/version`,
exists only under the `dev` profile, and has no CSRF, no JSON and no error surface to
normalize. It stays as it is.

Callers branch on `error.status`: `409` is a product answer, `401` already carries the
right copy, everything else gets the page's own sentence.

Manual review rule; no gate id.

### R7 -- Build DOM, never markup

Two sinks, both real:

```js
// no: Leaflet parses a string popup as HTML, and place names come from
// the admin ABM and the OSM importer, neither of which sanitizes them
marker.bindPopup('<strong>' + place.name + '</strong>');

// yes
marker.bindPopup(el('div', {}, [el('strong', { text: place.name })]));
```

`innerHTML` follows the same rule: assigning a literal is fine, assigning data is not.
`textContent` is the only way a value becomes text.

`el(tag, props, children)` in `/js/lib/dom.js` is the one constructor for new code, and the
only file under `pages/` and `lib/` that calls `document.createElement`. The name of the
text prop is `text` -- it sets `textContent`:

```js
el('a', {
  class: 'text-primary fw-medium d-inline-block mt-1',
  href: directionsUrl(place),
  target: '_blank',
  rel: 'noopener',
  text: 'Cómo llegar'
})
```

Every other prop becomes an attribute, `dataset` is merged, and `null`/`undefined` props
**and children** are skipped, so `cond ? el('br') : null` is a conditional node.

Gate: **JS03**, **JS05**.

### R8 -- Errors render inline, never `alert()`

A `v-if` block bound to an `error` ref. `alert()` blocks the thread, cannot be styled, and
reads as a browser failure rather than a product message.

```js
try {
  await apiFetch(url, { method: 'POST', body });
} catch (failure) {
  error.value = failure.message;
}
```

A module with no error surface of its own does not invent one: it dispatches a `CustomEvent`
and the host page renders it. `add-to-plan.js` emits `plan-error`; `pages/places-list.js`
feeds that into the same banner as its own failures.

Every `await` on a request sits inside a `try`, and the loading flag is cleared in the
`catch` as well: a spinner that never stops is a worse failure than the original error.

Gate: **JS04**.

### R9 -- Submits are guarded

An in-flight flag, bound to `:disabled`, on every submit that leaves the page. Two native
POSTs to `/plans` because the button was clickable is a data defect, not a UX nit.

Note that a `:disabled` binding alone does not stop a form submit fired by Enter; the
handler checks the flag too.

### R10 -- Accessibility floor

Not a full audit -- four things, because they are cheap and their absence is severe:

1. Icon-only buttons carry `aria-label`.
2. `@click` on a non-interactive element carries `role`, `tabindex="0"` and `@keydown.enter`
   / `@keydown.space`.
3. A dialog moves focus into itself on open and returns it to the trigger on close.
4. `v-for` always has a stable `:key` -- an id, never the array index.

### R11 -- No remote origin at runtime

Libraries are vendored under `/js/` and `/css/`. A remote `<script src>` or stylesheet
`<link>` breaks offline runs, defeats any future CSP, and puts the supply chain on someone
else's release schedule. It also fails silently in the worst way: a missing stylesheet is a
page that renders, just wrong.

The one accepted exception is Google Fonts, because `brand.css` declares a local fallback
family: if `fonts.googleapis.com` is unreachable the typeface changes and nothing else.

Gate: **JS07**.

## File layout

```
src/main/webapp/resources/core/js/
  pages/places-list.js     the Places view: map, filters, plan panel      [shipped]
  pages/plans-detail.js    one plan's map, itinerary and catalogue        [shipped]
  pages/place-import.js    the OSM importer                              [shipped]
  pages/plans-public.js    the read-only public plan map                 [shipped]
  lib/api.js               csrfToken(), apiFetch(), ApiError             [shipped]
  lib/dom.js               el()                                          [shipped]
  lib/places-map.js        buildMarker(place, { color, icon })           [pending]
  <vendored>.js            vue.global.prod.js, tailwind-browser.js, lucide.min.js
```

`lib/api.js` and `lib/dom.js` are live, and the four extracted views import them. Nothing
under `WEB-INF/templates` builds a node by hand any more: `document.createElement` appears
once in the tree, inside `el()`. A classic script cannot `import`, so `lib/` is only usable
from a module.

`lib/places-map.js` does not exist yet: `buildMarker()` is the destination of phase 5,
when Leaflet moves from a remote `<script src>` to a vendored file. Vendored stylesheets
(Leaflet's, once it is local) belong in `resources/css/` next to `brand.css`, not at the
document root.

Naming: files `kebab-case.js` matching their view, functions `camelCase`, event handlers
prefixed `on`, module-level constants `UPPER_SNAKE`, `data-*` attributes `kebab-case`.

## The gate

```powershell
.\scripts\gate.ps1 js            # report only; exit 0
.\scripts\gate.ps1 js -Strict    # exit 1 on any FAIL rule
.\scripts\js-check.ps1 -Strict   # the checker on its own
```

A grep gate, not a linter: it reads templates and project JS, never touches the files, and
matches the shapes below. It is deliberately narrow -- a rule that needs a parser stops
being cheap enough to trust. It walks `resources/core/js` recursively, so a module under
`pages/` or `lib/` is checked exactly like a template's inline script.

| Rule | Severity | Catches |
| :--- | :--- | :--- |
| **JS01** | FAIL | `[[${`, `[[@{`, `[(${`, `[(@{` inside a `<script>` without `th:inline="javascript"` |
| **JS02** | FAIL | `var` declarations (comments excluded) |
| **JS03** | FAIL | `bindPopup(` opening with a quoted string, line break included |
| **JS04** | FAIL | `alert(` outside comments |
| **JS05** | FAIL | `innerHTML` assigned anything but a literal |
| **JS06** | INFO | an inline `<script>` over 20 lines |
| **JS07** | INFO | a `<script src>` or stylesheet `<link>` on an `http(s)://` origin, Google Fonts excepted |

INFO rules never fail the gate, with or without `-Strict`: they mark structure to fix when
the file is open anyway, not defects.

### What the gate cannot see

- `bindPopup(popupText)` -- the popup expression assigned to a variable first.
  `pages/plans-detail.js` does exactly this and is a real finding the gate reports as clean.
  Review R7 by eye there.
- `v-for` without `:key`, focus handling, `role`/`tabindex`: R10 is review-only.
- `@import` and `url()` inside a stylesheet: JS07 reads HTML tags, so a remote asset pulled
  in by CSS is invisible to it.
- Anything inside `v-html`, `insertAdjacentHTML` or a `data:` URL.
- `Vue.createApp` with `data()`: R1 is review-only, because the Options API is legal
  legacy here until its page is migrated.

### Current debt

The contract landed on a baseline of **72 FAIL, 21 INFO** -- every FAIL row was the backlog.
Each phase lowers it; this table is the live count.

| Rule | Findings | Where |
| :--- | ---: | :--- |
| JS01 | 0 | shipped in phase 1 |
| JS02 | 42 | `avatar-picker` 12, `layouts/base` 12, `landing` 10, `navbar-menu` 8 |
| JS03 | 0 | shipped in phase 1 |
| JS04 | 0 | shipped in phase 2 |
| JS05 | 0 | preventive: no `innerHTML` in the tree today |
| JS06 | 7 | `avatar-picker` 52 lines, `landing` 65 and 41, `navbar-menu` 40, `place-form` 25, `login` 22, `home` 22 |
| JS07 | 10 | Leaflet js + css in four views; Bootstrap 5.3 and Font Awesome in `plans/detail` |

**42 FAIL, 17 INFO.** CI keeps reporting until the FAIL count is zero, then the gate flips
to `-Strict`.

Phase 3 paid back what phases 1 and 2 borrowed: the four big views carried 1164 lines of
inline JavaScript between them (`places/list` 532, `plans/detail` 322, `place-import` 215,
`public` 95) and JS06 does not flag any of them now. The remaining seven are ordinary pages
under 70 lines, and they move with their own phase rather than as a batch.

## Migration order

Each phase is independently shippable and independently reviewable.

| Phase | Work | State |
| :--- | :--- | :--- |
| 0 | This document + the gate | shipped: 72 FAIL, report only |
| 1 | Fix every JS01 and JS03: `data-*` bridge, DOM popups | shipped: FAIL 72 -> 50 |
| 2 | Ship `lib/api.js`, migrate the API `fetch` calls, replace the 8 `alert()`s with an inline error surface | shipped: JS04 0, FAIL 50 -> 42 |
| 3 | Extract `places/list`, `plans/detail`, `place-import`, `plans/public` to `pages/`; ship `lib/dom.js` and consolidate every popup on it | shipped: JS06 11 -> 7 |
| 4 | Convert the 8 remaining Options API views to `setup()`: `place-form`, `places`, `user-form`, `users`, `login`, `new-user`, `home`, `plans-detail.js`. Rename each root to `#<view>-app`, E2E page objects included | R1 and R2 satisfied |
| 5 | Vendor Leaflet (js + css); extract `buildMarker()` to `lib/places-map.js`; resolve the Bootstrap/Font Awesome conflict in `plans/detail` | JS07 -> 0 |
| 6 | Flip CI to `.\scripts\gate.ps1 js -Strict` | 0 FAIL |

## Two things that are not JavaScript, but block the claims above

1. **The site does not work without JavaScript.** `layouts/base.html` sets
   `html { visibility: hidden }` and only an inline script reveals it, with no `<noscript>`
   fallback. For that reason, rule 3 of guide-vue.md ("the form still posts when the script
   fails to load") and rule 1 of guide-css-js.md are both false today. Either add the
   `<noscript>` un-hide or stop claiming progressive enhancement in the guides.
2. **A `defer` on an inline `<script>` is ignored.** `layouts/base.html` puts `defer` on
   inline scripts; the attribute only orders *external* scripts, so the ordering those
   comments describe is not the ordering the browser applies.
