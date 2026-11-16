package fr.sybers.debout.ui.wake

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
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
class HoldInPagerTest {

    @get:Rule
    val compose = createComposeRule()

    private var wakes = 0
    private lateinit var pagerState: PagerState

    @Before
    fun setContent() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            pagerState = rememberPagerState { 3 }
            HorizontalPager(pagerState, Modifier.size(300.dp).testTag(TAG)) {
                val hold = rememberHoldToWake(enabled = true) { wakes++ }
                Box(Modifier.fillMaxSize().then(hold.modifier))
            }
        }
    }

    @Test
    fun `swiping to the next machine never wakes, however long the finger stays down`() {
        compose.onNodeWithTag(TAG).performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithTag(TAG).performTouchInput { moveBy(Offset(-width * 0.6f, 0f)) }
        compose.mainClock.advanceTimeBy(HOLD_DURATION_MILLIS * 2L)
        compose.onNodeWithTag(TAG).performTouchInput { up() }
        compose.mainClock.advanceTimeBy(2_000)

        assertEquals(0, wakes)
        assertEquals(1, pagerState.currentPage)
    }

    @Test
    fun `holding still inside the pager still wakes`() {
        compose.onNodeWithTag(TAG).performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(HOLD_DURATION_MILLIS + 100L)
        compose.onNodeWithTag(TAG).performTouchInput { up() }
        compose.mainClock.advanceTimeBy(2_000)

        assertEquals(1, wakes)
        assertEquals(0, pagerState.currentPage)
    }

    private companion object {
        const val TAG = "pager"
    }
}
