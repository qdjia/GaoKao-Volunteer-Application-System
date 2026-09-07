param([switch]$KeepDockerDesktop)

$ErrorActionPreference = 'Stop'
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Split-Path -Parent $ScriptDir
$EnvFile = Join-Path $RootDir '.env'

function Test-DockerEngine {
    & docker info *> $null
    return $LASTEXITCODE -eq 0
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

function Read-AdminToken {
    param([string]$BaseUrl)
    $defaultUser = Get-EnvValue 'GAOKAO_ADMIN_USERNAME' 'admin'
    $username = Read-Host "Admin username [$defaultUser]"
    if ([string]::IsNullOrWhiteSpace($username)) { $username = $defaultUser }
    $securePassword = Read-Host 'Admin password' -AsSecureString
    $credential = [pscredential]::new($username, $securePassword)
    $password = $credential.GetNetworkCredential().Password
    try {
        $body = @{ username = $username; password = $password } | ConvertTo-Json
        $result = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" -ContentType 'application/json' -Body $body -TimeoutSec 15
        if ($result.code -ne 200 -or $result.data.role -ne 'ADMIN') { throw 'The supplied account is not an administrator.' }
        return $result.data.token
    } finally {
        $password = $null
        $credential = $null
        $securePassword.Dispose()
    }
}

try {
    Set-Location $RootDir
    if (-not (Get-Command docker -ErrorAction SilentlyContinue) -or -not (Test-DockerEngine)) {
        Write-Host 'The app is already stopped and Docker Engine is not running.'
        Read-Host 'Press Enter to close this window'
        exit 0
    }
    if (-not (Test-Path -LiteralPath $EnvFile)) { throw 'Missing .env; the current application configuration cannot be identified.' }

    $running = & docker compose ps --services --status running 2>$null
    if ($LASTEXITCODE -ne 0 -or -not $running) {
        Write-Host 'No running application containers were found.'
        if (-not $KeepDockerDesktop) { & docker desktop stop }
        Read-Host 'Press Enter to close this window'
        exit 0
    }

    $port = Get-EnvValue 'GAOKAO_HTTP_PORT' '5173'
    $baseUrl = "http://localhost:$port"
    Write-Host 'Administrator verification is required to check the live candidate count.'
    $token = Read-AdminToken -BaseUrl $baseUrl
    $headers = @{ Authorization = "Bearer $token" }
    $presence = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/admin/workflow/online" -Headers $headers -TimeoutSec 15
    $online = [int]$presence.data.count

    if ($online -gt 0) {
        Write-Host "Online candidates: $online" -ForegroundColor Yellow
        Write-Host 'Candidates are still online. Unsaved browser changes will be lost.' -ForegroundColor Yellow
        $first = Read-Host 'First confirmation: enter STOP'
        if ($first -cne 'STOP') { Write-Host 'Stop cancelled.'; exit 0 }
        $second = Read-Host 'Second confirmation: enter STOP again'
        if ($second -cne 'STOP') { Write-Host 'Stop cancelled.'; exit 0 }
    } else {
        Write-Host 'Online candidates: 0' -ForegroundColor Green
        $confirm = Read-Host 'Enter STOP to stop the app'
        if ($confirm -cne 'STOP') { Write-Host 'Stop cancelled.'; exit 0 }
    }

    Write-Host 'Automatic backup belongs to P1-3.5. This stop preserves the database directory but does not create a backup.' -ForegroundColor Yellow
    Write-Host 'Closing the public tunnel...'
    & docker compose --profile quick-tunnel --profile named-tunnel stop quick-tunnel named-tunnel 2>$null
    Write-Host 'Stopping application containers gracefully...'
    & docker compose --profile quick-tunnel --profile named-tunnel down --remove-orphans
    if ($LASTEXITCODE -ne 0) { throw 'Containers did not all stop cleanly. Run docker compose ps for details.' }

    if (-not $KeepDockerDesktop) {
        Write-Host 'Stopping Docker Desktop...'
        & docker desktop stop
        if ($LASTEXITCODE -ne 0) { Write-Host 'The app stopped, but Docker Desktop did not exit. Exit it from the system tray.' -ForegroundColor Yellow }
    }
    Write-Host 'The app is stopped. Database files remain in the data directory.' -ForegroundColor Green
    Read-Host 'Press Enter to close this window'
} catch {
    Write-Host ''
    Write-Host "Stop failed or was incomplete: $($_.Exception.Message)" -ForegroundColor Red
    Read-Host 'Press Enter to close this window'
    exit 1
} finally {
    Set-Location $RootDir
}
