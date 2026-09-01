param(
    [int]$Port = 8080
)

$ErrorActionPreference = "Stop"
$lines = netstat -ano | Select-String "LISTENING" | Select-String ":$Port "

if (!$lines) {
    Write-Host "No backend process is listening on port $Port."
    exit 0
}

$processIds = $lines | ForEach-Object {
    $parts = ($_ -replace '^\s+', '') -split '\s+'
    $parts[-1]
} | Sort-Object -Unique

foreach ($processId in $processIds) {
    Stop-Process -Id $processId -Force
    Write-Host "Stopped process $processId on port $Port."
}
