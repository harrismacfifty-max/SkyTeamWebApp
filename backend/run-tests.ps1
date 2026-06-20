param()

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$mainClasses = Join-Path $root "target/classes"
$testClasses = Join-Path $root "target/test-classes"
$mainSources = Get-ChildItem -Path (Join-Path $root "src/main/java") -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
$testSources = Get-ChildItem -Path (Join-Path $root "src/test/java") -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }

if (-not $mainSources) {
    throw "No Java main sources found."
}
if (-not $testSources) {
    throw "No Java test sources found."
}

New-Item -ItemType Directory -Force -Path $mainClasses | Out-Null
New-Item -ItemType Directory -Force -Path $testClasses | Out-Null

javac -encoding UTF-8 -d $mainClasses $mainSources
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

javac -encoding UTF-8 -cp $mainClasses -d $testClasses $testSources
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$classpath = "$mainClasses$([System.IO.Path]::PathSeparator)$testClasses"
java -cp $classpath de.skyteam.flightschool.service.BackendTestRunner
exit $LASTEXITCODE
