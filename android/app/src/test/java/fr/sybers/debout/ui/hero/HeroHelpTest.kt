package fr.sybers.debout.ui.hero

import android.app.Application
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sybers.debout.R
import fr.sybers.debout.domain.MacAddress
import fr.sybers.debout.domain.Machine
import fr.sybers.debout.domain.MachineType
import fr.sybers.debout.domain.WakeRequestResult
import fr.sybers.debout.domain.WakeState
import fr.sybers.debout.network.WakeStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.time.TimeSource

// Below Android 13, the hero asks for no permission, so it needs no app container.
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, sdk = [Build.VERSION_CODES.S_V2])
class HeroHelpTest {

    @get:Rule
    val compose = createComposeRule()

    private val machine = Machine(name = "Salon", type = MachineType.Desktop, mac = MacAddress(0xAABBCCDDEEFF))
    private val helpLabel = ApplicationProvider.getApplicationContext<Application>().getString(R.string.hero_help)
    private var wakes = 0
    private var helpOpened = 0

    private fun setContent(state: WakeState) {
        compose.setContent {
            HeroPage(
                machine = machine,
                slot = null,
                status = WakeStatus(state, TimeSource.Monotonic.markNow()),
                localNetwork = null,
                onWake = { wakes++; WakeRequestResult.Started },
                wakeRequested = false,
                onWakeRequestHandled = {},
                onOpenHelp = { helpOpened++ },
            )
        }
    }

    @Test
    fun `a wake without response offers help, and tapping it wakes nothing`() {
        setContent(WakeState.NoResponse)

        compose.onNodeWithText(helpLabel).assertIsDisplayed().performClick()
        compose.mainClock.advanceTimeBy(2_000)

        assertEquals(1, helpOpened)
        assertEquals(0, wakes)
    }

    @Test
    fun `an asleep machine offers no help`() {
        setContent(WakeState.Offline)

        compose.onNodeWithText(helpLabel).assertDoesNotExist()
    }
}
