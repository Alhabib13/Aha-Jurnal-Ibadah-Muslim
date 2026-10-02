package com.islami.Aha.ui.home.components

import androidx.compose.material3.MaterialTheme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.islami.Aha.R
import com.islami.Aha.ui.home.CATEGORY_DZIKIR
import com.islami.Aha.ui.home.CATEGORY_PUASA
import com.islami.Aha.ui.home.CATEGORY_SHOLAT
import com.islami.Aha.ui.home.CATEGORY_TILAWAH
import com.islami.Aha.ui.theme.Emerald
import com.islami.Aha.ui.theme.EmeraldDark
import com.islami.Aha.ui.theme.Gold

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.NightlightRound
import androidx.compose.material.icons.rounded.Mosque

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
    val iconVector: ImageVector? = null,
    val gradientStart: Color,
    val gradientEnd: Color
)

val categoryCards = listOf(
    CategoryCardData(CATEGORY_SHOLAT, iconVector = Icons.Rounded.Mosque, gradientStart = EmeraldDark, gradientEnd = Emerald),
    CategoryCardData(CATEGORY_PUASA, iconVector = Icons.Rounded.NightlightRound, gradientStart = PurpleStart, gradientEnd = PurpleEnd),
    CategoryCardData(CATEGORY_DZIKIR, iconVector = Icons.Rounded.SelfImprovement, gradientStart = TealStart, gradientEnd = TealEnd),
    CategoryCardData(CATEGORY_TILAWAH, iconVector = Icons.Rounded.MenuBook, gradientStart = AmberStart, gradientEnd = AmberEnd)
)

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
                        if (card.iconVector != null) {
                            Icon(
                                imageVector = card.iconVector,
                                contentDescription = card.name,
                                tint = MaterialTheme.colorScheme.onPrimary,
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
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
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
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
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
