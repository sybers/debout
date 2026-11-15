package fr.sybers.debout.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MachineEntity::class], version = 1)
abstract class DeboutDatabase : RoomDatabase() {
    abstract fun machineDao(): MachineDao

    companion object {
        fun create(context: Context): DeboutDatabase =
            Room.databaseBuilder(context, DeboutDatabase::class.java, "debout.db").build()
    }
}
