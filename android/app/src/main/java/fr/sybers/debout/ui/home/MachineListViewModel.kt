package fr.sybers.debout.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.sybers.debout.data.MachineRepository
import fr.sybers.debout.domain.Machine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MachineListViewModel(private val repository: MachineRepository) : ViewModel() {

    /** Null until the first read from the database, so the empty state does not flash. */
    val machines: StateFlow<List<Machine>?> =
        repository.machines.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _recentlyDeleted = MutableStateFlow<Machine?>(null)
    val recentlyDeleted: StateFlow<Machine?> = _recentlyDeleted.asStateFlow()

    fun delete(id: Long) {
        viewModelScope.launch {
            val machine = repository.get(id) ?: return@launch
            repository.delete(machine)
            _recentlyDeleted.value = machine
        }
    }

    fun undoDelete() {
        val machine = _recentlyDeleted.value ?: return
        _recentlyDeleted.value = null
        viewModelScope.launch { repository.save(machine) }
    }

    fun dismissDeleted() {
        _recentlyDeleted.value = null
    }
}
