# [PROF] Perfil de usuario y cambio de contraseña

> Trello: (pendiente — se crea en el paso 3)
> **Estimación:** 5 pts Fibonacci
> **Note:** This is an SDD proposal. Implementation may change based on team decisions.

## Objetivo

Que un usuario pueda completar y mantener su cuenta. Hoy el recorrido entero es: se registra con
email y password, entra, crea planes, y no puede hacer nada más con su cuenta. No tiene nombre —
`/auth/register` solo pide email y password, pero `User` ya tiene `firstName`/`lastName` y
`plans/detail.html:38` los renderiza, así que salen siempre en null. No tiene perfil:
`UserController` es 100% `/admin/users/**`, no existe `/profile`. Y nunca cambia su contraseña:
`UserService.rotatePassword(id)` solo se dispara desde el panel admin o desde la recuperación con
password temporal.

## Pre-requisitos

- [AUT] completed (`/auth/**` público, sesión con `maximumSessions(1)`, `BCryptPasswordEncoder`)
- [BRD] completado (esta spec agrega un link al `navbar.html`)
- `UserController` es hoy 100% `/admin/users/**`. `/profile` es un mapeo **nuevo** en un
  `@Controller` separado, no una extensión del admin: su superficie de authorization es distinta.
- `SecurityConfig` tiene 3 filter chains (`/auth/**`, `/admin/**`, default). `/profile` cae en la
  default: sin sesión redirige a `/auth/login`.

## Criterios de Aceptacion

| # | Criterio |
|---|----------|
| AC-01 | `GET /profile` con sesión muestra los datos del usuario logueado: email (read-only), `firstName`, `lastName`, rol |
| AC-02 | `POST /profile` actualiza `firstName` / `lastName` del usuario logueado y redirige a `/profile` con un mensaje de éxito |
| AC-03 | `POST /profile` con nombre vacío devuelve errores de validación visibles, sin escribir nada |
| AC-04 | Un `USER` no puede leer ni editar el perfil de otro. El endpoint no acepta un id de usuario: toma el de la sesión |
| AC-05 | `GET /profile` sin sesión redirige a `/auth/login` |
| AC-06 | `GET /profile/change-password` muestra el form: actual, nueva, confirmación |
| AC-07 | `POST /profile/change-password` exige que la contraseña actual sea correcta. Una incorrecta vuelve al form con error y no cambia nada |
| AC-08 | `POST /profile/change-password` valida la nueva contra el `BCryptPasswordEncoder` existente (`@Size(min = 8)` y confirmación coincidente) |
| AC-09 | Después del cambio, la contraseña anterior deja de servir y la nueva sí: logout + login con la nueva |
| AC-10 | El navbar, con sesión `USER`, tiene link a `/profile` |
| AC-11 | `plans/detail.html` deja de renderizar `firstName`/`lastName` en null para un usuario que completó su perfil |

## Escenarios de Test

### Tests Unitarios (`domain/user/UserProfileTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| U-01 | `updateProfile()` copia `firstName` / `lastName` | AC-02 |
| U-02 | `updateProfile()` no toca `email` ni `role` | AC-02 |
| U-03 | `User.changePassword(new)` re-codifica el hash con el encoder | AC-08, AC-09 |
| U-04 | `User.changePassword()` no guarda la contraseña en claro | AC-09 |

### Tests Unitarios (`infrastructure/user/UserServiceTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| U-05 | `updateProfile(id, ...)` con id existente guarda el cambio | AC-02 |
| U-06 | `updateProfile(id, ...)` con id inexistente devuelve empty / error, no revienta | AC-04 |
| U-07 | `changePassword(id, wrongCurrent, nueva)` rechaza y no escribe | AC-07 |
| U-08 | `changePassword(id, current, nueva)` con `current` correcta persiste el hash nuevo | AC-08, AC-09 |
| U-09 | `changePassword(id, current, nueva)` con confirmación distinta rechaza | AC-08 |

### Tests de Integracion (`integration/ProfileControllerIntegrationTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| I-01 | `GET /profile` con sesión `USER` → 200 con email y nombres | AC-01 |
| I-02 | `GET /profile` sin sesión → redirige a `/auth/login` | AC-05 |
| I-03 | `POST /profile` válido → 302 a `/profile`, el nombre persiste | AC-02 |
| I-04 | `POST /profile` con `firstName` vacío → 200 con error de campo | AC-03 |
| I-05 | `POST /profile` no acepta `id` de otro usuario: solo escribe el de la sesión | AC-04 |
| I-06 | `GET /profile/change-password` → 200 con los 3 campos | AC-06 |
| I-07 | `POST /profile/change-password` con actual incorrecta → 200 con error, la vieja sigue sirviendo | AC-07 |
| I-08 | `POST /profile/change-password` con nueva de 5 chars → 200 con error de validación | AC-08 |
| I-09 | `POST /profile/change-password` válido → la anterior da 401 y la nueva autentica | AC-09 |
| I-10 | `GET /plans/{id}` de un usuario con perfil completo muestra el nombre, no `null` | AC-11 |
| I-11 | `GET /places` con sesión: el navbar tiene link a `/profile` | AC-10 |

### Tests de Seguridad (`integration/ProfileSecurityTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| S-01 | `POST /profile` sin token CSRF → 403 | AC-02 |
| S-02 | `POST /profile/change-password` sin token CSRF → 403 | AC-08 |
| S-03 | `GET /profile` con rol `ADMIN` responde 200 (el perfil propio es de todos los autenticados) | AC-01 |

### E2E (minimos)

| # | Test | Flujo | AC que cubre |
|---|------|-------|-------------|
| E-01 | `ProfileViewE2E` | Login → navbar "Mi perfil" → completar nombre → guardar → el nombre aparece en `/plans/{id}` | AC-01, AC-02, AC-10, AC-11 |
| E-02 | `ProfileViewE2E` | Login → cambiar contraseña con la actual incorrecta → error; con la correcta → logout → login con la nueva | AC-07, AC-08, AC-09 |

## Notas / decisiones de diseño

- **`/profile` no recibe un id de usuario.** Es la diferencia entre un perfil y un CRUD de
  administracion: el id sale de la sesión, nunca de un parámetro. Un endpoint
  `GET /profile/{id}` abriría la puerta a leer el nombre de cualquiera. `UserController` ya resuelve
  el caso admin con `/admin/users/{id}` y su `@PreAuthorize`; `/profile` no reusa esa superficie.
- **Verificar la contraseña actual es obligatorio**, no cosmético: sin ese paso, un XSS o un
  dispositivo compartido cambian la cuenta con la sesión abierta. La comparación va contra el
  `BCryptPasswordEncoder` existente, que es lo que hace `LoginService.validateLogin`. Se reusa, no
  se reimplementa el encoder.
- **`maximumSessions(1)` interactúa con el cambio de contraseña.** Cambiar la own password no
  invalida la sesión actual: si lo hiciera, el `POST` devolvería 302 a `/auth/login` y el usuario
  perdería lo que estaba escribiendo. E-02 verifica el flujo completo (cambio → logout → login con
  la nueva) justamente para que el cambio de contraseña sea observable desde el navegador.
- **El gate de JaCoCo no deja margen.** La regla de `pom.xml` pone
  `com.valhalla.domain.*` **y** `com.valhalla.presentation.*` al **100%** de LINE COVEREDRATIO a
  nivel `PACKAGE`. `User` y `Plan` están excluidas por nombre, así que un método nuevo en `User`
  sí cuenta: o tiene test, o CI falla. Los DTOs (`*Request.class`) también están excluidos.
- **No tocar `SecurityConfig`.** `/profile` cae en la cadena default, que ya manda a
  `/auth/login` a los no autenticados. Agregar una cadena sería romper la convención de 3 chains
  que [AUT] instaló.
- **Conflicto de sprint**: PROF y APL-BE tocan `SecurityConfig` si alguien lo necesita; por eso
  PROF es Sprint 5, después de APL-BE. PROF también toca `navbar.html`, así que no puede ir en
  paralelo con BRD ni con LAND.

## Referencia de Implementacion

> Los pasos a continuación son guía de implementación, no reemplazan los acceptance criteria de arriba.

### 1. Métodos de dominio en `User`

File: `src/main/java/com/valhalla/domain/user/User.java`

`User` ya tiene `firstName` y `lastName`; el admin los setea. Falta el método de cambio de
contraseña y el de actualización de perfil. Importante: `User.java` está en los `<excludes>` de
JaCoCo, pero **no** los métodos que se agreguen — si el método nuevo tiene líneas, tiene test.

```java
public void updateProfile(String firstName, String lastName) {
    this.firstName = firstName;
    this.lastName = lastName;
}

public void changePassword(String newRawPassword, PasswordEncoder encoder) {
    this.password = encoder.encode(newRawPassword);
}
```

### 2. `UserService`: profile y password

File: `src/main/java/com/valhalla/infrastructure/user/UserService.java` (interfaz en
`domain/user/UserService.java`)

- `updateProfile(Long id, String firstName, String lastName)` — busca, actualiza y guarda.
- `changePassword(Long id, String currentRaw, String newRaw)` — busca, compara con
  `passwordEncoder.matches(currentRaw, user.getPassword())`; si no coincide, devuelve error **sin
  escribir**; si coincide, `user.changePassword(newRaw, passwordEncoder)` y `save`. Devuelve la
  sesión inválida si el usuario no existe.

El encoder es el mismo bean inyectado que usa `LoginService`. `UserService.rotatePassword(id)` se
queda como está: lo usan el panel admin y la recuperación de [AUT].

### 3. `ProfileController`

File: `src/main/java/com/valhalla/presentation/profile/ProfileController.java`

```java
@Controller
@RequestMapping("/profile")
public class ProfileController {

    @GetMapping
    public ModelAndView profile(@AuthenticationPrincipal UserDetails userDetails,
                                Model model) {
        model.addAttribute("user", userService.getByEmail(userDetails.getUsername()));
        return new ModelAndView("pages/profile/edit");
    }

    @PostMapping
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                @Valid @ModelAttribute("profileForm") ProfileForm form,
                                BindingResult bindingResult) {
        if (bindingResult.hasErrors()) return "pages/profile/edit";
        userService.updateProfile(currentUserId(userDetails), form.getFirstName(), form.getLastName());
        return "redirect:/profile?updated=true";
    }

    @GetMapping("/change-password")
    public ModelAndView changePasswordForm() {
        return new ModelAndView("pages/profile/change-password", new ChangePasswordForm());
    }

    @PostMapping("/change-password")
    public String changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                 @Valid @ModelAttribute("passwordForm") ChangePasswordForm form,
                                 BindingResult bindingResult) {
        // ... validación de BindingResult, luego userService.changePassword(...)
        return "redirect:/profile/change-password?changed=true";
    }
}
```

`ProfileForm` y `ChangePasswordForm` son DTOs en `presentation/profile/` (o `presentation/shared/`),
con `@NotBlank` en los nombres y `@Size(min = 8)` + `@AssertTrue` de coincidencia en la contraseña.
Los DTOs están excluidos de JaCoCo. **El id del usuario sale siempre de la sesión**, nunca de un
parámetro del request.

### 4. Templates y navbar

| Archivo | Contenido |
|---------|-----------|
| `templates/pages/profile/edit.html` | Form de `firstName` / `lastName` con `th:action="@{/profile}"` + CSRF, email read-only, aviso con `?updated=true`, `th:errors` |
| `templates/pages/profile/change-password.html` | 3 campos de password (actual, nueva, confirmar) + CSRF + `th:errors` |
| `templates/components/navbar.html` | Link "Mi perfil" → `/profile` en el bloque de autenticado, con el token de marca de [BRD] |

`plans/detail.html:38` ya renderiza `firstName` / `lastName`: no se toca, solo se verifica que
dejen de salir en null (I-10).

## Archivos a crear/modificar

| Archivo | Accion |
|---------|--------|
| `domain/user/User.java` | Actualizar (`updateProfile`, `changePassword`) |
| `domain/user/UserService.java` | Actualizar (interface: `updateProfile`, `changePassword`) |
| `infrastructure/user/UserServiceImpl.java` | Actualizar (comparación con `passwordEncoder.matches`) |
| `presentation/profile/ProfileController.java` | Crear |
| `presentation/profile/ProfileForm.java`, `presentation/profile/ChangePasswordForm.java` | Crear (DTOs) |
| `templates/pages/profile/edit.html` | Crear |
| `templates/pages/profile/change-password.html` | Crear |
| `templates/components/navbar.html` | Actualizar (link "Mi perfil" → `/profile`) |