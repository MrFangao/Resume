package com.example.drawingapp.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [CanvasEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class CanvasDatabase : RoomDatabase() {
    abstract fun canvasDao(): CanvasDao

    companion object {
        @Volatile
        private var INSTANCE: CanvasDatabase? = null

        fun getDatabase(context: Context): CanvasDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CanvasDatabase::class.java,
                    "canvas_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
