package fr.sybers.debout.ui.machine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.sybers.debout.data.MachineRepository
import fr.sybers.debout.domain.Machine
import fr.sybers.debout.network.LocalNetwork
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** [machineId] null means adding a new machine. */
class MachineEditViewModel(
    private val repository: MachineRepository,
    private val machineId: Long?,
    localNetwork: Flow<LocalNetwork?>,
) : ViewModel() {

    var form by mutableStateOf(MachineForm())
        private set

    var loaded by mutableStateOf(machineId == null)
        private set

    private var original: Machine? = null

    val isEditing: Boolean get() = machineId != null

    /** What an empty broadcast field resolves to on the current network. */
    val automaticBroadcast: StateFlow<String?> = localNetwork
        .map { it?.broadcast?.hostAddress }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        if (machineId != null) {
            viewModelScope.launch {
                repository.get(machineId)?.let {
                    original = it
                    form = MachineForm.from(it)
                }
                loaded = true
            }
        }
    }

    fun update(transform: (MachineForm) -> MachineForm) {
        form = transform(form)
    }

    fun save(onSaved: (machineId: Long) -> Unit) {
        val machine = form.toMachine(base = original)
        if (machine == null) {
            form = form.submitted()
            return
        }
        viewModelScope.launch {
            onSaved(repository.save(machine))
        }
    }
}
