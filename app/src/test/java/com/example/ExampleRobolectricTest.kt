package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `verify Munna AI app name resource and home screen elements`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Munna AI", context.getString(R.string.app_name))

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_title_text").assertIsDisplayed()
        composeTestRule.onNodeWithTag("greeting_hindi_text").assertIsDisplayed()

        composeTestRule.onNodeWithTag("home_screen_list")
            .performScrollToNode(hasTestTag("central_mic_button"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("central_mic_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mic_status_label").assertIsDisplayed()
    }

    @Test
    fun `verify navigation between Home, AI Chat, History, and Settings`() {
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("nav_tab_history").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("history_screen_list").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_tab_settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_screen_list").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_tab_chat").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ai_chat_screen").assertIsDisplayed()
    }
}
