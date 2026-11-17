package fr.sybers.debout.domain

import java.net.URLDecoder

/** What a pairing QR code carries: `debout://v1?mac=…&ip=…&name=…&os=…`. [name] is the machine's hostname. */
data class PairingPayload(
    val mac: MacAddress,
    val ip: String?,
    val name: String?,
    val type: MachineType,
) {
    companion object {
        private const val PREFIX = "debout://"
        private const val SUPPORTED_VERSION = "v1"
        private val version = Regex("""v\d+""", RegexOption.IGNORE_CASE)

        fun parse(text: String): PairingParseResult {
            val trimmed = text.trim()
            if (!trimmed.startsWith(PREFIX, ignoreCase = true)) return PairingParseResult.Invalid
            val rest = trimmed.substring(PREFIX.length)
            val payloadVersion = rest.substringBefore('?')
            if (!version.matches(payloadVersion)) return PairingParseResult.Invalid
            if (!payloadVersion.equals(SUPPORTED_VERSION, ignoreCase = true)) return PairingParseResult.UnsupportedVersion

            val params = parseQuery(rest.substringAfter('?', missingDelimiterValue = ""))
                ?: return PairingParseResult.Invalid
            val mac = params["mac"]?.let { MacAddress.parse(it) as? MacAddressParseResult.Valid }
                ?: return PairingParseResult.Invalid
            val ip = params["ip"]?.trim()?.ifEmpty { null }
            if (ip != null && Ipv4.parse(ip) == null) return PairingParseResult.Invalid

            return PairingParseResult.Valid(
                PairingPayload(
                    mac = mac.mac,
                    ip = ip,
                    name = params["name"]?.trim()?.ifEmpty { null },
                    type = typeFor(params["os"]),
                ),
            )
        }

        /** Null when a key repeats or a value is badly encoded: the code was not made by a Debout script. */
        private fun parseQuery(query: String): Map<String, String>? {
            val params = mutableMapOf<String, String>()
            for (pair in query.split('&').filter { it.isNotEmpty() }) {
                val key = pair.substringBefore('=')
                val value = try {
                    URLDecoder.decode(pair.substringAfter('=', missingDelimiterValue = ""), "UTF-8")
                } catch (_: IllegalArgumentException) {
                    return null
                }
                if (params.put(key, value) != null) return null
            }
            return params
        }

        private fun typeFor(os: String?): MachineType = when (os?.lowercase()) {
            "windows", "linux" -> MachineType.Desktop
            "macos" -> MachineType.Mac
            else -> MachineType.Other
        }
    }
}

sealed interface PairingParseResult {
    data class Valid(val payload: PairingPayload) : PairingParseResult

    /** Made by a newer version of the pairing scripts: the app needs an update. */
    data object UnsupportedVersion : PairingParseResult

    data object Invalid : PairingParseResult
}
