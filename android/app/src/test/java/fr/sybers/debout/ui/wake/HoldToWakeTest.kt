package fr.sybers.debout.ui.wake

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class HoldToWakeTest {

    @get:Rule
    val compose = createComposeRule()

    private var wakes = 0

    private fun setContent(enabled: Boolean = true) {
        compose.setContent {
            val hold = rememberHoldToWake(enabled) { wakes++ }
            Box(Modifier.size(200.dp).testTag(TAG).then(hold.modifier))
        }
    }

    @Before
    fun controlTheClock() {
        compose.mainClock.autoAdvance = false
    }

    @Test
    fun `releasing before the hold duration sends nothing`() {
        setContent()

        compose.onNodeWithTag(TAG).performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(HOLD_DURATION_MILLIS - 150L)
        compose.onNodeWithTag(TAG).performTouchInput { up() }
        compose.mainClock.advanceTimeBy(2_000)

        assertEquals(0, wakes)
    }

    @Test
    fun `holding for the hold duration wakes once`() {
        setContent()

        compose.onNodeWithTag(TAG).performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(HOLD_DURATION_MILLIS + 100L)
        compose.onNodeWithTag(TAG).performTouchInput { up() }
        compose.mainClock.advanceTimeBy(2_000)

        assertEquals(1, wakes)
    }

    @Test
    fun `holding a disabled button sends nothing`() {
        setContent(enabled = false)

        compose.onNodeWithTag(TAG).performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(2_000)

        assertEquals(0, wakes)
    }

    @Test
    fun `accessibility click wakes`() {
        setContent()

        compose.onNodeWithTag(TAG).performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(1, wakes)
    }

    private companion object {
        const val TAG = "hold"
    }
}
