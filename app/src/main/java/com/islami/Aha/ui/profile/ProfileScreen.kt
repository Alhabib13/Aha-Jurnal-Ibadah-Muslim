package com.islami.Aha.ui.profile

import android.content.Intent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.islami.Aha.R
import com.islami.Aha.ui.theme.Emerald
import com.islami.Aha.ui.theme.EmeraldDark
import com.islami.Aha.ui.theme.Gray500
import com.islami.Aha.ui.theme.HabitIslamiTheme

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAvatarOptions by remember { mutableStateOf(false) }
    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.updateAvatar(uri.toString())
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    ProfileScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToAdmin = onNavigateToAdmin,
        onNavigateToLogin = onNavigateToLogin,
        onAvatarEditClick = {
            showAvatarOptions = true
        }
    )

    if (showAvatarOptions) {
        AvatarOptionsDialog(
            hasAvatar = !uiState.userInfo.avatarUri.isNullOrBlank(),
            onPickFromGallery = {
                showAvatarOptions = false
                avatarPickerLauncher.launch(arrayOf("image/*"))
            },
            onRemoveAvatar = {
                showAvatarOptions = false
                viewModel.clearAvatar()
            },
            onDismiss = { showAvatarOptions = false }
        )
    }
}

@Composable
private fun AvatarOptionsDialog(
    hasAvatar: Boolean,
    onPickFromGallery: () -> Unit,
    onRemoveAvatar: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Foto Profil",
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = onPickFromGallery,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pilih dari Galeri")
                }
                if (hasAvatar) {
                    TextButton(
                        onClick = onRemoveAvatar,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Hapus Foto",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", color = Gray500)
            }
        }
    )
}

@Composable
fun ProfileScreenContent(
    uiState: ProfileUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateToSettings: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onAvatarEditClick: () -> Unit
) {
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        },
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
                if (uiState.isSaving) {
                    item {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = Emerald,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }

                item {
                    ProfileHeader(
                        userInfo = uiState.userInfo,
                        totalHabits = uiState.totalHabits,
                        totalCompleted = uiState.totalCompleted,
                        currentStreak = uiState.currentStreak,
                        onSettingsClick = onNavigateToSettings,
                        onLoginClick = onNavigateToLogin,
                        onAvatarEditClick = onAvatarEditClick
                    )
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }

                item {
                    Text(
                        text = "\uD83C\uDFC6 Pencapaian Saya",
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

                if (uiState.userInfo.isLoggedIn) {
                    if (uiState.isAdmin) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = onNavigateToAdmin,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AdminPanelSettings,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Panel Admin", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileHeader(
    userInfo: UserInfo,
    totalHabits: Int,
    totalCompleted: Int,
    currentStreak: Int,
    onSettingsClick: () -> Unit,
    onLoginClick: () -> Unit,
    onAvatarEditClick: () -> Unit
) {
    val avatarSize = 88.dp
    val displayName = if (userInfo.isLoggedIn) userInfo.name else "Tamu"

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
                            text = "Login",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }

                Text(
                    text = "Profil",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.align(Alignment.Center)
                )

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(Icons.Outlined.Settings, "Pengaturan", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(avatarSize),
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
                            coil.compose.AsyncImage(
                                model = userInfo.avatarUri,
                                contentDescription = stringResource(R.string.profile_photo_cd),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = if (userInfo.isLoggedIn) userInfo.avatarInitial else "Tamu",
                                fontSize = if (userInfo.isLoggedIn) 28.sp else 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald
                            )
                        }
                    }

                    if (userInfo.isLoggedIn) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.onPrimary, CircleShape)
                                .clickable { onAvatarEditClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = stringResource(R.string.profile_edit_photo_cd),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
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
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
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
                ProfileStat(value = "$totalHabits", label = "Kebiasaan")
                ProfileStat(value = "$totalCompleted", label = "Selesai")
                ProfileStat(value = "$currentStreak", label = "Streak")
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
                    text = "\u2705 Tercapai!",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Emerald
                )
            } else {
                Text(
                    text = "\uD83D\uDD12 Belum",
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
                text = "\uD83D\uDEE0\uFE0F Aktivitas & Pengingat",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            ProfileInfoRow(label = "Habit Sholat Sunnah", value = "$sholatCount")
            Spacer(modifier = Modifier.height(10.dp))
            ProfileInfoRow(label = "Habit Puasa Sunnah", value = "$puasaCount")
            Spacer(modifier = Modifier.height(10.dp))
            ProfileInfoRow(label = "Pengingat Aktif", value = "$reminderCount")
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
            snackbarHostState = remember { SnackbarHostState() },
            onNavigateToSettings = {},
            onNavigateToAdmin = {},
            onNavigateToLogin = {},
            onAvatarEditClick = {}
        )
    }
}
