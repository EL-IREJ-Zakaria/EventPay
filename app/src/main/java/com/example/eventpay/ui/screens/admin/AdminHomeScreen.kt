package com.example.eventpay.ui.screens.admin

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eventpay.data.model.User
import com.example.eventpay.domain.model.Event
import com.example.eventpay.domain.model.EventStatus
import com.example.eventpay.ui.admin.AdminViewModel
import com.example.eventpay.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    currentUser: User,
    adminViewModel: AdminViewModel,
    onNavigateToEvents: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onLogout: () -> Unit
) {
    val uiState by adminViewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        adminViewModel.loadDashboard()
        adminViewModel.loadEvents()
    }

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
                        .background(Brush.verticalGradient(listOf(GradientStart, GradientMid)))
                        .padding(top = 52.dp, start = 20.dp, end = 20.dp, bottom = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Admin Panel",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.7f),
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                "Welcome, ${currentUser.fullName.split(" ").firstOrNull() ?: "Admin"}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box {
                                IconButton(
                                    onClick = { showMenu = true },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.15f))
                                ) {
                                    Icon(Icons.Default.MoreVert, null, tint = Color.White)
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Logout") },
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
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    uiState.successMessage?.let { msg ->
                        AnimatedVisibility(
                            visible = true,
                            enter = slideInVertically() + fadeIn(),
                            exit = slideOutVertically() + fadeOut()
                        ) {
                            Surface(
                                color = Tertiary,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(msg, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnBackgroundLight,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            AdminStatCard(
                                title = "Events",
                                value = uiState.stats.totalEvents.toString(),
                                icon = Icons.Outlined.Event,
                                color = Primary,
                                isLoading = uiState.isLoading
                            )
                        }
                        item {
                            AdminStatCard(
                                title = "Tickets",
                                value = uiState.stats.totalTickets.toString(),
                                icon = Icons.Outlined.ConfirmationNumber,
                                color = Secondary,
                                isLoading = uiState.isLoading
                            )
                        }
                        item {
                            AdminStatCard(
                                title = "Check-Ins",
                                value = uiState.stats.totalCheckIns.toString(),
                                icon = Icons.Outlined.HowToReg,
                                color = Tertiary,
                                isLoading = uiState.isLoading
                            )
                        }
                        item {
                            AdminStatCard(
                                title = "Scanners",
                                value = uiState.stats.totalScanners.toString(),
                                icon = Icons.Outlined.QrCodeScanner,
                                color = Color(0xFF9B59B6),
                                isLoading = uiState.isLoading
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        "Quick Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnBackgroundLight,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AdminActionCard(
                            icon = Icons.Filled.Event,
                            title = "Manage Events",
                            subtitle = "Create, edit and delete events",
                            gradient = listOf(GradientStart, GradientMid),
                            onClick = onNavigateToEvents
                        )
                        AdminActionCard(
                            icon = Icons.Filled.Group,
                            title = "Manage Scanner Staff",
                            subtitle = "Create and manage scanner accounts",
                            gradient = listOf(Color(0xFF9B59B6), Color(0xFF6C3483)),
                            onClick = onNavigateToUsers
                        )
                        AdminActionCard(
                            icon = Icons.Filled.QrCodeScanner,
                            title = "Scan QR Code",
                            subtitle = "Admin can also scan tickets directly",
                            gradient = listOf(Tertiary, TertiaryDark),
                            onClick = onNavigateToScanner
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(28.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Recent Events",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnBackgroundLight
                        )
                        TextButton(onClick = onNavigateToEvents) {
                            Text("See All", color = Primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (uiState.isLoading && uiState.events.isEmpty()) {
                    items(3) {
                        EventCardSkeleton()
                    }
                } else if (uiState.events.isEmpty()) {
                    item {
                        EmptyEventsPlaceholder(onCreateEvent = onNavigateToEvents)
                    }
                } else {
                    items(uiState.events.take(5)) { event ->
                        AdminEventPreviewCard(
                            event = event,
                            onClick = onNavigateToEvents
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    isLoading: Boolean
) {
    Surface(
        modifier = Modifier
            .width(130.dp)
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceLight
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(ShimmerLight)
                )
            } else {
                Text(
                    value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
            }
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariantLight,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AdminActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(gradient))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun AdminEventPreviewCard(
    event: Event,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val statusColor = when (event.status) {
        EventStatus.PUBLISHED -> Tertiary
        EventStatus.ONGOING -> Secondary
        EventStatus.CANCELLED -> Error
        EventStatus.COMPLETED -> Primary
        else -> OnSurfaceVariantLight
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceLight
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Event, null, tint = Primary, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    event.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurfaceLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    dateFormat.format(Date(event.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariantLight
                )
                Text(
                    event.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariantLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        event.status.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${event.checkedInCount}/${event.totalTickets}",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariantLight
                )
            }
        }
    }
}

@Composable
private fun EventCardSkeleton() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceLight
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ShimmerLight)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(ShimmerLight)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .height(11.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(ShimmerLight)
                )
            }
        }
    }
}

@Composable
private fun EmptyEventsPlaceholder(onCreateEvent: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.Event,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = OutlineLight
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No events yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = OnSurfaceVariantLight
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Create your first event to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariantLight
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onCreateEvent,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Event")
        }
    }
}
