package fr.sybers.debout.ui.help

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import fr.sybers.debout.R

private class CheckItem(@StringRes val title: Int, @StringRes val detail: Int)

private val bios = CheckItem(R.string.help_bios_title, R.string.help_bios_detail)
private val ethernet = CheckItem(R.string.help_ethernet_title, R.string.help_ethernet_detail)
private val sameNetwork = CheckItem(R.string.help_same_network_title, R.string.help_same_network_detail)

private val HelpOs.checklist: List<CheckItem>
    get() = when (this) {
        HelpOs.Windows -> listOf(
            bios,
            CheckItem(R.string.help_windows_adapter_title, R.string.help_windows_adapter_detail),
            CheckItem(R.string.help_windows_fast_startup_title, R.string.help_windows_fast_startup_detail),
            ethernet,
            sameNetwork,
            CheckItem(R.string.help_windows_script_title, R.string.help_windows_script_detail),
        )
        HelpOs.MacOs -> listOf(
            CheckItem(R.string.help_macos_network_access_title, R.string.help_macos_network_access_detail),
            CheckItem(R.string.help_macos_sleep_title, R.string.help_macos_sleep_detail),
            ethernet,
            sameNetwork,
        )
        HelpOs.Linux -> listOf(
            bios,
            CheckItem(R.string.help_linux_interface_title, R.string.help_linux_interface_detail),
            ethernet,
            sameNetwork,
        )
    }

private val HelpOs.label: Int
    get() = when (this) {
        HelpOs.Windows -> R.string.help_os_windows
        HelpOs.MacOs -> R.string.help_os_macos
        HelpOs.Linux -> R.string.help_os_linux
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TroubleshootingScreen(initialOs: HelpOs, onBack: () -> Unit) {
    var os by rememberSaveable { mutableStateOf(initialOs) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.help_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.navigate_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = os.ordinal) {
                HelpOs.entries.forEach { option ->
                    Tab(
                        selected = option == os,
                        onClick = { os = option },
                        text = { Text(stringResource(option.label)) },
                    )
                }
            }
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.help_intro), style = MaterialTheme.typography.bodyLarge)
                os.checklist.forEachIndexed { index, item ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                stringResource(R.string.help_item_title, index + 1, stringResource(item.title)),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(stringResource(item.detail), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                OutlinedButton(
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, os.helpUrl.toUri()))
                        } catch (_: ActivityNotFoundException) {
                            // No browser: the checklist above is the whole help anyway.
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.help_open_site))
                }
            }
        }
    }
}
