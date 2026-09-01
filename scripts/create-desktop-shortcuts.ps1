$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$DesktopDir = [Environment]::GetFolderPath("Desktop")
$PowerShell = "$env:SystemRoot\System32\WindowsPowerShell\v1.0\powershell.exe"

function New-Shortcut {
    param(
        [string]$Name,
        [string]$TargetScript,
        [string]$Description
    )

    $shell = New-Object -ComObject WScript.Shell
    $shortcut = $shell.CreateShortcut((Join-Path $DesktopDir "$Name.lnk"))
    $shortcut.TargetPath = $PowerShell
    $shortcut.Arguments = "-NoProfile -ExecutionPolicy Bypass -File `"$TargetScript`""
    $shortcut.WorkingDirectory = $ScriptDir
    $shortcut.Description = $Description
    $shortcut.IconLocation = "$PowerShell,0"
    $shortcut.Save()
}

New-Shortcut `
    -Name "Gaokao Start App" `
    -TargetScript (Join-Path $ScriptDir "start-app-session.ps1") `
    -Description "Start the app in a standalone window, then stop services when the window closes."

New-Shortcut `
    -Name "Gaokao Backend Status" `
    -TargetScript (Join-Path $ScriptDir "backend-status.ps1") `
    -Description "Check whether the backend is listening on port 8080."

New-Shortcut `
    -Name "Gaokao Stop Backend" `
    -TargetScript (Join-Path $ScriptDir "stop-backend.ps1") `
    -Description "Stop the backend process listening on port 8080."

Write-Host "Desktop shortcuts created in $DesktopDir"
