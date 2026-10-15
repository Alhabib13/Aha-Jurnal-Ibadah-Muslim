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
    val gradientEnd: Color,
    val contentColor: Color = Color.White
)

val categoryCards = listOf(
    CategoryCardData(CATEGORY_SHOLAT, iconVector = Icons.Rounded.Mosque, gradientStart = Color(0xFF0D7C5F), gradientEnd = Color(0xFF14A87D)),
    CategoryCardData(CATEGORY_PUASA, iconVector = Icons.Rounded.NightlightRound, gradientStart = Color(0xFF5B2FC4), gradientEnd = Color(0xFF7C4DE0)),
    CategoryCardData(CATEGORY_DZIKIR, iconVector = Icons.Rounded.SelfImprovement, gradientStart = Color(0xFF0B7A72), gradientEnd = Color(0xFF149C8F)),
    CategoryCardData(CATEGORY_TILAWAH, iconVector = Icons.Rounded.MenuBook, gradientStart = Color(0xFFD28A2A), gradientEnd = Color(0xFFEDB84E), contentColor = Color(0xFF3A2400))
)

@Composable
fun CategoryCardsRow(
    selectedCategory: String,
    comingSoonCategories: List<String>,
    onSelectCategory: (String) -> Unit,
    getBadge: (String) -> String
) {
    // Empat kartu sama lebar dalam satu baris (tanpa geser), sesuai desain baru.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        categoryCards.forEach { card ->
            val isSelected = card.name == selectedCategory
            val isComingSoon = card.name in comingSoonCategories
            val badge = getBadge(card.name)
            val redup = badge == "Cuti"
            val categoryStateDescription = when {
                isComingSoon -> stringResource(R.string.home_state_unavailable)
                isSelected -> stringResource(R.string.home_state_selected)
                else -> stringResource(R.string.home_state_not_selected)
            }
            val shape = RoundedCornerShape(18.dp)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 96.dp)
                    .alpha(if (redup || isComingSoon) 0.6f else 1f)
                    .clip(shape)
                    .background(Brush.linearGradient(listOf(card.gradientStart, card.gradientEnd)))
                    .then(if (isSelected) Modifier.border(2.dp, Color(0xFFF2C66D), shape) else Modifier)
                    .clickable(enabled = !isComingSoon) { onSelectCategory(card.name) }
                    .semantics(mergeDescendants = true) {
                        if (!isComingSoon) role = Role.Button
                        selected = isSelected
                        stateDescription = categoryStateDescription
                    }
                    .padding(horizontal = 4.dp, vertical = 12.dp)
            ) {
                if (card.iconVector != null) {
                    Icon(card.iconVector, contentDescription = null, tint = card.contentColor, modifier = Modifier.size(26.dp))
                } else {
                    Text(text = card.icon.orEmpty(), fontSize = 24.sp)
                }
                Text(card.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = card.contentColor, maxLines = 1)
                Text(
                    badge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = card.contentColor,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = if (card.contentColor == Color.White) 0.18f else 0.32f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Kontrol segmen (Fardhu/Sunnah, Al-Qur'an/Iqro, Sunnah/Wajib): satu wadah bulat, pilihan aktif
 * diisi warna kategori.
 */
@Composable
fun SubTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onSelectTab: (Int) -> Unit,
    accent: Color = Color(0xFF2ECC9B),
    onAccent: Color = Color(0xFF062B1F)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = index == selectedIndex
            val tabStateDescription = if (isSelected) {
                stringResource(R.string.home_state_selected)
            } else {
                stringResource(R.string.home_state_not_selected)
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) accent else Color.Transparent)
                    .semantics {
                        role = Role.Tab
                        selected = isSelected
                        stateDescription = tabStateDescription
                    }
                    .clickable { onSelectTab(index) }
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = tab,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = if (isSelected) onAccent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
