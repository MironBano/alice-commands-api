# Load scripts/.env, then override pipeline keys from repo root .env when present.

function Load-ScriptEnv {
    param(
        [string[]]$RootOverrideKeys = @(
            'STAGING_API_URL',
            'ADMIN_USERNAME',
            'ADMIN_PASSWORD',
            'PUBLIC_BASE_URL'
        )
    )

    $scriptsEnv = Join-Path $PSScriptRoot '.env'
    if (-not (Test-Path $scriptsEnv)) {
        throw 'Missing scripts/.env - copy from scripts/.env.example'
    }

    $values = @{}
    Get-Content $scriptsEnv | ForEach-Object {
        if ($_ -match '^\s*#' -or $_ -match '^\s*$') { return }
        $name, $value = $_ -split '=', 2
        $values[$name.Trim()] = $value.Trim().Trim('"').Trim("'")
    }

    $rootEnv = Join-Path (Split-Path -Parent $PSScriptRoot) '.env'
    if (Test-Path $rootEnv) {
        Get-Content $rootEnv | ForEach-Object {
            if ($_ -match '^\s*#' -or $_ -match '^\s*$') { return }
            $name, $value = $_ -split '=', 2
            $key = $name.Trim()
            if ($RootOverrideKeys -contains $key) {
                $values[$key] = $value.Trim().Trim('"').Trim("'")
            }
        }
    }

    foreach ($entry in $values.GetEnumerator()) {
        Set-Item -Path "Env:$($entry.Key)" -Value $entry.Value
    }

    if (-not $env:STAGING_API_URL -and $env:PUBLIC_BASE_URL) {
        Set-Item -Path 'Env:STAGING_API_URL' -Value $env:PUBLIC_BASE_URL
    }
}
