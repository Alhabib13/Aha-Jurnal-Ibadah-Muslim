package com.islami.Aha.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.islami.Aha.R
import com.islami.Aha.ui.theme.Emerald
import com.islami.Aha.ui.theme.EmeraldDark
import com.islami.Aha.ui.theme.GoldShimmer
import com.islami.Aha.util.GenderProfile

@Composable
fun HomeHeader(
    isLoggedIn: Boolean,
    userName: String,
    currentTime: String,
    gregorianDate: String,
    hijriDate: String,
    location: String,
    nextPrayerName: String,
    nextPrayerTimeRemaining: String,
    nextPrayerProgress: Float,
    prayerTimeStatusText: String = "",
    showRamadanSchedule: Boolean = false,
    ramadanImsakTime: String = "",
    ramadanIftarTime: String = "",
    ramadanStatusText: String = "",
    isLocationLoading: Boolean = false,
    onRefreshLocation: () -> Unit = {},
    isHaidhMode: Boolean = false,
    genderProfile: GenderProfile = GenderProfile.UNSPECIFIED,
    onToggleHaidhMode: (Boolean) -> Unit = {}
) {
    val greetingText = if (isLoggedIn && userName.isNotBlank()) {
        "Assalamu'alaikum, $userName \uD83D\uDC4B"
    } else {
        "Assalamu'alaikum \uD83D\uDC4B"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(brush = Brush.verticalGradient(colors = listOf(EmeraldDark, Emerald)))
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = greetingText,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )
                
                if (genderProfile == GenderProfile.FEMALE) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                if (isHaidhMode) EmeraldDark.copy(alpha = 0.5f) else Color.Transparent, 
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp, 
                                if (isHaidhMode) Color.Transparent else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f), 
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onToggleHaidhMode(!isHaidhMode) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.home_haidh_mode_label),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = isHaidhMode,
                            onCheckedChange = null,
                            modifier = Modifier.height(16.dp).width(32.dp).scale(0.6f),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = Emerald
                            )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currentTime,
                fontSize = 48.sp,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = gregorianDate, fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f))
                Text(
                    text = stringResource(R.string.home_date_separator),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                    fontSize = 13.sp
                )
                Text(text = hijriDate, fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f))
            }

            // Location with refresh button
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = stringResource(R.string.home_location_cd),
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = location,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                )
                if (isLocationLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 1.5.dp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    )
                } else {
                    IconButton(
                        onClick = onRefreshLocation,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.home_refresh_location_cd),
                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            if (prayerTimeStatusText.isNotBlank()) {
                Text(
                    text = prayerTimeStatusText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                )
            }

            // Next prayer info
            if (nextPrayerName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Mosque,
                                contentDescription = stringResource(R.string.home_mosque_cd),
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(
                                    R.string.home_next_prayer_format,
                                    nextPrayerName,
                                    nextPrayerTimeRemaining
                                ),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { nextPrayerProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = GoldShimmer,
                            trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                        )
                    }
                }
            }

            if (showRamadanSchedule) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(
                                R.string.home_ramadan_schedule_summary,
                                ramadanImsakTime,
                                ramadanIftarTime
                            ),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        if (ramadanStatusText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = ramadanStatusText,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }
    }
}
