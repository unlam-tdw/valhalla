# [DIS] Estandandarizar disenos a lo largo del sitio

> Trello: card **[DIS] Estandandarizar disenos a lo largo del sitio** (link pendiente)
> **Estado (2026-10-22):** spec creada; implementacion pendiente en la rama `task-17-DIS`.
> La rama arranca con el fix de contraste en modo oscuro (`fix(dis)`, commit `1e8f5f1`),
> que viaja en la misma entrega pero no corresponde a ningun AC de la card.

## Objetivo

Que el sitio se vea como un solo producto: los mismos componentes visuales (cards de lugar,
botones CTA, tipografia, chips) en el landing y en la aplicacion, y los detalles de UI que
hoy quedan sueltos —iconos sin renderizar, header del plan desordenado, checkbox de
visibilidad crudo, navbar que no queda fijo— resueltos con el mismo lenguaje visual: tokens
de `brand.css`, utilidades Tailwind, y sin JS nuevo salvo lo imprescindible.

## Pre-requisitos

- Tokens de marca en `resources/css/brand.css` expuestos por el bloque `@theme` de
  `layouts/base :: head` (incluye los overrides de modo oscuro).
- Landing (`pages/landing.html`) que pinta sus iconos con `data-lucide` (libreria
  vendorizada).
- `plans/detail.html` con Vue existente: las cards del itinerario (`.place-item`) ya son
  `draggable="true"` cuando `canEditPlan`.
- `22-MOBILE` ya mergeada en main: el responsive ≤575 px, el explorador full-screen y el
  orden de acciones del detalle no se rompen (ver interacciones en Notas).

## Criterios de Aceptacion

Cada AC lleva el id del item del checklist de la card `[DIS-00N]`.

- **AC-01 ([DIS-001]):** las cards de lugar (paradas) del hero del landing se pintan con el
  mismo estilo que las cards de lugar usadas dentro de la app: borde, padding, chip de
  categoria y jerarquia de textos iguales.
- **AC-02 ([DIS-002]):** los botones CTA del landing (hero y seccion final, y todo CTA
  equivalente) comparten **un unico** estilo de padding, font-weight, radio y anillo de foco.
  El estilo vive en un solo lugar (clase compartida o fragmento), no copiado a mano por
  boton.
- **AC-03 ([DIS-003]):** las cards de Preguntas Frecuentes muestran un indicador visual de
  contenido abrible (icono/marca que cambia al abrir) que no exige conocer el patron
  accordion.
- **AC-04 ([DIS-004]):** los iconos de las cards de "Un plan que se arma una vez y no se
  vuelve a discutir" se renderizan; ninguno queda vacio.
- **AC-05 ([DIS-005]):** el tag "Publico" del mock del hero ("Un sabado en Recoleta")
  renderiza su icono.
- **AC-06 ([DIS-006]):** el navbar superior queda **sticky** (fijo arriba) al hacer scroll en
  todas las vistas, sin tapar contenido y con z-index por debajo de modales y sheets.
- **AC-07 ([DIS-007]):** la "P" del logo de la app usa una tipografia proxima a la del
  landing / la aplicacion (token de marca), no una fuente ajena.
- **AC-08 ([DIS-008]):** el header del plan en desktop: titulo a la izquierda y las acciones
  de la derecha (Configuracion / Eliminar plan / Salir del plan / Clonar plan, segun rol) en
  **la misma fila**, con separacion visible entre botones. En mobile (≤575 px) las acciones
  conservan su propia fila (22-MOBILE, AC-04).
- **AC-09 ([DIS-009]):** los tiles grises del resumen del plan (DESCRIPCION, FECHA, HORA,
  VISIBILIDAD, ADMINISTRADOR, CODIGO) tienen separacion visible entre si.
- **AC-10 ([DIS-010]):** el checkbox "Hacer publico este plan" se reemplaza por un **switch
  estilo iOS** con la paleta del sitio, con label que muestra claramente el estado
  Privado/Publico. El campo sigue enviando lo mismo en el POST (mismo name/value).
- **AC-11 ([DIS-011]):** las vistas tienen padding inferior para que la ultima seccion no
  toque el borde inferior de la pantalla. El explorador full-screen no se rompe.
- **AC-12 ([DIS-012]):** cada card de lugar del itinerario muestra un **handle** de drag &amp;
  drop en su borde izquierdo, visible solo cuando `canEditPlan`.
- **AC-13 ([DIS-013]):** "Clonar plan" se ve **solo para participantes no-administradores**;
  el plan recien clonado (del cual el clonador es admin) ya no lo muestra. El admin ve
  Eliminar/Configuracion sin Clonar.
- **AC-14 ([DIS-014]):** el titulo del plan se apoya en un **banner visual** con imagen por
  defecto, compuesto en front (tokens/CSS, sin backend). Elegir o quitar la imagen queda
  para la card [IMG].

## Escenarios de Test

### T-DIS-001 — gate completa en verde

`pwsh -File scripts/gate.ps1 all` -> BUILD SUCCESS sin regresiones: unit/integration + E2E
+ checkstyle/PMD/prettier. La verificacion de los AC-01..14 es **visual** (V-01..V-14):

- **V-01:** card del hero vs card de lugar de la app: mismo estilo a simple vista.
- **V-02:** CTA del hero vs CTA final: mismo padding y font-weight (inspeccionando el HTML).
- **V-03:** FAQ: indicador visible en las cards cerradas; cambia al abrir.
- **V-04:** beneficios: los cuatro iconos se ven.
- **V-05:** tag "Publico" del hero: icono visible.
- **V-06:** navbar: queda sticky al hacer scroll en landing, /explore, /plans y detalle.
- **V-07:** "P" del logo: tipografia proxima a la del landing.
- **V-08:** header del plan en desktop: titulo + acciones en una fila, botones separados; a
  ≤575 px las acciones vuelven a su propia fila.
- **V-09:** tiles del resumen: separados entre si.
- **V-10:** switch de visibilidad: estado Privado/Publico claro; guardar un plan publico y
  uno privado funciona (el POST no cambia).
- **V-11:** la ultima seccion de cada vista tiene aire abajo; /explore conserva su mapa
  full-screen.
- **V-12:** handle visible en las cards del itinerario siendo editor; ausente para
  lectores.
- **V-13:** participante no-admin ve Salir + Clonar; admin ve Eliminar/Configuracion sin
  Clonar; el plan recien clonado no muestra Clonar.
- **V-14:** banner atras del titulo del plan, por defecto y sin llamadas de backend (red
  del devtools quieta).

## Notas / decisiones de diseño

- **Contraste en modo oscuro ya incluido.** `fix(dis) 1e8f5f1`: `brand.css` les da
  complemento legible a los `text-blue-600` (17 links sueltos) y `text-red-600` (11 errores
  de formulario) que no llegaban a AA sobre tarjetas oscuras, los hovers rojos de los
  botones destructivos pasan a superficie roja oscura, y el hover de las cruces del
  explorador pasa a `hover:text-gray-900`. Es el reporte de QA (Mateo) durante la revision
  del PR #36; `ThemeModeTest` pinnega los overrides nuevos. Los chips con fondo pastel no se
  tocan: su fondo queda claro y su texto oscuro sigue leyendose.
- **Causa comun probable de los iconos vacios (AC-03/04/05).** Los tres items pintan iconos
  con `data-lucide` (beneficios, plus del FAQ, tag Publico). Si la libreria no inicializa
  (o lo hace despues de que el DOM ya esta pintado), los tres caen juntos: verificar la
  inicializacion antes de reemplazar iconos de a uno. Si el FAQ necesita un indicador que no
  dependa de lucide, que sea CSS puro (`summary::after`).
- **CTA unico (AC-02).** Hero y CTA final hoy repiten la misma tanda de clases a mano
  (`rounded-lg bg-primary px-3 py-2 text-base font-semibold ...`); el AC es extraerlas a un
  solo lugar para que "mismo diseno" sea estructural y no una convencion.
- **Sticky sin romper capas (AC-06).** El navbar pasa a `sticky top-0` con z-index por
  debajo de los modales del sitio y de la card de detalle del explorador (`z-[1100]`, de
  22-MOBILE). El mapa full-screen de /explore no debe empujar la barra ni quedar tapado.
- **Header del plan: hoy dos filas (AC-08).** Fila 1 = titulo + bookmark; fila 2 = acciones
  `d-flex justify-content-end` **sin gap** (por eso se tocan). Pasa a una sola fila con gap
  visible; la media query ≤575 px de 22-MOBILE (acciones a fila propia) sigue mandando en
  mobile.
- **Clonar hoy no tiene condicion (AC-13).** El form de clone viaja para todo visitante,
  incluso anonimo. La regla nueva se apoya en flags de modelo ya existentes
  (`isPlanParticipant` / `isPlanAdministrator`), la misma condicion que "Salir del plan".
  Puede haber tests/E2E que pinnen la visibilidad actual: actualizarlos forma parte del AC.
- **Switch sin JS (AC-10).** Checkbox real + `peer` + span estilado con Tailwind, label con
  el estado; `name`/`value` intactos para el form. La paleta usa tokens de marca y en oscuro
  hereda los overrides de `brand.css`.
- **Banner sin backend (AC-14).** Imagen por defecto compuesta con tokens (gradiente o SVG
  inline), no un endpoint nuevo. La card [IMG] agregara elegir/quitar imagen.
- **Padding inferior (AC-11).** Va en el layout (main) o por vista; ojo con /explore: el
  mapa full-screen de 22-MOBILE no puede heredar un padding que le robe altura.

## Archivos a crear/modificar

- `src/main/webapp/WEB-INF/templates/pages/landing.html` — cards del hero (AC-01), CTA
  unico (AC-02), indicador del FAQ (AC-03), iconos de beneficios (AC-04), tag Publico
  (AC-05).
- `src/main/webapp/WEB-INF/templates/components/navbar.html` — sticky (AC-06) y P del logo
  (AC-07).
- `src/main/webapp/WEB-INF/templates/pages/plans/detail.html` — header + acciones (AC-08),
  tiles del resumen (AC-09), switch de visibilidad (AC-10), handle de drag (AC-12),
  condicion de Clonar (AC-13), banner del titulo (AC-14).
- `src/main/webapp/WEB-INF/templates/layouts/base.html` — padding inferior si va en el
  layout (AC-11).
- `src/main/webapp/resources/css/brand.css` — tokens/reglas que apoyen los items visuales
  (ya incluye el fix de contraste del commit `1e8f5f1`).
- `src/main/webapp/resources/images/logo.svg` (o su reemplazo compuesto) — P del logo
  (AC-07).
- `src/test/java/com/valhalla/**` — actualizar los tests que pinnen la visibilidad de
  "Clonar plan" (AC-13), si los hay.
