param(
    [int]$Port = 8080
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Split-Path -Parent $ScriptDir
$BackendDir = Join-Path $RootDir "backend"
$JarPath = Join-Path $BackendDir "target\gaokao-zhiyuan-1.0.0.jar"
$LogDir = Join-Path $RootDir "logs"
$BackendLog = Join-Path $LogDir "backend.log"

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

function Resolve-Java {
    $preferred = "D:\java\bin\java.exe"
    if (Test-Path -LiteralPath $preferred) {
        return $preferred
    }
    return "java"
}

function Resolve-Maven {
    $preferred = "D:\maven\apache-maven-3.9.16\bin\mvn.cmd"
    if (Test-Path -LiteralPath $preferred) {
        return $preferred
    }
    return "mvn"
}

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

if (Test-PortListening -CheckPort $Port) {
    Write-Host "Backend is already running on http://localhost:$Port"
    exit 0
}

if (!(Test-Path -LiteralPath $JarPath)) {
    Write-Host "Backend jar not found. Building once..."
    $maven = Resolve-Maven
    Push-Location $BackendDir
    try {
        & $maven -q -DskipTests package
    } finally {
        Pop-Location
    }
}

$java = Resolve-Java
Start-Process -FilePath $java `
    -ArgumentList @("-jar", $JarPath, "--logging.file.name=$BackendLog") `
    -WorkingDirectory $BackendDir `
    -WindowStyle Hidden

for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 1
    if (Test-PortListening -CheckPort $Port) {
        Write-Host "Backend started: http://localhost:$Port"
        exit 0
    }
}

Write-Host "Backend start command was issued, but port $Port is not ready yet."
Write-Host "Check log: $BackendLog"
exit 1
