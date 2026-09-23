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
| AC-04 | Login en `/auth/login` con email + password correctos de una cuenta activa → inicia sesión y redirige a `/plans` (usuario `USER`). Un `ADMIN` va a `/admin/home` |
| AC-05 | Login con credenciales inválidas o cuenta desactivada → mensaje de error en `/auth/login` |
| AC-06 | POST `/auth/validate-login` sin token CSRF → 403 Forbidden |
| AC-07 | Recuperación de password: desde `/auth/login` hay link "Forgot password?" → `/auth/forgot-password`; con email válido la pantalla muestra una password temporal generada, y la password anterior deja de servir |
| AC-08 | Email inexistente en la recuperación → mensaje "Email no encontrado" en `/auth/forgot-password` (no se crea ni modifica nada) |
| AC-09 | `/auth/logout` invalida la sesión y redirige a `/auth/login` |
| AC-10 | Las rutas `/auth/**` son públicas (no requieren sesión) |
| AC-11 | Rutas protegidas de usuario (`/places`, `/plans`, `/plans/**`) sin sesión → redirigen a `/auth/login` (no a `/admin/login`) |
| AC-12 | `/admin/**` sigue requiriendo rol `ADMIN`. El flujo admin queda intacto en `/admin/login` |
| AC-13 | Navbar sin sesión: link "Login" → `/auth/login` y "Register" → `/auth/register`. Con sesión: logout |
| AC-14 | Un `ADMIN` que se loguea por `/auth/login` (entró a una ruta protegida de usuario) se redirige a `/admin/home` y conserva permisos admin |

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

### Tests de Seguridad (`integration/SecurityConfigTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| S-01 | `GET /auth/login`, `/auth/register`, `/auth/forgot-password` sin sesión → 200 | AC-10 |
| S-02 | `GET /admin/users` con rol `USER` → 403 | AC-12 |
| S-03 | `GET /admin/login` sin sesión → 200 (flujo admin intacto) | AC-12 |
| S-04 | `GET /places` con sesión `USER` → 200 | AC-11 |
| S-05 | `POST /auth/register` sin CSRF → 403 | AC-06 |

### E2E (mínimos, solo happy path completo)

| # | Test | Flujo | AC que cubre |
|---|------|-------|-------------|
| E-01 | `shouldRegisterLoginAndLandOnPlans` | `/auth/register` → crear cuenta → `/auth/login` → `/plans` | AC-01, AC-04 |
| E-02 | `shouldRecoverPasswordAndSignIn` | `/auth/forgot-password` → password temporal → login → `/plans` | AC-07, AC-08 |

## Notas / decisiones de diseño

- **Recuperación (AC-07/08)**: password temporal en pantalla. Sin infraestructura de email,
  se reusa `UserService.rotatePassword(id)`. El email debe existir (si no, "Email no
  encontrado"): la pantalla de recuperación es el único lugar donde se revela la existencia
  del email, aceptado como trade-off del mecanismo.
- **Landing post-login (AC-04)**: `USER` → `/plans`; `ADMIN` → `/admin/home` (por rol).
- **Registro (AC-01/02/03)**: solo email + password, reusando `NewUserRequest` y
  `LoginService.register(email, password, "", "")` (crea rol `USER` activo).

## Referencia de Implementacion

> Los pasos a continuación son guía de implementación, no reemplazan los acceptance criteria de arriba.

### 1. Arquitectura de seguridad: 3 filter chains

Un solo `formLogin` no soporta dos login pages. Se separa en tres `SecurityFilterChain`
ordenadas (todo en `config/SecurityConfig.java`):

| Chain | `securityMatcher` | loginPage / processingUrl | logout | Ruta éxito |
|-------|-------------------|---------------------------|--------|------------|
| 1 (`@Order(1)`) | `/auth/**` | `/auth/login` / `/auth/validate-login` | `/auth/logout` → `/auth/login` | según rol |
| 2 (`@Order(2)`) | `/admin/**` | `/admin/login` / `/admin/validate-login` | `/admin/logout` → `/admin/login` | `/admin/home` |
| 3 (default) | resto | `/auth/login` / `/auth/validate-login` | `/auth/logout` → `/auth/login` | según rol |

- Las 3 comparten el mismo `UserDetailsService`, `PasswordEncoder` y `SessionRegistry`
  (max 1 sesión por usuario se mantiene).
- Chain 3: `/` y `/share/**` permitAll (no cambia), `/places/**`, `/plans/**`, resto
  `authenticated()`.
- Un `AuthenticationSuccessHandler` común: si `authorities` incluye `ROLE_ADMIN` →
  `/admin/home`, si no → `/plans` (AC-04).
- Chain 2 queda con la config actual de LOG (solo cambia el `securityMatcher`).

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
            .usernameParameter("email")
            .passwordParameter("password")
            .successHandler(roleAwareSuccessHandler())
            .failureUrl("/auth/login?error=true")
            .permitAll())
        .logout(logout -> logout
            .logoutUrl("/auth/logout")
            .logoutSuccessUrl("/auth/login")
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
- `POST /auth/register` — valida (`NewUserRequest` existente: email + password) →
  `LoginService.register(email, password, "", "")` (ya existe, crea rol `USER` activo) →
  redirige `/auth/login`. Email duplicado / fallo → vuelve a `register` con error.
- `GET /auth/forgot-password` — vista `pages/auth/user/forgot-password`.
- `POST /auth/recover` — `RecoverPasswordService.recover(email)` → si el email existe,
  rotar password y devolver la temporal para mostrar (éxito); si no, mensaje
  "Email no encontrado" en la misma vista (AC-08).

### 3. RecoverPasswordService (`domain/user/` o `domain/login/`)

Reusa lo existente: `userRepository.findByEmail(email)` + `UserService.rotatePassword(id)`
(ya genera y persiste password temporal con BCrypt). `AuthController` muestra la temporal
en la vista success.

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
| `presentation/auth/AuthRegisterRequest.java` (o reuso de `NewUserRequest`) | Crear/Reusar |
| `domain/login/RecoverPasswordService.java` (+ Impl) | Crear |
| `templates/pages/auth/user/*.html` (login, register, forgot-password, recovered) | Crear |
| `templates/components/navbar.html` | Actualizar (links `/auth/*`, logout por rol) |
| `presentation/auth/AuthControllerTest.java`, `integration/AuthControllerTest.java`, `integration/SecurityConfigTest.java` | Crear/Actualizar |
| `docs/specs/01-LOG.md` | Actualizar nota: AC-07 parcialmente reemplazado (AC-11 de este spec) |