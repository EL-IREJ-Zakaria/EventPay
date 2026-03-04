package com.example.eventpay.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.eventpay.ui.theme.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

/**
 * QR Scanner Screen with CameraX
 * 
 * Features:
 * - Real-time camera preview
 * - QR code detection and parsing
 * - Flashlight toggle
 * - Real-time feedback overlay
 * - Ticket preview before check-in
 * - Success/error animations
 */
@Composable
fun QRScannerScreen(
    eventId: String,
    deviceId: String,
    userId: String,
    onBack: () -> Unit,
    viewModel: QRScannerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    
    // Camera executor
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    // Camera permission state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }
    
    // Request permission on first launch
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    // Cleanup camera executor
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Camera Preview
        if (hasCameraPermission) {
            CameraPreview(
                lifecycleOwner = lifecycleOwner,
                cameraExecutor = cameraExecutor,
                isFlashlightOn = uiState.isFlashlightOn,
                onQRCodeDetected = { qrCode ->
                    viewModel.onQRCodeScanned(qrCode, eventId)
                },
                isScanningEnabled = uiState.scanningState == ScanningState.SCANNING
            )
        } else {
            // Permission denied UI
            CameraPermissionDenied(
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            )
        }
        
        // Scanning overlay
        ScanningOverlay(
            scanningState = uiState.scanningState,
            isFlashlightOn = uiState.isFlashlightOn,
            onToggleFlashlight = { viewModel.toggleFlashlight() },
            onBack = onBack
        )
        
        // Loading overlay
        if (uiState.isLoading) {
            LoadingOverlay()
        }
        
        // Preview dialog
        if (uiState.scanningState == ScanningState.PREVIEW && uiState.previewData != null) {
            TicketPreviewDialog(
                previewData = uiState.previewData!!,
                onConfirm = {
                    viewModel.confirmCheckIn(eventId, deviceId, userId)
                },
                onCancel = { viewModel.cancelPreview() }
            )
        }
        
        // Success overlay
        if (uiState.scanningState == ScanningState.SUCCESS && uiState.successData != null) {
            SuccessOverlay(
                successData = uiState.successData!!,
                onDismiss = { viewModel.resetScanner() }
            )
        }
        
        // Error overlay
        if (uiState.scanningState == ScanningState.ERROR && uiState.error != null) {
            ErrorOverlay(
                error = uiState.error!!,
                onDismiss = { viewModel.dismissError() }
            )
        }
    }
}

/**
 * Camera Preview with QR Code detection
 */
@Composable
fun CameraPreview(
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    cameraExecutor: ExecutorService,
    isFlashlightOn: Boolean,
    onQRCodeDetected: (String) -> Unit,
    isScanningEnabled: Boolean
) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }
    
    // QR Code analyzer
    val qrCodeAnalyzer = remember {
        QRCodeAnalyzer { qrCode ->
            if (isScanningEnabled) {
                onQRCodeDetected(qrCode)
            }
        }
    }
    
    // Camera provider future
    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }
    
    // Bind camera use cases
    LaunchedEffect(cameraProviderFuture, isFlashlightOn) {
        val cameraProvider = cameraProviderFuture.get()
        
        // Unbind all use cases
        cameraProvider.unbindAll()
        
        // Preview use case
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
        
        // Image analysis use case for QR code detection
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, qrCodeAnalyzer)
            }
        
        // Select back camera
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        
        try {
            // Bind use cases to camera
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
            
            // Control flashlight
            camera.cameraControl.enableTorch(isFlashlightOn)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Camera preview view
    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * QR Code Analyzer using ZXing
 */
class QRCodeAnalyzer(
    private val onQRCodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {
    
    private val reader = MultiFormatReader()
    private var isProcessing = false
    
    override fun analyze(image: ImageProxy) {
        if (isProcessing) {
            image.close()
            return
        }
        
        isProcessing = true
        
        try {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            
            val source = PlanarYUVLuminanceSource(
                bytes,
                image.width,
                image.height,
                0,
                0,
                image.width,
                image.height,
                false
            )
            
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            
            try {
                val result = reader.decode(binaryBitmap)
                onQRCodeDetected(result.text)
            } catch (e: Exception) {
                // No QR code found in this frame
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            image.close()
            isProcessing = false
        }
    }
}

@Composable
fun ScanningOverlay(
    scanningState: ScanningState,
    isFlashlightOn: Boolean,
    onToggleFlashlight: () -> Unit,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (scanningState == ScanningState.SCANNING) {
            ScannerVignetteOverlay()
            ScanFrame()
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.45f),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Scan Ticket",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = onToggleFlashlight,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isFlashlightOn) Primary.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.45f),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                Icon(
                    imageVector = if (isFlashlightOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Flash",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (scanningState == ScanningState.SCANNING) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 52.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 28.dp, vertical = 14.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Point camera at the QR Code",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Scanning happens automatically",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ScannerVignetteOverlay() {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        val scanSize = 264.dp.toPx()
        val cx = size.width / 2f
        val cy = size.height * 0.44f
        drawRect(Color.Black.copy(alpha = 0.66f))
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(cx - scanSize / 2f, cy - scanSize / 2f),
            size = Size(scanSize, scanSize),
            cornerRadius = CornerRadius(20.dp.toPx()),
            blendMode = BlendMode.Clear
        )
    }
}

@Composable
fun ScanFrame() {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")

    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "scanLine"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val scanSize = 264.dp.toPx()
        val cx = size.width / 2f
        val cy = size.height * 0.44f
        val left = cx - scanSize / 2f
        val top = cy - scanSize / 2f
        val right = cx + scanSize / 2f
        val bottom = cy + scanSize / 2f
        val bracketLen = 38.dp.toPx()
        val cornerR = 18.dp.toPx()
        val strokeW = 4.dp.toPx()

        listOf(
            Triple(left, top, Pair(true, true)),
            Triple(right, top, Pair(true, false)),
            Triple(left, bottom, Pair(false, true)),
            Triple(right, bottom, Pair(false, false))
        ).forEach { (x, y, dirs) ->
            val path = buildCornerPath(x, y, bracketLen, cornerR, dirs.first, dirs.second)
            drawPath(path, Primary.copy(alpha = 0.28f * glowAlpha), style = Stroke(strokeW + 7.dp.toPx(), cap = StrokeCap.Round))
            drawPath(path, Primary.copy(alpha = glowAlpha), style = Stroke(strokeW, cap = StrokeCap.Round))
        }

        val lineY = (top + scanSize * scanLineProgress).coerceIn(top, bottom)
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, Primary.copy(0.35f), Primary, Primary.copy(0.35f), Color.Transparent),
                startX = left, endX = right
            ),
            start = Offset(left, lineY), end = Offset(right, lineY),
            strokeWidth = 2.5.dp.toPx()
        )
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, Primary.copy(0.08f), Primary.copy(0.18f), Primary.copy(0.08f), Color.Transparent),
                startX = left, endX = right
            ),
            start = Offset(left, lineY), end = Offset(right, lineY),
            strokeWidth = 14.dp.toPx()
        )
    }
}

private fun DrawScope.buildCornerPath(
    x: Float, y: Float,
    bracketLen: Float,
    cornerR: Float,
    isTop: Boolean,
    isLeft: Boolean
): Path {
    val path = Path()
    val r = cornerR.coerceAtMost(bracketLen * 0.8f)
    val hDir = if (isLeft) 1f else -1f
    val vDir = if (isTop) 1f else -1f
    path.moveTo(x + hDir * bracketLen, y)
    path.lineTo(x + hDir * r, y)
    path.quadraticBezierTo(x, y, x, y + vDir * r)
    path.lineTo(x, y + vDir * bracketLen)
    return path
}

@Composable
fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF12122A))
                .padding(horizontal = 40.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = Primary,
                modifier = Modifier.size(48.dp),
                strokeWidth = 3.dp
            )
            Text(
                "Verifying ticket...",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun TicketPreviewDialog(
    previewData: PreviewData,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(250)) + slideInVertically(tween(350, easing = FastOutSlowInEasing)) { it },
        exit = fadeOut() + slideOutVertically { it }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp))
                    .background(Color(0xFF12122A))
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 20.dp)
                        .size(width = 44.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Primary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ConfirmationNumber, null, tint = Primary, modifier = Modifier.size(26.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            "Confirm Check-In",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            "Review ticket details below",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TicketInfoRow("Ticket ID", previewData.ticket.id.take(8) + "…")
                    HorizontalDivider(color = Color.White.copy(alpha = 0.07f))
                    TicketInfoRow("Type", previewData.ticket.ticketTypeDisplayName())
                    HorizontalDivider(color = Color.White.copy(alpha = 0.07f))
                    TicketInfoRow("Event", previewData.event.name)
                    previewData.ticket.seatNumber?.let {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.07f))
                        TicketInfoRow("Seat", it)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.linearGradient(listOf(GradientStart, GradientMid))),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Confirm", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun TicketInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.5f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
fun SuccessOverlay(successData: SuccessData, onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val infiniteTransition = rememberInfiniteTransition(label = "successRings")
    val ring1Scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.9f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "r1s"
    )
    val ring1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.55f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "r1a"
    )
    val ring2Scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.9f,
        animationSpec = infiniteRepeatable(tween(1300, delayMillis = 650, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "r2s"
    )
    val ring2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.55f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1300, delayMillis = 650, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "r2a"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + scaleIn(tween(420, easing = FastOutSlowInEasing), initialScale = 0.88f),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF091A14).copy(alpha = 0.97f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(180.dp)) {
                        val c = Offset(size.width / 2f, size.height / 2f)
                        val base = 52.dp.toPx()
                        drawCircle(SuccessColor.copy(alpha = ring1Alpha * 0.45f), base * ring1Scale, c, style = Stroke(2.dp.toPx()))
                        drawCircle(SuccessColor.copy(alpha = ring2Alpha * 0.35f), base * ring2Scale, c, style = Stroke(2.dp.toPx()))
                    }
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(SuccessColor, TertiaryDark))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(54.dp))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    "Check-In Successful!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    successData.formattedCheckInTime(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.55f)
                )

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.height(52.dp).width(220.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(SuccessColor, TertiaryDark))),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan Next Ticket", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorOverlay(error: ScanError, onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val iconColor = when (error.getIcon()) {
        FeedbackIcon.WARNING -> WarningColor
        FeedbackIcon.EXPIRED -> ExpiredColor
        else -> ErrorColor
    }
    val icon = when (error.getIcon()) {
        FeedbackIcon.WARNING -> Icons.Default.Warning
        FeedbackIcon.EXPIRED -> Icons.Default.Schedule
        else -> Icons.Default.Cancel
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + scaleIn(tween(420, easing = FastOutSlowInEasing), initialScale = 0.88f),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A0808).copy(alpha = 0.97f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(CircleShape)
                            .background(iconColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = Color.White, modifier = Modifier.size(46.dp))
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    error.getDisplayMessage(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.height(52.dp).width(200.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = iconColor)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Try Again", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CameraPermissionDenied(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GradientStart, PrimaryDark))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(48.dp))
                }
            }

            Text(
                "Camera Access Required",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                "Please allow camera access to scan QR codes for event check-in",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.68f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onRequestPermission,
                modifier = Modifier.height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Icon(Icons.Default.CameraAlt, null, tint = GradientStart, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Allow Camera", color = GradientStart, fontWeight = FontWeight.Bold)
            }
        }
    }
}

val SuccessColor = Color(0xFF10B981)
val ErrorColor = Color(0xFFEF4444)
val WarningColor = Color(0xFFF59E0B)
val ExpiredColor = Color(0xFF6B7280)
