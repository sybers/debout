package fr.sybers.debout.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec

@Database(
    entities = [MachineEntity::class],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2, spec = DeboutDatabase.DropFavorite::class)],
)
abstract class DeboutDatabase : RoomDatabase() {
    abstract fun machineDao(): MachineDao

    /** The list order decides which machines the tiles and shortcuts control. */
    @DeleteColumn(tableName = "machines", columnName = "favorite")
    class DropFavorite : AutoMigrationSpec

    companion object {
        fun create(context: Context): DeboutDatabase =
            Room.databaseBuilder(context, DeboutDatabase::class.java, "debout.db").build()
    }
}
