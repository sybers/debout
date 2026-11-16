package fr.sybers.debout.ui.machine

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.sybers.debout.R
import fr.sybers.debout.domain.MachineType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MachineEditScreen(
    viewModel: MachineEditViewModel,
    onBack: () -> Unit,
    onDelete: () -> Unit,
) {
    val form = viewModel.form
    val automaticBroadcast by viewModel.automaticBroadcast.collectAsStateWithLifecycle()
    var showMacHelp by rememberSaveable { mutableStateOf(false) }
    var showAdvanced by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (viewModel.isEditing) R.string.edit_title_edit else R.string.edit_title_add))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.navigate_back))
                    }
                },
                actions = {
                    if (viewModel.isEditing) {
                        IconButton(onClick = onDelete) {
                            Icon(painterResource(R.drawable.ic_delete), stringResource(R.string.delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (!viewModel.loaded) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FormField(
                value = form.name,
                onValueChange = { value -> viewModel.update { it.copy(name = value) } },
                label = R.string.field_name,
                error = form.nameError,
                capitalization = KeyboardCapitalization.Sentences,
            )
            TypeField(form.type, onTypeChange = { type -> viewModel.update { it.copy(type = type) } })
            Column {
                FormField(
                    value = form.mac,
                    onValueChange = { value -> viewModel.update { it.copy(mac = value) } },
                    label = R.string.field_mac,
                    placeholder = stringResource(R.string.field_mac_placeholder),
                    error = form.macError,
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Ascii,
                )
                TextButton(onClick = { showMacHelp = true }) { Text(stringResource(R.string.mac_help_button)) }
            }
            FormField(
                value = form.ip,
                onValueChange = { value -> viewModel.update { it.copy(ip = value) } },
                label = R.string.field_ip,
                error = form.ipError,
                keyboardType = KeyboardType.Decimal,
            )
            FormField(
                value = form.hostname,
                onValueChange = { value -> viewModel.update { it.copy(hostname = value) } },
                label = R.string.field_hostname,
                keyboardType = KeyboardType.Uri,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.field_favorite), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.field_favorite_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = form.favorite,
                    onCheckedChange = { checked -> viewModel.update { it.copy(favorite = checked) } },
                )
            }
            TextButton(onClick = { showAdvanced = !showAdvanced }) {
                Text(stringResource(R.string.field_advanced))
                Icon(
                    painterResource(if (showAdvanced) R.drawable.ic_expand_less else R.drawable.ic_expand_more),
                    contentDescription = null,
                )
            }
            val advancedError = form.broadcastError ?: form.wolPortError ?: form.probePortError
            AnimatedVisibility(visible = showAdvanced || advancedError != null) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormField(
                        value = form.broadcast,
                        onValueChange = { value -> viewModel.update { it.copy(broadcast = value) } },
                        label = R.string.field_broadcast,
                        placeholder = automaticBroadcast
                            ?.let { stringResource(R.string.field_broadcast_auto, it) }
                            ?: stringResource(R.string.field_broadcast_auto_unknown),
                        error = form.broadcastError,
                        keyboardType = KeyboardType.Decimal,
                    )
                    FormField(
                        value = form.wolPort,
                        onValueChange = { value -> viewModel.update { it.copy(wolPort = value) } },
                        label = R.string.field_wol_port,
                        error = form.wolPortError,
                        keyboardType = KeyboardType.Number,
                    )
                    FormField(
                        value = form.probePort,
                        onValueChange = { value -> viewModel.update { it.copy(probePort = value) } },
                        label = R.string.field_probe_port,
                        placeholder = stringResource(R.string.field_probe_port_auto),
                        error = form.probePortError,
                        keyboardType = KeyboardType.Number,
                    )
                }
            }
            Button(onClick = { viewModel.save(onSaved = onBack) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.save))
            }
        }
    }

    if (showMacHelp) MacHelpDialog(onDismiss = { showMacHelp = false })
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: Int,
    error: FormError? = null,
    placeholder: String? = null,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        placeholder = placeholder?.let { { Text(it) } },
        isError = error != null,
        supportingText = error?.let { { Text(stringResource(it.message)) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            autoCorrectEnabled = false,
            keyboardType = keyboardType,
            imeAction = ImeAction.Next,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeField(type: MachineType, onTypeChange: (MachineType) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = stringResource(type.label),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.field_type)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MachineType.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.label)) },
                    onClick = {
                        onTypeChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
