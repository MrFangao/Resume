package com.example.drawingapp.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CanvasDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(canvasEntity: CanvasEntity): Long

    @Query("SELECT * FROM canvas_table WHERE id = :id")
    fun getCanvas(id: Int): Flow<CanvasEntity?>

    @Query("SELECT * FROM canvas_table")
    fun getAllCanvases(): Flow<List<CanvasEntity>>

    @Delete
    suspend fun deleteCanvas(canvasEntity: CanvasEntity)

    @Query("DELETE FROM canvas_table WHERE id = :id")
    suspend fun deleteCanvasById(id: Int)

    @Query("SELECT * from canvas_table ORDER BY id DESC LIMIT 1")
    fun latestCanvas() : Flow<CanvasEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCanvas(canvas: CanvasEntity)

    @Query("DELETE FROM canvas_table")
    suspend fun deleteAll()
}