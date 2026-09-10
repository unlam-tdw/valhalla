# [CMP] Share Plan

> Trello: https://trello.com/c/vZuil82c/7-cmp-compartir-plan
> **Note:** This is an SDD proposal. Implementation may change based on team decisions.

## Objective

User can share their plan via unique URL. Copy link with feedback. Public/private control.

## Prerequisites

- [PLN] completed (Plan entity with shortCode)
- [APL] completed (itinerary working)

## Steps

### 1. Verify Plan.shortCode

The Plan entity already has `shortCode` (String, unique) created in [PLN].

### 2. Verify PlanService.generateShortCode()

Already exists in [PLN]. Generates 8-character UUID.

### 3. Add CSRF meta tag to plan detail template

For Vue.js `fetch()` calls to POST endpoints outside `/api/**`, CSRF token must be sent manually. Add this to the `<head>` of `plans/detail.html`:

```html
<meta name="_csrf" th:content="${_csrf.token}">
<meta name="_csrf_header" th:content="${_csrf.parameterName}">
```

### 4. Update PlanController (add share endpoints)

File: `src/main/java/com/valhalla/presentation/plan/PlanController.java`

Add these methods:

```java
@PostMapping("/{id}/share")
@ResponseBody
public Map<String, String> sharePlan(@PathVariable Long id) {
    Plan plan = planService.getPlanById(id)
        .orElseThrow(() -> new RuntimeException("Plan not found"));

    if (plan.getShortCode() == null) {
        plan.setShortCode(planService.generateShortCode());
        planService.updatePlan(plan);
    }

    String shareUrl = "/share/" + plan.getShortCode();
    return Map.of("url", shareUrl);
}

@PostMapping("/{id}/visibility")
@ResponseBody
public Map<String, String> updateVisibility(
    @PathVariable Long id,
    @RequestParam String visibility
) {
    Plan plan = planService.getPlanById(id)
        .orElseThrow(() -> new RuntimeException("Plan not found"));
    plan.setVisibility(Plan.Visibility.valueOf(visibility));
    planService.updatePlan(plan);
    return Map.of("visibility", visibility);
}
```

Add required imports:
```java
import org.springframework.web.bind.annotation.ResponseBody;
```

### 5. Update plan detail template (add share modal)

File: `src/main/webapp/WEB-INF/templates/pages/plans/detail.html`

Add CSRF meta tag to `<head>` (from step 3).

Add within the Vue app div (after the main content):

```html
<!-- Share Modal -->
<div v-if="showShareModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
    <div class="bg-white p-6 rounded-lg shadow-lg max-w-md w-full mx-4">
        <h3 class="text-xl font-bold mb-4">Share Plan</h3>
        <p class="text-gray-600 mb-4">Copy this link to share your plan:</p>
        <div class="flex gap-2 mb-4">
            <input type="text" :value="shareUrl" readonly
                   class="flex-1 px-3 py-2 border rounded focus:outline-none">
            <button @click="copyShareLink"
                    class="bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded">
                Copy
            </button>
        </div>
        <p v-if="copied" class="text-green-500 text-sm mb-4">Copied!</p>
        <button @click="showShareModal = false"
                class="w-full bg-gray-500 hover:bg-gray-600 text-white px-4 py-2 rounded">
            Close
        </button>
    </div>
</div>
```

Add to the Vue setup:

```javascript
const showShareModal = ref(false);
const shareUrl = ref('');
const copied = ref(false);

function getCsrfToken() {
    const meta = document.querySelector('meta[name="_csrf"]');
    return meta ? meta.content : '';
}

function getCsrfHeader() {
    const meta = document.querySelector('meta[name="_csrf_header"]');
    return meta ? meta.content : '';
}

async function sharePlan() {
    const response = await fetch('/plans/' + planId + '/share', {
        method: 'POST',
        headers: { [getCsrfHeader()]: getCsrfToken() }
    });
    const data = await response.json();
    shareUrl.value = window.location.origin + data.url;
    showShareModal.value = true;
    copied.value = false;
}

function copyShareLink() {
    navigator.clipboard.writeText(shareUrl.value);
    copied.value = true;
    setTimeout(() => { copied.value = false; }, 2000);
}
```

## Verification

1. Go to `/plans/{id}`
2. Click "Share" → modal appears
3. Link generates with shortCode
4. Click "Copy" → feedback "Copied!"
5. Open link in another window → plan visible
6. Toggle visibility → changes PUBLIC/PRIVATE
7. If PRIVATE and someone opens link → error

## Files to modify

| File | Action |
|------|--------|
| `presentation/plan/PlanController.java` | Update (add share endpoints) |
| `templates/pages/plans/detail.html` | Update (add CSRF meta tag + share modal with CSRF in fetch) |
