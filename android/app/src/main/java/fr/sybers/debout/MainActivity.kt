package fr.sybers.debout

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.sybers.debout.ui.DeboutApp
import fr.sybers.debout.ui.theme.DeboutTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val wakeRequest = MutableStateFlow<Long?>(null)
    private val pairingRequest = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handle(intent)
        setContent {
            val wake by wakeRequest.collectAsStateWithLifecycle()
            val pairing by pairingRequest.collectAsStateWithLifecycle()
            DeboutTheme {
                DeboutApp(
                    wakeRequest = wake,
                    onWakeRequestHandled = { wakeRequest.value = null },
                    pairingRequest = pairing,
                    onPairingRequestHandled = { pairingRequest.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent) {
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return
        WakeIntent.machineId(intent)?.let { machineId ->
            appContainer.preferences.setLastViewedMachine(machineId)
            wakeRequest.value = machineId
        }
        if (intent.action == Intent.ACTION_VIEW && intent.data?.scheme == "debout") {
            pairingRequest.value = intent.dataString
        }
    }
}
