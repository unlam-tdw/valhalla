# [LOG] Login & Seguridad

> Trello: https://trello.com/c/AuMX9RgJ/1-log-registro-e-inicio-de-sesion
> **Note:** This is an SDD proposal. Implementation may change based on team decisions.

## Objective

User can register, login, and logout. Session managed by Spring Security with CSRF protection.

## Prerequisites

- Project running with `mvn jetty:run`
- HSQLDB configured (dev)

## Steps

### 1. Add Spring Security dependencies

File: `pom.xml`

Add to `<dependencies>`:

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-web</artifactId>
    <version>6.5.11</version>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-config</artifactId>
    <version>6.5.11</version>
</dependency>
<dependency>
    <groupId>org.thymeleaf.extras</groupId>
    <artifactId>thymeleaf-extras-springsecurity6</artifactId>
    <version>3.1.5.RELEASE</version>
</dependency>
```

`spring-security-crypto` already exists in pom.xml — do NOT add it again.

### 2. Create SecurityConfig

File: `src/main/java/com/valhalla/config/SecurityConfig.java`

Replace the existing file entirely:

```java
package com.valhalla.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**")
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/register", "/new-user").permitAll()
                .requestMatchers("/share/**").permitAll()
                .requestMatchers("/api/**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/validate-login")
                .defaultSuccessUrl("/home")
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            )
            .sessionManagement(session -> session
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### 3. Update BaseWebConfig (remove SessionInterceptor)

File: `src/main/java/com/valhalla/config/BaseWebConfig.java`

Two changes:

**a)** Remove the import:
```java
// DELETE this line:
import com.valhalla.presentation.shared.SessionInterceptor;
```

**b)** Remove the interceptor registration from `addInterceptors()`:
```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    // DELETE these lines:
    // registry
    //   .addInterceptor(new SessionInterceptor())
    //   .addPathPatterns("/home", "/users", "/users/**");

    // KEEP the DevReloadInterceptor block (if present)
    if (isLiveReload()) {
        registry
            .addInterceptor(new DevReloadInterceptor(devReloadController()))
            .addPathPatterns("/**");
    }
}
```

### 4. Update BaseWebConfig (add Thymeleaf Security dialect)

File: `src/main/java/com/valhalla/config/BaseWebConfig.java`

Update `templateEngine()` to register the Spring Security dialect:

```java
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;

// In templateEngine() method, add this line:
@Bean
public SpringTemplateEngine templateEngine() {
    SpringTemplateEngine templateEngine = new SpringTemplateEngine();
    templateEngine.setTemplateResolver(templateResolver());
    templateEngine.setEnableSpringELCompiler(true);
    templateEngine.addDialect("sec", new SpringSecurityDialect());  // ADD THIS
    return templateEngine;
}
```

### 5. Create UserDetailsService

File: `src/main/java/com/valhalla/infrastructure/security/CustomUserDetailsService.java`

```java
package com.valhalla.infrastructure.security;

import com.valhalla.domain.user.User;
import com.valhalla.domain.user.UserRepository;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return new org.springframework.security.core.userdetails.User(
            user.getEmail(),
            user.getPassword(),
            Boolean.TRUE.equals(user.getActive()),
            true, true, true,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }
}
```

### 6. LoginService compatibility note

The existing `LoginService` interface and `LoginServiceImpl` are **already compatible** with Spring Security:

- `LoginService.findUser()` — still used by LoginController for manual validation (optional, can keep or remove)
- `LoginService.register()` — still used for registration, already uses `PasswordEncoder`
- `LoginServiceImpl` already injects `PasswordEncoder` and uses `BCrypt`

**Do NOT rewrite LoginServiceImpl.** Just add `CustomUserDetailsService` (step 5) which Spring Security uses internally. The existing `LoginService` continues to work for registration.

### 7. Add validation to NewUserRequest

File: `src/main/java/com/valhalla/presentation/shared/NewUserRequest.java`

```java
package com.valhalla.presentation.shared;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class NewUserRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    // Getters and setters
}
```

### 8. Update LoginController

File: `src/main/java/com/valhalla/presentation/login/LoginController.java`

Replace the entire file:

```java
package com.valhalla.presentation.login;

import com.valhalla.domain.login.LoginService;
import com.valhalla.presentation.shared.NewUserRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class LoginController {

    private static final String VIEW_LOGIN = "pages/auth/login";
    private static final String VIEW_NEW_USER = "pages/auth/new-user";
    private static final String REDIRECT_LOGIN = "redirect:/login";
    private static final String ATTR_NEW_USER_DATA = "newUserData";

    private final LoginService loginService;

    @Autowired
    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @GetMapping("/login")
    public ModelAndView showLogin(@RequestParam(required = false) String error) {
        Map<String, Object> model = new ModelMap();
        if (error != null) {
            model.put("error", "Invalid email or password");
        }
        return new ModelAndView(VIEW_LOGIN, model);
    }

    @GetMapping("/new-user")
    public ModelAndView showNewUser() {
        Map<String, Object> model = new ModelMap();
        model.put(ATTR_NEW_USER_DATA, new NewUserRequest());
        return new ModelAndView(VIEW_NEW_USER, model);
    }

    @PostMapping("/register")
    public ModelAndView register(
        @Valid @ModelAttribute(ATTR_NEW_USER_DATA) NewUserRequest newUserData,
        BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return renderNewUserWithError(newUserData);
        }
        try {
            loginService.register(newUserData.getEmail(), newUserData.getPassword());
            return new ModelAndView(REDIRECT_LOGIN);
        } catch (Exception e) {
            return renderNewUserWithError(newUserData, "Email is already registered");
        }
    }

    @GetMapping("/home")
    public ModelAndView showHome() {
        return new ModelAndView("pages/home");
    }

    @GetMapping("/")
    public ModelAndView index() {
        return new ModelAndView(REDIRECT_LOGIN);
    }

    private ModelAndView renderNewUserWithError(NewUserRequest newUserData) {
        return renderNewUserWithError(newUserData, "Invalid registration data");
    }

    private ModelAndView renderNewUserWithError(NewUserRequest newUserData, String message) {
        Map<String, Object> model = new ModelMap();
        model.put(ATTR_NEW_USER_DATA, newUserData);
        model.put("error", message);
        return new ModelAndView(VIEW_NEW_USER, model);
    }
}
```

### 9. Update templates

#### 9.1 Header fragment

File: `src/main/webapp/WEB-INF/templates/fragments/header.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<body>
    <div th:fragment="header" class="bg-gray-800 text-white p-4">
        <nav class="container mx-auto flex justify-between items-center">
            <a th:href="@{/home}" class="text-xl font-bold">PlanIt</a>
            <div sec:authorize="isAuthenticated()" class="flex items-center gap-4">
                <span sec:authentication="name">user</span>
                <form th:action="@{/logout}" method="post" class="inline">
                    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">
                    <button type="submit" class="bg-red-500 hover:bg-red-600 px-3 py-1 rounded">
                        Logout
                    </button>
                </form>
            </div>
            <div sec:authorize="!isAuthenticated()">
                <a th:href="@{/login}" class="bg-blue-500 hover:bg-blue-600 px-3 py-1 rounded">
                    Login
                </a>
            </div>
        </nav>
    </div>
</body>
</html>
```

#### 9.2 Login template

File: `src/main/webapp/WEB-INF/templates/pages/auth/login.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Login</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-gray-100 min-h-screen flex items-center justify-center">
    <div class="bg-white p-8 rounded-lg shadow-md w-full max-w-md">
        <h1 class="text-2xl font-bold mb-6 text-center">Login</h1>

        <div th:if="${error}" class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4"
             th:text="${error}"></div>

        <form th:action="@{/validate-login}" method="post">
            <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">
            <div class="mb-4">
                <label for="username" class="block text-gray-700 font-medium mb-2">Email</label>
                <input type="email" id="username" name="username"
                       class="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:border-blue-500"
                       required>
            </div>
            <div class="mb-6">
                <label for="password" class="block text-gray-700 font-medium mb-2">Password</label>
                <input type="password" id="password" name="password"
                       class="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:border-blue-500"
                       required>
            </div>
            <button type="submit"
                    class="w-full bg-blue-500 hover:bg-blue-600 text-white font-medium py-2 px-4 rounded">
                Login
            </button>
        </form>

        <p class="mt-4 text-center text-gray-600">
            Don't have an account? <a th:href="@{/new-user}" class="text-blue-500 hover:underline">Register</a>
        </p>
    </div>
</body>
</html>
```

#### 9.3 Registration template

File: `src/main/webapp/WEB-INF/templates/pages/auth/new-user.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Register</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-gray-100 min-h-screen flex items-center justify-center">
    <div class="bg-white p-8 rounded-lg shadow-md w-full max-w-md">
        <h1 class="text-2xl font-bold mb-6 text-center">Register</h1>

        <div th:if="${error}" class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4"
             th:text="${error}"></div>

        <form th:action="@{/register}" th:object="${newUserData}" method="post">
            <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">
            <div class="mb-4">
                <label for="email" class="block text-gray-700 font-medium mb-2">Email</label>
                <input type="email" id="email" th:field="*{email}"
                       class="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:border-blue-500"
                       required>
                <p th:if="${#fields.hasErrors('email')}" th:errors="*{email}" class="text-red-500 text-sm mt-1"></p>
            </div>
            <div class="mb-6">
                <label for="password" class="block text-gray-700 font-medium mb-2">Password</label>
                <input type="password" id="password" th:field="*{password}"
                       class="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:border-blue-500"
                       required>
                <p th:if="${#fields.hasErrors('password')}" th:errors="*{password}" class="text-red-500 text-sm mt-1"></p>
            </div>
            <button type="submit"
                    class="w-full bg-blue-500 hover:bg-blue-600 text-white font-medium py-2 px-4 rounded">
                Create Account
            </button>
        </form>

        <p class="mt-4 text-center text-gray-600">
            Already have an account? <a th:href="@{/login}" class="text-blue-500 hover:underline">Login</a>
        </p>
    </div>
</body>
</html>
```

#### 9.4 Home template

File: `src/main/webapp/WEB-INF/templates/pages/home.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
    <meta charset="UTF-8">
    <title>PlanIt - Home</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-gray-100 min-h-screen">
    <div th:replace="~{fragments/header :: header}"></div>

    <main class="container mx-auto px-4 py-8 text-center">
        <h1 class="text-3xl font-bold mb-4">Welcome to PlanIt</h1>
        <p class="text-gray-600 mb-8">Hello, <span sec:authentication="name">user</span></p>

        <div class="flex justify-center gap-4">
            <a th:href="@{/places}" class="bg-blue-500 hover:bg-blue-600 text-white font-medium py-3 px-6 rounded-lg">
                Explore Places
            </a>
            <a th:href="@{/plans}" class="bg-green-500 hover:bg-green-600 text-white font-medium py-3 px-6 rounded-lg">
                My Plans
            </a>
            <a th:href="@{/plans/new}" class="bg-purple-500 hover:bg-purple-600 text-white font-medium py-3 px-6 rounded-lg">
                Create Plan
            </a>
        </div>
    </main>
</body>
</html>
```

### 10. Files to delete

Delete these files (replaced by Spring Security):
- `src/main/java/com/valhalla/presentation/shared/SessionInterceptor.java`
- `src/main/java/com/valhalla/presentation/shared/UserSession.java`
- `src/main/java/com/valhalla/presentation/login/LoginRequest.java`

**Keep these files** (still needed):
- `src/main/java/com/valhalla/presentation/shared/GlobalExceptionHandler.java` — no dependency on SessionInterceptor
- `src/main/java/com/valhalla/presentation/shared/NewUserRequest.java` — used by registration form
- `src/main/java/com/valhalla/domain/login/LoginService.java` — interface still used
- `src/main/java/com/valhalla/domain/login/LoginServiceImpl.java` — registration logic still used

### 11. Update BaseJpaConfig

File: `src/main/java/com/valhalla/config/BaseJpaConfig.java`

Scan ALL domain packages (not just user + login) and ALL infrastructure packages for repositories:

```java
// Change packagesToScan to:
entityManagerFactory.setPackagesToScan(
    "com.valhalla.domain.user",
    "com.valhalla.domain.login",
    "com.valhalla.domain.place",
    "com.valhalla.domain.plan",
    "com.valhalla.domain.planplace"
);

// Change @EnableJpaRepositories to:
@EnableJpaRepositories(basePackages = "com.valhalla.infrastructure")
```

### 12. Update MyServletInitializer

File: `src/main/java/com/valhalla/MyServletInitializer.java`

Add SecurityConfig to servlet config classes:

```java
package com.valhalla;

import com.valhalla.config.DatabaseInitializationConfig;
import com.valhalla.config.JpaConfig;
import com.valhalla.config.SecurityConfig;  // ADD THIS
import com.valhalla.config.SpringWebConfig;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class MyServletInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class<?>[0];
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class<?>[] {
            SpringWebConfig.class,
            JpaConfig.class,
            DatabaseInitializationConfig.class,
            SecurityConfig.class,  // ADD THIS
        };
    }

    @Override
    protected String[] getServletMappings() {
        return new String[] { "/" };
    }
}
```

### 13. CSRF token helpers

#### In Thymeleaf forms

```html
<form th:action="@{/plans}" method="post">
    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">
    <!-- form fields -->
</form>
```

#### In Vue.js fetch() calls

Add meta tag in `<head>`:

```html
<meta name="_csrf" th:content="${_csrf.token}">
<meta name="_csrf_header" th:content="${_csrf.parameterName}">
```

Vue.js helper function:

```javascript
methods: {
    getCsrfToken() {
        const meta = document.querySelector('meta[name="_csrf"]');
        return meta ? meta.getAttribute('content') : '';
    },
    getCsrfHeader() {
        const meta = document.querySelector('meta[name="_csrf_header"]');
        return meta ? meta.getAttribute('content') : 'X-CSRF-TOKEN';
    },
    async postData(url, data) {
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [this.getCsrfHeader()]: this.getCsrfToken()
            },
            body: JSON.stringify(data)
        });
        return response.json();
    }
}
```

### 14. Environment variables

File: `.env.example`

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=valhalla
DB_USER=user
DB_PASSWORD=user

# Spring Security
SECURITY_SECRET=mySecretKeyForCSRFAtLeast32CharactersLong!

# Server
SERVER_PORT=8080
```

### 15. Security rules summary

| Route | Access |
|-------|--------|
| `/login`, `/register`, `/new-user` | Public |
| `/api/**` | Public (CSRF exempt) |
| `/share/**` | Public |
| `/places/**` | Authenticated |
| `/plans/**` | Authenticated |
| Everything else | Authenticated |

## Verification

1. `mvn test` — all tests pass
2. `mvn jetty:run` — app starts at localhost:8080
3. Open http://localhost:8080 → redirects to /login
4. Create account → redirects to /login
5. Login → redirects to /home
6. Logout → redirects to /login
7. Without session → /home redirects to /login
8. Header shows user email when logged in
9. Header shows login link when not logged in
10. Submit registration with empty email → validation error shown
11. Submit registration with short password → validation error shown
12. CSRF token present in all forms
13. POST without CSRF → 403 Forbidden
14. `/api/**` endpoints work without CSRF
15. Logout clears session

## Files to create/modify

| File | Action |
|------|--------|
| `pom.xml` | Add spring-security-web, spring-security-config, thymeleaf-extras-springsecurity6 |
| `config/SecurityConfig.java` | Replace entirely |
| `config/BaseWebConfig.java` | Remove SessionInterceptor import + registration; add SpringSecurityDialect |
| `config/BaseJpaConfig.java` | Scan all domain + infrastructure packages |
| `MyServletInitializer.java` | Add SecurityConfig.class |
| `infrastructure/security/CustomUserDetailsService.java` | Create |
| `presentation/shared/NewUserRequest.java` | Add validation annotations |
| `presentation/login/LoginController.java` | Rewrite |
| `templates/fragments/header.html` | Create (with CSRF token in logout form) |
| `templates/pages/auth/login.html` | Create (with CSRF token) |
| `templates/pages/auth/new-user.html` | Create (with CSRF token + validation errors) |
| `templates/pages/home.html` | Create |
| `.env.example` | Create with all env vars |
| `presentation/shared/SessionInterceptor.java` | Delete |
| `presentation/shared/UserSession.java` | Delete |
| `presentation/login/LoginRequest.java` | Delete |
