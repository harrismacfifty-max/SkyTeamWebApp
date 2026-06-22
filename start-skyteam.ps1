param(
    [int]$BackendPort = 8080,
    [int]$FrontendPort = 8000,
    [switch]$NoBrowser,
    [switch]$Docker
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$starter = Join-Path $projectRoot "scripts\start.ps1"

if (-not (Test-Path $starter)) {
    Write-Error "Startskript nicht gefunden: $starter"
    exit 1
}

$starterArgs = @{
    BackendPort = $BackendPort
    FrontendPort = $FrontendPort
}

if ($NoBrowser) {
    $starterArgs.NoBrowser = $true
}

if ($Docker) {
    $starterArgs.Docker = $true
}

& $starter @starterArgs
exit $LASTEXITCODE
