#Requires -Version 5.1
$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path $PSScriptRoot -Parent
$SdkPreferred = "D:\Android\Sdk"
$StudioDefault = Join-Path $env:LOCALAPPDATA "Android\Sdk"
$StudioJbr = "${env:ProgramFiles}\Android\Android Studio\jbr"
$AdoptiumJdk = Get-ChildItem "${env:ProgramFiles}\Eclipse Adoptium\jdk-*" -ErrorAction SilentlyContinue |
    Sort-Object Name -Descending | Select-Object -First 1

function Test-SdkReady {
    param([string]$Root)
    Test-Path (Join-Path $Root "platforms\android-34")
}

function Pick-SdkRoot {
    if (Test-SdkReady $SdkPreferred) { return $SdkPreferred }
    if (Test-SdkReady $StudioDefault) { return $StudioDefault }
    if (Test-Path (Join-Path $SdkPreferred "platforms")) { return $SdkPreferred }
    if (Test-Path (Join-Path $StudioDefault "platforms")) { return $StudioDefault }
    return $SdkPreferred
}

$sdk = Pick-SdkRoot
$localProps = Join-Path $ProjectRoot "AiTrainer\local.properties"
$escaped = ($sdk -replace '\\', '\\')
"sdk.dir=$escaped" | Set-Content -Path $localProps -Encoding ASCII

Write-Host "local.properties -> sdk.dir=$sdk"

if (-not (Test-SdkReady $sdk)) {
    Write-Host ""
    Write-Host "Missing platforms\android-34 (project compileSdk 34)."
    Write-Host "Run: powershell -ExecutionPolicy Bypass -File scripts\setup-android-sdk.ps1"
    if (Test-Path (Join-Path $StudioDefault "platforms")) {
        Write-Host "Note: Studio SDK at $StudioDefault has:"
        Get-ChildItem (Join-Path $StudioDefault "platforms") | ForEach-Object { Write-Host "  - $($_.Name)" }
    }
    exit 2
}

[Environment]::SetEnvironmentVariable("ANDROID_HOME", $sdk, "User")
[Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", $sdk, "User")
$gradleJava = Get-ChildItem "${env:ProgramFiles}\Eclipse Adoptium\jdk-17*" -ErrorAction SilentlyContinue |
    Sort-Object Name -Descending | Select-Object -First 1
if (-not $gradleJava) {
    $gradleJava = $AdoptiumJdk
}
if ($gradleJava) {
    $home = if ($gradleJava.FullName) { $gradleJava.FullName } else { $gradleJava }
    [Environment]::SetEnvironmentVariable("JAVA_HOME", $home, "User")
    Write-Host "Set user JAVA_HOME -> $home (use JDK 17 for Gradle 8.7, not JDK 25)"
}
$userPath = [Environment]::GetEnvironmentVariable("Path", "User")
$pt = Join-Path $sdk "platform-tools"
if ((Test-Path $pt) -and $userPath -notlike "*$pt*") {
    [Environment]::SetEnvironmentVariable("Path", "$userPath;$pt", "User")
}
Write-Host "See AiTrainer/gradle.properties (org.gradle.java.home)"
Write-Host "Build: cd AiTrainer; .\gradlew.bat assembleDebug"
