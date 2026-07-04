# Staging HTTP helpers (DNS fallback via curl --resolve for RF networks).

function Test-StagingDnsWorks {
    $hostName = ([Uri]$env:STAGING_API_URL.TrimEnd('/')).Host
    try {
        [void][System.Net.Dns]::GetHostAddresses($hostName)
        return $true
    } catch {
        return $false
    }
}

function Get-StagingOriginIp {
    if ($env:STAGING_ORIGIN_IP) { return $env:STAGING_ORIGIN_IP }
    return '161.104.46.92'
}

function Get-StagingCurlArgs {
    param(
        [ValidateSet('Get', 'Post')][string]$Method = 'Get',
        [string]$CookieJar,
        [string]$ContentType = 'application/json',
        [string]$BodyPath
    )
    $args = @('--silent', '--show-error', '--fail', '--max-time', '60')
    if (-not (Test-StagingDnsWorks)) {
        $hostName = ([Uri]$env:STAGING_API_URL.TrimEnd('/')).Host
        $args += @('--resolve', "${hostName}:443:$(Get-StagingOriginIp)")
        Write-Warning "DNS unavailable for $hostName; curl --resolve $(Get-StagingOriginIp)"
    }
    if ($Method -eq 'Post') { $args += '-X', 'POST' }
    if ($CookieJar) { $args += '-c', $CookieJar, '-b', $CookieJar }
    if ($BodyPath) {
        $args += @('-H', "Content-Type: $ContentType", '--data-binary', "@$BodyPath")
    }
    return $args
}

function Invoke-StagingJsonGet {
    param(
        [Parameter(Mandatory)][string]$Path,
        [string]$CookieJar
    )
    $url = "$($env:STAGING_API_URL.TrimEnd('/'))$Path"
    $curlArgs = @(Get-StagingCurlArgs -Method Get -CookieJar $CookieJar) + @($url)
    $json = & curl.exe @curlArgs
    if ($LASTEXITCODE -ne 0) { throw "GET $Path failed (exit $LASTEXITCODE)" }
    return $json | ConvertFrom-Json
}

function Invoke-StagingJsonPost {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][string]$CookieJar,
        [string]$BodyPath,
        [string]$BodyInline
    )
    $url = "$($env:STAGING_API_URL.TrimEnd('/'))$Path"
    $tmp = $null
    try {
        if ($BodyInline -and -not $BodyPath) {
            $tmp = New-TemporaryFile
            [IO.File]::WriteAllText($tmp.FullName, $BodyInline, [Text.UTF8Encoding]::new($false))
            $BodyPath = $tmp.FullName
        }
        $curlArgs = @(Get-StagingCurlArgs -Method Post -CookieJar $CookieJar -BodyPath $BodyPath) + @($url)
        $json = & curl.exe @curlArgs
        if ($LASTEXITCODE -ne 0) { throw "POST $Path failed (exit $LASTEXITCODE)" }
        if ($json) { return $json | ConvertFrom-Json }
        return $null
    } finally {
        if ($tmp) { Remove-Item $tmp.FullName -Force -ErrorAction SilentlyContinue }
    }
}

function Invoke-StagingDownload {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][string]$OutFile
    )
    $url = "$($env:STAGING_API_URL.TrimEnd('/'))$Path"
    $out = [IO.Path]::GetFullPath($OutFile)
    $curlArgs = @(Get-StagingCurlArgs -Method Get) + @('-o', $out, $url)
    & curl.exe @curlArgs
    if ($LASTEXITCODE -ne 0) { throw "Download $Path failed (exit $LASTEXITCODE)" }
    if (-not (Test-Path -LiteralPath $out)) { throw "Download $Path did not create $out" }
}

function New-StagingSession {
    param([Parameter(Mandatory)][string]$CookieJar)
    $login = @{ username = $env:ADMIN_USERNAME; password = $env:ADMIN_PASSWORD } | ConvertTo-Json -Compress
    Invoke-StagingJsonPost -Path '/admin/api/login' -CookieJar $CookieJar -BodyInline $login | Out-Null
}
