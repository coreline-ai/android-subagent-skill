package com.example.tastepick.ui

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.tastepick.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun rendersTitleAndRecommendButton() {
        composeRule.onNodeWithText("TastePick").assertExists()
        composeRule.onNodeWithText("추천 받기").assertExists()
    }
}
