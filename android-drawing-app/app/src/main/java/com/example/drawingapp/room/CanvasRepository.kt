package com.example.drawingapp.room

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch



class CanvasRepository(private val scope: CoroutineScope,
                       private val dao: CanvasDao) {

    // Observable list of all canvases stored in the Room database.
    val allCanvases = dao.getAllCanvases().asLiveData()
    val latestCanvas = dao.latestCanvas().asLiveData()
    // fun getCanvasById(id: Int) = dao.getCanvas(id).asLiveData()

    // Function to add a new canvas
    fun addCanvas(canvasEntity: CanvasEntity?) {
        scope.launch {
            Log.e("REPO", "Adding new canvas")

            val emptyBitmap = Bitmap.createBitmap(1100, 1100, Bitmap.Config.ARGB_8888)

            val bitmapFileName = "canvas_${System.currentTimeMillis()}.png"

            if (canvasEntity == null) {
                val canvasEntity2 = CanvasEntity(
                    filePath = bitmapFileName,
                    bitmap = emptyBitmap
                )
                dao.insert(canvasEntity2)
            }
            else {
                val canvasEntity2 = CanvasEntity(
                    filePath = bitmapFileName,
                    bitmap = canvasEntity.bitmap
                )
                dao.insert(canvasEntity2)
            }
            Log.e("REPO", "Inserted new canvas into the DB")
        }
    }

    suspend fun insertCanvas(canvas: CanvasEntity) {
        try {
            dao.insertCanvas(canvas)
            Log.d("REPO", "Successfully added canvas with ID: ${canvas.id}")
        } catch (e: Exception) {
            Log.e("REPO", "Error adding canvas with ID: ${canvas.id}", e)
        }
    }
    // Function to update a canvas by its ID
    fun updateCanvas(canvasId: Int, newBitmap: Bitmap) {
        scope.launch {
            Log.e("REPO", "Updating canvas data")

            try {
                val canvas = dao.getCanvas(canvasId).firstOrNull()
                if (canvas != null) {
                    val updatedCanvas = canvas.copy(bitmap = newBitmap)
                    dao.insert(updatedCanvas)
                    Log.e("REPO", "Updated canvas in DB")
                } else {
                    Log.e("REPO", "No canvas found with ID $canvasId")
                }
            } catch (e: Exception) {
                Log.e("REPO", "Failed to update canvas: ${e.message}")
            }
        }

    }

    suspend fun addCanvasAndReturnId(canvas: CanvasEntity): Long {
        return try {
            dao.insert(canvas)
        } catch (e: Exception) {
            Log.e("Repository", "Error inserting canvas", e)
            -1L // return invalid id
        }
    }

    suspend fun getCanvasById(id: Long): CanvasEntity? {
        return try {
            dao.getCanvas(id.toInt()).firstOrNull()
        } catch (e: Exception) {
            Log.e("Repository", "Error fetching canvas by ID: $id", e)
            null
        }
    }

    suspend fun getAllCanvasesDirect(): List<CanvasEntity> {
        return dao.getAllCanvases().firstOrNull() ?: emptyList()
    }

//    suspend fun deleteCanvas(id: Int) {
//        dao.deleteCanvasById(id)
//    }


    suspend fun deleteAllWithCallback(onComplete: () -> Unit) {
        dao.deleteAll()
        onComplete() // notify that deletion is complete
    }

    // Function to delete a canvas by its ID
    suspend fun deleteCanvas(canvasId: Int) {
        scope.launch {
            Log.e("REPO", "Deleting canvas with ID $canvasId")

            try {
                dao.getCanvas(canvasId).collect { canvas ->
                    if (canvas != null) {
                        dao.deleteCanvas(canvas)
                        Log.e("REPO", "Canvas deleted from DB")
                    } else {
                        Log.e("REPO", "No canvas found with ID $canvasId")
                    }
                }
            } catch (e: Exception) {
                Log.e("REPO", "Failed to delete canvas: ${e.message}")
            }
        }
    }
}