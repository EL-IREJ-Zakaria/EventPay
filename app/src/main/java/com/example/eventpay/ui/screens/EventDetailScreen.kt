package com.example.eventpay.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.eventpay.domain.model.Event
import com.example.eventpay.domain.model.User
import com.example.eventpay.domain.model.UserRole
import com.example.eventpay.ui.event.EventViewModel
import com.example.eventpay.ui.components.*
import com.example.eventpay.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    event: Event,
    currentUser: User,
    checkedInCount: Int,
    eventViewModel: EventViewModel,
    onBack: () -> Unit,
    onPurchaseTicket: () -> Unit,
    onViewTickets: () -> Unit
) {
    val context = LocalContext.current
    var showShareDialog by remember { mutableStateOf(false) }

    fun buildShareText(): String {
        val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return buildString {
            append("🎉 *${event.name}*\n\n")
            append("📅 ${df.format(Date(event.date))} · ${event.startTime} - ${event.endTime}\n")
            append("📍 ${event.location}\n")
            if (event.description.isNotBlank()) append("\n${event.description}\n")
            if (event.ticketPrice > 0) append("\n💰 Ticket: ${String.format("%.2f", event.ticketPrice)} MAD")
            else append("\n✅ Free Entry")
            append("\n\n#EventPay #${event.name.replace(" ", "")}")
        }
    }

    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("Share Event", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        Triple("WhatsApp", "💬", Color(0xFF25D366)) to "com.whatsapp",
                        Triple("Facebook", "📘", Color(0xFF1877F2)) to "com.facebook.katana",
                        Triple("Instagram", "📸", Color(0xFFE1306C)) to "com.instagram.android",
                        Triple("Other Apps", "🔗", Primary) to null
                    ).forEach { (info, pkg) ->
                        val (label, emoji, color) = info
                        SharePlatformButton(label = label, emoji = emoji, color = color) {
                            showShareDialog = false
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, buildShareText())
                                if (pkg != null) setPackage(pkg)
                            }
                            runCatching { context.startActivity(intent) }.onFailure {
                                val fallback = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, buildShareText())
                                }
                                context.startActivity(Intent.createChooser(fallback, "Share via"))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showShareDialog = false }) { Text("Cancel") } },
            shape = RoundedCornerShape(24.dp)
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundLight)) {
        AsyncImage(
            model = event.imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(360.dp),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent, Color.Transparent, BackgroundLight),
                        startY = 0f, endY = 900f
                    )
                )
        )

        if (event.imageUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .background(Brush.linearGradient(listOf(GradientStart, GradientMid, AuroraBlue)))
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.35f))
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.35f))
                            .clickable { showShareDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Share, "Share", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            },
            bottomBar = {
                if (currentUser.role == UserRole.SCANNER) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .shadow(24.dp, RoundedCornerShape(24.dp), spotColor = Primary.copy(0.2f))
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Ticket Price",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Zinc400,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    if (event.ticketPrice > 0) "${String.format("%.2f", event.ticketPrice)} MAD" else "Free",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Primary
                                )
                            }
                            AnimatedGradientButton(
                                onClick = onPurchaseTicket,
                                text = "Get Ticket",
                                icon = Icons.Default.ConfirmationNumber,
                                modifier = Modifier.width(170.dp)
                            )
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item { Spacer(modifier = Modifier.height(210.dp)) }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                            .background(BackgroundLight)
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = PrimaryContainer,
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Category, null, tint = Primary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        event.category.name.lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Primary
                                    )
                                }
                            }

                            if (event.ticketPrice <= 0) {
                                Surface(
                                    color = TertiaryContainer,
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text(
                                        "FREE",
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Tertiary,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            event.name,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = OnBackgroundLight
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PremiumInfoBadge(
                                icon = Icons.Default.CalendarToday,
                                title = "Date",
                                subtitle = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(event.date)),
                                color = Primary,
                                modifier = Modifier.weight(1f)
                            )
                            PremiumInfoBadge(
                                icon = Icons.Default.AccessTime,
                                title = "Time",
                                subtitle = "${event.startTime} – ${event.endTime}",
                                color = Secondary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        PremiumInfoBadge(
                            icon = Icons.Default.LocationOn,
                            title = "Location",
                            subtitle = event.location,
                            color = Rose,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (event.totalTickets > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val pct = (checkedInCount.toFloat() / event.totalTickets).coerceIn(0f, 1f)
                            val animPct by animateFloatAsState(targetValue = pct, animationSpec = tween(1200, easing = FastOutSlowInEasing), label = "checkIn")
                            Surface(
                                color = TertiaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Attendance", style = MaterialTheme.typography.labelMedium, color = Zinc500, fontWeight = FontWeight.SemiBold)
                                        Text("$checkedInCount / ${event.totalTickets}", style = MaterialTheme.typography.labelMedium, color = Tertiary, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Zinc200)) {
                                        Box(modifier = Modifier.fillMaxWidth(animPct).fillMaxHeight().clip(CircleShape).background(Brush.horizontalGradient(listOf(Tertiary, TertiaryLight))))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Text("About this Event", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            event.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Zinc500,
                            lineHeight = 26.sp
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Text("Organizer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            color = Zinc50,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(AuroraViolet, AuroraCyan))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(26.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Event Organizer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Verified, null, tint = Tertiary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Verified Professional", style = MaterialTheme.typography.bodySmall, color = Tertiary)
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, null, tint = Zinc300)
                            }
                        }

                        if (currentUser.role == UserRole.ADMIN) {
                            Spacer(modifier = Modifier.height(20.dp))
                            SaaSButton(
                                onClick = onViewTickets,
                                text = "View All Tickets",
                                icon = Icons.Outlined.ConfirmationNumber,
                                containerColor = Zinc900,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumInfoBadge(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color = Primary,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.07f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun InfoBadge(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    PremiumInfoBadge(icon = icon, title = title, subtitle = subtitle, modifier = modifier)
}

@Composable
private fun SharePlatformButton(
    label: String,
    emoji: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 22.sp)
            Spacer(modifier = Modifier.width(14.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = color)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowForwardIos, null, tint = color.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
        }
    }
}
