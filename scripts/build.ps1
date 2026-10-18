<#
.SYNOPSIS
    Builds the School Management System application.
.DESCRIPTION
    Compiles all Java source files and tests, copies resources (forms, images, html),
    and packages the executable JAR file.
#>

[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host " School Management System - Automated Build" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

# 1. Verify Java compiler
try {
    $javacVersion = javac -version 2>&1
    Write-Host "[BUILD] Detected Java compiler: $javacVersion" -ForegroundColor Green
} catch {
    Write-Error "[BUILD] ERROR: 'javac' is not found in PATH. Please install OpenJDK 11 or configure PATH."
    exit 1
}

# 2. Setup build directories
$rootDir = (Get-Item $PSScriptRoot).Parent.FullName
$buildDir = Join-Path $rootDir "build\classes"
$distDir = Join-Path $rootDir "dist"
$distLibDir = Join-Path $distDir "lib"
$jarFile = Join-Path $distDir "Students_Database.jar"
$manifestFile = Join-Path $rootDir "manifest.mf"

if (-not (Test-Path $buildDir)) {
    New-Item -ItemType Directory -Path $buildDir -Force | Out-Null
}

$classpath = "$distLibDir\*;$rootDir\src"

Write-Host "[BUILD] Compiling Java source files and test suite..." -ForegroundColor Yellow
$javaSources = @(Get-ChildItem -Path "$rootDir\src\*.java" -File) + @(Get-ChildItem -Path "$rootDir\test\*.java" -File)

& javac -encoding UTF-8 -cp $classpath -d $buildDir ($javaSources | ForEach-Object { $_.FullName })
if ($LASTEXITCODE -ne 0) {
    Write-Error "[BUILD] Compilation failed with exit code $LASTEXITCODE."
    exit $LASTEXITCODE
}
Write-Host "[BUILD] Compilation completed successfully." -ForegroundColor Green

# 3. Copy resources from src to build/classes (NetBeans form files, images, html)
Write-Host "[BUILD] Copying resources to build output directory..." -ForegroundColor Yellow
Get-ChildItem -Path "$rootDir\src" -Recurse -File | Where-Object { $_.Extension -ne ".java" } | ForEach-Object {
    $relativePath = $_.FullName.Substring((Join-Path $rootDir "src").Length + 1)
    $targetPath = Join-Path $buildDir $relativePath
    $targetFolder = Split-Path $targetPath -Parent
    if (-not (Test-Path $targetFolder)) {
        New-Item -ItemType Directory -Path $targetFolder -Force | Out-Null
    }
    Copy-Item -Path $_.FullName -Destination $targetPath -Force
}
Write-Host "[BUILD] Resources copied successfully." -ForegroundColor Green

# 4. Package executable JAR (Mandatory release deliverable)
Write-Host "[BUILD] Packaging executable JAR: $jarFile..." -ForegroundColor Yellow

# Remove pre-existing JAR artifact to guarantee a stale file cannot be mistaken for a successful build
if (Test-Path $jarFile) {
    Remove-Item -Path $jarFile -Force -ErrorAction Stop
}

$jarSuccess = $false
try {
    & jar cvfm $jarFile $manifestFile -C $buildDir . | Out-Null
    if ($LASTEXITCODE -eq 0 -and (Test-Path $jarFile) -and ((Get-Item $jarFile).Length -gt 0)) {
        $jarSuccess = $true
        $jarSize = (Get-Item $jarFile).Length
        Write-Host "[BUILD] JAR packaged successfully ($([Math]::Round($jarSize / 1KB, 2)) KB)." -ForegroundColor Green
    }
} catch {
    Write-Host "[BUILD] Error: Invocation of 'jar' command failed: $_" -ForegroundColor Red
}

if (-not $jarSuccess) {
    Write-Host "==================================================" -ForegroundColor Red
    Write-Host " BUILD FAILED - JAR PACKAGING INCOMPLETE" -ForegroundColor Red
    Write-Host "==================================================" -ForegroundColor Red
    Write-Error "[BUILD] Packaging of '$jarFile' failed. Build cannot succeed without required deliverable."
    exit 1
}

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host " BUILD SUCCESSFUL" -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Cyan
exit 0
