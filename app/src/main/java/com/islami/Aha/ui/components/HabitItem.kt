package com.islami.Aha.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.islami.Aha.R
import com.islami.Aha.data.model.Habit
import com.islami.Aha.ui.theme.*

@Composable
fun HabitItem(
    habit: Habit,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (habit.isCompleted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        label = "bgColor"
    )
    val checkScale by animateFloatAsState(
        targetValue = if (habit.isCompleted) 1f else 0.8f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "checkScale"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (habit.isCompleted) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Background
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (habit.isCompleted) Emerald.copy(alpha = 0.15f) else EmeraldLight,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (habit.icon == "masjid" || habit.icon == "\uD83D\uDD4C") {
                        Image(
                            painter = painterResource(id = R.drawable.masjid),
                            contentDescription = habit.name,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = mapHabitIcon(habit.icon),
                            contentDescription = habit.name,
                            tint = Emerald,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Name & Description
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (habit.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                if (habit.description.isNotEmpty()) {
                    Text(
                        text = habit.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Custom Circular Checkbox
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .scale(checkScale)
                    .clip(CircleShape)
                    .then(
                        if (habit.isCompleted) {
                            Modifier.background(Emerald, CircleShape)
                        } else {
                            Modifier.border(2.dp, Emerald, CircleShape)
                        }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onCheckedChange(!habit.isCompleted) },
                contentAlignment = Alignment.Center
            ) {
                if (habit.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selesai",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun mapHabitIcon(icon: String): ImageVector {
    return when (icon) {
        "sunrise", "sun", "\uD83C\uDF05", "\u2600\uFE0F" -> Icons.Filled.WbSunny
        "cloud", "\u2601\uFE0F" -> Icons.Filled.Cloud
        "moon", "night", "\uD83C\uDF19", "\uD83C\uDF1C" -> Icons.Filled.DarkMode
        "plate", "\uD83C\uDF7D\uFE0F" -> Icons.Filled.Restaurant
        else -> Icons.Outlined.AccessTime
    }
}

@Preview(showBackground = true)
@Composable
fun HabitItemPreview() {
    HabitIslamiTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HabitItem(
                habit = Habit(1, "Sholat Subuh", "Sholat Fardhu", "sunrise", "04:30 WIB", false),
                onCheckedChange = {}
            )
            HabitItem(
                habit = Habit(2, "Sholat Dzuhur", "Sholat Fardhu", "sun", "11:55 WIB", true),
                onCheckedChange = {}
            )
        }
    }
}

