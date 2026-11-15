package fr.sybers.debout.ui.machine

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import fr.sybers.debout.R
import fr.sybers.debout.domain.MachineType

@get:StringRes
val MachineType.label: Int
    get() = when (this) {
        MachineType.Desktop -> R.string.type_desktop
        MachineType.Laptop -> R.string.type_laptop
        MachineType.Server -> R.string.type_server
        MachineType.Nas -> R.string.type_nas
        MachineType.Mac -> R.string.type_mac
        MachineType.Other -> R.string.type_other
    }

@get:DrawableRes
val MachineType.icon: Int
    get() = when (this) {
        MachineType.Desktop -> R.drawable.ic_type_desktop
        MachineType.Laptop -> R.drawable.ic_type_laptop
        MachineType.Server -> R.drawable.ic_type_server
        MachineType.Nas -> R.drawable.ic_type_nas
        MachineType.Mac -> R.drawable.ic_type_mac
        MachineType.Other -> R.drawable.ic_type_other
    }

@get:StringRes
val FormError.message: Int
    get() = when (this) {
        FormError.Required -> R.string.error_required
        FormError.MacIncomplete -> R.string.error_mac_incomplete
        FormError.MacInvalidCharacter -> R.string.error_mac_invalid_character
        FormError.MacTooLong -> R.string.error_mac_too_long
        FormError.InvalidIpv4 -> R.string.error_ipv4
        FormError.InvalidPort -> R.string.error_port
    }
