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

    @Query("SELECT COUNT(*) FROM machines")
    protected abstract suspend fun count(): Int

    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM machines")
    protected abstract suspend fun maxOrder(): Int

    @Upsert
    protected abstract suspend fun upsert(machine: MachineEntity): Long

    @Delete
    protected abstract suspend fun deleteRow(machine: MachineEntity)

    @Query("UPDATE machines SET favorite = 0 WHERE id != :id")
    protected abstract suspend fun clearFavoriteExcept(id: Long)

    @Query("UPDATE machines SET favorite = 1 WHERE id = (SELECT id FROM machines ORDER BY sort_order LIMIT 1)")
    protected abstract suspend fun makeFirstFavorite()

    /**
     * A new machine (id 0) goes last, and becomes the favorite if it is the first one.
     * An existing id is written as is, which also restores a deleted machine in place.
     */
    @Transaction
    open suspend fun save(machine: MachineEntity): Long {
        val toSave = if (machine.id == 0L) {
            machine.copy(order = maxOrder() + 1, favorite = machine.favorite || count() == 0)
        } else {
            machine
        }
        // Upsert returns -1 when it updates an existing row.
        val id = upsert(toSave).takeIf { it != -1L } ?: toSave.id
        if (toSave.favorite) clearFavoriteExcept(id)
        return id
    }

    @Transaction
    open suspend fun delete(machine: MachineEntity) {
        deleteRow(machine)
        if (machine.favorite) makeFirstFavorite()
    }
}
