param(
    [int]$BackendPort = 8080,
    [int]$FrontendPort = 8000,
    [switch]$NoBrowser
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$launcher = Join-Path $projectRoot "start-skyteam.ps1"
$driverJar = Join-Path $projectRoot "backend\lib\ojdbc11.jar"

function Get-PortOwner {
    param([int]$Port)
    $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($null -eq $connection) {
        return $null
    }
    return Get-Process -Id $connection.OwningProcess -ErrorAction SilentlyContinue
}

function Get-OracleHealth {
    param([int]$Port)
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:$Port/api/health" -TimeoutSec 2
        if ($response.success -eq $true -and
            $response.data.profile -eq "oracle" -and
            $response.data.databaseConnected -eq $true) {
            return $response
        }
    } catch {
        return $null
    }
    return $null
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

if (-not (Test-Path -LiteralPath $launcher -PathType Leaf)) {
    Write-Error "Startskript nicht gefunden: $launcher"
    exit 1
}

if (-not (Test-Path -LiteralPath $driverJar -PathType Leaf)) {
    Write-Error "Oracle JDBC-Treiber fehlt. Lege ojdbc11.jar unter 'backend\lib\ojdbc11.jar' ab. Die JAR wird nicht in Git eingecheckt."
    exit 1
}

if (-not (Get-Command java -ErrorAction SilentlyContinue) -or
    -not (Get-Command javac -ErrorAction SilentlyContinue)) {
    Write-Error "Java/Javac wurde nicht gefunden. Installiere ein JDK 17 oder neuer."
    exit 1
}

$backendOwner = Get-PortOwner -Port $BackendPort
$backendAlreadyRunning = $false
if ($null -ne $backendOwner) {
    $backendAlreadyRunning = $null -ne (Get-OracleHealth -Port $BackendPort)
    if (-not $backendAlreadyRunning) {
        Write-Error "Backend-Port $BackendPort ist durch Prozess $($backendOwner.Id) ($($backendOwner.ProcessName)) belegt, aber dort laeuft kein verbundenes Oracle-Profil."
        exit 1
    }
    Write-Host "[OK] Verbundenes Oracle-Backend laeuft bereits auf Port $BackendPort."
}

$frontendOwner = Get-PortOwner -Port $FrontendPort
if ($null -ne $frontendOwner -and -not (Test-HttpEndpoint -Url "http://localhost:$FrontendPort")) {
    Write-Error "Frontend-Port $FrontendPort ist durch Prozess $($frontendOwner.Id) ($($frontendOwner.ProcessName)) belegt, antwortet aber nicht per HTTP."
    exit 1
}

$securePassword = $null
$plainPassword = $null
$passwordPointer = [IntPtr]::Zero
$startInfo = $null

try {
    if (-not $backendAlreadyRunning) {
        $securePassword = Read-Host "Oracle-Passwort fuer DB_USER SkyTeam" -AsSecureString
        if ($securePassword.Length -eq 0) {
            Write-Error "DB_PASSWORD darf nicht leer sein."
            exit 1
        }
        $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
        $plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    }

    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = "powershell.exe"
    $startInfo.Arguments = "-NoProfile -ExecutionPolicy Bypass -File `"$launcher`" -BackendPort $BackendPort -FrontendPort $FrontendPort"
    if ($NoBrowser) {
        $startInfo.Arguments += " -NoBrowser"
    }
    $startInfo.WorkingDirectory = $projectRoot
    $startInfo.UseShellExecute = $false
    $startInfo.EnvironmentVariables["APP_PROFILE"] = "oracle"
    $startInfo.EnvironmentVariables["DB_URL"] = "jdbc:oracle:thin:@rs03-db-inf-min.ad.fh-bielefeld.de:1521:ORCL"
    $startInfo.EnvironmentVariables["DB_USER"] = "SkyTeam"
    $startInfo.EnvironmentVariables["DB_DRIVER"] = "oracle.jdbc.OracleDriver"
    $startInfo.EnvironmentVariables["EXTRA_CLASSPATH"] = $driverJar
    if (-not $backendAlreadyRunning) {
        $startInfo.EnvironmentVariables["DB_PASSWORD"] = $plainPassword
    }

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo
    if (-not $process.Start()) {
        Write-Error "Oracle-Launcher konnte nicht gestartet werden."
        exit 1
    }

    if ($startInfo.EnvironmentVariables.ContainsKey("DB_PASSWORD")) {
        $startInfo.EnvironmentVariables.Remove("DB_PASSWORD")
    }
    $plainPassword = $null
    $process.WaitForExit()
    exit $process.ExitCode
} finally {
    $plainPassword = $null
    if ($passwordPointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
}
