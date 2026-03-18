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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
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

    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = remember(currentHour) {
        when (currentHour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else -> "Welcome Back"
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "livePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(modifier = Modifier.fillMaxSize().background(BackgroundLight)) {
        Column(modifier = Modifier.fillMaxSize()) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(GradientStart, GradientMid, GradientEnd))
                    )
                    .padding(top = 12.dp, bottom = 28.dp, start = 20.dp, end = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f))
                                .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser.fullName
                                    .split(" ")
                                    .mapNotNull { it.firstOrNull()?.toString() }
                                    .take(2)
                                    .joinToString("")
                                    .uppercase(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4ADE80).copy(alpha = pulseAlpha))
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    "SCANNER",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF4ADE80).copy(alpha = pulseAlpha),
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.5.sp
                                )
                            }
                            Text(
                                greeting,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                currentUser.fullName.split(" ").firstOrNull() ?: currentUser.fullName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        ) {
                            Icon(Icons.Default.MoreVert, null, tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Logout",
                                        fontWeight = FontWeight.SemiBold,
                                        color = Error
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Logout, null, tint = Error)
                                },
                                onClick = {
                                    showMenu = false
                                    onLogout()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.13f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.QrCodeScanner,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Select an event below to start scanning tickets",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            when {
                isLoading -> {
                    LazyColumn(
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(3) { index ->
                            EventSelectionCardShimmer(index)
                        }
                    }
                }
                events.isEmpty() -> {
                    ScannerNoEventsState(onRefresh = onRefresh)
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 20.dp,
                            bottom = 32.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(Tertiary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.Event,
                                            null,
                                            tint = Tertiary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Assigned Events",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = OnBackgroundLight
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Primary.copy(alpha = 0.1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "${events.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Primary
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        IconButton(
                                            onClick = onRefresh,
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Refresh,
                                                null,
                                                tint = Primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        items(events, key = { it.id }) { event ->
                            var visible by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                kotlinx.coroutines.delay(60L)
                                visible = true
                            }
                            AnimatedVisibility(
                                visible = visible,
                                enter = fadeIn(tween(250)) + slideInVertically(
                                    initialOffsetY = { it / 4 },
                                    animationSpec = tween(300)
                                )
                            ) {
                                EventSelectionCard(
                                    event = event,
                                    onClick = { onSelectEvent(event) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventSelectionCard(event: Event, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()) }
    val checkInPct = if (event.totalTickets > 0)
        (event.checkedInCount.toFloat() / event.totalTickets).coerceIn(0f, 1f)
    else 0f

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "cardScale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Primary.copy(alpha = 0.1f)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceLight
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(listOf(Primary, PrimaryLight)),
                        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Primary.copy(alpha = 0.15f), PrimaryContainer)
                                )
                            )
                            .border(1.dp, Primary.copy(alpha = 0.2f), RoundedCornerShape(15.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Event,
                            null,
                            tint = Primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            event.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = OnSurfaceLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.LocationOn,
                                null,
                                tint = OnSurfaceVariantLight,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                event.location,
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariantLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Schedule,
                                null,
                                tint = OnSurfaceVariantLight,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                dateFormat.format(Date(event.date)),
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariantLight
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            null,
                            tint = Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                if (event.totalTickets > 0) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = DividerLight, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.HowToReg,
                                null,
                                tint = Tertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                "${event.checkedInCount} / ${event.totalTickets} checked in",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariantLight
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                "TAP TO SCAN",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Primary,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { checkInPct },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Tertiary,
                        trackColor = TertiaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun EventSelectionCardShimmer(index: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 80L)
        visible = true
    }
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(200))) {
        Surface(
            modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = SurfaceLight
        ) {
            Column {
                Box(
                    modifier = Modifier.fillMaxWidth().height(4.dp)
                        .background(ShimmerLight.copy(alpha = shimmerAlpha))
                )
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(15.dp))
                            .background(ShimmerLight.copy(alpha = shimmerAlpha))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(0.65f).height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(ShimmerLight.copy(alpha = shimmerAlpha))
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth(0.5f).height(11.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(ShimmerLight.copy(alpha = shimmerAlpha))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannerNoEventsState(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Primary.copy(alpha = 0.15f), PrimaryContainer)
                    )
                )
                .border(2.dp, Primary.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.EventBusy,
                null,
                tint = Primary,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "No events assigned",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = OnBackgroundLight
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            "Contact your admin to get assigned\nto an event before scanning",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariantLight,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onRefresh,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Refresh", fontWeight = FontWeight.Bold)
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

    val infiniteTransition = rememberInfiniteTransition(label = "counterPulse")
    val counterScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (uiState.scanStatus == ScanStatus.SUCCESS) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "counterScale"
    )

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

        ScannerVignetteOverlay(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 52.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { scannerViewModel.clearSelectedEvent() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.padding(end = 14.dp)) {
                        Text(
                            event.name,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            event.location,
                            color = Color.White.copy(alpha = 0.65f),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, Tertiary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .graphicsLayer(
                            scaleX = if (uiState.scanStatus == ScanStatus.SUCCESS) counterScale else 1f,
                            scaleY = if (uiState.scanStatus == ScanStatus.SUCCESS) counterScale else 1f
                        )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${uiState.sessionCheckInCount}",
                            color = Tertiary,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Scanned",
                            color = Color.White.copy(alpha = 0.65f),
                            style = MaterialTheme.typography.labelSmall
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
                PremiumScanFrame(
                    isSuccess = uiState.scanStatus == ScanStatus.SUCCESS,
                    isError = uiState.scanStatus == ScanStatus.INVALID ||
                        uiState.scanStatus == ScanStatus.NOT_FOUND ||
                        uiState.scanStatus == ScanStatus.ERROR
                )
            }

            AnimatedVisibility(
                visible = uiState.scanStatus != ScanStatus.IDLE,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(200)),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(200))
            ) {
                PremiumScanResultCard(uiState = uiState)
            }

            if (uiState.scanStatus == ScanStatus.IDLE) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 36.dp, start = 20.dp, end = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.QrCodeScanner,
                            null,
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Align QR code inside the frame",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumScanFrame(
    isSuccess: Boolean,
    isError: Boolean
) {
    val cornerColor = when {
        isSuccess -> Tertiary
        isError -> Error
        else -> QrScannerCorner
    }
    val glowColor = when {
        isSuccess -> Tertiary.copy(alpha = 0.35f)
        isError -> Error.copy(alpha = 0.35f)
        else -> Primary.copy(alpha = 0.2f)
    }

    val size = 256.dp
    val cornerLength = 36.dp
    val strokeWidth = 4.5.dp

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
    val frameScale by animateFloatAsState(
        targetValue = if (isSuccess) 1.04f else if (isError) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "frameScale"
    )

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer(scaleX = frameScale, scaleY = frameScale),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    1.dp,
                    glowColor,
                    RoundedCornerShape(4.dp)
                )
                .background(Color.Transparent)
        )

        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.toPx()
            val h = size.toPx()
            val cl = cornerLength.toPx()
            val sw = strokeWidth.toPx()
            val cap = StrokeCap.Round

            listOf(
                Offset(0f, 0f) to Offset(cl, 0f),
                Offset(0f, 0f) to Offset(0f, cl),
                Offset(w, 0f) to Offset(w - cl, 0f),
                Offset(w, 0f) to Offset(w, cl),
                Offset(0f, h) to Offset(cl, h),
                Offset(0f, h) to Offset(0f, h - cl),
                Offset(w, h) to Offset(w - cl, h),
                Offset(w, h) to Offset(w, h - cl)
            ).forEach { (start, end) ->
                drawLine(
                    color = cornerColor,
                    start = start,
                    end = end,
                    strokeWidth = sw,
                    cap = cap
                )
            }

            if (!isSuccess && !isError) {
                val lineY = h * lineOffset
                drawLine(
                    color = cornerColor.copy(alpha = 0.85f),
                    start = Offset(16f, lineY),
                    end = Offset(w - 16f, lineY),
                    strokeWidth = sw * 0.65f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun ScannerVignetteOverlay(modifier: Modifier) {
    Box(
        modifier = modifier.background(
            Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.55f)
                ),
                radius = 900f
            )
        )
    )
}

@Composable
private fun PremiumScanResultCard(uiState: com.example.eventpay.ui.scanner.ScannerUiState) {
    val (bgGradient, icon, title, subtitle) = when (uiState.scanStatus) {
        ScanStatus.SUCCESS -> PremiumResultStyle(
            gradient = listOf(Tertiary, TertiaryDark),
            icon = Icons.Default.CheckCircle,
            title = "Check-In Successful!",
            subtitle = uiState.lastScannedName ?: "Ticket verified and recorded"
        )
        ScanStatus.ALREADY_SCANNED -> PremiumResultStyle(
            gradient = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
            icon = Icons.Default.Warning,
            title = "Already Checked In",
            subtitle = uiState.lastScannedName ?: "This ticket was already used"
        )
        ScanStatus.INVALID -> PremiumResultStyle(
            gradient = listOf(Error, ErrorDark),
            icon = Icons.Default.Cancel,
            title = "Invalid Ticket",
            subtitle = uiState.error ?: "This ticket is not valid"
        )
        ScanStatus.NOT_FOUND -> PremiumResultStyle(
            gradient = listOf(Error, ErrorDark),
            icon = Icons.Default.SearchOff,
            title = "Ticket Not Found",
            subtitle = "No matching ticket found in the system"
        )
        ScanStatus.ERROR -> PremiumResultStyle(
            gradient = listOf(Error, ErrorDark),
            icon = Icons.Default.ErrorOutline,
            title = "Scan Error",
            subtitle = uiState.error ?: "An error occurred, please try again"
        )
        else -> PremiumResultStyle(
            gradient = listOf(Primary, PrimaryDark),
            icon = Icons.Default.QrCodeScanner,
            title = "Scanning…",
            subtitle = "Please wait"
        )
    }

    val isSuccess = uiState.scanStatus == ScanStatus.SUCCESS
    val infiniteTransition = rememberInfiniteTransition(label = "resultPulse")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isSuccess) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = bgGradient.first().copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(bgGradient))
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .graphicsLayer(scaleX = iconScale, scaleY = iconScale),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleSmall
                )
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        subtitle,
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (isSuccess) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Icon(
                        Icons.Default.Done,
                        null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(36.dp)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

private data class PremiumResultStyle(
    val gradient: List<Color>,
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
