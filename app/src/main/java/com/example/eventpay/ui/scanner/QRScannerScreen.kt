package com.example.eventpay.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.eventpay.ui.theme.*
import com.example.eventpay.ui.components.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

// Data classes
data class QRPreviewData(
    val visitorName: String,
    val ticketType: String,
    val eventName: String,
    val ticketId: String
)

@Composable
fun QRScannerScreen(
    eventId: String = "",
    deviceId: String = "",
    userId: String = "",
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var scanningState by remember { mutableStateOf(com.example.eventpay.ui.scanner.ScanningState.SCANNING) }
    var isFlashlightOn by remember { mutableStateOf(false) }
    var successData by remember { mutableStateOf<QRPreviewData?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var previewData by remember { mutableStateOf<QRPreviewData?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }
    
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            CameraPreview(
                lifecycleOwner = lifecycleOwner,
                cameraExecutor = cameraExecutor,
                isFlashlightOn = isFlashlightOn,
                onQRCodeDetected = { qrCode ->
                    // Simulate QR processing
                    previewData = QRPreviewData(
                        visitorName = "John Doe",
                        ticketType = "VIP",
                        eventName = "Summer Festival",
                        ticketId = qrCode
                    )
                    scanningState = com.example.eventpay.ui.scanner.ScanningState.PREVIEW
                },
                isScanningEnabled = scanningState == com.example.eventpay.ui.scanner.ScanningState.SCANNING
            )
        }

        // Scanning overlay
        ScanningOverlay(
            scanningState = scanningState,
            isFlashlightOn = isFlashlightOn,
            onToggleFlashlight = { isFlashlightOn = !isFlashlightOn },
            onBack = onBack
        )
        
        // Success/Error/Preview Components
        AnimatedVisibility(
            visible = scanningState == com.example.eventpay.ui.scanner.ScanningState.SUCCESS,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            successData?.let { 
                StatusOverlay(
                    title = "Check-in Successful",
                    subtitle = "Welcome, ${it.visitorName}!",
                    icon = Icons.Default.CheckCircle,
                    color = Success,
                    onDismiss = { 
                        scanningState = com.example.eventpay.ui.scanner.ScanningState.SCANNING
                        successData = null
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = scanningState == com.example.eventpay.ui.scanner.ScanningState.ERROR,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            errorMessage?.let {
                StatusOverlay(
                    title = "Invalid Ticket",
                    subtitle = it,
                    icon = Icons.Default.Error,
                    color = Error,
                    onDismiss = { 
                        scanningState = com.example.eventpay.ui.scanner.ScanningState.SCANNING
                        errorMessage = null
                    }
                )
            }
        }

        if (scanningState == com.example.eventpay.ui.scanner.ScanningState.PREVIEW && previewData != null) {
            TicketPreviewDialog(
                previewData = previewData!!,
                onConfirm = { 
                    isLoading = true
                    // Simulate API call
                    successData = previewData
                    scanningState = com.example.eventpay.ui.scanner.ScanningState.SUCCESS
                    isLoading = false
                },
                onCancel = { 
                    scanningState = com.example.eventpay.ui.scanner.ScanningState.SCANNING
                    previewData = null
                }
            )
        }
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

@Composable
private fun StatusOverlay(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        SaaSCard(
            modifier = Modifier.width(300.dp),
            containerColor = Color.White,
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = color.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = color, modifier = Modifier.size(48.dp))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Slate900
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                SaaSButton(
                    onClick = onDismiss,
                    text = "Continue Scanning",
                    containerColor = color,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ScanningOverlay(
    scanningState: com.example.eventpay.ui.scanner.ScanningState,
    isFlashlightOn: Boolean,
    onToggleFlashlight: () -> Unit,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (scanningState == com.example.eventpay.ui.scanner.ScanningState.SCANNING) {
            ScannerVignetteOverlay()
            ScanFrame()
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "Scanner",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = onToggleFlashlight,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isFlashlightOn) Primary else Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    if (isFlashlightOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    null,
                    tint = Color.White
                )
            }
        }

        if (scanningState == com.example.eventpay.ui.scanner.ScanningState.SCANNING) {
            Text(
                "Align ticket QR code within the frame",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
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
        val scanSize = 260.dp.toPx()
        val offset = Offset((size.width - scanSize) / 2, (size.height - scanSize) / 2)
        
        drawRect(color = Color.Black.copy(alpha = 0.6f))
        drawRoundRect(
            color = Color.Transparent,
            topLeft = offset,
            size = Size(scanSize, scanSize),
            cornerRadius = CornerRadius(24.dp.toPx()),
            blendMode = BlendMode.Clear
        )
    }
}

@Composable
private fun ScanFrame() {
    val infiniteTransition = rememberInfiniteTransition(label = "scanLine")
    val lineOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "lineOffset"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val scanSize = 260.dp.toPx()
        val topLeft = Offset((size.width - scanSize) / 2, (size.height - scanSize) / 2)
        
        val cornerLen = 40.dp.toPx()
        val strokeWidth = 4.dp.toPx()
        
        val corners = listOf(
            topLeft to Offset(topLeft.x + cornerLen, topLeft.y),
            topLeft to Offset(topLeft.x, topLeft.y + cornerLen),
            Offset(topLeft.x + scanSize, topLeft.y) to Offset(topLeft.x + scanSize - cornerLen, topLeft.y),
            Offset(topLeft.x + scanSize, topLeft.y) to Offset(topLeft.x + scanSize, topLeft.y + cornerLen),
            Offset(topLeft.x, topLeft.y + scanSize) to Offset(topLeft.x + cornerLen, topLeft.y + scanSize),
            Offset(topLeft.x, topLeft.y + scanSize) to Offset(topLeft.x, topLeft.y + scanSize - cornerLen),
            Offset(topLeft.x + scanSize, topLeft.y + scanSize) to Offset(topLeft.x + scanSize - cornerLen, topLeft.y + scanSize),
            Offset(topLeft.x + scanSize, topLeft.y + scanSize) to Offset(topLeft.x + scanSize, topLeft.y + scanSize - cornerLen)
        )
        
        corners.forEach { (start, end) ->
            drawLine(
                color = Primary,
                start = start,
                end = end,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
        
        val lineY = topLeft.y + (scanSize * lineOffset)
        drawLine(
            brush = Brush.horizontalGradient(listOf(Color.Transparent, Primary, Color.Transparent)),
            start = Offset(topLeft.x + 10.dp.toPx(), lineY),
            end = Offset(topLeft.x + scanSize - 10.dp.toPx(), lineY),
            strokeWidth = 2.dp.toPx()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketPreviewDialog(
    previewData: QRPreviewData,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        modifier = Modifier.clip(RoundedCornerShape(28.dp)),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        content = {
            SaaSCard(
                modifier = Modifier.fillMaxWidth(0.9f),
                containerColor = Color.White,
                elevation = 0.dp
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Primary.copy(alpha = 0.1f)
                        ) {
                            Icon(
                                Icons.Default.ConfirmationNumber,
                                null,
                                tint = Primary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Confirm Check-in",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                "Review ticket details",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    SaaSCard(
                        containerColor = Slate50,
                        elevation = 0.dp,
                        padding = 16.dp
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            InfoRow("Visitor", previewData.visitorName)
                            InfoRow("Ticket Type", previewData.ticketType)
                            InfoRow("Event", previewData.eventName)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Slate200)
                        ) {
                            Text("Cancel", color = Slate600, fontWeight = FontWeight.Bold)
                        }
                        SaaSButton(
                            onClick = onConfirm,
                            text = "Confirm",
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Slate500)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Slate900
        )
    }
}

@Composable
fun CameraPreview(
    lifecycleOwner: LifecycleOwner,
    cameraExecutor: ExecutorService,
    isFlashlightOn: Boolean,
    onQRCodeDetected: (String) -> Unit,
    isScanningEnabled: Boolean
) {
    val context = LocalContext.current
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, QRCodeAnalyzer { qrCode ->
                            if (isScanningEnabled) {
                                onQRCodeDetected(qrCode)
                            }
                        })
                    }
                
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    
                    camera.cameraControl.enableTorch(isFlashlightOn)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))
            
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

private class QRCodeAnalyzer(
    private val onQRCodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {
    private val reader = MultiFormatReader()
    
    override fun analyze(image: ImageProxy) {
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
            val result = reader.decode(binaryBitmap)
            onQRCodeDetected(result.text)
        } catch (e: Exception) {
            // No QR code found
        } finally {
            image.close()
        }
    }
}
