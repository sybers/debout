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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) requestWake(intent)
        setContent {
            val request by wakeRequest.collectAsStateWithLifecycle()
            DeboutTheme {
                DeboutApp(wakeRequest = request, onWakeRequestHandled = { wakeRequest.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        requestWake(intent)
    }

    private fun requestWake(intent: Intent) {
        val machineId = WakeIntent.machineId(intent) ?: return
        appContainer.preferences.setLastViewedMachine(machineId)
        wakeRequest.value = machineId
    }
}
