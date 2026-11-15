package fr.sybers.debout.domain

object MagicPacket {
    private const val SYNC_LENGTH = 6
    private const val MAC_REPETITIONS = 16

    fun of(mac: MacAddress): ByteArray {
        val macBytes = mac.toBytes()
        return ByteArray(SYNC_LENGTH) { 0xFF.toByte() } +
            ByteArray(MAC_REPETITIONS * macBytes.size) { macBytes[it % macBytes.size] }
    }
}
