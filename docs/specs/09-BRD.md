# [BRD] Branding e identidad visual

> Trello: https://trello.com/c/v457o0yv/11-brd-branding (`IN PROGRESS`, 3 pts Fibonacci, due 2026-10-08)
> **Estimación:** 3 pts Fibonacci
> **Estado:** implementada. Esta spec quedó alineada con el código entregado (2026-10-06); los
> criterios de abajo son los que la implementación cumple, no los que se escribieron antes de empezar.

## Objetivo

Que la aplicación se vea como un solo producto. Antes de esta card el navbar decía `UNLAM`, el
`<title>` de cuatro páginas de auth decía `Valhalla`, no había favicon, no había tipografía
declarada y los colores de categoría estaban hardcodeados dos veces en el mismo archivo. Esta spec
centraliza los tokens de diseño, fija el nombre del producto (`PlanIt`), declara una tipografía, una
marca y una identidad para compartir y para instalar, y aplica todo en las páginas que ya existen.

## Pre-requisitos

- [AUT] completed (navbar con links por rol, 3 filter chains)
- No hay build de CSS: Tailwind corre en el navegador (`resources/core/js/tailwind-browser.js`).
  Los tokens viven en un stylesheet estático bajo `resources/css/` y se exponen a Tailwind con un
  bloque `@theme` inline en el `<head>` compartido, no en un `tailwind.config.js`.

## Criterios de Aceptacion

| # | Criterio |
|---|----------|
| AC-01 | El navbar dice `PlanIt` en los tres estados: anónimo, `USER` y `ADMIN` |
| AC-02 | No queda el nombre `UNLAM` ni `Valhalla` en ninguna vista visible, ni en ningún `<title>` |
| AC-03 | Existe un token único de color por categoría: `places/list.html` no declara el mapa dos veces |
| AC-04 | Ningún hex de categoría queda literal en el JS del mapa: el color sale del token |
| AC-05 | Existe un favicon vectorial servido por la app y declarado en `layouts/base :: head` |
| AC-06 | Existe una tipografía declarada y aplicada a `body`, con respaldo de sistema detrás de la webfont |
| AC-07 | El navbar aplica los tokens de marca (color, fondo, hover) en vez de las clases sueltas actuales |
| AC-08 | Los títulos por defecto de `layouts/base :: head` dicen `PlanIt` cuando la página no pasa título |
| AC-09 | El `<head>` compartido declara la vista previa al compartir: Open Graph (`og:type`, `og:site_name`, `og:title`, `og:description`, `og:image` + `image:type`/`width`/`height`) y tarjeta de Twitter (`summary_large_image` + title/description/image), y `og:title`/`twitter:title` toman el título de la página |
| AC-10 | `og:image` es una URL absoluta y existe el asset: `resources/images/og-image.png`, PNG de 1200x630, con la marca y la tipografía de la web |
| AC-11 | La app declara `theme-color`, un web manifest y `apple-touch-icon`; el manifest se sirve con tipo JSON y declara nombre, `short_name` `PlanIt`, `start_url`, color de tema e íconos |
| AC-12 | La tipografía serif está efectivamente usada en el lockup de marca (el wordmark del navbar) y en el `h1` de la landing. Declarar una segunda tipografía sin usarla no cuenta |
| AC-13 | Los íconos de categoría salen de una librería vendorizada bajo `resources/core/js/`, sin CDN, y el color se hereda por `currentColor` en vez de viajar hardcodeado en cada SVG |
| AC-14 | Los neutros siguen en la escala por defecto de Tailwind y la decisión queda escrita en `brand.css`, junto al motivo |

## Escenarios de Test

### Tests Unitarios (`presentation/brand/BrandTokensTest.java`, 20 tests)

Leen los archivos de `src/main/webapp` como texto: es la capa que puede fijar el contrato de los
tokens sin levantar Spring.

| # | Test | AC que cubre |
|---|------|-------------|
| U-01 | `navbarCarriesNoInstitutionOrRepoName` | AC-01, AC-02 |
| U-02 | `baseHeadDeclaresTheFavicon` | AC-05 |
| U-03 | `baseHeadDeclaresAFontAppliedToBody` | AC-06 |
| U-04 | `theWebfontsLoadWithSwapAndPreconnect` / `theTypefacesKeepASystemFallbackBehindTheWebfont` | AC-06 |
| U-05 | `basePaletteAndTypefacesAreDeclaredOnce` / `theThemeLayerOnlyPointsAtTheStylesheetTokens` | AC-07 |
| U-06 | `categoryColorsLiveInOneTokenPerCategory` / `everyCategoryTokenIsDeclaredOnce` | AC-03 |
| U-07 | `categoryTokensKeepTheHexTheE2EAsserts` | AC-04 |
| U-08 | `categoryIconsComeFromTheVendoredLibrary` | AC-13 |
| U-09 | `baseHeadDefaultsTheTitleToPlanIt` / `noViewTitleCarriesTheOldNames` | AC-08, AC-02 |
| U-10 | `navbarUsesTheBrandTokens` / `navbarUsesRealUtilitiesInsteadOfArbitraryValues` | AC-07 |
| U-11 | `theSharedHeadDeclaresTheSharePreview` / `theSharePreviewTakesThePageTitle` | AC-09 |
| U-12 | `theSharedHeadDeclaresTheInstallIcons` | AC-11 |
| U-13 | `theSerifIsActuallyUsedForTheBrandLockup` | AC-12 |
| U-14 | `theNeutralScaleStaysOnTailwindAndSaysSo` | AC-14 |

### Tests de Integracion (`integration/BrandingPagesTest.java`, 11 tests)

| # | Test | AC que cubre |
|---|------|-------------|
| I-01 | `shouldBrandTheNavbarForAnonymousVisitors` | AC-01, AC-02 |
| I-02 | `shouldBrandTheNavbarForAUser` | AC-01 |
| I-03 | `shouldBrandTheNavbarForAnAdmin` | AC-01 |
| I-04 | `shouldNotTitleTheLoginPageWithTheRepoName` / `shouldNotTitleTheRegisterPageWithTheRepoName` | AC-02 |
| I-05 | `shouldServeTheFaviconAndTheFontFromTheSharedHead` / `shouldServeTheFaviconFile` | AC-05, AC-06 |
| I-06 | `shouldTakeTheCategoryColorFromTheToken` | AC-03, AC-04 |
| I-07 | `shouldServeTheInstallAndShareAssets` | AC-10, AC-11 |
| I-08 | `shouldShipAUsableWebManifest` | AC-11 |
| I-09 | `shouldRenderTheSharePreviewAndInstallIconsInTheHead` | AC-09, AC-11 |

### E2E (`e2e/BrandingViewE2E.java`, 7 tests)

| # | Test | Flujo | AC que cubre |
|---|------|-------|-------------|
| E-01 | `shouldShowTheBrandAndTheFaviconToAnAdmin` | Entra como `ADMIN`, verifica el brand y el favicon | AC-01, AC-02, AC-05 |
| E-02 | `shouldShowTheBrandAndTheFaviconToAUser` | Entra como `USER`, idem | AC-01, AC-02, AC-05 |
| E-03 | `shouldWearTheSerifOnTheBrandLockup` | El wordmark computa la serif de marca | AC-12 |
| E-04 | `shouldSetTheBodyInTheBrandSansSerif` | `body` computa la sans de marca | AC-06 |
| E-05 | `shouldKeepTheNavbarWordmarkReadableOnBlack` | Contraste del wordmark sobre el navbar negro | AC-07 |
| E-06 | `shouldPublishTheSharePreviewWithAbsoluteUrls` | `og:image` / `twitter:image` absolutas y el PNG descargable | AC-09, AC-10 |
| E-07 | `shouldNotTitleAnyPageWithTheRepoName` | Barrido de títulos | AC-02 |

E-03 y E-04 además fallan si la webfont no llegó: la serif del wordmark tiene que ser la de Google
Fonts, no el respaldo del sistema.

## Notas / decisiones de diseño

- **Los tokens no viven en el `<head>`.** La primera versión de esta spec los ponía en un `<style>`
  inline de `base :: head`. Salió un `.css` (`resources/css/brand.css`): un archivo se puede diffear,
  revisar y cachear, y un `<style>` de 40 líneas en cada render no.
- **`@theme` sí tiene que ser inline.** El build vendorizado de Tailwind compila únicamente el nodo
  DOM `style[type="text/tailwindcss"]` (`resources/core/js/tailwind-browser.js`): un `@theme` en un
  stylesheet externo nunca se procesa. El bloque no lleva ningún color propio, solo mapea
  `--color-primary: var(--primary)` y companhia, así que `brand.css` sigue siendo la única fuente y
  aparecen utilidades con nombre (`bg-primary`, `text-category-restaurant`) en vez de corchetes.
- **Los tokens crudos no pueden llamarse como las utilidades.** Una variable CSS que se referencia a
  sí misma es inválida, así que `brand.css` declara `--primary`, `--secondary`, `--typeface-sans`,
  `--typeface-serif` y el `@theme` los publica como `--color-primary`, `--color-black`,
  `--font-sans`, `--font-serif`.
- **Los neutros no son de la marca.** `gray-*` y `slate-*` quedan en la escala por defecto de Tailwind
  y no se remapean en el `@theme`. Declarar una rampa neutra propia obligaría a migrar los `gray-*`
  de todas las vistas para no dejar dos fuentes de gris: es un cambio de otro tamaño. La decisión y
  su motivo están escritos en `brand.css` y espejados en el comentario del `@theme`.
- **Inter + Playfair Display, servidas por Google Fonts.** `display=swap` no es opcional: sin él el
  texto queda invisible hasta que llega la fuente, encima del escudo anti-flash de `base :: head`.
  La pila de sistema queda detrás en ambos tokens para que la página se lea igual sin red.
- **Lucide vendorizado, como Vue y Tailwind.** Los íconos de categoría no pueden venir de un CDN: el
  E2E necesita la red estable y el repo ya no depende de un tercero para sus librerías.
  `categoryIcon()` usa `lucide.createElement(...)` y no `createIcons()`, porque los íconos viven en
  dos DOM distintos a la vez (el HTML string del marker de Leaflet y el `v-html` del panel de Vue) y
  `createIcons()` solo recorre el documento. Lucide dibuja con `stroke="currentColor"`, así que el
  color se hereda del CSS y desaparece el parámetro que antes viajaba por función.
- **La serif es voz de marca, no contenido de formulario.** Se aplica a los dos anchors
  `navbar-brand` y al `h1` de la landing. Los otros siete `<h1>` (auth, formularios) quedan en sans:
  una display serif Compuesta en un formulario se lee como adorno.
- **El wordmark quedó en 20px/700 a decisión del equipo.** `text-lg font-semibold` (18px/600) no
  alcanzaba el contraste mínimo de 4.5:1 con `text-primary` sobre `bg-black`. Subirlo a
  `font-serif text-xl font-bold` lo deja en texto grande de WCAG (≥14pt bold), donde el umbral es
  3:1: el mismo par de colores mide **3.43:1** y pasa sin tocar la paleta. El E2E mide tamaño, peso y
  ratio por separado justamente para que este número no se pueda shrinkingar con el umbral viejo.
- **`/manifest.json`, no `.webmanifest`.** Spring 6.2 no tiene esa extensión en su `mime.types`, así
  que se sirve como `application/octet-stream` y el navegador descarta el manifest entero;
  `ResourceHandlerRegistration.setMediaType` no existe en esa versión. El archivo vive en
  `resources/manifest.json` y `BaseWebConfig` lo sirve con un handler de path exacto.
- **`og:image` necesita URL absoluta y `@{}` no la da.** En un GET directo Thymeleaf devuelve una URL
  relativa, y un scraper no la baja; además `#request` ya no existe en las expresiones de Thymeleaf
  3.1. El prefijo sale del bean `BaseWebConfig.siteOrigin(HttpServletRequest)`, de scope request, que
  lo arma con scheme, host y puerto reales: detrás de un proxy el host público no es adivinable desde
  el código.
- **`#111827` aparece dos veces fuera de `brand.css`** (en `theme-color` y en el manifest). Un `<meta>`
  y un JSON no leen `var()`; es el único hex de la marca que no puede salir del token, y está
  documentado como duplicación, no como descuido.
- **`SPORT` → `Trophy` de Lucide.** Confirmado contra el resto del set: `Utensils`, `Wine`, `Coffee`,
  `Landmark`, `Trees`, `ShoppingBag`, `Music`, `BookOpen`, `Trophy`, `MapPin`.
- **Los hex de categoría no se tocaron.** `PlacesViewE2E` asserta el valor renderizado
  (`rgb(231, 76, 60)`, `stroke` computado) y `BrandTokensTest.categoryTokensKeepTheHexTheE2EAsserts`
  lo fija en la capa unitaria: cambiar un color de la paleta es un trabajo aparte de esta spec.
- **`UNLAM` viene de la institución, no del producto.** La clase del anchor ya se llamaba
  `navbar-brand`, o sea que la intención de branding estaba; lo que quedó fue el texto de la Facultad.
  Se cambió el texto, no la clase. El paquete `com.valhalla` y la base de datos `valhalla` siguen
  así a propósito: son identificadores técnicos, no texto visible.
- **Riesgo de conflicto:** BRD toca `navbar.html`, y también lo tocan LAND y PROF. Por eso van
  **secuenciales** (Sprint 3: BRD → LAND; Sprint 5: PROF), nunca en paralelo.

## Referencia de Implementacion

> Rutas relativas a la raíz del repo. La referencia muestra cómo quedó, no un plan.

### 1. Tokens en `resources/css/brand.css`

```css
:root {
  /* Paleta base */
  --white: #ffffff;
  --black: #111827;
  --gray: #6b7280;

  /* Marca */
  --primary: #2563eb;
  --secondary: #7c3aed;

  /* Una entrada por categoría. La clave es el valor de PlaceCategory. */
  --category-restaurant: #e74c3c;
  --category-bar: #9b59b6;
  /* ... las otras ocho ... */
  --category-other: #95a5a6;

  --typeface-sans: "Inter", system-ui, -apple-system, "Segoe UI", sans-serif;
  --typeface-serif: "Playfair Display", Georgia, "Times New Roman", serif;
}

body {
  font-family: var(--typeface-sans);
}
```

`layouts/base :: head` lo carga con `<link rel="stylesheet" th:href="@{/css/brand.css}">`, que
necesita el handler `/css/** -> /resources/css/` en `BaseWebConfig` y el matcher `permitAll` de
`SecurityConfig`.

### 2. La capa `@theme` inline

```html
<style type="text/tailwindcss">
  @theme {
    --color-white: var(--white);
    --color-black: var(--black);
    --color-gray: var(--gray);
    --color-primary: var(--primary);
    --color-secondary: var(--secondary);
    --color-category-restaurant: var(--category-restaurant);
    /* ... */
    --font-sans: var(--typeface-sans);
    --font-serif: var(--typeface-serif);
  }
</style>
```

Tailwind v4 solo emite una variable de `@theme` a `:root` si algo generado la referencia: `--font-serif`
no salió a `:root` hasta que una plantilla escribió `class="font-serif"`. No es un bug del token, es
cómo funciona el generador.

### 3. Nombre y marca en `components/navbar.html`

```html
<nav class="flex items-center justify-between bg-black px-4 text-white shadow">
  <a class="navbar-brand inline-block py-3 font-serif text-xl font-bold text-primary hover:text-secondary"
    sec:authorize="hasRole('ADMIN')" th:href="@{/admin/home}">PlanIt</a>
  <a class="navbar-brand inline-block py-3 font-serif text-xl font-bold text-primary hover:text-secondary"
    sec:authorize="!hasRole('ADMIN')" th:href="@{/}">PlanIt</a>
```

Los botones `rounded-md bg-gray-700` pasan a `bg-primary hover:bg-secondary`; el CTA de login usa
`bg-secondary hover:bg-primary`. La clase `navbar-brand` se conserva: es el hook que usan los tests.

### 4. Categorías e íconos en `pages/places/list.html`

El mapa guarda el **nombre del token**, no el color:

```js
const CATEGORY_TOKEN_BY_ENUM = {
  'RESTAURANT': '--category-restaurant', /* ... */ 'OTHER': '--category-other'
};

const categoryColor = (category) => {
  const rootStyle = getComputedStyle(document.documentElement);
  const token = CATEGORY_TOKEN_BY_ENUM[category] || OTHER_COLOR_TOKEN;
  /* ... trim + valor computado ... */
};

const categoryIcon = (category, size = 16) => {
  const icons = { 'RESTAURANT': 'Utensils', /* ... */ 'OTHER': 'MapPin' };
  return lucide.createElement(lucide.icons[icons[category] || icons['OTHER']], {
    width: size, height: size, 'aria-hidden': 'true'
  }).outerHTML;
};
```

El `color:#fff` del `div` del marker queda, pero es el color del icono sobre el fondo de categoría, no
un color de marca: por eso es el único hex suelto de la vista y está comentado.

### 5. `<title>` de las páginas de auth

`pages/auth/user/{login,register,forgot-password,recovered}.html` pasaron de `- Valhalla` a
`- PlanIt`. Ninguna otra vista menciona `Valhalla`.

### 6. Identidad para compartir e instalar

- `resources/images/favicon.svg` (pin de mapa `#2563eb`), `resources/images/apple-touch-icon.png`
  (180x180, a sangre completa porque iOS aplica su propia máscara) y
  `resources/images/og-image.png` (1200x630). El PNG se genera con las tipografías embebidas como
  `@font-face` en base64, así que el archivo commiteado no depende del CDN de Google Fonts al
  renderizarse.
- `resources/manifest.json` con `name`, `short_name: "PlanIt"`, `start_url: "/"`, `theme_color`,
  `background_color` e íconos.
- En `base :: head`: el bloque Open Graph / Twitter con `|${@siteOrigin.base()}@{/images/og-image.png}|`,
  `<meta name="theme-color" content="#111827">`, `<link rel="manifest" th:href="@{/manifest.json}">`
  y `<link rel="apple-touch-icon" th:href="@{/images/apple-touch-icon.png}">`.

### 7. Serif en el hero

`pages/landing.html`: el `h1` pasa a `font-serif text-5xl font-bold text-gray-900`.

## Archivos

| Archivo | Accion |
|---------|--------|
| `resources/css/brand.css` | Crear (fuente única de tokens) |
| `resources/images/favicon.svg` | Crear |
| `resources/images/apple-touch-icon.png` | Crear |
| `resources/images/og-image.png` | Crear |
| `resources/manifest.json` | Crear |
| `resources/core/js/lucide.min.js` | Crear (vendorizado, 1.52.0) |
| `java/com/valhalla/config/BaseWebConfig.java` | Actualizar (handler `/css/**` y `/manifest.json`, bean `siteOrigin`) |
| `java/com/valhalla/config/SecurityConfig.java` | Actualizar (`/css/**` y `/manifest.json` en `permitAll`) |
| `templates/layouts/base.html` | Actualizar (link a `brand.css`, `@theme`, webfonts, favicon, OG/Twitter, theme-color, manifest, apple-touch-icon, título por defecto) |
| `templates/components/navbar.html` | Actualizar (`UNLAM` → `PlanIt`, tokens de marca, serif en el lockup) |
| `templates/pages/landing.html` | Actualizar (serif en el `h1` del hero) |
| `templates/pages/places/list.html` | Actualizar (color de categoría desde el token, íconos de Lucide, sin hex sueltos ni mapa duplicado) |
| `templates/pages/auth/user/{login,register,forgot-password,recovered}.html` | Actualizar (sufijo `- Valhalla` → `- PlanIt`) |
| `test/java/com/valhalla/presentation/brand/BrandTokensTest.java` | Crear (20 tests) |
| `test/java/com/valhalla/integration/BrandingPagesTest.java` | Crear (11 tests) |
| `test/java/com/valhalla/e2e/BrandingViewE2E.java` | Crear (7 tests) |
| `test/java/com/valhalla/e2e/views/WebPage.java` | Actualizar (helper `getTitle()`) |
| `test/java/com/valhalla/e2e/LoginViewE2E.java` | Actualizar (`shouldShowUNLAMInTheNavbar` → `shouldShowPlanItInTheNavbar`) |
| `test/java/com/valhalla/e2e/PlacesViewE2E.java` | Actualizar (fija el hex de categoría que renderiza el token) |
| `test/java/com/valhalla/e2e/UserAuthViewE2E.java` | Actualizar (título de auth) |
