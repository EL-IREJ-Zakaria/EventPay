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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eventpay.domain.model.Event
import com.example.eventpay.domain.model.EventStatus
import com.example.eventpay.ui.admin.AdminViewModel
import com.example.eventpay.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

private val statusFilterOptions = listOf("All", "PUBLISHED", "DRAFT", "ONGOING", "COMPLETED", "CANCELLED")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEventListScreen(
    adminViewModel: AdminViewModel,
    currentUserId: String,
    onBack: () -> Unit,
    onViewParticipants: (String) -> Unit = {}
) {
    val uiState by adminViewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var eventToDelete by remember { mutableStateOf<Event?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    LaunchedEffect(Unit) { adminViewModel.loadEvents() }

    val filteredEvents = remember(uiState.events, searchQuery, selectedFilter) {
        uiState.events
            .filter { event ->
                val matchesSearch = searchQuery.isBlank() ||
                    event.name.contains(searchQuery, ignoreCase = true) ||
                    event.location.contains(searchQuery, ignoreCase = true)
                val matchesFilter = selectedFilter == "All" ||
                    event.status.name == selectedFilter
                matchesSearch && matchesFilter
            }
    }

    val statusCounts = remember(uiState.events) {
        mapOf(
            "All" to uiState.events.size,
            "PUBLISHED" to uiState.events.count { it.status == EventStatus.PUBLISHED },
            "DRAFT" to uiState.events.count { it.status == EventStatus.DRAFT },
            "ONGOING" to uiState.events.count { it.status == EventStatus.ONGOING },
            "COMPLETED" to uiState.events.count { it.status == EventStatus.COMPLETED },
            "CANCELLED" to uiState.events.count { it.status == EventStatus.CANCELLED }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundLight)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                EventListTopBar(
                    eventCount = filteredEvents.size,
                    onBack = onBack
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = Primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(18.dp),
                        spotColor = Primary.copy(alpha = 0.5f)
                    ),
                    icon = {
                        Icon(
                            Icons.Default.Add,
                            null,
                            tint = Color.White
                        )
                    },
                    text = {
                        Text(
                            "Create Event",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                AnimatedVisibility(
                    visible = uiState.error != null,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                ) {
                    uiState.error?.let { err ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ErrorContainer)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                null,
                                tint = Error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                err,
                                color = ErrorDark,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                EventSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(statusFilterOptions.filter { filter ->
                        (statusCounts[filter] ?: 0) > 0 || filter == "All"
                    }) { filter ->
                        StatusFilterChip(
                            label = filter,
                            count = statusCounts[filter] ?: 0,
                            isSelected = selectedFilter == filter,
                            onClick = { selectedFilter = filter }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                when {
                    uiState.isLoading -> {
                        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 4.dp)) {
                            items(4) { index ->
                                EventCardShimmer(index)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                    filteredEvents.isEmpty() -> {
                        EventListEmptyState(
                            isFiltered = searchQuery.isNotBlank() || selectedFilter != "All",
                            searchQuery = searchQuery,
                            onCreateEvent = { showCreateDialog = true },
                            onClearFilter = {
                                searchQuery = ""
                                selectedFilter = "All"
                            }
                        )
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                top = 4.dp,
                                bottom = 96.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredEvents, key = { it.id }) { event ->
                                var visible by remember { mutableStateOf(false) }
                                LaunchedEffect(Unit) {
                                    kotlinx.coroutines.delay(50L)
                                    visible = true
                                }
                                AnimatedVisibility(
                                    visible = visible,
                                    enter = fadeIn(tween(250)) + slideInVertically(
                                        initialOffsetY = { it / 4 },
                                        animationSpec = tween(300)
                                    )
                                ) {
                                    EventManagementCard(
                                        event = event,
                                        onDelete = { eventToDelete = event },
                                        onViewParticipants = { onViewParticipants(event.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateEventDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, description, location, date, totalTickets, price ->
                adminViewModel.createEvent(
                    com.example.eventpay.domain.model.Event(
                        id = "",
                        name = name,
                        description = description,
                        location = location,
                        date = date,
                        endDate = date + (4 * 60 * 60 * 1000),
                        totalTickets = totalTickets,
                        organizerId = currentUserId,
                        status = EventStatus.PUBLISHED,
                        isPublished = true
                    )
                )
                showCreateDialog = false
            }
        )
    }

    eventToDelete?.let { event ->
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            shape = RoundedCornerShape(24.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(ErrorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        null,
                        tint = Error,
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                Text(
                    "Delete Event",
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurfaceLight
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${event.name}\"? This action cannot be undone.",
                    color = OnSurfaceVariantLight,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.deleteEvent(event.id)
                        eventToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { eventToDelete = null },
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Text("Cancel", color = OnSurfaceVariantLight)
                }
            }
        )
    }
}

@Composable
private fun EventListTopBar(
    eventCount: Int,
    onBack: () -> Unit
) {
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
            .padding(start = 4.dp, end = 20.dp, top = 8.dp, bottom = 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Event Management",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    "$eventCount event${if (eventCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f)
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.18f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Event, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        "Events",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun EventSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Primary.copy(alpha = 0.08f))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = Primary, modifier = Modifier.size(20.dp))
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    "Search by name or location…",
                    color = Zinc400,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedTextColor = Zinc900,
                unfocusedTextColor = Zinc900,
                cursorColor = Primary
            ),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium
        )
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Zinc100)
                    .clickable { onQueryChange("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, null, tint = Zinc500, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun StatusFilterChip(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val chipColor = when (label) {
        "PUBLISHED" -> Tertiary
        "ONGOING" -> Secondary
        "CANCELLED" -> Error
        "COMPLETED" -> Primary
        "DRAFT" -> OnSurfaceVariantLight
        else -> Primary
    }
    val selectedColor = if (label == "All") Primary else chipColor

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) selectedColor else SurfaceLight,
        label = "chipBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else OnSurfaceVariantLight,
        label = "chipText"
    )

    Surface(
        modifier = Modifier
            .shadow(
                elevation = if (isSelected) 6.dp else 2.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = if (isSelected) selectedColor.copy(alpha = 0.4f) else Color.Transparent
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label.replace("_", " "),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = textColor
            )
            if (count > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (isSelected) Color.White.copy(alpha = 0.25f)
                            else selectedColor.copy(alpha = 0.12f)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "$count",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else selectedColor
                    )
                }
            }
        }
    }
}

@Composable
private fun EventManagementCard(
    event: Event,
    onDelete: () -> Unit,
    onViewParticipants: () -> Unit = {}
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault()) }
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
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f)
            ),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceLight
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(statusColor, statusColor.copy(alpha = 0.3f))
                        ),
                        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.Top) {
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
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            event.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = OnSurfaceLight,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.LocationOn,
                                null,
                                modifier = Modifier.size(13.dp),
                                tint = OnSurfaceVariantLight
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
                                Icons.Outlined.CalendarToday,
                                null,
                                modifier = Modifier.size(13.dp),
                                tint = OnSurfaceVariantLight
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                dateFormat.format(Date(event.date)),
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariantLight
                            )
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = statusColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                event.status.name,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ErrorContainer)
                                .clickable(onClick = onDelete),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                null,
                                tint = Error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DividerLight, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    EventMetricChip(
                        label = "Total",
                        value = "${event.totalTickets}",
                        color = OnSurfaceVariantLight
                    )
                    EventMetricChip(
                        label = "Sold",
                        value = "${event.reservedTickets}",
                        color = Secondary
                    )
                    EventMetricChip(
                        label = "Check-Ins",
                        value = "${event.checkedInCount}",
                        color = Tertiary
                    )
                    EventMetricChip(
                        label = "Rate",
                        value = "${(checkInPct * 100).toInt()}%",
                        color = Primary
                    )
                }

                if (event.totalTickets > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Check-in Progress",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariantLight
                        )
                        Text(
                            "${event.checkedInCount} / ${event.totalTickets}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Tertiary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    val animPct by animateFloatAsState(
                        targetValue = checkInPct,
                        animationSpec = tween(900, easing = FastOutSlowInEasing),
                        label = "checkInPct"
                    )
                    Box(modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)).background(TertiaryContainer)) {
                        Box(modifier = Modifier.fillMaxWidth(animPct).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(Brush.horizontalGradient(listOf(Tertiary, TertiaryLight))))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DividerLight, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onViewParticipants,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Outlined.People,
                            null,
                            modifier = Modifier.size(15.dp),
                            tint = Primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Participants",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventMetricChip(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariantLight
        )
    }
}

@Composable
private fun EventListEmptyState(
    isFiltered: Boolean,
    searchQuery: String,
    onCreateEvent: () -> Unit,
    onClearFilter: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(88.dp),
            shape = CircleShape,
            color = if (isFiltered) SurfaceVariantLight else PrimaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (isFiltered) Icons.Outlined.SearchOff else Icons.Outlined.Event,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = if (isFiltered) OnSurfaceVariantLight else Primary
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            if (isFiltered) {
                if (searchQuery.isNotBlank()) "No results for \"$searchQuery\""
                else "No events in this category"
            } else "No events yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = OnBackgroundLight
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            if (isFiltered) "Try adjusting your search or filters"
            else "Create your first event and start selling tickets",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariantLight
        )
        Spacer(modifier = Modifier.height(24.dp))
        if (isFiltered) {
            OutlinedButton(
                onClick = onClearFilter,
                shape = RoundedCornerShape(14.dp),
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Icon(Icons.Default.FilterListOff, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Clear Filters")
            }
        } else {
            Button(
                onClick = onCreateEvent,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Event", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EventCardShimmer(index: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 100L)
        visible = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    AnimatedVisibility(visible = visible, enter = fadeIn(tween(200))) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = SurfaceLight
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(ShimmerLight.copy(alpha = shimmerAlpha))
                )
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(ShimmerLight.copy(alpha = shimmerAlpha))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ShimmerLight.copy(alpha = shimmerAlpha))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .height(11.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ShimmerLight.copy(alpha = shimmerAlpha))
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(70.dp)
                                .height(24.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ShimmerLight.copy(alpha = shimmerAlpha))
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(DividerLight)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        repeat(4) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(16.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ShimmerLight.copy(alpha = shimmerAlpha))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(48.dp)
                                        .height(11.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(ShimmerLight.copy(alpha = shimmerAlpha))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateEventDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        description: String,
        location: String,
        date: Long,
        totalTickets: Int,
        price: Double
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var totalTickets by remember { mutableStateOf("100") }
    var price by remember { mutableStateOf("0") }

    val isValid = name.isNotBlank() && description.isNotBlank() &&
        location.isNotBlank() && totalTickets.toIntOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = SurfaceLight,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Event,
                        null,
                        tint = Primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Create New Event",
                        fontWeight = FontWeight.ExtraBold,
                        color = OnSurfaceLight
                    )
                    Text(
                        "Fill in the details below",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariantLight
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DialogTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Event Name *",
                    icon = Icons.Outlined.Event
                )
                DialogTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Description *",
                    icon = Icons.Outlined.Description,
                    minLines = 2,
                    maxLines = 3
                )
                DialogTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = "Location *",
                    icon = Icons.Outlined.LocationOn
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DialogTextField(
                        value = totalTickets,
                        onValueChange = { totalTickets = it },
                        label = "Capacity *",
                        icon = Icons.Outlined.ConfirmationNumber,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    DialogTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = "Price",
                        icon = Icons.Outlined.Payments,
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        name,
                        description,
                        location,
                        System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000),
                        totalTickets.toIntOrNull() ?: 100,
                        price.toDoubleOrNull() ?: 0.0
                    )
                },
                enabled = isValid,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel", color = OnSurfaceVariantLight)
            }
        }
    )
}

@Composable
private fun DialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        leadingIcon = {
            Icon(icon, null, tint = Primary, modifier = Modifier.size(18.dp))
        },
        modifier = modifier.fillMaxWidth(),
        minLines = minLines,
        maxLines = maxLines,
        singleLine = maxLines == 1,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            unfocusedBorderColor = OutlineLight,
            focusedLabelColor = Primary
        )
    )
}
