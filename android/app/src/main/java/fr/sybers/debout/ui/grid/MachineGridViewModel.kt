package fr.sybers.debout.ui.grid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.sybers.debout.data.MachineRepository
import fr.sybers.debout.data.UserPreferences
import fr.sybers.debout.domain.Machine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MachineGridViewModel(
    private val repository: MachineRepository,
    private val preferences: UserPreferences,
) : ViewModel() {

    val machines: StateFlow<List<Machine>?> =
        repository.machines.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The carousel opens on the last viewed machine, so choosing one here is enough. */
    fun select(id: Long) = preferences.setLastViewedMachine(id)

    fun reorder(ids: List<Long>) {
        viewModelScope.launch { repository.reorder(ids) }
    }
}
