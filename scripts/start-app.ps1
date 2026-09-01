$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

& (Join-Path $ScriptDir "start-backend.ps1")
& (Join-Path $ScriptDir "start-frontend.ps1")

Start-Process "http://localhost:5173"
