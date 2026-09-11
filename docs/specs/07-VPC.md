# [VPC] Vista Publica de Plan Compartido

> Trello: https://trello.com/c/hr1n4EwA/8-vpc-ver-plan-compartido-sin-cuenta
> **Note:** This is an SDD proposal. Implementation may change based on team decisions.

## Objetivo

Cualquier persona puede ver un plan compartido via URL, sin necesidad de login. Vista completa con mapa, itinerario numerado y ruta.

## Pre-requisitos

- [CMP] completed (shortCode and share working)

## Criterios de Aceptacion

| # | Criterio |
|---|----------|
| AC-01 | /share/{shortCode} es accesible sin login |
| AC-02 | Si el plan es PUBLIC, muestra nombre, descripcion, fecha, itinerario y mapa |
| AC-03 | Los markers del mapa estan numerados en orden de visita |
| AC-04 | El mapa dibuja una polyline de ruta entre los lugares |
| AC-05 | El itinerario muestra numero de orden, nombre, categoria, fecha y hora |
| AC-06 | Si el plan es PRIVATE, muestra error "This plan is private" |
| AC-07 | Si el shortCode es invalido, muestra error "Plan not found" |
| AC-08 | La ruta /share/** esta configurada como publica en SecurityConfig |

## Escenarios de Test

### Tests de Integracion (`integration/ShareControllerTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| I-01 | `GET /share/{shortCode}` con plan PUBLIC retorna 200 con plan | AC-01, AC-02 |
| I-02 | `GET /share/{shortCode}` con plan PRIVATE retorna 200 con error | AC-06 |
| I-03 | `GET /share/{shortCode}` con shortCode invalido retorna 200 con error | AC-07 |
| I-04 | `GET /share/{shortCode}` sin sesion no redirige a login | AC-01 |

### Tests de Seguridad

| # | Test | AC que cubre |
|---|------|-------------|
| S-01 | `/share/test` es accesible sin autenticacion | AC-08 |
| S-02 | `/share/test` no retorna 302 a login | AC-08 |

### E2E (minimos)

| # | Test | Flujo | AC que cubre |
|---|------|-------|-------------|
| E-01 | `PublicShareE2E` | Abrir link en ventana incognito, verificar plan y mapa | AC-01, AC-02, AC-03, AC-04 |

## Referencia de Implementacion

> Los pasos a continuación son guía de implementación, no reemplazan los acceptance criteria de arriba.

**Nota:** Esta spec esta implementada dentro de [CMP] (06-CMP.md). El ShareController y share/view.html ya fueron creados ahi.

### 1. Verify ShareController exists

File: `src/main/java/com/valhalla/presentation/share/ShareController.java`

Already created in [CMP]. Verify it handles:
- `GET /share/{shortCode}` with PUBLIC plan -> show plan
- `GET /share/{shortCode}` with PRIVATE plan -> show error
- `GET /share/{shortCode}` with invalid shortCode -> show error

### 2. Verify share view template exists

File: `src/main/webapp/WEB-INF/templates/pages/share/view.html`

Already created in [CMP]. Verify it shows:
- Plan name, description, date
- Itinerary with numbered places
- Map with numbered markers and polyline route
- Error state for private/not found plans

### 3. Verify SecurityConfig (public route)

The `/share/**` route is already configured as public in SecurityConfig (created in [LOG]):

```java
.requestMatchers("/share/**").permitAll()
```

No additional configuration needed.

## Archivos a crear/modificar

| File | Action |
|------|--------|
| `presentation/share/ShareController.java` | Already created in [CMP] |
| `templates/pages/share/view.html` | Already created in [CMP] |
| `config/SecurityConfig.java` | Verify `/share/**` is permitAll |
