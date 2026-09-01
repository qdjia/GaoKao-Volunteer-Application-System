param(
    [int]$BackendPort = 8080,
    [int]$FrontendPort = 5173
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Split-Path -Parent $ScriptDir
$RuntimeDir = Join-Path $RootDir ".runtime"
$BrowserProfileDir = Join-Path $RuntimeDir "browser-profile"
$AppUrl = "http://localhost:$FrontendPort"

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

function Resolve-AppBrowser {
    $programFilesX86 = [Environment]::GetEnvironmentVariable("ProgramFiles(x86)")
    $programFiles = [Environment]::GetEnvironmentVariable("ProgramFiles")
    $localAppData = [Environment]::GetEnvironmentVariable("LocalAppData")

    function Join-OptionalPath {
        param(
            [string]$BasePath,
            [string]$ChildPath
        )
        if ([string]::IsNullOrWhiteSpace($BasePath)) {
            return $null
        }
        return Join-Path $BasePath $ChildPath
    }

    $candidates = @(
        (Join-OptionalPath $programFilesX86 "Microsoft\Edge\Application\msedge.exe"),
        (Join-OptionalPath $programFiles "Microsoft\Edge\Application\msedge.exe"),
        (Join-OptionalPath $localAppData "Microsoft\Edge\Application\msedge.exe"),
        (Join-OptionalPath $programFiles "Google\Chrome\Application\chrome.exe"),
        (Join-OptionalPath $programFilesX86 "Google\Chrome\Application\chrome.exe"),
        (Join-OptionalPath $localAppData "Google\Chrome\Application\chrome.exe")
    )

    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path -LiteralPath $candidate)) {
            return $candidate
        }
    }

    return $null
}

$backendWasRunning = Test-PortListening -CheckPort $BackendPort
$frontendWasRunning = Test-PortListening -CheckPort $FrontendPort

try {
    & (Join-Path $ScriptDir "start-backend.ps1") -Port $BackendPort
    & (Join-Path $ScriptDir "start-frontend.ps1") -Port $FrontendPort

    New-Item -ItemType Directory -Force -Path $BrowserProfileDir | Out-Null
    $browser = Resolve-AppBrowser

    if ($browser) {
        $browserArgs = @(
            "--app=$AppUrl",
            "--user-data-dir=$BrowserProfileDir",
            "--no-first-run",
            "--disable-features=Translate"
        )
        $process = Start-Process -FilePath $browser -ArgumentList $browserArgs -PassThru
        Write-Host "App window opened: $AppUrl"
        Write-Host "Close the app window to stop services started by this session."
        Wait-Process -Id $process.Id
    } else {
        Start-Process $AppUrl
        Write-Host "App opened in the default browser: $AppUrl"
        Read-Host "Press Enter after closing the page to stop services started by this session"
    }
} finally {
    if (!$frontendWasRunning) {
        & (Join-Path $ScriptDir "stop-frontend.ps1") -Port $FrontendPort
    } else {
        Write-Host "Frontend was already running before this session; leaving it untouched."
    }

    if (!$backendWasRunning) {
        & (Join-Path $ScriptDir "stop-backend.ps1") -Port $BackendPort
    } else {
        Write-Host "Backend was already running before this session; leaving it untouched."
    }
}
