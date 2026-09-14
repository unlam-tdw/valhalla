# Error Handling

How errors are handled across all layers of the application.

## Overview

```
Exception thrown in domain/service
        ↓
GlobalExceptionHandler catches it
        ↓
Returns ModelAndView with error message + re-renders the form
```

## Validation Errors

Validation happens in two places:

### 1. DTO Validation (before reaching the service)

Using Hibernate Validator annotations on DTOs:

```java
package com.valhalla.presentation.login;

public class LoginRequest {

  @NotBlank(message = "Email is required")
  @Email(message = "Email is not valid")
  private String email;

  @NotBlank(message = "Password is required")
  private String password;
}
```

Controller checks `BindingResult`:

```java
package com.valhalla.presentation.login;

@PostMapping("/admin/validate-login")
public ModelAndView validateLogin(
  @Valid @ModelAttribute("loginData") LoginRequest loginData,
  BindingResult bindingResult
) {
  if (bindingResult.hasErrors()) {
    // Re-render form with validation errors
    return renderLoginWithError(loginData);
  }
  // ... proceed with business logic
}
```

Template displays errors:

```html
<p th:if="${#fields.hasErrors('email')}" th:errors="*{email}" class="text-sm text-red-600"></p>
```

### 2. Business Errors (thrown by the service)

Services throw custom exceptions:

```java
package com.valhalla.domain.login;

@Service
public class LoginServiceImpl implements LoginService {

  @Override
  public void register(String email, String password) {
    if (userRepository.findByEmail(email).isPresent()) {
      throw new UserAlreadyExists();
    }
    // ... create user
  }
}
```

## Custom Exceptions

All domain exceptions live in `com.valhalla.domain.exception`:

```java
package com.valhalla.domain.exception;

public class UserAlreadyExists extends RuntimeException {}
```

```java
package com.valhalla.domain.exception;

public class UserNotFoundException extends RuntimeException {}
```

**Rules:**
- Extend `RuntimeException` (unchecked exceptions)
- One class per exception
- Name them descriptively: `UserAlreadyExists`, not `DuplicateError`

## Global Exception Handling

`GlobalExceptionHandler` is a `@ControllerAdvice` that catches exceptions thrown by any controller:

```java
package com.valhalla.presentation.shared;

@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger LOGGER = Logger.getLogger(GlobalExceptionHandler.class.getName());

  @ExceptionHandler(UserAlreadyExists.class)
  public ModelAndView handleUserAlreadyExists() {
    Map<String, Object> model = new ModelMap();
    model.put("newUserData", new NewUserRequest());
    model.put("error", "Email is already registered");
    return new ModelAndView("pages/auth/new-user", model);
  }

  @ExceptionHandler(Exception.class)
  public ModelAndView handleUnexpectedError(Exception ex) {
    LOGGER.log(Level.SEVERE, "Unhandled error", ex);
    Map<String, Object> model = new ModelMap();
    model.put("error", "An unexpected error occurred");
    return new ModelAndView("pages/error", model);
  }
}
```

**How it works:**
1. Service throws `UserAlreadyExists`
2. Controller doesn't catch it (lets it propagate)
3. `GlobalExceptionHandler` catches it
4. Returns the registration form with an error message

## Error Messages

All error messages are in **English**. They are defined in:
- DTO validation annotations (`message` attribute)
- Controller helper methods (e.g., `renderLoginWithError`)
- `GlobalExceptionHandler` handlers

**Convention:** error messages start with a capital letter, no period at the end.

## Testing Error Handling

See [testing.md](testing.md) for patterns on testing error handling.

Key tests to write:
- Controller returns correct view on validation error
- Controller throws exception on business error
- `GlobalExceptionHandler` catches and returns correct view
