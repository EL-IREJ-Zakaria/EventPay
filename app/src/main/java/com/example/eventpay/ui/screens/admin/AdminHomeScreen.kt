package com.example.eventpay.ui.screens.admin

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.graphicsLayer
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
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onLogout: () -> Unit
) {
    val uiState by adminViewModel.uiState.collectAsState()
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
                AdminTopBar(
                    currentUser = currentUser,
                    greeting = greeting,
                    showMenu = showMenu,
                    onMenuToggle = { showMenu = it },
                    onLogout = onLogout
                )
            },
            bottomBar = {
                AdminBottomBar(
                    selectedDestination = AdminBottomDestination.Home,
                    onHomeClick = { },
                    onScannersClick = onNavigateToUsers
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                item {
                    AnimatedVisibility(
                        visible = uiState.successMessage != null,
                        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                    ) {
                        uiState.successMessage?.let { msg ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.horizontalGradient(listOf(Tertiary, TertiaryDark))
                                    )
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    msg,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                item {
                    AdminStatsSection(
                        totalEvents = uiState.stats.totalEvents,
                        totalTickets = uiState.stats.totalTickets,
                        totalCheckIns = uiState.stats.totalCheckIns,
                        totalScanners = uiState.stats.totalScanners,
                        isLoading = uiState.isLoading
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeader(
                        title = "Quick Actions",
                        icon = Icons.Outlined.Bolt,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PremiumActionCard(
                            icon = Icons.Filled.Event,
                            title = "Manage Events",
                            subtitle = "Create, edit and publish events",
                            gradient = listOf(GradientStart, GradientMid),
                            accentColor = PrimaryLight,
                            onClick = onNavigateToEvents
                        )
                        PremiumActionCard(
                            icon = Icons.Filled.Group,
                            title = "Scanner Staff",
                            subtitle = "Create and manage scanner accounts",
                            gradient = listOf(Color(0xFF7B2FBE), Color(0xFF5A1E8A)),
                            accentColor = Color(0xFFBB8EF8),
                            onClick = onNavigateToUsers
                        )
                        PremiumActionCard(
                            icon = Icons.Filled.QrCodeScanner,
                            title = "Scan Tickets",
                            subtitle = "Admin can validate tickets directly",
                            gradient = listOf(Tertiary, TertiaryDark),
                            accentColor = TertiaryLight,
                            onClick = onNavigateToScanner
                        )
                        PremiumActionCard(
                            icon = Icons.Filled.BarChart,
                            title = "Analytics",
                            subtitle = "Revenue, tickets and check-in stats",
                            gradient = listOf(Color(0xFF0F766E), Color(0xFF0D9488)),
                            accentColor = Color(0xFF5EEAD4),
                            onClick = onNavigateToAnalytics
                        )
                        PremiumActionCard(
                            icon = Icons.Filled.Dashboard,
                            title = "Dashboard",
                            subtitle = "Live revenue and transaction overview",
                            gradient = listOf(Color(0xFF1D4ED8), Color(0xFF2563EB)),
                            accentColor = Color(0xFF93C5FD),
                            onClick = onNavigateToDashboard
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
                        SectionHeader(
                            title = "Recent Events",
                            icon = Icons.Outlined.Schedule
                        )
                        TextButton(onClick = onNavigateToEvents) {
                            Text(
                                "See All",
                                color = Primary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                Icons.Default.ChevronRight,
                                null,
                                tint = Primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (uiState.isLoading && uiState.events.isEmpty()) {
                    items(3) { index ->
                        EventCardSkeleton(index)
                    }
                } else if (uiState.events.isEmpty()) {
                    item {
                        EmptyEventsPlaceholder(onCreateEvent = onNavigateToEvents)
                    }
                } else {
                    items(uiState.events.take(5), key = { it.id }) { event ->
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(80L)
                            visible = true
                        }
                        AnimatedVisibility(
                            visible = visible,
                            enter = slideInHorizontally(initialOffsetX = { it / 3 }) + fadeIn(
                                animationSpec = tween(300)
                            )
                        ) {
                            AdminEventPreviewCard(
                                event = event,
                                onClick = onNavigateToEvents
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    AdminSystemStatusBar(
                        activeScanners = uiState.stats.totalScanners,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminTopBar(
    currentUser: User,
    greeting: String,
    showMenu: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(GradientStart, GradientMid, GradientEnd))
            )
            .padding(top = 52.dp, start = 20.dp, end = 20.dp, bottom = 28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
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
                            "LIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4ADE80).copy(alpha = pulseAlpha),
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        greeting,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        currentUser.fullName.split(" ").firstOrNull() ?: "Admin",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { onMenuToggle(true) },
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
                    onDismissRequest = { onMenuToggle(false) }
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
                            onMenuToggle(false)
                            onLogout()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminStatsSection(
    totalEvents: Int,
    totalTickets: Int,
    totalCheckIns: Int,
    totalScanners: Int,
    isLoading: Boolean
) {
    Spacer(modifier = Modifier.height(20.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionHeader(
            title = "Overview",
            icon = Icons.Outlined.BarChart
        )
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = Primary.copy(alpha = 0.5f)
            )
        }
    }
    Spacer(modifier = Modifier.height(14.dp))

    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AdminStatCard(
                title = "Total Events",
                value = totalEvents.toString(),
                icon = Icons.Outlined.Event,
                color = Primary,
                bgGradient = listOf(Primary.copy(alpha = 0.1f), PrimaryContainer),
                isLoading = isLoading,
                modifier = Modifier.weight(1f)
            )
            AdminStatCard(
                title = "Tickets Issued",
                value = totalTickets.toString(),
                icon = Icons.Outlined.ConfirmationNumber,
                color = Secondary,
                bgGradient = listOf(Secondary.copy(alpha = 0.1f), SecondaryContainer),
                isLoading = isLoading,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AdminStatCard(
                title = "Check-Ins",
                value = totalCheckIns.toString(),
                icon = Icons.Outlined.HowToReg,
                color = Tertiary,
                bgGradient = listOf(Tertiary.copy(alpha = 0.1f), TertiaryContainer),
                isLoading = isLoading,
                modifier = Modifier.weight(1f)
            )
            AdminStatCard(
                title = "Active Scanners",
                value = totalScanners.toString(),
                icon = Icons.Outlined.QrCodeScanner,
                color = Color(0xFF7B2FBE),
                bgGradient = listOf(Color(0xFF7B2FBE).copy(alpha = 0.1f), Color(0xFFF3E8FF)),
                isLoading = isLoading,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AdminStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    bgGradient: List<Color>,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var animatedValue by remember { mutableIntStateOf(0) }
    val targetValue = value.toIntOrNull() ?: 0

    LaunchedEffect(targetValue) {
        if (targetValue > 0) {
            val steps = minOf(targetValue, 20)
            val step = targetValue / steps
            for (i in 1..steps) {
                animatedValue = minOf(i * step, targetValue)
                kotlinx.coroutines.delay(30L)
            }
            animatedValue = targetValue
        }
    }

    Surface(
        modifier = modifier.shadow(
            elevation = 8.dp,
            shape = RoundedCornerShape(20.dp),
            ambientColor = color.copy(alpha = 0.15f),
            spotColor = color.copy(alpha = 0.25f)
        ),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceLight
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(bgGradient),
                    RoundedCornerShape(20.dp)
                )
                .padding(16.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(color.copy(alpha = 0.15f))
                        .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(ShimmerLight)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(ShimmerLight)
                    )
                } else {
                    Text(
                        text = if (targetValue > 0) animatedValue.toString() else "0",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = color
                    )
                    Text(
                        title,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariantLight,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Primary, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = OnBackgroundLight
        )
    }
}

@Composable
private fun PremiumActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    gradient: List<Color>,
    accentColor: Color,
    onClick: () -> Unit
) {
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
                elevation = 12.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = gradient.first().copy(alpha = 0.4f)
            )
            .clickable(
                onClick = onClick,
                onClickLabel = title
            ),
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradient))
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.CenterEnd)
                    .graphicsLayer(alpha = 0.08f)
                    .clip(CircleShape)
                    .background(Color.White)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(17.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
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
    val checkInPct = if (event.totalTickets > 0)
        (event.checkedInCount.toFloat() / event.totalTickets).coerceIn(0f, 1f)
    else 0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color.Black.copy(alpha = 0.06f)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceLight
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Primary.copy(alpha = 0.15f), PrimaryContainer)
                            )
                        )
                        .border(1.dp, Primary.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Event,
                        null,
                        tint = Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        event.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.CalendarToday,
                            null,
                            modifier = Modifier.size(12.dp),
                            tint = OnSurfaceVariantLight
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            dateFormat.format(Date(event.date)),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariantLight
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Outlined.LocationOn,
                            null,
                            modifier = Modifier.size(12.dp),
                            tint = OnSurfaceVariantLight
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            event.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariantLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        event.status.name,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            if (event.totalTickets > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${event.checkedInCount} / ${event.totalTickets} checked in",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariantLight
                    )
                    Text(
                        "${(checkInPct * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Tertiary
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                LinearProgressIndicator(
                    progress = { checkInPct },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Tertiary,
                    trackColor = TertiaryContainer
                )
            }
        }
    }
}

@Composable
private fun EventCardSkeleton(index: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 80L)
        visible = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200))
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 5.dp)
                .shadow(4.dp, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            color = SurfaceLight
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ShimmerLight.copy(alpha = shimmerAlpha))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(ShimmerLight.copy(alpha = shimmerAlpha))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.42f)
                            .height(11.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(ShimmerLight.copy(alpha = shimmerAlpha))
                    )
                }
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(22.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ShimmerLight.copy(alpha = shimmerAlpha))
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
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = PrimaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.Event,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = Primary
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No events yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = OnBackgroundLight
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Create your first event to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariantLight
        )
        Spacer(modifier = Modifier.height(22.dp))
        Button(
            onClick = onCreateEvent,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Event", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AdminSystemStatusBar(
    activeScanners: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceLight
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusIndicator(
                label = "System",
                status = "Online",
                color = Tertiary
            )
            VerticalDivider(
                modifier = Modifier.height(32.dp),
                color = DividerLight
            )
            StatusIndicator(
                label = "Scanners",
                status = "$activeScanners Active",
                color = Primary
            )
            VerticalDivider(
                modifier = Modifier.height(32.dp),
                color = DividerLight
            )
            StatusIndicator(
                label = "Sync",
                status = "Real-time",
                color = Secondary
            )
        }
    }
}

@Composable
private fun StatusIndicator(
    label: String,
    status: String,
    color: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "status")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "statusPulse"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = pulseAlpha))
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                status,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariantLight
        )
    }
}
