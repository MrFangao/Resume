package com.example.drawingapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SimpleTest {

    @Test
    fun useAppContext() {
        // 获取应用的 Context
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        // 检查应用包名是否正确
        assertEquals("com.example.drawingapp", appContext.packageName)
    }
}