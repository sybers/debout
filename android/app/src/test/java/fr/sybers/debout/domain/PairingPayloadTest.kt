package fr.sybers.debout.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PairingPayloadTest {

    private val mac = MacAddress(0xAABBCCDDEEFF)

    @Test
    fun `parses a complete v1 payload`() {
        val result = PairingPayload.parse("debout://v1?mac=AA:BB:CC:DD:EE:FF&ip=192.168.1.42&name=SALON-PC&os=windows")

        assertEquals(
            PairingParseResult.Valid(PairingPayload(mac, ip = "192.168.1.42", name = "SALON-PC", type = MachineType.Desktop)),
            result,
        )
    }

    @Test
    fun `decodes percent-encoded values`() {
        val result = PairingPayload.parse("debout://v1?mac=AA%3ABB%3ACC%3ADD%3AEE%3AFF&name=Mon%20PC")

        assertEquals(PairingParseResult.Valid(PairingPayload(mac, ip = null, name = "Mon PC", type = MachineType.Other)), result)
    }

    @Test
    fun `accepts any MAC notation the manual entry accepts`() {
        val result = PairingPayload.parse("debout://v1?mac=aa-bb-cc-dd-ee-ff")

        assertEquals(PairingParseResult.Valid(PairingPayload(mac, ip = null, name = null, type = MachineType.Other)), result)
    }

    @Test
    fun `guesses the machine type from the OS`() {
        fun typeFor(os: String) = (PairingPayload.parse("debout://v1?mac=aabbccddeeff&os=$os") as PairingParseResult.Valid).payload.type

        assertEquals(MachineType.Desktop, typeFor("windows"))
        assertEquals(MachineType.Mac, typeFor("macos"))
        assertEquals(MachineType.Desktop, typeFor("linux"))
        assertEquals(MachineType.Other, typeFor("beos"))
    }

    @Test
    fun `reads the network name apart from the display name`() {
        val result = PairingPayload.parse("debout://v1?mac=aabbccddeeff&name=Mac%20mini%20de%20Stan&os=macos&host=Mac-mini.local")

        assertEquals(
            PairingParseResult.Valid(
                PairingPayload(mac, ip = null, name = "Mac mini de Stan", type = MachineType.Mac, host = "Mac-mini.local"),
            ),
            result,
        )
    }

    @Test
    fun `ignores parameters it does not know, so v1 can grow`() {
        val result = PairingPayload.parse("debout://v1?mac=aabbccddeeff&chassis=laptop")

        assertEquals(PairingParseResult.Valid(PairingPayload(mac, ip = null, name = null, type = MachineType.Other)), result)
    }

    @Test
    fun `treats a blank name as absent`() {
        val result = PairingPayload.parse("debout://v1?mac=aabbccddeeff&name=%20")

        assertEquals(PairingParseResult.Valid(PairingPayload(mac, ip = null, name = null, type = MachineType.Other)), result)
    }

    @Test
    fun `accepts surrounding whitespace and an uppercase scheme`() {
        val result = PairingPayload.parse("  DEBOUT://V1?mac=aabbccddeeff\n")

        assertEquals(PairingParseResult.Valid(PairingPayload(mac, ip = null, name = null, type = MachineType.Other)), result)
    }

    @Test
    fun `rejects a newer version as unsupported`() {
        assertEquals(PairingParseResult.UnsupportedVersion, PairingPayload.parse("debout://v2?mac=aabbccddeeff&key=abc"))
        assertEquals(PairingParseResult.UnsupportedVersion, PairingPayload.parse("debout://v12"))
    }

    @Test
    fun `rejects a payload without a valid MAC`() {
        assertEquals(PairingParseResult.Invalid, PairingPayload.parse("debout://v1?ip=192.168.1.42"))
        assertEquals(PairingParseResult.Invalid, PairingPayload.parse("debout://v1?mac=aabbcc"))
        assertEquals(PairingParseResult.Invalid, PairingPayload.parse("debout://v1?mac=aabbccddeeff&mac=112233445566"))
    }

    @Test
    fun `rejects an IP that is not IPv4`() {
        assertEquals(PairingParseResult.Invalid, PairingPayload.parse("debout://v1?mac=aabbccddeeff&ip=salon.local"))
    }

    @Test
    fun `rejects broken percent-encoding`() {
        assertEquals(PairingParseResult.Invalid, PairingPayload.parse("debout://v1?mac=aabbccddeeff&name=%G1"))
    }

    @Test
    fun `rejects anything that is not a Debout payload`() {
        assertEquals(PairingParseResult.Invalid, PairingPayload.parse("https://debout.sybers.fr"))
        assertEquals(PairingParseResult.Invalid, PairingPayload.parse("debout://pair?mac=aabbccddeeff"))
        assertEquals(PairingParseResult.Invalid, PairingPayload.parse("WIFI:S:home;T:WPA;P:secret;;"))
        assertEquals(PairingParseResult.Invalid, PairingPayload.parse(""))
    }
}
