package com.example.drawingapp

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.drawingapp.room.CanvasEntity
import com.example.drawingapp.room.CanvasRepository
import kotlinx.coroutines.launch


class DrawingViewModel(private val repository: CanvasRepository) : ViewModel() {
    private var canvasRef = MutableLiveData<CanvasEntity>()
    val currentCanvas: LiveData<CanvasEntity> = canvasRef

    val allCanvases: LiveData<List<CanvasEntity>> = repository.allCanvases
    val latestCanvas: LiveData<CanvasEntity> = repository.latestCanvas

    // Current canvas related states
    private val currentCanvasId = MutableLiveData<Int?>(null)

    var penSize = MutableLiveData<Float>(10F)

    var current = MutableLiveData<Int>(0)

    var toolNum = MutableLiveData<Int>(0)

    var color = MutableLiveData<Color>(Color.valueOf(0F, 0F, 0F))

    var currentShape = MutableLiveData<String>("pen")  // 默认是圆形


    fun addCanvasToList(canvas: CanvasEntity) {
        viewModelScope.launch {
            try {
                repository.insertCanvas(canvas)
                Log.d("ViewModel", "Added canvas with ID: ${canvas.id} to list")
            } catch (e: Exception) {
                Log.e("ViewModel", "Error adding canvas with ID: ${canvas.id}", e)
            }
        }
    }

    // In DrawingViewModel
    fun setColor(colorNum: Int) {
        val newColor = when (colorNum) {
            2 -> Color.valueOf(Color.RED) // Red
            3 -> Color.valueOf(Color.MAGENTA) // Magenta
            4 -> Color.valueOf(Color.YELLOW) // Yellow
            5 -> Color.valueOf(Color.GREEN) // Green
            6 -> Color.valueOf(Color.BLUE) // Blue
            7 -> Color.valueOf(Color.CYAN) // Cyan
            else -> Color.valueOf(0F, 0F, 0F) // Default to Black
        }
        color.value = newColor
    }

    fun getBitMap(): Bitmap {
        var bitmap = canvasRef.value?.bitmap

        if (bitmap == null) {
            val emptyBitmap = Bitmap.createBitmap(1100, 1100, Bitmap.Config.ARGB_8888)
            val bitmapFileName = "canvas_${System.currentTimeMillis()}.png"

            val canvasEntity = CanvasEntity(
                id = -1,
                filePath = bitmapFileName,
                bitmap = emptyBitmap
            )

            canvasRef.postValue(canvasEntity)
            return emptyBitmap
        } else {
            if (!bitmap.isMutable) {
                bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val updatedEntity = canvasRef.value?.copy(bitmap = bitmap)
                if (updatedEntity != null) {
                    canvasRef.postValue(updatedEntity!!)
                }
            }
            return bitmap!!
        }
    }

    fun updateCanvas(canvasId: Int, updatedBitmap: Bitmap) {
        repository.updateCanvas(canvasId, updatedBitmap)
    }

    fun updateCurrCanvas(bitmap: Bitmap) {
        val canvasEntity = CanvasEntity(
            id = currentCanvas.value!!.id,
            filePath = currentCanvas.value!!.filePath,
            bitmap = bitmap
        )
    }

    fun setCurrentCanvas(canvas: CanvasEntity) {
        canvasRef.postValue(canvas)
        Log.d("CanvasItem", "set on canvas with ID: ${currentCanvas.value?.id}")
    }

    // Add a new canvas
    fun addCanvas() {
        repository.addCanvas(null)
    }

    fun saveFirst(onComplete: (CanvasEntity?) -> Unit) {
        viewModelScope.launch {
            val currentCanvas = currentCanvas.value ?: return@launch
            try {
                val newCanvasId = repository.addCanvasAndReturnId(currentCanvas.copy(id = 0))

                val newCanvas = repository.getCanvasById(newCanvasId)

                newCanvas?.let {
                    setCurrentCanvas(it)
                    onComplete(it)
                    Log.d("Save", "Successfully saved canvas with ID: ${it.id}")
                } ?: run {
                    onComplete(null)
                    Log.e("Save", "Failed to fetch saved canvas")
                }
            } catch (e: Exception) {
                onComplete(null)
                Log.e("Save", "Error saving canvas", e)
            }
        }
    }



    //Delete a canvas by ID
    // In DrawingViewModel class
//     fun deleteCanvas() {
//         if (currentCanvas.value!!.id != -1) {
//             repository.deleteCanvas(currentCanvas.value!!.id)
//             Log.d("delete", "delete canvas with ID: ${currentCanvas.value?.id}")
//
//             // 检查是否是最后一张画布
//             allCanvases.value?.let { canvases ->
//                 if (canvases.size <= 1) {  // 如果删除后将没有画布
//                     // 创建一个新的空白画布
//                     val emptyBitmap = Bitmap.createBitmap(1100, 1100, Bitmap.Config.ARGB_8888)
//                     val bitmapFileName = "canvas_${System.currentTimeMillis()}.png"
//
//                     val newCanvas = CanvasEntity(
//                         id = -1,
//                         filePath = bitmapFileName,
//                         bitmap = emptyBitmap
//                     )
//
//                     // 设置当前画布为新创建的空白画布
//                     canvasRef.postValue(newCanvas)
//
//                     // 保存新画布到数据库
//                     repository.addCanvas(newCanvas)
//                 }
//             }
//         } else {
//             return
//         }
//     }
    fun deleteCanvas(onComplete: (CanvasEntity?) -> Unit) {
        viewModelScope.launch {
            val currentId = currentCanvas.value?.id ?: return@launch

            if (currentId != -1) {
                repository.deleteCanvas(currentId)
                Log.d("DeleteCanvas", "Deleted canvas with ID: $currentId")

                // check if there are any canvases left
                val remainingCanvases = repository.getAllCanvasesDirect()
                val nextCanvas = if (remainingCanvases.isEmpty()) {
                    val emptyBitmap = Bitmap.createBitmap(1100, 1100, Bitmap.Config.ARGB_8888)
                    val bitmapFileName = "canvas_${System.currentTimeMillis()}.png"
                    val newCanvas = CanvasEntity(
                        id = -1,
                        filePath = bitmapFileName,
                        bitmap = emptyBitmap
                    )
                    repository.addCanvas(newCanvas)
                    newCanvas
                } else {
                    remainingCanvases.lastOrNull()
                }

                setCurrentCanvas(nextCanvas!!)
                onComplete(nextCanvas) // notify completion
            } else {
                onComplete(null)
                Log.e("DeleteCanvas", "Cannot delete: Current canvas ID is -1")
            }
        }
    }


    fun getCurrentColor(): Color? {
        return color.value
    }

    fun clearCanvas() {
        val emptyBitmap = Bitmap.createBitmap(1100, 1100, Bitmap.Config.ARGB_8888)
        val updatedEntity = canvasRef.value?.copy(bitmap = emptyBitmap)

        Log.d("clear", "clear canvas with ID: ${currentCanvas.value?.id}")

        canvasRef.postValue(updatedEntity!!)

        Log.d("clear", "clear2 canvas with ID: ${currentCanvas.value?.id}")

    }

//    fun clearAllLocalCanvases() {
//        viewModelScope.launch {
//            repository.deleteAll()
//        }
//    }
    fun clearAllLocalCanvases(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAllWithCallback(onComplete)
        }
    }
    // Flood Fill algorithm (non-recursive implementation using a queue)
    fun floodFill(x: Int, y: Int, targetColor: Int, replacementColor: Int, bitmap: Bitmap, maxFillArea: Int = 40000) {
        if (targetColor == replacementColor) return

        val width = bitmap.width
        val height = bitmap.height
        var filledPixels = 0  // 用于记录已填充的像素数量

        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.add(Pair(x, y))

        while (queue.isNotEmpty() && filledPixels < maxFillArea) {
            val (currX, currY) = queue.removeFirst()

            if (bitmap.getPixel(currX, currY) == targetColor) {
                bitmap.setPixel(currX, currY, replacementColor)
                filledPixels++

                // 向四个方向扩展
                if (currX > 0) queue.add(Pair(currX - 1, currY))  // 左
                if (currX < width - 1) queue.add(Pair(currX + 1, currY))  // 右
                if (currY > 0) queue.add(Pair(currX, currY - 1))  // 上
                if (currY < height - 1) queue.add(Pair(currX, currY + 1))  // 下
            }
        }
    }


    fun switchTool(toolNum: Int) {
        // Update the current tool number
        this.toolNum.value = toolNum

        val newColor = when (toolNum) {
            0 -> Color.valueOf(0F, 0F, 0F) // Black
            1 -> Color.valueOf(Color.rgb(200, 200, 200)) // Gray
            2 -> Color.valueOf(Color.RED) // Red
            3 -> Color.valueOf(Color.MAGENTA) // Magenta
            4 -> Color.valueOf(Color.YELLOW) // Yellow
            5 -> Color.valueOf(Color.GREEN) // Green
            6 -> Color.valueOf(Color.BLUE) // Blue
            7 -> Color.valueOf(Color.CYAN) // Cyan
            else -> Color.valueOf(0F, 0F, 0F) // Default to Black
        }

        // Update the color LiveData
        color.value = newColor
    }

    fun setShape(shape: String) {
        currentShape.value = shape
    }

    fun getShape(): String {
        return currentShape.value ?: "pen"
    }



}

// This factory class allows us to define custom constructors for the view model
class DrawingViewModelFactory(private val repository: CanvasRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DrawingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DrawingViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}