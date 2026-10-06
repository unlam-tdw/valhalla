# [AUT] Auth de Usuarios (login, registro, recuperación de password)

> Trello: https://trello.com/c/AhEeTyf7
> **Note:** This is an SDD proposal. Implementation may change based on team decisions.

## Objetivo

Los usuarios finales crean su propia cuenta, inician sesión y recuperan su password en
`/auth/**`, separado del flujo admin (`/admin/**`). El login de usuario vive en
`/auth/login`, no en `/admin/login`. Las rutas protegidas de usuario (`/places`, `/plans`)
sin sesión redirigen a `/auth/login`.

## Pre-requisitos

- [LOG] completado (Spring Security, sesiones, `User`, `LoginService.register`)
- **Modifica AC-07 de 01-LOG**: `/places` y `/plans` pasan de redirigir a `/admin/login`
  a redirigir a `/auth/login`. `/admin/**` sigue siendo admin-only.
- No hay infraestructura de email (sin spring-boot-starter-mail, sin SMTP). El mecanismo
  de recuperación no puede enviar correos (ver AC-07).

## Criterios de Aceptacion

| # | Criterio |
|---|----------|
| AC-01 | Un visitante puede crear cuenta desde `/auth/register` con email + password. Queda activa inmediatamente con rol `USER` |
| AC-02 | El email debe ser válido y la password mínimo 6 caracteres. Errores visibles en el form |
| AC-03 | Un email ya registrado no puede volver a registrarse (error en `/auth/register`) |
| AC-04 | Login en `/auth/login` con email + password correctos de una cuenta activa → inicia sesión y redirige a `/` (usuario `USER`). Un `ADMIN` va a `/admin/home` |
| AC-05 | Login con credenciales inválidas o cuenta desactivada → mensaje de error en `/auth/login` |
| AC-06 | POST `/auth/validate-login` sin token CSRF → 403 Forbidden |
| AC-07 | Recuperación de password: desde `/auth/login` hay link "Forgot password?" → `/auth/forgot-password`; con email válido la pantalla muestra una password temporal generada, y la password anterior deja de servir |
| AC-08 | Email inexistente en la recuperación → mensaje "Email no encontrado" en `/auth/forgot-password` (no se crea ni modifica nada) |
| AC-09 | `/auth/logout` invalida la sesión y redirige a `/auth/login` |
| AC-10 | Las rutas `/auth/**` están abiertas a **quien no sea admin**: un anónimo puede alcanzarlas sin sesión (si no nadie podría registrarse) y un `USER` las usa entera; un `ADMIN` que las cruce es redirigido a `/admin/home` |
| AC-11 | Rutas protegidas de usuario (`/places`, `/plans`, `/plans/**`) sin sesión → redirigen a `/auth/login` (no a `/admin/login`) |
| AC-12 | `/admin/**` requiere rol `ADMIN` **sin excepción**, `/admin/home` incluido. Solo `/admin/login` y `/admin/validate-login` quedan abiertos, porque son el punto de entrada. El flujo admin queda intacto en `/admin/login` |
| AC-13 | Navbar sin sesión: link "Login" → `/auth/login` y "Register" → `/auth/register`. Con sesión: logout |
| AC-14 | Un `ADMIN` que entre a `/admin/**` funciona normal; un `USER` y un `ADMIN` que cruzan la línea entre las dos superficies (**no** un anónimo: `/admin/login` tiene que seguir rindiendo) son devueltos a su propio home, no con un 403 |

## Escenarios de Test

### Tests Unitarios (`presentation/auth/AuthControllerTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| U-01 | `showLogin()` devuelve vista `pages/auth/user/login` | n/a |
| U-02 | `showLogin()` con `error` agrega atributo `error` al model | AC-05 |
| U-03 | `showRegister()` devuelve vista `pages/auth/user/register` con form vacío | n/a |
| U-04 | `register()` con datos válidos → redirige a `/auth/login` | AC-01 |
| U-05 | `register()` con email inválido → error de validación en `email` | AC-02 |
| U-06 | `register()` con password < 6 chars → error de validación en `password` | AC-02 |
| U-07 | `register()` con email duplicado → vuelve a `register` con error | AC-03 |
| U-08 | `showForgotPassword()` devuelve vista `pages/auth/user/forgot-password` | n/a |
| U-09 | `recover()` con email registrado → devuelve password temporal | AC-07 |
| U-10 | `recover()` con email inexistente → vista con mensaje "Email no encontrado" | AC-08 |

### Tests de Integracion (`integration/AuthControllerTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| I-01 | `GET /auth/register` → 200, vista registro | n/a |
| I-02 | `POST /auth/register` válido → redirige a `/auth/login` y el usuario puede loguearse | AC-01, AC-04 |
| I-03 | `POST /auth/register` email duplicado → 200, vista registro con error | AC-03 |
| I-04 | `POST /auth/validate-login` credenciales válidas → redirige a landing de usuario | AC-04 |
| I-05 | `POST /auth/validate-login` credenciales inválidas → redirige a `/auth/login?error=true` | AC-05 |
| I-06 | `POST /auth/validate-login` usuario desactivado → redirige a `/auth/login?error=true` | AC-05 |
| I-07 | `POST /auth/validate-login` sin CSRF → 403 | AC-06 |
| I-08 | `POST /auth/recover` email registrado → vista con password temporal; la password anterior ya no funciona y la temporal sí | AC-07 |
| I-09 | `POST /auth/recover` email inexistente → 200, vista con "Email no encontrado" | AC-08 |
| I-10 | `POST /auth/logout` → redirige a `/auth/login`, sesión invalidada | AC-09 |
| I-11 | `GET /places` sin sesión → redirige a `/auth/login` | AC-11 |
| I-12 | `GET /plans` sin sesión → redirige a `/auth/login` | AC-11 |
| I-13 | `GET /admin/users` sin sesión → redirige a `/admin/login` (sin cambios) | AC-12 |
| I-14 | `GET /auth/login` sin sesión → el navbar tiene link a `/auth/register` | AC-13 |
| I-15 | `GET /` con sesión `USER` → el navbar tiene form de logout a `/auth/logout` | AC-13 |
| I-16 | `GET /admin/users` con la sesión creada por `POST /auth/validate-login` de un `ADMIN` → 200, no 403: el login por el form de usuario lo deja en `/admin/home` y con los permisos admin intactos | AC-04, AC-14 |
| I-17 | `GET /auth/forgot-password` → 200, vista recuperación con el form ya bound (el botón `#btn-recover` renderiza) | n/a |

### Tests de Seguridad (`integration/SecurityConfigTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| S-01 | `GET /auth/login`, `/auth/register`, `/auth/forgot-password` sin sesión → 200 | AC-10 |
| S-02 | `GET /admin/users` con rol `USER` → redirige a `/` (su propio home), no 403 | AC-12, AC-14 |
| S-03 | `GET /admin/login` sin sesión → 200 (flujo admin intacto) | AC-12 |
| S-04 | `GET /places` con sesión `USER` → 200 | AC-11 |
| S-05 | `POST /auth/register` sin CSRF → 403 | AC-06 |
| S-06 | `GET /admin/home` con rol `USER` → redirige a `/`; con rol `ADMIN` → 200 | AC-12 |
| S-07 | `GET /auth/register` y `/auth/forgot-password` con rol `ADMIN` → redirige a `/admin/home` | AC-10, AC-14 |
| S-08 | `GET /auth/register` y `/auth/forgot-password` con rol `USER` → 200 | AC-10 |
| S-09 | `POST /admin/validate-login` sin CSRF → 403 (el handler compartido no convierte un fallo de CSRF en un redirect) | AC-06 |

### E2E (`UserAuthViewE2E.java`)

Happy path completo, más los errores que un usuario puede alcanzar desde el navegador. La
integración sigue siendo la capa que cubre los rechazos que el navegador impide (ver la nota
sobre el email mal formado).

| # | Test | Flujo | AC que cubre |
|---|------|-------|-------------|
| E-01 | `shouldRegisterLoginAndLandOnTheLandingPage` | `/auth/register` → crear cuenta → `/auth/login` → `/` con el navbar mostrando la sesión | AC-01, AC-04 |
| E-02 | `shouldRecoverPasswordAndSignIn` | `/auth/forgot-password` → password temporal → login → `/` | AC-07, AC-08 |
| E-03 | `shouldSignOutAndReturnToLogin` | login → navbar "Logout" → `/auth/login` con el aviso de sesión cerrada | AC-09 |
| E-04 | `shouldTellTheUserTheEmailIsAlreadyRegistered` | registrar dos veces el mismo email → alerta en el form | AC-03 |
| E-05 | `shouldRejectAPasswordShorterThanSixCharacters` | password de 3 chars → error de campo, sin `minlength` que lo frene | AC-02 |
| E-06 | `shouldNotSendAMalformedEmailToTheServer` | email mal formado → el navegador lo rechaza, el request no sale | AC-02 |
| E-07 | `shouldTellTheUserTheCredentialsAreWrong` | login con password incorrecta → alerta en `/auth/login?error=true` | AC-05 |
| E-08 | `shouldTellTheUserTheEmailIsUnknownOnRecovery` | recuperación con email inexistente → "Email no encontrado" | AC-08 |

E-03 y E-07 asertan el texto que renderiza la vista, no la URL. Un assert de path pasa igual
contra la página de error, así que no distingue un logout de un fallo de login.

## Notas / decisiones de diseño

- **Dos superficies disjuntas por rol (AC-10/12/14)**: `/admin/**` es de `ADMIN` y `/auth/**` es de
  `USER`, así que cruzar la línea con sesión abierta devuelve al principal a su propio home. Hay
  tres excepciones, y las tres son forzosas: `/admin/login` y `/admin/validate-login` quedan
  `permitAll()` porque son el punto de entrada de la sesión de admin, y `/auth/register` +
  `/auth/forgot-password` tienen que quedar abiertas para anónimos porque nadie puede registrarse
  estando adentro. "Público" en la cadena 1 significa entonces "abierto a quien **no** sea admin",
  que es lo que expresa `isAnonymous() or hasRole('USER')`: un `permitAll()` plano también
  autorizaría al `ADMIN`, que es justo lo que la regla viene a negar.
- **`/admin/home` era el agujero**: estaba en `authenticated()`, así que un `USER` común entraba al
  home del admin (200). Ahora cae bajo el `hasRole("ADMIN")` de `/admin/**`.
- **Cruce de línea: redirect, no 403 (AC-14)**: `HomeOnAccessDeniedHandler`
  (`infrastructure/security`) devuelve al principal a `LoginRedirects.landingFor(auth)`, la misma
  regla que ya define dónde aterriza una sesión recién iniciada. Un 403 con la página de error
  genérica le decía "no tenés permiso" sin decirle por dónde seguir, y en el caso inverso le
  mostraba el mismo error por haber tipeado mal una URL. Solo se instala en las cadenas 1 y 2: la
  cadena 3 (`/`, `/places`, `/plans`, `/api/**`) conserva su 403.
- **El handler tiene que respetar CSRF**: `CsrfFilter` corre antes que los filtros de
  autorización, así que un POST sin token va directo al `AccessDeniedHandler` sin pasar por
  `ExceptionTranslationFilter` (el orden de los filtros lo decide `CsrfFilter`, no el orden del
  DSL). Si el handler redirigiera ahí, un login sin token contestaría 302 y el AC-06 mudaría de
  signo sin que nadie lo decidiera; por eso las excepciones `CsrfException` se delegan al
  `AccessDeniedHandlerImpl` de Spring. Lo fijan `S-05` y `S-09`.
- **El POST de login de `/auth` no filtra por rol**: un `ADMIN` que llegue a
  `POST /auth/validate-login` sigue autenticando y aterriza en `/admin/home` con sus permisos
  intactos, aunque la página `/auth/login` ya no le sea alcanzable. Es lo que sobrevive del AC-14
  viejo y lo comprueba `I-16`: `UsernamePasswordAuthenticationFilter` autentica y corta la cadena
  antes de que corra la autorización, así que negar el endpoint por rol exigiría un success handler
  por cadena. No se hizo a propósito: el resultado observable (un admin en su home) ya es el
  correcto, y el endpoint no expone nada que el admin no pueda ver ya.
- **Recuperación (AC-07/08)**: password temporal en pantalla. Sin infraestructura de email,
  se reusa `UserService.rotatePassword(id)`. El email debe existir (si no, "Email no
  encontrado"): la pantalla de recuperación es el único lugar donde se revela la existencia
  del email, aceptado como trade-off del mecanismo.
- **Recuperación, DTO y formato (AC-07/08)**: `presentation/shared/RecoverPasswordRequest` con
  `@Email`. Un email mal formado se rechaza en el form con "Email is not valid" y nunca llega al
  service; uno bien formado que no existe responde "Email no encontrado". Son dos mensajes a
  propósito: el primero es un error de tipeo corregible, el segundo revela existencia y por eso
  solo aparece después de pasar la validación de formato.
- **Landing post-login (AC-04)**: `USER` → `/`; `ADMIN` → `/admin/home` (por rol). Apuntaba a
  `/plans`, que ningún controller sirve: el login terminaba en la página de error devuelta con
  HTTP 200, así que el destino roto era invisible. `/` sí existe (`LandingController`) y es
  público. `/plans` sigue sin construirse: es el trabajo de 03-PLN, no de este spec.
- **Registro (AC-01/02/03)**: solo email + password, con un DTO propio,
  `presentation/shared/RegisterRequest` (`@NotBlank` + `@Email` en email, `@Size(min = 6)` en
  password). **No** se reusa `NewUserRequest`: ese pide `firstName`/`lastName` y su `@NotBlank`
  está ejercitado por un test del flujo admin, así que reutilizarlo cambiaría el contrato de la
  vista admin. El service es `LoginService.register(email, password)`, que crea rol `USER` activo.
- **Email mal formado (AC-02)**: el input de `register.html` es `type="email"`, así que el
  navegador no envía el request: la rama `@Email` del DTO es inalcanzable desde la UI y solo la
  ejercitan `U-05` e `I-03`. `E-06` verifica el rechazo del navegador, que es lo que el usuario
  ve. El password, en cambio, no tiene `minlength`, así que sí llega al server y `E-05` lo cubre
  de punta a punta.
- **S-04 sin implementar**: el spec pide `GET /places` con sesión `USER` → 200, pero `/places` no
  lo sirve ningún controller. El unico test que toca `/places` es el de la redirección sin sesión
  (`I-11`), que no depende de que la ruta exista. Con el 404 real (`GlobalExceptionHandler`)
  una URL sin mapear responde 404, así que el 200 que pedía S-04 ya no es obtenible. Corregirlo
  es parte del trabajo de `/plans`.
- **404 real**: `GlobalExceptionHandler` devuelve 404 para `NoHandlerFoundException` y
  `NoResourceFoundException`, y 500 para el catch-all. Antes respondía 200 en ambos casos
  renderizando `pages/error`, lo que volvía indistinguible una URL inexistente de una válida.
  Efecto directo sobre este spec: las aserciones que sólo miran la URL ya no pueden pasar por
  alto una página de error, porque el status y el body-travel son lo que las distingue.

## Referencia de Implementacion

> Los pasos a continuación son guía de implementación, no reemplazan los acceptance criteria de arriba.

### 1. Arquitectura de seguridad: 3 filter chains

Un solo `formLogin` no soporta dos login pages. Se separa en tres `SecurityFilterChain`
ordenadas (todo en `config/SecurityConfig.java`):

| Chain | `securityMatcher` | loginPage / processingUrl | logout | Ruta éxito |
|-------|-------------------|---------------------------|--------|------------|
| 1 (`@Order(1)`) | `/auth/**` | `/auth/login` / `/auth/validate-login` | `/auth/logout` → `/auth/login?logout=true` | según rol |
| 2 (`@Order(2)`) | `/admin/**` | `/admin/login` / `/admin/validate-login` | `/admin/logout` → `/admin/login?logout=true` | `/admin/home` |
| 3 (default) | resto | `/auth/login` / `/auth/validate-login` | `/auth/logout` → `/auth/login?logout=true` | según rol |

- Las 3 comparten el mismo `UserDetailsService`, `PasswordEncoder` y `SessionRegistry`
  (max 1 sesión por usuario se mantiene).
- Chain 3: `/` y `/share/**` permitAll (no cambia), `/places/**`, `/plans/**`, resto
  `authenticated()`.
- Un `AuthenticationSuccessHandler` común: si `authorities` incluye `ROLE_ADMIN` →
  `/admin/home`, si no → `/` (AC-04).
- Chain 2 queda con la config actual de LOG (solo cambia el `securityMatcher`).
- `usernameParameter` de las cadenas 1 y 3 es `"username"`, no `"email"`: el input de
  `pages/auth/user/login.html` declara `name="username"` (con `id="email"`), mientras que las
  cadenas admin usan `name="email"`. La UI y los tests usan el selector `#email`; el nombre del
  parámetro que viaja al servidor es otro.

Ref de la estructura:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  @Order(1)
  public SecurityFilterChain authFilterChain(HttpSecurity http, SessionRegistry registry) throws Exception {
    http.securityMatcher("/auth/**")
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .formLogin(form -> form
            .loginPage("/auth/login")
            .loginProcessingUrl("/auth/validate-login")
            .usernameParameter("username")
            .passwordParameter("password")
            .successHandler(roleAwareSuccessHandler())
            .failureUrl("/auth/login?error=true")
            .permitAll())
        .logout(logout -> logout
            .logoutUrl("/auth/logout")
            .logoutSuccessUrl("/auth/login?logout=true")
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID"))
        .sessionManagement(s -> s.maximumSessions(1).sessionRegistry(registry).maxSessionsPreventsLogin(false));
    return http.build();
  }

  // chain 2 = /admin/** (config actual de LOG)
  // chain 3 = default (formLogin /auth/login)
}
```

### 2. AuthController (`presentation/auth/AuthController.java`)

- `GET /auth/login` — vista `pages/auth/user/login`, atributo `error` si `?error=true` (patrón de LOG).
- `GET /auth/register` — vista `pages/auth/user/register` con form.
- `POST /auth/register` — valida `RegisterRequest` (`@NotBlank` + `@Email` en email,
  `@Size(min = 6)` en password) → `LoginService.register(email, password)` (crea rol `USER`
  activo) → redirige `/auth/login`. Email duplicado / fallo → vuelve a `register` con error.
- `GET /auth/forgot-password` — vista `pages/auth/user/forgot-password`.
- `POST /auth/recover` — valida `RecoverPasswordRequest` (`@Email`); mal formado → vuelve al
  form con "Email is not valid" sin tocar la base. Bien formado →
  `RecoverPasswordService.recover(email)` → si el email existe, rotar password y devolver la
  temporal para mostrar (éxito); si no, mensaje "Email no encontrado" en la misma vista (AC-08).

### 3. RecoverPasswordService (`infrastructure/user/`)

Reusa lo existente: `userRepository.findByEmail(email)` + `UserService.rotatePassword(id)`
(ya genera y persiste password temporal con BCrypt). `AuthController` muestra la temporal
en la vista success. Va en `infrastructure/` y no en `domain/` porque es una clase concreta
anotada con `@Service`: las interfaces (`UserService`, `UserRepository`) se quedan en el dominio.

### 4. Templates

| Archivo | Contenido |
|---------|-----------|
| `WEB-INF/templates/pages/auth/user/login.html` | Form POST `/auth/validate-login` (email/password + CSRF), link "Forgot password?" → `/auth/forgot-password`, link "Register" → `/auth/register`. Patrón de `login.html` admin |
| `WEB-INF/templates/pages/auth/user/register.html` | Form POST `/auth/register` (email/password + CSRF + validación), link a login |
| `WEB-INF/templates/pages/auth/user/forgot-password.html` | Form POST `/auth/recover` (email + CSRF) |
| `WEB-INF/templates/pages/auth/user/recovered.html` | Muestra la password temporal (éxito de AC-07) |
| `components/navbar.html` | Sin sesión → "Login" → `/auth/login`, "Register" → `/auth/register`. Con sesión → logout según rol (`/auth/logout` para USER, `/admin/logout` para ADMIN) |

Los templates del flujo admin (`pages/auth/login.html`, `new-user.html`) no se tocan.

## Archivos a crear/modificar

| Archivo | Accion |
|---------|--------|
| `config/SecurityConfig.java` | Actualizar (3 filter chains + success handler por rol) |
| `presentation/auth/AuthController.java` | Crear |
| `presentation/shared/RegisterRequest.java`, `presentation/shared/RecoverPasswordRequest.java` | Crear (DTOs del flujo público) |
| `infrastructure/user/RecoverPasswordService.java` | Crear |
| `templates/pages/auth/user/*.html` (login, register, forgot-password, recovered) | Crear |
| `templates/components/navbar.html` | Actualizar (links `/auth/*`, logout por rol) |
| `presentation/auth/AuthControllerTest.java`, `integration/AuthControllerTest.java`, `integration/SecurityConfigTest.java` | Crear/Actualizar |
| `docs/specs/01-LOG.md` | Actualizar nota: AC-07 parcialmente reemplazado (AC-11 de este spec) |