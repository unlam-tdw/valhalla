# Formato de Spec

Formato estándar para las specs del proyecto. Cada spec vive en `docs/specs/` y tiene esta estructura.

## Estructura

```markdown
# [CARD] Nombre

> Trello: <url de la card>
> **Note:** This is an SDD proposal. Implementation may change based on team decisions.

## Objetivo

Qué debería hacer el sistema (behavior, no implementation).

## Pre-requisitos

Dependencias con otras cards.

## Criterios de Aceptacion

Tabla con # y criterio. Cada criterio describe una capability del sistema observable por el usuario.

| # | Criterio |
|---|----------|
| AC-01 | ... |

## Escenarios de Test

Agrupados por capa. Cada test referencia el AC que cubre.

### Tests Unitarios (`path/to/Test.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| U-01 | ... | AC-XX |

### Tests de Integracion (`path/to/Test.java`)

| # | Test | AC que cubre |
|---|------|-------------|
| I-01 | ... | AC-XX |

### Tests de Seguridad (`path/to/Test.java`)

Solo si la card toca seguridad/rutas.

| # | Test | AC que cubre |
|---|------|-------------|
| S-01 | ... | AC-XX |

### E2E (minimos)

Solo happy path completo.

| # | Test | Flujo | AC que cubre |
|---|------|-------|-------------|
| E-01 | ... | ... | AC-XX |

## Referencia de Implementacion

> Los pasos a continuacion son guia de implementacion, no reemplazan los criterios de aceptacion de arriba.

Pasos detallados con codigo. Esta seccion es referencia para quien implementa, no define los criterios de exito.

## Archivos a crear/modificar

| Archivo | Accion |
|---------|--------|
| ... | Crear/Actualizar/Eliminar |
```

## Convenciones

- **Criterios de Aceptacion**: describen QUE hace el sistema, no COMO esta escrito
- **Escenarios de Test**: referencian AC con `AC-XX`, se agrupan por capa (unit, integration, security, E2E)
- **Referencia de Implementacion**: codigo exacto para copiar/pegar. Es guia, no spec
- **Cards de Trello**: cada card tiene dos checklists, "Implementacion" y "Tests"
- **Numeracion**: implementacion usa `[CARD-NNN]`, tests usa `[T-CARD-NNN]`
- **E2E**: minimo posible (solo happy path). La mayoria de la cobertura es unit + integration

## Relacion con Trello

Cada card en Trello tiene:

1. **Descripcion**: User Story, dependencias, link a spec
2. **Checklist "Implementacion**: tasks tecnicos numerados
3. **Checklist "Tests"**: escenarios de test numerados

Los checks de Trello corresponden a los pasos de Referencia de Implementacion (implementacion) y Escenarios de Test (tests).
