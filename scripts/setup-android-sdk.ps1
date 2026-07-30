#Requires -Version 5.1
<#
  Install / refresh SDK packages for AiTrainer (compileSdk 34).

  Finds sdkmanager under D:\Android\Sdk or default Studio SDK path.
  Run:
    powershell -ExecutionPolicy Bypass -File scripts\setup-android-sdk.ps1
#>
$ErrorActionPreference = "Stop"
$SdkRoot = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "D:\Android\Sdk" }
$ProjectRoot = Split-Path $PSScriptRoot -Parent
$StudioJbr = "${env:ProgramFiles}\Android\Android Studio\jbr"
$AdoptiumJdk = Get-ChildItem "${env:ProgramFiles}\Eclipse Adoptium\jdk-*" -ErrorAction SilentlyContinue |
    Sort-Object Name -Descending | Select-Object -First 1

function Get-SdkManagerPath {
    param([string]$Root)
    $candidates = @(
        (Join-Path $Root "cmdline-tools\latest\bin\sdkmanager.bat"),
        (Join-Path $Root "cmdline-tools\bin\sdkmanager.bat")
    )
    foreach ($c in $candidates) {
        if (Test-Path $c) { return $c }
    }
    $studioDefault = Join-Path $env:LOCALAPPDATA "Android\Sdk"
    foreach ($c in @(
        (Join-Path $studioDefault "cmdline-tools\latest\bin\sdkmanager.bat"),
        (Join-Path $studioDefault "cmdline-tools\bin\sdkmanager.bat")
    )) {
        if (Test-Path $c) {
            if (-not (Test-Path (Join-Path $Root "cmdline-tools"))) {
                Write-Host "Using sdkmanager from Studio SDK; installing into $Root"
            }
            return $c
        }
    }
    return $null
}

New-Item -ItemType Directory -Path $SdkRoot -Force | Out-Null
$sdkmanager = Get-SdkManagerPath -Root $SdkRoot
if (-not $sdkmanager) {
    Write-Host "sdkmanager.bat not found."
    Write-Host "In Android Studio: Settings -> Android SDK -> SDK Tools ->"
    Write-Host "  enable 'Android SDK Command-line Tools (latest)', Apply."
    Write-Host "Or unzip commandlinetools-win into $SdkRoot\cmdline-tools\latest\"
    exit 1
}

Write-Host "sdkmanager: $sdkmanager"
Write-Host "SDK root:   $SdkRoot"

$javaHome = $null
if ($AdoptiumJdk) {
    $javaHome = $AdoptiumJdk.FullName
} elseif ($StudioJbr -and (Test-Path -LiteralPath $StudioJbr)) {
    $javaHome = $StudioJbr
}
if (-not $javaHome) {
    Write-Host "ERROR: Need JDK on disk (Eclipse Adoptium or Android Studio JBR) for sdkmanager."
    exit 1
}
Write-Host "JAVA_HOME -> $javaHome"
$env:JAVA_HOME = $javaHome
$env:ANDROID_HOME = $SdkRoot
$env:ANDROID_SDK_ROOT = $SdkRoot

Write-Host "Accepting SDK licenses (y)..."
$yes = 1..80 | ForEach-Object { "y" }
$yes | & $sdkmanager --sdk_root=$SdkRoot --licenses | Out-Null

Write-Host "Installing platform-tools, Android 34 platform, build-tools 34.0.0..."
& $sdkmanager --sdk_root=$SdkRoot `
    "platform-tools" `
    "platforms;android-34" `
    "build-tools;34.0.0"

$localProps = Join-Path $ProjectRoot "AiTrainer\local.properties"
$escaped = ($SdkRoot -replace '\\', '\\')
"sdk.dir=$escaped" | Set-Content -Path $localProps -Encoding ASCII

Write-Host "Wrote $localProps"
Write-Host "Build: cd AiTrainer; .\gradlew.bat assembleDebug"
