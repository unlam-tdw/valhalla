# Commands Reference

## Maven

To run Maven commands, either in the IDE's integrated terminal or in another terminal, use the main `mvn` command followed by the command or lifecycle phase to execute.

> Maven runs all the phases prior to the lifecycle phase you specify.

### Lifecycle phases

```shell
# Cleans the target directory from the previous build
mvn clean

# Validates that the project is correct
mvn validate

# Compiles the project source code
mvn compile

# Runs the Java test suites (unit + MockMvc integration)
mvn test

# Packages the compiled code into a JAR or WAR file
mvn package

# Verifies that the package is valid
mvn verify

# Installs the package into the local Maven repository
mvn install
```

### Common combinations

```shell
# Most common , downloads dependencies, compiles, and runs tests
mvn clean install

# Clean build with tests
mvn clean package

# Run tests only
mvn test
```

### Development server

```shell
# Start Jetty with hot-reload (template + JS changes auto-refresh)
mvn jetty:run

# Full rebuild + start
mvn clean jetty:run
```

Jetty runs at [http://localhost:8080](http://localhost:8080). Java changes require a restart; template and vendored JS changes reload live.

## Docker

### Start / stop

```shell
# Start PostgreSQL + app with hot-reload
docker compose up

# Stop and remove containers + volumes
docker compose down --rmi local
```

### Common commands

```shell
# Show running containers
docker ps

# Show all containers
docker ps -a

# Show all images
docker images

# Show container logs
docker logs <containerId>

# Remove a container
docker rm <containerId>

# Remove an image
docker rmi <imageId>

# Run a container with bash
docker run -it --entrypoint /bin/bash valhalla
```

## Testing

```shell
# Run all Java tests (uses in-memory HSQLDB, no PostgreSQL needed)
mvn test

# Run E2E tests (requires Docker stack running)
docker compose up -d
mvn test -Dtest="LoginViewE2E"

# Run a specific E2E test
mvn test -Dtest="LoginViewE2E#shouldNavigateToHomeWhenUserExists"
```

## CI/CD (GitHub Actions)

The pipeline runs on every push and PR to `main`. It has two jobs:

### `backend` , build + test + quality gates

Runs `mvn clean verify --fail-at-end` which triggers:
1. Prettier formatting (auto-fix)
2. Checkstyle (naming, Javadoc, imports)
3. PMD + CPD (logic issues, duplication)
4. Unit + integration tests (HSQLDB)
5. JaCoCo coverage check (80% floor, 100% for domain/presentation)

If any gate fails, the build fails.

### `e2e` , Playwright against a real stack

1. Spins up a PostgreSQL service container
2. Installs Playwright's Chromium
3. Packages the app (`mvn package -DskipTests`)
4. Starts Jetty against the local Postgres
5. Runs E2E tests (`LoginViewE2E`, `UserViewABME2E`) with quality gates skipped

**To run the full pipeline locally before pushing:**

```shell
mvn clean verify
```
