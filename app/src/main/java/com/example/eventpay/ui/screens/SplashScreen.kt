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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eventpay.ui.components.AnimatedLoadingDots
import com.example.eventpay.ui.components.VersionTag
import com.example.eventpay.ui.theme.*
import kotlinx.coroutines.delay

private enum class SplashPhase { INIT, LOGO_IN, TEXT_IN, TAGLINE_IN, DOTS_IN, EXIT }

@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    var phase by remember { mutableStateOf(SplashPhase.INIT) }

    LaunchedEffect(Unit) {
        delay(150)
        phase = SplashPhase.LOGO_IN
        delay(750)
        phase = SplashPhase.TEXT_IN
        delay(550)
        phase = SplashPhase.TAGLINE_IN
        delay(500)
        phase = SplashPhase.DOTS_IN
        delay(1000)
        phase = SplashPhase.EXIT
        delay(550)
        onSplashComplete()
    }

    val logoScale by animateFloatAsState(
        targetValue = when (phase) {
            SplashPhase.INIT -> 0f
            SplashPhase.EXIT -> 1.2f
            else -> 1f
        },
        animationSpec = when (phase) {
            SplashPhase.LOGO_IN -> spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)
            SplashPhase.EXIT -> tween(400, easing = FastOutLinearInEasing)
            else -> tween(300)
        },
        label = "logoScale"
    )

    val logoAlpha by animateFloatAsState(
        targetValue = if (phase == SplashPhase.INIT || phase == SplashPhase.EXIT) 0f else 1f,
        animationSpec = tween(450),
        label = "logoAlpha"
    )

    val textOffsetY by animateFloatAsState(
        targetValue = when (phase) {
            SplashPhase.INIT, SplashPhase.LOGO_IN -> 50f
            SplashPhase.EXIT -> -40f
            else -> 0f
        },
        animationSpec = tween(550, easing = FastOutSlowInEasing),
        label = "textOffsetY"
    )

    val textAlpha by animateFloatAsState(
        targetValue = when (phase) {
            SplashPhase.TEXT_IN, SplashPhase.TAGLINE_IN, SplashPhase.DOTS_IN -> 1f
            else -> 0f
        },
        animationSpec = tween(500),
        label = "textAlpha"
    )

    val taglineAlpha by animateFloatAsState(
        targetValue = when (phase) {
            SplashPhase.TAGLINE_IN, SplashPhase.DOTS_IN -> 1f
            else -> 0f
        },
        animationSpec = tween(500),
        label = "taglineAlpha"
    )

    val dotsAlpha by animateFloatAsState(
        targetValue = when (phase) {
            SplashPhase.DOTS_IN -> 1f
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
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    val ringScale1 by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.7f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "ring1"
    )
    val ringAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "ringAlpha1"
    )

    val ringScale2 by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.7f,
        animationSpec = infiniteRepeatable(tween(1600, delayMillis = 800, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "ring2"
    )
    val ringAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1600, delayMillis = 800, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "ringAlpha2"
    )

    val orbRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing)),
        label = "orbRotation"
    )

    val auroraDrift by infiniteTransition.animateFloat(
        initialValue = -30f, targetValue = 30f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "aurora"
    )

    val particle1X by infiniteTransition.animateFloat(
        initialValue = -25f, targetValue = 25f,
        animationSpec = infiniteRepeatable(tween(2300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p1x"
    )
    val particle1Y by infiniteTransition.animateFloat(
        initialValue = -18f, targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p1y"
    )
    val particle2X by infiniteTransition.animateFloat(
        initialValue = 22f, targetValue = -22f,
        animationSpec = infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p2x"
    )
    val particle2Y by infiniteTransition.animateFloat(
        initialValue = 16f, targetValue = -16f,
        animationSpec = infiniteRepeatable(tween(2100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p2y"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(bgAlpha)
            .background(
                Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF2D1B69),
                        0.4f to GradientStart,
                        0.75f to GradientMid,
                        1f to Color(0xFF0A0818)
                    ),
                    radius = 1600f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        AuroraBackground(auroraDrift = auroraDrift, orbRotation = orbRotation, particle1X = particle1X, particle1Y = particle1Y, particle2X = particle2X, particle2Y = particle2Y)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                PulsingRings(
                    ringScale1 = ringScale1, ringAlpha1 = ringAlpha1,
                    ringScale2 = ringScale2, ringAlpha2 = ringAlpha2,
                    logoAlpha = logoAlpha
                )

                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(logoScale * if (phase == SplashPhase.DOTS_IN || phase == SplashPhase.TEXT_IN || phase == SplashPhase.TAGLINE_IN) pulseScale else 1f)
                        .alpha(logoAlpha),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color.White.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.98f),
                                            Color.White.copy(alpha = 0.82f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Event,
                                contentDescription = null,
                                tint = GradientStart,
                                modifier = Modifier.size(50.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

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
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(1.5.dp)
                        .alpha(taglineAlpha)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.6f), Color.Transparent)
                            )
                        )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Manage · Discover · Experience",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp,
                    modifier = Modifier.alpha(taglineAlpha)
                )
            }

            Spacer(modifier = Modifier.height(72.dp))

            AnimatedLoadingDots(modifier = Modifier.alpha(dotsAlpha))
        }

        VersionTag(modifier = Modifier.align(Alignment.BottomCenter).alpha(dotsAlpha))
    }
}

@Composable
private fun AuroraBackground(
    auroraDrift: Float,
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
                colors = listOf(AuroraPink.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(cx * 0.15f + auroraDrift, cy * 0.25f),
                radius = 450f
            ),
            radius = 450f,
            center = Offset(cx * 0.15f + auroraDrift, cy * 0.25f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(AuroraCyan.copy(alpha = 0.22f), Color.Transparent),
                center = Offset(cx * 1.85f - auroraDrift * 0.6f, cy * 1.72f),
                radius = 400f
            ),
            radius = 400f,
            center = Offset(cx * 1.85f - auroraDrift * 0.6f, cy * 1.72f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(AuroraViolet.copy(alpha = 0.15f), Color.Transparent),
                center = Offset(cx * 1.7f, cy * 0.2f + auroraDrift * 0.5f),
                radius = 380f
            ),
            radius = 380f,
            center = Offset(cx * 1.7f, cy * 0.2f + auroraDrift * 0.5f)
        )

        val orb1X = cx + 280f * kotlin.math.cos(Math.toRadians(orbRotation.toDouble())).toFloat()
        val orb1Y = cy + 200f * kotlin.math.sin(Math.toRadians(orbRotation.toDouble())).toFloat()
        drawCircle(color = Color.White.copy(alpha = 0.05f), radius = 90f, center = Offset(orb1X, orb1Y))

        val orb2X = cx + 220f * kotlin.math.cos(Math.toRadians((orbRotation + 180).toDouble())).toFloat()
        val orb2Y = cy + 160f * kotlin.math.sin(Math.toRadians((orbRotation + 180).toDouble())).toFloat()
        drawCircle(color = AuroraAmber.copy(alpha = 0.08f), radius = 60f, center = Offset(orb2X, orb2Y))

        listOf(
            Offset(cx - 130f + particle1X, cy - 230f + particle1Y) to (Color.White to 5.5f),
            Offset(cx + 170f + particle2X, cy - 190f + particle2Y) to (AuroraAmber to 5f),
            Offset(cx - 190f + particle2X * 0.5f, cy + 210f + particle1Y * 0.5f) to (AuroraCyan to 4f),
            Offset(cx + 110f + particle1X * 0.7f, cy + 250f + particle2Y * 0.7f) to (AuroraViolet to 6f),
            Offset(cx + 70f - particle2X, cy - 290f - particle1Y) to (Color.White to 3f),
            Offset(cx - 230f - particle1X * 0.4f, cy - 100f + particle2Y * 0.3f) to (AuroraPink to 4.5f)
        ).forEach { (center, colorRadius) ->
            drawCircle(
                color = colorRadius.first.copy(alpha = 0.55f),
                radius = colorRadius.second,
                center = center
            )
        }
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
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = size.minDimension * 0.27f

        drawCircle(
            color = Color.White.copy(alpha = ringAlpha1 * logoAlpha * 0.35f),
            radius = baseRadius * ringScale1,
            center = center,
            style = Stroke(width = 1.5f)
        )

        drawCircle(
            color = AuroraViolet.copy(alpha = ringAlpha2 * logoAlpha * 0.3f),
            radius = baseRadius * ringScale2,
            center = center,
            style = Stroke(width = 1.5f)
        )
    }
}
