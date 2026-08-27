# Production HTTP helpers (DNS fallback via curl --resolve for RF networks).

function Test-ProdDnsWorks {
    $hostName = ([Uri]$env:PROD_API_URL.TrimEnd('/')).Host
    try {
        [void][System.Net.Dns]::GetHostAddresses($hostName)
        return $true
    } catch {
        return $false
    }
}

function Get-ProdOriginIp {
    if ($env:PROD_ORIGIN_IP) { return $env:PROD_ORIGIN_IP }
    if ($env:STAGING_ORIGIN_IP) { return $env:STAGING_ORIGIN_IP }
    return '161.104.46.92'
}

function Get-ProdCurlArgs {
    param(
        [ValidateSet('Get', 'Post', 'Put')][string]$Method = 'Get',
        [string]$CookieJar,
        [string]$ContentType = 'application/json',
        [string]$BodyPath
    )
    $args = @('--silent', '--show-error', '--fail', '--max-time', '60')
    if (-not (Test-ProdDnsWorks)) {
        $hostName = ([Uri]$env:PROD_API_URL.TrimEnd('/')).Host
        $args += @('--resolve', "${hostName}:443:$(Get-ProdOriginIp)")
        if ($env:PROD_TLS_INSECURE -ne '0') {
            $args += '--insecure'
        }
        Write-Warning "DNS unavailable for $hostName; curl --resolve $(Get-ProdOriginIp)"
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

function Invoke-ProdJsonGet {
    param(
        [Parameter(Mandatory)][string]$Path,
        [string]$CookieJar
    )
    $url = "$($env:PROD_API_URL.TrimEnd('/'))$Path"
    $curlArgs = @(Get-ProdCurlArgs -Method Get -CookieJar $CookieJar) + @($url)
    $json = Invoke-CurlUtf8Text -CurlArgs $curlArgs
    return $json | ConvertFrom-Json
}

function Invoke-ProdJsonPost {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][string]$CookieJar,
        [string]$BodyPath,
        [string]$BodyInline
    )
    $url = "$($env:PROD_API_URL.TrimEnd('/'))$Path"
    $tmp = $null
    try {
        if ($BodyInline -and -not $BodyPath) {
            $tmp = New-TemporaryFile
            [IO.File]::WriteAllText($tmp.FullName, $BodyInline, [Text.UTF8Encoding]::new($false))
            $BodyPath = $tmp.FullName
        }
        $curlArgs = @(Get-ProdCurlArgs -Method Post -CookieJar $CookieJar -BodyPath $BodyPath) + @($url)
        $json = Invoke-CurlUtf8Text -CurlArgs $curlArgs
        if ($json) { return $json | ConvertFrom-Json }
        return $null
    } finally {
        if ($tmp) { Remove-Item $tmp.FullName -Force -ErrorAction SilentlyContinue }
    }
}

function Invoke-ProdJsonPut {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][string]$CookieJar,
        [string]$BodyPath,
        [string]$BodyInline
    )
    $url = "$($env:PROD_API_URL.TrimEnd('/'))$Path"
    $tmp = $null
    try {
        if ($BodyInline -and -not $BodyPath) {
            $tmp = New-TemporaryFile
            [IO.File]::WriteAllText($tmp.FullName, $BodyInline, [Text.UTF8Encoding]::new($false))
            $BodyPath = $tmp.FullName
        }
        $curlArgs = @(Get-ProdCurlArgs -Method Put -CookieJar $CookieJar -BodyPath $BodyPath) + @($url)
        $json = Invoke-CurlUtf8Text -CurlArgs $curlArgs
        if ($json) { return $json | ConvertFrom-Json }
        return $null
    } finally {
        if ($tmp) { Remove-Item $tmp.FullName -Force -ErrorAction SilentlyContinue }
    }
}

function Invoke-ProdDownload {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][string]$OutFile
    )
    $url = "$($env:PROD_API_URL.TrimEnd('/'))$Path"
    $out = [IO.Path]::GetFullPath($OutFile)
    $curlArgs = @(Get-ProdCurlArgs -Method Get) + @('-o', $out, $url)
    & curl.exe @curlArgs
    if ($LASTEXITCODE -ne 0) { throw "Download $Path failed (exit $LASTEXITCODE)" }
    if (-not (Test-Path -LiteralPath $out)) { throw "Download $Path did not create $out" }
}

function New-ProdSession {
    param([Parameter(Mandatory)][string]$CookieJar)
    $login = @{ username = $env:ADMIN_USERNAME; password = $env:ADMIN_PASSWORD } | ConvertTo-Json -Compress
    Invoke-ProdJsonPost -Path '/admin/api/login' -CookieJar $CookieJar -BodyInline $login | Out-Null
}
