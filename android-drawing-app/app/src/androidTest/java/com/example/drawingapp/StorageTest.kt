package com.example.drawingapp

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.drawingapp.room.CanvasDao
import com.example.drawingapp.room.CanvasDatabase
import com.example.drawingapp.room.CanvasEntity
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class StorageTest {

    private lateinit var db: CanvasDatabase
    private lateinit var dao: CanvasDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, CanvasDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.canvasDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    // 测试 1：插入并检索 CanvasEntity
    @Test
    fun testInsertAndRetrieveCanvas() = runBlocking {
        val testCanvas = CanvasEntity(
            filePath = "test_path",
            bitmap = createTestBitmap()
        )

        val id = dao.insert(testCanvas)

        val retrievedCanvas = dao.getCanvas(id.toInt()).first()

        assertNotNull(retrievedCanvas)
        assertEquals("test_path", retrievedCanvas!!.filePath)
    }

    // 测试 2：插入多个 CanvasEntity 并获取所有记录
    @Test
    fun testGetAllCanvases() = runBlocking {
        val canvas1 = CanvasEntity(
            filePath = "path1",
            bitmap = createTestBitmap()
        )
        val canvas2 = CanvasEntity(
            filePath = "path2",
            bitmap = createTestBitmap()
        )

        dao.insert(canvas1)
        dao.insert(canvas2)

        val allCanvases = dao.getAllCanvases().first()

        assertEquals(2, allCanvases.size)
        assertEquals("path1", allCanvases[0].filePath)
        assertEquals("path2", allCanvases[1].filePath)
    }

    // 测试 3：更新现有的 CanvasEntity
    @Test
    fun testUpdateCanvas() = runBlocking {
        val originalCanvas = CanvasEntity(
            filePath = "original_path",
            bitmap = createTestBitmap()
        )

        val id = dao.insert(originalCanvas)

        val updatedCanvas = CanvasEntity(
            id = id.toInt(),
            filePath = "updated_path",
            bitmap = createTestBitmap()
        )

        dao.insert(updatedCanvas) // 使用 REPLACE 策略更新

        val retrievedCanvas = dao.getCanvas(id.toInt()).first()

        assertNotNull(retrievedCanvas)
        assertEquals("updated_path", retrievedCanvas!!.filePath)
    }

    // 测试 4：删除 CanvasEntity
    @Test
    fun testDeleteCanvas() = runBlocking {
        val canvas = CanvasEntity(
            filePath = "delete_path",
            bitmap = createTestBitmap()
        )

        val id = dao.insert(canvas)

        val canvasToDelete = canvas.copy(id = id.toInt())

        dao.deleteCanvas(canvasToDelete)

        val retrievedCanvas = dao.getCanvas(id.toInt()).firstOrNull()

        assertNull(retrievedCanvas)
    }

    // 测试 5：获取最新插入的 CanvasEntity
    @Test
    fun testGetLatestCanvas() = runBlocking {
        val canvas1 = CanvasEntity(
            filePath = "first_canvas",
            bitmap = createTestBitmap()
        )
        val canvas2 = CanvasEntity(
            filePath = "latest_canvas",
            bitmap = createTestBitmap()
        )

        dao.insert(canvas1)
        dao.insert(canvas2)

        val latestCanvas = dao.latestCanvas().first()

        assertNotNull(latestCanvas)
        assertEquals("latest_canvas", latestCanvas.filePath)
    }

    // 辅助函数，创建测试用的 Bitmap
    private fun createTestBitmap(): android.graphics.Bitmap {
        return android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
    }
}