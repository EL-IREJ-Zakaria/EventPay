package com.example.eventpay.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eventpay.data.model.UserRole
import com.example.eventpay.ui.auth.AuthViewModel
import com.example.eventpay.ui.components.*
import com.example.eventpay.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.SCANNER) }
    var passwordVisible by remember { mutableStateOf(false) }
    var isEntered by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn) onRegisterSuccess()
    }
    LaunchedEffect(Unit) { isEntered = true }

    val cardSlide by animateFloatAsState(
        targetValue = if (isEntered) 0f else 900f,
        animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow),
        label = "cardSlide"
    )
    val heroAlpha by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0f,
        animationSpec = tween(700, delayMillis = 100, easing = FastOutSlowInEasing),
        label = "heroAlpha"
    )
    val heroTranslation by animateFloatAsState(
        targetValue = if (isEntered) 0f else -50f,
        animationSpec = tween(700, delayMillis = 100, easing = FastOutSlowInEasing),
        label = "heroTranslation"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "regBg")
    val float1 by infiniteTransition.animateFloat(
        initialValue = -20f, targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(3400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
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
                .background(
                    Brush.linearGradient(
                        colorStops = arrayOf(
                            0f to Color(0xFF1A0A3D),
                            0.4f to GradientStart,
                            0.72f to GradientMid,
                            1f to Color(0xFF08060F)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(400f, 1600f)
                    )
                )
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(AuroraCyan.copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(size.width * 0.85f + float1, size.height * 0.08f + float1 * 0.3f),
                    radius = 310f
                ),
                radius = 310f,
                center = Offset(size.width * 0.85f + float1, size.height * 0.08f + float1 * 0.3f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(AuroraPink.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(size.width * 0.12f + float2, size.height * 0.2f + float2 * 0.4f),
                    radius = 270f
                ),
                radius = 270f,
                center = Offset(size.width * 0.12f + float2, size.height * 0.2f + float2 * 0.4f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(AuroraViolet.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.32f),
                    radius = 380f
                ),
                radius = 380f,
                center = Offset(size.width * 0.5f, size.height * 0.32f)
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
                    .weight(0.28f)
                    .graphicsLayer { alpha = heroAlpha; translationY = heroTranslation },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(Color.White.copy(0.95f), Color.White.copy(0.75f)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = GradientStart, modifier = Modifier.size(30.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Join EventPay",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Create your professional account",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 0.3.sp
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.72f)
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
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Zinc100)
                            .clickable { onNavigateToLogin() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Zinc700, modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        "Create Account",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Zinc900
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Get started with your event management journey",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Zinc400
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    PremiumTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = "Full Name",
                        leadingIcon = Icons.Outlined.Person,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

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

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        "Account Type",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Zinc600,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        UserRole.entries.forEach { role ->
                            val isSelected = role == selectedRole
                            val bgColor by animateColorAsState(
                                targetValue = if (isSelected) Primary.copy(alpha = 0.08f) else Zinc50,
                                animationSpec = tween(200),
                                label = "roleBg_${role.name}"
                            )
                            val borderColor by animateColorAsState(
                                targetValue = if (isSelected) Primary else Zinc200,
                                animationSpec = tween(200),
                                label = "roleBorder_${role.name}"
                            )
                            val iconTint by animateColorAsState(
                                targetValue = if (isSelected) Primary else Zinc400,
                                animationSpec = tween(200),
                                label = "roleIcon_${role.name}"
                            )

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedRole = role },
                                shape = RoundedCornerShape(16.dp),
                                color = bgColor,
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = borderColor
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(13.dp))
                                            .background(if (isSelected) Primary.copy(alpha = 0.12f) else Zinc100),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (role) {
                                                UserRole.ADMIN -> Icons.Outlined.AdminPanelSettings
                                                UserRole.SCANNER -> Icons.Outlined.QrCodeScanner
                                            },
                                            contentDescription = null,
                                            tint = iconTint,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = role.name.lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Primary else Zinc500
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

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
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Error, null, tint = Error, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(authState.error ?: "", color = Zinc800, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    AnimatedGradientButton(
                        onClick = { authViewModel.register(email, password, fullName, selectedRole) },
                        text = "Create Account",
                        icon = Icons.Filled.PersonAdd,
                        enabled = email.isNotBlank() && password.isNotBlank() && fullName.isNotBlank() && !authState.isLoading,
                        loading = authState.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f).height(1.dp).background(Zinc100))
                        Text("  or  ", style = MaterialTheme.typography.labelSmall, color = Zinc400)
                        Box(modifier = Modifier.weight(1f).height(1.dp).background(Zinc100))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Already have an account?", style = MaterialTheme.typography.bodyMedium, color = Zinc500)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Sign In",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onNavigateToLogin() }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
