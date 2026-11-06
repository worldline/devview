<#
.SYNOPSIS
Saves a local screenshot, UI hierarchy and device metadata without attaching images to a chat.
.EXAMPLE
.\scripts\capture_ui.ps1 -Name home-light
.EXAMPLE
.\scripts\capture_ui.ps1 -SelfTest
#>
[CmdletBinding()]
param(
    [ValidatePattern('^[a-zA-Z0-9_-]+$')]
    [string]$Name = 'screen',
    [string]$OutputDirectory = (Join-Path $env:TEMP 'devview-ui-captures'),
    [string]$Serial,
    [switch]$SelfTest
)

$ErrorActionPreference = 'Stop'

function Assert-Png([string]$Path) {
    $bytes = [IO.File]::ReadAllBytes($Path)
    $signature = [byte[]](137, 80, 78, 71, 13, 10, 26, 10)
    if ($bytes.Length -lt 8) { throw 'Screenshot is shorter than a PNG header.' }
    for ($i = 0; $i -lt 8; $i++) {
        if ($bytes[$i] -ne $signature[$i]) { throw 'Screenshot has an invalid PNG header.' }
    }
}

if ($SelfTest) {
    $testFile = [IO.Path]::GetTempFileName()
    try {
        [IO.File]::WriteAllBytes($testFile, [byte[]](137, 80, 78, 71, 13, 10, 26, 10))
        Assert-Png $testFile
        [IO.File]::WriteAllBytes($testFile, [byte[]](0, 1, 2, 3, 4, 5, 6, 7))
        $rejected = $false
        try { Assert-Png $testFile } catch { $rejected = $true }
        if (-not $rejected) { throw 'Self-test did not reject non-PNG data.' }
        'Self-test passed (PNG signature acceptance and rejection).'
    } finally { Remove-Item -LiteralPath $testFile -Force }
    return
}

$adb = (Get-Command adb -ErrorAction Stop).Source
$deviceLines = & $adb devices
if ($LASTEXITCODE -ne 0) { throw 'adb devices failed.' }
$devices = @($deviceLines | Where-Object { $_ -match '^\S+\s+device$' } | ForEach-Object { ($_ -split '\s+')[0] })
if ($Serial) {
    if ($Serial -notin $devices) { throw 'Requested device is not connected and authorized.' }
} else {
    if ($devices.Count -ne 1) { throw 'Connect exactly one authorized device, or specify -Serial.' }
    $Serial = $devices[0]
}
if ($Serial -notmatch '^[a-zA-Z0-9_.:-]+$') { throw 'Device serial contains unsupported command-line characters.' }

function Invoke-AdbText([string[]]$Arguments) {
    $result = & $adb -s $Serial @Arguments
    if ($LASTEXITCODE -ne 0) { throw "adb failed: $($Arguments -join ' ')" }
    return ($result -join "`n")
}

$directory = Join-Path $OutputDirectory ("{0}-{1}" -f $Name, (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
New-Item -ItemType Directory -Path $directory | Out-Null
$pngPath = Join-Path $directory 'screen.png'
$remoteXml = "/sdcard/devview-ui-$([Guid]::NewGuid().ToString('N')).xml"

# Let navigation finish before capturing; live content can still change between PNG and XML.
Start-Sleep -Seconds 1
# PowerShell 5 redirection decodes native stdout, corrupting binary PNG bytes.
$process = New-Object Diagnostics.Process
$process.StartInfo = New-Object Diagnostics.ProcessStartInfo
$process.StartInfo.FileName = $adb
$process.StartInfo.Arguments = "-s $Serial exec-out screencap -p"
$process.StartInfo.UseShellExecute = $false
$process.StartInfo.RedirectStandardOutput = $true
$process.StartInfo.RedirectStandardError = $true
$file = $null
try {
    if (-not $process.Start()) { throw 'Could not start screencap.' }
    $stderr = $process.StandardError.ReadToEndAsync()
    $file = [IO.File]::Create($pngPath)
    $process.StandardOutput.BaseStream.CopyTo($file)
    $file.Dispose()
    $file = $null
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) { throw "screencap failed: $($stderr.Result)" }
    Assert-Png $pngPath
} finally {
    if ($file) { $file.Dispose() }
    $process.Dispose()
}

try {
    Invoke-AdbText -Arguments @('shell', 'uiautomator', 'dump', $remoteXml) | Out-Null
    Invoke-AdbText -Arguments @('pull', $remoteXml, (Join-Path $directory 'ui.xml')) | Out-Null
} finally {
    Invoke-AdbText -Arguments @('shell', 'rm', $remoteXml) | Out-Null
}

[ordered]@{
    capturedAt = (Get-Date).ToString('o')
    label = $Name
    serial = $Serial
    size = Invoke-AdbText -Arguments @('shell', 'wm', 'size')
    density = Invoke-AdbText -Arguments @('shell', 'wm', 'density')
    fontScale = Invoke-AdbText -Arguments @('shell', 'settings', 'get', 'system', 'font_scale')
    note = 'PNG and UI XML are sequential captures, not an atomic frame. No automatic visual verdict.'
} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $directory 'metadata.json') -Encoding UTF8
"Saved local capture: $directory"
