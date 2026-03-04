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
    var expanded by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var isEntered by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn) onRegisterSuccess()
    }
    LaunchedEffect(Unit) { isEntered = true }

    val cardSlide by animateFloatAsState(
        targetValue = if (isEntered) 0f else 800f,
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
        initialValue = -18f, targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "f1"
    )
    val float2 by infiniteTransition.animateFloat(
        initialValue = 15f, targetValue = -15f,
        animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
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
                    listOf(Secondary.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width * 0.86f + float1 * 0.6f, size.height * 0.1f + float1 * 0.3f),
                    radius = 240f
                ),
                radius = 240f,
                center = Offset(size.width * 0.86f + float1 * 0.6f, size.height * 0.1f + float1 * 0.3f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(GradientAccent.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.1f + float2 * 0.5f, size.height * 0.16f + float2 * 0.4f),
                    radius = 200f
                ),
                radius = 200f,
                center = Offset(size.width * 0.1f + float2 * 0.5f, size.height * 0.16f + float2 * 0.4f)
            )
        }

        IconButton(
            onClick = onNavigateToLogin,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 52.dp, start = 20.dp)
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.18f))
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.75f)
                    .graphicsLayer { alpha = heroAlpha; translationY = heroTranslation },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.93f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.PersonAdd,
                            contentDescription = null,
                            tint = GradientStart,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Join EventPay",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Create your free account today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.85f)
                    .graphicsLayer { translationY = cardSlide }
                    .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                    .background(Color.White)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 14.dp, bottom = 24.dp)
                        .size(width = 44.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(OutlineVariantLight)
                )

                Text(
                    "Create Account",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OnBackgroundLight
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Fill in your details to get started",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariantLight
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

                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Account Type",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceVariantLight,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedRole.name.lowercase().replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = when (selectedRole) {
                                        UserRole.ADMIN -> Icons.Outlined.AdminPanelSettings
                                        UserRole.SCANNER -> Icons.Outlined.QrCodeScanner
                                    },
                                    contentDescription = null,
                                    tint = Primary
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .height(60.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = OutlineLight,
                                focusedContainerColor = SurfaceVariantLight.copy(alpha = 0.3f),
                                unfocusedContainerColor = SurfaceVariantLight.copy(alpha = 0.3f)
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            UserRole.entries.forEach { role ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            role.name.lowercase().replaceFirstChar { it.uppercase() },
                                            fontWeight = if (role == selectedRole) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        selectedRole = role
                                        expanded = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = when (role) {
                                                UserRole.ADMIN -> Icons.Outlined.AdminPanelSettings
                                                UserRole.SCANNER -> Icons.Outlined.QrCodeScanner
                                            },
                                            contentDescription = null,
                                            tint = if (role == selectedRole) Primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

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
                    onClick = { authViewModel.register(email, password, fullName, selectedRole) },
                    text = "Create Account",
                    icon = Icons.Filled.PersonAdd,
                    enabled = email.isNotBlank() && password.isNotBlank() && fullName.isNotBlank() && !authState.isLoading,
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
                        "Already have an account?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariantLight
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Sign In",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onNavigateToLogin() }
                    )
                }
            }
        }
    }
}
