#Requires -Version 5.1
<#
.SYNOPSIS
    Pairs this PC with the Debout Android app.

.DESCRIPTION
    Finds the active network adapter (Ethernet first), checks the Wake-on-LAN setup
    and offers to fix what it can, then opens a page that shows a QR code to scan
    with Debout. The machine's details travel in the URL fragment, which the
    browser never sends to the server.

    Run it with: irm debout.sybers.fr/win | iex
#>
[CmdletBinding()]
param(
    [string] $SiteUrl = 'https://debout.sybers.fr'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$PowerKey = 'HKLM:\SYSTEM\CurrentControlSet\Control\Power'
$SessionPowerKey = 'HKLM:\SYSTEM\CurrentControlSet\Control\Session Manager\Power'

$Messages = @{
    en = @{
        Title                  = 'Debout: pair this PC'
        NoAdapter              = 'No connected network adapter with an IPv4 address. Connect this PC to your local network and try again.'
        Adapter                = 'Adapter'
        Mac                    = 'MAC'
        Ip                     = 'IP'
        Name                   = 'Name'
        Checking               = 'Checking Wake-on-LAN...'
        AllGood                = 'Wake-on-LAN looks ready on this PC.'
        IssueWifi              = 'This PC uses Wi-Fi. Wake-on-LAN rarely works over Wi-Fi: plug in an Ethernet cable if you can.'
        IssueMagicUnsupported  = 'The adapter "{0}" does not support waking on a magic packet.'
        IssueMagicDisabled     = 'The adapter "{0}" does not wake the PC on a magic packet.'
        IssueWakeNotArmed      = 'The adapter "{0}" is not allowed to wake the PC.'
        IssueFastStartup       = 'Windows fast startup is on: the PC cannot wake after a shutdown.'
        BiosReminder           = 'Also check that Wake-on-LAN is enabled in the BIOS/UEFI: this script cannot see it.'
        FixIntro               = 'Debout can fix this, as administrator:'
        FixMagicPacket         = 'Enable "Wake on Magic Packet" on "{0}"'
        FixWakeArmed           = 'Allow "{0}" to wake the PC'
        FixFastStartup         = 'Turn off fast startup'
        FixConfirm             = 'Apply these changes? [y/N]'
        FixApplied             = 'Changes applied.'
        FixSkipped             = 'Nothing changed.'
        FixFailed              = 'The changes could not be applied: {0}'
        Opening                = 'Opening the pairing page. Scan the QR code with Debout:'
    }
    fr = @{
        Title                  = 'Debout : appairer ce PC'
        NoAdapter              = 'Aucune carte réseau connectée avec une adresse IPv4. Connecte ce PC à ton réseau local et réessaie.'
        Adapter                = 'Carte'
        Mac                    = 'MAC'
        Ip                     = 'IP'
        Name                   = 'Nom'
        Checking               = 'Vérification du Wake-on-LAN...'
        AllGood                = 'Le Wake-on-LAN semble prêt sur ce PC.'
        IssueWifi              = 'Ce PC est en Wi-Fi. Le Wake-on-LAN marche rarement en Wi-Fi : branche un câble Ethernet si tu peux.'
        IssueMagicUnsupported  = 'La carte « {0} » ne sait pas réveiller le PC sur un magic packet.'
        IssueMagicDisabled     = 'La carte « {0} » ne réveille pas le PC sur un magic packet.'
        IssueWakeNotArmed      = 'La carte « {0} » n''a pas le droit de réveiller le PC.'
        IssueFastStartup       = 'Le démarrage rapide de Windows est activé : le PC ne peut pas se réveiller après un arrêt.'
        BiosReminder           = 'Vérifie aussi que le Wake-on-LAN est activé dans le BIOS/UEFI : ce script ne peut pas le voir.'
        FixIntro               = 'Debout peut corriger ça, en administrateur :'
        FixMagicPacket         = 'Activer « Wake on Magic Packet » sur « {0} »'
        FixWakeArmed           = 'Autoriser « {0} » à réveiller le PC'
        FixFastStartup         = 'Désactiver le démarrage rapide'
        FixConfirm             = 'Appliquer ces changements ? [o/N]'
        FixApplied             = 'Changements appliqués.'
        FixSkipped             = 'Rien n''a été modifié.'
        FixFailed              = 'Les changements n''ont pas pu être appliqués : {0}'
        Opening                = 'Ouverture de la page d''appairage. Scanne le QR code avec Debout :'
    }
}

function Get-DeboutLanguage {
    param([Globalization.CultureInfo] $Culture = [Globalization.CultureInfo]::CurrentUICulture)
    if ($Culture.TwoLetterISOLanguageName -eq 'fr') { 'fr' } else { 'en' }
}

function Format-MacAddress {
    param([Parameter(Mandatory)] [string] $MacAddress)
    ($MacAddress -replace '[^0-9A-Fa-f]', '').ToUpperInvariant() -replace '(..)(?!$)', '$1:'
}

<#
    Candidates are plain records, so the choice is testable without network adapters:
    Name, Description, MacAddress, IPv4, HasGateway, IsWifi.
#>
function Select-PairingAdapter {
    param([object[]] $Candidates)
    $Candidates |
        Where-Object { $_.IPv4 } |
        Sort-Object -Property @{ Expression = { $_.HasGateway }; Descending = $true },
                              @{ Expression = { $_.IsWifi }; Descending = $false } |
        Select-Object -First 1
}

function Get-PairingAdapterCandidate {
    # NdisPhysicalMedium 9 is NdisPhysicalMediumNative802_11.
    foreach ($adapter in Get-NetAdapter -Physical | Where-Object Status -EQ 'Up') {
        $ipv4 = Get-NetIPAddress -InterfaceIndex $adapter.ifIndex -AddressFamily IPv4 -ErrorAction SilentlyContinue |
            Where-Object { $_.IPAddress -notlike '169.254.*' } |
            Select-Object -First 1
        $gateway = Get-NetRoute -InterfaceIndex $adapter.ifIndex -DestinationPrefix '0.0.0.0/0' -ErrorAction SilentlyContinue
        [pscustomobject]@{
            Name        = $adapter.Name
            Description = $adapter.InterfaceDescription
            MacAddress  = Format-MacAddress $adapter.MacAddress
            IPv4        = if ($ipv4) { $ipv4.IPAddress } else { $null }
            HasGateway  = [bool] $gateway
            IsWifi      = $adapter.NdisPhysicalMedium -eq 9 -or $adapter.PhysicalMediaType -eq 'Native 802.11'
        }
    }
}

<# Names of the devices allowed to wake the PC, or $null when powercfg fails. #>
function Get-WakeArmedDevice {
    try {
        # Windows PowerShell turns a native command's stderr into a terminating error under 'Stop'.
        $devices = & powercfg.exe /devicequery wake_armed 2>$null
        if ($LASTEXITCODE -ne 0) { return $null }
        , @($devices | ForEach-Object { $_.Trim() } | Where-Object { $_ })
    } catch {
        Write-Verbose "powercfg: $_"
        $null
    }
}

<#
    Facts are $null when Windows would not tell: the check is then skipped rather than
    reported as a problem.
#>
function Get-WolFact {
    param([Parameter(Mandatory)] $Adapter)

    $magicPacket = $null
    try {
        $magicPacket = [string] (Get-NetAdapterPowerManagement -Name $Adapter.Name).WakeOnMagicPacket
    } catch {
        Write-Verbose "Get-NetAdapterPowerManagement: $_"
    }

    $armedDevices = Get-WakeArmedDevice
    $wakeArmed = if ($null -eq $armedDevices) { $null } else { $armedDevices -contains $Adapter.Description }

    $hiberboot = Get-ItemProperty -Path $SessionPowerKey -Name HiberbootEnabled -ErrorAction SilentlyContinue
    $hibernate = Get-ItemProperty -Path $PowerKey -Name HibernateEnabled -ErrorAction SilentlyContinue
    # Fast startup relies on hibernation: with hibernation off, the flag has no effect.
    $fastStartup = [bool] ($hiberboot -and $hiberboot.HiberbootEnabled -eq 1 -and
        -not ($hibernate -and $hibernate.HibernateEnabled -eq 0))

    [pscustomobject]@{
        MagicPacket = $magicPacket
        WakeArmed   = $wakeArmed
        FastStartup = $fastStartup
    }
}

function Get-WolIssue {
    param(
        [Parameter(Mandatory)] $Adapter,
        [Parameter(Mandatory)] $Facts
    )
    if ($Adapter.IsWifi) { [pscustomobject]@{ Id = 'Wifi'; Fixable = $false } }
    switch ($Facts.MagicPacket) {
        'Unsupported' { [pscustomobject]@{ Id = 'MagicUnsupported'; Fixable = $false } }
        'Disabled' { [pscustomobject]@{ Id = 'MagicDisabled'; Fixable = $true } }
    }
    if ($Facts.WakeArmed -eq $false) { [pscustomobject]@{ Id = 'WakeNotArmed'; Fixable = $true } }
    if ($Facts.FastStartup) { [pscustomobject]@{ Id = 'FastStartup'; Fixable = $true } }
}

function ConvertTo-PowerShellLiteral {
    param([Parameter(Mandatory)] [AllowEmptyString()] [string] $Value)
    "'" + ($Value -replace "'", "''") + "'"
}

function Get-WolFixScript {
    param(
        [Parameter(Mandatory)] $Adapter,
        [Parameter(Mandatory)] [object[]] $Issues
    )
    $lines = foreach ($issue in $Issues) {
        switch ($issue.Id) {
            'MagicDisabled' {
                "Set-NetAdapterPowerManagement -Name $(ConvertTo-PowerShellLiteral $Adapter.Name) -WakeOnMagicPacket Enabled"
            }
            'WakeNotArmed' {
                "powercfg.exe /deviceenablewake $(ConvertTo-PowerShellLiteral $Adapter.Description)"
            }
            'FastStartup' {
                "Set-ItemProperty -Path $(ConvertTo-PowerShellLiteral $SessionPowerKey) -Name HiberbootEnabled -Value 0"
            }
        }
    }
    (@('$ErrorActionPreference = ''Stop''') + $lines) -join "`r`n"
}

function Get-PairingUrl {
    param(
        [Parameter(Mandatory)] [string] $SiteUrl,
        [Parameter(Mandatory)] $Adapter,
        [Parameter(Mandatory)] [string] $HostName
    )
    $query = [ordered]@{
        mac  = $Adapter.MacAddress
        ip   = $Adapter.IPv4
        name = $HostName
        os   = 'windows'
    }
    $pairs = foreach ($key in $query.Keys) { "$key=$([uri]::EscapeDataString($query[$key]))" }
    "$($SiteUrl.TrimEnd('/'))/pair/#v1?$($pairs -join '&')"
}

function Test-Confirmation {
    param([string] $Answer)
    $Answer.Trim().ToLowerInvariant() -in @('y', 'yes', 'o', 'oui')
}

function Test-Administrator {
    $principal = [Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Invoke-Elevated {
    param([Parameter(Mandatory)] [string] $Script)
    if (Test-Administrator) {
        & ([scriptblock]::Create($Script))
        return
    }
    $encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($Script))
    $process = Start-Process -FilePath powershell.exe -Verb RunAs -Wait -PassThru `
        -ArgumentList '-NoProfile', '-ExecutionPolicy', 'Bypass', '-EncodedCommand', $encoded
    if ($process.ExitCode -ne 0) { throw "exit code $($process.ExitCode)" }
}

function Invoke-DeboutPairing {
    param([string] $SiteUrl)

    $text = $Messages[(Get-DeboutLanguage)]
    Write-Host ''
    Write-Host $text.Title -ForegroundColor Yellow
    Write-Host ''

    $adapter = Select-PairingAdapter @(Get-PairingAdapterCandidate)
    if (-not $adapter) {
        Write-Host $text.NoAdapter -ForegroundColor Red
        return
    }
    $hostName = [Net.Dns]::GetHostName()
    Write-Host ("{0,-8} {1} ({2})" -f $text.Adapter, $adapter.Name, $adapter.Description)
    Write-Host ("{0,-8} {1}" -f $text.Mac, $adapter.MacAddress)
    Write-Host ("{0,-8} {1}" -f $text.Ip, $adapter.IPv4)
    Write-Host ("{0,-8} {1}" -f $text.Name, $hostName)
    Write-Host ''

    Write-Host $text.Checking
    $issues = @(Get-WolIssue -Adapter $adapter -Facts (Get-WolFact $adapter))
    if ($issues.Count -eq 0) {
        Write-Host "  $($text.AllGood)" -ForegroundColor Green
    }
    foreach ($issue in $issues) {
        Write-Host ("  ! " + ($text["Issue$($issue.Id)"] -f $adapter.Description)) -ForegroundColor Yellow
    }
    Write-Host "  $($text.BiosReminder)"
    Write-Host ''

    $fixable = @($issues | Where-Object Fixable)
    if ($fixable.Count -gt 0) {
        Write-Host $text.FixIntro
        foreach ($issue in $fixable) {
            $label = switch ($issue.Id) {
                'MagicDisabled' { $text.FixMagicPacket -f $adapter.Description }
                'WakeNotArmed' { $text.FixWakeArmed -f $adapter.Description }
                'FastStartup' { $text.FixFastStartup }
            }
            Write-Host "  - $label"
        }
        if (Test-Confirmation (Read-Host $text.FixConfirm)) {
            try {
                Invoke-Elevated (Get-WolFixScript -Adapter $adapter -Issues $fixable)
                Write-Host $text.FixApplied -ForegroundColor Green
            } catch {
                Write-Host ($text.FixFailed -f $_.Exception.Message) -ForegroundColor Red
            }
        } else {
            Write-Host $text.FixSkipped
        }
        Write-Host ''
    }

    $url = Get-PairingUrl -SiteUrl $SiteUrl -Adapter $adapter -HostName $hostName
    Write-Host $text.Opening
    Write-Host "  $url" -ForegroundColor Cyan
    Start-Process $url
}

# Dot-sourcing (as the tests do) only loads the functions.
if ($MyInvocation.InvocationName -ne '.') {
    Invoke-DeboutPairing -SiteUrl $SiteUrl
}
