# Frontend Stack

This document explains the frontend stack of the project: **why** each piece
exists, **when** to use it, and the conventions the team must follow when
touching views.

## The golden rule

The server is the single source of truth. **All business logic lives in Java**
(domain / services). The frontend only renders, and rendering happens with
Thymeleaf + Tailwind: HTML produced by the server, styled with utility classes.

Vue.js adds client-side interactivity, and it follows one rule:

| I need to...                                              | Use                         | Because |
| :-------------------------------------------------------- | :-------------------------- | :------ |
| Render a page / reuse markup                              | Thymeleaf (templates)       | Views are server-rendered HTML |
| Style it                                                   | Tailwind (in the browser)   | Utility classes, no build step |
| Interactive forms / reactive state / component logic       | **Vue.js** (client-side)    | Reactive data binding, component model, progressive enhancement |

**Choice rule:** before adding interactive behavior, ask *"can the server answer
this?"*. If yes, use Thymeleaf and keep the logic in Java. Only when the state
belongs to the browser session (form submission, loading states, reactive UI)
use Vue. Never make Vue the platform , Thymeleaf is the foundation.

## 1. Thymeleaf , the views

- Templates live in `src/main/webapp/WEB-INF/templates/`, resolved by path:
  a controller returning `"pages/auth/login"` maps to
  `WEB-INF/templates/pages/auth/login.html`.
- Organization: `layouts/` (page chrome + `layout(content)` decorator),
  `components/` (reusable fragments), `pages/` (full pages grouped by feature).
- Pages opt into the layout: `th:replace="~{layouts/base :: layout(~{::main})}"`.
- Reusable regions are declared as fragments, e.g.
  `<div id="login-form-area" th:fragment="loginForm">`.
- The theme works in the browser: Thymeleaf's `ViewResolver` is configured with
  `TemplateMode.HTML` (natural templates , the file opens fine in a browser
  without the server).

## 2. Tailwind , the styles (in the browser)

- There is **no build step and no Node**: the vendored
  `@tailwindcss/browser` script (`resources/core/js/tailwind-browser.js`)
  scans the rendered DOM and generates the CSS on the fly.
- `layouts/base.html` loads it with `defer` and an inline script hides the page
  until the injected `<style>` is large enough (no unstyled flash).
- Write utility classes directly in the Thymeleaf templates.
- To upgrade, replace the vendored file with a newer build:
  ```shell
  curl -o src/main/webapp/resources/core/js/tailwind-browser.js \
    https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4.3.3/dist/index.global.js
  ```

## 3. Vue.js , client-side interactivity

Vue (`resources/core/js/vue.global.prod.js`, loaded in `layouts/base.html`)
handles interactive forms and reactive UI. Each page mounts its own Vue app
independently, using Thymeleaf to render initial HTML and Vue to enhance it.

### Conventions used in this project

**Progressive enhancement.** Forms keep their native `th:action` / `method="POST"`
so they work without JavaScript. Vue enhances them with loading states and
reactive UI. The server handles all business logic:

```html
<form ref="form" @submit.prevent="submit" action="#"
  th:action="@{/admin/validate-login}" method="POST">
  <input v-model="email" name="email" type="email" />
  <button :disabled="loading">Sign in</button>
</form>

<script>
  Vue.createApp({
    data() {
      return { email: '', loading: false, error: '[[${error}]]' }
    },
    methods: {
      submit() {
        this.loading = true;
        this.$refs.form.submit();  // POST nativo del browser
      }
    }
  }).mount('#login-app');
</script>
```

**Server data enters via Thymeleaf.** The server renders initial values with
`[[${variable}]]` syntax (Thymeleaf expressions inside Vue data blocks) or
via `data-*` attributes for non-string values. Vue reads these on mount and
takes over from there.

**Error handling.** The server returns the page with error in the model.
Thymeleaf renders `error: '[[${error}]]'` into Vue's data. Vue displays it
with `v-if="error"`. No client-side HTML parsing needed.

**Loading states.** Vue's reactive `loading` property disables buttons and shows
feedback during submission , no round-trip flicker.

**Each page is independent.** Every template mounts its own Vue app. Pages
without interactivity don't mount Vue at all. Vue is optional per page.

### When to use Vue vs plain JS vs Thymeleaf

| Scenario | Approach |
| :--- | :--- |
| Render data from server | Thymeleaf (`th:text`, `th:field`) |
| Style with utility classes | Tailwind (in templates) |
| Simple form with no errors/loading | Plain HTML (no JS needed) |
| Form with loading state / inline errors | Vue (`v-model`, `@submit`, `ref`) |
| Timer / clock / reactive updates | Vue (`data()`, `setInterval`) |
| Complex multi-step form | Vue component with multiple `ref()` |
| Shared state across components | Add Pinia (when needed) |

## 4. Development live reload

`jetty:run` starts a controller that answers `GET /reload/version` with
a token built from the boot id plus signatures of:

- `WEB-INF/templates/**/*.html`
- `resources/core/js/**/*.js`

`layouts/base.html` polls it in an inline loop; when the token changes it
reloads the page. **Templates and vendored JS reload live; Java changes still
require a `mvn clean jetty:run` restart.** The controller is inert in the
Docker WAR (it detects it is not running from a source checkout).

## 5. Testing the stack

- **Unit tests** (`presentation`): Mockito , controllers use simple redirects,
  no branching on request headers.
- **MockMvc integration**: MockMvc tests verify redirects and view names.
  No custom header logic to test.
- **E2E (Playwright, real browser)**: Standard navigation assertions.
  `mvn test -Dtest=LoginViewE2E` runs them and requires PostgreSQL + the app up
  (`docker compose up -d`).

## 6. Vendored scripts (pinned versions)

| File                                        | Source                                   | Size      |
| :------------------------------------------ | :--------------------------------------- | :-------- |
| `resources/core/js/tailwind-browser.js`     | `@tailwindcss/browser` 4.3.3 (jsdelivr)  | ~282 KB   |
| `resources/core/js/vue.global.prod.js`      | Vue.js 3.5.13 (jsdelivr)                 | ~158 KB   |

Pin upgrades: change one at a time, re-run the E2E suite, and update this table.
