package fr.sybers.debout.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ProbePortsTest {

    @Test
    fun `without a remembered port, tries the common ports`() {
        assertEquals(listOf(445, 3389, 22, 5900, 548, 47989, 47984), ProbePorts.inOrder(null))
    }

    @Test
    fun `a remembered common port moves first without repeating`() {
        assertEquals(listOf(22, 445, 3389, 5900, 548, 47989, 47984), ProbePorts.inOrder(22))
    }

    @Test
    fun `a remembered custom port comes before the common ones`() {
        assertEquals(listOf(8080, 445, 3389, 22, 5900, 548, 47989, 47984), ProbePorts.inOrder(8080))
    }
}
