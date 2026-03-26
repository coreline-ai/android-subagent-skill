package com.example.tastepick.domain

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GetRecommendationsUseCaseTest {
    @Test
    fun returnsEmptyWhenEveryMenuIsFilteredOut() = runTest {
        val repository = object : MenuRepository {
            override suspend fun getMenus(): List<Menu> {
                return listOf(
                    Menu(
                        id = "peanut-noodle",
                        name = "땅콩국수",
                        category = "면",
                        mealTimeTags = setOf(MealTime.LUNCH),
                        moodTags = setOf(Mood.LIGHT),
                        priceTier = PriceTier.LOW,
                        ingredients = setOf("면", "땅콩"),
                        spicyLevel = 1,
                    ),
                )
            }
        }

        val useCase = GetRecommendationsUseCase(
            menuRepository = repository,
            scorer = KotlinRecommendationScorer(),
        )

        val result = useCase(
            request = RecommendationRequest(
                mealTime = MealTime.LUNCH,
                mood = Mood.LIGHT,
                priceTier = PriceTier.LOW,
            ),
            preference = UserPreference(
                preferredCategories = setOf("면"),
                excludedIngredients = setOf("땅콩"),
                allergies = emptySet(),
                spicyTolerance = 1,
            ),
        )

        assertThat(result).isInstanceOf(RecommendationResult.Empty::class.java)
        assertThat((result as RecommendationResult.Empty).message).contains("조건에 맞는 메뉴가 없습니다")
    }
}
