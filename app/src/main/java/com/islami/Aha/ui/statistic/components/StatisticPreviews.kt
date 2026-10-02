package com.islami.Aha.ui.statistic.components

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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

import com.islami.Aha.ui.statistic.*
import com.islami.Aha.domain.model.*

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