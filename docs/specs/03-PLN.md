# [PLN] Crear Plan

> Trello: https://trello.com/c/vij4lVFO/5-pln-crear-plan
> **Note:** This is an SDD proposal. Implementation may change based on team decisions.

## Objective

User can create a plan with name, description, date, and visibility (public/private).

## Prerequisites

- [LOG] completed (Spring Security configured)

## Steps

### 1. Create Plan entity

File: `src/main/java/com/valhalla/domain/plan/Plan.java`

```java
package com.valhalla.domain.plan;

import com.valhalla.domain.plan.PlanPlace;
import com.valhalla.domain.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "plans")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Plan name is required")
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "visit_date")
    private LocalDate visitDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility = Visibility.PRIVATE;

    @Column(name = "short_code", unique = true)
    private String shortCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanPlace> planPlaces = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Visibility {
        PUBLIC, PRIVATE
    }

    // Getters and setters
}
```

### 2. Create PlanRepository

File: `src/main/java/com/valhalla/domain/plan/PlanRepository.java`

```java
package com.valhalla.domain.plan;

import java.util.List;
import java.util.Optional;

public interface PlanRepository {
    Plan save(Plan plan);
    Optional<Plan> findById(Long id);
    Optional<Plan> findByShortCode(String shortCode);
    List<Plan> findByUserId(Long userId);
    void deleteById(Long id);
}
```

### 3. Create PlanService

File: `src/main/java/com/valhalla/domain/plan/PlanService.java`

```java
package com.valhalla.domain.plan;

import java.util.List;
import java.util.Optional;

public interface PlanService {
    Plan createPlan(Plan plan);
    Optional<Plan> getPlanById(Long id);
    Optional<Plan> getPlanByShortCode(String shortCode);
    List<Plan> getPlansByUserEmail(String email);
    Plan updatePlan(Plan plan);
    void deletePlan(Long id);
    String generateShortCode();
}
```

File: `src/main/java/com/valhalla/domain/plan/PlanServiceImpl.java`

```java
package com.valhalla.domain.plan;

import com.valhalla.domain.user.User;
import com.valhalla.domain.user.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PlanServiceImpl implements PlanService {

    private final PlanRepository planRepository;
    private final UserRepository userRepository;

    @Autowired
    public PlanServiceImpl(PlanRepository planRepository, UserRepository userRepository) {
        this.planRepository = planRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Plan createPlan(Plan plan) {
        if (plan.getShortCode() == null) {
            plan.setShortCode(generateShortCode());
        }
        return planRepository.save(plan);
    }

    @Override
    public Optional<Plan> getPlanById(Long id) {
        return planRepository.findById(id);
    }

    @Override
    public Optional<Plan> getPlanByShortCode(String shortCode) {
        return planRepository.findByShortCode(shortCode);
    }

    @Override
    public List<Plan> getPlansByUserEmail(String email) {
        return userRepository.findByEmail(email)
            .map(user -> planRepository.findByUserId(user.getId()))
            .orElse(List.of());
    }

    @Override
    public Plan updatePlan(Plan plan) {
        return planRepository.save(plan);
    }

    @Override
    public void deletePlan(Long id) {
        planRepository.deleteById(id);
    }

    @Override
    public String generateShortCode() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
```

### 4. Create JPA Repository

File: `src/main/java/com/valhalla/infrastructure/plan/JpaPlanRepository.java`

```java
package com.valhalla.infrastructure.plan;

import com.valhalla.domain.plan.Plan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPlanRepository extends JpaRepository<Plan, Long> {
    List<Plan> findByUserId(Long userId);
    Plan findByShortCode(String shortCode);
}
```

### 5. Create PlanRepositoryImpl

File: `src/main/java/com/valhalla/infrastructure/plan/PlanRepositoryImpl.java`

```java
package com.valhalla.infrastructure.plan;

import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class PlanRepositoryImpl implements PlanRepository {

    private final JpaPlanRepository jpaPlanRepository;

    @Autowired
    public PlanRepositoryImpl(JpaPlanRepository jpaPlanRepository) {
        this.jpaPlanRepository = jpaPlanRepository;
    }

    @Override
    public Plan save(Plan plan) {
        return jpaPlanRepository.save(plan);
    }

    @Override
    public Optional<Plan> findById(Long id) {
        return jpaPlanRepository.findById(id);
    }

    @Override
    public Optional<Plan> findByShortCode(String shortCode) {
        return Optional.ofNullable(jpaPlanRepository.findByShortCode(shortCode));
    }

    @Override
    public List<Plan> findByUserId(Long userId) {
        return jpaPlanRepository.findByUserId(userId);
    }

    @Override
    public void deleteById(Long id) {
        jpaPlanRepository.deleteById(id);
    }
}
```

### 6. Create PlanController

File: `src/main/java/com/valhalla/presentation/plan/PlanController.java`

```java
package com.valhalla.presentation.plan;

import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanService;
import com.valhalla.domain.user.User;
import com.valhalla.domain.user.UserRepository;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/plans")
public class PlanController {

    private final PlanService planService;
    private final UserRepository userRepository;

    @Autowired
    public PlanController(PlanService planService, UserRepository userRepository) {
        this.planService = planService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ModelAndView listPlans(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> model = new ModelMap();
        model.put("plans", planService.getPlansByUserEmail(userDetails.getUsername()));
        return new ModelAndView("pages/plans/list", model);
    }

    @GetMapping("/new")
    public ModelAndView newPlan() {
        Map<String, Object> model = new ModelMap();
        model.put("plan", new Plan());
        return new ModelAndView("pages/plans/new", model);
    }

    @PostMapping
    public ModelAndView createPlan(
        @Valid @ModelAttribute Plan plan,
        BindingResult bindingResult,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (bindingResult.hasErrors()) {
            return new ModelAndView("pages/plans/new");
        }
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        plan.setUser(user);
        Plan created = planService.createPlan(plan);
        return new ModelAndView("redirect:/plans/" + created.getId());
    }

    @GetMapping("/{id}")
    public ModelAndView planDetail(@PathVariable Long id) {
        Map<String, Object> model = new ModelMap();
        planService.getPlanById(id).ifPresent(plan -> model.put("plan", plan));
        return new ModelAndView("pages/plans/detail", model);
    }

    @PutMapping("/{id}")
    public ModelAndView updatePlan(
        @PathVariable Long id,
        @ModelAttribute Plan plan
    ) {
        plan.setId(id);
        planService.updatePlan(plan);
        return new ModelAndView("redirect:/plans/" + id);
    }

    @DeleteMapping("/{id}")
    public ModelAndView deletePlan(@PathVariable Long id) {
        planService.deletePlan(id);
        return new ModelAndView("redirect:/plans");
    }
}
```

### 7. Create templates

#### 7.1 Plan list

File: `src/main/webapp/WEB-INF/templates/pages/plans/list.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>My Plans</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-gray-100 min-h-screen">
    <div th:replace="~{fragments/header :: header}"></div>

    <main class="container mx-auto px-4 py-8">
        <div class="flex justify-between items-center mb-8">
            <h1 class="text-3xl font-bold">My Plans</h1>
            <a th:href="@{/plans/new}" class="bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded">
                Create Plan
            </a>
        </div>

        <div th:if="${plans}" class="grid gap-4">
            <div th:each="plan : ${plans}" class="bg-white p-6 rounded-lg shadow">
                <div class="flex justify-between items-start">
                    <div>
                        <h3 class="text-xl font-bold" th:text="${plan.name}"></h3>
                        <p class="text-gray-600 mt-1" th:text="${plan.description}"></p>
                        <span class="inline-block mt-2 px-2 py-0.5 text-xs text-white rounded"
                              th:classappend="${plan.visibility.name() == 'PUBLIC'} ? 'bg-green-500' : 'bg-gray-500'"
                              th:text="${plan.visibility}"></span>
                    </div>
                    <div class="flex gap-2">
                        <a th:href="@{/plans/{id}(id=${plan.id})}"
                           class="bg-blue-500 hover:bg-blue-600 text-white px-3 py-1 rounded text-sm">
                            View
                        </a>
                        <form th:action="@{/plans/{id}(id=${plan.id})}" method="post" class="inline">
                            <input type="hidden" name="_method" value="delete">
                            <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">
                            <button type="submit" class="bg-red-500 hover:bg-red-600 text-white px-3 py-1 rounded text-sm">
                                Delete
                            </button>
                        </form>
                    </div>
                </div>
            </div>
        </div>

        <div th:unless="${plans}" class="text-center py-16 text-gray-500">
            <p class="text-lg mb-4">You don't have any plans yet.</p>
            <a th:href="@{/plans/new}" class="bg-blue-500 hover:bg-blue-600 text-white px-6 py-3 rounded inline-block">
                Create your first plan
            </a>
        </div>
    </main>
</body>
</html>
```

#### 7.2 New plan form

File: `src/main/webapp/WEB-INF/templates/pages/plans/new.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Create Plan</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-gray-100 min-h-screen">
    <div th:replace="~{fragments/header :: header}"></div>

    <main class="container mx-auto px-4 py-8 max-w-2xl">
        <h1 class="text-3xl font-bold mb-8">Create New Plan</h1>

        <form th:action="@{/plans}" th:object="${plan}" method="post" class="bg-white p-6 rounded-lg shadow">
            <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">

            <div class="mb-4">
                <label for="name" class="block text-gray-700 font-medium mb-2">Plan Name</label>
                <input type="text" id="name" th:field="*{name}"
                       class="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:border-blue-500"
                       required placeholder="e.g., Palermo Tour">
                <p th:if="${#fields.hasErrors('name')}" th:errors="*{name}" class="text-red-500 text-sm mt-1"></p>
            </div>

            <div class="mb-4">
                <label for="description" class="block text-gray-700 font-medium mb-2">Description</label>
                <textarea id="description" th:field="*{description}" rows="3"
                          class="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:border-blue-500"
                          placeholder="Describe your plan..."></textarea>
            </div>

            <div class="mb-4">
                <label for="visitDate" class="block text-gray-700 font-medium mb-2">Visit Date</label>
                <input type="date" id="visitDate" th:field="*{visitDate}"
                       class="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:border-blue-500">
            </div>

            <div class="mb-6">
                <label for="visibility" class="block text-gray-700 font-medium mb-2">Visibility</label>
                <select id="visibility" th:field="*{visibility}"
                        class="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:border-blue-500">
                    <option value="PRIVATE">Private (only me)</option>
                    <option value="PUBLIC">Public (shareable)</option>
                </select>
            </div>

            <div class="flex gap-4 justify-end">
                <a th:href="@{/plans}" class="bg-gray-500 hover:bg-gray-600 text-white px-4 py-2 rounded">
                    Cancel
                </a>
                <button type="submit" class="bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded">
                    Create Plan
                </button>
            </div>
        </form>
    </main>
</body>
</html>
```

#### 7.3 Plan detail

File: `src/main/webapp/WEB-INF/templates/pages/plans/detail.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title th:text="${plan?.name}">Plan Detail</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-gray-100 min-h-screen">
    <div th:replace="~{fragments/header :: header}"></div>

    <main class="container mx-auto px-4 py-8" th:if="${plan}">
        <div class="flex justify-between items-start mb-8">
            <div>
                <h1 class="text-3xl font-bold" th:text="${plan.name}"></h1>
                <span class="inline-block mt-2 px-2 py-0.5 text-xs text-white rounded"
                      th:classappend="${plan.visibility.name() == 'PUBLIC'} ? 'bg-green-500' : 'bg-gray-500'"
                      th:text="${plan.visibility}"></span>
            </div>
            <div class="flex gap-2">
                <a th:href="@{/places}" class="bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded">
                    Add Places
                </a>
                <button class="bg-purple-500 hover:bg-purple-600 text-white px-4 py-2 rounded">
                    Share
                </button>
            </div>
        </div>

        <p class="text-gray-600 mb-4" th:text="${plan.description}"></p>
        <p class="text-gray-700 mb-8" th:if="${plan.visitDate}">
            <strong>Date:</strong> <span th:text="${plan.visitDate}"></span>
        </p>

        <div class="bg-white p-6 rounded-lg shadow">
            <h2 class="text-xl font-bold mb-4">Itinerary</h2>
            <p class="text-gray-500">No places added yet.</p>
        </div>
    </main>
</body>
</html>
```

## Verification

1. Go to `/plans` → shows empty list
2. Click "Create Plan" → form appears
3. Fill name, description, date, visibility
4. Submit → redirects to `/plans/{id}`
5. Detail shows name, description, date
6. Plan appears in the list
7. Submit with empty name → validation error shown

## Files to create

| File | Action |
|------|--------|
| `domain/plan/Plan.java` | Create (with @NotBlank validation) |
| `domain/plan/PlanRepository.java` | Create |
| `domain/plan/PlanService.java` | Create |
| `domain/plan/PlanServiceImpl.java` | Create |
| `infrastructure/plan/JpaPlanRepository.java` | Create |
| `infrastructure/plan/PlanRepositoryImpl.java` | Create |
| `presentation/plan/PlanController.java` | Create (with @Valid + BindingResult) |
| `templates/pages/plans/list.html` | Create (with CSRF token in delete form) |
| `templates/pages/plans/new.html` | Create (with CSRF token + validation errors) |
| `templates/pages/plans/detail.html` | Create |
