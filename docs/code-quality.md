# Code Quality Tools

The project integrates several tools to ensure the code is clean, maintainable and free of common mistakes. These tools run automatically during the Maven lifecycle.

## Overview

| Tool | Main Function | Role in the Project | Report Location |
| :--- | :--- | :--- | :--- |
| **Prettier** | **Automatic Formatting** | Defines the aesthetics (spaces, braces, indentation). It handles the "look". | N/A (Applies changes) |
| **Checkstyle** | **Conventions and Structure** | Validates variable names, Javadoc presence and imports. | `.code-quality/checkstyle/` |
| **PMD** | **Logic and Best Practices** | Detects potential errors, unused variables and optimizations. | `.code-quality/pmd/` |
| **CPD** | **Duplicate Detection** | Finds copied and pasted code blocks (Copy-Paste). | `.code-quality/cpd/` |
| **JaCoCo** | **Test Coverage** | Measures what percentage of the code is covered by tests (>80%). | `.code-quality/jacoco/` |

## PMD (Static Code Analyzer)

Analyzes Java code for design issues, unused variables, missing optimizations and bad practices.

- **Runs in:** `validate` phase (check) and `test` phase (report regeneration).
- **Configuration:** Uses `pmd-code-rules.xml`.

```shell
# Validates the rules and fails on violations
mvn pmd:check

# Generates the visual report
mvn pmd:pmd
```

- **Reports:** All results (XML and HTML) are saved in the `.code-quality/pmd/` folder.
- **Failure example:** Creating a variable with a very short name (e.g. `int x = 0;`) or having a method with cyclomatic complexity above 10.
- **Documentation:** [PMD Official Site](https://pmd.github.io/)

## CPD (Copy-Paste Detector)

An extension of PMD that detects duplicated (copy-paste) code blocks in the project.

- **Runs in:** `validate` phase (check) and `test` phase (report generation).

```shell
# Validates duplicates and fails if it finds any
mvn pmd:cpd-check

# Generates the duplicates report
mvn pmd:cpd
```

- **Reports:** All results are saved in the `.code-quality/cpd/` folder.
- **Failure example:** Copying and pasting an identical logic block into two different controllers instead of abstracting it into a service.
- **Documentation:** [CPD Documentation](https://pmd.github.io/latest/pmd_userdocs_cpd.html)

## Checkstyle

Verifies that the code follows formatting and style standards (based on the Google guide). It focuses on the aesthetics and structure of the code.

- **Runs in:** `validate` phase.
- **Configuration:** Uses `checkstyle-base.xml`. This file inherits from Google Style but **disables the formatting rules** (indentation, spaces, braces) to avoid conflicts with Prettier, focusing only on naming conventions, Javadocs and imports.

```shell
# Validates the style and fails on violations
mvn checkstyle:check

# Generates the visual report
mvn checkstyle:checkstyle
```

- **Reports:** All results are saved in the `.code-quality/checkstyle/` folder.
- **Failure example:** Using incorrect variable names (e.g. `int MiVariable`), missing Javadoc on public classes, or star imports (`import java.util.*`).
- **Documentation:** [Checkstyle Google Style](https://checkstyle.sourceforge.io/google_style.html)

## Prettier (Maven Plugin)

Automatically formats Java code so it complies with the style rules.

- **Runs in:** `process-sources` phase (before compiling).

```shell
# Formats and overwrites the files with the correct style
mvn prettier:write

# Verifies without modifying files
mvn prettier:check
```

- **Documentation:** [Prettier Java](https://github.com/jhipster/prettier-java)

## JaCoCo (Code Coverage)

Measures what percentage of the source code is covered by the tests.

- **Runs in:** `test` phase.
- **Configuration:** Per-package gates:
  - `domain` and `presentation` must reach **100%** line coverage
  - `infrastructure` must reach **80%**
  - Plus an **80%** global floor
  - `config`, the JPA entity and the DTOs are excluded (pure boilerplate)

```shell
# Generates the visual report
mvn jacoco:report
```

- **Reports:** All results are saved in the `.code-quality/jacoco/` folder.

### Generate a fresh report

Since results are saved outside `target`, delete previous results first:

**Linux / macOS / Git Bash:**
```shell
rm -rf .code-quality/jacoco && mvn clean test
```

**Windows (PowerShell):**
```powershell
Remove-Item -Recurse -Force .code-quality/jacoco; mvn clean test
```

**Windows (CMD):**
```cmd
rd /s /q .code-quality\jacoco & mvn clean test
```

## Running the gates

`scripts/gate.ps1` is the single entry point for every tool on this page.

```powershell
.\scripts\gate.ps1 check       # Checkstyle + PMD + CPD + Prettier, changes no files
.\scripts\gate.ps1 check -Fix  # same, but Prettier is allowed to rewrite the files
.\scripts\gate.ps1 coverage    # run the whole suite, print line coverage per package
```

`check` invokes `prettier:check`, so it never reformats your tree — `-Fix` is the explicit
opt-in for that.

That matters when comparing with Maven. `mvn validate` on its own is safe (it stops at the
`validate` phase, before Prettier's `write` at `process-sources`), but every longer goal —
`mvn test`, `mvn package`, `mvn verify` — reaches `process-sources` and **rewrites your
files**. Use `gate.ps1 check` when you want to look at a style problem without losing an
unrelated change.

`-Fast` on any test command skips these gates along with JaCoCo (it turns on the `dev`
profile). `coverage` ignores `-Fast`, since that profile disables JaCoCo and there would be
nothing left to report.
