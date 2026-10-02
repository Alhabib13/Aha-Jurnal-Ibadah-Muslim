package com.islami.Aha.ui.profile.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.islami.Aha.R
import com.islami.Aha.ui.components.AhaLoadingOverlay
import com.islami.Aha.ui.components.AhaToastHost
import com.islami.Aha.ui.theme.Emerald
import com.islami.Aha.ui.theme.EmeraldDark
import com.islami.Aha.ui.theme.HabitIslamiTheme

import com.islami.Aha.ui.profile.*
import coil.compose.AsyncImage

@Composable
fun ProfileScreenPreview() {
    HabitIslamiTheme {
        ProfileScreenContent(
            uiState = ProfileUiState(
                isLoading = false,
                userInfo = UserInfo(
                    name = "Ahmad Fauzi",
                    email = "ahmad@fauzi.com",
                    avatarInitial = "AF",
                    avatarUri = null,
                    isLoggedIn = true
                ),
                totalHabits = 18,
                totalCompleted = 3,
                currentStreak = 0,
                achievements = listOf(
                    Achievement("first_step", "Langkah Pertama", "Selesaikan ibadah pertama", true, 1f),
                    Achievement("burning", "Semangat Membara", "Streak 7 hari berturut", false, 0f),
                    Achievement("consistent", "Bintang Konsisten", "Streak 14 hari berturut", false, 0f),
                    Achievement("champion", "Juara Istiqomah", "Streak 30 hari berturut", false, 0f),
                    Achievement("hundred", "Seratus Ibadah", "100 ibadah total selesai", false, 0.03f),
                    Achievement("sharpshooter", "Penembak Jitu", "Semua habit selesai 1 hari", false, 0f)
                ),
                weeklySummary = WeeklySummary(
                    completionPercentage = 17f,
                    activeDays = 1,
                    totalDays = 7,
                    bestCategory = "Sholat Fardhu"
                )
            ),
            toastMessage = null,
            onToastDismissed = {},
            onNavigateToSettings = {},
            onNavigateToLogin = {},
            onEditProfileClick = {}
        )
    }
}