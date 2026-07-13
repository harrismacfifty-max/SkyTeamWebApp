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

function Get-ProcessCommandLine {
    param([System.Diagnostics.Process]$Process)
    if ($null -eq $Process) {
        return ""
    }
    try {
        $instance = Get-CimInstance Win32_Process -Filter "ProcessId = $($Process.Id)" -ErrorAction Stop
        return [string]$instance.CommandLine
    } catch {
        return ""
    }
}

function Test-SkyTeamBackendProcess {
    param([System.Diagnostics.Process]$Process)
    $commandLine = Get-ProcessCommandLine -Process $Process
    return $commandLine -like "*de.skyteam.flightschool.FlightSchoolApplication*"
}

function Test-BackendStale {
    param([System.Diagnostics.Process]$Process)
    if ($null -eq $Process -or -not $sources) {
        return $false
    }
    $latestSource = Get-Item $sources |
        Sort-Object LastWriteTimeUtc -Descending |
        Select-Object -First 1
    if ($null -eq $latestSource) {
        return $false
    }
    return $latestSource.LastWriteTimeUtc -gt $Process.StartTime.ToUniversalTime()
}

function Test-TheoryCancellationEndpoint {
    param([int]$Port)
    try {
        $login = Invoke-RestMethod -Method Post -Uri "http://localhost:$Port/api/auth/login" -ContentType "application/json" -Body '{"username":"sc901","password":"demo901"}' -TimeoutSec 2
        $headers = @{ Authorization = "Bearer $($login.data.token)" }
        $body = '{"schuelerId":"__probe__","kursId":"__probe__","grund":"Startpruefung"}'
        Invoke-RestMethod -Method Post -Uri "http://localhost:$Port/api/theorie/stornieren" -Headers $headers -ContentType "application/json" -Body $body -TimeoutSec 2 | Out-Null
        return $true
    } catch {
        $message = $_.ErrorDetails.Message
        if ([string]::IsNullOrWhiteSpace($message)) {
            return $false
        }
        return $message -notlike "*Endpoint nicht gefunden*"
    }
}

function Restart-StaleBackend {
    param(
        [int]$Port,
        [System.Diagnostics.Process]$Process,
        [string]$Reason
    )

    if (-not (Test-SkyTeamBackendProcess -Process $Process)) {
        Write-Error "Port $Port wird von Prozess $($Process.Id) ($($Process.ProcessName)) belegt, aber der Prozess konnte nicht als SkyTeam-Backend erkannt werden. Bitte diesen Prozess manuell stoppen und erneut starten."
        exit 1
    }

    Write-Host "Backend wird neu gestartet: $Reason"
    Stop-Process -Id $Process.Id -Force
    Start-Sleep -Seconds 1
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
        if (Test-BackendStale -Process $portOwner) {
            Restart-StaleBackend -Port $Port -Process $portOwner -Reason "Java-Quellen sind neuer als der laufende Prozess."
            $portOwner = $null
        } elseif (-not (Test-TheoryCancellationEndpoint -Port $Port)) {
            Restart-StaleBackend -Port $Port -Process $portOwner -Reason "laufendes Backend kennt den Theorie-Storno-Endpunkt noch nicht."
            $portOwner = $null
        } else {
            Write-Host "Backend is already running on http://localhost:$Port/api (process $($portOwner.Id), $($portOwner.ProcessName))."
            Write-Host "Open the frontend or stop that process before starting a second backend."
            exit 0
        }
    }

    if ($null -ne $portOwner) {
        Write-Error "Port $Port is already in use by process $($portOwner.Id) ($($portOwner.ProcessName)). Stop it or start this backend with -Port <otherPort>."
        exit 1
    }
}

$classpath = $classes
if ($ExtraClasspath) {
    $classpath = "$classes$([System.IO.Path]::PathSeparator)$ExtraClasspath"
}

java "-Dserver.port=$Port" -cp $classpath de.skyteam.flightschool.FlightSchoolApplication
exit $LASTEXITCODE

