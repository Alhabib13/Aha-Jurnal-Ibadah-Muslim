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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
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

@Composable
private fun EditProfileDialog(
    userInfo: UserInfo,
    pendingAvatarUri: Uri?,
    canChangeUsername: Boolean,
    daysUntilUsernameChange: Int,
    isSaving: Boolean,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSave: (newAvatarUri: Uri?, newUsername: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var nameInput by remember(userInfo.name) { mutableStateOf(userInfo.name) }
    val hasAvatarChange = pendingAvatarUri != null
    val hasNameChange = nameInput.trim().isNotBlank() &&
        nameInput.trim() != userInfo.name &&
        canChangeUsername
    val canSave = (hasAvatarChange || hasNameChange) && !isSaving

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.profile_edit_profile_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))

                // Preview foto
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        pendingAvatarUri != null -> {
                            coil.compose.AsyncImage(
                                model = pendingAvatarUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Emerald)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.profile_photo_new_badge),
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        !userInfo.avatarUri.isNullOrBlank() -> {
                            val bytes = remember(userInfo.avatarUri) {
                                if (userInfo.avatarUri.startsWith("data:image")) {
                                    android.util.Base64.decode(
                                        userInfo.avatarUri.substringAfter("base64,"),
                                        android.util.Base64.DEFAULT
                                    )
                                } else null
                            }
                            if (bytes != null) {
                                coil.compose.AsyncImage(
                                    model = bytes,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(userInfo.avatarInitial, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Emerald)
                            }
                        }
                        else -> Text(userInfo.avatarInitial, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Emerald)
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onPickPhoto) {
                        Text(
                            text = stringResource(R.string.profile_pick_photo_action),
                            color = Emerald,
                            fontSize = 13.sp
                        )
                    }
                    if (!userInfo.avatarUri.isNullOrBlank() || pendingAvatarUri != null) {
                        TextButton(onClick = onRemovePhoto) {
                            Text(
                                text = stringResource(R.string.profile_remove_photo_action),
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    text = stringResource(R.string.profile_username_label),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    placeholder = { Text(userInfo.name) },
                    singleLine = true,
                    enabled = canChangeUsername,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (canSave) onSave(
                            if (hasAvatarChange) pendingAvatarUri else null,
                            if (hasNameChange) nameInput.trim() else null
                        )
                    })
                )

                if (!canChangeUsername) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = pluralStringResource(
                            R.plurals.profile_username_locked_days_format,
                            daysUntilUsernameChange,
                            daysUntilUsernameChange
                        ),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            onSave(
                                if (hasAvatarChange) pendingAvatarUri else null,
                                if (hasNameChange) nameInput.trim() else null
                            )
                        },
                        enabled = canSave
                    ) {
                        Text(
                            stringResource(R.string.common_save),
                            color = if (canSave) Emerald else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

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
                    CircularProgressIndicator(color = Emerald)
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
                        Text(
                            text = stringResource(R.string.profile_achievement_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
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

@Composable
fun ProfileHeader(
    userInfo: UserInfo,
    totalHabits: Int,
    totalCompleted: Int,
    currentStreak: Int,
    isSavingAvatar: Boolean = false,
    onSettingsClick: () -> Unit,
    onLoginClick: () -> Unit,
    onEditProfileClick: () -> Unit = {}
) {
    val avatarSize = 88.dp
    val displayName = if (userInfo.isLoggedIn) userInfo.name else stringResource(R.string.profile_guest_name)

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 320.dp)
                .background(
                    brush = Brush.verticalGradient(colors = listOf(EmeraldDark, Emerald)),
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                )
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .align(Alignment.TopCenter)
            ) {
                if (!userInfo.isLoggedIn) {
                    OutlinedButton(
                        onClick = onLoginClick,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.08f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary),
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Text(
                            text = stringResource(R.string.profile_login_button),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = stringResource(R.string.profile_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.profile_settings_cd),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(avatarSize),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimary)
                            .border(3.dp, MaterialTheme.colorScheme.onPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (userInfo.isLoggedIn && userInfo.avatarUri != null) {
                            val imageModel = remember(userInfo.avatarUri) {
                                if (userInfo.avatarUri.startsWith("data:image")) {
                                    val base64Data = userInfo.avatarUri.substringAfter("base64,")
                                    android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                                } else {
                                    userInfo.avatarUri
                                }
                            }
                            coil.compose.AsyncImage(
                                model = imageModel,
                                contentDescription = stringResource(R.string.profile_photo_cd),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = if (userInfo.isLoggedIn) userInfo.avatarInitial else stringResource(R.string.profile_guest_name),
                                fontSize = if (userInfo.isLoggedIn) 28.sp else 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald
                            )
                        }
                    }

                    // Loading overlay saat upload foto
                    if (isSavingAvatar) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = androidx.compose.ui.graphics.Color.White,
                                strokeWidth = 3.dp
                            )
                        }
                    }

                    // Tombol edit profil (sembunyikan saat loading avatar)
                    if (userInfo.isLoggedIn && !isSavingAvatar) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 11.dp, y = 11.dp)
                                .size(48.dp)
                                .clickable { onEditProfileClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.onPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = stringResource(R.string.profile_edit_profile_cd),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = displayName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )

                if (userInfo.isLoggedIn && userInfo.email.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = userInfo.email,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStat(value = "$totalHabits", label = stringResource(R.string.profile_stat_habits))
                ProfileStat(value = "$totalCompleted", label = stringResource(R.string.profile_stat_completed))
                ProfileStat(value = "$currentStreak", label = stringResource(R.string.profile_stat_streak))
            }
        }
    }
}

@Composable
fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Emerald
        )
        Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
    }
}

@Composable
fun AchievementsGrid(achievements: List<Achievement>) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        achievements.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { achievement ->
                    AchievementCard(
                        achievement = achievement,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun AchievementCard(achievement: Achievement, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (achievement.isUnlocked) 2.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = achievement.emoji,
                fontSize = 32.sp,
                modifier = Modifier.alpha(if (achievement.isUnlocked) 1f else 0.3f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = achievement.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = achievement.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (achievement.isUnlocked) {
                Text(
                    text = stringResource(R.string.profile_achievement_reached),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Emerald
                )
            } else {
                Text(
                    text = stringResource(R.string.profile_achievement_not_yet),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ActivityOverviewCard(
    sholatCount: Int,
    puasaCount: Int,
    reminderCount: Int
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
                text = stringResource(R.string.profile_activity_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            ProfileInfoRow(label = stringResource(R.string.profile_activity_sholat), value = "$sholatCount")
            Spacer(modifier = Modifier.height(10.dp))
            ProfileInfoRow(label = stringResource(R.string.profile_activity_puasa), value = "$puasaCount")
            Spacer(modifier = Modifier.height(10.dp))
            ProfileInfoRow(label = stringResource(R.string.profile_activity_reminder), value = "$reminderCount")
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
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
                    Achievement("first_step", "\uD83C\uDF1F", "Langkah Pertama", "Selesaikan ibadah pertama", true, 1f),
                    Achievement("burning", "\uD83D\uDD25", "Semangat Membara", "Streak 7 hari berturut", false, 0f),
                    Achievement("consistent", "\u2B50", "Bintang Konsisten", "Streak 14 hari berturut", false, 0f),
                    Achievement("champion", "\uD83C\uDFC6", "Juara Istiqomah", "Streak 30 hari berturut", false, 0f),
                    Achievement("hundred", "\uD83D\uDCAF", "Seratus Ibadah", "100 ibadah total selesai", false, 0.03f),
                    Achievement("sharpshooter", "\uD83C\uDFAF", "Penembak Jitu", "Semua habit selesai 1 hari", false, 0f)
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
