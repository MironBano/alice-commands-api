#Requires -Version 5.1
$ErrorActionPreference = 'Stop'

function New-AliceShortcut {
    param(
        [Parameter(Mandatory)] $Shell,
        [Parameter(Mandatory)][string]$LnkPath,
        [Parameter(Mandatory)][string]$BatPath,
        [Parameter(Mandatory)][string]$WorkDir,
        [Parameter(Mandatory)][string]$Icon
    )
    $shortcut = $Shell.CreateShortcut($LnkPath)
    $shortcut.TargetPath = $BatPath
    $shortcut.WorkingDirectory = $WorkDir
    $shortcut.IconLocation = $Icon
    $shortcut.Description = 'Alice Commands content pipeline'
    $shortcut.Save()
}

function Install-AliceDesktopShortcuts {
    param(
        [string]$LauncherDir = $PSScriptRoot
    )

    $LauncherDir = [System.IO.Path]::GetFullPath($LauncherDir)
    $configPath = Join-Path $LauncherDir 'shortcuts.json'
    if (-not (Test-Path $configPath)) {
        throw "Missing $configPath"
    }
    $json = [System.IO.File]::ReadAllText($configPath, [Text.UTF8Encoding]::new($false))
    $config = $json | ConvertFrom-Json

    $repoRoot = Split-Path -Parent (Split-Path -Parent $LauncherDir)
    $desktopRoot = [Environment]::GetFolderPath('Desktop')
    $shell = New-Object -ComObject WScript.Shell
    $created = @()

    if (-not $config.folderName) {
        throw 'shortcuts.json: folderName is required'
    }

    $targetDir = [System.IO.Path]::GetFullPath((Join-Path $desktopRoot $config.folderName))
    New-Item -ItemType Directory -Force -Path $targetDir | Out-Null

    foreach ($item in $config.shortcuts) {
        $batPath = [System.IO.Path]::GetFullPath((Join-Path $LauncherDir $item.target))
        if (-not (Test-Path -LiteralPath $batPath)) {
            throw "Missing bat: $batPath"
        }
        $lnkPath = Join-Path $targetDir ($item.name + '.lnk')
        New-AliceShortcut -Shell $shell -LnkPath $lnkPath -BatPath $batPath -WorkDir $LauncherDir -Icon $item.icon
        $created += $lnkPath
    }

    $guideSrc = Join-Path $repoRoot 'docs\ADMIN-CONTENT-GUIDE.md'
    if (Test-Path $guideSrc) {
        Copy-Item $guideSrc (Join-Path $targetDir 'INSTRUKCIYA.md') -Force
    }

    # Drop stale names inside folder
    Get-ChildItem $targetDir -Filter '*.lnk' -ErrorAction SilentlyContinue | ForEach-Object {
        if ($created -notcontains $_.FullName) {
            Remove-Item -LiteralPath $_.FullName -Force -ErrorAction SilentlyContinue
        }
    }

    # Remove duplicate Alice shortcuts from desktop root (legacy auto-install)
    Get-ChildItem $desktopRoot -Filter 'Alice *.lnk' -ErrorAction SilentlyContinue | ForEach-Object {
        Remove-Item -LiteralPath $_.FullName -Force -ErrorAction SilentlyContinue
    }

    # Remove broken legacy Alice folders (OneDrive ghosts)
    Get-ChildItem $desktopRoot -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -like 'Alice Commands*' -and $_.FullName -ne $targetDir } |
        ForEach-Object { Remove-Item -LiteralPath $_.FullName -Recurse -Force -ErrorAction SilentlyContinue }

    return $targetDir
}

if ($MyInvocation.InvocationName -ne '.') {
    $dir = Install-AliceDesktopShortcuts
    Write-Host "OK: $dir" -ForegroundColor Green
    if ($Host.Name -eq 'ConsoleHost' -and [Environment]::UserInteractive -and -not $env:ALICE_SKIP_SHORTCUT_PROMPT) {
        Read-Host 'Enter to close'
    }
}
