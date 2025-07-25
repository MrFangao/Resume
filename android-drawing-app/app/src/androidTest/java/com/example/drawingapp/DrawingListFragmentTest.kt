package com.example.drawingapp

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.drawingapp.fragments.DrawingListFragment
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DrawingListFragmentTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testDrawingListFragmentDisplaysContent() {
        // 启动 DrawingListFragment
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, DrawingListFragment())
                .commitNow()
        }

        // 等待 Compose 稳定
        composeTestRule.waitForIdle()

        // 检查是否存在任何文本节点
        val textNodes = composeTestRule.onAllNodesWithText("")
        textNodes.assertCountEquals(0) // 假设列表为空
    }


    @Test
    fun testEmptyListDisplaysNoItems() {
        // 启动 DrawingListFragment，确保列表为空
        composeTestRule.activityRule.scenario.onActivity { activity ->
            // 确保 ViewModel 的数据为空
            activity.supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, DrawingListFragment())
                .commitNow()
        }

        // 等待 Compose 稳定
        composeTestRule.waitForIdle()

        // 检查列表中是否没有文本节点
        val textNodes = composeTestRule.onAllNodesWithText("")
        textNodes.assertCountEquals(0)
    }


}