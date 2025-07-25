package com.example.drawingapp.room

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

@RunWith(AndroidJUnit4::class)
class CanvasTest {

    private lateinit var db: CanvasDatabase
    private lateinit var canvasDao: CanvasDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, CanvasDatabase::class.java
        ).allowMainThreadQueries().build()
        canvasDao = db.canvasDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    // 测试插入和读取 CanvasEntity
    @Test
    fun testInsertAndGetCanvas() = runBlocking {
        val testCanvas = CanvasEntity(
            filePath = "test_path_1",
            bitmap = createTestBitmap()
        )
        val insertedId = canvasDao.insert(testCanvas)

        val retrievedCanvas = canvasDao.getCanvas(insertedId.toInt()).first()

        assertNotNull(retrievedCanvas)
        assertEquals(insertedId.toInt(), retrievedCanvas?.id)
        assertEquals("test_path_1", retrievedCanvas?.filePath)
    }

    // 测试获取所有 CanvasEntity
    @Test
    fun testGetAllCanvases() = runBlocking {
        val testCanvas1 = CanvasEntity(
            filePath = "test_path_1",
            bitmap = createTestBitmap()
        )
        val testCanvas2 = CanvasEntity(
            filePath = "test_path_2",
            bitmap = createTestBitmap()
        )
        canvasDao.insert(testCanvas1)
        canvasDao.insert(testCanvas2)

        val allCanvases = canvasDao.getAllCanvases().first()

        assertEquals(2, allCanvases.size)
        assertEquals("test_path_1", allCanvases[0].filePath)
        assertEquals("test_path_2", allCanvases[1].filePath)
    }

    // 测试更新 CanvasEntity
    @Test
    fun testUpdateCanvas() = runBlocking {
        val testCanvas = CanvasEntity(
            filePath = "test_path_1",
            bitmap = createTestBitmap()
        )
        val insertedId = canvasDao.insert(testCanvas)

        val updatedCanvas = CanvasEntity(
            id = insertedId.toInt(),
            filePath = "updated_path",
            bitmap = createTestBitmap()
        )
        canvasDao.insert(updatedCanvas) // 由于使用了 REPLACE 策略，insert 可以用来更新

        val retrievedCanvas = canvasDao.getCanvas(insertedId.toInt()).first()

        assertNotNull(retrievedCanvas)
        assertEquals("updated_path", retrievedCanvas?.filePath)
    }

    // 测试删除 CanvasEntity
    @Test
    fun testDeleteCanvas() = runBlocking {
        val testCanvas = CanvasEntity(
            filePath = "test_path_1",
            bitmap = createTestBitmap()
        )
        val insertedId = canvasDao.insert(testCanvas)

        val canvasToDelete = testCanvas.copy(id = insertedId.toInt())
        canvasDao.deleteCanvas(canvasToDelete)

        val retrievedCanvas = canvasDao.getCanvas(insertedId.toInt()).firstOrNull()

        assertNull(retrievedCanvas)
    }

    // 测试获取最新的 CanvasEntity
    @Test
    fun testGetLatestCanvas() = runBlocking {
        val testCanvas1 = CanvasEntity(
            filePath = "test_path_1",
            bitmap = createTestBitmap()
        )
        val testCanvas2 = CanvasEntity(
            filePath = "test_path_2",
            bitmap = createTestBitmap()
        )
        canvasDao.insert(testCanvas1)
        canvasDao.insert(testCanvas2)

        val latestCanvas = canvasDao.latestCanvas().first()

        assertNotNull(latestCanvas)
        assertEquals("test_path_2", latestCanvas.filePath)
    }

    // 辅助函数，创建测试用的 Bitmap
    private fun createTestBitmap(): android.graphics.Bitmap {
        return android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
    }
}