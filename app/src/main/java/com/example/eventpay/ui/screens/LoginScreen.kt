package com.example.eventpay.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eventpay.ui.auth.AuthViewModel
import com.example.eventpay.ui.components.*
import com.example.eventpay.ui.theme.*

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isEntered by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState.isLoggedIn) { if (authState.isLoggedIn) onLoginSuccess() }
    LaunchedEffect(Unit) { isEntered = true }

    val cardSlide by animateFloatAsState(
        targetValue = if (isEntered) 0f else 800f,
        animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow),
        label = "cardSlide"
    )
    val heroAlpha by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0f,
        animationSpec = tween(700, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "heroAlpha"
    )
    val heroTranslation by animateFloatAsState(
        targetValue = if (isEntered) 0f else -50f,
        animationSpec = tween(700, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "heroTranslation"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "loginBg")
    val float1 by infiniteTransition.animateFloat(
        initialValue = -25f, targetValue = 25f,
        animationSpec = infiniteRepeatable(tween(3800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "f1"
    )
    val float2 by infiniteTransition.animateFloat(
        initialValue = 22f, targetValue = -22f,
        animationSpec = infiniteRepeatable(tween(2900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "f2"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colorStops = arrayOf(
                            0f to Color(0xFF1A0A3D),
                            0.45f to GradientStart,
                            0.75f to GradientMid,
                            1f to Color(0xFF08060F)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(400f, 1400f)
                    )
                )
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(AuroraCyan.copy(alpha = 0.2f), Color.Transparent),
                    center = Offset(size.width * 0.1f + float1, size.height * 0.1f + float1 * 0.3f),
                    radius = 320f
                ),
                radius = 320f,
                center = Offset(size.width * 0.1f + float1, size.height * 0.1f + float1 * 0.3f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(AuroraPink.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(size.width * 0.9f + float2, size.height * 0.22f + float2 * 0.5f),
                    radius = 280f
                ),
                radius = 280f,
                center = Offset(size.width * 0.9f + float2, size.height * 0.22f + float2 * 0.5f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(AuroraViolet.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.38f),
                    radius = 350f
                ),
                radius = 350f,
                center = Offset(size.width * 0.5f, size.height * 0.38f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.38f)
                    .graphicsLayer { alpha = heroAlpha; translationY = heroTranslation },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Brush.linearGradient(listOf(Color.White.copy(0.95f), Color.White.copy(0.75f)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Event, contentDescription = null, tint = GradientStart, modifier = Modifier.size(36.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    "EventPay",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Secure Event Management",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 0.5.sp
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.62f)
                    .graphicsLayer { translationY = cardSlide }
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(SurfaceLight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 28.dp, vertical = 32.dp)
                ) {
                    Text(
                        "Welcome back",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Zinc900
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Sign in to continue",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Zinc400
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    PremiumTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email",
                        leadingIcon = Icons.Outlined.Email,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PremiumTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        leadingIcon = Icons.Outlined.Lock,
                        trailingIcon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        onTrailingIconClick = { passwordVisible = !passwordVisible },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            "Forgot password?",
                            style = MaterialTheme.typography.labelLarge,
                            color = Primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    AnimatedVisibility(
                        visible = authState.error != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            color = ErrorContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Error, null, tint = Error, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(authState.error ?: "", color = Zinc800, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    AnimatedGradientButton(
                        onClick = { authViewModel.login(email, password) },
                        text = "Sign In",
                        icon = Icons.Filled.Login,
                        enabled = email.isNotBlank() && password.isNotBlank() && !authState.isLoading,
                        loading = authState.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f).height(1.dp).background(Zinc100))
                        Text("  or  ", style = MaterialTheme.typography.labelSmall, color = Zinc400)
                        Box(modifier = Modifier.weight(1f).height(1.dp).background(Zinc100))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("New to EventPay?", style = MaterialTheme.typography.bodyMedium, color = Zinc500)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Create account",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onNavigateToRegister() }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
