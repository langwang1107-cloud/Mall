$ErrorActionPreference = 'Stop'
$block = @'
# BEGIN managed terminal network
$env:HTTP_PROXY = 'http://127.0.0.1:7890'
$env:HTTPS_PROXY = 'http://127.0.0.1:7890'
$env:ALL_PROXY = 'http://127.0.0.1:7890'
$env:NO_PROXY = 'localhost,127.0.0.1,::1'
function global:network {
    param([ValidateSet('on','off','status')][string]$Mode = 'status')
    switch ($Mode) {
        'on' {
            $env:HTTP_PROXY = 'http://127.0.0.1:7890'
            $env:HTTPS_PROXY = 'http://127.0.0.1:7890'
            $env:ALL_PROXY = 'http://127.0.0.1:7890'
            $env:NO_PROXY = 'localhost,127.0.0.1,::1'
        }
        'off' { Remove-Item Env:HTTP_PROXY,Env:HTTPS_PROXY,Env:ALL_PROXY -ErrorAction SilentlyContinue }
    }
    Get-ChildItem Env: | Where-Object { $_.Name -in @('HTTP_PROXY','HTTPS_PROXY','ALL_PROXY','NO_PROXY') } | Sort-Object Name
}
# END managed terminal network
'@
$documents = [Environment]::GetFolderPath('MyDocuments')
foreach ($folder in @('PowerShell','WindowsPowerShell')) {
    $profilePath = Join-Path (Join-Path $documents $folder) 'profile.ps1'
    [System.IO.Directory]::CreateDirectory((Split-Path -Parent $profilePath)) | Out-Null
    $content = if (Test-Path -LiteralPath $profilePath) { [System.IO.File]::ReadAllText($profilePath) } else { '' }
    if ($content) { Copy-Item -LiteralPath $profilePath -Destination ($profilePath + '.backup-' + (Get-Date -Format 'yyyyMMddHHmmss')) }
    $pattern = '(?ms)^# BEGIN managed terminal network\r?\n.*?^# END managed terminal network\r?\n?'
    if ($content -match $pattern) { $content = [regex]::Replace($content,$pattern,[System.Text.RegularExpressions.MatchEvaluator]{param($match) $block + "`r`n"}) }
    else { $content = $content.TrimEnd() + "`r`n" + $block + "`r`n" }
    [System.IO.File]::WriteAllText($profilePath,$content,(New-Object System.Text.UTF8Encoding($true)))
    Write-Output "Updated: $profilePath"
}
foreach ($name in @('HTTP_PROXY','HTTPS_PROXY','ALL_PROXY')) { [Environment]::SetEnvironmentVariable($name,'http://127.0.0.1:7890','User') }
[Environment]::SetEnvironmentVariable('NO_PROXY','localhost,127.0.0.1,::1','User')
Write-Output 'User proxy environment variables saved.'
