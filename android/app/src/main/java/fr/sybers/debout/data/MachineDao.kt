package fr.sybers.debout.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MachineDao {

    @Query("SELECT * FROM machines ORDER BY sort_order")
    abstract fun observeAll(): Flow<List<MachineEntity>>

    @Query("SELECT * FROM machines WHERE id = :id")
    abstract suspend fun get(id: Long): MachineEntity?

    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM machines")
    protected abstract suspend fun maxOrder(): Int

    @Upsert
    protected abstract suspend fun upsert(machine: MachineEntity): Long

    @Delete
    abstract suspend fun delete(machine: MachineEntity)

    @Query("UPDATE machines SET sort_order = :order WHERE id = :id")
    protected abstract suspend fun setOrder(id: Long, order: Int)

    /** Leaves a deleted machine deleted, unlike [save]. */
    @Query("UPDATE machines SET ip = :ip, probe_port = :probePort WHERE id = :id")
    abstract suspend fun updateProbeResult(id: Long, ip: String?, probePort: Int?)

    /**
     * A new machine (id 0) goes last. An existing id is written as is, which also restores
     * a deleted machine in place.
     */
    @Transaction
    open suspend fun save(machine: MachineEntity): Long {
        val toSave = if (machine.id == 0L) machine.copy(order = maxOrder() + 1) else machine
        // Upsert returns -1 when it updates an existing row.
        return upsert(toSave).takeIf { it != -1L } ?: toSave.id
    }

    @Transaction
    open suspend fun reorder(ids: List<Long>) {
        ids.forEachIndexed { order, id -> setOrder(id, order) }
    }
}
