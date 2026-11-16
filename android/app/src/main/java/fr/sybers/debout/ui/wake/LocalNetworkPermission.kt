package fr.sybers.debout.ui.wake

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import fr.sybers.debout.network.LOCAL_NETWORK_PROTECTION_SDK
import fr.sybers.debout.network.hasLocalNetworkAccess

/** Runs an action that needs the local network, asking for ACCESS_LOCAL_NETWORK first if needed. */
@Composable
fun rememberLocalNetworkAccess(onDenied: () -> Unit): (action: () -> Unit) -> Unit {
    if (Build.VERSION.SDK_INT < LOCAL_NETWORK_PROTECTION_SDK) return remember { { action -> action() } }

    val context = LocalContext.current
    val currentOnDenied by rememberUpdatedState(onDenied)
    val pending = remember { arrayOfNulls<() -> Unit>(1) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val action = pending[0]
        pending[0] = null
        if (granted) action?.invoke() else currentOnDenied()
    }
    return remember(context, launcher) {
        { action ->
            if (context.hasLocalNetworkAccess()) {
                action()
            } else {
                pending[0] = action
                launcher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
            }
        }
    }
}
