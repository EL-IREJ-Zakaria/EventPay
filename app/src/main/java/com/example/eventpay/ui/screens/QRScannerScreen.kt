package com.example.eventpay.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.eventpay.ui.qrcode.QRCodeViewModel
import com.example.eventpay.ui.qrcode.ScanResult
import com.example.eventpay.ui.theme.*
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRScannerScreen(
    qrCodeViewModel: QRCodeViewModel,
    onBack: () -> Unit
) {
    var qrCodeText by remember { mutableStateOf("") }
    val qrCodeState by qrCodeViewModel.qrCodeState.collectAsState()
    var hasCameraPermission by remember { mutableStateOf(false) }
    var isFlashOn by remember { mutableStateOf(false) }
    var isScanningEnabled by remember { mutableStateOf(true) }
    var showManualEntry by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasCameraPermission = isGranted }

    LaunchedEffect(Unit) {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            hasCameraPermission = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(qrCodeState.scanResult) {
        if (qrCodeState.scanResult != null) {
            isScanningEnabled = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            CameraPreview(
                isFlashOn = isFlashOn,
                isScanningEnabled = isScanningEnabled,
                onQRCodeDetected = { qrCode ->
                    if (isScanningEnabled && qrCode.isNotBlank()) {
                        isScanningEnabled = false
                        qrCodeViewModel.processQRCode(qrCode)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            ScannerNoPermissionView(
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) }
            )
        }

        if (hasCameraPermission && qrCodeState.scanResult == null) {
            QRScannerOverlay(modifier = Modifier.fillMaxSize())
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            ScannerTopBar(
                isFlashOn = isFlashOn,
                onToggleFlash = { isFlashOn = !isFlashOn },
                onBack = onBack,
                hasCameraPermission = hasCameraPermission
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (qrCodeState.scanResult == null && !showManualEntry) {
                    ScannerHintBadge()
                    Spacer(modifier = Modifier.height(16.dp))
                }

                AnimatedVisibility(
                    visible = qrCodeState.scanResult == null,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 }
                ) {
                    ScannerBottomPanel(
                        qrCodeText = qrCodeText,
                        onQrCodeChange = { qrCodeText = it.uppercase() },
                        isLoading = qrCodeState.isLoading,
                        showManualEntry = showManualEntry,
                        onToggleManualEntry = { showManualEntry = !showManualEntry },
                        onManualSubmit = {
                            if (qrCodeText.isNotBlank()) {
                                isScanningEnabled = false
                                qrCodeViewModel.processQRCode(qrCodeText)
                            }
                        }
                    )
                }

                AnimatedVisibility(
                    visible = qrCodeState.scanResult != null,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    when (val result = qrCodeState.scanResult) {
                        is ScanResult.Success -> ScanSuccessCard(
                            ticket = result.ticket,
                            eventName = result.eventName,
                            onScanAnother = {
                                qrCodeViewModel.clearResult()
                                qrCodeText = ""
                                isScanningEnabled = true
                                showManualEntry = false
                            }
                        )
                        is ScanResult.AlreadyCheckedIn -> ScanWarningCard(
                            title = "Already Checked In",
                            message = "This ticket was already used at ${
                                result.checkedInAt?.let {
                                    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                        .format(java.util.Date(it))
                                } ?: "Unknown time"
                            }",
                            onScanAnother = {
                                qrCodeViewModel.clearResult()
                                qrCodeText = ""
                                isScanningEnabled = true
                                showManualEntry = false
                            }
                        )
                        is ScanResult.Invalid -> ScanErrorCard(
                            message = result.message,
                            onScanAnother = {
                                qrCodeViewModel.clearResult()
                                qrCodeText = ""
                                isScanningEnabled = true
                                showManualEntry = false
                            }
                        )
                        null -> {}
                    }
                }

                AnimatedVisibility(visible = qrCodeState.error != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ErrorContainer)
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = Error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                qrCodeState.error ?: "",
                                color = ErrorDark,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { qrCodeViewModel.clearError() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = ErrorDark,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ScannerTopBar(
    isFlashOn: Boolean,
    onToggleFlash: () -> Unit,
    onBack: () -> Unit,
    hasCameraPermission: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "QR Scanner",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    "Point at a ticket QR code",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            if (hasCameraPermission) {
                IconButton(
                    onClick = onToggleFlash,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isFlashOn) Primary.copy(alpha = 0.85f)
                            else Color.Black.copy(alpha = 0.5f)
                        )
                ) {
                    Icon(
                        if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flash",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(44.dp))
            }
        }
    }
}

@Composable
private fun QRScannerOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanLine"
    )
    val cornerGlow by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cornerGlow"
    )

    Box(
        modifier = modifier
            .drawBehind {
                val cw = size.width
                val ch = size.height
                val frameSize = cw * 0.68f
                val left = (cw - frameSize) / 2
                val top = (ch - frameSize) / 2 - 60f
                val right = left + frameSize
                val bottom = top + frameSize
                val corner = 60f
                val strokeW = 4f

                drawRect(Color.Black.copy(alpha = 0.55f), Offset.Zero, Size(cw, top))
                drawRect(Color.Black.copy(alpha = 0.55f), Offset(0f, bottom), Size(cw, ch - bottom))
                drawRect(Color.Black.copy(alpha = 0.55f), Offset(0f, top), Size(left, frameSize))
                drawRect(Color.Black.copy(alpha = 0.55f), Offset(right, top), Size(cw - right, frameSize))

                val c = QrScannerCorner.copy(alpha = cornerGlow)

                drawLine(c, Offset(left, top), Offset(left + corner, top), strokeW, StrokeCap.Round)
                drawLine(c, Offset(left, top), Offset(left, top + corner), strokeW, StrokeCap.Round)

                drawLine(c, Offset(right - corner, top), Offset(right, top), strokeW, StrokeCap.Round)
                drawLine(c, Offset(right, top), Offset(right, top + corner), strokeW, StrokeCap.Round)

                drawLine(c, Offset(left, bottom), Offset(left + corner, bottom), strokeW, StrokeCap.Round)
                drawLine(c, Offset(left, bottom - corner), Offset(left, bottom), strokeW, StrokeCap.Round)

                drawLine(c, Offset(right - corner, bottom), Offset(right, bottom), strokeW, StrokeCap.Round)
                drawLine(c, Offset(right, bottom - corner), Offset(right, bottom), strokeW, StrokeCap.Round)

                val lineY = top + (frameSize * scanLineY)
                val scanBrush = Brush.horizontalGradient(
                    listOf(Color.Transparent, QrScannerLine.copy(alpha = 0.9f), Color.Transparent),
                    startX = left,
                    endX = right
                )
                drawRect(scanBrush, Offset(left, lineY), Size(frameSize, 2.5f))
            }
    )
}

@Composable
private fun ScannerHintBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Tertiary)
            )
            Text(
                "Scanning automatically...",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun ScannerBottomPanel(
    qrCodeText: String,
    onQrCodeChange: (String) -> Unit,
    isLoading: Boolean,
    showManualEntry: Boolean,
    onToggleManualEntry: () -> Unit,
    onManualSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(Color(0xF2111827))
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Manual Entry",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
            TextButton(onClick = onToggleManualEntry) {
                Text(
                    if (showManualEntry) "Hide" else "Enter Code",
                    color = Primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    if (showManualEntry) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = showManualEntry,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = qrCodeText,
                    onValueChange = onQrCodeChange,
                    placeholder = { Text("Ticket QR code...", color = Color.White.copy(alpha = 0.35f)) },
                    leadingIcon = {
                        Icon(Icons.Outlined.QrCode, null, tint = Primary)
                    },
                    trailingIcon = {
                        if (qrCodeText.isNotBlank()) {
                            IconButton(onClick = { onQrCodeChange("") }) {
                                Icon(Icons.Default.Clear, null, tint = Color.White.copy(alpha = 0.5f))
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Primary,
                        focusedContainerColor = Color.White.copy(alpha = 0.06f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.06f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (qrCodeText.isNotBlank() && !isLoading)
                                Brush.linearGradient(listOf(Primary, PrimaryDark))
                            else
                                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.1f)))
                        )
                        .then(
                            if (qrCodeText.isNotBlank() && !isLoading)
                                Modifier.clickableNoRipple(onManualSubmit)
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = if (qrCodeText.isNotBlank()) Color.White else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "Check In",
                                color = if (qrCodeText.isNotBlank()) Color.White else Color.White.copy(alpha = 0.3f),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanSuccessCard(
    ticket: com.example.eventpay.data.model.Ticket,
    eventName: String,
    onScanAnother: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(Color(0xF2111827))
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Tertiary.copy(alpha = 0.15f))
                .border(2.dp, Tertiary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CheckCircle, null, tint = Tertiary, modifier = Modifier.size(40.dp))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Check-in Successful!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Tertiary
            )
            Text(
                "Ticket validated",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ScanInfoRow("Event", eventName, Icons.Outlined.Event)
            ScanInfoRow("Ticket Type", ticket.ticketType.name, Icons.Outlined.ConfirmationNumber)
            ScanInfoRow("Price", "Free", Icons.Outlined.AttachMoney)
            ScanInfoRow("ID", ticket.qrCode.take(16) + "...", Icons.Outlined.QrCode)
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(Primary, PrimaryDark)))
                .clickableNoRipple(onScanAnother),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QrCodeScanner, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Text("Scan Another", color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ScanWarningCard(title: String, message: String, onScanAnother: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(Color(0xF2111827))
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Warning.copy(alpha = 0.15f))
                .border(2.dp, Warning, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Warning, null, tint = Warning, modifier = Modifier.size(40.dp))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Warning)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center)
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(Primary, PrimaryDark)))
                .clickableNoRipple(onScanAnother),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QrCodeScanner, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Text("Scan Another", color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ScanErrorCard(message: String, onScanAnother: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(Color(0xF2111827))
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Error.copy(alpha = 0.15f))
                .border(2.dp, Error, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Cancel, null, tint = Error, modifier = Modifier.size(40.dp))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Invalid Ticket", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Error)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center)
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(Primary, PrimaryDark)))
                .clickableNoRipple(onScanAnother),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QrCodeScanner, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Text("Try Again", color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ScanInfoRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Primary, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.45f))
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ScannerNoPermissionView(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GradientStart, GradientEnd))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.CameraAlt, null, tint = Color.White, modifier = Modifier.size(52.dp))
            }
            Text(
                "Camera Permission Required",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                "Please allow camera access to scan QR codes for ticket check-in.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .clickableNoRipple(onRequestPermission),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Grant Permission",
                    color = Primary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun CameraPreview(
    isFlashOn: Boolean,
    isScanningEnabled: Boolean,
    onQRCodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var camera: Camera? by remember { mutableStateOf(null) }
    val barcodeScanner = remember { BarcodeScanning.getClient() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = Executors.newSingleThreadExecutor()
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(executor) { imageProxy ->
                            if (isScanningEnabled) {
                                processImageProxy(imageProxy, barcodeScanner, onQRCodeDetected)
                            } else {
                                imageProxy.close()
                            }
                        }
                    }
                try {
                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        update = { _ -> camera?.cameraControl?.enableTorch(isFlashOn) },
        modifier = modifier
    )
}

private fun processImageProxy(
    imageProxy: ImageProxy,
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    onQRCodeDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    if (barcode.format == Barcode.FORMAT_QR_CODE) {
                        barcode.rawValue?.let { onQRCodeDetected(it) }
                    }
                }
            }
            .addOnFailureListener { it.printStackTrace() }
            .addOnCompleteListener { imageProxy.close() }
    } else {
        imageProxy.close()
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(
        Modifier.clickable(
            interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
            indication = null,
            onClick = onClick
        )
    )
