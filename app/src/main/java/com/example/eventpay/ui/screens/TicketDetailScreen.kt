package com.example.eventpay.ui.screens

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eventpay.domain.model.Event
import com.example.eventpay.domain.model.Ticket
import com.example.eventpay.domain.model.TicketStatus
import com.example.eventpay.domain.model.TicketType
import com.example.eventpay.ui.theme.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDetailScreen(
    ticket: Ticket,
    event: Event,
    onBack: () -> Unit
) {
    val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    val qrBitmap = remember(ticket.qrCode) {
        generateQrBitmap(ticket.qrValidationKey(), 512)
    }

    val typeColor = Color(ticket.ticketType.badgeColor())
    val isCheckedIn = ticket.isCheckedIn()
    val isCancelled = ticket.status == TicketStatus.CANCELLED

    val statusColor = when {
        isCancelled -> Error
        isCheckedIn -> Tertiary
        else -> Primary
    }
    val statusLabel = when {
        isCancelled -> "Cancelled"
        isCheckedIn -> "Used"
        else -> "Valid"
    }
    val statusIcon = when {
        isCancelled -> Icons.Default.Cancel
        isCheckedIn -> Icons.Default.CheckCircle
        else -> Icons.Default.ConfirmationNumber
    }

    var isEntered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isEntered = true }
    val cardSlide by animateFloatAsState(
        targetValue = if (isEntered) 0f else 700f,
        animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow),
        label = "slide"
    )
    val fadeIn by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "fade"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "ticketBg")
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "shimmer"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        typeColor.copy(alpha = 0.18f),
                        BackgroundLight.copy(alpha = 0.95f),
                        BackgroundLight
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Zinc100),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = Zinc700, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        "My Ticket",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = OnSurfaceLight
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { translationY = cardSlide; alpha = fadeIn }
                        .shadow(
                            elevation = 28.dp,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = typeColor.copy(alpha = 0.25f),
                            spotColor = typeColor.copy(alpha = 0.4f)
                        ),
                    shape = RoundedCornerShape(28.dp),
                    color = SurfaceLight
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(listOf(typeColor, typeColor.copy(alpha = 0.75f))),
                                    RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                                )
                                .padding(horizontal = 24.dp, vertical = 28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White.copy(alpha = 0.22f)
                                ) {
                                    Text(
                                        ticket.ticketTypeDisplayName().uppercase(),
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        letterSpacing = 2.5.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    event.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Outlined.CalendarToday, null, tint = Color.White.copy(0.85f), modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        "${dateFormat.format(Date(event.date))} · ${event.startTime}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(0.85f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                    Icon(Icons.Outlined.LocationOn, null, tint = Color.White.copy(0.85f), modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(event.location, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f))
                                }
                            }
                        }

                        TicketPerforation(color = typeColor.copy(alpha = 0.18f))

                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = statusColor.copy(alpha = 0.09f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(16.dp))
                                    Text(
                                        statusLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = statusColor,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            if (qrBitmap != null && !isCancelled) {
                                Box(
                                    modifier = Modifier
                                        .shadow(12.dp, RoundedCornerShape(24.dp))
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(Color.White)
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = qrBitmap.asImageBitmap(),
                                        contentDescription = "QR Code",
                                        modifier = Modifier.size(200.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                    if (isCheckedIn) {
                                        Box(
                                            modifier = Modifier
                                                .size(200.dp)
                                                .background(Color.White.copy(alpha = 0.82f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.CheckCircle, null, tint = Tertiary, modifier = Modifier.size(80.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    if (isCheckedIn) "Ticket already scanned" else "Scan this QR at entry",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Zinc400,
                                    textAlign = TextAlign.Center
                                )
                            } else if (isCancelled) {
                                Box(
                                    modifier = Modifier
                                        .size(220.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(Error.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Cancel, null, tint = Error, modifier = Modifier.size(72.dp))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("CANCELLED", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Error, letterSpacing = 2.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            HorizontalDivider(color = DividerLight, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TicketInfoItem("Ticket ID", ticket.id.take(8).uppercase())
                                TicketInfoItem("Type", ticket.ticketTypeDisplayName())
                                TicketInfoItem(
                                    "Price",
                                    if (event.ticketPrice > 0) "${String.format("%.0f", event.ticketPrice)} MAD" else "Free"
                                )
                            }

                            if (isCheckedIn && ticket.checkedInAt != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Tertiary.copy(alpha = 0.08f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.size(34.dp).clip(CircleShape).background(Tertiary.copy(0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.EventAvailable, null, tint = Tertiary, modifier = Modifier.size(18.dp))
                                        }
                                        Column {
                                            Text("Checked In", style = MaterialTheme.typography.labelSmall, color = Zinc400)
                                            Text(
                                                "${dateFormat.format(Date(ticket.checkedInAt))} at ${timeFormat.format(Date(ticket.checkedInAt))}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Tertiary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    "Ticket #${ticket.id.take(12).uppercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Zinc400,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer { alpha = fadeIn }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun TicketPerforation(color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(28.dp).offset(x = (-14).dp).background(BackgroundLight, CircleShape))
        Box(modifier = Modifier.weight(1f).height(1.dp).background(color, RoundedCornerShape(1.dp)))
        Box(modifier = Modifier.size(28.dp).offset(x = 14.dp).background(BackgroundLight, CircleShape))
    }
}

@Composable
private fun TicketInfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Zinc400)
        Spacer(modifier = Modifier.height(3.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = OnSurfaceLight)
    }
}

private fun generateQrBitmap(content: String, size: Int): Bitmap? {
    return try {
        val hints = mapOf(EncodeHintType.MARGIN to 1, EncodeHintType.CHARACTER_SET to "UTF-8")
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}
