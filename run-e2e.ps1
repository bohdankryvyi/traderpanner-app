# run-e2e.ps1 — Start Postgres, backend, frontend; run Playwright E2E; then cleanup.
# Run from repo root. Requires PowerShell 5+ (Windows).
# Usage: powershell -ExecutionPolicy Bypass -File .\run-e2e.ps1

$ErrorActionPreference = "Stop"
$RepoRoot = $PSScriptRoot
$LogsDir = Join-Path $RepoRoot "logs"
$BackendLog = Join-Path $LogsDir "backend.log"
$BackendErrLog = Join-Path $LogsDir "backend-err.log"
$FrontendLog = Join-Path $LogsDir "frontend.log"
$FrontendErrLog = Join-Path $LogsDir "frontend-err.log"
$BackendPidFile = Join-Path $LogsDir "backend.pid"
$FrontendPidFile = Join-Path $LogsDir "frontend.pid"

$BackendUrl = "http://localhost:8080"
$FrontendUrl = "http://localhost:5173"
$HealthUrl = "$BackendUrl/actuator/health"
$TestExitCode = 1

function Ensure-LogsDir {
    if (-not (Test-Path $LogsDir)) {
        New-Item -ItemType Directory -Path $LogsDir -Force | Out-Null
        Write-Host "Created $LogsDir"
    }
}

function Start-Postgres {
    Write-Host "Starting Postgres (docker compose)..."
    Push-Location $RepoRoot
    try {
        & docker compose up -d 2>&1 | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "docker compose up -d failed" }
    } finally {
        Pop-Location
    }
}

function Wait-Postgres {
    $maxAttempts = 30
    $attempt = 0
    while ($attempt -lt $maxAttempts) {
        try {
            Push-Location $RepoRoot
            $ps = & docker compose ps 2>$null
            Pop-Location
            if ($ps -match "healthy" -or $ps -match "Up") {
                Write-Host "Postgres is up."
                return
            }
        } catch { Pop-Location -ErrorAction SilentlyContinue }
        try {
            $tcp = New-Object System.Net.Sockets.TcpClient
            $tcp.Connect("localhost", 5432)
            $tcp.Close()
            Write-Host "Postgres port 5432 is accepting connections."
            return
        } catch { }
        $attempt++
        Write-Host "Waiting for Postgres... ($attempt/$maxAttempts)"
        Start-Sleep -Seconds 2
    }
    throw "Postgres did not become ready in time."
}

function Start-Backend {
    Write-Host "Starting backend..."
    $env:JAVA_TOOL_OPTIONS = "-Duser.timezone=UTC"
    $backendDir = Join-Path $RepoRoot "app-backend"
    $p = Start-Process -FilePath (Join-Path $backendDir "mvnw.cmd") -ArgumentList "-DskipTests","spring-boot:run" -WorkingDirectory $backendDir -PassThru -RedirectStandardOutput $BackendLog -RedirectStandardError $BackendErrLog -NoNewWindow
    $p.Id | Set-Content -Path $BackendPidFile
    Write-Host "Backend started (PID $($p.Id)), logging to $BackendLog and $BackendErrLog"
}

function Wait-Backend {
    $maxAttempts = 60
    $attempt = 0
    while ($attempt -lt $maxAttempts) {
        try {
            $r = Invoke-WebRequest -Uri $HealthUrl -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
            if ($r.StatusCode -eq 200) {
                $json = $r.Content | ConvertFrom-Json
                if ($json.status -eq "UP") {
                    Write-Host "Backend is UP."
                    return
                }
            }
        } catch { }
        $attempt++
        Write-Host "Waiting for backend... ($attempt/$maxAttempts)"
        Start-Sleep -Seconds 2
    }
    throw "Backend did not become ready in time."
}

function Start-Frontend {
    Write-Host "Starting frontend..."
    $frontendDir = Join-Path $RepoRoot "app-frontend"
    if (-not (Test-Path (Join-Path $frontendDir "node_modules"))) {
        Write-Host "Installing frontend dependencies (npm install)..."
        Push-Location $frontendDir
        try { & npm install } finally { Pop-Location }
    }
    $p = Start-Process -FilePath "cmd.exe" -ArgumentList "/c","npm run dev" -WorkingDirectory $frontendDir -PassThru -RedirectStandardOutput $FrontendLog -RedirectStandardError $FrontendErrLog -NoNewWindow
    $p.Id | Set-Content -Path $FrontendPidFile
    Write-Host "Frontend started (PID $($p.Id)), logging to $FrontendLog and $FrontendErrLog"
}

function Wait-Frontend {
    $maxAttempts = 30
    $attempt = 0
    while ($attempt -lt $maxAttempts) {
        try {
            $r = Invoke-WebRequest -Uri $FrontendUrl -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
            if ($r.StatusCode -eq 200) {
                Write-Host "Frontend is ready."
                return
            }
        } catch { }
        $attempt++
        Write-Host "Waiting for frontend... ($attempt/$maxAttempts)"
        Start-Sleep -Seconds 2
    }
    throw "Frontend did not become ready in time."
}

function Run-E2E {
    $e2eDir = Join-Path $RepoRoot "e2e"
    Push-Location $e2eDir
    try {
        if (-not (Test-Path "node_modules")) {
            Write-Host "Installing e2e dependencies (npm install)..."
            & npm install
            if ($LASTEXITCODE -ne 0) { throw "npm install failed in e2e" }
        }
        Write-Host "Ensuring Playwright browsers (npx playwright install)..."
        & npx playwright install
        if ($LASTEXITCODE -ne 0) { throw "npx playwright install failed" }
        Write-Host "Running E2E tests..."
        & npm run test
        $script:TestExitCode = $LASTEXITCODE
    } finally {
        Pop-Location
    }
}

function Stop-ProcessTree {
    param([int]$ProcessId)
    # On Windows, stored PIDs are wrappers (mvnw.cmd/cmd.exe). taskkill /T kills the process tree so Java/Node children are terminated.
    & taskkill /T /F /PID $ProcessId 2>$null
}

function Cleanup {
    Write-Host "Cleaning up..."
    if (Test-Path $FrontendPidFile) {
        $procId = Get-Content $FrontendPidFile -ErrorAction SilentlyContinue
        if ($procId -match "^\d+$") {
            Stop-ProcessTree -ProcessId ([int]$procId)
            Write-Host "Stopped frontend (PID $procId and child processes)"
        }
        Remove-Item $FrontendPidFile -Force -ErrorAction SilentlyContinue
    }
    if (Test-Path $BackendPidFile) {
        $procId = Get-Content $BackendPidFile -ErrorAction SilentlyContinue
        if ($procId -match "^\d+$") {
            Stop-ProcessTree -ProcessId ([int]$procId)
            Write-Host "Stopped backend (PID $procId and child processes)"
        }
        Remove-Item $BackendPidFile -Force -ErrorAction SilentlyContinue
    }
    Push-Location $RepoRoot
    try {
        & docker compose down 2>&1 | Out-Null
        Write-Host "Docker compose down done."
    } finally {
        Pop-Location
    }
}

# --- Main ---
try {
    Ensure-LogsDir
    Start-Postgres
    Wait-Postgres
    Start-Backend
    Wait-Backend
    Start-Frontend
    Wait-Frontend
    Run-E2E
} catch {
    Write-Host "Error: $_"
} finally {
    Cleanup
}
exit $TestExitCode
