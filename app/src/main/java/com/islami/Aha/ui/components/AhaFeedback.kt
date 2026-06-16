package com.islami.Aha.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.islami.Aha.ui.theme.Emerald
import com.islami.Aha.ui.theme.EmeraldDark
import com.islami.Aha.ui.theme.ErrorRed
import com.islami.Aha.ui.theme.Gray100
import com.islami.Aha.ui.theme.Gray300
import com.islami.Aha.ui.theme.Gray800
import kotlinx.coroutines.delay

enum class AhaToastTone {
    AUTO,
    SUCCESS,
    INFO,
    ERROR
}

@Composable
fun AhaToastHost(
    message: String?,
    modifier: Modifier = Modifier,
    onDismissed: () -> Unit = {},
    durationMillis: Long = 2800L,
    tone: AhaToastTone = AhaToastTone.AUTO
) {
    val latestOnDismissed by rememberUpdatedState(onDismissed)
    val resolvedTone = remember(message, tone) {
        if (tone != AhaToastTone.AUTO) {
            tone
        } else {
            inferToastTone(message)
        }
    }
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val accentColor = when (resolvedTone) {
        AhaToastTone.SUCCESS -> Emerald
        AhaToastTone.ERROR -> ErrorRed
        AhaToastTone.INFO,
        AhaToastTone.AUTO -> MaterialTheme.colorScheme.primary
    }
    val containerColor = when {
        isDarkTheme && resolvedTone == AhaToastTone.ERROR -> Color(0xFF2B1718)
        isDarkTheme -> Color(0xFF14221F)
        resolvedTone == AhaToastTone.ERROR -> Color(0xFFFFF1F0)
        resolvedTone == AhaToastTone.INFO -> Color(0xFFF1F8FF)
        else -> Color(0xFFF2FBF7)
    }
    val textColor = if (isDarkTheme) {
        Color(0xFFF3F4F6)
    } else if (resolvedTone == AhaToastTone.ERROR) {
        Color(0xFF7A1F17)
    } else {
        Gray800
    }
    val borderColor = if (isDarkTheme) {
        accentColor.copy(alpha = 0.28f)
    } else {
        Gray300.copy(alpha = 0.8f)
    }

    LaunchedEffect(message) {
        if (message.isNullOrBlank()) return@LaunchedEffect
        delay(durationMillis)
        latestOnDismissed()
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = !message.isNullOrBlank(),
            enter = slideInVertically(
                initialOffsetY = { -it / 2 },
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(220)),
            exit = slideOutVertically(
                targetOffsetY = { -it / 2 },
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(180))
        ) {
            Card(
                modifier = Modifier.border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(20.dp)
                ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = containerColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(34.dp),
                        shape = CircleShape,
                        color = accentColor.copy(alpha = if (isDarkTheme) 0.22f else 0.14f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (resolvedTone == AhaToastTone.ERROR) {
                                    Icons.Filled.Info
                                } else if (message.isNullOrBlank()) {
                                    Icons.Filled.Info
                                } else {
                                    Icons.Filled.CheckCircle
                                },
                                contentDescription = null,
                                tint = accentColor
                            )
                        }
                    }
                    Text(
                        text = message.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun AhaLoadingOverlay(
    visible: Boolean,
    message: String,
    modifier: Modifier = Modifier,
    minVisibleMillis: Long = 900L
) {
    var renderedVisible by remember { mutableStateOf(false) }
    var lastShownAt by remember { mutableLongStateOf(0L) }
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    LaunchedEffect(visible) {
        if (visible) {
            lastShownAt = System.currentTimeMillis()
            renderedVisible = true
        } else if (renderedVisible) {
            val elapsed = System.currentTimeMillis() - lastShownAt
            val remaining = minVisibleMillis - elapsed
            if (remaining > 0) {
                delay(remaining)
            }
            renderedVisible = false
        }
    }

    AnimatedVisibility(
        visible = renderedVisible,
        enter = fadeIn(animationSpec = tween(180)),
        exit = fadeOut(animationSpec = tween(180)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isDarkTheme) Color(0xB3121824) else Color(0x8AE7EDF3)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (isDarkTheme) {
                                listOf(Color(0xFF102A24), Color(0xFF0B1517))
                            } else {
                                listOf(Color(0xFFFFFFFF), Gray100)
                            }
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = if (isDarkTheme) {
                            Emerald.copy(alpha = 0.24f)
                        } else {
                            EmeraldDark.copy(alpha = 0.14f)
                        },
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(horizontal = 28.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = Emerald,
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    color = if (isDarkTheme) Color.White else Gray800,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tunggu sebentar...",
                    color = if (isDarkTheme) Color(0xFFD1D5DB) else Color(0xFF64748B),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun inferToastTone(message: String?): AhaToastTone {
    if (message.isNullOrBlank()) return AhaToastTone.INFO
    val normalized = message.lowercase()
    return when {
        normalized.contains("gagal") ||
            normalized.contains("salah") ||
            normalized.contains("tidak") ||
            normalized.contains("belum") ||
            normalized.contains("dibatalkan") ||
            normalized.contains("error") -> AhaToastTone.ERROR
        normalized.contains("berhasil") ||
            normalized.contains("selesai") ||
            normalized.contains("aktif") ||
            normalized.contains("diperbarui") ||
            normalized.contains("disimpan") -> AhaToastTone.SUCCESS
        else -> AhaToastTone.INFO
    }
}
