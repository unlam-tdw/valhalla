# [CMP] Compartir Plan

> Trello: https://trello.com/c/vZuil82c/7-cmp-compartir-plan
> **Note:** This is an SDD proposal. Implementation may change based on team decisions.

## Objetivo

El usuario puede compartir su plan via URL unica con shortCode desde un modal en el detalle del plan: boton, modal, copiar al portapapeles con feedback visual. El toggle publico/privado ya esta implementado.

## Pre-requisitos

- [PLN] completed (Plan entity with `Boolean isPublic` y `shortCode` de 8 chars)
- [APL-FE] completed (itinerary working)

## Criterios de Aceptacion

| # | Criterio |
|---|----------|
| AC-01 | Al hacer click en "Share" se genera una URL con el shortCode del plan |
| AC-02 | La URL tiene formato /share/{shortCode} |
| AC-03 | Se puede copiar el link al portapapeles con feedback "Copied!" |
| AC-04 | Se puede cambiar la visibilidad del plan con el checkbox `isPublic` de `/plans/{id}`. **Ya implementado**: `plans/detail.html` tiene `<input type="checkbox" th:field="*{isPublic}">` → `POST /plans/{id}` → `Plan.updateFrom()` → `planRepository.save()` |
| AC-05 | Si el plan es privado y alguien abre el link, se muestra error. La regla vive en [VPC] (07-VPC.md) |

## Escenarios de Test

### Tests Unitarios (`presentation/plan/PlanControllerTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| U-01 | `sharePlan()` retorna la URL con el shortCode existente del plan | AC-01, AC-02 |
| U-02 | `sharePlan()` con plan inexistente devuelve error | AC-01, AC-02 |

### Tests de Integracion (`integration/PlanControllerIntegrationTest.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| I-01 | `POST /plans/{id}/share` con sesion del owner retorna JSON con URL | AC-01, AC-02 |
| I-02 | `POST /plans/{id}` con `isPublic=true` → el checkbox queda tildado al recargar; con `false` → queda destildado | AC-04 |
| I-03 | `POST /plans/{id}/share` con sesion ajena al plan → 404, nunca 403 | AC-01, AC-02 |

### E2E (minimos)

| # | Test | Flujo | AC que cubre |
|---|------|-------|-------------|
| E-01 | `SharePlanE2E` | Compartir plan, copiar link, verificar el feedback "Copied!" | AC-01, AC-03 |

## Notas / decisiones de diseño

- **Esta spec queda reducida al modal de compartir** en `plans/detail.html`: el botón Share, el
  modal con la URL y el botón de copiar al portapapeles con feedback visual. `shortCode`, el
  render de la URL (`plans/detail.html:39`) y el toggle de visibilidad (`plans/detail.html:70`)
  ya existen: no son trabajo de CMP.
- **`ShareController` y `pages/share/view.html` se fueron a [VPC]** (07-VPC.md). Estaban
  definidos acá y se ejecutaban en el mismo sprint que VPC: dos personas, mismo archivo
  (`integration/ShareControllerTest.java`) y los mismos tests I-01..I-03. Con la separación, CMP
  toca `plans/detail.html` y VPC crea `share/*`: no se pisan.
- **`Boolean isPublic`, no un enum** (D4). El código usa `Boolean isPublic` y
  `plans/detail.html` ya bindea el checkbox con `th:field="*{isPublic}"`. No hay enum
  `Visibility` en el dominio ni en la base, y `Plan.class` está excluido del gate de JaCoCo
  justamente porque es una entidad con boilerplate. El toggle va por el `POST /plans/{id}` que ya
  existe, no por un endpoint `/visibility` aparte.
- **`shortCode` siempre existe** (AC-06 eliminado). `PlanServiceImpl.createPlan` lo genera siempre
  con 8 caracteres (`existsByShortCode`, 10 intentos); el propio código dice *"Always generated"*.
  No hay ningún path que deje `shortCode` en null, así que "generar al compartir" no es trabajo.
- **`generateShortCode()` es `private`** (U-05 eliminado). No hay nada público que testear desde
  acá, y el comportamiento ya está cubierto por los tests de 03-PLN.

## Referencia de Implementacion

> Los pasos a continuación son guía de implementación, no reemplazan los acceptance criteria de arriba.

### 1. Verify Plan.shortCode

The Plan entity already has `shortCode` (`String`, unique) created in [PLN], y
`PlanServiceImpl.createPlan` lo genera **siempre** con 8 caracteres. No hay nada que verificar ni
que generar desde acá: `plans/detail.html:39` ya lo renderiza.

### 2. Update PlanController (add share endpoint)

File: `src/main/java/com/valhalla/presentation/plan/PlanController.java`

Un solo endpoint nuevo. No hay endpoint de visibilidad: el toggle va por el `POST /plans/{id}`
que ya existe, con `Boolean isPublic` (D4).

```java
@PostMapping("/{id}/share")
@ResponseBody
public Map<String, String> sharePlan(
    @PathVariable Long id,
    @AuthenticationPrincipal UserDetails userDetails
) {
    Plan plan = planService.getOwnedPlan(id, userDetails.getUsername());  // 404 si no es owner
    return Map.of("url", "/share/" + plan.getShortCode());
}
```

Add required imports:
```java
import org.springframework.web.bind.annotation.ResponseBody;
```

La visibilidad se cambia por el `POST /plans/{id}` que ya existe:
`plans/detail.html` tiene `<input type="checkbox" th:field="*{isPublic}">` y `Plan.updateFrom()`
copia `isPublic`. No hay `POST /plans/{id}/visibility`, ni enum `Visibility`, ni
`Plan.Visibility.valueOf(...)`.

### 3. Update plan detail template (add share modal)

File: `src/main/webapp/WEB-INF/templates/pages/plans/detail.html`

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

## Archivos a crear/modificar

| File | Action |
|------|--------|
| `presentation/plan/PlanController.java` | Update (add `POST /plans/{id}/share`, con ownership "not found") |
| `templates/pages/plans/detail.html` | Update (add share modal + copiar al portapapeles) |

`ShareController` y `pages/share/view.html` quedan en [VPC] (07-VPC.md), junto con
`integration/ShareControllerTest.java`.
