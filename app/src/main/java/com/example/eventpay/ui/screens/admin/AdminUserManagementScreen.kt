package com.example.eventpay.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.eventpay.data.model.User
import com.example.eventpay.ui.admin.AdminViewModel
import com.example.eventpay.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserManagementScreen(
    adminViewModel: AdminViewModel,
    currentAdminId: String,
    onBack: () -> Unit
) {
    val uiState by adminViewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { adminViewModel.loadScanners() }

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            kotlinx.coroutines.delay(2500)
            adminViewModel.clearMessages()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundLight)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color(0xFF9B59B6), Color(0xFF6C3483))))
                        .padding(top = 52.dp, start = 4.dp, end = 20.dp, bottom = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                        }
                        Column {
                            Text(
                                "Scanner Staff",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "${uiState.scanners.size} accounts",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = Color(0xFF9B59B6),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, null, tint = Color.White)
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                uiState.successMessage?.let { msg ->
                    Surface(
                        color = Tertiary,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(msg, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                uiState.error?.let { err ->
                    Surface(
                        color = ErrorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = Error, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(err, color = ErrorDark, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF9B59B6))
                    }
                } else if (uiState.scanners.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.QrCodeScanner,
                                null,
                                modifier = Modifier.size(72.dp),
                                tint = OutlineLight
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No scanner accounts yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = OnSurfaceVariantLight
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Create scanner accounts for your staff",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariantLight
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { showCreateDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9B59B6))
                            ) {
                                Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Scanner")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.scanners, key = { it.id }) { scanner ->
                            ScannerUserCard(
                                user = scanner,
                                onToggleActive = { newActive ->
                                    adminViewModel.toggleScannerActive(scanner.id, newActive)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateScannerDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { email, password, fullName ->
                adminViewModel.createScanner(email, password, fullName, currentAdminId)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun ScannerUserCard(
    user: User,
    onToggleActive: (Boolean) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val initials = user.fullName
        .split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(5.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceLight
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (user.isActive) Color(0xFF9B59B6).copy(alpha = 0.15f)
                        else OutlineLight.copy(alpha = 0.3f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    initials,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (user.isActive) Color(0xFF9B59B6) else OnSurfaceVariantLight
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        user.fullName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (user.isActive) Tertiary.copy(alpha = 0.12f) else ErrorContainer
                    ) {
                        Text(
                            if (user.isActive) "ACTIVE" else "INACTIVE",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (user.isActive) Tertiary else Error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariantLight
                )
                user.lastLoginAt?.let { lastLogin ->
                    Text(
                        "Last login: ${dateFormat.format(Date(lastLogin))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariantLight
                    )
                }
            }

            Switch(
                checked = user.isActive,
                onCheckedChange = onToggleActive,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Tertiary,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = OutlineLight
                )
            )
        }
    }
}

@Composable
private fun CreateScannerDialog(
    onDismiss: () -> Unit,
    onConfirm: (email: String, password: String, fullName: String) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isValid = fullName.isNotBlank() && email.isNotBlank() && password.length >= 8

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.QrCodeScanner,
                    null,
                    tint = Color(0xFF9B59B6),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Scanner Account", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Create a new staff account with QR scanning access only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariantLight
                )
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name *") },
                    leadingIcon = { Icon(Icons.Outlined.Person, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF9B59B6))
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email *") },
                    leadingIcon = { Icon(Icons.Outlined.Email, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF9B59B6))
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (min. 8 chars) *") },
                    leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                null
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF9B59B6))
                )
                if (password.isNotEmpty() && password.length < 8) {
                    Text(
                        "Password must be at least 8 characters",
                        style = MaterialTheme.typography.labelSmall,
                        color = Error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(email.trim(), password, fullName.trim()) },
                enabled = isValid,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9B59B6))
            ) {
                Text("Create Account", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = OnSurfaceVariantLight)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
