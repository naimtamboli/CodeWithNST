$ErrorActionPreference = "Stop"

$projectDirectory = Split-Path -Parent $PSScriptRoot
$mavenCommand = Get-Command mvn -ErrorAction SilentlyContinue

if (-not $mavenCommand) {
    throw "Maven is required. Install Maven and make sure mvn.cmd is on PATH."
}

Push-Location $projectDirectory
try {
    & $mavenCommand.Source -q -DskipTests package
    if ($LASTEXITCODE -ne 0) {
        throw "Maven package failed with exit code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}

function Get-ConfigValue([string]$Path, [string]$Name) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        return ""
    }

    $prefix = "$Name="
    $line = Get-Content -LiteralPath $Path |
        Where-Object { $_.StartsWith($prefix, [StringComparison]::Ordinal) } |
        Select-Object -First 1
    if ($null -eq $line) {
        return ""
    }
    return $line.Substring($prefix.Length).Trim()
}

$configDirectory = Join-Path $env:APPDATA "CityWeather"
$configPath = Join-Path $configDirectory "weather.properties"
$savedSender = Get-ConfigValue $configPath "gmail.address"
$savedRecipient = Get-ConfigValue $configPath "gmail.recipient"
$savedAppPassword = Get-ConfigValue $configPath "gmail.appPassword"

$senderPrompt = "Gmail address to send from"
if ($savedSender) {
    $senderPrompt += " [$savedSender]"
}
$senderInput = Read-Host $senderPrompt
$sender = if ([string]::IsNullOrWhiteSpace($senderInput)) { $savedSender } else { $senderInput.Trim() }
if ([string]::IsNullOrWhiteSpace($sender)) {
    throw "A sender Gmail address is required."
}
try {
    $null = [System.Net.Mail.MailAddress]::new($sender)
} catch {
    throw "Enter a valid sender email address."
}

$recipientPrompt = "Gmail address to receive updates"
if ($savedRecipient) {
    $recipientPrompt += " [$savedRecipient]"
} elseif (-not $savedSender) {
    $recipientPrompt += " (Enter to use sender)"
}
$recipientInput = Read-Host $recipientPrompt
if ([string]::IsNullOrWhiteSpace($recipientInput) -and $savedRecipient) {
    $recipient = $savedRecipient
} elseif ([string]::IsNullOrWhiteSpace($recipientInput)) {
    $recipient = $sender
} else {
    $recipient = $recipientInput.Trim()
}
try {
    $null = [System.Net.Mail.MailAddress]::new($recipient)
} catch {
    throw "Enter a valid recipient email address."
}

$appPassword = $savedAppPassword
if (-not [string]::IsNullOrWhiteSpace($appPassword)) {
    $replacePassword = Read-Host "Replace saved Google App Password? (y/N)"
    if ($replacePassword -notmatch '^(y|yes)$') {
        $replacePassword = $false
    } else {
        $replacePassword = $true
    }
}
if ([string]::IsNullOrWhiteSpace($appPassword) -or $replacePassword -eq $true) {
    Write-Host "Use a Google App Password, not your normal Gmail password."
    $securePassword = Read-Host "Google App Password" -AsSecureString
    $passwordPointer = [IntPtr]::Zero
    try {
        $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
        $appPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer).Replace(" ", "")
    } finally {
        if ($passwordPointer -ne [IntPtr]::Zero) {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
        }
    }
    if ([string]::IsNullOrWhiteSpace($appPassword)) {
        throw "The Google App Password cannot be empty."
    }
}

New-Item -ItemType Directory -Path $configDirectory -Force | Out-Null

$identity = [Security.Principal.WindowsIdentity]::GetCurrent().Name
$icacls = Join-Path $env:SystemRoot "System32\icacls.exe"
& $icacls $configDirectory /inheritance:r /grant:r "${identity}:(OI)(CI)F" | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Could not restrict access to $configDirectory (icacls exit code $LASTEXITCODE)."
}

$configLines = @(
    "gmail.address=$sender",
    "gmail.recipient=$recipient",
    "gmail.appPassword=$appPassword"
)
[System.IO.File]::WriteAllLines($configPath, $configLines, [System.Text.Encoding]::ASCII)
& $icacls $configPath /inheritance:r /grant:r "${identity}:F" | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Could not restrict access to $configPath (icacls exit code $LASTEXITCODE)."
}
Remove-Variable appPassword, securePassword -ErrorAction SilentlyContinue

Unregister-ScheduledTask -TaskName "Pune Weather Email" -Confirm:$false -ErrorAction SilentlyContinue

Write-Host "Email configuration saved. No recurring schedule was created."
Write-Host "Run the WeatherApp Java class to send one Pune weather email."
Write-Host "Configuration file: $configPath"
