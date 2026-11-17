#Requires -Modules @{ ModuleName = 'Pester'; ModuleVersion = '5.0' }

BeforeAll {
    . $PSScriptRoot/pair.ps1

    function New-Candidate {
        param(
            [string] $Name = 'Ethernet',
            [string] $IPv4 = '192.168.1.42',
            [bool] $HasGateway = $true,
            [bool] $IsWifi = $false
        )
        [pscustomobject]@{
            Name        = $Name
            Description = "$Name adapter"
            MacAddress  = 'AA:BB:CC:DD:EE:FF'
            IPv4        = if ($IPv4) { $IPv4 } else { $null }
            HasGateway  = $HasGateway
            IsWifi      = $IsWifi
        }
    }

    function New-Facts {
        param($MagicPacket = 'Enabled', $WakeArmed = $true, [bool] $FastStartup = $false)
        [pscustomobject]@{ MagicPacket = $MagicPacket; WakeArmed = $WakeArmed; FastStartup = $FastStartup }
    }
}

Describe 'Get-DeboutLanguage' {
    It 'speaks French to <Culture>' -ForEach @(@{ Culture = 'fr-FR' }, @{ Culture = 'fr-CA' }, @{ Culture = 'fr-BE' }) {
        Get-DeboutLanguage -Culture $Culture | Should -Be 'fr'
    }

    It 'speaks English to <Culture>' -ForEach @(@{ Culture = 'en-US' }, @{ Culture = 'de-DE' }, @{ Culture = '' }) {
        Get-DeboutLanguage -Culture $Culture | Should -Be 'en'
    }

    It 'has every message in both languages' {
        $Messages.fr.Keys | Sort-Object | Should -Be ($Messages.en.Keys | Sort-Object)
    }
}

Describe 'Format-MacAddress' {
    It 'turns <Raw> into colon notation' -ForEach @(
        @{ Raw = 'AA-BB-CC-DD-EE-FF' }
        @{ Raw = 'aa-bb-cc-dd-ee-ff' }
        @{ Raw = 'AABBCCDDEEFF' }
    ) {
        Format-MacAddress $Raw | Should -BeExactly 'AA:BB:CC:DD:EE:FF'
    }
}

Describe 'Select-PairingAdapter' {
    It 'prefers Ethernet over Wi-Fi' {
        $wifi = New-Candidate -Name 'Wi-Fi' -IsWifi $true
        $ethernet = New-Candidate -Name 'Ethernet'

        (Select-PairingAdapter @($wifi, $ethernet)).Name | Should -Be 'Ethernet'
    }

    It 'prefers the adapter that reaches the router over a direct link' {
        $direct = New-Candidate -Name 'Ethernet' -HasGateway $false
        $wifi = New-Candidate -Name 'Wi-Fi' -IsWifi $true

        (Select-PairingAdapter @($direct, $wifi)).Name | Should -Be 'Wi-Fi'
    }

    It 'skips adapters without an IPv4 address' {
        $noIp = New-Candidate -Name 'Ethernet' -IPv4 ''
        $wifi = New-Candidate -Name 'Wi-Fi' -IsWifi $true

        (Select-PairingAdapter @($noIp, $wifi)).Name | Should -Be 'Wi-Fi'
    }

    It 'finds nothing without a usable adapter' {
        Select-PairingAdapter @() | Should -BeNullOrEmpty
        Select-PairingAdapter @(New-Candidate -IPv4 '') | Should -BeNullOrEmpty
    }
}

Describe 'Get-PairingAdapterCandidate' {
    BeforeAll {
        # Lets the mocks below exist on machines without the NetAdapter module.
        foreach ($command in 'Get-NetAdapter', 'Get-NetIPAddress', 'Get-NetRoute') {
            if (-not (Get-Command $command -ErrorAction SilentlyContinue)) {
                Set-Item "function:global:$command" { param([switch] $Physical, $InterfaceIndex, $AddressFamily, $DestinationPrefix) }
            }
        }
    }

    It 'describes each connected physical adapter' {
        Mock Get-NetAdapter {
            [pscustomobject]@{ Name = 'Ethernet'; InterfaceDescription = 'Intel(R) I219-V'; MacAddress = 'AA-BB-CC-DD-EE-FF'; ifIndex = 3; Status = 'Up'; NdisPhysicalMedium = 14; PhysicalMediaType = '802.3' }
            [pscustomobject]@{ Name = 'Wi-Fi'; InterfaceDescription = 'Intel(R) Wi-Fi 6'; MacAddress = '11-22-33-44-55-66'; ifIndex = 7; Status = 'Up'; NdisPhysicalMedium = 9; PhysicalMediaType = 'Native 802.11' }
            [pscustomobject]@{ Name = 'Ethernet 2'; InterfaceDescription = 'Realtek'; MacAddress = '00-00-00-00-00-01'; ifIndex = 9; Status = 'Disconnected'; NdisPhysicalMedium = 14; PhysicalMediaType = '802.3' }
        }
        Mock Get-NetIPAddress { [pscustomobject]@{ IPAddress = '169.254.10.1' }; [pscustomobject]@{ IPAddress = '192.168.1.42' } } -ParameterFilter { $InterfaceIndex -eq 3 }
        Mock Get-NetIPAddress { [pscustomobject]@{ IPAddress = '192.168.1.43' } } -ParameterFilter { $InterfaceIndex -eq 7 }
        Mock Get-NetRoute { [pscustomobject]@{ NextHop = '192.168.1.1' } }

        $candidates = @(Get-PairingAdapterCandidate)

        $candidates.Count | Should -Be 2
        $candidates[0].MacAddress | Should -Be 'AA:BB:CC:DD:EE:FF'
        $candidates[0].IPv4 | Should -Be '192.168.1.42'
        $candidates[0].HasGateway | Should -BeTrue
        $candidates[0].IsWifi | Should -BeFalse
        $candidates[1].IsWifi | Should -BeTrue
    }
}

Describe 'Get-WolFact' {
    BeforeAll {
        if (-not (Get-Command Get-NetAdapterPowerManagement -ErrorAction SilentlyContinue)) {
            Set-Item 'function:global:Get-NetAdapterPowerManagement' { param($Name) }
        }
        $adapter = New-Candidate
    }

    BeforeEach {
        Mock Get-NetAdapterPowerManagement { [pscustomobject]@{ WakeOnMagicPacket = 'Enabled' } }
        Mock Get-WakeArmedDevice { , @('HID Keyboard Device', 'Ethernet adapter') }
        Mock Get-ItemProperty { [pscustomobject]@{ HiberbootEnabled = 1 } } -ParameterFilter { $Name -eq 'HiberbootEnabled' }
        Mock Get-ItemProperty { [pscustomobject]@{ HibernateEnabled = 1 } } -ParameterFilter { $Name -eq 'HibernateEnabled' }
    }

    It 'reads the magic packet setting and the wake permission' {
        $facts = Get-WolFact $adapter

        $facts.MagicPacket | Should -Be 'Enabled'
        $facts.WakeArmed | Should -BeTrue
    }

    It 'sees an adapter missing from the wake-armed devices' {
        Mock Get-WakeArmedDevice { , @('NONE') }

        (Get-WolFact $adapter).WakeArmed | Should -BeFalse
    }

    It 'skips the checks Windows would not answer' {
        Mock Get-NetAdapterPowerManagement { throw 'Access denied' }
        Mock Get-WakeArmedDevice { $null }

        $facts = Get-WolFact $adapter

        $facts.MagicPacket | Should -BeNullOrEmpty
        $facts.WakeArmed | Should -BeNullOrEmpty
    }

    It 'sees fast startup when hibernation backs it' {
        (Get-WolFact $adapter).FastStartup | Should -BeTrue
    }

    It 'ignores fast startup when hibernation is off' {
        Mock Get-ItemProperty { [pscustomobject]@{ HibernateEnabled = 0 } } -ParameterFilter { $Name -eq 'HibernateEnabled' }

        (Get-WolFact $adapter).FastStartup | Should -BeFalse
    }

    It 'sees no fast startup when it is off' {
        Mock Get-ItemProperty { [pscustomobject]@{ HiberbootEnabled = 0 } } -ParameterFilter { $Name -eq 'HiberbootEnabled' }

        (Get-WolFact $adapter).FastStartup | Should -BeFalse
    }
}

Describe 'Get-WolIssue' {
    It 'finds nothing on a ready wired PC' {
        Get-WolIssue -Adapter (New-Candidate) -Facts (New-Facts) | Should -BeNullOrEmpty
    }

    It 'warns about Wi-Fi, which it cannot fix' {
        $issues = @(Get-WolIssue -Adapter (New-Candidate -IsWifi $true) -Facts (New-Facts))

        $issues.Count | Should -Be 1
        $issues[0].Id | Should -Be 'Wifi'
        $issues[0].Fixable | Should -BeFalse
    }

    It 'offers to fix a disabled magic packet, a disarmed adapter and fast startup' {
        $issues = @(Get-WolIssue -Adapter (New-Candidate) -Facts (New-Facts -MagicPacket 'Disabled' -WakeArmed $false -FastStartup $true))

        $issues.Id | Should -Be @('MagicDisabled', 'WakeNotArmed', 'FastStartup')
        $issues.Fixable | Should -Be @($true, $true, $true)
    }

    It 'cannot fix an adapter without magic packet support' {
        $issues = @(Get-WolIssue -Adapter (New-Candidate) -Facts (New-Facts -MagicPacket 'Unsupported'))

        $issues.Count | Should -Be 1
        $issues[0].Id | Should -Be 'MagicUnsupported'
        $issues[0].Fixable | Should -BeFalse
    }

    It 'stays quiet about checks that could not run' {
        Get-WolIssue -Adapter (New-Candidate) -Facts (New-Facts -MagicPacket $null -WakeArmed $null) | Should -BeNullOrEmpty
    }
}

Describe 'Get-WolFixScript' {
    It 'fixes exactly the issues it is given' {
        $issues = @([pscustomobject]@{ Id = 'MagicDisabled'; Fixable = $true }, [pscustomobject]@{ Id = 'FastStartup'; Fixable = $true })

        $script = Get-WolFixScript -Adapter (New-Candidate) -Issues $issues

        $script | Should -Match "Set-NetAdapterPowerManagement -Name 'Ethernet' -WakeOnMagicPacket Enabled"
        $script | Should -Match 'HiberbootEnabled -Value 0'
        $script | Should -Not -Match 'deviceenablewake'
    }

    It 'quotes adapter names safely' {
        $adapter = New-Candidate -Name "Bob's LAN"
        $adapter.Description = "Bob's adapter; Remove-Item C:\"
        $issues = @([pscustomobject]@{ Id = 'MagicDisabled'; Fixable = $true }, [pscustomobject]@{ Id = 'WakeNotArmed'; Fixable = $true })

        $script = Get-WolFixScript -Adapter $adapter -Issues $issues

        $script | Should -Match ([regex]::Escape("-Name 'Bob''s LAN'"))
        $script | Should -Match ([regex]::Escape("/deviceenablewake 'Bob''s adapter; Remove-Item C:\'"))
        { [scriptblock]::Create($script) } | Should -Not -Throw
    }
}

Describe 'Get-PairingUrl' {
    It 'puts the machine in the fragment, never in the query' {
        $url = Get-PairingUrl -SiteUrl 'https://debout.sybers.fr/' -Adapter (New-Candidate) -HostName 'SALON-PC'

        $url | Should -BeExactly 'https://debout.sybers.fr/pair/#v1?mac=AA%3ABB%3ACC%3ADD%3AEE%3AFF&ip=192.168.1.42&name=SALON-PC&os=windows'
    }

    It 'escapes the host name' {
        $url = Get-PairingUrl -SiteUrl 'https://debout.sybers.fr' -Adapter (New-Candidate) -HostName 'Mon PC&co'

        $url | Should -Match ([regex]::Escape('name=Mon%20PC%26co&'))
    }
}

Describe 'Test-Confirmation' {
    It 'accepts <Answer>' -ForEach @(@{ Answer = 'y' }, @{ Answer = 'Yes' }, @{ Answer = 'o' }, @{ Answer = ' OUI ' }) {
        Test-Confirmation $Answer | Should -BeTrue
    }

    It 'refuses <Answer>' -ForEach @(@{ Answer = '' }, @{ Answer = 'n' }, @{ Answer = 'non' }, @{ Answer = 'yolo' }) {
        Test-Confirmation $Answer | Should -BeFalse
    }
}
