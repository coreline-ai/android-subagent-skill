package com.example.tastepick.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tastepick.domain.MealTime
import com.example.tastepick.domain.Mood
import com.example.tastepick.domain.PriceTier

@Composable
fun TastePickApp(
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = viewModel(),
) {
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()

    MaterialTheme {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            HomeScreen(
                uiState = uiState,
                onMealTimeSelected = homeViewModel::updateMealTime,
                onMoodSelected = homeViewModel::updateMood,
                onPriceTierSelected = homeViewModel::updatePriceTier,
                onRecommendClicked = homeViewModel::loadRecommendations,
                onFeedbackSelected = homeViewModel::recordFeedback,
            )
        }
    }
}

@Composable
private fun HomeScreen(
    uiState: HomeUiState,
    onMealTimeSelected: (MealTime) -> Unit,
    onMoodSelected: (Mood) -> Unit,
    onPriceTierSelected: (PriceTier) -> Unit,
    onRecommendClicked: () -> Unit,
    onFeedbackSelected: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "TastePick",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(text = "오늘의 메뉴를 빠르게 추천받으세요.")

        SelectionRow(
            title = "식사 시간",
            options = MealTime.entries,
            selectedOption = uiState.selectedMealTime,
            label = { it.name },
            onSelected = onMealTimeSelected,
        )

        SelectionRow(
            title = "기분",
            options = Mood.entries,
            selectedOption = uiState.selectedMood,
            label = { it.name },
            onSelected = onMoodSelected,
        )

        SelectionRow(
            title = "예산",
            options = PriceTier.entries,
            selectedOption = uiState.selectedPriceTier,
            label = { it.name },
            onSelected = onPriceTierSelected,
        )

        Button(
            onClick = onRecommendClicked,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uiState.isLoading) "추천 계산 중..." else "추천 받기")
        }

        uiState.emptyMessage?.let { emptyMessage ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = emptyMessage,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        uiState.recommendations.forEach { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = item.menu.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(text = "카테고리: ${item.menu.category}")
                    Text(text = "추천 이유: ${item.reason}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onFeedbackSelected(item.menu.id, true) }) {
                            Text("좋아요")
                        }
                        OutlinedButton(onClick = { onFeedbackSelected(item.menu.id, false) }) {
                            Text("별로예요")
                        }
                    }
                }
            }
        }

        uiState.feedbackMessage?.let { message ->
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun <T> SelectionRow(
    title: String,
    options: List<T>,
    selectedOption: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = title, fontWeight = FontWeight.Medium)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                if (isSelected) {
                    Button(onClick = { onSelected(option) }) {
                        Text(label(option))
                    }
                } else {
                    OutlinedButton(onClick = { onSelected(option) }) {
                        Text(label(option))
                    }
                }
            }
        }
    }
}
