$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Net.Http

$apiBaseUrl = if ([string]::IsNullOrWhiteSpace($env:API_BASE_URL)) {
    "http://localhost:8080/api"
} else {
    $env:API_BASE_URL.TrimEnd("/")
}

function Invoke-SmokeRequest {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Path,
        [string]$Token = "",
        [string]$Body = ""
    )

    $handler = New-Object System.Net.Http.HttpClientHandler
    $client = New-Object System.Net.Http.HttpClient($handler)
    try {
        $request = New-Object System.Net.Http.HttpRequestMessage(
            [System.Net.Http.HttpMethod]::new($Method),
            "$apiBaseUrl$Path"
        )
        if (-not [string]::IsNullOrWhiteSpace($Token)) {
            $request.Headers.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", $Token)
        }
        if ($Method -ne "GET") {
            $payload = if ([string]::IsNullOrWhiteSpace($Body)) { "{}" } else { $Body }
            $request.Content = New-Object System.Net.Http.StringContent($payload, [System.Text.Encoding]::UTF8, "application/json")
        }

        $response = $client.SendAsync($request).GetAwaiter().GetResult()
        try {
            $responseBody = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
            $json = $null
            if (-not [string]::IsNullOrWhiteSpace($responseBody)) {
                try {
                    $json = $responseBody | ConvertFrom-Json
                } catch {
                    $json = $null
                }
            }
            return [pscustomobject]@{
                Status = [int]$response.StatusCode
                Body = $responseBody
                Json = $json
            }
        } finally {
            $response.Dispose()
        }
    } catch {
        throw "API-Aufruf $Method $Path fehlgeschlagen: $($_.Exception.Message)"
    } finally {
        $client.Dispose()
        $handler.Dispose()
    }
}

function Assert-Status {
    param($Response, [int]$Expected, [string]$Case)
    if ($Response.Status -ne $Expected) {
        throw "$Case fehlgeschlagen: erwartet HTTP $Expected, erhalten HTTP $($Response.Status). Antwort: $($Response.Body)"
    }
}

function Assert-BodyContains {
    param($Response, [string]$Expected, [string]$Case)
    if (-not $Response.Body.Contains($Expected)) {
        throw "$Case fehlgeschlagen: '$Expected' fehlt in der Antwort. Antwort: $($Response.Body)"
    }
}

function Get-Token {
    param([string]$Username, [string]$Password)
    $login = Invoke-SmokeRequest -Method "POST" -Path "/auth/login" -Body (@{
        username = $Username
        password = $Password
    } | ConvertTo-Json -Compress)
    Assert-Status $login 200 "Login $Username"
    if ($null -eq $login.Json -or [string]::IsNullOrWhiteSpace($login.Json.data.token)) {
        throw "Login $Username lieferte kein Token. Antwort: $($login.Body)"
    }
    return $login.Json.data.token
}

Write-Host "SkyTeam Praesentations-Smoke-Test gegen $apiBaseUrl"
Write-Host "Hinweis: Der Test legt Buchungen an und bestaetigt AA906. Vor jedem Lauf Demo-Daten zuruecksetzen."

$health = Invoke-SmokeRequest -Method "GET" -Path "/health"
Assert-Status $health 200 "Health-Endpoint"
Assert-BodyContains $health '"status":"ok"' "Health-Endpoint"
Assert-BodyContains $health '"activeProfile":"demo"' "Health-Endpoint"
Assert-BodyContains $health '"databaseMode":"demo"' "Health-Endpoint"
Write-Host "[OK] Vorbereitung: API erreichbar."

$studentToken = Get-Token -Username "sc901" -Password "demo901"
$managementToken = Get-Token -Username "demo2" -Password "demo2"

$openRequest = Invoke-SmokeRequest -Method "GET" -Path "/verwaltung/abschlussanfragen/AA906" -Token $managementToken
Assert-Status $openRequest 200 "Ausgangszustand AA906"
if ($openRequest.Body -notmatch '"status":"ANGEFRAGT"') {
    throw "AA906 ist nicht mehr offen. Bitte zuerst scripts/reset-demo.ps1 ausfuehren. Antwort: $($openRequest.Body)"
}

$theory = Invoke-SmokeRequest -Method "POST" -Path "/theorie/buchen" -Token $studentToken -Body (@{
    thema = "Praesentations-Smoke Theorie"
    termin = "2026-11-20"
    dauerMinuten = 60
    dozent = "Elias Schulz"
} | ConvertTo-Json -Compress)
Assert-Status $theory 201 "1. erfolgreiche Theoriebuchung"
$theoryList = Invoke-SmokeRequest -Method "GET" -Path "/theorie/me" -Token $studentToken
Assert-Status $theoryList 200 "1. Abruf der Theoriebuchung"
Assert-BodyContains $theoryList "Praesentations-Smoke Theorie" "1. Abruf der Theoriebuchung"
Write-Host "[OK] 1. Theoriebuchung fuer SC901 erstellt und wieder abgerufen (HTTP 201/200)."

$practiceBody = @{
    flugzeugId = "FZ002"
    fluglehrer = "P001"
    termin = "2026-11-21T08:00"
    dauerMinuten = 60
    ausbildungsinhalt = "Praesentations-Smoke Praxis"
    startFlughafen = "EDDV"
    zielFlughafen = "EDDV"
} | ConvertTo-Json -Compress
$practice = Invoke-SmokeRequest -Method "POST" -Path "/praxis/buchen" -Token $studentToken -Body $practiceBody
Assert-Status $practice 201 "2. erfolgreiche Praxisbuchung"
Assert-BodyContains $practice '"flugzeugId":"FZ002"' "2. erfolgreiche Praxisbuchung"
Assert-BodyContains $practice '"startFlughafen":"EDDV"' "2. erfolgreiche Praxisbuchung"
Assert-BodyContains $practice '"zielFlughafen":"EDDV"' "2. erfolgreiche Praxisbuchung"
Write-Host "[OK] 2. Praxisbuchung mit P001, FZ002 und EDDV/EDDV erstellt (HTTP 201)."

$blockedPractice = Invoke-SmokeRequest -Method "POST" -Path "/praxis/buchen" -Token $studentToken -Body (@{
    flugzeugId = "FZ001"
    fluglehrer = "P001"
    termin = "2026-11-21T10:00"
    dauerMinuten = 60
    ausbildungsinhalt = "Negativtest Wartung"
    startFlughafen = "EDDV"
    zielFlughafen = "EDDV"
} | ConvertTo-Json -Compress)
Assert-Status $blockedPractice 409 "3. blockiertes Flugzeug FZ001"
Assert-BodyContains $blockedPractice "FZ001" "3. blockiertes Flugzeug FZ001"
if ($blockedPractice.Body -notmatch "wartung|nicht buchbar|nicht verfuegbar") {
    throw "3. FZ001-Fehlermeldung erklaert Wartungs- oder Verfuegbarkeitsstatus nicht: $($blockedPractice.Body)"
}
Write-Host "[OK] 3. FZ001 fachlich blockiert; verstaendliche Meldung vorhanden (HTTP 409)."

$exam = Invoke-SmokeRequest -Method "POST" -Path "/pruefung/theorie/anmelden" -Token $studentToken -Body (@{
    pruefungsart = "Theoriepruefung"
    wunschtermin = "2026-11-25"
    pruefer = "P001"
} | ConvertTo-Json -Compress)
Assert-Status $exam 409 "4. Pruefungsanmeldung ohne Mindeststunden"
Assert-BodyContains $exam "mindestens 10.0 Theoriestunden erforderlich" "4. Pruefungsanmeldung ohne Mindeststunden"
Write-Host "[OK] 4. Pruefungsanmeldung von SC901 wegen fehlender Mindeststunden blockiert (HTTP 409)."

$forbidden = Invoke-SmokeRequest -Method "POST" -Path "/theorie/buchen" -Token $managementToken -Body (@{
    schuelerId = "SC901"
    thema = "Nicht erlaubt"
    termin = "2026-11-20"
    dauerMinuten = 60
    dozent = "Elias Schulz"
} | ConvertTo-Json -Compress)
Assert-Status $forbidden 403 "5. unerlaubter Verwaltungszugriff"
Assert-BodyContains $forbidden "Keine Berechtigung " "5. unerlaubter Verwaltungszugriff"
Assert-BodyContains $forbidden "diese Funktion." "5. unerlaubter Verwaltungszugriff"
Write-Host "[OK] 5. Schuelerverwaltung bei Schueleraktion blockiert (HTTP 403)."

Assert-BodyContains $openRequest '"schuelerId":"SC906"' "6. Abschlussanfrage AA906"
Assert-BodyContains $openRequest '"theorieKriterienErfuellt":true' "6. Abschlussanfrage AA906"
Assert-BodyContains $openRequest '"praxisKriterienErfuellt":true' "6. Abschlussanfrage AA906"
$confirmed = Invoke-SmokeRequest -Method "POST" -Path "/verwaltung/abschlussanfragen/AA906/bestaetigen" -Token $managementToken -Body "{}"
Assert-Status $confirmed 200 "6. erfolgreiche Abschlussbestaetigung"
Assert-BodyContains $confirmed '"status":"ABGESCHLOSSEN"' "6. erfolgreiche Abschlussbestaetigung"
$completionStatus = Invoke-SmokeRequest -Method "GET" -Path "/status/SC906/gesamt" -Token $managementToken
Assert-Status $completionStatus 200 "6. Gesamtstatus SC906"
Assert-BodyContains $completionStatus '"status":"ABGESCHLOSSEN"' "6. Gesamtstatus SC906"
Write-Host "[OK] 6. AA906 bestaetigt; SC906 ist ABGESCHLOSSEN (HTTP 200)."

Write-Host "Alle sechs Praesentations-Smoke-Faelle waren erfolgreich."
