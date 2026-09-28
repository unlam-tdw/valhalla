<#
.SYNOPSIS
    One command to run the E2E suite locally.

.DESCRIPTION
    Brings up PostgreSQL, creates the dedicated E2E database if it is missing,
    installs Chromium, exports DB_*, and runs `mvn verify` -- which starts Jetty,
    runs failsafe, and stops Jetty. PostgreSQL is stopped again on the way out,
    whether the build passed, failed, or was interrupted.

    Selectors go straight to failsafe's -Dit.test, so a name that matches nothing
    fails the build instead of reporting a green run over zero tests.

.EXAMPLE
    .\scripts\e2e.ps1
    .\scripts\e2e.ps1 -List
    .\scripts\e2e.ps1 LoginViewE2E
    .\scripts\e2e.ps1 LoginViewE2E UserViewABME2E
    .\scripts\e2e.ps1 LoginViewE2E#shouldLogoutAndReturnToLoginPage
    .\scripts\e2e.ps1 -Headed -SlowMo 300 LoginViewE2E

.NOTES
    Arguments are parsed by hand rather than with a param block: PowerShell binds an
    unbound positional like `LoginViewE2E` to the first positional parameter, which
    here is -SlowMo, and then fails to convert it to an int.
#>

$ErrorActionPreference = 'Stop'

# $env: is process-wide, not scope-wide, and Set-Location is not scoped either: both would
# otherwise survive the script and land in the caller's session. That matters because
# `docker compose` prefers the shell environment over .env, so a leaked DB_HOST=localhost
# plus DB_NAME=valhalla_e2e makes the next `docker compose up` point the app container at
# itself and at the E2E database. Save both, restore both on the way out.
$originalLocation = $PWD
$savedEnv = @{}
foreach ($name in 'DB_NAME', 'DB_HOST', 'DB_USER', 'DB_PASSWORD') {
    $savedEnv[$name] = [Environment]::GetEnvironmentVariable($name)
}

Set-Location (Join-Path $PSScriptRoot '..')

$headed = $false
$slowmo = 0
$list = $false
$selectors = @()

while ($args.Count -gt 0) {
    # if/elseif rather than switch: switch keeps evaluating after a match and falls
    # through to default, so a selector ahead of a flag made the flag look unknown.
    $arg = $args[0]
    if ($arg -match '^-(List|l)$') {
        $list = $true
    }
    elseif ($arg -match '^-(Headed|h)$') {
        $headed = $true
    }
    elseif ($arg -match '^-(SlowMo|s)$') {
        $slowmo = [int]$args[1]
        if (-not $slowmo) { throw "-SlowMo needs a number, got '$($args[1])'" }
        $args = @($args | Select-Object -Skip 1)
    }
    elseif ($arg.StartsWith('-')) {
        throw "unknown option: $arg"
    }
    else {
        $selectors += $arg
    }
    $args = @($args | Select-Object -Skip 1)
}

if ($list) {
    # Match over the whole file, not line by line: `@Test` sits on its own line above the
    # declaration, so a line-based match with `@Test` optional also lists @BeforeEach helpers
    # that failsafe cannot run.
    $testMethod = '@Test\s+(?:public\s+|protected\s+|)?void\s+([A-Za-z0-9_]+)\s*\(\s*\)'
    Get-ChildItem src/test/java -Recurse -Filter '*E2E.java' |
        Sort-Object FullName |
        ForEach-Object {
            $_.BaseName
            [regex]::Matches((Get-Content $_.FullName -Raw), $testMethod) |
                ForEach-Object { '  ' + $_.Groups[1].Value }
        }
    Set-Location $originalLocation
    exit 0
}

$e2eDb = 'valhalla_e2e'

docker compose up -d postgres
if ($LASTEXITCODE -ne 0) { throw 'docker compose up failed' }

# from here on the container is up, so everything below runs inside try/finally: a failing
# mvn, a throw, or Ctrl+C all still shut the database down. Captured before the stop,
# because the stop overwrites $LASTEXITCODE and that is the code the caller needs.
try {
    # createdb is not idempotent, so ask the catalog first. -d postgres is required:
    # without it psql connects to a database named after the user, which does not exist.
    $exists = docker compose exec -T postgres psql -U user -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='$e2eDb'"
    if ($LASTEXITCODE -ne 0) { throw 'could not query pg_database' }
    if (-not ($exists -join '').Trim()) {
        Write-Host "[e2e] creating database $e2eDb"
        docker compose exec -T postgres createdb -U user $e2eDb
    }

    # Read from the pom so a Playwright bump cannot silently drift out of sync.
    $pwVersion = (Select-String -Path pom.xml -Pattern '<playwright\.version>([^<]+)<').Matches[0].Groups[1].Value
    npx -y "playwright@$pwVersion" install chromium
    if ($LASTEXITCODE -ne 0) { throw 'playwright install failed' }

    # Jetty and failsafe must agree: Jetty needs the schema, ResetDatabase needs the
    # same data. The name must contain "e2e" or ResetDatabase refuses to run.
    $env:DB_NAME = $e2eDb
    $env:DB_HOST = 'localhost'
    $env:DB_USER = 'user'
    $env:DB_PASSWORD = 'user'

    $mvnArgs = @('verify')
    if ($selectors) { $mvnArgs += "-Dit.test=$($selectors -join ',')" }
    if ($headed) { $mvnArgs += '-De2e.headed=true' }
    if ($slowmo -gt 0) { $mvnArgs += "-De2e.slowMo=$slowmo" }

    & mvn @mvnArgs
    $exitCode = $LASTEXITCODE
}
finally {
    Write-Host '[e2e] stopping postgres'
    docker compose stop postgres

    Set-Location $originalLocation
    foreach ($name in $savedEnv.Keys) {
        # Remove-Item, not [Environment]::SetEnvironmentVariable($name, $null): on this
        # runtime that defines the variable as an empty string instead of unsetting it,
        # and an empty DB_NAME still beats the .env file and breaks the next `up`.
        if ($null -eq $savedEnv[$name]) { Remove-Item "env:$name" -ErrorAction SilentlyContinue }
        else { [Environment]::SetEnvironmentVariable($name, $savedEnv[$name]) }
    }
}

exit $exitCode
