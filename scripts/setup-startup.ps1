$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

& (Join-Path $ScriptDir "create-desktop-shortcuts.ps1")

Write-Host "Desktop shortcut setup complete. Backend will only start when you click the shortcut."
