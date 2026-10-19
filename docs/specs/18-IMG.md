# [IMG] Imagen y banner del Plan

> Trello: https://trello.com/c/uHF3Edz2/36-img-imagen-y-banner-del-plan
> **Estimacion:** 2 pts Fibonacci
> **Note:** This is an SDD proposal. Implementation may change based on team decisions.

## Objetivo

El plan gana una imagen propia: quien puede editar el plan sube una imagen, la cambia o la
quita, y **esa misma imagen** sale en tres superficies: (1) el banner de la cabecera de la edit
page (`/plans/{id}`, slot que [DIS-014] deja preparado con imagen por defecto), (2) la vista
publica (`GET /plans/{id}/public`, ver [PVP]) y (3) la card del plan en el feed —
`GET /for-you` (ver [PPV]) y su gemelo JSON `GET /api/plans/public` (el sidebar de `/explore`) —
de modo que cada plan tiene su propia caratula.

Sin imagen cargada nada cambia de aspecto: el banner muestra un default del batch de covers de
marca elegido al azar por render (AC-10), y la card del feed mantiene el cover deterministico
por id que [PPV] ya opera (ver Decisiones).

## Pre-requisitos

- [PLN] completed (`Plan` con `isPublic`, `administrator`; admins/editores ya resueltos por
  `PlanManager` — `canEditPlan` es la misma condicion del formulario "Editar plan").
- [PVP] completed (`GET /plans/{id}/public` existe y es `permitAll`; ahi vive el segundo banner).
- [PPV] completed (`GET /for-you` y `GET /api/plans/public` comparten `PlanFeedCards`: ahi vive el
  cover que esta card le da imagen propia a; el cover generico deterministico ya esta en produccion).
- [DIS-014] landed (branch `task-17-DIS`): la cabecera de `detail.html` ya tiene el slot de
  banner con imagen por defecto; esta card lo llena, no lo inventa.
- Esquema: `JpaConfig` corre con `hibernate.hbm2ddl.auto=update` y no hay Flyway/Liquibase — la
  columna nueva es aditiva y no pide reset de DB (ver Decisiones).
- No hay CDN ni storage externo: el archivo vive en disco local y se sirve desde la app, como el
  resto de los assets (`/images/**`, `/css/**`, `/js/**` en `BaseWebConfig`).

## Decisiones / aclaraciones

- **Archivo en disco, no `bytea`.** La imagen se guarda en un directorio de datos (configurable
  por env, default `~/.valhalla/uploads`) y la columna guarda la ruta publica (`/uploads/...`),
  no los bytes. Servir 5 MB desde la DB en cada render del itinerario es el tipo de magic que
  este TP evita a proposito.
- **Un solo batch de defaults, el que ya existe.** No se abre un catalogo nuevo de banners: el
  batch small de defaults es el batch de covers genericos del feed,
  `resources/images/plans/cover-1..6.svg` (`COVER_COUNT`, [PPV]). El resolver del banner elige
  uno al azar por render; sin imagen, sin estado. Las caratulas nuevas se suman reemplazando
  assets, cero codigo.
- **El default del banner es azar por render; el del feed, identidad.** Banner: cualquiera del
  batch (AC-10) — la eleccion **no persiste** (la columna sigue `NULL`) y es de presentacion.
  Feed: el cover generico por hash del id (cover-{id % 6}) queda tal cual, porque en el feed el
  default es identidad de la card ("cada plan su imagen"): el mismo plan se ve siempre igual
  mientras no suba una. Los dos defaults coexisten a proposito; unificarlos es una decision
  de producto que no entra aqui.
- **El cover del feed ya esperaba esta columna.** `PlanFeedCards.card()` construye `cover` y su
  javadoc dice "a futuro, una columna imageUrl en Plan la reemplaza": aca se cumple. Con
  `plan.image` seteada la card la usa tal cual; sin ella, el fallback por hash. `/explore`
  hereda gratis (mismo payload JSON), el template `/for-you` ya consume `${plan.cover}` sin
  cambios de markup.
- **El campo es aditivo y no pide reset.** `hibernate.hbm2ddl.auto=update` agrega la columna
  `varchar` a `plans` sin tocar data existente; los planes vivos y los del seeder arrancan con
  `image = NULL` y su aspecto no cambia (AC-08). La advertencia habitual de `reset-db` aplica a
  remapeos de columnas, no a columnas nuevas.
- **El nombre del archivo lo decide el servidor.** Al guardar, el archivo queda como
  `{uuid-plan}.{extension}` en el directorio de uploads (los nombres de usuario nunca llegan al
  filesystem — evitar rutas travesadas es gratis si no participan). La extension aceptada viene
  de la whitelist, no del filename original.
- **Cualquier `canEditPlan` administra la imagen.** Administrador y editores comparten la
  capacidad de cambiar/quitar la imagen, igual que editan el resto del plan. Un participante
  simple solo ve el banner; un anónimo pasa al soft gate de [PVP].
- **Quitar imagen borra el archivo.** `POST /plans/{id}/image/remove` limpia la columna y elimina
  el archivo del disco (best effort: si el unlink falla, el plan queda sin imagen igual). Cambiar
  imagen reemplaza: el archivo anterior tambien se borra, no se acumulan huerfanos.
- **Clonar plan copia el archivo.** `POST /plans/{id}/clone` (ya existente) copia los bytes del
  original a un nombre nuevo y el clon apunta a su copia. Si compartieran path, que el dueno del
  original quite su imagen borraria la del clon; con copia, cada plan es dueno de la suya.
- **Multipart en WAR sin Boot.** No hay Spring Boot; `multipartResolver`
  (`StandardServletMultipartResolver`) + `MultipartConfigElement` se registran como beans y el
  `MyServletInitializer` ya pone el servlet en 3.0+ — se ajusta el max sizes ahi. CSRF y sesion
  van por el mismo camino que `/plans/join`: form nativo POST, token escondido.
- **La validacion es declarativa, no forense.** Whitelist de extension/content-type
  (`jpg jpeg png webp`), tope de tamano 5 MB, rechazo con mensaje visible y la imagen anterior
  intacta. Nada de sniffing de bytes magicos: para un TP, declarar la blacklist falsa
  (`.svg`, `.html`, `.pdf`) como rechazada por defauult es suficiente y explicito.

## Criterios de Aceptacion

| # | Criterio |
|---|----------|
| AC-01 | La cabecera de la edit page (`/plans/{id}`) muestra el banner con la imagen del plan cuando existe; sin imagen muestra una imagen por defecto elegida al azar entre un batch chico de banners de marca declarado en `resources/images` |
| AC-02 | Quien puede editar el plan (administrador o editor) ve los botones "Cambiar imagen" y "Quitar imagen" superpuestos al banner; los demas usuarios lo ven sin botones |
| AC-03 | `POST /plans/{id}/image` (multipart, CSRF, sesion) guarda la imagen del plan y redirige de vuelta a `/plans/{id}`, donde el banner ya la muestra |
| AC-04 | `POST /plans/{id}/image/remove` (CSRF, sesion) limpia el campo y borra el archivo del disco; el banner vuelve a un default aleatorio del batch |
| AC-05 | Whitelist de tipo/extension y tope de tamano: un archivo fuera de la whitelist o mayor al maximo no se guarda, no reemplaza la imagen anterior y muestra el mensaje de error en la pagina |
| AC-06 | La vista publica (`GET /plans/{id}/public`) muestra el mismo banner con la imagen del plan cuando existe; sin imagen, un default aleatorio del batch |
| AC-07 | El clon de un plan con imagen apunta a una copia propia del archivo: quitar la imagen del original no altera al clon, y viceversa |
| AC-08 | La columna `image` de `plans` es aditiva y nullable: planes existentes y seeded arrancan sin imagen y su render no cambia (solo el banner por default aleatorio del batch) |
| AC-09 | Cambiar/quitar imagen exige sesion y `canEditPlan`: POST anonimo redirige a login, un usuario ajeno al plan recibe la misma respuesta que un id inexistente, y un participante sin permiso de edicion recibe rechazo explicito |
| AC-10 | La eleccion del default del banner es per-render y de solo lectura: persiste en ninguna tabla, no determina cuota ni exclusion entre renders consecutivos |
| AC-11 | Las cards del feed —`GET /for-you` (`pages/plans/public-list.html`) y `GET /api/plans/public` (mismo payload de `PlanFeedCards`)— usan la imagen del plan como cover cuando existe; sin imagen mantienen el cover generico deterministico por hash del id (cover-1..6.svg), y el mismo plan renderiza siempre el mismo cover generico |

## Escenarios de Test

### Tests Unitarios (`domain/plan/PlanImageServiceImplTest.java`, servicio de imagen)

| # | Test | AC que cubre |
|---|------|-------------|
| U-01 | `setImage()` con extension de la whitelist guarda la ruta `{uuid}.{ext}` y actualiza el plan | AC-03, AC-05 |
| U-02 | `setImage()` con extension fuera de la whitelist (ej. `.svg`, `.html`) lanza error de validacion y no escribe archivo | AC-05 |
| U-03 | `setImage()` con bytes mayores al tope lanza error de tamano | AC-05 |
| U-04 | `removeImage()` limpia el campo del plan y (pide) borrar el archivo | AC-04 |
| U-05 | reemplazo de imagen borra el archivo anterior: no quedan dos archivos por plan | Decisiones |
| U-06 | `resolveBanner()` con plan sin imagen devuelve siempre una ruta del batch declarado (una de `cover-1..6.svg`), nunca una ruta hacia afuera del batch | AC-01, AC-10 |

### Tests Unitarios (`presentation/plan/PlanFeedCardsTest.java` y `PlanControllerTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| U-07 | `showPlan()` expone la ruta de imagen resuelta (ruta propia o default aleatorio) al template | AC-01 |
| U-08 | `showPublicPlan()` expone la misma resolucion para `public.html` | AC-06 |
| U-09 | `clonePlan()` copia el archivo de imagen a un nombre nuevo y el clon apunta a la copia | AC-07 |
| U-10 | `showPlan()` no expone botones de imagen si `canEditPlan` es falso | AC-02 |
| U-11 | `PlanFeedCards.card()` con imagen seteada pone `plan.image` en `cover`; con `image` `null` mantiene el cover por hash del id (cover-{id % 6 + 1}), y es estable para el mismo id | AC-11 |

### Tests de Integracion (`presentation/plan/PlanImageControllerIntegrationTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| I-01 | POST multipart de imagen exitoso: 302 a `/plans/{id}`, campo guardado, banner reflejado | AC-03 |
| I-02 | POST remove: columna limpia, banner en default | AC-04 |
| I-03 | POST multipart de archivo invalido: mensaje de error visible, imagen anterior intacta | AC-05 |
| I-04 | la vista publica muestra el banner con la imagen cargada | AC-06 |

### Tests de Seguridad (`PlanPublicSecurityTest` / integration de la card)

| # | Test | AC que cubre |
|---|------|-------------|
| S-01 | POST anonimo a `/plans/{id}/image` redirige a login | AC-09 |
| S-02 | POST de un usuario ajeno al plan responde igual que un id inexistente | AC-09 |
| S-03 | POST de participante sin permiso de edicion es rechazado explicitamente | AC-09 |
| S-04 | POST sin token CSRF es rechazado | AC-03 |

### Tests E2E (`e2e/PlanImageE2E.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| E-01 | flujo admin completo: entrar a `/plans/{id}`, subir imagen (banner visible), reemplazarla, quitarla (banner vuelve a un default del batch) | AC-01..AC-04 |

### Puerta

El cierre de la card es el gate completo en verde con los counts escritos en el item de Tests
(`pwsh -File scripts/gate.ps1 all`, unit+integration y E2E). Es la card que agrega suites: los
items de la checklist de Tests se cierran cuando cada capa queda verde, y el ultimo item siempre
es `pwsh -File scripts/gate.ps1 all`.

## Referencia de Implementacion

> Los pasos a continuacion son guia de implementacion, no reemplazan los criterios de aceptacion de arriba.

### 1. Campo en el dominio (AC-08)

`Plan` (domain/plan) gana `@Column private String image;` nullable. La letra no vuelve a ninguna
entidad nueva: es un dato del plan mas. Los getters/setters se exponen desde el servicio, no
desprende el formulario del plan (el update de `POST /plans/{id}` sigue tocando name/description/
fechas/visibilidad/places y no toca la imagen).

### 2. Storage + endpoints (AC-03, AC-04, AC-05)

- Directorio de datos: propiedad de configuracion (`ValhallaProperties`/env), default
  `~/.valhalla/uploads`. En Docker se monta como volumen; en dev no hace falta nada.
- Nuevo `resource handler` `/uploads/**` en `BaseWebConfig` apuntando al directorio cuando existe
  (patron `/uploads/{uuidPlan}.{ext}` — el filename que sube el usuario nunca llega al filesystem:
  el nombre lo pone el servicio).
- `PlanImageService` guarda (whitelist + tope + `Files.copy` a nombre `{uuid}.{ext}` con
  `REPLACE_EXISTING` y borrado del anterior), quita (limpia campo + unlink) y resuelve la ruta
  para los templates: con `image` seteada devuelve su ruta, sin `image` elige al azar una cover
  del batch existente (`cover-1..6.svg`) — por render, sin persistir la eleccion. El mismo
  batch/feed: `PlanFeedCards` gana la preferencia `plan.image` sobre su cover por hash.
- Controller: `POST /plans/{id}/image` y `POST /plans/{id}/image/remove`, guardados por la guard
  de `canEditPlan` del plan (la misma que ya usa "Editar plan"), `@RequestParam MultipartFile`,
  mensaje de error via flash/redirect para que el form sea GET-friendly.

### 3. Configuracion multipart (AC-03)

`SpringWebConfig`/`MyServletInitializer`: beans `multipartResolver` + `MultipartConfigElement`
(max-file-size 5 MB, max-request-size 6 MB, threshold alto para no pilear disco) .

### 4. Templates (AC-01, AC-02, AC-06)

- `detail.html` (slot de DIS-014): `<img>` resuelto, botones superpuestos
  `th:if="${canEditPlan}"` — "Cambiar imagen" abre el form multipart (label + input file +
  submit) y "Quitar imagen" es un form POST propio con CSRF. Sin JS de por medio.
- `public.html`: el mismo banner al tope, sin botones (el anónimo no edita nada).

## Archivos a crear/modificar

| Archivo | Accion |
|---------|--------|
| `domain/plan/Plan.java` | Actualizar (columna `image` nullable) |
| `domain/plan/PlanImageService(+Impl).java` | Crear (guardar/quitar/resolver, whitelist y tope, default aleatorio del batch) |
| `infrastructure/plan/...` | Actualizar (`clonePlan` copia el archivo, no comparte path) |
| `presentation/plan/...ImageController(...)`, `PlanController` | Crear/actualizar (endpoints POST image/remove, exposicion al template) |
| `presentation/plan/PlanFeedCards.java` | Actualizar (cover = `plan.image` si existe, fallback deterministico por hash del id) |
| `config/SpringWebConfig.java` + `MyServletInitializer` (+ `BaseWebConfig` para `/uploads/**`) | Actualizar (multipart config + resource handler con datos de env) |
| `templates/pages/plans/detail.html` | Actualizar (banner con imagen real, botones de imagen en el slot de DIS-014) |
| `templates/pages/plans/public.html` | Actualizar (banner de la vista publica) |
| `templates/pages/plans/public-list.html` | Verificar (ya consume `${plan.cover}` de [PPV]: no hay cambio de markup salvo alt/loading) |
| `src/test/java/com/valhalla/...` | Crear/actualizar (suites U/I/S/E de arriba) |

No se tocan: SecurityConfig (la cadena default cubre los POSTs con sesion), datos de semilla (el
seeder sigue sirviendo RCA sin imagen; puede sumar una imagen de marca para los planes pero no es
AC), ni el feed/landing.
