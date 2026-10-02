package com.islami.Aha.ui.statistic

import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.islami.Aha.ui.theme.ThemeManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.islami.Aha.R
import com.islami.Aha.ui.theme.*

import com.islami.Aha.ui.statistic.components.*

import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticScreen(viewModel: StatisticViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedPastDate by viewModel.selectedPastDate.collectAsStateWithLifecycle()
    val pastDayHabits by viewModel.pastDayHabits.collectAsStateWithLifecycle()
    val isHaidhMode by com.islami.Aha.util.UserPreferencesManager.isHaidhMode.collectAsStateWithLifecycle()

    StatisticScreenContent(
        uiState = uiState,
        isHaidhMode = isHaidhMode,
        onDayClicked = viewModel::onDayClicked
    )

    if (selectedPastDate != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissPastDayPopup,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            PastDayHabitsList(
                dateKey = selectedPastDate!!,
                habits = pastDayHabits,
                onToggle = viewModel::togglePastDayHabitCompletion,
                onDismiss = viewModel::dismissPastDayPopup
            )
        }
    }
}

@Composable
fun StatisticScreenContent(
    uiState: StatisticUiState,
    isHaidhMode: Boolean = false,
    onDayClicked: (String) -> Unit = {}
) {
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else if (!isHaidhMode && uiState.totalCompleted == 0 && uiState.weeklyStats.all { it.completedCount == 0 }) {
        // Empty State / "Error" State
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.ic_nav_statistic), // Icon statistik
                    contentDescription = "No data",
                    modifier = Modifier.size(120.dp),
                    colorFilter = ColorFilter.tint(Color.Gray.copy(alpha = 0.5f))
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.statistic_no_history_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.statistic_no_history_desc),
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header
            item { StatisticHeader(currentDate = uiState.currentDate) }

            // 2. Today's Progress
            item {
                ProgressCard(
                    completed = uiState.todayCompleted,
                    total = uiState.todayTotal,
                    percentage = uiState.todayPercentage,
                    isHaidhMode = isHaidhMode
                )
            }

            // 3. Summary Cards
            item {
                SummaryCardsRow(
                    totalCompleted = uiState.totalCompleted,
                    longestStreak = uiState.longestStreak,
                    averagePerDay = uiState.averagePerDay
                )
            }

            // 4. Weekly Heatmap
            item { 
                WeeklyHeatmap(
                    weeklyStats = uiState.weeklyStats,
                    onDayClicked = onDayClicked
                ) 
            }

            // 5. Streak Cards
            item {
                StreakCards(
                    currentStreak = uiState.currentStreak,
                    longestStreak = uiState.longestStreak
                )
            }

            // 6. Category Stats
            item {
                Text(
                    text = stringResource(R.string.statistic_category_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            items(uiState.categoryStats) { category ->
                CategoryStatCard(
                    category = category,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            // 7. Coming Soon
            item { ComingSoonCard() }
        }
    }
}
// Extracted StatisticHeader
// Extracted ProgressCard
// Extracted SummaryCardsRow
// Extracted SummaryCard
// Extracted WeeklyHeatmap
// Extracted HeatmapCell

// Extracted StreakCards
// Extracted CategoryStatCard
// Extracted ComingSoonCard


// Extracted StatisticScreenPreview
// Extracted CategoryStatIcon

// Extracted PastDayHabitsList

