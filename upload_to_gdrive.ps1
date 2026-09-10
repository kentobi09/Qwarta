# ==============================================================================
# Ledger / IOU - Google Drive APK Upload Script
# ==============================================================================
[CmdletBinding()]
param (
    [string]$TargetFolder = "",
    [switch]$Rebuild = $false,
    [string]$ApkName = "Ledger.apk"
)

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "=================================================" -ForegroundColor DarkGray
Write-Host "  LEDGER / IOU - Google Drive Uploader           " -ForegroundColor Cyan
Write-Host "=================================================" -ForegroundColor DarkGray

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$ApkSourcePath = Join-Path $ScriptDir "app\build\outputs\apk\debug\app-debug.apk"

# Check if rebuild requested or APK does not exist
if ($Rebuild -or -not (Test-Path $ApkSourcePath)) {
    Write-Host "[*] Building latest debug APK..." -ForegroundColor Yellow
    Push-Location $ScriptDir
    try {
        & .\gradlew.bat assembleDebug
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Gradle build failed with exit code $LASTEXITCODE"
            exit 1
        }
    } finally {
        Pop-Location
    }
}

if (-not (Test-Path $ApkSourcePath)) {
    Write-Error "Could not find APK at: $ApkSourcePath. Run .\gradlew.bat assembleDebug first."
    exit 1
}

$ApkFile = Get-Item $ApkSourcePath
$ApkSizeMB = [math]::Round($ApkFile.Length / 1MB, 2)
Write-Host "[OK] Source APK located: $ApkSourcePath ($ApkSizeMB MB)" -ForegroundColor Green

# Resolve Google Drive target destination
$PossibleGdrivePaths = @(
    $TargetFolder,
    "G:\My Drive\my apks",
    "G:\My Drive\APKs",
    "G:\My Drive"
)

$ResolvedTargetDir = $null
foreach ($p in $PossibleGdrivePaths) {
    if (![string]::IsNullOrWhiteSpace($p) -and (Test-Path $p)) {
        $ResolvedTargetDir = $p
        break
    }
}

# If still not found, search dynamically for any drive with Google Drive
if ($null -eq $ResolvedTargetDir) {
    $Drives = Get-PSDrive -PSProvider FileSystem
    foreach ($d in $Drives) {
        $cand = Join-Path "$($d.Root)" "My Drive\my apks"
        if (Test-Path $cand) {
            $ResolvedTargetDir = $cand
            break
        }
        $rootCand = Join-Path "$($d.Root)" "My Drive"
        if (Test-Path $rootCand) {
            $ResolvedTargetDir = $rootCand
            break
        }
    }
}

if ($null -eq $ResolvedTargetDir) {
    Write-Host "[!] Google Drive 'My Drive' folder not found automatically." -ForegroundColor Red
    Write-Host "Please ensure Google Drive for Desktop is running and mounted." -ForegroundColor Yellow
    exit 1
}

Write-Host "[*] Target Google Drive folder: $ResolvedTargetDir" -ForegroundColor Cyan

# Destination file paths (Main APK + Latest alias)
$DestinationFile = Join-Path $ResolvedTargetDir $ApkName
$DestinationLatest = Join-Path $ResolvedTargetDir "Ledger_latest.apk"

Write-Host "[*] Copying APK to Google Drive..." -ForegroundColor Yellow
Copy-Item -Path $ApkSourcePath -Destination $DestinationFile -Force
Copy-Item -Path $ApkSourcePath -Destination $DestinationLatest -Force

# Verify
if ((Test-Path $DestinationFile) -and (Test-Path $DestinationLatest)) {
    $nowStr = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    Write-Host ""
    Write-Host "=================================================" -ForegroundColor Green
    Write-Host "  UPLOAD SUCCESSFUL!                             " -ForegroundColor Green
    Write-Host "=================================================" -ForegroundColor Green
    Write-Host "  File:   $DestinationFile" -ForegroundColor White
    Write-Host "  Alias:  $DestinationLatest" -ForegroundColor White
    Write-Host "  Size:   $ApkSizeMB MB" -ForegroundColor White
    Write-Host "  Time:   $nowStr" -ForegroundColor Gray
    Write-Host "=================================================" -ForegroundColor Green
    Write-Host "[i] Google Drive for Desktop will sync this file automatically." -ForegroundColor DarkGray
    Write-Host ""
} else {
    Write-Error "Failed to verify copied file in Google Drive."
    exit 1
}
