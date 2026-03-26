package com.example.tastepick.domain

enum class MealTime {
    BREAKFAST,
    LUNCH,
    DINNER,
    LATE_NIGHT,
}

enum class Mood {
    COMFORTING,
    LIGHT,
    SPICY,
    SOUPY,
    RANDOM,
}

enum class PriceTier {
    LOW,
    MEDIUM,
}

data class RecommendationRequest(
    val mealTime: MealTime,
    val mood: Mood,
    val priceTier: PriceTier,
)

data class UserPreference(
    val preferredCategories: Set<String>,
    val excludedIngredients: Set<String>,
    val allergies: Set<String>,
    val spicyTolerance: Int,
    val likedMenuIds: Set<String> = emptySet(),
    val dislikedMenuIds: Set<String> = emptySet(),
)

data class Menu(
    val id: String,
    val name: String,
    val category: String,
    val mealTimeTags: Set<MealTime>,
    val moodTags: Set<Mood>,
    val priceTier: PriceTier,
    val ingredients: Set<String>,
    val spicyLevel: Int,
)

data class RecommendationItem(
    val menu: Menu,
    val score: Int,
    val reason: String,
)

sealed interface RecommendationResult {
    data class Success(val items: List<RecommendationItem>) : RecommendationResult

    data class Empty(val message: String) : RecommendationResult
}

interface MenuRepository {
    suspend fun getMenus(): List<Menu>
}
