package com.example.tastepick.ui

import com.google.common.truth.Truth.assertThat
import com.example.tastepick.domain.GetRecommendationsUseCase
import com.example.tastepick.domain.KotlinRecommendationScorer
import com.example.tastepick.domain.MealTime
import com.example.tastepick.domain.Menu
import com.example.tastepick.domain.MenuRepository
import com.example.tastepick.domain.Mood
import com.example.tastepick.domain.PriceTier
import com.example.tastepick.domain.UserPreference
import com.example.tastepick.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadRecommendationsUpdatesUiStateWithResults() = runTest {
        val repository = object : MenuRepository {
            override suspend fun getMenus(): List<Menu> {
                return listOf(
                    Menu(
                        id = "kimchi-jjigae",
                        name = "김치찌개",
                        category = "한식",
                        mealTimeTags = setOf(MealTime.LUNCH),
                        moodTags = setOf(Mood.COMFORTING, Mood.SOUPY),
                        priceTier = PriceTier.MEDIUM,
                        ingredients = setOf("김치", "두부"),
                        spicyLevel = 2,
                    ),
                )
            }
        }

        val viewModel = HomeViewModel(
            getRecommendationsUseCase = GetRecommendationsUseCase(
                menuRepository = repository,
                scorer = KotlinRecommendationScorer(),
            ),
            defaultPreference = UserPreference(
                preferredCategories = setOf("한식"),
                excludedIngredients = emptySet(),
                allergies = emptySet(),
                spicyTolerance = 2,
            ),
            recommendationDispatcher = mainDispatcherRule.testDispatcher,
        )

        viewModel.loadRecommendations()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.recommendations).isNotEmpty()
        assertThat(state.emptyMessage).isNull()
    }
}
