#Requires -Version 5.1
<#
  Fix stuck Gradle wrapper download (timeout / .part file).
  Uses Tencent mirror (same as gradle-wrapper.properties).
#>
$ErrorActionPreference = "Stop"
$zipUrl = "https://mirrors.cloud.tencent.com/gradle/gradle-8.7-bin.zip"
$zipName = "gradle-8.7-bin.zip"
$gradleUserHome = if ($env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME } else { Join-Path $env:USERPROFILE ".gradle" }
$distsRoot = Join-Path $gradleUserHome "wrapper\dists\gradle-8.7-bin"

Write-Host "Cleaning incomplete Gradle 8.7 downloads under $distsRoot ..."
if (Test-Path $distsRoot) {
    Get-ChildItem $distsRoot -Recurse -Filter "*.lck" -ErrorAction SilentlyContinue | Remove-Item -Force
    Get-ChildItem $distsRoot -Recurse -Filter "*.part" -ErrorAction SilentlyContinue | Remove-Item -Force
}

Write-Host "Downloading $zipUrl (may take a few minutes)..."
$tmp = Join-Path $env:TEMP $zipName
Invoke-WebRequest -Uri $zipUrl -OutFile $tmp -UseBasicParsing

Write-Host "Run gradlew once so it creates the hash folder, or place zip manually."
Write-Host "Easiest: cd AiTrainer; .\gradlew.bat --version"
Write-Host ""
Write-Host "If gradlew still downloads, after it creates ...\gradle-8.7-bin\<hash>\ stop and copy:"
Write-Host "  Copy-Item '$tmp' '<hash-folder>\$zipName'"
Write-Host ""
Write-Host "Temp zip saved: $tmp"
