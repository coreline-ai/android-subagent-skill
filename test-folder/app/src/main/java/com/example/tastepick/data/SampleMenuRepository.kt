package com.example.tastepick.data

import com.example.tastepick.domain.MealTime
import com.example.tastepick.domain.Menu
import com.example.tastepick.domain.MenuRepository
import com.example.tastepick.domain.Mood
import com.example.tastepick.domain.PriceTier

class SampleMenuRepository : MenuRepository {
    override suspend fun getMenus(): List<Menu> {
        return listOf(
            Menu(
                id = "kimchi-jjigae",
                name = "김치찌개",
                category = "한식",
                mealTimeTags = setOf(MealTime.LUNCH, MealTime.DINNER),
                moodTags = setOf(Mood.COMFORTING, Mood.SPICY, Mood.SOUPY),
                priceTier = PriceTier.LOW,
                ingredients = setOf("김치", "돼지고기", "두부"),
                spicyLevel = 3,
            ),
            Menu(
                id = "bibimbap",
                name = "비빔밥",
                category = "한식",
                mealTimeTags = setOf(MealTime.LUNCH, MealTime.DINNER),
                moodTags = setOf(Mood.LIGHT),
                priceTier = PriceTier.MEDIUM,
                ingredients = setOf("쌀", "나물", "계란"),
                spicyLevel = 1,
            ),
            Menu(
                id = "seolleongtang",
                name = "설렁탕",
                category = "국물",
                mealTimeTags = setOf(MealTime.BREAKFAST, MealTime.LUNCH, MealTime.DINNER),
                moodTags = setOf(Mood.COMFORTING, Mood.SOUPY),
                priceTier = PriceTier.MEDIUM,
                ingredients = setOf("소고기", "파", "면"),
                spicyLevel = 0,
            ),
            Menu(
                id = "tteokbokki",
                name = "떡볶이",
                category = "분식",
                mealTimeTags = setOf(MealTime.LUNCH, MealTime.LATE_NIGHT),
                moodTags = setOf(Mood.SPICY),
                priceTier = PriceTier.LOW,
                ingredients = setOf("떡", "고추장", "어묵"),
                spicyLevel = 4,
            ),
            Menu(
                id = "peanut-noodles",
                name = "땅콩 비빔면",
                category = "면",
                mealTimeTags = setOf(MealTime.LUNCH, MealTime.DINNER),
                moodTags = setOf(Mood.LIGHT),
                priceTier = PriceTier.LOW,
                ingredients = setOf("면", "땅콩", "간장"),
                spicyLevel = 1,
            ),
        )
    }
}
