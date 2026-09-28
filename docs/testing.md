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

E2E tests need PostgreSQL and Playwright's Chromium.

```shell
./scripts/e2e.sh                         # everything
./scripts/e2e.sh --headed --slowmo 300   # watch the browser, 300ms between actions
```

The script is idempotent and does everything: starts PostgreSQL, creates the
`valhalla_e2e` database if missing, installs Chromium, exports `DB_*`, and runs
`mvn verify` — which starts Jetty, runs failsafe, and stops Jetty. Run it from
Git Bash on Windows.

<details>
<summary>What the script does, if you need to run the steps by hand</summary>

The E2E suite wipes the database before every test, so it refuses to run against
anything whose name does not contain `e2e`.

```shell
docker compose up -d postgres
docker compose exec postgres createdb -U user valhalla_e2e
npx -y playwright@1.61.0 install chromium   # keep in sync with pom.xml's playwright.version
```

Jetty and the tests must point at the same database: Jetty needs the schema, and
`ResetDatabase` needs the same data the app serves.

```shell
export DB_NAME=valhalla_e2e   # PowerShell: $env:DB_NAME="valhalla_e2e"
export DB_HOST=localhost
export DB_USER=user
export DB_PASSWORD=user
```

To keep the server running between runs, start Jetty yourself and invoke failsafe
directly:

```shell
mvn jetty:run &                              # 1. leave this running
mvn failsafe:integration-test failsafe:verify "-De2e.headed=true"   # 2. in another terminal
mvn jetty:stop                               # 3. when done
```

Failsafe derives the Playwright base URL from `jetty.port`, so moving the server
moves the tests with it. Quote the `-D` arguments in PowerShell; unquoted it
splits them and Maven reports `Unknown lifecycle phase`.

`-De2e.slowMo=N` inserts N milliseconds between Playwright actions, which is how
you watch a headless run without a visible window.

</details>

A failing test leaves a full-page screenshot and a Playwright trace in
`target/e2e-artifacts/<Class>-<method>/`. Open the trace with
`npx playwright show-trace target/e2e-artifacts/<Class>-<method>/trace.zip`.

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
