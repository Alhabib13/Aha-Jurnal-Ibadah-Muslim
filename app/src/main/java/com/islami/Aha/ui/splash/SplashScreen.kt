package com.islami.Aha.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.islami.Aha.R
import com.islami.Aha.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(
    viewModel: SplashViewModel = viewModel(),
    onNavigateToHome: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.shouldNavigate) {
        if (uiState.shouldNavigate) {
            onNavigateToHome()
            viewModel.onNavigationComplete()
        }
    }

    SplashScreenContent()
}

@Composable
fun SplashScreenContent() {
    // Fade in + slide up animation
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "alpha"
    )
    val scaleAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.6f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "scale"
    )
    val offsetAnim = animateFloatAsState(
        targetValue = if (startAnimation) 0f else 40f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "offset"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(EmeraldDark, Emerald),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val patternColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.06f)
        // Geometric Islamic pattern overlay
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val spacing = 80f
            val starSize = 20f

            for (x in 0..(size.width / spacing).toInt() + 1) {
                for (y in 0..(size.height / spacing).toInt() + 1) {
                    val cx = x * spacing + if (y % 2 == 0) 0f else spacing / 2
                    val cy = y * spacing

                    // Draw 8-pointed star
                    rotate(degrees = 22.5f, pivot = Offset(cx, cy)) {
                        val path = Path()
                        for (i in 0 until 8) {
                            val angle = Math.toRadians((i * 45.0))
                            val px = cx + starSize * cos(angle).toFloat()
                            val py = cy + starSize * sin(angle).toFloat()
                            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                        }
                        path.close()
                        drawPath(path, patternColor)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .alpha(alphaAnim.value)
                .offset(y = offsetAnim.value.dp)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo with glow effect
            Surface(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scaleAnim.value),
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                shadowElevation = 16.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Aha Logo",
                        modifier = Modifier.size(90.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App name
            Text(
                text = "Aha",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tagline
            Text(
                text = "Pelacak Ibadah Islami",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-tagline
            Text(
                text = "Catat, Pantau, Tingkatkan",
                fontSize = 14.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }

        // Version at bottom
        Text(
            text = "Versi 1.0",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}

// ============================================================================
// PREVIEW SECTION
// ============================================================================

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SplashScreenPreview() {
    HabitIslamiTheme {
        SplashScreenContent()
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Splash Dark")
@Composable
fun SplashScreenDarkPreview() {
    HabitIslamiTheme(darkTheme = true) {
        SplashScreenContent()
    }
}
