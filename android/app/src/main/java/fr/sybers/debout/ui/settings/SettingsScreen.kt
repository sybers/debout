package fr.sybers.debout.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.sybers.debout.R
import fr.sybers.debout.data.UserPreferences
import fr.sybers.debout.tracking.canPostNotifications

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(preferences: UserPreferences, onBack: () -> Unit) {
    val context = LocalContext.current
    val notificationsEnabled by preferences.wakeNotificationsEnabled.collectAsStateWithLifecycle(initialValue = null)
    // The user can change it in the system settings: read it again on every return.
    var notificationsAllowed by remember { mutableStateOf(context.canPostNotifications()) }
    LifecycleResumeEffect(Unit) {
        notificationsAllowed = context.canPostNotifications()
        onPauseOrDispose {}
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsAllowed = it
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.navigate_back))
                    }
                },
            )
        },
    ) { padding ->
        val enabled = notificationsEnabled ?: return@Scaffold
        val blocked = enabled && !notificationsAllowed
        Column(Modifier.padding(padding)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_wake_notifications)) },
                supportingContent = {
                    Text(
                        stringResource(
                            if (blocked) R.string.settings_wake_notifications_blocked else R.string.settings_wake_notifications_summary,
                        ),
                    )
                },
                trailingContent = { Switch(checked = enabled, onCheckedChange = null) },
                modifier = Modifier.toggleable(value = enabled, role = Role.Switch) { on ->
                    preferences.setWakeNotificationsEnabled(on)
                    if (on && !notificationsAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
            )
            if (blocked) {
                TextButton(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                        )
                    },
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Text(stringResource(R.string.settings_open_system_notifications))
                }
            }
        }
    }
}
