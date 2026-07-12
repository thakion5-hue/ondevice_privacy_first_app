<#
.SYNOPSIS
    Verifies the v0.6.6 label policy for the On-device Privacy First App.

.DESCRIPTION
    Uses ripgrep (rg) to enforce three rules across the repository:

      1. Legacy version strings (v6.5 / 0.6.5 / v0.6.5) must be fully removed
         from the cleanup scope.
      2. The current version string 0.6.6 must be present at least once.
      3. The compatibility label v6.6 must be present AND must only appear in
         the allowed files:
            - app/src/main/assets/model_manifest.json
            - app/src/main/java/com/genspark/privacyfirstai/ai/gemininano/provider/AiCoreTodoProvider.kt
            - app/src/main/java/com/genspark/privacyfirstai/ai/gemininano/sessionfactory/AiCoreTodoSessionFactory.kt

    Designed for both local developer runs and CI pipelines.

.PARAMETER ProjectRoot
    Path to the project root. Defaults to the current directory.

.PARAMETER CI
    Machine-parseable, quiet-friendly output for CI. Prints a compact result
    table at the end and uses distinct exit codes per failure class.

.PARAMETER Quiet
    Suppress per-hit output. Only summary lines are printed. Ignored when -CI
    is set (CI mode is already compact).

.EXAMPLE
    pwsh -File .\tools\rg_verify_v0_6_6.ps1

.EXAMPLE
    pwsh -File .\tools\rg_verify_v0_6_6.ps1 -CI

.NOTES
    Exit codes:
        0  All checks passed.
        2  Legacy version strings still present.
        3  Required version string 0.6.6 missing.
        4  Required compatibility label v6.6 missing.
        5  v6.6 used outside the allowed compatibility scope.
       10  Tooling error (e.g. ripgrep not installed).
#>

[CmdletBinding()]
param(
    [string]$ProjectRoot = ".",
    [switch]$CI,
    [switch]$Quiet
)

$ErrorActionPreference = "Stop"

# --- Exit code contract -----------------------------------------------------
$EXIT_OK               = 0
$EXIT_LEGACY_FOUND     = 2
$EXIT_MISSING_066      = 3
$EXIT_MISSING_V66      = 4
$EXIT_V66_OUT_OF_SCOPE = 5
$EXIT_TOOLING          = 10

# --- Output helpers ---------------------------------------------------------
function Write-Info($Message) {
    if ($CI) { Write-Host "::info::$Message" }
    else     { Write-Host "[INFO] $Message" -ForegroundColor Cyan }
}

function Write-Pass($Message) {
    if ($CI) { Write-Host "::pass::$Message" }
    else     { Write-Host "[PASS] $Message" -ForegroundColor Green }
}

function Write-FailLine($Message) {
    if ($CI) { Write-Host "::error::$Message" }
    else     { Write-Host "[FAIL] $Message" -ForegroundColor Red }
}

function Write-Hits($Title, $Hits) {
    if ($Quiet -or $CI) { return }
    if (-not $Hits) { return }
    Write-Host ""
    Write-Host "----- $Title -----" -ForegroundColor DarkGray
    Write-Host ($Hits -join "`n")
    Write-Host "-------------------" -ForegroundColor DarkGray
}

$script:Results = New-Object System.Collections.Generic.List[object]
function Add-Result([string]$Check, [string]$Status, [string]$Detail) {
    $script:Results.Add([pscustomobject]@{
        Check  = $Check
        Status = $Status
        Detail = $Detail
    }) | Out-Null
}

function Emit-Summary([int]$ExitCode) {
    if ($CI) {
        Write-Host ""
        Write-Host "=== rg_verify_v0_6_6 summary ==="
        foreach ($r in $script:Results) {
            Write-Host ("{0,-40} {1,-6} {2}" -f $r.Check, $r.Status, $r.Detail)
        }
        Write-Host ("exit_code={0}" -f $ExitCode)
        Write-Host "================================"
    }
    else {
        Write-Host ""
        Write-Host "=== Verification summary ===" -ForegroundColor Yellow
        $script:Results | Format-Table -AutoSize | Out-String | Write-Host
        if ($ExitCode -eq $EXIT_OK) {
            Write-Host "Result: PASS (exit $ExitCode)" -ForegroundColor Green
        } else {
            Write-Host ("Result: FAIL (exit {0})" -f $ExitCode) -ForegroundColor Red
        }
    }
}

function Exit-With([int]$Code, [string]$Reason) {
    if ($Reason) { Write-FailLine $Reason }
    Emit-Summary -ExitCode $Code
    exit $Code
}

# --- Preconditions ----------------------------------------------------------
$rg = Get-Command rg -ErrorAction SilentlyContinue
if (-not $rg) {
    Add-Result "ripgrep availability" "FAIL" "rg not found in PATH"
    Exit-With $EXIT_TOOLING "ripgrep (rg) was not found in PATH. Install from https://github.com/BurntSushi/ripgrep"
}
Add-Result "ripgrep availability" "PASS" $rg.Source

$root = Resolve-Path $ProjectRoot
Set-Location $root
Write-Info "Project root: $root"

# --- rg common args ---------------------------------------------------------
$commonArgs = @(
    "-n",
    "--hidden",
    "--glob", "!.git/",
    "--glob", "!**/.gradle/**",
    "--glob", "!**/build/**",
    "--glob", "!*.jar",
    "--glob", "!*.bin",
    "--glob", "!docs/release_notes_v0_6_6_short.md",
    "--glob", "!docs/INDEX.md",
    "--glob", "!README.md",
    "--glob", "!tools/rg_verify_v0_6_6.ps1",
    "--glob", "!tools/rg_verify_v0_6_6.sh"
)

function Invoke-Rg([string]$Pattern) {
    # rg exit: 0 = matches, 1 = no matches, >=2 = error.
    $out = & rg @commonArgs $Pattern .
    $code = $LASTEXITCODE
    return [pscustomobject]@{
        ExitCode = $code
        Lines    = if ($out) { @($out) } else { @() }
    }
}

# --- Check 1: no legacy version strings -------------------------------------
$legacyPattern = 'v6\.5|0\.6\.5|v0\.6\.5'
$legacy = Invoke-Rg $legacyPattern

if ($legacy.ExitCode -ge 2) {
    Add-Result "no-legacy-version-strings" "FAIL" "rg internal error (code $($legacy.ExitCode))"
    Exit-With $EXIT_TOOLING "ripgrep failed while scanning for legacy version strings."
}
if ($legacy.ExitCode -eq 0 -and $legacy.Lines.Count -gt 0) {
    Write-Hits "Legacy version strings" $legacy.Lines
    Add-Result "no-legacy-version-strings" "FAIL" ("{0} hit(s)" -f $legacy.Lines.Count)
    Exit-With $EXIT_LEGACY_FOUND "Legacy version strings (v6.5 / 0.6.5 / v0.6.5) are still present."
}
Write-Pass "No legacy version strings found."
Add-Result "no-legacy-version-strings" "PASS" "0 hits"

# --- Check 2: required 0.6.6 is present -------------------------------------
$version = Invoke-Rg '0\.6\.6'
if ($version.ExitCode -ge 2) {
    Add-Result "required-0.6.6-present" "FAIL" "rg internal error (code $($version.ExitCode))"
    Exit-With $EXIT_TOOLING "ripgrep failed while scanning for 0.6.6."
}
if ($version.ExitCode -ne 0 -or $version.Lines.Count -eq 0) {
    Add-Result "required-0.6.6-present" "FAIL" "0 hits"
    Exit-With $EXIT_MISSING_066 "Required version string 0.6.6 was not found anywhere in the repo."
}
Write-Hits "0.6.6 occurrences" $version.Lines
Write-Pass ("Required version string 0.6.6 is present ({0} hit(s))." -f $version.Lines.Count)
Add-Result "required-0.6.6-present" "PASS" ("{0} hits" -f $version.Lines.Count)

# --- Check 3: v6.6 exists AND is limited to allowed files -------------------
$v66 = Invoke-Rg 'v6\.6'
if ($v66.ExitCode -ge 2) {
    Add-Result "compat-v6.6-present" "FAIL" "rg internal error (code $($v66.ExitCode))"
    Exit-With $EXIT_TOOLING "ripgrep failed while scanning for v6.6."
}
if ($v66.ExitCode -ne 0 -or $v66.Lines.Count -eq 0) {
    Add-Result "compat-v6.6-present" "FAIL" "0 hits"
    Exit-With $EXIT_MISSING_V66 "Expected compatibility label v6.6 was not found."
}
Write-Hits "v6.6 occurrences" $v66.Lines
Add-Result "compat-v6.6-present" "PASS" ("{0} hits" -f $v66.Lines.Count)

$allowedPatterns = @(
    'app/src/main/assets/model_manifest\.json:',
    'app/src/main/java/com/genspark/privacyfirstai/ai/gemininano/provider/AiCoreTodoProvider\.kt:',
    'app/src/main/java/com/genspark/privacyfirstai/ai/gemininano/sessionfactory/AiCoreTodoSessionFactory\.kt:'
)
# Normalize backslashes (ripgrep on Windows may emit `\` separators).
$normalized = $v66.Lines | ForEach-Object { $_ -replace '\\', '/' }

$invalidV66 = $normalized | Where-Object {
    $line = $_
    -not ($allowedPatterns | Where-Object { $line -match $_ })
}

if ($invalidV66 -and $invalidV66.Count -gt 0) {
    Write-Hits "v6.6 outside allowed scope" $invalidV66
    Add-Result "compat-v6.6-scope" "FAIL" ("{0} out-of-scope hit(s)" -f $invalidV66.Count)
    Exit-With $EXIT_V66_OUT_OF_SCOPE "Unexpected v6.6 usage found outside the allowed compatibility scope."
}

Write-Pass "v6.6 usage is limited to the allowed compatibility scope."
Add-Result "compat-v6.6-scope" "PASS" "all hits inside allowed files"

# --- Success ----------------------------------------------------------------
Write-Info "Verification completed successfully."
Emit-Summary -ExitCode $EXIT_OK
exit $EXIT_OK
