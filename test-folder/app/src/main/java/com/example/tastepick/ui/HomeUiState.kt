package com.example.tastepick.ui

import com.example.tastepick.domain.MealTime
import com.example.tastepick.domain.Mood
import com.example.tastepick.domain.PriceTier
import com.example.tastepick.domain.RecommendationItem

data class HomeUiState(
    val selectedMealTime: MealTime = MealTime.LUNCH,
    val selectedMood: Mood = Mood.COMFORTING,
    val selectedPriceTier: PriceTier = PriceTier.MEDIUM,
    val isLoading: Boolean = false,
    val recommendations: List<RecommendationItem> = emptyList(),
    val emptyMessage: String? = null,
    val feedbackMessage: String? = null,
)
