# Shared Python resolver for Windows (ignores Microsoft Store python.exe stub).

function Get-PythonCommand {
    if (Get-Command py -ErrorAction SilentlyContinue) {
        & py -3 -c "import sys" 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) {
            return @{
                File = (Get-Command py).Source
                PrefixArgs = @('-3')
            }
        }
    }

    $realPython = @(
        "$env:LOCALAPPDATA\Programs\Python\Python313\python.exe",
        "$env:LOCALAPPDATA\Programs\Python\Python312\python.exe",
        "$env:ProgramFiles\Python313\python.exe",
        "$env:ProgramFiles\Python312\python.exe"
    ) | Where-Object { Test-Path $_ } | Select-Object -First 1

    if ($realPython) {
        return @{ File = $realPython; PrefixArgs = @() }
    }

    $pythonCmd = Get-Command python -ErrorAction SilentlyContinue
    if ($pythonCmd -and $pythonCmd.Source -notmatch 'WindowsApps') {
        & $pythonCmd.Source -c "import sys" 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) {
            return @{ File = $pythonCmd.Source; PrefixArgs = @() }
        }
    }

    throw @"
Python 3 not found.
Run: winget install -e --id Python.Python.3.13
Or disable Store alias: Settings > Apps > App execution aliases > python.exe OFF
"@
}

function Invoke-PythonScript {
    param(
        [Parameter(Mandatory, Position = 0)]
        [string]$ScriptPath,
        [Parameter(ValueFromRemainingArguments = $true)]
        [string[]]$ScriptArgs
    )
    $py = Get-PythonCommand
    & $py.File @($py.PrefixArgs + $ScriptPath) @ScriptArgs
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

function Invoke-PythonCode {
    param(
        [Parameter(Mandatory)]
        [string]$Code,
        [Parameter(ValueFromRemainingArguments = $true)]
        [string[]]$ScriptArgs
    )
    $py = Get-PythonCommand
    & $py.File @($py.PrefixArgs + '-c' + $Code) @ScriptArgs
    if ($LASTEXITCODE -ne 0) { throw "Python failed (exit $LASTEXITCODE)" }
}
