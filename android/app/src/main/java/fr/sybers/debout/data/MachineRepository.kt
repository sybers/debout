package fr.sybers.debout.data

import fr.sybers.debout.domain.Machine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MachineRepository(private val dao: MachineDao) {

    val machines: Flow<List<Machine>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun get(id: Long): Machine? = dao.get(id)?.toDomain()

    suspend fun save(machine: Machine): Long = dao.save(machine.toEntity())

    suspend fun rememberProbeResult(id: Long, ip: String?, probePort: Int?) = dao.updateProbeResult(id, ip, probePort)

    suspend fun reorder(ids: List<Long>) = dao.reorder(ids)

    suspend fun delete(machine: Machine) = dao.delete(machine.toEntity())
}
