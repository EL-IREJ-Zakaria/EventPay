package com.example.eventpay.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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

    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn) onLoginSuccess()
    }
    LaunchedEffect(Unit) { isEntered = true }

    val cardSlide by animateFloatAsState(
        targetValue = if (isEntered) 0f else 700f,
        animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow),
        label = "cardSlide"
    )
    val heroAlpha by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0f,
        animationSpec = tween(700, delayMillis = 150, easing = FastOutSlowInEasing),
        label = "heroAlpha"
    )
    val heroTranslation by animateFloatAsState(
        targetValue = if (isEntered) 0f else -60f,
        animationSpec = tween(700, delayMillis = 150, easing = FastOutSlowInEasing),
        label = "heroTranslation"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "loginBg")
    val float1 by infiniteTransition.animateFloat(
        initialValue = -20f, targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(3500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "f1"
    )
    val float2 by infiniteTransition.animateFloat(
        initialValue = 18f, targetValue = -18f,
        animationSpec = infiniteRepeatable(tween(2700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "f2"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(GradientStart, GradientMid, PrimaryDark)))
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(GradientEnd.copy(alpha = 0.3f), Color.Transparent),
                    center = Offset(size.width * 0.12f + float1, size.height * 0.12f + float1 * 0.4f),
                    radius = 280f
                ),
                radius = 280f,
                center = Offset(size.width * 0.12f + float1, size.height * 0.12f + float1 * 0.4f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(GradientAccent.copy(alpha = 0.2f), Color.Transparent),
                    center = Offset(size.width * 0.88f + float2, size.height * 0.26f + float2 * 0.5f),
                    radius = 220f
                ),
                radius = 220f,
                center = Offset(size.width * 0.88f + float2, size.height * 0.26f + float2 * 0.5f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.04f),
                radius = 160f,
                center = Offset(size.width * 0.5f, size.height * 0.07f)
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .graphicsLayer { alpha = heroAlpha; translationY = heroTranslation },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.White.copy(alpha = 0.93f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Event,
                            contentDescription = null,
                            tint = GradientStart,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(22.dp))
                Text(
                    "EventPay",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Your all-in-one event experience",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.65f)
                    .graphicsLayer { translationY = cardSlide }
                    .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                    .background(Color.White)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 14.dp, bottom = 28.dp)
                        .size(width = 44.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(OutlineVariantLight)
                )

                Text(
                    "Sign In",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OnBackgroundLight
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Welcome back! Enter your credentials",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariantLight
                )

                Spacer(modifier = Modifier.height(30.dp))

                PremiumTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email Address",
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        "Forgot Password?",
                        style = MaterialTheme.typography.bodySmall,
                        color = Primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = authState.error != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        color = ErrorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Error, null, tint = Error, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                authState.error ?: "",
                                color = ErrorDark,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                AnimatedGradientButton(
                    onClick = { authViewModel.login(email, password) },
                    text = "Sign In",
                    icon = Icons.Filled.Login,
                    enabled = email.isNotBlank() && password.isNotBlank() && !authState.isLoading,
                    isLoading = authState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Don't have an account?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariantLight
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Sign Up",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onNavigateToRegister() }
                    )
                }
            }
        }
    }
}
