package fr.sybers.debout.ui.machine

import fr.sybers.debout.domain.MacAddress
import fr.sybers.debout.domain.Machine
import fr.sybers.debout.domain.MachineType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MachineFormTest {

    @Test
    fun `an untouched empty form shows no errors`() {
        val form = MachineForm()

        assertNull(form.nameError)
        assertNull(form.macError)
    }

    @Test
    fun `submitting an empty form flags name and MAC as required`() {
        val form = MachineForm().submitted()

        assertEquals(FormError.Required, form.nameError)
        assertEquals(FormError.Required, form.macError)
    }

    @Test
    fun `a MAC being typed is not flagged until submit`() {
        val form = MachineForm(mac = "AA:BB:C")

        assertNull(form.macError)
        assertEquals(FormError.MacIncomplete, form.submitted().macError)
    }

    @Test
    fun `a non-hexadecimal MAC character is flagged while typing`() {
        assertEquals(FormError.MacInvalidCharacter, MachineForm(mac = "AA:BB:XX").macError)
    }

    @Test
    fun `a MAC with too many digits is flagged while typing`() {
        assertEquals(FormError.MacTooLong, MachineForm(mac = "AA:BB:CC:DD:EE:FF:0").macError)
    }

    @Test
    fun `IP and broadcast are optional`() {
        val form = MachineForm(ip = "", broadcast = "").submitted()

        assertNull(form.ipError)
        assertNull(form.broadcastError)
    }

    @Test
    fun `IP and broadcast must be IPv4 literals once submitted`() {
        val form = MachineForm(ip = "pc-salon", broadcast = "192.168.1")

        assertNull(form.ipError)
        assertEquals(FormError.InvalidIpv4, form.submitted().ipError)
        assertEquals(FormError.InvalidIpv4, form.submitted().broadcastError)
    }

    @Test
    fun `WoL port defaults to 9 and must be in 1-65535`() {
        assertNull(MachineForm().submitted().wolPortError)
        listOf("", "0", "65536", "neuf").forEach {
            assertEquals(it, FormError.InvalidPort, MachineForm(wolPort = it).submitted().wolPortError)
        }
    }

    @Test
    fun `status port is optional but must be in 1-65535 when given`() {
        assertNull(MachineForm().submitted().probePortError)
        listOf("0", "65536", "ssh").forEach {
            assertEquals(it, FormError.InvalidPort, MachineForm(probePort = it).submitted().probePortError)
        }
    }

    @Test
    fun `a status port typed by the user is kept`() {
        val machine = MachineForm(name = "NAS", mac = "001122334455", probePort = " 8080 ").toMachine()

        assertEquals(8080, machine?.probePort)
    }

    @Test
    fun `a valid form becomes a machine with blanks as absent values`() {
        val form = MachineForm(
            name = "  PC du salon ",
            type = MachineType.Desktop,
            mac = "aa-bb-cc-dd-ee-ff",
            ip = "192.168.1.42",
            hostname = " ",
            broadcast = "",
            wolPort = "7",
            favorite = true,
        )

        assertEquals(
            Machine(
                name = "PC du salon",
                type = MachineType.Desktop,
                mac = MacAddress(0xAABBCCDDEEFF),
                ip = "192.168.1.42",
                hostname = null,
                broadcast = null,
                wolPort = 7,
                favorite = true,
            ),
            form.toMachine(),
        )
    }

    @Test
    fun `an invalid form does not become a machine`() {
        assertNull(MachineForm(name = "PC", mac = "AA:BB").toMachine())
    }

    @Test
    fun `editing a machine keeps what the form does not show`() {
        val machine = Machine(
            id = 4,
            name = "NAS",
            type = MachineType.Nas,
            mac = MacAddress(0x001122334455),
            ip = "10.0.0.2",
            hostname = "nas.local",
            broadcast = "10.0.0.255",
            wolPort = 9,
            probePort = 445,
            favorite = false,
            order = 3,
        )

        assertEquals(machine, MachineForm.from(machine).toMachine(base = machine))
    }
}
