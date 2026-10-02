package com.islami.Aha.ui.statistic.components

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
fun WeeklyHeatmap(
    weeklyStats: List<DailyStatistic>,
    onDayClicked: (String) -> Unit = {}
) {
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
                    HeatmapCell(
                        day = day, 
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = day.dateKey.isNotEmpty() && !day.isHaidh) {
                                onDayClicked(day.dateKey)
                            }
                    )
                }
            }
            if (weeklyStats.any { it.isHaidh }) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = R.drawable.ic_flower),
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        colorFilter = ColorFilter.tint(androidx.compose.ui.graphics.Color(0xFFF48FB1))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.statistic_haidh_short),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}




@Composable
fun HeatmapCell(day: DailyStatistic, modifier: Modifier = Modifier) {
    val themeMode = ThemeManager.themeMode.collectAsStateWithLifecycle().value
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDark = when (themeMode) {
        com.islami.Aha.ui.theme.ThemeMode.LIGHT -> false
        com.islami.Aha.ui.theme.ThemeMode.DARK -> true
        com.islami.Aha.ui.theme.ThemeMode.SYSTEM -> isSystemDark
    }
    
    val showHaidh = day.isHaidh

    val bgColor = when {
        showHaidh -> androidx.compose.ui.graphics.Color(0xFFF48FB1)
        day.completedCount >= 8 -> if (isDark) EmeraldMuted else EmeraldDark
        day.completedCount in 4..7 -> if (isDark) EmeraldMuted.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary
        day.completedCount in 1..3 -> if (isDark) EmeraldMuted.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        else -> if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = when {
        day.completedCount >= 1 -> if (isDark) androidx.compose.ui.graphics.Color.White else (if (day.completedCount >= 4) androidx.compose.ui.graphics.Color.White else EmeraldDark)
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
                        if (isDark) EmeraldMuted else MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(8.dp)
                    ) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (showHaidh) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.ic_flower),
                    contentDescription = "Cuti",
                    modifier = Modifier.size(16.dp),
                    colorFilter = ColorFilter.tint(androidx.compose.ui.graphics.Color.White)
                )
            } else {
                Text(
                    text = "${day.completedCount}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
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