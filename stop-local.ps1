# stop-local.ps1 — Stop backend/frontend processes (by PID files) and docker compose.
# Run from repo root. Usage: powershell -ExecutionPolicy Bypass -File .\stop-local.ps1
# Uses taskkill /T so the process tree (e.g. Java/Node children of mvnw.cmd/cmd.exe) is terminated.

$RepoRoot = $PSScriptRoot
$LogsDir = Join-Path $RepoRoot "logs"
$BackendPidFile = Join-Path $LogsDir "backend.pid"
$FrontendPidFile = Join-Path $LogsDir "frontend.pid"

function Stop-ProcessTree {
    param([int]$ProcessId)
    & taskkill /T /F /PID $ProcessId 2>$null
}

if (Test-Path $FrontendPidFile) {
    $procId = Get-Content $FrontendPidFile -ErrorAction SilentlyContinue
    if ($procId -match "^\d+$") {
        Stop-ProcessTree -ProcessId ([int]$procId)
        Write-Host "Stopped frontend (PID $procId and child processes)."
    }
    Remove-Item $FrontendPidFile -Force -ErrorAction SilentlyContinue
} else {
    Write-Host "No frontend PID file found."
}

if (Test-Path $BackendPidFile) {
    $procId = Get-Content $BackendPidFile -ErrorAction SilentlyContinue
    if ($procId -match "^\d+$") {
        Stop-ProcessTree -ProcessId ([int]$procId)
        Write-Host "Stopped backend (PID $procId and child processes)."
    }
    Remove-Item $BackendPidFile -Force -ErrorAction SilentlyContinue
} else {
    Write-Host "No backend PID file found."
}

Push-Location $RepoRoot
try {
    & docker compose down 2>&1 | Out-Null
    Write-Host "Docker compose down done."
} finally {
    Pop-Location
}
