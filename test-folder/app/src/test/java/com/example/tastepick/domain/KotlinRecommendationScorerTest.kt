package com.example.tastepick.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class KotlinRecommendationScorerTest {
    private val scorer = KotlinRecommendationScorer()

    @Test
    fun excludedIngredientsAreFilteredAndTopThreeAreReturned() {
        val menus = listOf(
            Menu(
                id = "safe-1",
                name = "비빔밥",
                category = "한식",
                mealTimeTags = setOf(MealTime.LUNCH),
                moodTags = setOf(Mood.LIGHT),
                priceTier = PriceTier.MEDIUM,
                ingredients = setOf("쌀", "나물"),
                spicyLevel = 1,
            ),
            Menu(
                id = "safe-2",
                name = "설렁탕",
                category = "국물",
                mealTimeTags = setOf(MealTime.LUNCH),
                moodTags = setOf(Mood.SOUPY),
                priceTier = PriceTier.MEDIUM,
                ingredients = setOf("소고기", "파"),
                spicyLevel = 0,
            ),
            Menu(
                id = "safe-3",
                name = "김치찌개",
                category = "한식",
                mealTimeTags = setOf(MealTime.LUNCH),
                moodTags = setOf(Mood.SPICY, Mood.SOUPY),
                priceTier = PriceTier.LOW,
                ingredients = setOf("김치", "돼지고기"),
                spicyLevel = 3,
            ),
            Menu(
                id = "unsafe-1",
                name = "땅콩면",
                category = "면",
                mealTimeTags = setOf(MealTime.LUNCH),
                moodTags = setOf(Mood.LIGHT),
                priceTier = PriceTier.LOW,
                ingredients = setOf("면", "땅콩"),
                spicyLevel = 1,
            ),
        )

        val result = scorer.score(
            menus = menus,
            request = RecommendationRequest(
                mealTime = MealTime.LUNCH,
                mood = Mood.SOUPY,
                priceTier = PriceTier.MEDIUM,
            ),
            preference = UserPreference(
                preferredCategories = setOf("한식", "국물"),
                excludedIngredients = setOf("땅콩"),
                allergies = emptySet(),
                spicyTolerance = 2,
            ),
        )

        assertThat(result).isInstanceOf(RecommendationResult.Success::class.java)
        val items = (result as RecommendationResult.Success).items
        assertThat(items).hasSize(3)
        assertThat(items.any { "땅콩" in it.menu.ingredients }).isFalse()
        assertThat(items.first().reason).isNotEmpty()
    }
}
