# stop-local.ps1 — Stop backend/frontend processes (by PID files) and docker compose.
# Run from repo root. Usage: powershell -ExecutionPolicy Bypass -File .\stop-local.ps1

$RepoRoot = $PSScriptRoot
$LogsDir = Join-Path $RepoRoot "logs"
$BackendPidFile = Join-Path $LogsDir "backend.pid"
$FrontendPidFile = Join-Path $LogsDir "frontend.pid"

if (Test-Path $FrontendPidFile) {
    $pid = Get-Content $FrontendPidFile -ErrorAction SilentlyContinue
    if ($pid -match "^\d+$") {
        Stop-Process -Id ([int]$pid) -Force -ErrorAction SilentlyContinue
        Write-Host "Stopped frontend (PID $pid)."
    }
    Remove-Item $FrontendPidFile -Force -ErrorAction SilentlyContinue
} else {
    Write-Host "No frontend PID file found."
}

if (Test-Path $BackendPidFile) {
    $pid = Get-Content $BackendPidFile -ErrorAction SilentlyContinue
    if ($pid -match "^\d+$") {
        Stop-Process -Id ([int]$pid) -Force -ErrorAction SilentlyContinue
        Write-Host "Stopped backend (PID $pid)."
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
