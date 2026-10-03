# [LAND] Landing Page

> Trello: (pendiente — se crea en el paso 3)
> **Estimación:** 3 pts Fibonacci
> **Note:** This is an SDD proposal. Implementation may change based on team decisions.

## Objetivo

Que `/` sea la puerta de entrada real del producto. Hoy son 13 líneas con un único botón que
apunta a `/admin`, una ruta que ningún controller sirve: `LoginController` mapea `/admin/login`,
`/admin/new-user` y `/admin/home`, así que el CTA devuelve 404. `LandingController.landing()`
siempre devuelve la vista, sin importar el rol: un admin autenticado en `/` ve la misma landing
con el botón roto.

## Pre-requisitos

- [BRD] completado (tokens de diseño y nombre PlanIt). Esta spec consume `--brand-*`: no define
  colores propios.
- `LandingController` y `pages/landing.html` ya existen y responden 200 en `/`.

## Criterios de Aceptacion

| # | Criterio |
|---|----------|
| AC-01 | `/` renderiza un hero con el nombre del producto, el valor en una línea y al menos un CTA |
| AC-02 | **El CTA principal deja de apuntar a `/admin`.** No existe ruta `/admin`: `LoginController` mapea `/admin/login`, `/admin/new-user`, `/admin/home`. O se crea la ruta `/admin`, o el CTA apunta a `/admin/login` |
| AC-03 | Los CTAs cambian según el rol: anónimo ve "Crear cuenta" → `/auth/register` y "Iniciar sesión" → `/auth/login`; `USER` ve "Explorar lugares" → `/places` y "Mis planes" → `/plans`; `ADMIN` ve "Administrar usuarios" → `/admin/users` |
| AC-04 | Ningún link de la landing devuelve 404 en los tres estados de sesión |
| AC-05 | La landing explica qué es el producto: al menos 3 secciones de valor (explorar lugares, armar un plan, compartirlo) |
| AC-06 | La landing es usable en mobile: sin scroll horizontal a 375px de ancho |
| AC-07 | La landing mantiene el navbar (el fragmento `layouts/base :: layout`) y el mismo look que el resto de la app |

## Escenarios de Test

### Tests Unitarios (`presentation/landing/LandingControllerTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| U-01 | `landing()` sin `@AuthenticationPrincipal` devuelve la vista sin atributo de rol | AC-03 |
| U-02 | `landing()` con `USER` agrega el rol al model | AC-03 |
| U-03 | `landing()` con `ADMIN` agrega el rol al model | AC-03 |
| U-04 | `landing()` no redirige: siempre devuelve la vista | n/a |

### Tests de Integracion (`integration/LandingPageTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| I-01 | `GET /` sin sesión → 200, hero y CTAs de anónimo | AC-01, AC-03 |
| I-02 | `GET /` con sesión `USER` → 200, CTAs de `USER` | AC-03 |
| I-03 | `GET /` con sesión `ADMIN` → 200, CTAs de `ADMIN` | AC-03 |
| I-04 | Ningún `href` de la landing apunta a `/admin` | AC-02 |
| I-05 | Todos los `href` de la landing resuelven a una ruta mapeada (ningún 404) | AC-04 |
| I-06 | `GET /` incluye el navbar del layout | AC-07 |

### E2E (minimos)

| # | Test | Flujo | AC que cubre |
|---|------|-------|-------------|
| E-01 | `LandingViewE2E` | Anónimo → "Crear cuenta" → registro. Usuario → "Mis planes" | AC-03, AC-04 |
| E-02 | `LandingViewE2E` | Admin → "Administrar usuarios" → `/admin/users` con 200 | AC-03, AC-04 |
| E-03 | `LandingViewE2E` | Viewport 375×812: sin scroll horizontal | AC-06 |

## Notas / decisiones de diseño

- **El 404 del CTA es real, no cosmético.** Desde el hardening de [LOG], `GlobalExceptionHandler`
  devuelve 404 para URLs sin mapear (antes respondía 200 renderizando `pages/error`). El botón
  "Admin" de `landing.html:13` hoy lleva al usuario a un 404 de verdad. La opción de arreglarlo
  creando un `@GetMapping("/admin")` que redirija a `/admin/login` es una línea, pero mezcla una
  ruta de negocio con la landing: la spec la acepta como alternativa, no como preferencia.
- **La landing no decide a dónde va después de login.** `SecurityConfig` ya tiene un
  `AuthenticationSuccessHandler` por rol (`USER` → `/`, `ADMIN` → `/admin/home`). Esta spec solo
  cambia lo que se ve **en** `/`. Si más adelante se quiere que un admin autenticado en `/` vaya
  directo a su home, es `LandingController` con un redirect por rol, no un cambio en el handler:
  son dos comportamientos distintos y no se mezclan.
- **`/share/**` no necesita CTA acá**: un plan compartido se abre por link directo. Mencionarlo en
  la landing como "recibí un link" es texto, no navegación.
- **Responsive sin framework**: Tailwind corre en el navegador y `base :: head` ya declara el
  viewport meta. El responsive sale de clases `sm:`/`md:`; no hace falta CSS nuevo.
- **Conflicto de sprint**: LAND y PROF tocan `navbar.html`, así que no van en paralelo con BRD.
  Secuencia del Sprint 3: BRD → LAND. PROF es Sprint 5.

## Referencia de Implementacion

> Los pasos a continuación son guía de implementación, no reemplazan los acceptance criteria de arriba.

### 1. LandingController: exponer el rol

File: `src/main/java/com/valhalla/presentation/landing/LandingController.java`

Hoy devuelve siempre la vista sin saber quién es. Se le pasa el principal para que el template
elija los CTAs:

```java
@GetMapping("/")
public ModelAndView landing(@AuthenticationPrincipal UserDetails userDetails) {
    Map<String, Object> model = new ModelMap();
    model.put("authenticated", userDetails != null);
    model.put("isAdmin", userDetails != null
        && userDetails.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    return new ModelAndView("pages/landing", model);
}
```

No redirige: `/` es pública (`permitAll` en la cadena @Order(3) de `SecurityConfig`), y redirigir
por rol haría que un usuario logueado no pueda volver a la landing.

### 2. `pages/landing.html`: hero, valor, CTAs

File: `src/main/webapp/WEB-INF/templates/pages/landing.html`

Estructura, con los colores de [BRD]:

```html
<main class="flex-1 flex flex-col">
  <!-- Hero -->
  <section class="text-center px-4 py-16">
    <h1 class="text-5xl font-bold" style="color: var(--brand-primary)">PlanIt</h1>
    <p class="mt-4 text-lg text-gray-600 max-w-md mx-auto">
      Armá tu plan de un día en Buenos Aires y compartilo con un link
    </p>

    <!-- CTA según rol -->
    <div class="mt-8 flex flex-wrap gap-3 justify-center" th:if="${authenticated == false}">
      <a th:href="@{/auth/register}"
         class="rounded-lg px-8 py-3 text-white" style="background: var(--brand-primary)">Crear cuenta</a>
      <a th:href="@{/auth/login}"
         class="rounded-lg border px-8 py-3" style="border-color: var(--brand-primary)">Iniciar sesión</a>
    </div>
    <div class="mt-8 flex flex-wrap gap-3 justify-center"
         th:if="${authenticated == true and isAdmin == false}">
      <a th:href="@{/places}" class="rounded-lg px-8 py-3 text-white" style="background: var(--brand-primary)">Explorar lugares</a>
      <a th:href="@{/plans}" class="rounded-lg border px-8 py-3" style="border-color: var(--brand-primary)">Mis planes</a>
    </div>
    <div class="mt-8 flex flex-wrap gap-3 justify-center"
         th:if="${authenticated == true and isAdmin == true}">
      <a th:href="@{/admin/users}" class="rounded-lg px-8 py-3 text-white" style="background: var(--brand-primary)">Administrar usuarios</a>
      <a th:href="@{/places}" class="rounded-lg border px-8 py-3" style="border-color: var(--brand-primary)">Explorar lugares</a>
    </div>
  </section>

  <!-- Valor -->
  <section class="px-4 py-12 grid gap-8 sm:grid-cols-3 max-w-5xl mx-auto">
    <div>
      <h2 class="text-xl font-bold">Explorá lugares</h2>
      <p class="mt-2 text-gray-600">Mapa de Buenos Aires con filtros por categoría y búsqueda por nombre.</p>
    </div>
    <div>
      <h2 class="text-xl font-bold">Armá tu itinerario</h2>
      <p class="mt-2 text-gray-600">Ordená los lugares, asignales fecha y hora, y armá la ruta.</p>
    </div>
    <div>
      <h2 class="text-xl font-bold">Compartilo</h2>
      <p class="mt-2 text-gray-600">Un link público que cualquiera puede abrir sin crear una cuenta.</p>
    </div>
  </section>
</main>
```

Cero `th:href="@{/admin}"`: esa es la ruta que no existe. Los `href` que se usan son
`/auth/register`, `/auth/login`, `/places`, `/plans` y `/admin/users`, todas mapeadas.

### 3. Page object del E2E

`e2e/views/LandingPage.java` para el navbar, el hero y los CTAs, siguiendo el patrón de
`WebPage` / `UsersPage` que ya existe en `e2e/views/`.

## Archivos a crear/modificar

| Archivo | Accion |
|---------|--------|
| `presentation/landing/LandingController.java` | Actualizar (exponer `authenticated` / `isAdmin` al model) |
| `templates/pages/landing.html` | Actualizar (hero, 3 secciones de valor, CTAs por rol, sin `/admin`) |
| `e2e/views/LandingPage.java` | Crear |
| `e2e/LandingViewE2E.java` | Crear |