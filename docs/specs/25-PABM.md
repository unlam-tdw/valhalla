# [PABM] ABM de Planes en Admin

> Trello: <pendiente>
> **Note:** This is an SDD proposal. Implementation may change based on team decisions.

## Objetivo

Un admin puede gestionar todos los planes de la plataforma desde `/admin/plans`: listarlos,
crear nuevos (a nombre de la app como usuario promocional), editar cualquier característica
(datos, participantes, roles, lugares, settings), activar/desactivar y eliminar.

La desactivación oculta el plan de listados y feeds (`/plans` del usuario, `/for-you`,
`/explore`) pero conserva el acceso para los participantes existentes: el detalle sigue
funcionando para quienes ya están dentro.

Los planes creados desde admin pertenecen a un usuario sistema **Valhalla App**
(`app@planit.com`), no al admin logueado. Son planes que la plataforma promociona.

## Pre-requisitos

- [PLN] completed (Plan entity, PlanService, PlanController funcionando)
- [PLC] completed (Place entity y ABM admin de places como patrón de referencia)
- `SecurityConfig` protege `/admin/**` con `hasRole("ADMIN")` — no cambia.
- `PlaceAdminController` y `UserController` existen como patrón de ABM admin.
- No existe usuario sistema "app" — se crea en esta card.

## Criterios de Aceptacion

| # | Criterio |
|---|----------|
| AC-01 | `GET /admin/plans` muestra todos los planes con nombre, dueño (nombre completo), visibilidad, estado (activo/inactivo), cantidad de participantes y acciones |
| AC-02 | `GET /admin/plans/new` muestra el formulario de alta con nota indicando que el plan será de "Valhalla App" |
| AC-03 | `POST /admin/plans` crea un plan con `administrator` = usuario sistema `app@planit.com` (nombre: PlanIt, rol: ADMIN); si el usuario no existe se crea automáticamente |
| AC-04 | `GET /admin/plans/{id}/edit` muestra el formulario con los datos actuales del plan |
| AC-05 | `POST /admin/plans/{id}` actualiza nombre, descripción, fecha, horarios, modo de transporte y visibilidad de cualquier plan |
| AC-06 | `POST /admin/plans/{id}/toggle-active` alterna el campo `active` del plan y redirige al listado con mensaje |
| AC-07 | `POST /admin/plans/{id}/delete` elimina el plan y sus dependencias (participantes, invitaciones, lugares) |
| AC-08 | `GET /admin/plans/{id}` muestra detalle completo: datos básicos, participantes con roles, lugares del plan y settings |
| AC-09 | Desde el detalle el admin puede cambiar el rol de un participante (guest ↔ editor) |
| AC-10 | Desde el detalle el admin puede eliminar un participante del plan |
| AC-11 | Desde el detalle el admin puede editar los settings del plan (handleRequests, inviteInternal, inviteByEmail, autoSchedule) |
| AC-12 | Un plan con `active=false` no aparece en `/for-you`, `/explore` ni en el feed de planes públicos |
| AC-13 | Un plan con `active=false` sigue siendo accesible por `/plans/{id}` para sus participantes |
| AC-14 | Un plan con `active=false` NO es accesible para usuarios que no son participantes ni dueño |
| AC-15 | `/admin/plans` y todas sus sub-rutas requieren `ROLE_ADMIN`; un USER común recibe 403 |
| AC-16 | El campo `active` tiene default `true` — los planes existentes no se ven afectados por la migración |
| AC-17 | El admin no puede crear un plan sin nombre (validación `@NotBlank`) |
| AC-18 | El usuario sistema `app@planit.com` tiene rol `ADMIN` y su contraseña no es un valor loginable (no tiene cuenta de acceso) |

## Escenarios de Test

### Tests Unitarios (`presentation/plan/PlanAdminControllerTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| U-01 | `listPlans()` retorna vista con todos los planes | AC-01 |
| U-02 | `showNewPlanForm()` retorna vista con form vacío y flag `isEdit=false` | AC-02 |
| U-03 | `createPlan()` con datos válidos redirige al listado | AC-03 |
| U-04 | `createPlan()` con nombre vacío retorna vista con error de validación | AC-17 |
| U-05 | `showEditPlanForm()` con id válido retorna vista con datos del plan | AC-04 |
| U-06 | `updatePlan()` con datos válidos redirige al listado | AC-05 |
| U-07 | `deletePlan()` elimina y redirige al listado | AC-07 |
| U-08 | `toggleActive()` con `active=true` activa el plan y redirige | AC-06 |
| U-09 | `toggleActive()` con `active=false` desactiva el plan y redirige | AC-06 |
| U-10 | `showPlanDetail()` con id válido retorna vista con participantes, lugares y settings | AC-08 |

### Tests Unitarios (`infrastructure/plan/PlanServiceImplAdminTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| U-11 | `getAllPlans()` retorna todos los planes del repositorio | AC-01 |
| U-12 | `getAnyPlan()` con id existente retorna el plan sin verificar ownership | AC-04 |
| U-13 | `getAnyPlan()` con id inexistente lanza `PlanNotFoundException` | AC-04 |
| U-14 | `createAdminPlan()` asigna `administrator` = usuario sistema `app@planit.com` | AC-03 |
| U-15 | `createAdminPlan()` genera shortCode único | AC-03 |
| U-16 | `createAdminPlan()` crea el usuario sistema con rol ADMIN si no existe | AC-03, AC-18 |
| U-17 | `updateAnyPlan()` actualiza los campos del plan | AC-05 |
| U-18 | `deleteAnyPlan()` elimina el plan del repositorio | AC-07 |
| U-19 | `togglePlanActive()` cambia el campo `active` | AC-06 |
| U-20 | `togglePlanActive()` con plan inexistente lanza `PlanNotFoundException` | AC-06 |

### Tests de Integracion (`integration/PlanAdminControllerIntegrationTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| I-01 | `GET /admin/plans` con sesión admin retorna 200 y listado | AC-01, AC-15 |
| I-02 | `GET /admin/plans/new` con sesión admin retorna 200 y formulario | AC-02 |
| I-03 | `POST /admin/plans` con datos válidos redirige al listado y el plan tiene administrator = app user | AC-03 |
| I-04 | `POST /admin/plans` con nombre vacío retorna 200 con errores | AC-17 |
| I-05 | `GET /admin/plans/{id}/edit` con id válido retorna 200 con datos precargados | AC-04 |
| I-06 | `POST /admin/plans/{id}` actualiza y redirige | AC-05 |
| I-07 | `POST /admin/plans/{id}/toggle-active` alterna estado y redirige | AC-06 |
| I-08 | `POST /admin/plans/{id}/delete` elimina y redirige | AC-07 |
| I-09 | `GET /admin/plans/{id}` con id válido retorna 200 con detalle completo | AC-08 |
| I-10 | `GET /for-you` no incluye planes con `active=false` | AC-12 |
| I-11 | `GET /explore` no incluye planes con `active=false` | AC-12 |

### Tests de Seguridad (`integration/PlanAdminSecurityTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| S-01 | `GET /admin/plans` sin sesión redirige a login | AC-15 |
| S-02 | `GET /admin/plans` con sesión USER retorna 403 | AC-15 |
| S-03 | `POST /admin/plans` con sesión USER retorna 403 | AC-15 |
| S-04 | `POST /admin/plans/{id}/delete` con sesión USER retorna 403 | AC-15 |
| S-05 | `POST /admin/plans/{id}/toggle-active` con sesión USER retorna 403 | AC-15 |
| S-06 | `GET /admin/plans` con sesión ADMIN retorna 200 | AC-15 |
| S-07 | `GET /plans/{id}` de plan inactivo sin ser participante retorna 404 o redirect | AC-14 |
| S-08 | `GET /plans/{id}` de plan inactivo siendo participante retorna 200 | AC-13 |

### E2E (minimos)

| # | Test | Flujo | AC que cubre |
|---|------|-------|-------------|
| E-01 | `AdminPlansE2E` | Admin crea plan → lo ve en listado → lo edita → lo desactiva → lo reactiva → lo elimina | AC-01, AC-03, AC-05, AC-06, AC-07 |

## Referencia de Implementacion

> Los pasos a continuación son guía de implementación, no reemplazan los acceptance criteria de arriba.

### 1. Agregar campo `active` a Plan

File: `src/main/java/com/valhalla/domain/plan/Plan.java`

Agregar después de `isPublic`:

```java
private Boolean active = true;
```

Getter/setter:

```java
public Boolean getActive() {
  return active != null ? active : true;
}

public void setActive(Boolean active) {
  this.active = active;
}
```

Actualizar `updateFrom` para incluir `this.active = changes.getActive();`.

### 2. Agregar métodos admin a PlanService

File: `src/main/java/com/valhalla/domain/plan/PlanService.java`

```java
// Admin operations
List<Plan> getAllPlans();
Plan getAnyPlan(UUID id);
Plan createAdminPlan(Plan plan);
Plan updateAnyPlan(UUID id, Plan changes);
void deleteAnyPlan(UUID id);
void togglePlanActive(UUID id, boolean active);
```

### 3. Implementar en PlanServiceImpl

File: `src/main/java/com/valhalla/infrastructure/plan/PlanServiceImpl.java`

```java
private static final String APP_USER_EMAIL = "app@planit.com";

@Override
@Transactional(readOnly = true)
public List<Plan> getAllPlans() {
  return planRepository.findAll();
}

@Override
@Transactional(readOnly = true)
public Plan getAnyPlan(UUID id) {
  return planRepository.findById(id).orElseThrow(PlanNotFoundException::new);
}

@Override
public Plan createAdminPlan(Plan plan) {
  User appUser = getOrCreateAppUser();
  plan.setAdministrator(appUser);
  plan.setShortCode(generateUniqueShortCode());
  plan.setActive(true);
  return planRepository.save(plan);
}

@Override
public Plan updateAnyPlan(UUID id, Plan changes) {
  Plan plan = planRepository.findById(id).orElseThrow(PlanNotFoundException::new);
  plan.updateFrom(changes);
  return planRepository.save(plan);
}

@Override
public void deleteAnyPlan(UUID id) {
  planRepository.deleteById(id);
}

@Override
public void togglePlanActive(UUID id, boolean active) {
  Plan plan = planRepository.findById(id).orElseThrow(PlanNotFoundException::new);
  plan.setActive(active);
  planRepository.save(plan);
}

private User getOrCreateAppUser() {
  return userRepository.findByEmail(APP_USER_EMAIL).orElseGet(() -> {
    User appUser = new User();
    appUser.setEmail(APP_USER_EMAIL);
    appUser.setPassword("N/A");
    appUser.setFirstName("PlanIt");
    appUser.setLastName("App");
    appUser.setRole("ADMIN");
    appUser.setActive(true);
    return userRepository.save(appUser);
  });
}
```

### 4. Filtrar planes inactivos en listings públicos

En `getPublicPlans()` de `PlanServiceImpl`, agregar filtro `plan.getActive()`.

En `PlanDiscovery` o `ForYouController`, asegurar que solo se muestren planes activos.

### 5. Crear PlanAdminController

File: `src/main/java/com/valhalla/presentation/plan/PlanAdminController.java`

Seguir el patrón exacto de `PlaceAdminController`. Rutas:

- `GET /admin/plans` — listado
- `GET /admin/plans/new` — form alta
- `POST /admin/plans` — crear
- `GET /admin/plans/{id}/edit` — form edición
- `POST /admin/plans/{id}` — actualizar
- `POST /admin/plans/{id}/toggle-active` — alternar active
- `POST /admin/plans/{id}/delete` — eliminar
- `GET /admin/plans/{id}` — detalle completo

### 6. Crear EditPlanRequest

File: `src/main/java/com/valhalla/presentation/plan/EditPlanRequest.java`

Form object con: `name` (@NotBlank), `description`, `eventDate`, `eventTime`, `dayEndTime`,
`transportMode`, `isPublic`, `active`.

### 7. Crear templates

- `pages/admin/plans.html` — listado (patrón `places.html`)
- `pages/admin/plan-form.html` — alta/edición (patrón `place-form.html`)
- `pages/admin/plan-detail.html` — detalle con tabs: datos, participantes, lugares, settings

### 8. Tests

- `PlanAdminControllerTest` — unit tests del controller
- `PlanServiceImplAdminTest` — unit tests del service admin
- `PlanAdminControllerIntegrationTest` — integration tests
- `PlanAdminSecurityTest` — security tests (403 para USER)
- `AdminPlansE2E` — happy path completo

## Archivos a crear/modificar

| Archivo | Accion |
|---------|--------|
| `domain/plan/Plan.java` | Actualizar (campo `active`) |
| `domain/plan/PlanService.java` | Actualizar (métodos admin) |
| `infrastructure/plan/PlanServiceImpl.java` | Actualizar (implementación admin) |
| `presentation/plan/PlanAdminController.java` | Crear |
| `presentation/plan/EditPlanRequest.java` | Crear |
| `templates/pages/admin/plans.html` | Crear |
| `templates/pages/admin/plan-form.html` | Crear |
| `templates/pages/admin/plan-detail.html` | Crear |
| `test/.../PlanAdminControllerTest.java` | Crear |
| `test/.../PlanServiceImplAdminTest.java` | Crear |
| `test/.../PlanAdminControllerIntegrationTest.java` | Crear |
| `test/.../PlanAdminSecurityTest.java` | Crear |
| `test/.../AdminPlansE2E.java` | Crear |
