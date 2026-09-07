$ErrorActionPreference = 'Stop'
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host 'This compatibility entry now uses Docker. Closing it does not stop the app.'
& (Join-Path $ScriptDir 'start-app.ps1')
