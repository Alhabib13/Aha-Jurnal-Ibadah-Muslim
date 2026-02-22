package com.islami.Aha.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.islami.Aha.R
import com.islami.Aha.ui.theme.*

data class BottomNavItem(
    val route: String,
    val title: String,
    val iconRes: Int,
    val contentDescription: String = title
)

object BottomNavItems {
    val items = listOf(
        BottomNavItem("home", "Beranda", R.drawable.ic_nav_home),
        BottomNavItem("statistic", "Statistik", R.drawable.ic_nav_statistic),
        BottomNavItem("add_habit", "Tambah", R.drawable.ic_nav_add),
        BottomNavItem("notification", "Notifikasi", R.drawable.ic_nav_notification),
        BottomNavItem("profile", "Profil", R.drawable.ic_nav_profile)
    )
}

@Composable
fun AhaBottomNavBar(
    currentRoute: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Outer Box: wraps nav bar, FAB floats above via negative offset
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // ── Nav bar background with top divider ──
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(
                    WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
                )
        ) {
            // Nav bar surface
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .shadow(
                        elevation = 12.dp,
                        spotColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.1f),
                        ambientColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.05f)
                    )
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Beranda, Statistik
                    BottomNavItems.items.take(2).forEach { item ->
                        NavBarItem(
                            item = item,
                            isSelected = currentRoute == item.route,
                            onClick = { onItemSelected(item.route) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Center: empty space for FAB
                    Spacer(modifier = Modifier.weight(1f))

                    // Right: Notifikasi, Profil
                    BottomNavItems.items.takeLast(2).forEach { item ->
                        NavBarItem(
                            item = item,
                            isSelected = currentRoute == item.route,
                            onClick = { onItemSelected(item.route) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ── Floating Center FAB ──
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-16).dp)
                .size(56.dp)
                .shadow(elevation = 8.dp, shape = CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(EmeraldDark, Emerald)
                    ),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onItemSelected("add_habit") },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.nav_add_cd),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────
// Single nav bar item: icon + label (always visible)
// ────────────────────────────────────────────────────────────────

@Composable
private fun NavBarItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "navIcon"
    )
    val labelColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "navLabel"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .selectable(
                selected = isSelected,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = item.iconRes),
            contentDescription = item.contentDescription,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = labelColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ============================================================================
// PREVIEWS
// ============================================================================

@Preview(showBackground = true)
@Composable
fun AhaBottomNavBarPreview() {
    HabitIslamiTheme {
        AhaBottomNavBar(
            currentRoute = "home",
            onItemSelected = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AhaBottomNavBarStatisticSelectedPreview() {
    HabitIslamiTheme {
        AhaBottomNavBar(
            currentRoute = "statistic",
            onItemSelected = {}
        )
    }
}
