$ErrorActionPreference = 'Stop'

$logs = docker compose --profile quick-tunnel logs quick-tunnel --no-color 2>&1 | Out-String
$match = [regex]::Match($logs, 'https://[a-z0-9-]+\.trycloudflare\.com')

if (-not $match.Success) {
    Write-Error 'No public URL was found. Verify that quick-tunnel is running, wait a few seconds, and try again.'
}

Write-Host "Candidate public URL: $($match.Value)"
