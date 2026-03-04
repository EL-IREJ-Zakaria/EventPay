package com.example.eventpay.ui.screens.admin

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eventpay.domain.model.Event
import com.example.eventpay.domain.model.EventCategory
import com.example.eventpay.domain.model.EventStatus
import com.example.eventpay.ui.admin.AdminViewModel
import com.example.eventpay.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEventListScreen(
    adminViewModel: AdminViewModel,
    currentUserId: String,
    onBack: () -> Unit
) {
    val uiState by adminViewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var eventToDelete by remember { mutableStateOf<Event?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { adminViewModel.loadEvents() }

    val filteredEvents = remember(uiState.events, searchQuery) {
        if (searchQuery.isBlank()) uiState.events
        else uiState.events.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.location.contains(searchQuery, ignoreCase = true)
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
                        .padding(top = 52.dp, start = 4.dp, end = 20.dp, bottom = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                        }
                        Text(
                            "Event Management",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = Primary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White)
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                uiState.error?.let { err ->
                    Surface(
                        color = ErrorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = Error, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(err, color = ErrorDark, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search events…") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Primary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = OutlineLight
                    )
                )

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                } else if (filteredEvents.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Event,
                                null,
                                modifier = Modifier.size(64.dp),
                                tint = OutlineLight
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                if (searchQuery.isBlank()) "No events yet" else "No results for \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyLarge,
                                color = OnSurfaceVariantLight
                            )
                        }
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
                        items(filteredEvents, key = { it.id }) { event ->
                            EventManagementCard(
                                event = event,
                                onDelete = { eventToDelete = event }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
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
                    Event(
                        id = "",
                        name = name,
                        description = description,
                        location = location,
                        date = date,
                        endDate = date + (4 * 60 * 60 * 1000),
                        ticketPrice = price,
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
            title = { Text("Delete Event") },
            text = { Text("Are you sure you want to delete \"${event.name}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        adminViewModel.deleteEvent(event.id)
                        eventToDelete = null
                    }
                ) {
                    Text("Delete", color = Error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { eventToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EventManagementCard(
    event: Event,
    onDelete: () -> Unit
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
        (event.checkedInCount.toFloat() / event.totalTickets * 100).toInt()
    else 0

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceLight
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Event, null, tint = Primary, modifier = Modifier.size(26.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        event.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.LocationOn,
                            null,
                            modifier = Modifier.size(14.dp),
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
                            modifier = Modifier.size(14.dp),
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
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            event.status.name,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            null,
                            tint = Error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = DividerLight)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                EventMetric(label = "Tickets", value = "${event.totalTickets}")
                EventMetric(label = "Sold", value = "${event.soldTickets}")
                EventMetric(label = "Checked In", value = "${event.checkedInCount}")
                EventMetric(label = "Check-In %", value = "$checkInPct%")
            }

            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { checkInPct / 100f },
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

@Composable
private fun EventMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = OnSurfaceLight
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariantLight
        )
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
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Event, null, tint = Primary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create New Event", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Event Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary)
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = totalTickets,
                        onValueChange = { totalTickets = it },
                        label = { Text("Capacity *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary)
                    )
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Price") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary)
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
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Create", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = OnSurfaceVariantLight)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
