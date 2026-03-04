package com.example.eventpay.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eventpay.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    var phase by remember { mutableStateOf(SplashPhase.INIT) }

    LaunchedEffect(Unit) {
        delay(200)
        phase = SplashPhase.LOGO_IN
        delay(700)
        phase = SplashPhase.TEXT_IN
        delay(600)
        phase = SplashPhase.TAGLINE_IN
        delay(500)
        phase = SplashPhase.DOTS_IN
        delay(900)
        phase = SplashPhase.EXIT
        delay(600)
        onSplashComplete()
    }

    val logoScale by animateFloatAsState(
        targetValue = when (phase) {
            SplashPhase.INIT -> 0f
            SplashPhase.EXIT -> 1.15f
            else -> 1f
        },
        animationSpec = when (phase) {
            SplashPhase.LOGO_IN -> spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
            SplashPhase.EXIT -> tween(400, easing = FastOutLinearInEasing)
            else -> tween(300)
        },
        label = "logoScale"
    )

    val logoAlpha by animateFloatAsState(
        targetValue = if (phase == SplashPhase.INIT || phase == SplashPhase.EXIT) 0f else 1f,
        animationSpec = tween(400),
        label = "logoAlpha"
    )

    val textOffsetY by animateFloatAsState(
        targetValue = when (phase) {
            SplashPhase.INIT, SplashPhase.LOGO_IN -> 40f
            SplashPhase.EXIT -> -30f
            else -> 0f
        },
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "textOffsetY"
    )

    val textAlpha by animateFloatAsState(
        targetValue = when (phase) {
            SplashPhase.TEXT_IN, SplashPhase.TAGLINE_IN, SplashPhase.DOTS_IN -> 1f
            SplashPhase.EXIT -> 0f
            else -> 0f
        },
        animationSpec = tween(450),
        label = "textAlpha"
    )

    val taglineAlpha by animateFloatAsState(
        targetValue = when (phase) {
            SplashPhase.TAGLINE_IN, SplashPhase.DOTS_IN -> 1f
            SplashPhase.EXIT -> 0f
            else -> 0f
        },
        animationSpec = tween(500),
        label = "taglineAlpha"
    )

    val dotsAlpha by animateFloatAsState(
        targetValue = when (phase) {
            SplashPhase.DOTS_IN -> 1f
            SplashPhase.EXIT -> 0f
            else -> 0f
        },
        animationSpec = tween(400),
        label = "dotsAlpha"
    )

    val bgAlpha by animateFloatAsState(
        targetValue = if (phase == SplashPhase.EXIT) 0f else 1f,
        animationSpec = tween(500),
        label = "bgAlpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "splash_infinite")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val ringScale1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            tween(1400, easing = LinearOutSlowInEasing),
            RepeatMode.Restart
        ),
        label = "ring1"
    )
    val ringAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            tween(1400, easing = LinearOutSlowInEasing),
            RepeatMode.Restart
        ),
        label = "ringAlpha1"
    )

    val ringScale2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            tween(1400, delayMillis = 700, easing = LinearOutSlowInEasing),
            RepeatMode.Restart
        ),
        label = "ring2"
    )
    val ringAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            tween(1400, delayMillis = 700, easing = LinearOutSlowInEasing),
            RepeatMode.Restart
        ),
        label = "ringAlpha2"
    )

    val orbRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "orbRotation"
    )

    val particle1X by infiniteTransition.animateFloat(
        initialValue = -30f, targetValue = 30f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p1x"
    )
    val particle1Y by infiniteTransition.animateFloat(
        initialValue = -20f, targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p1y"
    )
    val particle2X by infiniteTransition.animateFloat(
        initialValue = 20f, targetValue = -20f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p2x"
    )
    val particle2Y by infiniteTransition.animateFloat(
        initialValue = 15f, targetValue = -15f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p2y"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(bgAlpha)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        GradientMid,
                        GradientStart,
                        PrimaryDark
                    ),
                    radius = 1400f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        BackgroundOrbs(
            orbRotation = orbRotation,
            particle1X = particle1X,
            particle1Y = particle1Y,
            particle2X = particle2X,
            particle2Y = particle2Y
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                PulsingRings(
                    ringScale1 = ringScale1,
                    ringAlpha1 = ringAlpha1,
                    ringScale2 = ringScale2,
                    ringAlpha2 = ringAlpha2,
                    logoAlpha = logoAlpha
                )

                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(logoScale * if (phase == SplashPhase.DOTS_IN || phase == SplashPhase.TEXT_IN || phase == SplashPhase.TAGLINE_IN) pulseScale else 1f)
                        .alpha(logoAlpha)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.75f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Event,
                            contentDescription = null,
                            tint = GradientStart,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .graphicsLayer { translationY = textOffsetY }
                    .alpha(textAlpha)
            ) {
                Text(
                    text = "EventPay",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(2.dp)
                        .alpha(taglineAlpha)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.7f), Color.Transparent)
                            )
                        )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Manage · Discover · Experience",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.alpha(taglineAlpha)
                )
            }

            Spacer(modifier = Modifier.height(64.dp))

            AnimatedLoadingDots(modifier = Modifier.alpha(dotsAlpha))
        }

        VersionTag(modifier = Modifier.align(Alignment.BottomCenter).alpha(dotsAlpha))
    }
}

@Composable
private fun BackgroundOrbs(
    orbRotation: Float,
    particle1X: Float,
    particle1Y: Float,
    particle2X: Float,
    particle2Y: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height / 2

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(GradientEnd.copy(alpha = 0.25f), Color.Transparent),
                center = Offset(cx * 0.2f, cy * 0.3f),
                radius = 400f
            ),
            radius = 400f,
            center = Offset(cx * 0.2f, cy * 0.3f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(GradientAccent.copy(alpha = 0.2f), Color.Transparent),
                center = Offset(cx * 1.8f, cy * 1.7f),
                radius = 350f
            ),
            radius = 350f,
            center = Offset(cx * 1.8f, cy * 1.7f)
        )

        val orb1X = cx + 260f * kotlin.math.cos(Math.toRadians(orbRotation.toDouble())).toFloat()
        val orb1Y = cy + 180f * kotlin.math.sin(Math.toRadians(orbRotation.toDouble())).toFloat()
        drawCircle(
            color = Color.White.copy(alpha = 0.06f),
            radius = 80f,
            center = Offset(orb1X, orb1Y)
        )

        val orb2X = cx + 200f * kotlin.math.cos(Math.toRadians((orbRotation + 180).toDouble())).toFloat()
        val orb2Y = cy + 150f * kotlin.math.sin(Math.toRadians((orbRotation + 180).toDouble())).toFloat()
        drawCircle(
            color = Color.White.copy(alpha = 0.04f),
            radius = 55f,
            center = Offset(orb2X, orb2Y)
        )

        drawCircle(
            color = Color.White.copy(alpha = 0.5f),
            radius = 6f,
            center = Offset(cx - 140f + particle1X, cy - 220f + particle1Y)
        )
        drawCircle(
            color = GradientAccent.copy(alpha = 0.7f),
            radius = 5f,
            center = Offset(cx + 160f + particle2X, cy - 180f + particle2Y)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.4f),
            radius = 4f,
            center = Offset(cx - 180f + particle2X * 0.5f, cy + 200f + particle1Y * 0.5f)
        )
        drawCircle(
            color = GradientEnd.copy(alpha = 0.6f),
            radius = 7f,
            center = Offset(cx + 120f + particle1X * 0.7f, cy + 240f + particle2Y * 0.7f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.3f),
            radius = 3f,
            center = Offset(cx + 60f - particle2X, cy - 280f - particle1Y)
        )
    }
}

@Composable
private fun PulsingRings(
    ringScale1: Float,
    ringAlpha1: Float,
    ringScale2: Float,
    ringAlpha2: Float,
    logoAlpha: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val baseRadius = size.minDimension / 2f * 0.52f

        drawCircle(
            color = Color.White.copy(alpha = ringAlpha1 * logoAlpha * 0.4f),
            radius = baseRadius * ringScale1,
            center = center,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
        drawCircle(
            color = Color.White.copy(alpha = ringAlpha2 * logoAlpha * 0.3f),
            radius = baseRadius * ringScale2,
            center = center,
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.08f * logoAlpha), Color.Transparent),
                center = center,
                radius = baseRadius * 1.2f
            ),
            radius = baseRadius * 1.2f,
            center = center
        )
    }
}

@Composable
private fun AnimatedLoadingDots(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    val dot1Scale by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(500, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Scale by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(500, delayMillis = 160, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Scale by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(500, delayMillis = 320, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(dot1Scale, dot2Scale, dot3Scale).forEachIndexed { index, scale ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        when (index) {
                            0 -> Color.White
                            1 -> Color.White.copy(alpha = 0.8f)
                            else -> Color.White.copy(alpha = 0.6f)
                        }
                    )
            )
        }
    }
}

@Composable
private fun VersionTag(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Powered by EventPay",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.45f),
            letterSpacing = 1.sp
        )
        Text(
            text = "v1.0",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.3f)
        )
    }
}

private enum class SplashPhase {
    INIT, LOGO_IN, TEXT_IN, TAGLINE_IN, DOTS_IN, EXIT
}
