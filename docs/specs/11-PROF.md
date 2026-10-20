# [PROF] Perfil de usuario, perfil público y cambio de contraseña

> Trello: https://trello.com/c/h1q9vG7N — card `[PROF] Perfil de usuario` (lista IN PROGRESS)
> **Estimación:** 8 pts Fibonacci
> **Branch:** `11-PROF` (desde `main`)

## Objetivo

Que un usuario tenga una cuenta completa, visible y compartible. Hoy el recorrido entero es: se
registra con email y password, entra, crea planes, y no puede hacer nada más con su cuenta. No
tiene nombre — `/auth/register` solo pide email y password, pero `User` ya tiene
`firstName`/`lastName` y `plans/detail.html:38` los renderiza, así que salen siempre en null. No
tiene perfil: `UserController` es 100% `/admin/users/**`, no existe `/profile` en el área de
usuario. Y nunca cambia su contraseña: `UserService.rotatePassword(id)` solo se dispara desde el
panel admin o desde la recuperación con password temporal.

Esta spec tiene dos superficies:

1. **Vista y edición del propio perfil** (`/profile/{id}` con id de la sesión): datos personales,
   avatar, cambio de contraseña y secciones placeholder para features futuras (Guardados `[MARK]`,
   Seguidores `[FOLLOW]`, Tema `[THEME]`).
2. **Vista pública del perfil de otro usuario** (`/profile/{id}` ajeno): read-only, sin datos
   privados (email/rol), con los planes públicos del dueño.

`GET /profile` redirige al propio `/profile/{id}`: la URL canónica siempre lleva el id, así que el
perfil es compartible desde la barra de direcciones sin ningún botón extra, y deja listo el
link-sharing de metadata (card `[META]`) a futuro.

## Pre-requisitos

- [AUT] completed (`/auth/**` público, sesión con `maximumSessions(1)`, `BCryptPasswordEncoder`)
- [BRD] completado (esta spec agrega link/entradas al `navbar.html`)
- `UserController` es hoy 100% `/admin/users/**`. `/profile` es un mapeo **nuevo** en un
  `@Controller` separado, no una extensión del admin: su superficie de authorization es distinta.
- `SecurityConfig` tiene 3 filter chains (`/auth/**`, `/admin/**`, default). `/profile` cae en la
  default: sin sesión redirige a `/auth/login`.
- No hay infraestructura de upload en el proyecto (cero `MultipartFile`): el avatar es **URL-based**
  (patrón `Place.imageUrl`), no archivo subido. El upload real será su propia card a futuro.
- `[THEME]`, `[MARK]` y `[FOLLOW]` no son bloqueantes: sus secciones entran como placeholders no
  interactivos y se activan en las cards de cada feature.

## Criterios de Aceptacion

| # | Criterio |
|---|----------|
| AC-01 | `GET /profile/{id}` con sesión propia muestra los datos del usuario logueado: email (read-only), `firstName`, `lastName`, rol |
| AC-02 | `POST /profile` (sin id en el path) actualiza `firstName` / `lastName` / `avatarUrl` del usuario de la sesión y redirige a `/profile/{id}` con un mensaje de éxito |
| AC-03 | `POST /profile` con nombre vacío devuelve errores de validación visibles, sin escribir nada |
| AC-04 | La escritura no acepta id: el usuario es siempre el de la sesión. El path `/profile/{id}` solo sirve vistas read-only de otros — nadie puede escribir sobre un id ajeno |
| AC-05 | `GET /profile` y `GET /profile/{id}` sin sesión redirigen a `/auth/login` |
| AC-06 | `GET /profile/change-password` muestra el form: actual, nueva, confirmación |
| AC-07 | `POST /profile/change-password` exige que la contraseña actual sea correcta. Una incorrecta vuelve al form con error y no cambia nada |
| AC-08 | `POST /profile/change-password` valida la nueva contra el `BCryptPasswordEncoder` existente (`@Size(min = 8)` y confirmación coincidente) |
| AC-09 | Después del cambio, la contraseña anterior deja de servir y la nueva sí: logout + login con la nueva |
| AC-10 | El navbar, con sesión `USER`, tiene acceso al perfil vía el modal del avatar |
| AC-11 | `plans/detail.html` deja de renderizar `firstName`/`lastName` en null para un usuario que completó su perfil |
| AC-12 | `GET /profile` con sesión redirige (302) a `/profile/{id}` del usuario de la sesión — la URL canónica siempre lleva el id |
| AC-13 | `GET /profile/{id}` sobre OTRO usuario muestra vista read-only: nombre, avatar/iniciales, placeholder de seguidores y los planes públicos del dueño. Nunca renderiza email ni rol de ese usuario |
| AC-14 | `GET /profile/{id}` sobre el PROPIO id muestra la superficie de edición del propio perfil (form de nombres/avatar, email read-only, rol, link a cambio de contraseña) |
| AC-15 | La vista pública lista los planes **públicos** del dueño; los privados no aparecen |
| AC-16 | `avatarUrl` es opcional: con valor se renderiza la imagen; vacío cae al avatar de iniciales (patrón actual) |
| AC-17 | Click en el avatar del navbar abre un modal con iconos y cinco entradas: Ver perfil (activa), Cambiar contraseña (activa), Guardados (pronto), Siguiendo (pronto), Cerrar sesión (activa, roja) — con la ruta de logout correcta por rol (`/admin/logout` para ADMIN, `/auth/logout` para USER) |
| AC-18 | Las entradas "pronto" (Guardados, Siguiendo) se renderizan deshabilitadas con etiqueta "Pronto", sin `<a>`/`<button>` navegables — no navegan hasta que su feature (MARK/FOLLOW) aterrice |
| AC-19 | El perfil propio contiene secciones placeholder no interactivas dentro de la página: Guardados (`#bookmarks`), Seguidores (`#follow`) y Tema (`#theme`) — ancladas y listas para activar |
| AC-20 | `GET /profile/{id}` con id inexistente → 404 |

**Fuera de alcance (explícito),** de esta spec: cambio de email (no se permite como producto),
borrado de cuenta (será card propia), upload de avatar como archivo (card propia cuando exista
infra de upload).

## Escenarios de Test

### Tests Unitarios (`domain/user/UserProfileTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| U-01 | `updateProfile()` copia `firstName` / `lastName` | AC-02 |
| U-02 | `updateProfile()` no toca `email` ni `role` | AC-02 |
| U-03 | `User.changePassword(new)` re-codifica el hash con el encoder | AC-08, AC-09 |
| U-04 | `User.changePassword()` no guarda la contraseña en claro | AC-09 |
| U-05 | `updateProfile()` guarda `avatarUrl` cuando viene con valor | AC-16 |
| U-06 | `updateProfile()` con `avatarUrl` vacío la deja en null (fallback iniciales) | AC-16 |

### Tests Unitarios (`infrastructure/user/UserServiceTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| U-07 | `updateProfile(id, ...)` con id existente guarda el cambio | AC-02 |
| U-08 | `updateProfile(id, ...)` con id inexistente devuelve empty / error, no revienta | AC-04 |
| U-09 | `changePassword(id, wrongCurrent, nueva)` rechaza y no escribe | AC-07 |
| U-10 | `changePassword(id, current, nueva)` con `current` correcta persiste el hash nuevo | AC-08, AC-09 |
| U-11 | `changePassword(id, current, nueva)` con confirmación distinta rechaza | AC-08 |

### Tests de Integracion (`integration/ProfileControllerIntegrationTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| I-01 | `GET /profile/{id}` con sesión sobre el PROPIO id → 200 con email y nombres | AC-01, AC-14 |
| I-02 | `GET /profile` sin sesión → redirige a `/auth/login` | AC-05 |
| I-03 | `POST /profile` válido → 302 a `/profile/{id}`, el nombre persiste | AC-02 |
| I-04 | `POST /profile` con `firstName` vacío → 200 con error de campo | AC-03 |
| I-05 | `POST /profile` no acepta `id` de otro usuario: solo escribe el de la sesión | AC-04 |
| I-06 | `GET /profile/change-password` → 200 con los 3 campos | AC-06 |
| I-07 | `POST /profile/change-password` con actual incorrecta → 200 con error, la vieja sigue sirviendo | AC-07 |
| I-08 | `POST /profile/change-password` con nueva de 5 chars → 200 con error de validación | AC-08 |
| I-09 | `POST /profile/change-password` válido → la anterior da 401 y la nueva autentica | AC-09 |
| I-10 | `GET /plans/{id}` de un usuario con perfil completo muestra el nombre, no `null` | AC-11 |
| I-11 | `GET /places` con sesión: el navbar tiene el avatar con modal perfil | AC-10 |
| I-12 | `GET /profile` con sesión USER → 302 a `/profile/{idPropio}` | AC-12 |
| I-13 | `GET /profile/{id}` de UN USUARIO DIFERENTE (con sesión) → 200 read-only, sin email ni rol | AC-13 |
| I-14 | El usuario NO EXISTENTE en `GET /profile/{id}` → 404 | AC-20 |
| I-15 | La vista pública del perfil muestra los planes PUBLICOS del dueño y ningún privado | AC-15 |
| I-16 | `POST /profile` con `avatarUrl` válido → el usuario lo guarda | AC-16 |
| I-17 | `POST /profile` con `avatarUrl` vacío → null; el perfil cae al fallback de iniciales | AC-16 |
| I-18 | El perfil PROPIO renderiza las secciones placeholder con sus anclas (`#bookmarks`, `#follow`, `#theme`) | AC-19 |
| I-19 | El navbar renderiza el modal con sus 5 entradas y sus estados (activas / pronto) | AC-17, AC-18 |

### Tests de Seguridad (`integration/ProfileSecurityTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| S-01 | `POST /profile` sin token CSRF → 403 | AC-02 |
| S-02 | `POST /profile/change-password` sin token CSRF → 403 | AC-08 |
| S-03 | `GET /profile/{idPropio}` con rol `ADMIN` responde 200 (el perfil propio es de todos los autenticados) | AC-01 |
| S-04 | `GET /profile/{idAjeno}` con sesión USER nunca contiene el email/rol de ese usuario (ni el texto del form de edición) | AC-13 |
| S-05 | `GET /profile` y `GET /profile/{id}` sin sesión → redirect a `/auth/login` (default chain) | AC-05, AC-12 |
| S-06 | No hay endpoint de escritura con id ajeno: `POST` sobre `/profile/{idAjeno}` no existe como superficie (405/404) | AC-04 |

### E2E (minimos)

| # | Test | Flujo | AC que cubre |
|---|------|-------|-------------|
| E-01 | `ProfileViewE2E` | Login → modal del navbar → "Ver perfil" → completar nombre → guardar → el nombre aparece en `/plans/{id}` | AC-01, AC-02, AC-10, AC-11 |
| E-02 | `ProfileViewE2E` | Login → cambiar contraseña con la actual incorrecta → error; con la correcta → logout → login con la nueva | AC-07, AC-08, AC-09 |
| E-03 | `ProfileViewE2E` | Login (USER) → click avatar → modal con 5 entradas e iconos; "Guardados"/"Siguiendo" disabled y NO navegan; "Ver perfil" lleva a `/profile/{id}`; Log Out (rojo) cierra sesión | AC-17, AC-18 |
| E-04 | `ProfileViewE2E` | Login → completar avatar URL → el avatar se renderiza; dejarlo vacío → vuelve el avatar de iniciales | AC-16 |

> Nota E2E: los helpers del navbar cuentan `body > nav a, body > nav button`. El avatar pasa de
> `<span>` a `<button>` (abre el modal), así que esos helpers y/o los items del navbar
> pueden cambiar: actualizar los helpers E2E está EN esta spec, no es un ajuste aparte. El modal
> permanece oculto (sin marcado navegable) hasta abrirse, para no inflar el conteo por defecto.

## Notas / decisiones de diseño

- **La URL canónica lleva el id: `/profile/{id}`.** `GET /profile` es solo un atajo que
  redirige. Razones: (1) el perfil es compartible desde la barra de direcciones sin botón extra;
  (2) la card `[META]` (metadata para link sharing de plans/places/perfiles) solo funciona si la
  URL lleva el id; (3) una sola route con branch `isSelf` (sesión vs path) evita duplicar
  `/my-profile` + URL pública. La sesión decide: id propio → superficie de edición (+ datos
  privados), id ajeno → vista pública read-only.
- **Qué expone la vista pública:** nombre, avatar/iniciales, contador de seguidores placeholder
  (0 — llega con [FOLLOW]) y los planes **públicos** del dueño. Email, rol y edición son de la
  superficie propia; nunca aparecen para un visitante. La diferencia entre perfil y CRUD de
  administración sigue en pie: `UserController` resuelve el caso admin con `/admin/users/{id}` y
  su `@PreAuthorize`; `/profile` no reusa esa superficie.
- **Verificar la contraseña actual es obligatorio**, no cosmético: sin ese paso, un XSS o un
  dispositivo compartido cambian la cuenta con la sesión abierta. La comparación va contra el
  `BCryptPasswordEncoder` existente, que es lo que hace `LoginService.validateLogin`. Se reusa, no
  se reimplementa el encoder.
- **`maximumSessions(1)` interactúa con el cambio de contraseña.** Cambiar la propia password no
  invalida la sesión actual: si lo hiciera, el `POST` devolvería 302 a `/auth/login` y el usuario
  perdería lo que estaba escribiendo. E-02 verifica el flujo completo (cambio → logout → login con
  la nueva) justamente para que el cambio de contraseña sea observable desde el navegador.
- **Avatar = URL, no archivo.** No hay `MultipartFile` en el proyecto: las imágenes hoy son
  strings (`Place.imageUrl`, covers `/images/plans/cover-N.svg`). El avatar sigue el mismo patrón:
  `avatarUrl` nullable en `User`, campo opcional en el form, fallback al componente de iniciales.
  El upload real (storage, validación MIME, cleanup) será card propia.
- **El modal es la caja del perfil.** Click en el avatar abre un panel con: header (iniciales +
  nombre), Ver perfil, Cambiar contraseña, Guardados (pronto), Siguiendo (pronto), Cerrar sesión
  (roja). Las entradas pronto son deshabilitadas visibles — "no interactivo hasta que la feature
  existe", no ocultas: anuncian el roadmap y mantienen el layout estable. Los anchors
  `#bookmarks` / `#follow` ya existen en la vista propia desde v2, de modo que activar MARK/FOLLOW
  es un cambio de una línea en el href. Implementación mínima (JS vanilla embebido o `<details>`),
  accesible: `aria-expanded`, cierre con Esc y click-afuera.
- **Secciones placeholder en el perfil propio:** Guardados (`#bookmarks`), Seguidores/Siguiendo
  (`#follow`) y Tema (`#theme`) con copy "a futuro"; el toggle del tema NO va al modal — cuando
  `[THEME]` se mergea, su setting de navbar se infla acá (ver card THEME). Los placeholders son
  estáticos y no agregan lógica de dominio en esta iteración.
- **El gate de JaCoCo no deja margen.** La regla de `pom.xml` pone
  `com.valhalla.domain.*` **y** `com.valhalla.presentation.*` al **100%** de LINE COVEREDRATIO a
  nivel `PACKAGE`. `User` está excluida por nombre PERO los métodos nuevos que se agreguen sí
  cuentan: o tienen test, o CI falla. Los DTOs (`*Request.class`/`*View.class`) están excluidos.
- **No tocar `SecurityConfig`.** `/profile` cae en la cadena default, que ya manda a
  `/auth/login` a los no autenticados. Agregar una cadena sería romper la convención de 3 chains
  que [AUT] instaló.
- **Conflicto de sprint**: PROF toca `navbar.html`, así que no puede ir en paralelo con BRD ni con
  LAND. Los placeholders de THEME/MARK/FOLLOW evitan que PROF compita por los mismos archivos más
  adelante.

## Referencia de Implementacion

> Los pasos a continuación son guía de implementación, no reemplazan los acceptance criteria de arriba.

### 1. Métodos de dominio en `User`

File: `src/main/java/com/valhalla/domain/user/User.java`

`User` ya tiene `firstName` y `lastName`; agrega `avatarUrl` (nullable) y los métodos de
actualización y cambio de contraseña. Importante: `User.java` está en los `<excludes>` de JaCoCo,
pero **sí** los métodos que se agreguen — si el método nuevo tiene líneas, tiene test.

```java
public void updateProfile(String firstName, String lastName, String avatarUrl) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.avatarUrl = normalize(avatarUrl); // blank -> null
}

public void changePassword(String newRawPassword, PasswordEncoder encoder) {
    this.password = encoder.encode(newRawPassword);
}
```

### 2. `UserService`: profile y password

File: `src/main/java/com/valhalla/infrastructure/user/UserService.java` (interfaz en
`domain/user/UserService.java`, impl en `infrastructure/user/UserServiceImpl.java`)

- `updateProfile(Long id, String firstName, String lastName, String avatarUrl)` — busca,
  actualiza y guarda; devuelve la sesión inválida si el usuario no existe.
- `changePassword(Long id, String currentRaw, String newRaw)` — busca, compara con
  `passwordEncoder.matches(currentRaw, user.getPassword())`; si no coincide, devuelve error **sin
  escribir**; si coincide, `user.changePassword(newRaw, passwordEncoder)` y `save`.

El encoder es el mismo bean inyectado que usa `LoginService`. `UserService.rotatePassword(id)` se
queda como está: lo usan el panel admin y la recuperación de [AUT].

### 3. `ProfileController` y vistas

File: `src/main/java/com/valhalla/presentation/profile/ProfileController.java`

```java
@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;
    private final PlanQueryService planQueryService; // planes públicos del dueño

    @GetMapping
    public String own(@AuthenticationPrincipal AppUserDetails me) {
        return "redirect:/profile/" + me.getId();
    }

    @GetMapping("/{id:\\d+}")
    public ModelAndView view(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails me) {
        User owner = userService.getById(id); // empty -> 404 (AC-20)
        boolean self = owner.getId().equals(me.getId());
        if (self) return editView(owner);                       // AC-01/AC-14
        return publicView(owner, planQueryService.publicPlansOf(owner)); // AC-13/AC-15
    }

    @PostMapping
    public String updateProfile(@AuthenticationPrincipal AppUserDetails me,
                                @Valid @ModelAttribute("profileForm") ProfileForm form,
                                BindingResult bindingResult) {
        if (bindingResult.hasErrors()) return "pages/profile/edit";
        userService.updateProfile(me.getId(), form.getFirstName(),
                                  form.getLastName(), form.getAvatarUrl());
        return "redirect:/profile/" + me.getId() + "?updated=true";
    }

    @GetMapping("/change-password")
    public ModelAndView changePasswordForm() { ... }   // igual a v1

    @PostMapping("/change-password")
    public String changePassword(@AuthenticationPrincipal AppUserDetails me,
                                 @Valid @ModelAttribute("passwordForm") ChangePasswordForm form,
                                 BindingResult bindingResult) { ... } // igual a v1
}
```

- `ProfileForm` (nombres + `avatarUrl` opcional), `ChangePasswordForm` — DTOs en
  `presentation/profile/`, excluidos de JaCoCo. El usuario de la **escritura** sale SIEMPRE de la
  sesión; el path solo alimenta la vista.
- DTO de vista pública `PublicProfileView` (nombre, iniciales/avatar, contador placeholder,
  lista de planes públicos) en el mismo paquete.
- El mapeo `/{id:\\d+}` evita colisiones con los sub-paths existentes (`/profile/change-password`).

### 4. Templates

| Archivo | Contenido |
|---------|-----------|
| `templates/pages/profile/edit.html` | Superficie propia: form de `firstName`/`lastName`/`avatarUrl` con `th:action="@{/profile}"` + CSRF, email read-only, rol, link a `/profile/change-password`, aviso `?updated=true`, `th:errors`. Debajo, secciones placeholder con anclas y copy "a futuro": Guardados (`id="bookmarks"`), Seguidores (`id="follow"`), Tema (`id="theme"`) |
| `templates/pages/profile/view.html` | Vista pública: header (avatar/iniciales + nombre + contador placeholder), grilla/lista de `PublicProfileView.plans` reutilizando los patrones de `plans/public-list.html` |
| `templates/components/navbar.html` | El avatar pasa de `<span>` a `<button>` que abre el modal. Modal: header (iniciales + nombre), Ver perfil → `/profile/{id}`, Cambiar contraseña, Guardados y Siguiendo **deshabilitados con badge "Pronto"**, Cerrar sesión (roja) con ruta por rol (`/admin/logout` vs `/auth/logout`) + CSRF. Cierre con Esc/click afuera, `aria-expanded` |

`plans/detail.html:38` ya renderiza `firstName`/`lastName`: no se toca, solo se verifica que
dejen de salir en null (I-10).

## Archivos a crear/modificar

| Archivo | Accion |
|---------|--------|
| `domain/user/User.java` | Actualizar (`avatarUrl`, `updateProfile`, `changePassword`) |
| `domain/user/UserService.java` | Actualizar (interface: `updateProfile` con avatar) |
| `infrastructure/user/UserServiceImpl.java` | Actualizar (persistencia del nuevo campo) |
| `presentation/profile/ProfileController.java` | Crear (`/profile` redirect, `/{id}` dual, `POST /profile`, change-password) |
| `presentation/profile/ProfileForm.java`, `ChangePasswordForm.java`, `PublicProfileView.java` | Crear (DTOs) |
| `presentation/shared/` o `domain/plan/` | Consulta de planes públicos por owner (según dónde viva hoy la query de planes) |
| `templates/pages/profile/edit.html` | Crear (superficie propia + placeholders) |
| `templates/pages/profile/view.html` | Crear (vista pública + planes públicos) |
| `templates/pages/profile/change-password.html` | Crear |
| `templates/components/navbar.html` | Actualizar (avatar → button + modal con 5 entradas) |
| helpers E2E de navbar | Actualizar (conteo `nav a, nav button` con el avatar ahora button) |
