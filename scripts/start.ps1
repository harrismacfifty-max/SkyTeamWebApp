param(
    [int]$BackendPort = 8080,
    [int]$FrontendPort = 8000,
    [switch]$NoBrowser,
    [switch]$Docker
)

$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptRoot
$backendSourceRoot = Join-Path $projectRoot "backend\src\main\java"
$backendClasses = Join-Path $projectRoot "backend\target\classes"
$frontendRoot = Join-Path $projectRoot "frontend"
$frontendHtml = Join-Path $frontendRoot "index.html"
$logDir = Join-Path $projectRoot "backend\target\logs"
$backendOutLog = Join-Path $logDir "backend.out.log"
$backendErrLog = Join-Path $logDir "backend.err.log"
$frontendOutLog = Join-Path $logDir "frontend.out.log"
$frontendErrLog = Join-Path $logDir "frontend.err.log"

function Test-ApiHealth {
    param([int]$Port)
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:$Port/api/health" -TimeoutSec 2
        return $response.success -eq $true
    } catch {
        return $false
    }
}

function Test-HttpEndpoint {
    param([string]$Url)
    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 2
        return $response.StatusCode -ge 200 -and $response.StatusCode -lt 500
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
    if ($null -eq $Process -or -not (Test-Path $backendSourceRoot)) {
        return $false
    }
    $latestSource = Get-ChildItem -Path $backendSourceRoot -Recurse -Filter "*.java" |
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

    Write-Host "[RESTART] Backend wird neu gestartet: $Reason"
    Stop-Process -Id $Process.Id -Force
    Start-Sleep -Seconds 1
}

function Wait-ForBackend {
    param(
        [int]$Port,
        [System.Diagnostics.Process]$Process
    )

    for ($attempt = 1; $attempt -le 40; $attempt++) {
        if (Test-ApiHealth -Port $Port) {
            return $true
        }

        if ($null -ne $Process) {
            $Process.Refresh()
            if ($Process.HasExited) {
                return $false
            }
        }

        Start-Sleep -Milliseconds 500
    }

    return $false
}

function Show-LogHint {
    param([string]$Path)
    if (Test-Path $Path) {
        Write-Host "Log: $Path"
    }
}

function Start-DockerCompose {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        Write-Error "Docker wurde nicht gefunden. Bitte Docker Desktop installieren oder ohne -Docker lokal mit Java/PHP starten."
        exit 1
    }

    Push-Location $projectRoot
    try {
        docker compose version | Out-Null
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Docker Compose ist nicht verfuegbar. Bitte Docker Desktop mit Compose installieren."
            exit 1
        }

        docker compose up --build
        exit $LASTEXITCODE
    } finally {
        Pop-Location
    }
}

if ($Docker) {
    & (Join-Path $scriptRoot "check-encoding.ps1")
    Start-DockerCompose
}

New-Item -ItemType Directory -Force -Path $backendClasses, $logDir | Out-Null

Write-Host "SkyTeam Flugschule wird lokal im Demo-Modus gestartet..."

& (Join-Path $scriptRoot "check-encoding.ps1")

if (-not (Test-Path $backendSourceRoot)) {
    Write-Error "Backend-Quellen nicht gefunden: $backendSourceRoot"
    exit 1
}

if (-not (Test-Path $frontendHtml)) {
    Write-Error "Frontend-Fallback nicht gefunden: $frontendHtml"
    exit 1
}

$backendPortOwner = Get-PortOwner -Port $BackendPort
if (Test-ApiHealth -Port $BackendPort) {
    if (Test-BackendStale -Process $backendPortOwner) {
        Restart-StaleBackend -Port $BackendPort -Process $backendPortOwner -Reason "Java-Quellen sind neuer als der laufende Prozess."
        $backendPortOwner = $null
    } elseif (-not (Test-TheoryCancellationEndpoint -Port $BackendPort)) {
        Restart-StaleBackend -Port $BackendPort -Process $backendPortOwner -Reason "laufendes Backend kennt den Theorie-Storno-Endpunkt noch nicht."
        $backendPortOwner = $null
    } else {
        Write-Host "[OK] Backend laeuft bereits: http://localhost:$BackendPort/api"
    }
}

if (-not (Test-ApiHealth -Port $BackendPort)) {
    $backendPortOwner = Get-PortOwner -Port $BackendPort
    if ($null -ne $backendPortOwner) {
        Write-Error "Port $BackendPort ist belegt durch Prozess $($backendPortOwner.Id) ($($backendPortOwner.ProcessName)), aber /api/health antwortet nicht."
        exit 1
    }

    $javaCommand = Get-Command java -ErrorAction SilentlyContinue
    $javacCommand = Get-Command javac -ErrorAction SilentlyContinue

    if ($null -eq $javaCommand -or $null -eq $javacCommand) {
        Write-Error "Java/JDK wurde nicht gefunden. Bitte Java 17 oder neuer installieren. Alternativ: docker compose up --build"
        exit 1
    }

    $sources = Get-ChildItem -Path $backendSourceRoot -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
    if (-not $sources) {
        Write-Error "Keine Java-Quellen gefunden unter: $backendSourceRoot"
        exit 1
    }

    & $javacCommand.Source -encoding UTF-8 -d $backendClasses $sources
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Backend-Kompilierung fehlgeschlagen."
        exit $LASTEXITCODE
    }

    $env:APP_PROFILE = if ($env:APP_PROFILE) { $env:APP_PROFILE } else { "demo" }
    $classpath = $backendClasses
    if ($env:EXTRA_CLASSPATH) {
        $classpath = "$backendClasses$([System.IO.Path]::PathSeparator)$env:EXTRA_CLASSPATH"
    }

    Write-Host "[START] Backend wird auf Port $BackendPort gestartet..."
    $backendArgs = @("-Dserver.port=$BackendPort", "-cp", "`"$classpath`"", "de.skyteam.flightschool.FlightSchoolApplication")
    $backendProcess = Start-Process `
        -FilePath $javaCommand.Source `
        -ArgumentList $backendArgs `
        -WorkingDirectory $projectRoot `
        -WindowStyle Hidden `
        -PassThru `
        -RedirectStandardOutput $backendOutLog `
        -RedirectStandardError $backendErrLog

    if (-not (Wait-ForBackend -Port $BackendPort -Process $backendProcess)) {
        Write-Error "Backend konnte nicht gestartet werden."
        Show-LogHint -Path $backendOutLog
        Show-LogHint -Path $backendErrLog
        exit 1
    }

    Write-Host "[OK] Backend gestartet: http://localhost:$BackendPort/api"
}

$phpCommand = Get-Command php -ErrorAction SilentlyContinue
$frontendUrl = "http://localhost:$FrontendPort"
$frontendTarget = $null

if ($null -ne $phpCommand) {
    if (Test-HttpEndpoint -Url $frontendUrl) {
        Write-Host "[OK] Frontend-Server laeuft bereits: $frontendUrl"
        $frontendTarget = $frontendUrl
    } else {
        $frontendPortOwner = Get-PortOwner -Port $FrontendPort
        if ($null -eq $frontendPortOwner) {
            $env:API_BASE_URL = "http://localhost:$BackendPort/api"
            $frontendArgs = "-S localhost:$FrontendPort -t `"$frontendRoot`""
            Write-Host "[START] Frontend-Server wird auf Port $FrontendPort gestartet..."

            Start-Process `
                -FilePath $phpCommand.Source `
                -ArgumentList $frontendArgs `
                -WorkingDirectory $projectRoot `
                -WindowStyle Hidden `
                -RedirectStandardOutput $frontendOutLog `
                -RedirectStandardError $frontendErrLog | Out-Null

            Start-Sleep -Seconds 1

            if (Test-HttpEndpoint -Url $frontendUrl) {
                Write-Host "[OK] Frontend-Server gestartet: $frontendUrl"
                $frontendTarget = $frontendUrl
            } else {
                Write-Host "[WARN] Frontend-Server antwortet nicht. Oeffne statischen Fallback."
                Show-LogHint -Path $frontendOutLog
                Show-LogHint -Path $frontendErrLog
                $frontendTarget = $frontendHtml
            }
        } else {
            Write-Host "[WARN] Frontend-Port $FrontendPort ist belegt durch Prozess $($frontendPortOwner.Id) ($($frontendPortOwner.ProcessName)). Oeffne statischen Fallback."
            $frontendTarget = $frontendHtml
        }
    }
} else {
    Write-Host "[INFO] PHP wurde nicht gefunden. Oeffne statischen Frontend-Fallback."
    $frontendTarget = $frontendHtml
}

if ($NoBrowser) {
    Write-Host "Browserstart uebersprungen. Ziel waere: $frontendTarget"
    exit 0
}

Write-Host "[OPEN] $frontendTarget"
Start-Process $frontendTarget
