<#
.SYNOPSIS
    Comprehensive Zero-Trust Repository & Release Verification Script.
.DESCRIPTION
    Validates environment, configuration hygiene, secrets exclusion, compiles the project,
    executes all 170+ automated tests, and verifies repository structure.
#>

[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host " ZERO-TRUST REPOSITORY VERIFICATION & INTEGRITY CHECK" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan

$rootDir = (Get-Item $PSScriptRoot).Parent.FullName
$failures = @()

# Step 1: Check Environment
Write-Host "`n[CHECK 1/6] Verifying Java & Git Environment..." -ForegroundColor Yellow
try {
    $javaVer = java -version 2>&1
    $javacVer = javac -version 2>&1
    Write-Host "  [PASS] Java Runtime: $($javaVer[0])" -ForegroundColor Green
    Write-Host "  [PASS] Java Compiler: $javacVer" -ForegroundColor Green
} catch {
    $failures += "Java or javac is not available in PATH."
    Write-Host "  [FAIL] Java compiler check failed." -ForegroundColor Red
}

# Step 2: Check Git Status
Write-Host "`n[CHECK 2/6] Verifying Git Repository Status..." -ForegroundColor Yellow
try {
    $branch = git rev-parse --abbrev-ref HEAD 2>&1
    $commit = git rev-parse HEAD 2>&1
    Write-Host "  [PASS] Active branch: $branch" -ForegroundColor Green
    Write-Host "  [PASS] HEAD commit: $commit" -ForegroundColor Green
} catch {
    Write-Host "  [WARN] Git command returned error or not in Git worktree." -ForegroundColor Yellow
}

# Step 3: Secret Hygiene & File Safety
Write-Host "`n[CHECK 3/6] Inspecting Configuration & Secret Hygiene..." -ForegroundColor Yellow
$configTemplate = Join-Path $rootDir "config\database.properties.template"
if (Test-Path $configTemplate) {
    $content = Get-Content $configTemplate -Raw
    if ($content -match "db\.mysql\.password=\S+") {
        $failures += "Hardcoded password found in config/database.properties.template."
        Write-Host "  [FAIL] Password detected in template file!" -ForegroundColor Red
    } else {
        Write-Host "  [PASS] database.properties.template has empty passwords and safe instructions." -ForegroundColor Green
    }
} else {
    $failures += "config/database.properties.template is missing."
    Write-Host "  [FAIL] config/database.properties.template missing." -ForegroundColor Red
}

# Step 4: Verify Repository Directory Structure
Write-Host "`n[CHECK 4/6] Verifying Production-Grade Directory Structure..." -ForegroundColor Yellow
$requiredPaths = @(
    "docs\architecture\REPOSITORY_STRUCTURE.md",
    "docs\audit\BASELINE.md",
    "docs\audit\ZERO_TRUST_AUDIT_REGISTER.md",
    "docs\audit\FINAL_COMPLETION_REPORT.md",
    "docs\security\SECURITY_REMEDIATION.md",
    "docs\testing\TEST_RESULTS.md",
    "Database\schema\schema-sqlite.sql",
    "Database\schema\schema-mysql.sql",
    "Database\migrations\README.md",
    "config\database.properties.template",
    "config\README.md",
    "scripts\build.ps1",
    "scripts\test.ps1",
    "scripts\run.ps1",
    ".github\workflows\ci.yml",
    ".gitattributes",
    "CONTRIBUTING.md",
    "SECURITY.md",
    "CHANGELOG.md",
    "README.md"
)

foreach ($relPath in $requiredPaths) {
    $full = Join-Path $rootDir $relPath
    if (Test-Path $full) {
        Write-Host "  [PASS] $relPath exists." -ForegroundColor Green
    } else {
        $failures += "Missing expected repository path: $relPath"
        Write-Host "  [FAIL] Missing: $relPath" -ForegroundColor Red
    }
}

# Step 5: Execute Automated Build
Write-Host "`n[CHECK 5/6] Executing Clean Automated Build..." -ForegroundColor Yellow
& "$PSScriptRoot\build.ps1"
if ($LASTEXITCODE -ne 0) {
    $failures += "Build script failed with exit code $LASTEXITCODE."
}

# Step 6: Execute Comprehensive Test Suite
Write-Host "`n[CHECK 6/6] Executing Automated Test Suite..." -ForegroundColor Yellow
& "$PSScriptRoot\test.ps1" -SkipBuild
if ($LASTEXITCODE -ne 0) {
    $failures += "Test suite failed with exit code $LASTEXITCODE."
}

# Final Verdict
Write-Host "`n================================================================================" -ForegroundColor Cyan
if ($failures.Count -eq 0) {
    Write-Host " VERDICT: 100% GREEN - ALL ZERO-TRUST VERIFICATION CHECKS PASSED!" -ForegroundColor Green
    Write-Host "================================================================================" -ForegroundColor Cyan
    exit 0
} else {
    Write-Host " VERDICT: FAILED - $($failures.Count) issue(s) detected:" -ForegroundColor Red
    foreach ($f in $failures) {
        Write-Host "  - $f" -ForegroundColor Red
    }
    Write-Host "================================================================================" -ForegroundColor Cyan
    exit 1
}
