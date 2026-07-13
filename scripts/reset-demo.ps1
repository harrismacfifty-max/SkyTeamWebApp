$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptRoot

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Error "Docker wurde nicht gefunden. Bitte Docker Desktop bzw. Docker Engine installieren."
    exit 1
}

Push-Location $projectRoot
try {
    docker compose version | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Docker Compose ist nicht verfuegbar. Bitte Docker mit Compose-Unterstuetzung installieren."
    }

    Write-Host "[RESET] Laufende SkyTeam-Container und persistente Demo-Daten werden entfernt..."
    docker compose down -v
    if ($LASTEXITCODE -ne 0) {
        throw "'docker compose down -v' ist mit Exitcode $LASTEXITCODE fehlgeschlagen."
    }

    Write-Host "[START] SkyTeam wird mit dem definierten Demo-Ausgangszustand neu gebaut und gestartet..."
    docker compose up --build
    if ($LASTEXITCODE -ne 0) {
        throw "'docker compose up --build' ist mit Exitcode $LASTEXITCODE fehlgeschlagen."
    }
} catch {
    Write-Error "Demo-Reset abgebrochen: $($_.Exception.Message)" -ErrorAction Continue
    exit 1
} finally {
    Pop-Location
}
