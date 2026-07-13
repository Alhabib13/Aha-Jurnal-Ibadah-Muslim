package com.islami.Aha.ui.statistic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.islami.Aha.R
import com.islami.Aha.ui.theme.*
import java.util.Locale

// Category gradient pairs
private val categoryGradients = listOf(
    listOf(Color(0xFF0D7C5F), Color(0xFF14A87D)),  // Sholat Fardhu - green
    listOf(Color(0xFF0D9488), Color(0xFF14B8A6)),  // Sholat Sunnah - teal
    listOf(Color(0xFFD97706), Color(0xFFFBBF24)),  // Puasa Wajib - amber
    listOf(Color(0xFFEA580C), Color(0xFFFB923C))   // Puasa Sunnah - orange
)

@Composable
fun StatisticScreen(viewModel: StatisticViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StatisticScreenContent(uiState = uiState)
}

@Composable
fun StatisticScreenContent(uiState: StatisticUiState) {
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Emerald)
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

            // 2. Progress Card
            item {
                ProgressCard(
                    completed = uiState.todayCompleted,
                    total = uiState.todayTotal,
                    percentage = uiState.todayPercentage
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
            item { WeeklyHeatmap(weeklyStats = uiState.weeklyStats) }

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

@Composable
fun StatisticHeader(currentDate: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(colors = listOf(EmeraldDark, Emerald)),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = stringResource(R.string.statistic_screen_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Text(
                text = stringResource(R.string.statistic_header_subtitle),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.statistic_active_date_title),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (currentDate.isNotEmpty()) currentDate else stringResource(R.string.statistic_active_date_unavailable),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.statistic_header_summary),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
fun ProgressCard(completed: Int, total: Int, percentage: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(EmeraldDark, Emerald)
                    )
                )
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.statistic_today_progress_title),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.statistic_today_progress_format, completed, total),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = stringResource(R.string.statistic_today_progress_suffix),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                }

                // Circular progress ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(90.dp)
                ) {
                    val progress = percentage / 100f
                    val onPrimary = MaterialTheme.colorScheme.onPrimary
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(80.dp)) {
                        val strokeWidth = 8.dp.toPx()
                        val diameter = size.minDimension
                        val radius = (diameter - strokeWidth) / 2
                        val topLeft = Offset(
                            (size.width - diameter + strokeWidth) / 2,
                            (size.height - diameter + strokeWidth) / 2
                        )
                        val arcSize = Size(diameter - strokeWidth, diameter - strokeWidth)

                        // Track
                        drawArc(
                            color = onPrimary.copy(alpha = 0.3f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        // Progress
                        drawArc(
                            color = onPrimary,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = stringResource(R.string.statistic_percent_format, percentage),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryCardsRow(totalCompleted: Int, longestStreak: Int, averagePerDay: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryCard(
            icon = Icons.Filled.CheckCircle,
            value = "$totalCompleted",
            label = stringResource(R.string.statistic_summary_total_label),
            backgroundColor = InfoBlue
        )
        SummaryCard(
            icon = Icons.Filled.LocalFireDepartment,
            value = "$longestStreak",
            label = stringResource(R.string.statistic_summary_streak_label),
            backgroundColor = Gold
        )
        SummaryCard(
            icon = Icons.Filled.BarChart,
            value = String.format(Locale.getDefault(), "%.1f", averagePerDay),
            label = stringResource(R.string.statistic_summary_avg_label),
            backgroundColor = Color(0xFF17A2B8)
        )
    }
}

@Composable
fun SummaryCard(icon: ImageVector, value: String, label: String, backgroundColor: Color) {
    Card(
        modifier = Modifier.width(110.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun WeeklyHeatmap(weeklyStats: List<DailyStatistic>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.statistic_weekly_progress_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
            ) {
                weeklyStats.forEach { day ->
                    HeatmapCell(day = day, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun HeatmapCell(day: DailyStatistic, modifier: Modifier = Modifier) {
    val bgColor = when {
        day.completedCount >= 8 -> MaterialTheme.colorScheme.primary
        day.completedCount in 4..7 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        day.completedCount in 1..3 -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        day.completedCount >= 4 -> MaterialTheme.colorScheme.onPrimary
        day.completedCount in 1..3 -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(bgColor)
                .then(
                    if (day.isToday) Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(8.dp)
                    ) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${day.completedCount}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = day.dayName,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun StreakCards(currentStreak: Int, longestStreak: Int) {
    val isDarkTheme = isSystemInDarkTheme()
    val streakContainerColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        GoldLight
    }
    val streakSupportColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.82f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val streakValueColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = streakContainerColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = streakSupportColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(R.string.statistic_streak_label),
                        fontSize = 13.sp,
                        color = streakSupportColor
                    )
                }
                Text(
                    text = stringResource(R.string.statistic_streak_current),
                    fontSize = 12.sp,
                    color = streakSupportColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = pluralStringResource(R.plurals.statistic_streak_days_format, currentStreak, currentStreak),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = streakValueColor
                )
            }
        }
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(R.string.statistic_streak_label),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = stringResource(R.string.statistic_streak_longest),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = pluralStringResource(R.plurals.statistic_streak_days_format, longestStreak, longestStreak),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun CategoryStatCard(category: CategoryStatistic, modifier: Modifier = Modifier) {
    val gradientColors = categoryGradients.getOrElse(category.gradientIndex) {
        listOf(Emerald, EmeraldDark)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryStatIcon(iconKey = category.icon)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = category.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = stringResource(R.string.statistic_percent_format, category.percentage),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(
                        R.string.statistic_category_weekly_format,
                        category.completedCount,
                        category.totalCount
                    ),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = pluralStringResource(R.plurals.statistic_streak_days_format, category.streak, category.streak),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Gradient progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val fraction = (category.percentage / 100f).coerceIn(0f, 1f)
                if (fraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                brush = Brush.horizontalGradient(gradientColors)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun ComingSoonCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.HourglassTop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.statistic_coming_soon_title),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = stringResource(R.string.statistic_coming_soon_desc),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StatisticScreenPreview() {
    HabitIslamiTheme {
        StatisticScreenContent(
            uiState = StatisticUiState(
                isLoading = false,
                currentDate = "Minggu, 08 Februari 2026",
                todayCompleted = 3,
                todayTotal = 18,
                todayPercentage = 17,
                totalCompleted = 3,
                currentStreak = 0,
                longestStreak = 0,
                averagePerDay = 3f,
                weeklyStats = listOf(
                    DailyStatistic("Min", 3, true),
                    DailyStatistic("Sen", 0, false),
                    DailyStatistic("Sel", 0, false),
                    DailyStatistic("Rab", 0, false),
                    DailyStatistic("Kam", 0, false),
                    DailyStatistic("Jum", 0, false),
                    DailyStatistic("Sab", 0, false)
                ),
                categoryStats = listOf(
                    CategoryStatistic("Sholat Fardhu", "masjid", 2, 5, 40, 0, 0),
                    CategoryStatistic("Sholat Sunnah", "sun", 1, 7, 14, 0, 1),
                    CategoryStatistic("Puasa Wajib", "plate", 0, 1, 0, 0, 2),
                    CategoryStatistic("Puasa Sunnah", "moon", 0, 5, 0, 0, 3)
                )
            )
        )
    }
}

@Composable
private fun CategoryStatIcon(iconKey: String) {
    when (iconKey) {
        "masjid", "\uD83D\uDD4C" -> {
            Icon(
                painter = painterResource(id = R.drawable.masjid),
                contentDescription = stringResource(R.string.home_mosque_cd),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
        "sun", "☀️" -> Icon(Icons.Filled.WbSunny, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        "plate", "🍽️" -> Icon(Icons.Filled.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        "moon", "🌙" -> Icon(Icons.Filled.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        else -> Icon(Icons.Filled.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
    }
}
