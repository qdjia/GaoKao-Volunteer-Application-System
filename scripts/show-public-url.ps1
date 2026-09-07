$ErrorActionPreference = 'Stop'

$logs = docker compose --profile quick-tunnel logs quick-tunnel --no-color 2>&1 | Out-String
$match = [regex]::Match($logs, 'https://[a-z0-9-]+\.trycloudflare\.com')

if (-not $match.Success) {
    Write-Error '尚未找到公网地址。请确认 quick-tunnel 容器已启动，再等待几秒后重试。'
}

Write-Host "考生公网地址：$($match.Value)"
