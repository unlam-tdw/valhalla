# Testing Guide

What to test at each layer, how to write tests, and the testing conventions used in this project.

> Los escenarios de test de cada feature están definidos en las specs (`docs/specs/`). Cada spec incluye Test Scenarios agrupados por capa (unit, integration, security, E2E) con referencias a los Acceptance Criteria que cubren. Ver `docs/spec-format.md` para el formato completo.

## Test Pyramid

```
        ┌─────────┐
        │   E2E   │  Playwright, real browser, real database
        │ (slow)  │
        ├─────────┤
        │Integration│  MockMvc + in-memory HSQLDB
        │  (medium) │
        ├─────────┤
        │  Unit   │  Mockito, no Spring context
        │ (fast)  │
        └─────────┘
```

## Test Structure

Tests live under `src/test/java/com/valhalla/` and mirror the main source layout:

```
src/test/java/com/valhalla/
├── config/                         # Test-specific Spring configs
│   └── JpaTestConfig.java
├── domain/                         # Unit tests for services
│   ├── login/LoginServiceTest.java
│   └── user/UserServiceTest.java
├── e2e/                            # Playwright E2E tests (real browser)
│   ├── LoginViewE2E.java
│   ├── UserViewABME2E.java
│   ├── ResetDatabase.java          # DB cleanup between E2E runs
│   └── views/                      # Page objects for Playwright
│       ├── WebPage.java
│       ├── LoginPage.java
│       ├── NewUserPage.java
│       ├── UsersPage.java
│       └── UserFormPage.java
├── infrastructure/                 # Repository tests
│   └── user/UserRepositoryTest.java
├── integration/                    # MockMvc integration tests
│   ├── config/SpringWebTestConfig.java
│   ├── WebIntegrationTest.java     # Composed annotation (see below)
│   ├── JpaIntegrationTest.java     # Composed annotation for JPA tests
│   ├── LoginControllerTest.java
│   └── UserControllerTest.java
└── presentation/                   # Pure Mockito unit tests
    ├── login/LoginControllerTest.java
    └── shared/GlobalExceptionHandlerTest.java
```

**Rule:** put tests in the directory that matches what you're testing. Services go in `domain/`, controllers in `presentation/` (unit) or `integration/` (MockMvc).

## `@WebIntegrationTest`, composed annotation

A custom composed annotation that bundles the boilerplate for MockMvc integration tests:

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(SpringExtension.class)     // JUnit 5 + Spring
@WebAppConfiguration                    // simulate a web context
@ContextConfiguration(classes = {
  SpringWebTestConfig.class,            // MVC + Thymeleaf
  JpaTestConfig.class                   // HSQLDB in-memory
})
public @interface WebIntegrationTest {}
```

**Usage:**

```java
@WebIntegrationTest
public class LoginControllerTest {

  @Autowired private WebApplicationContext wac;
  @Autowired private LoginService loginService;
  private MockMvc mockMvc;

  @BeforeEach
  public void setUp() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
  }
  // ...
}
```

No need to repeat `@ExtendWith`, `@WebAppConfiguration`, or `ContextConfiguration` on every test class, just annotate with `@WebIntegrationTest`.

## Unit Tests (`presentation/`)

Pure Mockito tests. No Spring context. Fast.

### Pattern

```java
public class LoginControllerTest {

  private LoginController controller;
  private LoginService loginServiceMock;

  @BeforeEach
  public void init() {
    loginServiceMock = mock(LoginService.class);
    controller = new LoginController(loginServiceMock);
  }

  @Test
  public void shouldReturnLoginView() {
    ModelAndView modelAndView = controller.showLogin();
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("pages/auth/login"));
  }
}
```

### What to test

| Method | Test cases |
| :--- | :--- |
| `showLogin()` | Returns correct view |
| `register()` | Success → redirect; duplicate email → exception; invalid input → validation error |
| `showHome()` | Returns home view with loginTime |

Note: `validateLogin()` and `logout()` are handled by Spring Security, not the controller. Test them via integration tests with `@WithMockUser` + `.with(csrf())`.

### Key patterns

**Mock the service, not the repository:**
```java
loginServiceMock = mock(LoginService.class);
controller = new LoginController(loginServiceMock);
```

**Integration tests use `@WithMockUser` + `springSecurity()` filter:**
```java
this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac)
    .apply(springSecurity()).build();

this.mockMvc.perform(post("/admin/validate-login")
    .with(csrf())
    .param("email", email).param("password", password))
    .andExpect(redirectedUrl("/admin/home"));
```

**Test that exceptions propagate:**
```java
@Test
public void shouldPropagateExceptionWhenEmailAlreadyExists() {
  doThrow(UserAlreadyExists.class).when(loginServiceMock).register(anyString(), anyString());
  BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(newUserData, "newUserData");
  assertThrows(UserAlreadyExists.class, () -> controller.register(newUserData, bindingResult));
}
```

## Integration Tests (`integration/`)

MockMvc tests with the full Spring context. Uses in-memory HSQLDB.

### Pattern

```java
@WebIntegrationTest
public class LoginControllerTest {

  @Autowired
  private WebApplicationContext wac;

  @Autowired
  private LoginService loginService;

  private MockMvc mockMvc;

  @BeforeEach
  public void setUp() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
  }

  @Test
  public void shouldShowLandingPage() throws Exception {
    this.mockMvc.perform(get("/"))
      .andExpect(status().isOk())
      .andExpect(view().name("pages/landing"));
  }
}
```

### What to test

| Endpoint | Test cases |
| :--- | :--- |
| `GET /` | Returns landing page |
| `GET /admin/login` | Returns login view with `loginData` model attribute |
| `POST /admin/validate-login` | Valid credentials → redirect `/admin/home`; invalid → re-render with error; missing fields → validation error |
| `GET /admin/new-user` | Returns registration view |
| `POST /admin/register` | New email → redirect `/admin/login`; duplicate → error; invalid → validation error |
| `GET /admin/home` | No session → redirect `/admin/login`; with session → home view |
| `POST /admin/logout` | Invalidates session, redirects to `/admin/login` |

### Key patterns

**Use composed annotations:**
```java
@WebIntegrationTest    // MockMvc + in-memory HSQLDB
public class LoginControllerTest { ... }
```

**Create test data in `@BeforeEach`:**
```java
@BeforeEach
public void setUp() {
  this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
  if (userRepository.findByEmail(LOGIN_EMAIL).isEmpty()) {
    loginService.register(LOGIN_EMAIL, LOGIN_PASSWORD);
  }
}
```

## E2E Tests (`e2e/`)

Playwright tests with a real browser. Requires PostgreSQL + app running.

### Pattern

```java
public class LoginViewE2E {

  @Test
  public void shouldNavigateToHomeWhenUserExists() {
    page.navigate(baseUrl + "/admin/login");
    page.locator("#email").fill("test@unlam.edu.ar");
    page.locator("#password").fill("test");
    page.locator("#btn-login").click();
    waitForPath("/admin/home");
  }
}
```

### What to test

| Flow | Test cases |
| :--- | :--- |
| Login | Fill form → submit → navigate to home |
| Login error | Wrong password → error message appears |
| Register | Fill form → submit → navigate to login |
| Logout | Click logout → navigate to login |

### Key patterns

**Wait for navigation (async):**
```java
private void waitForPath(String expectedPath) {
  await().atMost(Duration.ofSeconds(5))
    .until(() -> page.url().contains(expectedPath));
}
```

**UI contract:** E2E tests depend on element IDs and names:
- `#email`, `#password`, input fields
- `#btn-login`, `#btn-register`, buttons
- Error message text: "Invalid email or password"

## Running Tests

### Unit + integration tests

```shell
# All Java tests
mvn test

# Specific test class
mvn test -Dtest="LoginControllerTest"

# Specific test method
mvn test -Dtest="LoginControllerTest#shouldReturnToLoginWhenCredentialsAreWrong"
```

### E2E tests

E2E tests need PostgreSQL and Playwright's Chromium. `mvn verify` auto-starts Jetty, runs E2E via failsafe, then stops Jetty.

```shell
# One-time: install Chromium
npx playwright install chromium

# Run everything (unit + integration + E2E)
docker compose up -d postgres
mvn verify
```

Or run E2E manually:

```shell
# 1. Start PostgreSQL + Jetty
docker compose up -d postgres
mvn jetty:run &

# 2. Wait for server, then run E2E
mvn failsafe:integration-test failsafe:verify -DskipTests
```

### Skipping quality gates

During development you may want to skip static analysis to iterate faster:

```shell
# Skip all quality gates
mvn test \
  -Djacoco.skip=true \
  -Dcheckstyle.skip=true \
  -Dpmd.skip=true \
  -Dcpd.skip=true

# Skip only Checkstyle
mvn test -Dcheckstyle.skip=true

# Skip only PMD + CPD
mvn test -Dpmd.skip=true -Dcpd.skip=true

# Skip only JaCoCo coverage check (still generates report)
mvn test -Djacoco.skip=true
```

CI enforces these gates on `main`, always run `mvn clean verify` before pushing.

## Coverage

Coverage is measured by JaCoCo. See [code-quality.md](code-quality.md) for details.

**Requirements:**
- `domain/` and `presentation/` must reach **100%** line coverage
- `infrastructure/` must reach **80%**
- Global floor: **80%**
