param(
    [ValidateSet('Quick', 'Named', 'None')]
    [string]$TunnelMode = 'Quick',
    [switch]$NoBrowser,
    [switch]$NoPause
)

$ErrorActionPreference = 'Stop'
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Split-Path -Parent $ScriptDir
$EnvFile = Join-Path $RootDir '.env'

function Test-DockerEngine {
    & docker info *> $null
    return $LASTEXITCODE -eq 0
}

function Start-DockerEngine {
    if (Test-DockerEngine) { return }
    Write-Host 'Starting Docker Desktop. Please wait...'
    & docker desktop start *> $null
    if ($LASTEXITCODE -ne 0) {
        $desktop = Join-Path $env:ProgramFiles 'Docker\Docker\Docker Desktop.exe'
        if (-not (Test-Path -LiteralPath $desktop)) { throw 'Cannot start Docker Desktop. Verify that it is installed and start it manually.' }
        Start-Process -FilePath $desktop
    }
    for ($i = 0; $i -lt 90; $i++) {
        Start-Sleep -Seconds 2
        if (Test-DockerEngine) { return }
    }
    throw 'Docker Engine was not ready within 3 minutes. Open Docker Desktop to inspect the error.'
}

function Get-EnvValue {
    param([string]$Name, [string]$DefaultValue)
    $line = Get-Content -LiteralPath $EnvFile -Encoding utf8 |
        Where-Object { $_ -match "^\s*$([regex]::Escape($Name))\s*=" } |
        Select-Object -Last 1
    if (-not $line) { return $DefaultValue }
    $value = ($line -split '=', 2)[1].Trim().Trim('"').Trim("'")
    if ([string]::IsNullOrWhiteSpace($value)) { return $DefaultValue }
    return $value
}

function Get-QuickTunnelUrl {
    for ($i = 0; $i -lt 45; $i++) {
        $logs = & docker compose --profile quick-tunnel logs quick-tunnel --no-color 2>&1 | Out-String
        $match = [regex]::Match($logs, 'https://[a-z0-9-]+\.trycloudflare\.com')
        if ($match.Success) { return $match.Value }
        Start-Sleep -Seconds 2
    }
    throw 'The app started, but no Quick Tunnel URL was available within 90 seconds. Check the cloudflared logs.'
}

try {
    Set-Location $RootDir
    if (-not (Test-Path -LiteralPath $EnvFile)) { throw 'Missing .env. Copy .env.example to .env and fill in all passwords and secrets.' }
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'The docker command was not found. Install Docker Desktop first.' }
    Start-DockerEngine
    & docker compose config --quiet
    if ($LASTEXITCODE -ne 0) { throw 'Compose validation failed. Check .env.' }

    $profileArgs = @()
    if ($TunnelMode -eq 'Quick') { $profileArgs = @('--profile', 'quick-tunnel') }
    if ($TunnelMode -eq 'Named') {
        if ([string]::IsNullOrWhiteSpace((Get-EnvValue 'CLOUDFLARE_TUNNEL_TOKEN' ''))) { throw 'Named Tunnel mode requires CLOUDFLARE_TUNNEL_TOKEN in .env.' }
        $profileArgs = @('--profile', 'named-tunnel')
    }

    Write-Host 'Starting the database, backend, frontend, and tunnel...'
    & docker compose @profileArgs up -d --wait
    if ($LASTEXITCODE -ne 0) { throw 'Application containers failed to start. Run docker compose ps for details.' }

    $port = Get-EnvValue 'GAOKAO_HTTP_PORT' '5173'
    $adminUrl = "http://localhost:$port/admin"
    $publicUrl = $null
    if ($TunnelMode -eq 'Quick') { $publicUrl = Get-QuickTunnelUrl }
    if (-not $NoBrowser) { Start-Process $adminUrl }

    Write-Host ''
    Write-Host "Local admin page: $adminUrl" -ForegroundColor Green
    if ($publicUrl) {
        Write-Host "Candidate public URL: $publicUrl" -ForegroundColor Cyan
        Write-Host 'This temporary URL changes when the tunnel is recreated.'
    } elseif ($TunnelMode -eq 'Named') {
        Write-Host 'Named Tunnel started. Use the hostname configured in Cloudflare.' -ForegroundColor Cyan
    } else {
        Write-Host 'No public tunnel was started.'
    }
    Write-Host ''
    Write-Host 'Closing this window does not stop the app. Use the Gaokao - Stop desktop shortcut.'
    if (-not $NoPause) { Read-Host 'Press Enter to close this window' }
} catch {
    Write-Host ''
    Write-Host "Start failed: $($_.Exception.Message)" -ForegroundColor Red
    if (-not $NoPause) { Read-Host 'Press Enter to close this window' }
    exit 1
} finally {
    Set-Location $RootDir
}
