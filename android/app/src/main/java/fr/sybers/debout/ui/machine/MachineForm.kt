package fr.sybers.debout.ui.machine

import fr.sybers.debout.domain.Ipv4
import fr.sybers.debout.domain.MacAddress
import fr.sybers.debout.domain.MacAddressError
import fr.sybers.debout.domain.MacAddressParseResult
import fr.sybers.debout.domain.Machine
import fr.sybers.debout.domain.MachineType

enum class FormError { Required, MacIncomplete, MacInvalidCharacter, MacTooLong, InvalidIpv4, InvalidPort }

/**
 * Errors the user can still fix by typing more stay hidden until [submitted],
 * so the form does not complain mid-keystroke.
 */
data class MachineForm(
    val name: String = "",
    val type: MachineType = MachineType.Desktop,
    val mac: String = "",
    val ip: String = "",
    val hostname: String = "",
    val broadcast: String = "",
    val wolPort: String = Machine.DEFAULT_WOL_PORT.toString(),
    val favorite: Boolean = false,
    val submitAttempted: Boolean = false,
) {
    val nameError: FormError?
        get() = if (submitAttempted && name.isBlank()) FormError.Required else null

    val macError: FormError?
        get() {
            if (mac.isBlank()) return if (submitAttempted) FormError.Required else null
            val result = MacAddress.parse(mac) as? MacAddressParseResult.Invalid ?: return null
            return when (result.error) {
                MacAddressError.InvalidCharacter -> FormError.MacInvalidCharacter
                MacAddressError.TooLong -> FormError.MacTooLong
                MacAddressError.TooShort -> if (submitAttempted) FormError.MacIncomplete else null
            }
        }

    val ipError: FormError?
        get() = optionalIpv4Error(ip)

    val broadcastError: FormError?
        get() = optionalIpv4Error(broadcast)

    val wolPortError: FormError?
        get() = if (submitAttempted && parsedWolPort() == null) FormError.InvalidPort else null

    private fun optionalIpv4Error(value: String): FormError? =
        if (submitAttempted && value.isNotBlank() && Ipv4.parse(value) == null) FormError.InvalidIpv4 else null

    private fun parsedWolPort(): Int? = wolPort.trim().toIntOrNull()?.takeIf { it in 1..65535 }

    fun submitted() = copy(submitAttempted = true)

    /** [base] carries what the form does not edit: id, order and the detected probe port. */
    fun toMachine(base: Machine? = null): Machine? {
        val validated = submitted()
        val errors = listOf(
            validated.nameError,
            validated.macError,
            validated.ipError,
            validated.broadcastError,
            validated.wolPortError,
        )
        if (errors.any { it != null }) return null
        val fields = Machine(
            name = name.trim(),
            type = type,
            mac = (MacAddress.parse(mac) as MacAddressParseResult.Valid).mac,
            ip = ip.trim().ifEmpty { null },
            hostname = hostname.trim().ifEmpty { null },
            broadcast = broadcast.trim().ifEmpty { null },
            wolPort = parsedWolPort()!!,
            favorite = favorite,
        )
        return base?.let { fields.copy(id = it.id, order = it.order, probePort = it.probePort) } ?: fields
    }

    companion object {
        fun from(machine: Machine) = MachineForm(
            name = machine.name,
            type = machine.type,
            mac = machine.mac.toString(),
            ip = machine.ip.orEmpty(),
            hostname = machine.hostname.orEmpty(),
            broadcast = machine.broadcast.orEmpty(),
            wolPort = machine.wolPort.toString(),
            favorite = machine.favorite,
        )
    }
}
