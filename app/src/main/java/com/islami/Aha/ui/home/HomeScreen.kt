package com.islami.Aha.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.islami.Aha.data.model.Habit
import com.islami.Aha.R
import com.islami.Aha.domain.model.SunnahHabit
import com.islami.Aha.ui.addhabit.SunnahCategoryType
import com.islami.Aha.ui.theme.*
import com.islami.Aha.util.rememberLocationPermissionState
import com.islami.Aha.util.rememberNotificationPermissionState

// Category card colors
private val TealStart = Color(0xFF0D9488)
private val TealEnd = Color(0xFF14B8A6)
private val PurpleStart = Color(0xFF7C3AED)
private val PurpleEnd = Color(0xFF8B5CF6)
private val AmberStart = Color(0xFFD97706)
private val AmberEnd = Color(0xFFFBBF24)

data class CategoryCardData(
    val name: String,
    val icon: String? = null,
    val iconRes: Int? = null,
    val gradientStart: Color,
    val gradientEnd: Color
)

private val categoryCards = listOf(
    CategoryCardData("Sholat", iconRes = R.drawable.card_icon_sholat, gradientStart = EmeraldDark, gradientEnd = Emerald),
    CategoryCardData("Puasa", iconRes = R.drawable.card_icon_puasa, gradientStart = PurpleStart, gradientEnd = PurpleEnd),
    CategoryCardData("Dzikir", iconRes = R.drawable.card__icon_dzikir, gradientStart = TealStart, gradientEnd = TealEnd),
    CategoryCardData("Tilawah", iconRes = R.drawable.card_icon_tilawah, gradientStart = AmberStart, gradientEnd = AmberEnd)
)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToAddHabit: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLocationPermissionBanner by rememberSaveable { mutableStateOf(false) }

    val locationPermission = rememberLocationPermissionState(
        onPermissionResult = { granted ->
            if (granted) viewModel.refreshLocation()
        }
    )

    val notificationPermission = rememberNotificationPermissionState()

    HomeScreenContent(
        uiState = uiState,
        locationPermissionGranted = locationPermission.isGranted,
        showLocationPermissionBanner = showLocationPermissionBanner && !locationPermission.isGranted,
        notificationPermissionGranted = notificationPermission.isGranted,
        onRequestLocationPermission = locationPermission.requestPermission,
        onRequestNotificationPermission = notificationPermission.requestPermission,
        onRefresh = { viewModel.refreshData() },
        onRefreshLocation = {
            if (locationPermission.isGranted) {
                viewModel.refreshLocation()
            } else {
                showLocationPermissionBanner = true
                locationPermission.requestPermission()
            }
        },
        onToggleHabitCompletion = viewModel::toggleHabitCompletion,
        onToggleHabitReminder = viewModel::toggleReminderEnabled,
        onNavigateToAddHabit = onNavigateToAddHabit,
        onSelectMainCategory = viewModel::selectMainCategory,
        onSelectSubTab = viewModel::selectSubTab,
        onToggleSunnahCompletion = viewModel::toggleSunnahHabitCompletion,
        onToggleSunnahReminder = viewModel::toggleSunnahReminder,
        onDeleteSunnah = viewModel::removeSunnahHabit
    )
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
    onSelectMainCategory: (String) -> Unit,
    onSelectSubTab: (Int) -> Unit,
    onToggleSunnahCompletion: (String) -> Unit = {},
    onToggleSunnahReminder: (String) -> Unit = {},
    onDeleteSunnah: (String) -> Unit = {}
) {
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Emerald)
        }
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
                    isLocationLoading = uiState.isLocationLoading,
                    onRefreshLocation = onRefreshLocation
                )
            }

            // Permission banners
            if (showLocationPermissionBanner && !locationPermissionGranted) {
                item {
                    PermissionBanner(
                        icon = Icons.Filled.LocationOn,
                        text = "Aktifkan lokasi untuk info waktu sholat",
                        buttonText = "Aktifkan Lokasi",
                        onClick = onRequestLocationPermission
                    )
                }
            }
            if (!notificationPermissionGranted) {
                item {
                    PermissionBanner(
                        icon = Icons.Filled.Notifications,
                        text = "Aktifkan notifikasi untuk pengingat ibadah",
                        buttonText = "Aktifkan Notifikasi",
                        onClick = onRequestNotificationPermission
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

            // Sub-tabs
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

            // Habit Section Header
            item {
                HabitSectionHeader(
                    completedCount = uiState.completedHabitsCount,
                    totalCount = uiState.totalHabitsCount
                )
            }

            // Content based on category
            if (uiState.isComingSoon) {
                item {
                    ComingSoonState(categoryName = uiState.selectedMainCategory)
                }
            } else if (uiState.filteredHabits.isEmpty() && uiState.filteredSunnahHabits.isEmpty()) {
                item {
                    EmptyHabitState(onAddHabitClick = onNavigateToAddHabit)
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

            // Motivational Quote
            item {
                IslamicMotivationCard(
                    quote = uiState.motivationalQuote,
                    source = uiState.quoteSource
                )
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
    val backgroundColor by animateColorAsState(
        targetValue = if (sunnahHabit.isCompletedToday) MaterialTheme.colorScheme.primaryContainer
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
                color = if (sunnahHabit.isCompletedToday) Emerald.copy(alpha = 0.15f) else EmeraldLight,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = sunnahHabit.name,
                        modifier = Modifier.size(24.dp)
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
                    color = Gray500
                )
                sunnahHabit.rakaat?.let { rakaat ->
                    Text(
                        text = "$rakaat rakaat",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (sunnahHabit.reminderEnabled && sunnahHabit.reminderTime != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = "Reminder Time",
                            tint = Gray500,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = sunnahHabit.reminderTime,
                            fontSize = 12.sp,
                            color = Gray500
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
                            Modifier.background(Emerald, CircleShape)
                        } else {
                            Modifier.border(2.dp, Emerald, CircleShape)
                        }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onToggleComplete() },
                contentAlignment = Alignment.Center
            ) {
                if (sunnahHabit.isCompletedToday) {
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
        colors = CardDefaults.cardColors(containerColor = GoldLight)
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
    isLocationLoading: Boolean = false,
    onRefreshLocation: () -> Unit = {}
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
            Text(
                text = greetingText,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
            )
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
                Text(text = gregorianDate, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                Text(text = "\u2022", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f), fontSize = 12.sp)
                Text(text = hijriDate, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
            }

            // Location with refresh button
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = "Lokasi",
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = location,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
                if (isLocationLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                } else {
                    IconButton(
                        onClick = onRefreshLocation,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh lokasi",
                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
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
                                contentDescription = "Masjid",
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "$nextPrayerName dalam $nextPrayerTimeRemaining",
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
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.05f else 1f,
                label = "categoryScale"
            )

            val baseModifier = Modifier
                .width(100.dp)
                .scale(scale)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = index == selectedIndex
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onSelectTab(index) },
                shape = RoundedCornerShape(50),
                color = if (isSelected) Emerald else Color.Transparent,
                border = if (!isSelected) {
                    androidx.compose.foundation.BorderStroke(1.dp, Emerald.copy(alpha = 0.5f))
                } else null
            ) {
                Text(
                    text = tab,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Emerald,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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
            text = "Habit Hari Ini",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Surface(
            shape = RoundedCornerShape(50),
            color = GoldLight
        ) {
            Text(
                text = "$completedCount/$totalCount selesai",
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
    // Determine if this is the current prayer time
    val isCurrentPrayer = isCurrentPrayerTime(habit.time, currentTime)

    val backgroundColor by animateColorAsState(
        targetValue = when {
            habit.isCompleted -> MaterialTheme.colorScheme.primaryContainer
            isCurrentPrayer -> EmeraldLight
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
                        .background(Emerald)
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
                    color = if (habit.isCompleted) Emerald.copy(alpha = 0.15f) else EmeraldLight,
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
                    onClick = onToggleReminder,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (habit.isReminderEnabled) {
                            Icons.Outlined.Notifications
                        } else {
                            Icons.Outlined.NotificationsOff
                        },
                        contentDescription = "Pengingat",
                        tint = if (habit.isReminderEnabled) Emerald else Gray400,
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
        habit.category.startsWith("Puasa", ignoreCase = true) -> Icons.Filled.Restaurant
        else -> Icons.Outlined.AccessTime
    }
}

private fun isMasjidIcon(icon: String): Boolean {
    return icon == "masjid" || icon == "\uD83D\uDD4C"
}

private fun isCurrentPrayerTime(habitTime: String, currentTime: String): Boolean {
    if (habitTime.isEmpty() || currentTime.isEmpty()) return false
    try {
        val habitParts = habitTime.split(":")
        val currentParts = currentTime.split(":")
        if (habitParts.size < 2 || currentParts.size < 2) return false

        val habitHour = habitParts[0].toInt()
        val habitMinute = habitParts[1].toInt()
        val currentHour = currentParts[0].toInt()
        val currentMinute = currentParts[1].toInt()

        val habitMinutes = habitHour * 60 + habitMinute
        val currentMinutes = currentHour * 60 + currentMinute

        // Consider current if within 30 minutes before or after the prayer time
        return currentMinutes in (habitMinutes - 15)..(habitMinutes + 30)
    } catch (_: Exception) {
        return false
    }
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
                contentDescription = "Segera hadir",
                tint = Emerald,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Segera Hadir!",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Fitur $categoryName sedang dalam pengembangan.",
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
                contentDescription = "Belum ada kebiasaan",
                tint = Gray500,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Belum Ada Kebiasaan",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Mulai lacak ibadah harian Anda dengan menambahkan kebiasaan baru.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onAddHabitClick,
                colors = ButtonDefaults.buttonColors(containerColor = Emerald)
            ) {
                Text("Tambah Kebiasaan Baru", color = MaterialTheme.colorScheme.onPrimary)
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
        colors = CardDefaults.cardColors(containerColor = GoldLight)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "\"$quote\"",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = FontStyle.Italic
            )
            Text(
                text = source,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    HabitIslamiTheme {
        HomeScreenContent(
            uiState = HomeUiState(
                isLoading = false,
                isLoggedIn = true,
                userName = "Ahmad",
                currentTime = "14:30",
                gregorianDate = "23 Mei 2024",
                hijriDate = "15 Dzulqa'dah 1445 H",
                location = "Jakarta",
                nextPrayerName = "Ashar",
                nextPrayerTimeRemaining = "40 menit lagi",
                nextPrayerProgress = 0.6f,
                selectedMainCategory = "Sholat",
                selectedSubTabIndex = 0,
                allHabits = listOf(
                    Habit(1, "Sholat Subuh", "Sholat Fardhu", "sunrise", "", true, time = "04:30"),
                    Habit(2, "Sholat Dzuhur", "Sholat Fardhu", "sun", "", true, time = "11:55"),
                    Habit(3, "Sholat Ashar", "Sholat Fardhu", "cloud", "", false, time = "15:10"),
                    Habit(4, "Sholat Maghrib", "Sholat Fardhu", "moon", "", false, time = "18:00"),
                    Habit(5, "Sholat Isya", "Sholat Fardhu", "moon", "", false, time = "19:15")
                ),
                sunnahHabits = listOf( // Added for preview
                    SunnahHabit(
                        name = "Dhuha",
                        category = SunnahCategoryType.SHOLAT,
                        frequencyLabel = "Setiap hari",
                        reminderEnabled = true,
                        reminderTime = "06:00"
                    ),
                    SunnahHabit(
                        name = "Puasa Senin Kamis",
                        category = SunnahCategoryType.PUASA,
                        frequencyLabel = "Hari: Sen • Kam",
                        reminderEnabled = false,
                        reminderTime = null
                    )
                ),
                motivationalQuote = "Amalan yang paling dicintai oleh Allah adalah yang paling konsisten.",
                quoteSource = "- HR. Bukhari"
            ),
            onToggleHabitCompletion = {},
            onToggleHabitReminder = {},
            onNavigateToAddHabit = {},
            onSelectMainCategory = {},
            onSelectSubTab = {},
            onToggleSunnahCompletion = {}
        )
    }
}
