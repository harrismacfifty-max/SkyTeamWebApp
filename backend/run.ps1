param(
    [int]$Port = 8080,
    [string]$ExtraClasspath = $env:EXTRA_CLASSPATH,
    [switch]$CompileOnly
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$classes = Join-Path $root "target/classes"
$sources = Get-ChildItem -Path (Join-Path $root "src/main/java") -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }

function Test-ApiHealth {
    param([int]$Port)
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:$Port/api/health" -TimeoutSec 2
        return $response.success -eq $true
    } catch {
        return $false
    }
}

function Get-PortOwner {
    param([int]$Port)
    $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($null -eq $connection) {
        return $null
    }
    return Get-Process -Id $connection.OwningProcess -ErrorAction SilentlyContinue
}

if (-not $sources) {
    throw "No Java sources found."
}

New-Item -ItemType Directory -Force -Path $classes | Out-Null
javac -encoding UTF-8 -d $classes $sources
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

if ($CompileOnly) {
    Write-Host "Compiled backend to $classes"
    exit 0
}

$portOwner = Get-PortOwner -Port $Port
if ($null -ne $portOwner) {
    if (Test-ApiHealth -Port $Port) {
        Write-Host "Backend is already running on http://localhost:$Port/api (process $($portOwner.Id), $($portOwner.ProcessName))."
        Write-Host "Open the frontend or stop that process before starting a second backend."
        exit 0
    }
    Write-Error "Port $Port is already in use by process $($portOwner.Id) ($($portOwner.ProcessName)). Stop it or start this backend with -Port <otherPort>."
    exit 1
}

$classpath = $classes
if ($ExtraClasspath) {
    $classpath = "$classes$([System.IO.Path]::PathSeparator)$ExtraClasspath"
}

java "-Dserver.port=$Port" -cp $classpath de.skyteam.flightschool.FlightSchoolApplication
exit $LASTEXITCODE

