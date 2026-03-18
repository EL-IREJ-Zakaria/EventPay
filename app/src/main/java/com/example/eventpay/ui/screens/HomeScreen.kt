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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var searchQuery by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) { eventViewModel.loadEvents() }

    val filteredEvents = remember(eventState.events, searchQuery, selectedCategory) {
        eventState.events.filter { event ->
            val matchesSearch = searchQuery.isBlank() ||
                event.name.contains(searchQuery, ignoreCase = true) ||
                event.location.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == null || event.category == selectedCategory
            matchesSearch && matchesCategory
        }
    }

    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colorStops = arrayOf(
                                0f to Color(0xFF1A0A3D),
                                0.5f to GradientStart,
                                1f to GradientMid
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarBadge(
                                name = currentUser.fullName,
                                size = 46.dp,
                                gradient = listOf(AuroraViolet, AuroraCyan)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    greeting,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.6f),
                                    letterSpacing = 0.3.sp
                                )
                                Text(
                                    currentUser.fullName.split(" ").first(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable { onLogout() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Logout, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    ModernSearchBar(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        onFilterClick = { },
                        placeholder = "Discover events near you..."
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
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                QuickActionsSection(
                    currentUser = currentUser,
                    onScanQR = onScanQR,
                    onWallet = onWallet,
                    onDashboard = onDashboard,
                    onCashier = onCashier
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                CategoryFilterSection(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                SectionHeader(
                    title = "Featured Events",
                    onAction = {},
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            if (eventState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(64.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = Primary,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            } else if (filteredEvents.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(PrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.EventBusy, null, modifier = Modifier.size(40.dp), tint = Primary.copy(alpha = 0.5f))
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("No events found", style = MaterialTheme.typography.titleSmall, color = Zinc600, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Try a different search or category", style = MaterialTheme.typography.bodySmall, color = Zinc400)
                    }
                }
            } else {
                items(filteredEvents) { event ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        ModernEventCard(
                            title = event.name,
                            location = event.location,
                            date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(event.date)),
                            price = "Free",
                            imageUrl = event.imageUrl,
                            category = event.category.name,
                            onClick = { onEventClick(event.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionsSection(
    currentUser: User,
    onScanQR: () -> Unit,
    onWallet: () -> Unit,
    onDashboard: () -> Unit,
    onCashier: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (currentUser.role.canAccessDashboard()) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = "Analytics",
                icon = Icons.Default.BarChart,
                gradient = listOf(GradientStart, AuroraBlue),
                onClick = onDashboard
            )
        }

        QuickActionCard(
            modifier = Modifier.weight(1f),
            title = "Wallet",
            icon = Icons.Default.AccountBalanceWallet,
            gradient = listOf(Accent, Color(0xFFD97706)),
            onClick = onWallet
        )

        if (currentUser.role == UserRole.ADMIN) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = "POS",
                icon = Icons.Default.PointOfSale,
                gradient = listOf(Tertiary, TertiaryDark),
                onClick = onCashier
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(18.dp), spotColor = gradient.first().copy(0.2f))
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(gradient))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CategoryFilterSection(
    selectedCategory: EventCategory?,
    onCategorySelected: (EventCategory?) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            CategoryChip(label = "All", isSelected = selectedCategory == null, onClick = { onCategorySelected(null) })
        }
        items(EventCategory.entries) { category ->
            CategoryChip(
                label = category.name.lowercase().replaceFirstChar { it.uppercase() },
                isSelected = category == selectedCategory,
                onClick = { onCategorySelected(category) }
            )
        }
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(24.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), spotColor = Primary.copy(0.1f))
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = if (selectedIndex == 0) Icons.Filled.Home else Icons.Outlined.Home,
                label = "Home",
                selected = selectedIndex == 0,
                onClick = { onTabSelected(0) }
            )

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(8.dp, CircleShape, spotColor = Primary.copy(0.3f))
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(GradientStart, AuroraBlue)))
                    .clickable { if (currentUser.role.canManageEvents()) onCreateEvent() else onScanQR() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (currentUser.role.canManageEvents()) Icons.Default.Add else Icons.Default.QrCodeScanner,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            BottomNavItem(
                icon = if (selectedIndex == 1) Icons.Filled.Person else Icons.Outlined.Person,
                label = "Profile",
                selected = selectedIndex == 1,
                onClick = { onTabSelected(1) }
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val animatedColor by animateColorAsState(
        targetValue = if (selected) Primary else Zinc400,
        animationSpec = tween(200),
        label = "navColor"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = animatedColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = animatedColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
