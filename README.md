# Valhalla, Taller Web I

Spring MVC + Thymeleaf + Tailwind project for Taller Web I (UNLaM).

## Quick Start

```shell
git clone <repo-url>
cd valhalla
cp .env.example .env
docker compose up
```

The app runs at [http://localhost:8080](http://localhost:8080).

**Default credentials:** `test@unlam.edu.ar` / `password`

This starts PostgreSQL + the app in Docker with hot-reload. Source code is mounted as a volume, changes reflect immediately.

## Project Structure

```
src/main/java/com/valhalla/
├── config/                 # Spring configuration (JPA, MVC, security, validation)
├── domain/                 # Business logic (services, models, exceptions)
│   ├── exception/          # Custom domain exceptions
│   ├── login/              # Login service interface + implementation
│   ├── user/               # User entity, service, repository interface
│   ├── place/              # Place entity, service, repository interface
│   ├── plan/               # Plan entity, service, repository interface
│   └── planplace/          # PlanPlace entity, service, repository interface
├── infrastructure/         # Persistence (Spring Data JPA repositories) + seeders
│   ├── user/               # UserRepositoryImpl, JpaUserRepository
│   ├── place/              # PlaceRepositoryImpl, JpaPlaceRepository
│   ├── plan/               # PlanRepositoryImpl, JpaPlanRepository
│   ├── planplace/          # PlanPlaceRepositoryImpl, JpaPlanPlaceRepository
│   ├── UserSeeder.java     # Seeds test admin on startup
│   └── PlaceDataSeeder.java # Seeds 10 Buenos Aires places
├── presentation/           # MVC controllers, DTOs, session interceptor
│   ├── login/              # Login controller + DTOs
│   ├── shared/             # Cross-cutting: GlobalExceptionHandler, SessionInterceptor, UserSession
│   ├── user/               # User controller + DTOs
│   ├── place/              # PlaceController, PlaceRestController
│   ├── plan/               # PlanController, PlanPlaceRestController
│   └── share/              # ShareController (public plan view)
└── MyServletInitializer.java  # Bootstrap for external servlet containers

src/main/webapp/
├── WEB-INF/templates/
│   ├── layouts/            # base page chrome (head + layout decorator)
│   ├── components/         # reusable fragments (header, alerts)
│   ├── pages/auth/         # login, register
│   ├── pages/places/       # places list (map + sidebar), place detail
│   ├── pages/plans/        # plans list, create, detail (itinerary + map)
│   └── pages/share/        # public shared plan view
└── resources/core/js/
    ├── tailwind-browser.js # Tailwind CSS compiled in the browser
    └── vue.global.prod.js  # Vue.js for client-side interactivity
```

## Documentation

| Document | Description |
| :--- | :--- |
| [Architecture](docs/architecture.md) | Layered architecture, domain model, dependency rules |
| [Adding a Feature](docs/adding-a-feature.md) | Step-by-step: domain, repository, service, controller, template |
| [Frontend Stack](docs/frontend-stack.md) | Why each tool exists and team conventions |
| [Vue + Tailwind Guide](docs/guide-vue.md) | How to create views with Vue |
| [CSS + JS Guide](docs/guide-css-js.md) | How to create views with plain CSS + JS |
| [Error Handling](docs/error-handling.md) | Validation, custom exceptions, GlobalExceptionHandler |
| [Testing](docs/testing.md) | Unit tests, integration tests, E2E tests |
| [Environment Setup](docs/setup.md) | Install Java, Maven, Docker, IDE config |
| [Commands Reference](docs/commands.md) | Maven, Docker, and testing commands |
| [Code Quality](docs/code-quality.md) | Checkstyle, PMD, CPD, JaCoCo, Prettier |
| [Spec Format](docs/spec-format.md) | Spec structure: criterios, escenarios de test, referencia |
| [Sprint Planner](docs/sprint-planner.md) | Sprint plan, dependencies, team assignments |

## Specs

Las specs definen cada feature del proyecto. Cada una tiene Criterios de Aceptacion, Escenarios de Test y Referencia de Implementacion.

| Spec | Feature | Trello |
| :--- | :--- | :--- |
| [01-LOG](docs/specs/01-LOG.md) | Login, Register, Security | [LOG](https://trello.com/c/AuMX9RgJ) |
| [02-PLC](docs/specs/02-PLC.md) | Explorar Lugares + Ficha | [PLC](https://trello.com/c/mPp6mZml) |
| [03-PLN](docs/specs/03-PLN.md) | Crear Plan | [PLN](https://trello.com/c/vij4lVFO) |
| [04-APL-BE](docs/specs/04-APL-BE.md) | Agregar Lugares al Plan (Backend) | [APL-BE](https://trello.com/c/zHj13Gy6) |
| [05-APL-FE](docs/specs/05-APL-FE.md) | Agregar Lugares al Plan (Frontend) | [APL-FE](https://trello.com/c/K7XqvGWZ) |
| [06-CMP](docs/specs/06-CMP.md) | Compartir Plan | [CMP](https://trello.com/c/vZuil82c) |
| [07-VPC](docs/specs/07-VPC.md) | Vista Publica de Plan Compartido | [VPC](https://trello.com/c/hr1n4EwA) |

## Authentication

Session-based. On successful login the controller maps the domain `User` to a `UserSession` DTO (email + role) and stores it in the HTTP session. `SessionInterceptor` guards `/home` (redirects to `/login` if no session), and `POST /logout` invalidates the session. New registrations get `role = USER` and `active = true`.

## Technologies

- Docker
- Java 25 (LTS)
- Spring 6.2.19 + Spring Data JPA 3.5.13
- Hibernate Validator 8.0.5.Final
- Thymeleaf 3.1.5.RELEASE
- Embedded Jetty Server EE10 12.0.37
- Tailwind CSS 4.3.3 (compiled in the browser)
- Vue.js 3.5.13 (client-side interactivity)
- Leaflet 1.9.4 + OpenStreetMap (interactive maps)
- Spring Test 6.2.19 / Hamcrest 2.2 / JUnit 6.1.2
- Mockito 5.23.0 / Playwright 1.61.0
- PMD 7.26.0 / Checkstyle 13.8.0 / Prettier 0.22 / JaCoCo 0.8.15
