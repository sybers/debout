package fr.sybers.debout.ui.wake

import android.Manifest
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.sybers.debout.appContainer
import fr.sybers.debout.tracking.canPostNotifications

/**
 * Asks for the notification permission after a wake starts, so the dialog never delays the
 * wake. Android shows a rationale state once the user refused: from then on the app stops
 * asking and follows wakes silently, until the user turns the setting back on.
 */
@Composable
fun rememberWakeNotificationsPermission(): () -> Unit {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return remember { {} }

    val activity = LocalActivity.current
    val enabled by LocalContext.current.appContainer.preferences.wakeNotificationsEnabled
        .collectAsStateWithLifecycle(initialValue = false)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    return {
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (enabled && activity != null && !activity.canPostNotifications() &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        ) {
            launcher.launch(permission)
        }
    }
}
