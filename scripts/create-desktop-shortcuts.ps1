$ErrorActionPreference = 'Stop'
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Split-Path -Parent $ScriptDir
$DesktopDir = [Environment]::GetFolderPath('Desktop')
$PowerShell = "$env:SystemRoot\System32\WindowsPowerShell\v1.0\powershell.exe"

function New-AppShortcut {
    param([string]$Name, [string]$ScriptName, [string]$Description)
    $shell = New-Object -ComObject WScript.Shell
    $shortcut = $shell.CreateShortcut((Join-Path $DesktopDir "$Name.lnk"))
    $shortcut.TargetPath = $PowerShell
    $shortcut.Arguments = "-NoProfile -ExecutionPolicy Bypass -File `"$(Join-Path $ScriptDir $ScriptName)`""
    $shortcut.WorkingDirectory = $RootDir
    $shortcut.Description = $Description
    $shortcut.IconLocation = "$PowerShell,0"
    $shortcut.Save()
}

foreach ($legacyName in @('Gaokao Start App.lnk', 'Gaokao Backend Status.lnk', 'Gaokao Stop Backend.lnk')) {
    $legacyPath = Join-Path $DesktopDir $legacyName
    if (Test-Path -LiteralPath $legacyPath) { Remove-Item -LiteralPath $legacyPath -Force }
}

New-AppShortcut -Name 'Gaokao - Start' -ScriptName 'start-app.ps1' -Description 'Start the app and its temporary public URL'
New-AppShortcut -Name 'Gaokao - Stop' -ScriptName 'stop-app.ps1' -Description 'Check online candidates, stop the app, and exit Docker Desktop'

Write-Host "Desktop shortcuts created in: $DesktopDir"
Write-Host 'No automatic startup task is created.'
