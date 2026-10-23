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
| `src/main/webapp/resources/core/js/*.js` | covered |
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

Why: the two largest scripts in the tree -- `places/list.html` and `place-import.html` --
already use it, and it is the only form whose state can be tested without mounting a
component.

### R2 -- One root per page, named after the view

The mount target is `#<view>-app`: `#place-form-app`, `#users-app`, `#explore-app`. Not
`#app`.

Mount exactly one app per page. A fragment that mounts is a bug: the host page owns the
lifecycle of the subtree.

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
throws `ApiError` in three cases:

- the response is a redirect or carries `text/html` -- an expired session answers `/api/*`
  with the login page and **status 200**, which is what used to reach the user as a parse
  error;
- `!response.ok`, with the API's `message` when the body has one, or the status otherwise;
- the request itself fails (network, abort).

Callers branch on `error.status`: `409` is a product answer, `401` already carries the
right copy, everything else gets the page's own sentence.

One page still calls `fetch` directly: `admin/place-import.html` drives its search with an
`AbortController`, and the autocomplete needs to tell an abort apart from a failure. It
moves with its page in phase 3. The other direct call is not API traffic: `layouts/base.html`
polls `/reload/version`, exists only under the `dev` profile, and has no CSRF, no JSON and no
error surface to normalize. It stays as it is.

Manual review rule; no gate id.

### R7 -- Build DOM, never markup

Two sinks, both real:

```js
// no: Leaflet parses a string popup as HTML, and place names come from
// the admin ABM and the OSM importer, neither of which sanitizes them
marker.bindPopup('<strong>' + place.name + '</strong>');

// yes
marker.bindPopup(el('div', {}, [el('strong', { textContent: place.name })]));
```

`innerHTML` follows the same rule: assigning a literal is fine, assigning data is not.
`textContent` is the only way a value becomes text.

`el(tag, props, children)` in `/js/lib/dom.js` is the one constructor for new code. It does
not exist yet: a plain `<script>` cannot `import`, so the two popups rebuilt in phase 1 each
carry a local `createElement` helper, and phase 3 consolidates them into the shared module.

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
and the host page renders it. `add-to-plan.js` emits `plan-error`; `places/list.html` feeds
that into the same banner as its own failures.

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
  pages/<view>.js      one entry point per view: /js/pages/place-import.js
  lib/api.js           csrfToken(), apiFetch(), ApiError          [shipped]
  lib/dom.js           el()
  lib/places-map.js    buildMarker(place, { color, icon })
  <vendored>.js        vue.global.prod.js, tailwind-browser.js, lucide.min.js
```

`lib/api.js` is live and imported by the three migrated views. `pages/`, `lib/dom.js` and
`lib/places-map.js` do not exist yet: they are the destination of the migration below.
Vendored stylesheets (Leaflet's, once it is local) belong in `resources/css/` next to
`brand.css`, not at the document root.

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
being cheap enough to trust.

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

- `bindPopup(popupText)` -- the popup expression assigned to a variable first. `plans/detail.html`
  does exactly this and is a real finding the gate reports as clean. Review R7 by eye there.
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
| JS06 | 11 | `places/list` 532 lines, `plans/detail` 322, `place-import` 215, `public` 95, plus 7 more between 22 and 65 |
| JS07 | 10 | Leaflet js + css in four views; Bootstrap 5.3 and Font Awesome in `plans/detail` |

**42 FAIL, 21 INFO.** CI keeps reporting until the FAIL count is zero, then the gate flips
to `-Strict`.

JS06, and with it the INFO total, does not move until phase 3: extraction is what pays it
back. It went *up* in phases 1 and 2 (526 -> 532 lines in `places/list`), because the `data-*`
bridge, the request helper and the inline error surface all live in the page until it becomes
a module.

## Migration order

Each phase is independently shippable and independently reviewable.

| Phase | Work | Gate effect |
| :--- | :--- | :--- |
| 0 | This document + the gate | 72 FAIL, report only |
| 1 | Fix every JS01 and JS03: `data-*` bridge, DOM popups | FAIL 72 -> 50 |
| 2 | Ship `lib/api.js` and migrate 9 of the 10 API `fetch` calls to it; replace the 8 `alert()`s with an inline error surface | JS04 down to 0, FAIL 50 -> 42 |
| 3 | Extract `places/list`, `plans/detail`, `place-import`, `plans/public` to `pages/`; consolidate the two popup builders into `lib/dom.js` | JS06 down to 7 |
| 4 | Convert the 8 Options API views to `setup()` and `#<view>-app` | R1 satisfied |
| 5 | Vendor Leaflet (js + css); resolve the Bootstrap/Font Awesome conflict in `plans/detail` | JS07 -> 0 |
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
