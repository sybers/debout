package fr.sybers.debout.ui.help

import fr.sybers.debout.domain.MachineType
import org.junit.Assert.assertEquals
import org.junit.Test

class HelpOsTest {

    @Test
    fun `opens the checklist of the system the machine most likely runs`() {
        assertEquals(HelpOs.MacOs, HelpOs.likelyFor(MachineType.Mac))
        assertEquals(HelpOs.Linux, HelpOs.likelyFor(MachineType.Server))
        assertEquals(HelpOs.Linux, HelpOs.likelyFor(MachineType.Nas))
        assertEquals(HelpOs.Windows, HelpOs.likelyFor(MachineType.Desktop))
        assertEquals(HelpOs.Windows, HelpOs.likelyFor(MachineType.Laptop))
        assertEquals(HelpOs.Windows, HelpOs.likelyFor(MachineType.Other))
    }

    @Test
    fun `round-trips through its anchor, falling back to Windows`() {
        HelpOs.entries.forEach { assertEquals(it, HelpOs.fromAnchor(it.anchor)) }
        assertEquals(HelpOs.Windows, HelpOs.fromAnchor("beos"))
        assertEquals(HelpOs.Windows, HelpOs.fromAnchor(null))
    }

    @Test
    fun `links to the matching section of the site`() {
        assertEquals("https://debout.sybers.fr/help/#macos", HelpOs.MacOs.helpUrl)
    }
}
