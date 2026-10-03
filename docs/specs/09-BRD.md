# [BRD] Branding e identidad visual

> Trello: (pendiente — se crea en el paso 3)
> **Estimación:** 3 pts Fibonacci
> **Note:** This is an SDD proposal. Implementation may change based on team decisions.

## Objetivo

Que la aplicación se vea como un solo producto. Hoy el navbar dice `UNLAM`, el `<title>` de tres
páginas dice `Valhalla`, la landing y el README dicen `PlanIt`, no hay favicon, no hay tipografía
declarada y los colores de categoría están hardcodeados dos veces en el mismo archivo. Esta spec
centraliza los tokens de diseño, fija el nombre del producto y aplica todo en las páginas que ya
existen.

## Pre-requisitos

- [AUT] completed (navbar con links por rol, 3 filter chains)
- No hay build de CSS: Tailwind corre en el navegador (`resources/core/js/tailwind-browser.js`).
  Los tokens tienen que vivir en el `<head>` compartido (`layouts/base :: head`) o en un stylesheet
  estático bajo `resources/`, no en un `tailwind.config.js`.

## Criterios de Aceptacion

| # | Criterio |
|---|----------|
| AC-01 | El navbar dice `PlanIt` en los tres estados: anónimo, `USER` y `ADMIN`. Hoy dice `UNLAM` en los dos anchors de marca |
| AC-02 | No queda el nombre `UNLAM` ni `Valhalla` en ninguna vista. Hoy 3 `<title>` de `pages/auth/user/*.html` dicen `- Valhalla` |
| AC-03 | Existe un token único de color por categoría. Hoy `places/list.html` declara el mismo mapa `categoryColors` dos veces (línea ~140 y ~169), 10 hex hardcodeados en cada copia |
| AC-04 | Ningún hex de categoría queda literal en el JS del mapa: el color sale del token |
| AC-05 | Existe un favicon servido por la app y declarado en `layouts/base :: head`. Hoy no hay `<link rel="icon">` en ninguna vista |
| AC-06 | Existe una tipografía declarada, por variable CSS o por clase base, aplicada a `body`. Hoy no hay ningún `font-family` en el repo |
| AC-07 | El navbar aplica los tokens de marca (color, fondo, hover) en vez de las clases sueltas actuales |
| AC-08 | Los títulos por defecto de `layouts/base :: head` dicen `PlanIt` cuando la página no pasa título |

## Escenarios de Test

### Tests Unitarios (`presentation/brand/BrandTokensTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| U-01 | El navbar no contiene el texto `UNLAM` ni `Valhalla` | AC-01, AC-02 |
| U-02 | `layouts/base.html` declara `<link rel="icon">` | AC-05 |
| U-03 | `layouts/base.html` declara una `font-family` (o una clase que la cargue) | AC-06 |
| U-04 | El token de color existe una sola vez por categoría (el mapa no está duplicado) | AC-03, AC-04 |
| U-05 | El título por defecto de `base :: head` es `PlanIt` | AC-08 |

### Tests de Integracion (`integration/BrandingPagesTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| I-01 | `GET /` sin sesión: el brand del navbar dice `PlanIt` | AC-01, AC-02 |
| I-02 | `GET /places` con sesión `USER`: el brand del navbar dice `PlanIt` | AC-01 |
| I-03 | `GET /admin/users` con sesión `ADMIN`: el brand del navbar dice `PlanIt` | AC-01 |
| I-04 | `GET /auth/login`: el `<title>` no contiene `Valhalla` | AC-02 |
| I-05 | `GET /auth/register`: el `<title>` no contiene `Valhalla` | AC-02 |
| I-06 | `GET /places`: el mapa y el panel usan el color del token, no un hex literal | AC-03, AC-04 |

### E2E (minimos)

| # | Test | Flujo | AC que cubre |
|---|------|-------|-------------|
| E-01 | `BrandingViewE2E` | Entrar como `ADMIN` y como `USER`, verificar el brand del navbar y el favicon en ambos | AC-01, AC-02, AC-05 |

## Notas / decisiones de diseño

- **Un solo lugar para los tokens.** Las 16 vistas pasan por
  `<head th:replace="~{layouts/base :: head(...)}">`, así que favicon, tipografía y variables CSS
  globales se declaran una vez ahí y heredan todas. No hace falta tocar cada `<head>`.
- **Los tokens de categoría, no hex sueltos.** `places/list.html` tiene hoy dos copias del mapa
  `categoryColors` (una para el marker, otra para el borde/icono del panel) con los mismos 10 hex.
  Se reemplaza por una lectura de variable CSS, o por una función que derive el color de un solo
  objeto. El E2E asserta el valor renderizado (`background-color:#e74c3c`, `stroke="#e74c3c"`),
  así que el token debe seguir resolviendo al mismo hex: cambiar el valor de un color es un
  trabajo aparte de esta spec.
- **`UNLAM` viene de la institution, no del producto.** La clase del anchor ya se llama
  `navbar-brand`, o sea que la intención de branding estaba; lo que quedó fue el texto de la
 Facultad. Se cambia el texto, no la clase.
- **Los `<title>` mezclan dos nombres.** `pages/auth/user/{login,register,forgot-password,recovered}.html`
  pasan `'... - Valhalla'`, que es el nombre del repo, no del producto. Se unifican con el patrón
  `'Pantalla - PlanIt'` que ya usan el resto de las páginas.
- **Favicon**: `resources/images/place-placeholder.svg` prueba que el directorio se sirve
  (`SecurityConfig` abre `/images/**`). El favicon va ahí y se declara en `base :: head`.
- **Riesgo de conflicto**: BRD toca `navbar.html`, y también lo tocan LAND y PROF. Por eso van
  **secuenciales** (Sprint 3: BRD → LAND; Sprint 5: PROF), nunca en paralelo.

## Referencia de Implementacion

> Los pasos a continuación son guía de implementación, no reemplazan los acceptance criteria de arriba.

### 1. Tokens de diseño en `layouts/base.html`

Agregar en el fragmento `head(pageTitle)`: las variables CSS de marca y de categoría, la
tipografía y el favicon.

```html
<head th:fragment="head(pageTitle)">
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">

  <title th:text="${pageTitle != null} ? ${pageTitle} : 'PlanIt'">PlanIt</title>
  <link rel="icon" th:href="@{/images/favicon.svg}" type="image/svg+xml">

  <style>
    :root {
      --brand-primary: #2563eb;
      --brand-accent: #7c3aed;

      /* Una entrada por categoría. La clave es el valor de PlaceCategory. */
      --category-restaurant: #e74c3c;
      --category-bar: #9b59b6;
      --category-cafe: #e67e22;
      --category-museum: #3498db;
      --category-park: #2ecc71;
      --category-shopping: #f39c12;
      --category-nightlife: #1abc9c;
      --category-culture: #34495e;
      --category-sport: #e91e63;
      --category-other: #95a5a6;

      --font-sans: system-ui, -apple-system, "Segoe UI", sans-serif;
    }
  </style>
  ...
</head>
```

Las variables `--category-*` se leen desde el JS del mapa con
`getComputedStyle(document.documentElement).getPropertyValue(...)`, o se generan una sola vez al
arrancar Vue y se reusan en el marker y en el panel. Lo que no puede volver a pasar es el mismo
objeto de hex declarado dos veces.

### 2. Nombre y logo en `components/navbar.html`

```html
<a class="navbar-brand inline-block py-3 text-lg font-semibold hover:text-gray-300"
  sec:authorize="hasRole('ADMIN')" th:href="@{/admin/home}"
  style="color: var(--brand-primary)">PlanIt</a>
<a class="navbar-brand inline-block py-3 text-lg font-semibold hover:text-gray-300"
  sec:authorize="!hasRole('ADMIN')" th:href="@{/}"
  style="color: var(--brand-primary)">PlanIt</a>
```

Los botones (`rounded-md bg-gray-700`) pasan a `bg-[var(--brand-primary)]` y el CTA de login a
`bg-[var(--brand-accent)]`. La clase `navbar-brand` se conserva: es el hook que usan los tests.

### 3. `<title>` de las páginas de auth

En `pages/auth/user/login.html`, `register.html`, `forgot-password.html` y `recovered.html`,
reemplazar el sufijo `- Valhalla` por `- PlanIt`. Ninguna otra vista menciona `Valhalla`.

### 4. Favicon

Crear `resources/images/favicon.svg` con la marca. El directorio ya se sirve (`/images/**` es
`permitAll` en la cadena @Order(3) de `SecurityConfig`) y ahí vive `place-placeholder.svg`.

## Archivos a crear/modificar

| Archivo | Accion |
|---------|--------|
| `templates/layouts/base.html` | Actualizar (tokens CSS, tipografía, favicon, título por defecto) |
| `templates/components/navbar.html` | Actualizar (`UNLAM` → `PlanIt`, tokens de marca) |
| `templates/pages/places/list.html` | Actualizar (color de categoría desde el token, sin hex sueltos ni mapa duplicado) |
| `templates/pages/auth/user/{login,register,forgot-password,recovered}.html` | Actualizar (sufijo `- Valhalla` → `- PlanIt`) |
| `resources/images/favicon.svg` | Crear |