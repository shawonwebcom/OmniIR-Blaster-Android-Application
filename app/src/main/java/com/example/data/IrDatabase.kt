package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [IrRemote::class, IrButton::class], version = 1, exportSchema = false)
abstract class IrDatabase : RoomDatabase() {
    abstract fun irDao(): IrDao

    companion object {
        @Volatile
        private var INSTANCE: IrDatabase? = null

        fun getDatabase(context: Context): IrDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    IrDatabase::class.java,
                    "omni_ir_database"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
