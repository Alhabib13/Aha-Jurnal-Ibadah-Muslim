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

// Category card colors
private val TealStart = Color(0xFF0D9488)
private val TealEnd = Color(0xFF14B8A6)
private val PurpleStart = Color(0xFF7C3AED)
private val PurpleEnd = Color(0xFF8B5CF6)
private val AmberStart = Color(0xFFD97706)
private val AmberEnd = Color(0xFFFBBF24)

private const val CATEGORY_SHOLAT = "Sholat"
private const val CATEGORY_PUASA = "Puasa"
private const val CATEGORY_DZIKIR = "Dzikir"
private const val CATEGORY_TILAWAH = "Tilawah"
private const val CATEGORY_SHOLAT_TARAWIH = "Sholat Tarawih"
private const val CATEGORY_PUASA_WAJIB = "Puasa Wajib"
private const val CATEGORY_PREFIX_PUASA = "Puasa"

data class CategoryCardData(
    val name: String,
    val icon: String? = null,
    val iconRes: Int? = null,
    val gradientStart: Color,
    val gradientEnd: Color
)

private val categoryCards = listOf(
    CategoryCardData(CATEGORY_SHOLAT, iconRes = R.drawable.card_icon_sholat, gradientStart = EmeraldDark, gradientEnd = Emerald),
    CategoryCardData(CATEGORY_PUASA, iconRes = R.drawable.card_icon_puasa, gradientStart = PurpleStart, gradientEnd = PurpleEnd),
    CategoryCardData(CATEGORY_DZIKIR, iconRes = R.drawable.card__icon_dzikir, gradientStart = TealStart, gradientEnd = TealEnd),
    CategoryCardData(CATEGORY_TILAWAH, iconRes = R.drawable.card_icon_tilawah, gradientStart = AmberStart, gradientEnd = AmberEnd)
)

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
        showLocationPermissionDialog = !hasPermission && !hasPromptedLocationPermission
        showLocationServiceDialog = hasPermission && !locationEnabled && !hasPromptedLocationService
        if (hasPermission) {
            viewModel.refreshLocation()
        }
    }

    LaunchedEffect(locationPermission.isGranted) {
        syncLocationRequirementUi()
        if (!locationPermission.isGranted && !hasAutoRequestedLocationPermission) {
            hasAutoRequestedLocationPermission = true
            locationPermission.requestPermission()
        }
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
                locationPermission.requestPermission()
            },
            onRequestNotificationPermission = notificationPermission.requestPermission,
            onRefresh = { viewModel.refreshData() },
            onRefreshLocation = {
                if (!locationPermission.isGranted) {
                    showLocationPermissionBanner = true
                    showLocationPermissionDialog = true
                    hasPromptedLocationPermission = true
                    locationPermission.requestPermission()
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

    if (uiState.showGenderPrompt) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { viewModel.dismissGenderPrompt() }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Atur Layar Ibadahmu Yuk!",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Bantu kami menyesuaikan aplikasi ini khusus untukmu. Pilih Profil Ibadahmu sekarang untuk mengaktifkan jadwal Salat Jumat atau Mode Cuti (bagi perempuan).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { 
                            viewModel.dismissGenderPrompt()
                            onNavigateToSettings() 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald)
                    ) {
                        Text("Atur Sekarang", color = Color.White)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.TextButton(
                        onClick = { viewModel.dismissGenderPrompt() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Nanti Saja", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

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
            if (uiState.isComingSoon) {
                item {
                    ComingSoonState(categoryName = uiState.selectedMainCategory)
                }
            } else if (uiState.showRamadanUnifiedCard) {
                item {
                    val selectedSubCategory =
                        uiState.subTabCategories.getOrNull(uiState.selectedSubTabIndex)
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
                    if (!uiState.isHaidhMode) {
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
                if (uiState.isHaidhMode) {
                    HaidhMotivationCard()
                } else {
                    IslamicMotivationCard(
                        quote = uiState.motivationalQuote,
                        source = uiState.quoteSource
                    )
                }
            }
        }
    }
}

@Composable
fun SunnahHabitCard(
    sunnahHabit: SunnahHabit,
    onToggleComplete: () -> Unit = {},
    onToggleReminder: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val completionStateDescription = if (sunnahHabit.isCompletedToday) {
        stringResource(R.string.home_completed_cd)
    } else {
        stringResource(R.string.home_not_completed_cd)
    }
    val backgroundColor by animateColorAsState(
        targetValue = if (sunnahHabit.isCompletedToday) MaterialTheme.colorScheme.surfaceVariant
        else MaterialTheme.colorScheme.surface,
        label = "sunnahBgColor"
    )
    val checkScale by animateFloatAsState(
        targetValue = if (sunnahHabit.isCompletedToday) 1f else 0.8f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "sunnahCheckScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (sunnahHabit.isCompletedToday) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon based on category - using drawable icons
            val iconRes = when (sunnahHabit.category) {
                SunnahCategoryType.SHOLAT -> R.drawable.card_icon_sholat
                SunnahCategoryType.PUASA -> R.drawable.card_icon_puasa
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when (sunnahHabit.category) {
                    SunnahCategoryType.PUASA -> WarningAmber.copy(alpha = 0.12f)
                    SunnahCategoryType.SHOLAT -> InfoBlue.copy(alpha = 0.12f)
                },
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = sunnahHabit.name,
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(
                            when (sunnahHabit.category) {
                                SunnahCategoryType.PUASA -> WarningAmber
                                SunnahCategoryType.SHOLAT -> Emerald
                            }
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sunnahHabit.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (sunnahHabit.isCompletedToday) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    text = sunnahHabit.frequencyLabel,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                sunnahHabit.rakaat?.let { rakaat ->
                    Text(
                        text = pluralStringResource(R.plurals.home_rakaat_format, rakaat, rakaat),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (sunnahHabit.reminderEnabled && sunnahHabit.reminderTime != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = stringResource(R.string.home_reminder_time_cd),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = sunnahHabit.reminderTime,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Circular checkbox
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .scale(checkScale)
                    .clip(CircleShape)
                    .then(
                        if (sunnahHabit.isCompletedToday) {
                            Modifier.background(InfoBlue, CircleShape)
                        } else {
                            Modifier.border(2.dp, InfoBlue, CircleShape)
                        }
                    )
                    .semantics {
                        role = Role.Checkbox
                        stateDescription = completionStateDescription
                    }
                    .toggleable(
                        value = sunnahHabit.isCompletedToday,
                        role = Role.Checkbox,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onToggleComplete() },
                contentAlignment = Alignment.Center
            ) {
                if (sunnahHabit.isCompletedToday) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.home_completed_cd),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun PermissionBanner(
    icon: ImageVector,
    text: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick = onClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = buttonText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Emerald
                )
            }
        }
    }
}

@Composable
fun SyncStatusBanner(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Cloud,
                contentDescription = null,
                tint = Emerald,
                modifier = Modifier.size(18.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.sync_status_label),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Emerald
                )
                Text(
                    text = message,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

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
    genderProfile: com.islami.Aha.util.GenderProfile = com.islami.Aha.util.GenderProfile.UNSPECIFIED,
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
                
                if (genderProfile == com.islami.Aha.util.GenderProfile.FEMALE) {
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
                            text = "Mode Cuti",
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
                            Image(
                                painter = painterResource(id = R.drawable.masjid),
                                contentDescription = stringResource(R.string.home_mosque_cd),
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

@Composable
fun CategoryCardsRow(
    selectedCategory: String,
    comingSoonCategories: List<String>,
    onSelectCategory: (String) -> Unit,
    getBadge: (String) -> String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        categoryCards.forEach { card ->
            val isSelected = card.name == selectedCategory
            val isComingSoon = card.name in comingSoonCategories
            val categoryStateDescription = when {
                isComingSoon -> stringResource(R.string.home_state_unavailable)
                isSelected -> stringResource(R.string.home_state_selected)
                else -> stringResource(R.string.home_state_not_selected)
            }
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.05f else 1f,
                label = "categoryScale"
            )

            val baseModifier = Modifier
                .width(100.dp)
                .scale(scale)
                .semantics(mergeDescendants = true) {
                    if (!isComingSoon) role = Role.Button
                    selected = isSelected
                    stateDescription = categoryStateDescription
                }
                .then(
                    if (isSelected) {
                        Modifier.border(2.dp, Gold, RoundedCornerShape(16.dp))
                    } else {
                        Modifier
                    }
                )

            val cardModifier = if (!isComingSoon) {
                baseModifier.clickable { onSelectCategory(card.name) }
            } else {
                baseModifier
            }

            Card(
                modifier = cardModifier,
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(
                            when {
                                isSelected -> 1f
                                isComingSoon -> 0.65f
                                else -> 0.82f
                            }
                        )
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(card.gradientStart, card.gradientEnd)
                            )
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (card.iconRes != null) {
                            Image(
                                painter = painterResource(id = card.iconRes),
                                contentDescription = card.name,
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Text(text = card.icon.orEmpty(), fontSize = 28.sp)
                        }
                        Text(
                            text = card.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = getBadge(card.name),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onSelectTab: (Int) -> Unit
) {
    val useEqualWidthTabs = tabs.size <= 3

    if (useEqualWidthTabs) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = index == selectedIndex
                val tabStateDescription = if (isSelected) {
                    stringResource(R.string.home_state_selected)
                } else {
                    stringResource(R.string.home_state_not_selected)
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .semantics {
                            role = Role.Tab
                            selected = isSelected
                            stateDescription = tabStateDescription
                        }
                        .clickable { onSelectTab(index) },
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) Emerald else Color.Transparent,
                    border = if (!isSelected) {
                        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    } else null
                ) {
                    Text(
                        text = tab,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 9.dp)
                    )
                }
            }
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = index == selectedIndex
            val tabStateDescription = if (isSelected) {
                stringResource(R.string.home_state_selected)
            } else {
                stringResource(R.string.home_state_not_selected)
            }
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .semantics {
                        role = Role.Tab
                        selected = isSelected
                        stateDescription = tabStateDescription
                    }
                    .clickable { onSelectTab(index) },
                shape = RoundedCornerShape(50),
                color = if (isSelected) Emerald else Color.Transparent,
                border = if (!isSelected) {
                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                } else null
            ) {
                Text(
                    text = tab,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun RamadanUnifiedHabitCard(
    puasaHabit: Habit?,
    tarawihHabit: Habit?,
    onToggleHabitCompletion: (Habit) -> Unit,
    onToggleHabitReminder: (Habit) -> Unit
) {
    val habits = buildList {
        puasaHabit?.let { add(it) }
        tarawihHabit?.let { add(it) }
    }
    if (habits.isEmpty()) return
    val hasPuasa = habits.any { it.category == CATEGORY_PUASA_WAJIB }
    val hasTarawih = habits.any { it.category == CATEGORY_SHOLAT_TARAWIH }
    val titleText = when {
        hasPuasa && !hasTarawih -> stringResource(R.string.home_ramadan_unified_title_puasa)
        hasTarawih && !hasPuasa -> stringResource(R.string.home_ramadan_unified_title_tarawih)
        else -> stringResource(R.string.home_ramadan_unified_title)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = titleText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))

            habits.forEachIndexed { index, habit ->
                RamadanUnifiedHabitRow(
                    habit = habit,
                    displayName = when (habit.category) {
                        CATEGORY_PUASA_WAJIB -> stringResource(R.string.home_ramadan_unified_puasa_label)
                        CATEGORY_SHOLAT_TARAWIH -> stringResource(R.string.home_ramadan_unified_tarawih_label)
                        else -> habit.name
                    },
                    onToggleHabitCompletion = { onToggleHabitCompletion(habit) },
                    onToggleHabitReminder = { onToggleHabitReminder(habit) }
                )
                if (index < habits.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun RamadanUnifiedHabitRow(
    habit: Habit,
    displayName: String,
    onToggleHabitCompletion: () -> Unit,
    onToggleHabitReminder: () -> Unit
) {
    val completionStateDescription = if (habit.isCompleted) {
        stringResource(R.string.home_completed_cd)
    } else {
        stringResource(R.string.home_not_completed_cd)
    }
    val reminderStateDescription = if (habit.isReminderEnabled) {
        stringResource(R.string.notification_switch_habit_on_format, displayName)
    } else {
        stringResource(R.string.notification_switch_habit_off_format, displayName)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = habitIconContainerColor(habit),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = getHabitItemIcon(habit),
                    contentDescription = displayName,
                    tint = Emerald,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            val secondaryText = habit.time.ifBlank { habit.description }
            if (secondaryText.isNotBlank()) {
                Text(
                    text = secondaryText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(
            onClick = onToggleHabitReminder,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = if (habit.isReminderEnabled) {
                    Icons.Outlined.Notifications
                } else {
                    Icons.Outlined.NotificationsOff
                },
                contentDescription = reminderStateDescription,
                tint = if (habit.isReminderEnabled) Emerald else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .then(
                    if (habit.isCompleted) {
                        Modifier.background(InfoBlue, CircleShape)
                    } else {
                        Modifier.border(2.dp, InfoBlue, CircleShape)
                    }
                )
                .semantics {
                    role = Role.Checkbox
                    stateDescription = completionStateDescription
                }
                .toggleable(
                    value = habit.isCompleted,
                    role = Role.Checkbox,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onToggleHabitCompletion() },
            contentAlignment = Alignment.Center
        ) {
            if (habit.isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.home_completed_cd),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun HabitSectionHeader(completedCount: Int, totalCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.home_habit_today_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = stringResource(R.string.home_habit_progress_format, completedCount, totalCount),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Gold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
fun HomeHabitItem(
    habit: Habit,
    onCheckedChange: (Boolean) -> Unit,
    onToggleReminder: () -> Unit,
    currentTime: String,
    modifier: Modifier = Modifier
) {
    val completionStateDescription = if (habit.isCompleted) {
        stringResource(R.string.home_completed_cd)
    } else {
        stringResource(R.string.home_not_completed_cd)
    }
    val reminderStateDescription = if (habit.isReminderEnabled) {
        stringResource(R.string.notification_switch_habit_on_format, habit.name)
    } else {
        stringResource(R.string.notification_switch_habit_off_format, habit.name)
    }

    // Determine if this is the current prayer time
    val isCurrentPrayer = isCurrentPrayerTime(habit.time, currentTime)

    val backgroundColor by animateColorAsState(
        targetValue = when {
            habit.isCompleted -> MaterialTheme.colorScheme.surfaceVariant
            isCurrentPrayer -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surface
        },
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
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left accent bar for current prayer
            if (isCurrentPrayer && !habit.isCompleted) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(InfoBlue)
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = habitIconContainerColor(habit),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isMasjidIcon(habit.icon)) {
                            Image(
                                painter = painterResource(id = R.drawable.masjid),
                                contentDescription = habit.name,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(
                                imageVector = getHabitItemIcon(habit),
                                contentDescription = habit.name,
                                tint = Emerald,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name + Time
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (habit.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    if (habit.time.isNotEmpty()) {
                        Text(
                            text = habit.time,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (habit.description.isNotEmpty()) {
                        Text(
                            text = habit.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Bell icon
                IconButton(
                    onClick = onToggleReminder
                ) {
                    Icon(
                        imageVector = if (habit.isReminderEnabled) {
                            Icons.Outlined.Notifications
                        } else {
                            Icons.Outlined.NotificationsOff
                        },
                        contentDescription = reminderStateDescription,
                        tint = if (habit.isReminderEnabled) Emerald else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Circular checkbox
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .scale(checkScale)
                        .clip(CircleShape)
                    .then(
                        if (habit.isCompleted) {
                            Modifier.background(InfoBlue, CircleShape)
                        } else {
                            Modifier.border(2.dp, InfoBlue, CircleShape)
                        }
                    )
                    .semantics {
                        role = Role.Checkbox
                        stateDescription = completionStateDescription
                    }
                    .toggleable(
                        value = habit.isCompleted,
                        role = Role.Checkbox,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onCheckedChange(!habit.isCompleted) },
                contentAlignment = Alignment.Center
            ) {
                if (habit.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.home_completed_cd),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun habitIconContainerColor(habit: Habit): Color {
    return if (habit.category.startsWith(CATEGORY_PREFIX_PUASA, ignoreCase = true)) {
        WarningAmber.copy(alpha = 0.12f)
    } else {
        InfoBlue.copy(alpha = 0.12f)
    }
}

private fun getHabitItemIcon(habit: Habit): ImageVector {
    return when {
        habit.icon == "sunrise" -> Icons.Filled.WbSunny
        habit.icon == "sun" -> Icons.Filled.WbSunny
        habit.icon == "cloud" -> Icons.Filled.Cloud
        habit.icon == "moon" -> Icons.Filled.DarkMode
        habit.icon == "night" -> Icons.Filled.DarkMode
        habit.icon == "plate" -> Icons.Filled.Restaurant
        habit.icon == "\uD83C\uDF05" -> Icons.Filled.WbSunny // sunrise
        habit.icon == "\u2600\uFE0F" -> Icons.Filled.WbSunny // sun
        habit.icon == "\u2601\uFE0F" -> Icons.Filled.Cloud // cloud
        habit.icon == "\uD83C\uDF19" -> Icons.Filled.DarkMode // crescent moon
        habit.icon == "\uD83C\uDF1C" -> Icons.Filled.DarkMode // moon face
        habit.icon == "\uD83C\uDF7D\uFE0F" -> Icons.Filled.Restaurant // plate
        habit.category.startsWith(CATEGORY_PREFIX_PUASA, ignoreCase = true) -> Icons.Filled.Restaurant
        else -> Icons.Outlined.AccessTime
    }
}

private fun isMasjidIcon(icon: String): Boolean {
    return icon == "masjid" || icon == "\uD83D\uDD4C"
}

private fun isCurrentPrayerTime(habitTime: String, currentTime: String): Boolean {
    val prayerTime = parseHourMinute(habitTime) ?: return false
    val nowTime = parseHourMinute(currentTime) ?: return false
    val habitMinutes = prayerTime.first * 60 + prayerTime.second
    val currentMinutes = nowTime.first * 60 + nowTime.second
    return currentMinutes in (habitMinutes - 15)..(habitMinutes + 30)
}

@Composable
fun ComingSoonState(categoryName: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.HourglassTop,
                contentDescription = stringResource(R.string.home_coming_soon_cd),
                tint = Emerald,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = stringResource(R.string.home_coming_soon_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.home_coming_soon_desc_format, categoryName),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EmptyHabitState(onAddHabitClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Inbox,
                contentDescription = stringResource(R.string.home_empty_habit_cd),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = stringResource(R.string.home_empty_habit_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.home_empty_habit_desc),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onAddHabitClick,
                colors = ButtonDefaults.buttonColors(containerColor = Emerald)
            ) {
                Text(
                    text = stringResource(R.string.home_add_new_habit_button),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun IslamicMotivationCard(quote: String, source: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.home_quote_format, quote),
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                lineHeight = 22.sp
            )
            Text(
                text = source,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun HaidhMotivationCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "Info",
                    tint = Emerald,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mode Cuti Ibadah Aktif",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Text(
                text = "Saat ini kamu sedang dalam masa halangan (haidh). Kewajiban sholat dan puasamu untuk sementara digugurkan.\n\nJangan lupa matikan mode ini ketika sudah suci kembali ya, agar bisa mencatat amal ibadahmu lagi!",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f),
                lineHeight = 20.sp
            )
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
