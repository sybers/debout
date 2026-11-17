package fr.sybers.debout.ui.help

import fr.sybers.debout.domain.MachineType

/** [anchor] is the section of the site's help page. */
enum class HelpOs(val anchor: String) {
    Windows("windows"),
    MacOs("macos"),
    Linux("linux");

    val helpUrl: String get() = "https://debout.sybers.fr/help/#$anchor"

    companion object {
        /** The checklist to open first: the type is a guess, the screen lets the user switch. */
        fun likelyFor(type: MachineType): HelpOs = when (type) {
            MachineType.Mac -> MacOs
            MachineType.Server, MachineType.Nas -> Linux
            MachineType.Desktop, MachineType.Laptop, MachineType.Other -> Windows
        }

        fun fromAnchor(anchor: String?): HelpOs = entries.firstOrNull { it.anchor == anchor } ?: Windows
    }
}
