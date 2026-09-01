param(
    [int]$Port = 8080
)

try {
    $client = New-Object System.Net.Sockets.TcpClient
    $async = $client.BeginConnect("127.0.0.1", $Port, $null, $null)
    $ok = $async.AsyncWaitHandle.WaitOne(500)
    if ($ok) {
        $client.EndConnect($async)
    }
    $client.Close()

    if ($ok) {
        Write-Host "Backend is running: http://localhost:$Port"
        exit 0
    }
} catch {
}

Write-Host "Backend is not running on port $Port."
exit 1
