package com.example.eventpay.ui.screens.scanner

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.eventpay.data.model.User
import com.example.eventpay.domain.model.Event
import com.example.eventpay.ui.scanner.ScanStatus
import com.example.eventpay.ui.scanner.ScannerViewModel
import com.example.eventpay.ui.theme.*
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerHomeScreen(
    currentUser: User,
    scannerViewModel: ScannerViewModel,
    onLogout: () -> Unit
) {
    val uiState by scannerViewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    if (uiState.selectedEvent == null) {
        ScannerEventSelectionScreen(
            currentUser = currentUser,
            events = uiState.events,
            isLoading = uiState.isLoading,
            onSelectEvent = { scannerViewModel.selectEvent(it) },
            onRefresh = { scannerViewModel.loadActiveEvents() },
            onLogout = onLogout
        )
    } else {
        ScannerActiveScreen(
            currentUser = currentUser,
            uiState = uiState,
            scannerViewModel = scannerViewModel,
            onLogout = onLogout
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScannerEventSelectionScreen(
    currentUser: User,
    events: List<Event>,
    isLoading: Boolean,
    onSelectEvent: (Event) -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundLight)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(GradientStart, GradientMid))
                    )
                    .padding(top = 48.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                "Scanner Mode",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Hello, ${currentUser.fullName.split(" ").firstOrNull() ?: currentUser.fullName}",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentUser.fullName.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Logout") },
                                    leadingIcon = { Icon(Icons.Default.Logout, null) },
                                    onClick = { showMenu = false; onLogout() }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.15f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.TouchApp,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Select an active event below to start scanning",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (events.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.EventBusy,
                            contentDescription = null,
                            tint = Primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No active events",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnSurfaceVariantLight
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Contact your admin to assign you to an event",
                            fontSize = 13.sp,
                            color = OnSurfaceVariantLight.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Refresh")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${events.size} Active Event${if (events.size != 1) "s" else ""}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = OnSurfaceLight
                            )
                            IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Refresh, null, tint = Primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    items(events, key = { it.id }) { event ->
                        EventSelectionCard(event = event, onClick = { onSelectEvent(event) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EventSelectionCard(event: Event, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Event, null, tint = Primary, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    event.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = OnSurfaceLight
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.LocationOn, null,
                        tint = OnSurfaceVariantLight,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        event.location,
                        fontSize = 12.sp,
                        color = OnSurfaceVariantLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Schedule, null,
                        tint = OnSurfaceVariantLight,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        dateFormat.format(Date(event.date)),
                        fontSize = 12.sp,
                        color = OnSurfaceVariantLight
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Select event",
                tint = Primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScannerActiveScreen(
    currentUser: User,
    uiState: com.example.eventpay.ui.scanner.ScannerUiState,
    scannerViewModel: ScannerViewModel,
    onLogout: () -> Unit
) {
    val event = uiState.selectedEvent ?: return
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        QRCameraPreview(
            modifier = Modifier.fillMaxSize(),
            onQRCodeScanned = { rawData ->
                scannerViewModel.processQRCode(
                    rawQrData = rawData,
                    scannerId = currentUser.id,
                    scannerName = currentUser.fullName
                )
            }
        )

        ScannerOverlay(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { scannerViewModel.clearSelectedEvent() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                event.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "Tap back to change event",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            null,
                            tint = Tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "${uiState.sessionCheckInCount}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ScanFrame()
            }

            AnimatedVisibility(
                visible = uiState.scanStatus != ScanStatus.IDLE,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                ScanResultCard(uiState = uiState)
            }

            if (uiState.scanStatus == ScanStatus.IDLE) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f))
                    ) {
                        Text(
                            "Point camera at QR code",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanFrame() {
    val cornerColor = QrScannerCorner
    val size = 240.dp
    val cornerLength = 32.dp
    val strokeWidth = 4.dp
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val lineOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLine"
    )

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(strokeWidth, Color.Transparent, RoundedCornerShape(4.dp))
        )

        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.toPx()
            val h = size.toPx()
            val cl = cornerLength.toPx()
            val sw = strokeWidth.toPx()
            val color = cornerColor
            val cap = androidx.compose.ui.graphics.StrokeCap.Round

            drawLine(color = color, start = androidx.compose.ui.geometry.Offset(0f, 0f), end = androidx.compose.ui.geometry.Offset(cl, 0f), strokeWidth = sw, cap = cap)
            drawLine(color = color, start = androidx.compose.ui.geometry.Offset(0f, 0f), end = androidx.compose.ui.geometry.Offset(0f, cl), strokeWidth = sw, cap = cap)
            drawLine(color = color, start = androidx.compose.ui.geometry.Offset(w, 0f), end = androidx.compose.ui.geometry.Offset(w - cl, 0f), strokeWidth = sw, cap = cap)
            drawLine(color = color, start = androidx.compose.ui.geometry.Offset(w, 0f), end = androidx.compose.ui.geometry.Offset(w, cl), strokeWidth = sw, cap = cap)
            drawLine(color = color, start = androidx.compose.ui.geometry.Offset(0f, h), end = androidx.compose.ui.geometry.Offset(cl, h), strokeWidth = sw, cap = cap)
            drawLine(color = color, start = androidx.compose.ui.geometry.Offset(0f, h), end = androidx.compose.ui.geometry.Offset(0f, h - cl), strokeWidth = sw, cap = cap)
            drawLine(color = color, start = androidx.compose.ui.geometry.Offset(w, h), end = androidx.compose.ui.geometry.Offset(w - cl, h), strokeWidth = sw, cap = cap)
            drawLine(color = color, start = androidx.compose.ui.geometry.Offset(w, h), end = androidx.compose.ui.geometry.Offset(w, h - cl), strokeWidth = sw, cap = cap)

            val lineY = h * lineOffset
            drawLine(
                color = QrScannerLine.copy(alpha = 0.8f),
                start = androidx.compose.ui.geometry.Offset(8f, lineY),
                end = androidx.compose.ui.geometry.Offset(w - 8f, lineY),
                strokeWidth = sw * 0.8f
            )
        }
    }
}

@Composable
private fun ScannerOverlay(modifier: Modifier) {
    Box(modifier = modifier.background(QrScannerOverlay.copy(alpha = 0.45f)))
}

@Composable
private fun ScanResultCard(uiState: com.example.eventpay.ui.scanner.ScannerUiState) {
    val (bgColor, iconTint, icon, title, subtitle) = when (uiState.scanStatus) {
        ScanStatus.SUCCESS -> ResultStyle(
            bg = Success,
            tint = Color.White,
            icon = Icons.Default.CheckCircle,
            title = "Check-In Successful!",
            subtitle = uiState.lastScannedName ?: "Ticket verified"
        )
        ScanStatus.ALREADY_SCANNED -> ResultStyle(
            bg = Warning,
            tint = Color.White,
            icon = Icons.Default.Warning,
            title = "Already Checked In",
            subtitle = uiState.lastScannedName ?: "This ticket was already used"
        )
        ScanStatus.INVALID -> ResultStyle(
            bg = Error,
            tint = Color.White,
            icon = Icons.Default.Cancel,
            title = "Invalid Ticket",
            subtitle = uiState.error ?: "Ticket is not valid"
        )
        ScanStatus.NOT_FOUND -> ResultStyle(
            bg = Error,
            tint = Color.White,
            icon = Icons.Default.SearchOff,
            title = "Ticket Not Found",
            subtitle = "No matching ticket in system"
        )
        ScanStatus.ERROR -> ResultStyle(
            bg = Error,
            tint = Color.White,
            icon = Icons.Default.ErrorOutline,
            title = "Scan Error",
            subtitle = uiState.error ?: "An error occurred"
        )
        else -> ResultStyle(
            bg = Primary,
            tint = Color.White,
            icon = Icons.Default.QrCodeScanner,
            title = "Scanning...",
            subtitle = "Please wait"
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = iconTint, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, color = iconTint.copy(alpha = 0.85f), fontSize = 13.sp)
                }
            }
        }
    }
}

private data class ResultStyle(
    val bg: Color,
    val tint: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val subtitle: String
)

@Composable
private fun QRCameraPreview(
    modifier: Modifier = Modifier,
    onQRCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(executor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val inputImage = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                scanner.process(inputImage)
                                    .addOnSuccessListener { barcodes ->
                                        barcodes.firstOrNull()?.rawValue?.let { value ->
                                            onQRCodeScanned(value)
                                        }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else {
                                imageProxy.close()
                            }
                        }
                    }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = modifier
    )
}
