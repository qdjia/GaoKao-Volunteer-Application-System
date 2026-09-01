param(
    [int]$Port = 5173
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Split-Path -Parent $ScriptDir
$FrontendDir = Join-Path $RootDir "frontend"
$LogDir = Join-Path $RootDir "logs"

function Test-PortListening {
    param([int]$CheckPort)
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $async = $client.BeginConnect("127.0.0.1", $CheckPort, $null, $null)
        $ok = $async.AsyncWaitHandle.WaitOne(500)
        if ($ok) {
            $client.EndConnect($async)
        }
        $client.Close()
        return $ok
    } catch {
        return $false
    }
}

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

if (Test-PortListening -CheckPort $Port) {
    Write-Host "Frontend is already running on http://localhost:$Port"
    exit 0
}

$npm = "npm.cmd"
Start-Process -FilePath $npm `
    -ArgumentList @("run", "dev", "--", "--host", "127.0.0.1") `
    -WorkingDirectory $FrontendDir `
    -WindowStyle Hidden

for ($i = 0; $i -lt 20; $i++) {
    Start-Sleep -Seconds 1
    if (Test-PortListening -CheckPort $Port) {
        Write-Host "Frontend started: http://localhost:$Port"
        exit 0
    }
}

Write-Host "Frontend start command was issued, but port $Port is not ready yet."
Write-Host "Run this manually if you need console logs: cd frontend; npm run dev"
exit 1
