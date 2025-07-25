package com.example.drawingapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val drawPath = Path()
    private val viewModel: DrawingViewModel by lazy {
        (context as MainActivity).viewModel
    }
    val emptyBitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
    private val bitmapCanvas = Canvas(emptyBitmap)
    private var startX = 0f
    private var startY = 0f
    private val paint = Paint().apply {
        isAntiAlias = true
        isDither = true
        color = Color.BLACK  // 默认颜色设为黑色
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 10f  // 默认笔宽
    }

    init {
        // 在init块中设置观察者
        viewModel.currentCanvas.observe(context as LifecycleOwner) {
            invalidate()
        }
        viewModel.penSize.observe(context as LifecycleOwner) {
            paint.strokeWidth = it
        }
        paint.color = viewModel.getCurrentColor()?.toArgb() ?: Color.BLACK
        paint.strokeWidth = viewModel.penSize.value ?: 10f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bitmap = viewModel.getBitMap()
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        val canvasEntity = viewModel.currentCanvas
        Log.e("draw", "" + canvasEntity.value?.id)
        canvas.drawPath(drawPath, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = x
                startY = y
                paint.color = viewModel.getCurrentColor()?.toArgb() ?: Color.BLACK

                val currentTool = viewModel.toolNum.value
                if (currentTool == 8) {
                    val bitmap = viewModel.getBitMap() ?: return false
                    val targetColor = bitmap.getPixel(x.toInt(), y.toInt())
                    val replacementColor = viewModel.getCurrentColor()?.toArgb() ?: return false

                    viewModel.floodFill(x.toInt(), y.toInt(), targetColor, replacementColor, bitmap, 40000)
                    invalidate()
                } else {
                    drawPath.moveTo(x, y)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val currentTool = viewModel.toolNum.value
                if (currentTool != 8) {
                    when(viewModel.getShape()) {
                        "rectangle" -> {
                            drawPath.reset()
                            drawPath.addRect(startX, startY, x, y, Path.Direction.CW)
                        }
                        "circle" -> {
                            drawPath.reset()
                            val radius = Math.sqrt(((x - startX) * (x - startX) + (y - startY) * (y - startY)).toDouble()).toFloat()
                            drawPath.addCircle(startX, startY, radius, Path.Direction.CW)
                        }
                        "pen" -> {
                            drawPath.lineTo(x, y)
                        }
                    }
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                val currentTool = viewModel.toolNum.value
                if (currentTool != 8) {
                    drawOnBitmap()
                    drawPath.reset()
                }
            }
        }
        return true
    }

    private fun drawOnBitmap() {
        val canvasMap = viewModel.getBitMap() ?: return
        val canvas = Canvas(canvasMap)
        canvas.drawPath(drawPath, paint)
        invalidate()
    }
}
