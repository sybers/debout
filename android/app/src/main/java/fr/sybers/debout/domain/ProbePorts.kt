package fr.sybers.debout.domain

object ProbePorts {
    /**
     * SMB, RDP, SSH, VNC, AFP, then Sunshine/GameStream (HTTP, HTTPS): at least one is usually
     * open on a desktop, server, NAS or Mac. Sunshine opens its own firewall rules, so its ports
     * answer on a Windows PC whose firewall silently drops everything else.
     */
    val COMMON = listOf(445, 3389, 22, 5900, 548, 47989, 47984)

    fun inOrder(remembered: Int?): List<Int> = listOfNotNull(remembered) + COMMON.filter { it != remembered }
}
