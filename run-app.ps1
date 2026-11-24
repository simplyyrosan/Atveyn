# Atveyn One-Click App Runner
param(
    [switch]$NoBuild
)

$ErrorActionPreference = "Stop"

$JAVA_HOME = "C:\Users\rosan\tools\jdk-17.0.12+7"
$ANDROID_HOME = "C:\Users\rosan\android-sdk"
$env:JAVA_HOME = $JAVA_HOME
$env:ANDROID_HOME = $ANDROID_HOME
$env:PATH = "$JAVA_HOME\bin;$ANDROID_HOME\platform-tools;$ANDROID_HOME\emulator;$env:PATH"

Write-Host "=== Atveyn Android Runner ===" -ForegroundColor Cyan

# 1. Check connected devices
$devices = & adb devices | Where-Object { $_ -match "\bdevice\b" -and $_ -notmatch "List of devices" }

if (-not $devices) {
    Write-Host "No connected Android device or running emulator detected." -ForegroundColor Yellow
    Write-Host "Checking for available Virtual Devices (AVDs)..." -ForegroundColor Yellow

    $avds = & emulator -list-avds
    if ($avds) {
        $avdName = $avds[0]
        Write-Host "Starting emulator: $avdName..." -ForegroundColor Green
        Start-Process emulator -ArgumentList "-avd $avdName -no-snapshot-load"
        Write-Host "Waiting for device to boot..." -ForegroundColor Cyan
        & adb wait-for-device
        
        while ($true) {
            $boot = & adb shell getprop sys.boot_completed
            if ($boot -eq "1") { break }
            Start-Sleep -Seconds 2
        }
        Write-Host "Emulator booted successfully!" -ForegroundColor Green
    } else {
        Write-Host "No AVD found. To run Atveyn:" -ForegroundColor Yellow
        Write-Host "1. Connect any physical Android device via USB with USB Debugging enabled, OR" -ForegroundColor White
        Write-Host "2. Open Android Studio and launch a virtual device." -ForegroundColor White
        Write-Host "`nAPK is ready at: Atveyn.apk" -ForegroundColor Green
        exit 0
    }
}

# 2. Build & Install APK
$apkPath = "app\build\outputs\apk\debug\app-debug.apk"
if (-not $NoBuild -or -not (Test-Path $apkPath)) {
    Write-Host "Building debug APK..." -ForegroundColor Cyan
    .\gradlew assembleDebug
    Copy-Item $apkPath "Atveyn.apk" -Force
}

Write-Host "Installing Atveyn APK on device..." -ForegroundColor Cyan
& adb install -r $apkPath

# 3. Launch MainActivity
Write-Host "Launching Atveyn..." -ForegroundColor Green
& adb shell am start -n com.atveyn.app/.MainActivity

Write-Host "`nAtveyn is now running on your device!" -ForegroundColor Green
