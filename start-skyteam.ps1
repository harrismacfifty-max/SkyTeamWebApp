param(
    [int]$BackendPort = 8080,
    [int]$FrontendPort = 8000,
    [switch]$NoBrowser
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendScript = Join-Path $projectRoot "backend\run.ps1"
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

New-Item -ItemType Directory -Force -Path $logDir | Out-Null

Write-Host "SkyTeam Flugschule wird vorbereitet..."

if (-not (Test-Path $backendScript)) {
    throw "Backend-Startskript nicht gefunden: $backendScript"
}

if (-not (Test-Path $frontendHtml)) {
    throw "Frontend-Fallback nicht gefunden: $frontendHtml"
}

if (Test-ApiHealth -Port $BackendPort) {
    Write-Host "[OK] Backend laeuft bereits: http://localhost:$BackendPort/api"
} else {
    $backendPortOwner = Get-PortOwner -Port $BackendPort
    if ($null -ne $backendPortOwner) {
        Write-Error "Port $BackendPort ist belegt durch Prozess $($backendPortOwner.Id) ($($backendPortOwner.ProcessName)), aber der Healthcheck antwortet nicht. Bitte Prozess beenden oder anderen Backend-Port nutzen."
        exit 1
    }

    if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
        Write-Error "Java wurde nicht gefunden. Bitte Java 17 oder neuer installieren und danach erneut starten."
        exit 1
    }

    if (-not (Get-Command javac -ErrorAction SilentlyContinue)) {
        Write-Error "javac wurde nicht gefunden. Bitte ein JDK installieren, nicht nur eine JRE."
        exit 1
    }

    $env:APP_PROFILE = if ($env:APP_PROFILE) { $env:APP_PROFILE } else { "dev" }
    $backendArgs = "-NoProfile -ExecutionPolicy Bypass -File `"$backendScript`" -Port $BackendPort"

    Write-Host "[START] Backend wird auf Port $BackendPort gestartet..."
    $backendProcess = Start-Process `
        -FilePath "powershell.exe" `
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
            $phpArgs = "-S localhost:$FrontendPort -t `"$frontendRoot`""

            Write-Host "[START] Frontend-Server wird auf Port $FrontendPort gestartet..."
            Start-Process `
                -FilePath $phpCommand.Source `
                -ArgumentList $phpArgs `
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
