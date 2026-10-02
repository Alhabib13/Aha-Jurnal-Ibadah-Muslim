package com.islami.Aha.ui.profile

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
import androidx.compose.material.icons.filled.EmojiEvents
import com.islami.Aha.R
import com.islami.Aha.ui.components.AhaLoadingOverlay
import com.islami.Aha.ui.components.AhaToastHost
import com.islami.Aha.ui.theme.Emerald
import com.islami.Aha.ui.theme.EmeraldDark
import com.islami.Aha.ui.theme.HabitIslamiTheme

import com.islami.Aha.ui.profile.components.*


@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var showEditProfileModal by remember { mutableStateOf(false) }
    var pendingAvatarUri by remember { mutableStateOf<Uri?>(null) }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            pendingAvatarUri = uri
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            toastMessage = message
        }
    }

    ProfileScreenContent(
        uiState = uiState,
        toastMessage = toastMessage,
        onToastDismissed = {
            toastMessage = null
            viewModel.clearSnackbar()
        },
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToLogin = onNavigateToLogin,
        onEditProfileClick = { showEditProfileModal = true }
    )

    if (showEditProfileModal) {
        EditProfileDialog(
            userInfo = uiState.userInfo,
            pendingAvatarUri = pendingAvatarUri,
            canChangeUsername = uiState.canChangeUsername,
            daysUntilUsernameChange = uiState.daysUntilUsernameChange,
            isSaving = uiState.isSavingAvatar || uiState.isSavingUsername,
            onPickPhoto = { avatarPickerLauncher.launch(arrayOf("image/*")) },
            onRemovePhoto = {
                pendingAvatarUri = null
                viewModel.clearAvatar()
                showEditProfileModal = false
            },
            onSave = { newAvatarUri, newUsername ->
                showEditProfileModal = false
                pendingAvatarUri = null
                newAvatarUri?.let { viewModel.updateAvatar(it.toString()) }
                newUsername?.let { viewModel.updateUsername(it) }
            },
            onDismiss = {
                showEditProfileModal = false
                pendingAvatarUri = null
            }
        )
    }
}
// Extracted EditProfileDialog


@Composable
fun ProfileScreenContent(
    uiState: ProfileUiState,
    toastMessage: String?,
    onToastDismissed: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onEditProfileClick: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background
        ) { _: PaddingValues ->
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        ProfileHeader(
                            userInfo = uiState.userInfo,
                            totalHabits = uiState.totalHabits,
                            totalCompleted = uiState.totalCompleted,
                            currentStreak = uiState.currentStreak,
                            isSavingAvatar = uiState.isSavingAvatar,
                            onSettingsClick = onNavigateToSettings,
                            onLoginClick = onNavigateToLogin,
                            onEditProfileClick = onEditProfileClick
                        )
                    }

                    item { Spacer(modifier = Modifier.height(20.dp)) }

                    item {
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.profile_achievement_title),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    item {
                        AchievementsGrid(achievements = uiState.achievements)
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }

                    item {
                        ActivityOverviewCard(
                            sholatCount = uiState.sholatCount,
                            puasaCount = uiState.puasaCount,
                            reminderCount = uiState.reminderCount
                        )
                    }
                }
            }
        }

        AhaToastHost(
            message = toastMessage,
            onDismissed = onToastDismissed,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 20.dp)
        )

        AhaLoadingOverlay(
            visible = uiState.isSavingAvatar || uiState.isSavingUsername,
            message = stringResource(R.string.profile_loading_message)
        )
    }
}
// Extracted ProfileHeader
// Extracted ProfileStat
// Extracted AchievementsGrid
// Extracted AchievementCard
// Extracted ActivityOverviewCard
// Extracted ProfileInfoRow


// Extracted ProfileScreenPreview

