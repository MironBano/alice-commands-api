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
        [ValidateSet('Get', 'Post', 'Put')][string]$Method = 'Get',
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
    if ($Method -eq 'Put') { $args += '-X', 'PUT' }
    if ($CookieJar) { $args += '-c', $CookieJar, '-b', $CookieJar }
    if ($BodyPath) {
        $args += @('-H', "Content-Type: $ContentType", '--data-binary', "@$BodyPath")
    }
    return $args
}

function Invoke-CurlUtf8Text {
    param([Parameter(Mandatory)][string[]]$CurlArgs)
    $outFile = New-TemporaryFile
    try {
        # Drop --fail so we can surface HTTP status in the throw (needed for PUT→POST on 404).
        $argsNoFail = @($CurlArgs | Where-Object { $_ -ne '--fail' })
        $httpCode = & curl.exe @($argsNoFail + @('-o', $outFile.FullName, '-w', '%{http_code}'))
        if ($LASTEXITCODE -ne 0) {
            throw "curl failed (exit $LASTEXITCODE)"
        }
        $code = [int]$httpCode
        if ($code -lt 200 -or $code -ge 300) {
            $bodyPreview = ''
            if (Test-Path -LiteralPath $outFile.FullName) {
                $bodyPreview = [IO.File]::ReadAllText($outFile.FullName, [Text.UTF8Encoding]::new($false))
                if ($bodyPreview.Length -gt 300) { $bodyPreview = $bodyPreview.Substring(0, 300) }
            }
            throw "curl failed (HTTP $code) $bodyPreview"
        }
        return [IO.File]::ReadAllText($outFile.FullName, [Text.UTF8Encoding]::new($false))
    } finally {
        Remove-Item $outFile.FullName -Force -ErrorAction SilentlyContinue
    }
}

function Invoke-StagingJsonGet {
    param(
        [Parameter(Mandatory)][string]$Path,
        [string]$CookieJar
    )
    $url = "$($env:STAGING_API_URL.TrimEnd('/'))$Path"
    $curlArgs = @(Get-StagingCurlArgs -Method Get -CookieJar $CookieJar) + @($url)
    $json = Invoke-CurlUtf8Text -CurlArgs $curlArgs
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
        $json = Invoke-CurlUtf8Text -CurlArgs $curlArgs
        if ($json) { return $json | ConvertFrom-Json }
        return $null
    } finally {
        if ($tmp) { Remove-Item $tmp.FullName -Force -ErrorAction SilentlyContinue }
    }
}

function Invoke-StagingJsonPut {
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
        $curlArgs = @(Get-StagingCurlArgs -Method Put -CookieJar $CookieJar -BodyPath $BodyPath) + @($url)
        $json = Invoke-CurlUtf8Text -CurlArgs $curlArgs
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
