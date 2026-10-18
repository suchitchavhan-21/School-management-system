<#
.SYNOPSIS
    Runs the automated test suite for the School Management System.
.DESCRIPTION
    Executes all 170+ unit, integration, and security tests across GradeCalculatorTest,
    ExamMarksDAOTest, AttendanceDAOTest, and SecurityAndResilienceTest using an isolated SQLite database.
#>

[CmdletBinding()]
param(
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$rootDir = (Get-Item $PSScriptRoot).Parent.FullName
$buildDir = Join-Path $rootDir "build\classes"
$distLibDir = Join-Path $rootDir "dist\lib"

# 1. Build if build directory is absent or not skipped
if (-not $SkipBuild -or -not (Test-Path $buildDir)) {
    Write-Host "[TEST] Triggering automated build before test execution..." -ForegroundColor Cyan
    & "$PSScriptRoot\build.ps1"
    if ($LASTEXITCODE -ne 0) {
        Write-Error "[TEST] Build step failed. Cannot execute tests."
        exit $LASTEXITCODE
    }
}

# 2. Run Master Test Suite
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host " Executing SystemTestSuite (Zero-Trust Harness)..." -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

$testClasspath = "$distLibDir\*;$buildDir"

& java -cp $testClasspath SystemTestSuite
$testExit = $LASTEXITCODE

if ($testExit -eq 0) {
    Write-Host "`n[TEST] ALL TESTS PASSED SUCCESSFULLY (Exit Code: 0)." -ForegroundColor Green
    exit 0
} else {
    Write-Error "`n[TEST] TEST SUITE FAILED WITH EXIT CODE: $testExit"
    exit $testExit
}
