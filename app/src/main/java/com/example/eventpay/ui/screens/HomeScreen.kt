package com.example.eventpay.ui.screens

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.eventpay.domain.model.Event
import com.example.eventpay.domain.model.EventCategory
import com.example.eventpay.domain.model.User
import com.example.eventpay.domain.model.UserRole
import com.example.eventpay.ui.auth.AuthViewModel
import com.example.eventpay.ui.event.EventViewModel
import com.example.eventpay.ui.components.*
import com.example.eventpay.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentUser: User,
    eventViewModel: EventViewModel,
    authViewModel: AuthViewModel,
    onEventClick: (String) -> Unit,
    onCreateEvent: () -> Unit,
    onScanQR: () -> Unit,
    onWallet: () -> Unit,
    onCashier: () -> Unit,
    onDashboard: () -> Unit,
    onLogout: () -> Unit
) {
    val eventState by eventViewModel.eventState.collectAsState()
    var selectedCategory by remember { mutableStateOf<EventCategory?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        eventViewModel.loadEvents()
    }

    val filteredEvents = remember(eventState.events, searchQuery, selectedCategory) {
        eventState.events.filter { event ->
            val matchesSearch = searchQuery.isBlank() ||
                event.name.contains(searchQuery, ignoreCase = true) ||
                event.location.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == null || event.category == selectedCategory
            matchesSearch && matchesCategory
        }
    }

    val firstName = currentUser.fullName.split(" ").firstOrNull() ?: ""
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(GradientStart, GradientMid)
                            )
                        )
                        .padding(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 24.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
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
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .clickable { },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentUser.fullName.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = greeting,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = firstName,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = onWallet,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.15f))
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (currentUser.walletBalance > 0) {
                                                Badge(
                                                    containerColor = Accent
                                                ) { }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Outlined.AccountBalanceWallet,
                                            contentDescription = "Wallet",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Box {
                                    IconButton(
                                        onClick = { showMenu = true },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.15f))
                                    ) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = "Menu",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        if (currentUser.role.canAccessDashboard()) {
                                            DropdownMenuItem(
                                                text = { Text("Analytics Dashboard") },
                                                onClick = { showMenu = false; onDashboard() },
                                                leadingIcon = { Icon(Icons.Default.BarChart, null, tint = Primary) }
                                            )
                                        }
                                        if (currentUser.role.canSellTickets()) {
                                            DropdownMenuItem(
                                                text = { Text("Cashier Terminal") },
                                                onClick = { showMenu = false; onCashier() },
                                                leadingIcon = { Icon(Icons.Default.PointOfSale, null, tint = Tertiary) }
                                            )
                                        }
                                        HorizontalDivider()
                                        DropdownMenuItem(
                                            text = { Text("Sign Out", color = Error) },
                                            onClick = { showMenu = false; onLogout() },
                                            leadingIcon = { Icon(Icons.Default.Logout, null, tint = Error) }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            placeholder = {
                                Text(
                                    "Search events, venues...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                AnimatedVisibility(
                                    visible = searchQuery.isNotEmpty(),
                                    enter = fadeIn() + scaleIn(),
                                    exit = fadeOut() + scaleOut()
                                ) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            singleLine = true
                        )
                    }
                }
            },
            bottomBar = {
                HomeBottomBar(
                    currentUser = currentUser,
                    selectedIndex = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it },
                    onCreateEvent = onCreateEvent,
                    onScanQR = onScanQR
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    QuickActionsSection(
                        currentUser = currentUser,
                        walletBalance = currentUser.walletBalance,
                        onScanQR = onScanQR,
                        onWallet = onWallet,
                        onDashboard = onDashboard,
                        onCashier = onCashier
                    )
                }

                item {
                    CategoryFilterSection(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it }
                    )
                }

                item {
                    SectionHeader(
                        title = "Featured Events",
                        subtitle = "Trending near you",
                        actionText = "See All",
                        onActionClick = {}
                    )
                }

                when {
                    eventState.isLoading -> {
                        item { LoadingEventsSection() }
                    }
                    filteredEvents.isEmpty() -> {
                        item {
                            EmptyEventsState(
                                hasEvents = eventState.events.isNotEmpty(),
                                canCreate = currentUser.role.canManageEvents(),
                                onCreateEvent = onCreateEvent
                            )
                        }
                    }
                    else -> {
                        items(filteredEvents) { event ->
                            PremiumEventCard(
                                event = event,
                                onClick = { onEventClick(event.id) },
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class QuickAction(
    val icon: ImageVector,
    val label: String,
    val iconTint: Color,
    val containerColor: Color,
    val onClick: () -> Unit
)

@Composable
fun QuickActionsSection(
    currentUser: User,
    walletBalance: Double,
    onScanQR: () -> Unit,
    onWallet: () -> Unit,
    onDashboard: () -> Unit,
    onCashier: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(listOf(GradientStart, GradientMid, GradientEnd))
                )
                .clickable { onWallet() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "My Wallet",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = String.format("%.2f MAD", walletBalance),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.AddCard,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                "Top Up",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.AccountBalanceWallet,
                        contentDescription = "Wallet",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        val quickActions = buildList<QuickAction> {
            add(QuickAction(Icons.Default.QrCodeScanner, "Scan QR", Primary, PrimaryContainer, onScanQR))
            if (currentUser.role.canSellTickets()) {
                add(QuickAction(Icons.Default.PointOfSale, "Cashier", Tertiary, TertiaryContainer, onCashier))
            }
            if (currentUser.role.canAccessDashboard()) {
                add(QuickAction(Icons.Default.BarChart, "Analytics", Secondary, SecondaryContainer, onDashboard))
            }
        }

        if (quickActions.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                quickActions.forEach { action ->
                    QuickActionButton(
                        icon = action.icon,
                        label = action.label,
                        iconTint = action.iconTint,
                        containerColor = action.containerColor,
                        onClick = action.onClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = Primary,
    containerColor: Color = PrimaryContainer
) {
    Surface(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = iconTint.copy(alpha = 0.15f)
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun CategoryFilterSection(
    selectedCategory: EventCategory?,
    onCategorySelected: (EventCategory?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Browse by Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategorySelected(null) },
                    label = {
                        Text(
                            "All Events",
                            fontWeight = if (selectedCategory == null) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    leadingIcon = if (selectedCategory == null) {
                        { Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    )
                )
            }

            items(EventCategory.values().toList()) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(if (selectedCategory == category) null else category) },
                    label = {
                        Text(
                            category.name.lowercase().replaceFirstChar { it.uppercase() },
                            fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    leadingIcon = if (selectedCategory == category) {
                        { Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = getCategoryColor(category),
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (actionText != null && onActionClick != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = PrimaryContainer,
                onClick = onActionClick
            ) {
                Text(
                    text = actionText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PremiumEventCard(
    event: Event,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
    val now = System.currentTimeMillis()
    val daysUntil = ((event.date - now) / (1000 * 60 * 60 * 24)).toInt()
    val countdownLabel = when {
        daysUntil < 0 -> "Ended"
        daysUntil == 0 -> "Today!"
        daysUntil == 1 -> "Tomorrow"
        daysUntil <= 7 -> "In $daysUntil days"
        else -> dateFormat.format(Date(event.date))
    }
    val isUrgent = daysUntil in 0..3
    val soldFraction = if (event.totalTickets > 0) {
        (event.soldTickets.toFloat() / event.totalTickets.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val capacityColor = when {
        soldFraction >= 0.9f -> Error
        soldFraction >= 0.7f -> Warning
        else -> Success
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Primary.copy(alpha = 0.06f),
                spotColor = Primary.copy(alpha = 0.10f)
            ),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                AsyncImage(
                    model = event.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.1f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.55f)
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = getCategoryColor(event.category)
                    ) {
                        Text(
                            text = event.category.name,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    if (isUrgent) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Error
                        ) {
                            Text(
                                text = "🔥 $countdownLabel",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (event.ticketPrice == 0.0) TertiaryDark else Color.Black.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = if (event.ticketPrice == 0.0) "FREE" else "${event.ticketPrice.toInt()} MAD",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                if (!isUrgent) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.4f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                countdownLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(InfoContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Info,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = dateFormat.format(Date(event.date)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SuccessContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Success,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = event.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${event.availableTickets()} spots left",
                            style = MaterialTheme.typography.labelSmall,
                            color = capacityColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${(soldFraction * 100).toInt()}% filled",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = { soldFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = capacityColor,
                        trackColor = capacityColor.copy(alpha = 0.15f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (event.ticketPrice > 0) {
                        Column {
                            Text(
                                "From",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${event.ticketPrice.toInt()} MAD",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Primary
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TertiaryContainer
                        ) {
                            Text(
                                "Free Entry",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = Tertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PrimaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "View Details",
                                style = MaterialTheme.typography.labelMedium,
                                color = Primary,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingEventsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(2) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    ) {
                        ShimmerEffect(modifier = Modifier.fillMaxSize())
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ShimmerBox(modifier = Modifier.fillMaxWidth(0.8f).height(22.dp))
                        ShimmerBox(modifier = Modifier.fillMaxWidth(0.55f).height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ShimmerBox(modifier = Modifier.width(90.dp).height(14.dp))
                            ShimmerBox(modifier = Modifier.width(100.dp).height(14.dp))
                        }
                        ShimmerBox(modifier = Modifier.fillMaxWidth().height(5.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ShimmerBox(modifier = Modifier.width(70.dp).height(20.dp))
                            ShimmerBox(modifier = Modifier.width(110.dp).height(36.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
    ) {
        ShimmerEffect(modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun HomeBottomBar(
    currentUser: User,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    onCreateEvent: () -> Unit,
    onScanQR: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Primary.copy(alpha = 0.15f),
                spotColor = Primary.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = Icons.Default.Explore,
                label = "Explore",
                isSelected = selectedIndex == 0,
                onClick = { onTabSelected(0) }
            )

            BottomNavItem(
                icon = Icons.Outlined.ConfirmationNumber,
                label = "Tickets",
                isSelected = selectedIndex == 1,
                onClick = { onTabSelected(1) }
            )

            if (currentUser.role.canManageEvents()) {
                Box(contentAlignment = Alignment.Center) {
                    FloatingActionButton(
                        onClick = onCreateEvent,
                        containerColor = Primary,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(52.dp),
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 8.dp
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Create Event",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                BottomNavItem(
                    icon = Icons.Outlined.QrCodeScanner,
                    label = "Scan",
                    isSelected = selectedIndex == 2,
                    onClick = { onScanQR() }
                )
            }

            BottomNavItem(
                icon = Icons.Outlined.Person,
                label = "Profile",
                isSelected = selectedIndex == 3,
                onClick = { onTabSelected(3) }
            )
        }
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val itemColor by animateColorAsState(
        targetValue = if (isSelected) Primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        label = "navItemColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) PrimaryContainer else Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = itemColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = itemColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun EmptyEventsState(
    hasEvents: Boolean,
    canCreate: Boolean,
    onCreateEvent: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(PrimaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.EventBusy,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = Primary
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (hasEvents) "No Events Found" else "No Events Yet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (hasEvents)
                    "Try adjusting your search or category filters"
                else
                    "Be the first to create an amazing event",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        if (canCreate) {
            AnimatedGradientButton(
                onClick = onCreateEvent,
                text = "Create Your First Event",
                icon = Icons.Default.Add,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


