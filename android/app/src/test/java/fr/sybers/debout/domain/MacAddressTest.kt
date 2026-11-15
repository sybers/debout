package fr.sybers.debout.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MacAddressTest {

    @Test
    fun `parses colon-separated uppercase MAC`() {
        val result = MacAddress.parse("AA:BB:CC:DD:EE:FF")

        assertEquals(MacAddressParseResult.Valid(MacAddress(0xAABBCCDDEEFF)), result)
    }

    @Test
    fun `parses dash-separated lowercase MAC`() {
        val result = MacAddress.parse("aa-bb-cc-dd-ee-ff")

        assertEquals(MacAddressParseResult.Valid(MacAddress(0xAABBCCDDEEFF)), result)
    }

    @Test
    fun `parses MAC without separators`() {
        val result = MacAddress.parse("aabbccddeeff")

        assertEquals(MacAddressParseResult.Valid(MacAddress(0xAABBCCDDEEFF)), result)
    }

    @Test
    fun `ignores whitespace around pasted MAC`() {
        val result = MacAddress.parse("  AA:BB:CC:DD:EE:FF\n")

        assertEquals(MacAddressParseResult.Valid(MacAddress(0xAABBCCDDEEFF)), result)
    }

    @Test
    fun `rejects MAC with too few digits`() {
        val result = MacAddress.parse("AA:BB:CC:DD:EE")

        assertEquals(MacAddressParseResult.Invalid(MacAddressError.TooShort), result)
    }

    @Test
    fun `rejects MAC with too many digits`() {
        val result = MacAddress.parse("AA:BB:CC:DD:EE:FF:00")

        assertEquals(MacAddressParseResult.Invalid(MacAddressError.TooLong), result)
    }

    @Test
    fun `rejects non-hexadecimal characters`() {
        val result = MacAddress.parse("AA:BB:CC:DD:EE:GG")

        assertEquals(MacAddressParseResult.Invalid(MacAddressError.InvalidCharacter), result)
    }

    @Test
    fun `formats as colon-separated uppercase pairs`() {
        assertEquals("0A:1B:2C:3D:4E:5F", MacAddress(0x0A1B2C3D4E5F).toString())
    }

    @Test
    fun `rejects a sign prefix that number parsing would accept`() {
        val result = MacAddress.parse("+ABBCCDDEEFF")

        assertEquals(MacAddressParseResult.Invalid(MacAddressError.InvalidCharacter), result)
    }
}
