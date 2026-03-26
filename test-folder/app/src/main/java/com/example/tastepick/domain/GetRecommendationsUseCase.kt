package com.example.tastepick.domain

class GetRecommendationsUseCase(
    private val menuRepository: MenuRepository,
    private val scorer: RecommendationScorer,
) {
    suspend operator fun invoke(
        request: RecommendationRequest,
        preference: UserPreference,
    ): RecommendationResult {
        return scorer.score(
            menus = menuRepository.getMenus(),
            request = request,
            preference = preference,
        )
    }
}
