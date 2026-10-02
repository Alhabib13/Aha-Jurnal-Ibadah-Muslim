package com.islami.Aha.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.islami.Aha.data.model.Habit
import com.islami.Aha.R
import com.islami.Aha.domain.model.SunnahHabit
import com.islami.Aha.ui.addhabit.SunnahCategoryType
import com.islami.Aha.ui.components.AhaLoadingOverlay
import com.islami.Aha.ui.components.AhaToastTone
import com.islami.Aha.ui.components.AhaToastHost
import com.islami.Aha.ui.theme.*
import com.islami.Aha.util.LocationHelper
import com.islami.Aha.util.rememberLocationPermissionState
import com.islami.Aha.util.rememberNotificationPermissionState
import com.islami.Aha.ui.home.components.*

internal const val CATEGORY_SHOLAT = "Sholat"
internal const val CATEGORY_PUASA = "Puasa"
internal const val CATEGORY_DZIKIR = "Dzikir"
internal const val CATEGORY_TILAWAH = "Tilawah"
internal const val CATEGORY_SHOLAT_TARAWIH = "Sholat Tarawih"
internal const val CATEGORY_PUASA_WAJIB = "Puasa Wajib"
internal const val CATEGORY_PREFIX_PUASA = "Puasa"


@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    transientSnackbarMessage: String? = null,
    onTransientSnackbarShown: () -> Unit = {},
    onNavigateToAddHabit: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var showLocationPermissionBanner by rememberSaveable { mutableStateOf(false) }
    var showLocationPermissionDialog by rememberSaveable { mutableStateOf(false) }
    var showLocationServiceDialog by rememberSaveable { mutableStateOf(false) }
    var hasPromptedLocationPermission by rememberSaveable { mutableStateOf(false) }
    var hasPromptedLocationService by rememberSaveable { mutableStateOf(false) }
    var hasAutoRequestedLocationPermission by rememberSaveable { mutableStateOf(false) }

    var backPressedTime by remember { mutableLongStateOf(0L) }
    androidx.activity.compose.BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - backPressedTime < 2000) {
            (context as? android.app.Activity)?.finish()
        } else {
            backPressedTime = currentTime
            android.widget.Toast.makeText(context, context.getString(R.string.double_tap_exit), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val locationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) {
        if (LocationHelper.isLocationEnabled(context)) {
            showLocationServiceDialog = false
            viewModel.refreshLocation(force = true)
        } else {
            // Keep app usable with last known location fallback.
            viewModel.refreshLocation(force = true)
        }
    }

    val locationPermission = rememberLocationPermissionState(
        onPermissionResult = { granted ->
            if (!granted) {
                showLocationPermissionDialog = true
                hasPromptedLocationPermission = true
                return@rememberLocationPermissionState
            }
            if (LocationHelper.isLocationEnabled(context)) {
                showLocationPermissionDialog = false
                showLocationServiceDialog = false
                viewModel.refreshLocation()
            } else {
                showLocationServiceDialog = !hasPromptedLocationService
                viewModel.refreshLocation()
            }
        }
    )

    fun syncLocationRequirementUi() {
        val hasPermission = locationPermission.isGranted
        val locationEnabled = hasPermission && LocationHelper.isLocationEnabled(context)
        showLocationPermissionBanner = !hasPermission
        
        // Auto-show GPS warning if permission is granted but GPS is off
        showLocationServiceDialog = hasPermission && !locationEnabled && !hasPromptedLocationService
        
        if (hasPermission) {
            viewModel.refreshLocation()
        }
    }

    LaunchedEffect(locationPermission.isGranted) {
        syncLocationRequirementUi()
    }

    LaunchedEffect(transientSnackbarMessage) {
        val message = transientSnackbarMessage ?: return@LaunchedEffect
        toastMessage = message
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage ?: return@LaunchedEffect
        toastMessage = message
    }

    val toastTone = if (
        toastMessage == transientSnackbarMessage && !transientSnackbarMessage.isNullOrBlank()
    ) {
        AhaToastTone.SUCCESS
    } else {
        AhaToastTone.AUTO
    }

    DisposableEffect(lifecycleOwner, locationPermission.isGranted) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                syncLocationRequirementUi()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermission = rememberNotificationPermissionState()

    Box(modifier = Modifier.fillMaxSize()) {
        HomeScreenContent(
            uiState = uiState,
            locationPermissionGranted = locationPermission.isGranted,
            showLocationPermissionBanner = showLocationPermissionBanner && !locationPermission.isGranted,
            notificationPermissionGranted = notificationPermission.isGranted,
            onRequestLocationPermission = {
                showLocationPermissionDialog = true
                hasPromptedLocationPermission = true
            },
            onRequestNotificationPermission = notificationPermission.requestPermission,
            onRefresh = { viewModel.refreshData() },
            onRefreshLocation = {
                if (!locationPermission.isGranted) {
                    showLocationPermissionBanner = true
                    showLocationPermissionDialog = true
                    hasPromptedLocationPermission = true
                } else if (!LocationHelper.isLocationEnabled(context)) {
                    showLocationServiceDialog = true
                    hasPromptedLocationService = true
                    viewModel.refreshLocation(force = true)
                } else {
                    viewModel.refreshLocation(force = true)
                }
            },
            onToggleHabitCompletion = viewModel::toggleHabitCompletion,
            onToggleHabitReminder = viewModel::toggleReminderEnabled,
            onNavigateToAddHabit = onNavigateToAddHabit,
            onNavigateToSettings = onNavigateToSettings,
            onToggleHaidhMode = viewModel::toggleHaidhMode,
            onSelectMainCategory = viewModel::selectMainCategory,
            onSelectSubTab = viewModel::selectSubTab,
            onToggleSunnahCompletion = viewModel::toggleSunnahHabitCompletion,
            onToggleSunnahReminder = viewModel::toggleSunnahReminder,
            onDeleteSunnah = viewModel::removeSunnahHabit
        )

        AhaToastHost(
            message = toastMessage,
            tone = toastTone,
            onDismissed = {
                if (toastMessage == transientSnackbarMessage) {
                    onTransientSnackbarShown()
                }
                if (toastMessage == uiState.snackbarMessage) {
                    viewModel.clearSnackbar()
                }
                toastMessage = null
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 20.dp)
        )

        AhaLoadingOverlay(
            visible = uiState.isLoading,
            message = stringResource(R.string.home_loading_message)
        )
    }

    // Gender prompt popup ("Atur Layar Ibadahmu Yuk!") has been permanently removed

    if (showLocationPermissionDialog && !locationPermission.isGranted) {
        AlertDialog(
            onDismissRequest = {
                showLocationPermissionDialog = false
                hasPromptedLocationPermission = true
            },
            title = {
                Text(
                    text = stringResource(R.string.home_location_permission_button),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.home_location_permission_banner_text),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    hasPromptedLocationPermission = true
                    locationPermission.requestPermission()
                }) {
                    Text(stringResource(R.string.home_location_permission_button))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    hasPromptedLocationPermission = true
                    openAppPermissionSettings(context)
                }) {
                    Text(stringResource(R.string.home_location_service_button))
                }
            }
        )
    }

    if (
        showLocationServiceDialog &&
        locationPermission.isGranted &&
        !LocationHelper.isLocationEnabled(context)
    ) {
        AlertDialog(
            onDismissRequest = {
                showLocationServiceDialog = false
                hasPromptedLocationService = true
                viewModel.refreshLocation()
            },
            title = {
                Text(
                    text = stringResource(R.string.home_location_service_dialog_title),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.home_location_service_dialog_text),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        LocationHelper.requestEnableLocationFromApp(
                            context = context,
                            onResolvable = { intentSender ->
                                locationSettingsLauncher.launch(
                                    IntentSenderRequest.Builder(intentSender).build()
                                )
                            },
                            onAlreadyEnabled = {
                                showLocationServiceDialog = false
                                viewModel.refreshLocation()
                            },
                            onFailure = {
                                openLocationSettings(context)
                            }
                        )
                    }
                ) {
                    Text(stringResource(R.string.home_location_service_enable_now))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        hasPromptedLocationService = true
                        openLocationSettings(context)
                    }
                ) {
                    Text(stringResource(R.string.home_location_service_button))
                }
            }
        )
    }
}

private fun openLocationSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}

private fun openAppPermissionSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    locationPermissionGranted: Boolean = true,
    showLocationPermissionBanner: Boolean = false,
    notificationPermissionGranted: Boolean = true,
    onRequestLocationPermission: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onRefreshLocation: () -> Unit = {},
    onToggleHabitCompletion: (Habit) -> Unit,
    onToggleHabitReminder: (Habit) -> Unit,
    onNavigateToAddHabit: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onToggleHaidhMode: (Boolean) -> Unit,
    onSelectMainCategory: (String) -> Unit,
    onSelectSubTab: (Int) -> Unit,
    onToggleSunnahCompletion: (String) -> Unit = {},
    onToggleSunnahReminder: (String) -> Unit = {},
    onDeleteSunnah: (String) -> Unit = {}
) {
    if (uiState.isLoading) {
        return
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Header with prayer info
            item {
                HomeHeader(
                    isLoggedIn = uiState.isLoggedIn,
                    userName = uiState.userName,
                    currentTime = uiState.currentTime,
                    gregorianDate = uiState.gregorianDate,
                    hijriDate = uiState.hijriDate,
                    location = uiState.location,
                    nextPrayerName = uiState.nextPrayerName,
                    nextPrayerTimeRemaining = uiState.nextPrayerTimeRemaining,
                    nextPrayerProgress = uiState.nextPrayerProgress,
                    prayerTimeStatusText = uiState.prayerTimeStatusText,
                    showRamadanSchedule = uiState.showRamadanScheduleCard,
                    ramadanImsakTime = uiState.ramadanImsakTime,
                    ramadanIftarTime = uiState.ramadanIftarTime,
                    ramadanStatusText = uiState.ramadanStatusText,
                    isLocationLoading = uiState.isLocationLoading,
                    onRefreshLocation = onRefreshLocation,
                    isHaidhMode = uiState.isHaidhMode,
                    genderProfile = uiState.genderProfile,
                    onToggleHaidhMode = onToggleHaidhMode
                )
            }

            // Permission banners
            if (showLocationPermissionBanner && !locationPermissionGranted) {
                item {
                    PermissionBanner(
                        icon = Icons.Filled.LocationOn,
                        text = stringResource(R.string.home_location_permission_banner_text),
                        buttonText = stringResource(R.string.home_location_permission_button),
                        onClick = onRequestLocationPermission
                    )
                }
            }
            if (!notificationPermissionGranted) {
                item {
                    PermissionBanner(
                        icon = Icons.Filled.Notifications,
                        text = stringResource(R.string.notification_permission_banner_text),
                        buttonText = stringResource(R.string.notification_permission_action),
                        onClick = onRequestNotificationPermission
                    )
                }
            }
            if (uiState.showSyncNotice) {
                item {
                    SyncStatusBanner(
                        message = uiState.syncNoticeMessage.ifBlank {
                            stringResource(R.string.offline_sync_notice_default)
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Category Cards
            item {
                CategoryCardsRow(
                    selectedCategory = uiState.selectedMainCategory,
                    comingSoonCategories = uiState.comingSoonCategories,
                    onSelectCategory = onSelectMainCategory,
                    getBadge = { uiState.getCategoryBadge(it) }
                )
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }

            // Habit Section Header
            item {
                HabitSectionHeader(
                    completedCount = uiState.completedHabitsCount,
                    totalCount = uiState.totalHabitsCount
                )
            }

            // Sub-tabs (moved below "Habit Hari Ini")
            if (!uiState.isComingSoon && uiState.subTabDisplayNames.isNotEmpty()) {
                item {
                    SubTabRow(
                        tabs = uiState.subTabDisplayNames,
                        selectedIndex = uiState.selectedSubTabIndex,
                        onSelectTab = onSelectSubTab
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // Content based on category
            val selectedSubCategory = uiState.subTabCategories.getOrNull(uiState.selectedSubTabIndex)
            if (uiState.isComingSoon) {
                item {
                    ComingSoonState(categoryName = uiState.selectedMainCategory)
                }
            } else if (!uiState.isRamadanMonth && uiState.selectedMainCategory == CATEGORY_PUASA && selectedSubCategory == CATEGORY_PUASA_WAJIB) {
                item {
                    ComingSoonState(
                        categoryName = "Ramadan (Puasa Wajib)",
                        customMessage = "Fitur pelacakan puasa ini akan otomatis hadir di bulan Ramadan. Mari maksimalkan ibadah dengan Puasa Sunnah dulu ya!"
                    )
                }
            } else if (uiState.showRamadanUnifiedCard) {
                item {
                    RamadanUnifiedHabitCard(
                        puasaHabit = if (
                            uiState.selectedMainCategory == CATEGORY_PUASA &&
                            selectedSubCategory == CATEGORY_PUASA_WAJIB
                        ) {
                            uiState.ramadanPuasaHabit
                        } else {
                            null
                        },
                        tarawihHabit = if (
                            uiState.selectedMainCategory == CATEGORY_SHOLAT &&
                            selectedSubCategory == CATEGORY_SHOLAT_TARAWIH
                        ) {
                            uiState.ramadanTarawihHabit
                        } else {
                            null
                        },
                        onToggleHabitCompletion = onToggleHabitCompletion,
                        onToggleHabitReminder = onToggleHabitReminder
                    )
                }
            } else if (uiState.filteredHabits.isEmpty() && uiState.filteredSunnahHabits.isEmpty()) {
                item {
                    if (uiState.isHaidhMode && (uiState.selectedMainCategory == "Sholat" || uiState.selectedMainCategory == "Puasa")) {
                        HaidhEmptyState()
                    } else if (!uiState.isHaidhMode) {
                        EmptyHabitState(onAddHabitClick = onNavigateToAddHabit)
                    }
                }
            } else {
                // Seed habits (from Room DB)
                items(uiState.filteredHabits, key = { it.id }) { habit ->
                    HomeHabitItem(
                        habit = habit,
                        onCheckedChange = { onToggleHabitCompletion(habit) },
                        onToggleReminder = { onToggleHabitReminder(habit) },
                        currentTime = uiState.currentTime,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                // User-added sunnah habits (from AddHabitScreen)
                items(uiState.filteredSunnahHabits, key = { it.id }) { sunnahHabit ->
                    SunnahHabitCard(
                        sunnahHabit = sunnahHabit,
                        onToggleComplete = { onToggleSunnahCompletion(sunnahHabit.id) },
                        onToggleReminder = { onToggleSunnahReminder(sunnahHabit.id) },
                        onDelete = { onDeleteSunnah(sunnahHabit.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            // Motivational Quote or Haidh Card
            item {
                    IslamicMotivationCard(
                        quote = uiState.motivationalQuote,
                        source = uiState.quoteSource
                    )
            }
        }
    }
}





@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    HabitIslamiTheme {
        HomeScreen(
            transientSnackbarMessage = null,
            onTransientSnackbarShown = {},
            onNavigateToAddHabit = {},
            onNavigateToSettings = {}
        )
    }
}
