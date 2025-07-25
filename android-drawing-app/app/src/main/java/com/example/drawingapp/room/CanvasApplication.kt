package com.example.drawingapp.room

import android.app.Application
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class CanvasApplication : Application() {

    val scope = CoroutineScope(SupervisorJob())

    //get a reference to the DB singleton
    val db by lazy {
        Room.databaseBuilder(
        applicationContext,
        CanvasDatabase::class.java,
        "canvas_database"
    ).fallbackToDestructiveMigration().build()}

    val canvasRepository by lazy {CanvasRepository(scope, db.canvasDao())}
}