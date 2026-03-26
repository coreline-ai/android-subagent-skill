package com.example.tastepick.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tastepick.data.SampleMenuRepository
import com.example.tastepick.domain.GetRecommendationsUseCase
import com.example.tastepick.domain.KotlinRecommendationScorer
import com.example.tastepick.domain.MealTime
import com.example.tastepick.domain.Mood
import com.example.tastepick.domain.PriceTier
import com.example.tastepick.domain.RecommendationRequest
import com.example.tastepick.domain.RecommendationResult
import com.example.tastepick.domain.UserPreference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(
    private val getRecommendationsUseCase: GetRecommendationsUseCase = GetRecommendationsUseCase(
        menuRepository = SampleMenuRepository(),
        scorer = KotlinRecommendationScorer(),
    ),
    private val defaultPreference: UserPreference = UserPreference(
        preferredCategories = setOf("한식", "국물"),
        excludedIngredients = setOf("땅콩"),
        allergies = emptySet(),
        spicyTolerance = 2,
        likedMenuIds = setOf("kimchi-jjigae"),
    ),
    private val recommendationDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun updateMealTime(mealTime: MealTime) {
        _uiState.update { it.copy(selectedMealTime = mealTime) }
    }

    fun updateMood(mood: Mood) {
        _uiState.update { it.copy(selectedMood = mood) }
    }

    fun updatePriceTier(priceTier: PriceTier) {
        _uiState.update { it.copy(selectedPriceTier = priceTier) }
    }

    fun loadRecommendations() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    emptyMessage = null,
                    feedbackMessage = null,
                )
            }

            val request = RecommendationRequest(
                mealTime = uiState.value.selectedMealTime,
                mood = uiState.value.selectedMood,
                priceTier = uiState.value.selectedPriceTier,
            )

            val result = withContext(recommendationDispatcher) {
                getRecommendationsUseCase(request, defaultPreference)
            }

            _uiState.update { state ->
                when (result) {
                    is RecommendationResult.Success -> {
                        state.copy(
                            isLoading = false,
                            recommendations = result.items,
                            emptyMessage = null,
                        )
                    }

                    is RecommendationResult.Empty -> {
                        state.copy(
                            isLoading = false,
                            recommendations = emptyList(),
                            emptyMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    fun recordFeedback(menuId: String, liked: Boolean) {
        _uiState.update {
            it.copy(
                feedbackMessage = if (liked) {
                    "$menuId 좋아요 반영"
                } else {
                    "$menuId 별로예요 반영"
                },
            )
        }
    }
}
