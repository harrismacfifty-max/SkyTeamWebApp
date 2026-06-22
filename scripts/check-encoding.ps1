$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptRoot

$extensions = @(
    ".bat",
    ".cmd",
    ".css",
    ".html",
    ".java",
    ".js",
    ".json",
    ".md",
    ".php",
    ".properties",
    ".ps1",
    ".sh",
    ".sql",
    ".txt",
    ".xml",
    ".yaml",
    ".yml"
)

$patterns = @(
    @{ Name = "mojibake marker U+00C3"; Value = [string][char]0x00C3 },
    @{ Name = "mojibake marker U+00C2"; Value = [string][char]0x00C2 },
    @{ Name = "mojibake marker U+00E2"; Value = [string][char]0x00E2 },
    @{ Name = "replacement character U+FFFD"; Value = [string][char]0xFFFD }
)

$findings = New-Object System.Collections.Generic.List[object]

Get-ChildItem -Path $projectRoot -Recurse -File |
    Where-Object {
        $relative = $_.FullName.Substring($projectRoot.Length + 1)
        $extension = $_.Extension.ToLowerInvariant()
        $extensions -contains $extension -and
            $relative -notmatch '(^|[\\/])\.git([\\/]|$)' -and
            $relative -notmatch '(^|[\\/])target([\\/]|$)'
    } |
    ForEach-Object {
        $relative = $_.FullName.Substring($projectRoot.Length + 1)
        $text = [System.IO.File]::ReadAllText($_.FullName, [System.Text.Encoding]::UTF8)
        $lines = $text -split "`r?`n"

        for ($lineIndex = 0; $lineIndex -lt $lines.Count; $lineIndex++) {
            foreach ($pattern in $patterns) {
                if ($lines[$lineIndex].Contains($pattern.Value)) {
                    $findings.Add([pscustomobject]@{
                        File = $relative
                        Line = $lineIndex + 1
                        Pattern = $pattern.Name
                    }) | Out-Null
                }
            }
        }
    }

if ($findings.Count -gt 0) {
    $findings | Format-Table -AutoSize
    Write-Error "Encoding check failed: suspicious mojibake markers found. Save text files as UTF-8 and repair the listed lines."
    exit 1
}

Write-Host "Encoding check passed."
