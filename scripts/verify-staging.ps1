#Requires -Version 5.1
param(
    [switch]$SkipFetch
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

function Load-ScriptEnv {
    $envFile = Join-Path $PSScriptRoot ".env"
    if (-not (Test-Path $envFile)) {
        throw "Missing scripts/.env — copy from scripts/.env.example"
    }
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*#' -or $_ -match '^\s*$') { return }
        $name, $value = $_ -split '=', 2
        Set-Item -Path "Env:$name" -Value $value.Trim()
    }
}

Load-ScriptEnv
$baseUrl = $env:STAGING_API_URL.TrimEnd('/')

$manifest = Invoke-RestMethod -Uri "$baseUrl/v1/content/manifest" -Method Get
Write-Host "Manifest: content_version=$($manifest.content_version), schema=$($manifest.schema_version)"

$bundleBytes = Invoke-WebRequest -Uri "$baseUrl/v1/content/bundle" -Method Get
$sha = (Get-FileHash -InputStream ([IO.MemoryStream]::new($bundleBytes.Content)) -Algorithm SHA256).Hash.ToLower()
if ($sha -ne $manifest.bundle_sha256) {
    Write-Warning "SHA256 mismatch: manifest=$($manifest.bundle_sha256) actual=$sha"
} else {
    Write-Host "Bundle SHA256 OK ($($manifest.bundle_size_bytes) bytes gzip)"
}

if ($manifest.content_version -eq 0 -or -not $manifest.content_version) {
    Write-Host "No published content yet (expected before first Publish)"
    exit 0
}

$tmp = New-TemporaryFile
try {
    $gzipPath = "$tmp.gz"
    [IO.File]::WriteAllBytes($gzipPath, $bundleBytes.Content)
    $python = Get-Command python -ErrorAction SilentlyContinue
    if ($python) {
        $stats = python -c @"
import gzip, json, sys
with gzip.open(sys.argv[1], 'rt', encoding='utf-8') as f:
    b = json.load(f)
print(len(b.get('categories', [])), len(b.get('commands', [])))
"@ $gzipPath
        $parts = $stats -split ' '
        Write-Host "Published bundle: $($parts[0]) categories, $($parts[1]) commands"
    }
} finally {
    Remove-Item $tmp -Force -ErrorAction SilentlyContinue
    Remove-Item "$tmp.gz" -Force -ErrorAction SilentlyContinue
}
