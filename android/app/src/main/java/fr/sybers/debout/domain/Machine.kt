package fr.sybers.debout.domain

enum class MachineType { Desktop, Laptop, Server, Nas, Mac, Other }

/**
 * [broadcast] null means "derive it from the phone's subnet at send time".
 * [probePort] null means "not detected yet".
 */
data class Machine(
    val id: Long = 0,
    val name: String,
    val type: MachineType,
    val mac: MacAddress,
    val ip: String? = null,
    val hostname: String? = null,
    val broadcast: String? = null,
    val wolPort: Int = DEFAULT_WOL_PORT,
    val probePort: Int? = null,
    val favorite: Boolean = false,
    val order: Int = 0,
) {
    companion object {
        const val DEFAULT_WOL_PORT = 9
    }
}
