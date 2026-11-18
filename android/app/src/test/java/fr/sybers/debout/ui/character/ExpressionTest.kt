package fr.sybers.debout.ui.character

import fr.sybers.debout.domain.WakeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpressionTest {

    @Test
    fun `each state has its expression`() {
        assertEquals(Expression.Asleep, WakeState.Offline.expression(onLocalNetwork = true))
        assertEquals(Expression.Surprised, WakeState.Sending.expression(onLocalNetwork = true))
        assertEquals(Expression.Drowsy, WakeState.Booting.expression(onLocalNetwork = true))
        assertEquals(Expression.Awake, WakeState.Online.expression(onLocalNetwork = true))
        assertEquals(Expression.Sad, WakeState.NoResponse.expression(onLocalNetwork = true))
    }

    @Test
    fun `an unknown state searches on a local network and stares blankly off it`() {
        assertEquals(Expression.Searching, WakeState.Unknown.expression(onLocalNetwork = true))
        assertEquals(Expression.Blank, WakeState.Unknown.expression(onLocalNetwork = false))
    }

    @Test
    fun `closed eyes are wider than tall, open ones taller than wide`() {
        listOf(Expression.Asleep, Expression.Sad).forEach { assertTrue(it.pose.width > it.pose.height) }
        assertTrue(Expression.Awake.pose.height > Expression.Awake.pose.width)
    }

    @Test
    fun `holding opens a sleeping character's eyes with the finger`() {
        assertEquals(Expression.Asleep.pose, Expression.Asleep.poseWhileHolding(0f))
        assertEquals(Expression.Awake.pose, Expression.Asleep.poseWhileHolding(1f))

        val halfway = Expression.Asleep.poseWhileHolding(0.5f)
        assertTrue(halfway.height > Expression.Asleep.pose.height)
        assertTrue(halfway.height < Expression.Awake.pose.height)
    }

    @Test
    fun `holding straightens drooping eyes`() {
        assertEquals(0f, Expression.Sad.poseWhileHolding(1f).tilt)
    }

    @Test
    fun `holding an awake character widens its eyes`() {
        assertEquals(Expression.Surprised.pose, Expression.Awake.poseWhileHolding(1f))
    }
}
