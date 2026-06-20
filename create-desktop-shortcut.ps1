param(
    [string]$ShortcutName = "SkyTeam Flugschule"
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$launcher = Join-Path $projectRoot "start-skyteam.cmd"
$icon = Join-Path $projectRoot "assets\skyteam-icon.ico"

if (-not (Test-Path $launcher)) {
    throw "Launcher nicht gefunden: $launcher"
}

$desktop = [Environment]::GetFolderPath("Desktop")
$shortcutPath = Join-Path $desktop "$ShortcutName.lnk"
$shell = New-Object -ComObject WScript.Shell
$shortcut = $shell.CreateShortcut($shortcutPath)
$shortcut.TargetPath = $launcher
$shortcut.Arguments = ""
$shortcut.WorkingDirectory = $projectRoot
if (Test-Path $icon) {
    $shortcut.IconLocation = $icon
} else {
    $shortcut.IconLocation = "$env:SystemRoot\System32\shell32.dll,220"
}
$shortcut.Description = "SkyTeam Flugschule starten"
$shortcut.Save()

Write-Host "Desktop-Shortcut erstellt: $shortcutPath"
