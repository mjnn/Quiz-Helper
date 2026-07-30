# Generate local release keystore + keystore.properties for assembleRelease.
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$aiTrainer = Join-Path $root "AiTrainer"
$keystore = Join-Path $aiTrainer "release.keystore"
$props = Join-Path $aiTrainer "keystore.properties"
$example = Join-Path $aiTrainer "keystore.properties.example"

$javaHome = $env:JAVA_HOME
if (-not $javaHome) {
    $javaHome = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
}
$keytool = Join-Path $javaHome "bin\keytool.exe"
if (-not (Test-Path $keytool)) {
    throw "keytool not found at $keytool — set JAVA_HOME to JDK 17+"
}

$alias = "aitrainer"
$password = "aitrainer123"

if (-not (Test-Path $keystore)) {
    Write-Host "Creating release keystore: $keystore"
    & $keytool -genkeypair -v -storetype PKCS12 -keystore $keystore `
        -alias $alias -keyalg RSA -keysize 2048 -validity 10000 `
        -storepass $password -keypass $password `
        -dname "CN=AiTrainer Practice, OU=Local, O=MaTeacher, L=Local, ST=Local, C=CN"
} else {
    Write-Host "Keystore already exists: $keystore"
}

if (-not (Test-Path $props)) {
    Copy-Item $example $props
    Write-Host "Created keystore.properties from example"
} else {
    Write-Host "keystore.properties already exists"
}

Write-Host ""
Write-Host "Done. Build release APK with:"
Write-Host "  cd AiTrainer"
Write-Host "  .\gradlew.bat assembleRelease"
