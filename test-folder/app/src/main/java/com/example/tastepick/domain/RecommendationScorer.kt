package com.example.tastepick.domain

import kotlin.math.abs

interface RecommendationScorer {
    fun score(
        menus: List<Menu>,
        request: RecommendationRequest,
        preference: UserPreference,
    ): RecommendationResult
}

class KotlinRecommendationScorer : RecommendationScorer {
    override fun score(
        menus: List<Menu>,
        request: RecommendationRequest,
        preference: UserPreference,
    ): RecommendationResult {
        val bannedIngredients = preference.excludedIngredients + preference.allergies
        val safeMenus = menus.filter { menu ->
            bannedIngredients.intersect(menu.ingredients).isEmpty()
        }

        val rankedMenus = safeMenus
            .filter { request.mealTime in it.mealTimeTags }
            .map { menu ->
                RecommendationItem(
                    menu = menu,
                    score = buildScore(menu, request, preference),
                    reason = buildReason(menu, request, preference),
                )
            }
            .sortedByDescending { it.score }
            .take(3)

        if (rankedMenus.isEmpty()) {
            return RecommendationResult.Empty(
                message = "조건에 맞는 메뉴가 없습니다. 제외 재료나 기분 조건을 완화해보세요.",
            )
        }

        return RecommendationResult.Success(rankedMenus)
    }

    private fun buildScore(
        menu: Menu,
        request: RecommendationRequest,
        preference: UserPreference,
    ): Int {
        var score = 0

        if (menu.category in preference.preferredCategories) {
            score += 4
        }

        if (request.mood == Mood.RANDOM || request.mood in menu.moodTags) {
            score += 3
        }

        if (menu.priceTier == request.priceTier) {
            score += 2
        }

        score += 3 - abs(menu.spicyLevel - preference.spicyTolerance).coerceAtMost(3)

        if (menu.id in preference.likedMenuIds) {
            score += 2
        }

        if (menu.id in preference.dislikedMenuIds) {
            score -= 3
        }

        return score
    }

    private fun buildReason(
        menu: Menu,
        request: RecommendationRequest,
        preference: UserPreference,
    ): String {
        val reasons = mutableListOf<String>()

        if (menu.category in preference.preferredCategories) {
            reasons += "선호 카테고리 반영"
        }

        if (request.mood == Mood.RANDOM || request.mood in menu.moodTags) {
            reasons += "현재 기분과 잘 맞음"
        }

        if (menu.priceTier == request.priceTier) {
            reasons += "예산 범위 적합"
        }

        if (menu.id in preference.likedMenuIds) {
            reasons += "이전 좋아요 기록 반영"
        }

        return reasons.take(2).ifEmpty { listOf("기본 추천 결과") }.joinToString(" · ")
    }
}
