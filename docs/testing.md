# Testing Guide

What to test at each layer, how to write tests, and the testing conventions used in this project.

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

## `@WebIntegrationTest` — Composed Annotation

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

No need to repeat `@ExtendWith`, `@WebAppConfiguration`, or `@ContextConfiguration` on every test class — just annotate with `@WebIntegrationTest`.

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
| `showLogin()` | Returns correct view, includes `LoginRequest` in model |
| `validateLogin()` | Success → redirect; failure → re-render with error; invalid input → validation error |
| `register()` | Success → redirect; duplicate email → exception; invalid input → validation error |
| `showHome()` | Has session → home view; no session → redirect to login |
| `logout()` | Invalidates session; handles null session |

### Key patterns

**Mock the service, not the repository:**
```java
loginServiceMock = mock(LoginService.class);
controller = new LoginController(loginServiceMock);
```

**Use `ArgumentCaptor` to verify session storage:**
```java
ArgumentCaptor<UserSession> captor = ArgumentCaptor.forClass(UserSession.class);
verify(sessionMock, times(1))
  .setAttribute(eq(SessionInterceptor.USER_SESSION), captor.capture());
assertThat(captor.getValue().getEmail(), equalToIgnoringCase("dami@unlam.com"));
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
  public void shouldRedirectToLoginPageFromRoot() throws Exception {
    this.mockMvc.perform(get("/"))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/login"));
  }
}
```

### What to test

| Endpoint | Test cases |
| :--- | :--- |
| `GET /` | Redirects to `/login` |
| `GET /login` | Returns login view with `loginData` model attribute |
| `POST /validate-login` | Valid credentials → redirect `/home`; invalid → re-render with error; missing fields → validation error |
| `GET /new-user` | Returns registration view |
| `POST /register` | New email → redirect `/login`; duplicate → error; invalid → validation error |
| `GET /home` | No session → redirect `/login`; with session → home view |
| `POST /logout` | Invalidates session, redirects to `/login` |

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
    page.navigate(baseUrl + "/login");
    page.locator("#email").fill("test@unlam.edu.ar");
    page.locator("#password").fill("test");
    page.locator("#btn-login").click();
    waitForPath("/home");
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
- `#email`, `#password` — input fields
- `#btn-login`, `#btn-register` — buttons
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

E2E tests need a real PostgreSQL and Playwright's Chromium.

```shell
# 1. Start PostgreSQL
docker compose up -d postgres

# 2. Install Chromium (first time only)
mvn -q exec:java -e \
  -Dexec.mainClass=com.microsoft.playwright.CLI \
  -Dexec.args="install --with-deps chromium"

# 3. Start Jetty (in a separate terminal, with DB env vars from your .env)
mvn jetty:run

# 4. Run E2E tests (in another terminal)
mvn test -Dtest=LoginViewE2E \
  -Djacoco.skip=true \
  -Dcheckstyle.skip=true \
  -Dpmd.skip=true \
  -Dcpd.skip=true
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

CI enforces these gates on `main` — always run `mvn clean verify` before pushing.

## Coverage

Coverage is measured by JaCoCo. See [code-quality.md](code-quality.md) for details.

**Requirements:**
- `domain/` and `presentation/` must reach **100%** line coverage
- `infrastructure/` must reach **80%**
- Global floor: **80%**
