<#
.SYNOPSIS
    Launches the School Management System application.
.DESCRIPTION
    Runs Login_frame with the compiled classes and bundled JAR dependencies.
#>

[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

$rootDir = (Get-Item $PSScriptRoot).Parent.FullName
$buildDir = Join-Path $rootDir "build\classes"
$distLibDir = Join-Path $rootDir "dist\lib"
$jarFile = Join-Path $rootDir "dist\Students_Database.jar"

if (-not (Test-Path $buildDir)) {
    Write-Host "[RUN] Build output missing. Building project first..." -ForegroundColor Yellow
    & "$PSScriptRoot\build.ps1"
    if ($LASTEXITCODE -ne 0) {
        Write-Error "[RUN] Build failed."
        exit $LASTEXITCODE
    }
}

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host " Launching School Management System..." -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

$runClasspath = "$distLibDir\*;$buildDir"

& java -cp $runClasspath Login_frame
