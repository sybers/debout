package fr.sybers.debout.domain

@JvmInline
value class MacAddress(val value: Long) {

    fun toBytes(): ByteArray = ByteArray(6) { (value shr (8 * (5 - it))).toByte() }

    companion object {
        private const val HEX_DIGITS = 12

        fun parse(input: String): MacAddressParseResult {
            val hex = input.trim().replace(":", "").replace("-", "")
            if (!hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
                return MacAddressParseResult.Invalid(MacAddressError.InvalidCharacter)
            }
            if (hex.length != HEX_DIGITS) return MacAddressParseResult.Invalid(MacAddressError.WrongLength)
            return MacAddressParseResult.Valid(MacAddress(hex.toLong(16)))
        }
    }
}

sealed interface MacAddressParseResult {
    data class Valid(val mac: MacAddress) : MacAddressParseResult
    data class Invalid(val error: MacAddressError) : MacAddressParseResult
}

enum class MacAddressError {
    WrongLength,
    InvalidCharacter,
}
