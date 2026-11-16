package fr.sybers.debout.domain

object ProbePorts {
    /** SMB, RDP, SSH, VNC, AFP: at least one is usually open on a desktop, server, NAS or Mac. */
    val COMMON = listOf(445, 3389, 22, 5900, 548)

    fun inOrder(remembered: Int?): List<Int> = listOfNotNull(remembered) + COMMON.filter { it != remembered }
}
