package com.islami.Aha.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.islami.Aha.R
import com.islami.Aha.ui.theme.BackgroundDark
import com.islami.Aha.ui.theme.Emerald
import kotlinx.coroutines.launch

enum class IconTint { EMERALD, WHITE, NONE }

data class OnboardingPage(
    val title: String,
    val description: String,
    val iconRes: Int,
    val iconTint: IconTint = IconTint.EMERALD
)

val onboardingPages = listOf(
    OnboardingPage(
        title = "Selamat Datang di Aha",
        description = "Aplikasi pelacak kebiasaan Islami yang elegan, minimalis, dan dirancang khusus untuk kedamaian hati.",
        iconRes = R.drawable.logo,
        iconTint = IconTint.EMERALD
    ),
    OnboardingPage(
        title = "Pantau Ibadah Harian",
        description = "Catat Sholat, Puasa, dan Dzikir dengan statistik mendalam untuk memotivasi dirimu setiap hari.",
        iconRes = R.drawable.card_icon_sholat,
        iconTint = IconTint.EMERALD
    ),
    OnboardingPage(
        title = "Pengalaman Khusus Untukmu",
        description = "Bantu kami menyempurnakan jadwal ibadahmu. Atur profil sekarang untuk membuka fitur khusus: Mode Cuti (wanita) atau pengingat Salat Jumat (pria).",
        iconRes = R.drawable.ic_flower,
        iconTint = IconTint.EMERALD
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onNavigateToSettings: (() -> Unit)? = null
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == onboardingPages.size - 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Tombol "Atur Nanti" di kanan atas
        if (isLastPage) {
            TextButton(
                onClick = { onFinish() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .statusBarsPadding()
            ) {
                Text("Atur Nanti", color = Color.Gray, fontSize = 14.sp)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(3f)
            ) { position ->
                OnboardingPageContent(page = onboardingPages[position])
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Page Indicator
            Row(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pagerState.pageCount) { iteration ->
                    val color = if (pagerState.currentPage == iteration) Emerald else Color.Gray.copy(alpha = 0.5f)
                    val width = if (pagerState.currentPage == iteration) 24.dp else 12.dp
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(color)
                            .height(8.dp)
                            .width(width)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Tombol navigasi bawah — sama untuk semua slide
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Kiri: Kembali (atau spacer di slide 1)
                if (pagerState.currentPage > 0) {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }) {
                        Text("Kembali", color = Color.Gray)
                    }
                } else {
                    Spacer(modifier = Modifier.width(64.dp))
                }

                // Kanan: Lanjut atau Atur Sekarang di slide terakhir
                Button(
                    onClick = {
                        if (isLastPage) {
                            onFinish()
                            onNavigateToSettings?.invoke()
                        } else {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = if (isLastPage) "Atur Sekarang" else "Lanjut",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    val colorFilter = when (page.iconTint) {
        IconTint.EMERALD -> ColorFilter.tint(Emerald)
        IconTint.WHITE -> ColorFilter.tint(Color.White)
        IconTint.NONE -> null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = page.iconRes),
                contentDescription = page.title,
                modifier = Modifier.size(110.dp),
                contentScale = ContentScale.Fit,
                colorFilter = colorFilter
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 26.sp
        )
    }
}
