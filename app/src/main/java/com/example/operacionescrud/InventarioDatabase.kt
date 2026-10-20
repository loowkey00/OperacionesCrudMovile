package com.example.operacionescrud

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Producto::class], version = 1, exportSchema = false)
abstract class InventarioDatabase : RoomDatabase() {
    abstract fun productoDao(): ProductoDao

    companion object {
        @Volatile
        private var INSTANCE: InventarioDatabase? = null

        fun getDatabase(context: Context): InventarioDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InventarioDatabase::class.java,
                    "inventario_db",
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
