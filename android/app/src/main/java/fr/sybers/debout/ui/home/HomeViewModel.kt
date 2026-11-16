package fr.sybers.debout.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.sybers.debout.data.MachineRepository
import fr.sybers.debout.data.UserPreferences
import fr.sybers.debout.domain.Machine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeState(val machines: List<Machine>, val currentMachineId: Long?)

class HomeViewModel(
    private val repository: MachineRepository,
    private val preferences: UserPreferences,
) : ViewModel() {

    /** Null until both the database and the preferences are read, so nothing flashes. */
    val state: StateFlow<HomeState?> =
        combine(repository.machines, preferences.lastViewedMachineId, ::HomeState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _recentlyDeleted = MutableStateFlow<Machine?>(null)
    val recentlyDeleted: StateFlow<Machine?> = _recentlyDeleted.asStateFlow()

    fun onMachineViewed(id: Long) = preferences.setLastViewedMachine(id)

    /** Lands on the next machine, or the previous one when the last is deleted. */
    fun delete(id: Long) {
        viewModelScope.launch {
            val machine = repository.get(id) ?: return@launch
            val machines = state.value?.machines.orEmpty()
            val index = machines.indexOfFirst { it.id == id }
            (machines.getOrNull(index + 1) ?: machines.getOrNull(index - 1))
                ?.let { preferences.setLastViewedMachine(it.id) }
            repository.delete(machine)
            _recentlyDeleted.value = machine
        }
    }

    fun undoDelete() {
        val machine = _recentlyDeleted.value ?: return
        _recentlyDeleted.value = null
        viewModelScope.launch {
            repository.save(machine)
            preferences.setLastViewedMachine(machine.id)
        }
    }

    fun dismissDeleted() {
        _recentlyDeleted.value = null
    }
}
