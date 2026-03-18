package com.example.eventpay.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eventpay.domain.model.EventCategory
import com.example.eventpay.domain.model.User
import com.example.eventpay.ui.components.*
import com.example.eventpay.ui.event.EventViewModel
import com.example.eventpay.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    currentUser: User,
    eventViewModel: EventViewModel,
    onBack: () -> Unit,
    onEventCreated: () -> Unit
) {
    var step by remember { mutableStateOf(1) }
    val totalSteps = 3

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(EventCategory.GENERAL) }
    var ticketPrice by remember { mutableStateOf("0") }
    var totalTickets by remember { mutableStateOf("") }
    var vipTickets by remember { mutableStateOf("0") }
    var earlyBirdTickets by remember { mutableStateOf("0") }
    var contactEmail by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("18:00") }

    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis() + 86400000L) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    var isEntered by remember { mutableStateOf(false) }
    val eventState by eventViewModel.eventState.collectAsState()

    LaunchedEffect(Unit) { isEntered = true }

    val cardSlide by animateFloatAsState(
        targetValue = if (isEntered) 0f else 800f,
        animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow),
        label = "cardSlide"
    )

    val sdf = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                    showDatePicker = false
                }) { Text("Confirm", color = Primary) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = Primary,
                    todayDateBorderColor = Primary
                )
            )
        }
    }

    if (showStartTimePicker) {
        TimePickerDialog(
            title = "Start Time",
            initialTime = startTime,
            onDismiss = { showStartTimePicker = false },
            onConfirm = { h, m ->
                startTime = String.format("%02d:%02d", h, m)
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            title = "End Time",
            initialTime = endTime,
            onDismiss = { showEndTimePicker = false },
            onConfirm = { h, m ->
                endTime = String.format("%02d:%02d", h, m)
                showEndTimePicker = false
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundLight)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(GradientStart, GradientMid, GradientEnd)))
                    .padding(bottom = 24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (step > 1) step-- else onBack() },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            "Create Event",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "$step / $totalSteps",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(totalSteps) { index ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (index < step) Color.White
                                        else Color.White.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .graphicsLayer { translationY = cardSlide }
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(SurfaceLight)
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp)
            ) {
                Crossfade(targetState = step, label = "stepTransition") { currentStep ->
                    when (currentStep) {
                        1 -> BasicInfoStep(
                            name = name,
                            onNameChange = { name = it },
                            description = description,
                            onDescriptionChange = { description = it },
                            location = location,
                            onLocationChange = { location = it },
                            selectedCategory = selectedCategory,
                            onCategoryChange = { selectedCategory = it },
                            contactEmail = contactEmail,
                            onContactEmailChange = { contactEmail = it },
                            contactPhone = contactPhone,
                            onContactPhoneChange = { contactPhone = it }
                        )
                        2 -> DateTimeStep(
                            selectedDate = sdf.format(Date(selectedDateMillis)),
                            onDateClick = { showDatePicker = true },
                            startTime = startTime,
                            onStartTimeClick = { showStartTimePicker = true },
                            endTime = endTime,
                            onEndTimeClick = { showEndTimePicker = true }
                        )
                        3 -> TicketLogisticsStep(
                            ticketPrice = ticketPrice,
                            onPriceChange = { ticketPrice = it.filter { c -> c.isDigit() || c == '.' } },
                            totalTickets = totalTickets,
                            onTotalTicketsChange = { totalTickets = it.filter { c -> c.isDigit() } },
                            vipTickets = vipTickets,
                            onVipTicketsChange = { vipTickets = it.filter { c -> c.isDigit() } },
                            earlyBirdTickets = earlyBirdTickets,
                            onEarlyBirdTicketsChange = { earlyBirdTickets = it.filter { c -> c.isDigit() } }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (eventState.error != null) {
                    Surface(
                        color = ErrorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, null, tint = Error, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                eventState.error ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate900
                            )
                        }
                    }
                }

                val canProceedStep1 = name.isNotBlank() && description.isNotBlank() && location.isNotBlank()
                val canProceedStep2 = true
                val canProceedStep3 = totalTickets.isNotBlank() && (totalTickets.toIntOrNull() ?: 0) > 0

                if (step < totalSteps) {
                    SaaSButton(
                        onClick = { step++ },
                        text = "Next Step",
                        icon = Icons.Default.ArrowForward,
                        enabled = when (step) {
                            1 -> canProceedStep1
                            2 -> canProceedStep2
                            else -> canProceedStep3
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    SaaSButton(
                        onClick = {
                            val vip = vipTickets.toIntOrNull() ?: 0
                            val eb = earlyBirdTickets.toIntOrNull() ?: 0
                            val total = totalTickets.toIntOrNull() ?: 100
                            eventViewModel.createEventFull(
                                name = name,
                                description = description,
                                location = location,
                                date = selectedDateMillis,
                                startTime = startTime,
                                endTime = endTime,
                                totalTickets = total,
                                vipTickets = vip,
                                earlyBirdTickets = eb,
                                ticketPrice = ticketPrice.toDoubleOrNull() ?: 0.0,
                                category = selectedCategory,
                                contactEmail = contactEmail.ifBlank { null },
                                contactPhone = contactPhone.ifBlank { null },
                                organizerId = currentUser.id
                            )
                        },
                        text = "Publish Event",
                        icon = Icons.Default.RocketLaunch,
                        enabled = canProceedStep3 && !eventState.isLoading,
                        loading = eventState.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    LaunchedEffect(eventState.success) {
        if (eventState.success != null) {
            onEventCreated()
        }
    }
}

@Composable
private fun BasicInfoStep(
    name: String,
    onNameChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
    selectedCategory: EventCategory,
    onCategoryChange: (EventCategory) -> Unit,
    contactEmail: String,
    onContactEmailChange: (String) -> Unit,
    contactPhone: String,
    onContactPhoneChange: (String) -> Unit
) {
    Column {
        Text(
            "Basic Information",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Slate900
        )
        Text(
            "Start with the core details of your event",
            style = MaterialTheme.typography.bodyMedium,
            color = Slate500
        )

        Spacer(modifier = Modifier.height(32.dp))

        PremiumTextField(
            value = name,
            onValueChange = onNameChange,
            label = "Event Title",
            leadingIcon = Icons.Outlined.Event,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "Description",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Slate700,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            modifier = Modifier.fillMaxWidth().height(120.dp),
            placeholder = { Text("Describe what makes this event special...", color = Slate400) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = Slate200,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Slate50
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        PremiumTextField(
            value = location,
            onValueChange = onLocationChange,
            label = "Venue Location",
            leadingIcon = Icons.Outlined.LocationOn,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "Category",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Slate700,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        CategoryDropdown(
            selected = selectedCategory,
            onSelected = onCategoryChange
        )

        Spacer(modifier = Modifier.height(20.dp))

        PremiumTextField(
            value = contactEmail,
            onValueChange = onContactEmailChange,
            label = "Contact Email (optional)",
            leadingIcon = Icons.Outlined.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        PremiumTextField(
            value = contactPhone,
            onValueChange = onContactPhoneChange,
            label = "Contact Phone (optional)",
            leadingIcon = Icons.Outlined.Phone,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selected: EventCategory,
    onSelected: (EventCategory) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected.displayName(),
            onValueChange = {},
            readOnly = true,
            leadingIcon = { Icon(Icons.Outlined.Category, null, tint = Slate500) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = Slate200,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Slate50
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            EventCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.displayName()) },
                    onClick = {
                        onSelected(category)
                        expanded = false
                    },
                    leadingIcon = {
                        if (category == selected) {
                            Icon(Icons.Default.Check, null, tint = Primary, modifier = Modifier.size(16.dp))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DateTimeStep(
    selectedDate: String,
    onDateClick: () -> Unit,
    startTime: String,
    onStartTimeClick: () -> Unit,
    endTime: String,
    onEndTimeClick: () -> Unit
) {
    Column {
        Text(
            "Date & Time",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Slate900
        )
        Text(
            "When will your event take place?",
            style = MaterialTheme.typography.bodyMedium,
            color = Slate500
        )

        Spacer(modifier = Modifier.height(32.dp))

        DateTimeSelector(
            icon = Icons.Default.CalendarToday,
            label = "Event Date",
            value = selectedDate,
            onClick = onDateClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DateTimeSelector(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Schedule,
                label = "Start Time",
                value = startTime,
                onClick = onStartTimeClick
            )
            DateTimeSelector(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Timer,
                label = "End Time",
                value = endTime,
                onClick = onEndTimeClick
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SaaSCard(containerColor = Primary.copy(alpha = 0.06f), elevation = 0.dp, showBorder = false) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = Primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "Tip: Make sure the event date is in the future. Attendees will see a countdown once published.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PrimaryDark,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun DateTimeSelector(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Primary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = Slate400)
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Slate900)
            }
        }
    }
}

@Composable
private fun TicketLogisticsStep(
    ticketPrice: String,
    onPriceChange: (String) -> Unit,
    totalTickets: String,
    onTotalTicketsChange: (String) -> Unit,
    vipTickets: String,
    onVipTicketsChange: (String) -> Unit,
    earlyBirdTickets: String,
    onEarlyBirdTicketsChange: (String) -> Unit
) {
    Column {
        Text(
            "Tickets & Logistics",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Slate900
        )
        Text(
            "Configure tickets and capacity",
            style = MaterialTheme.typography.bodyMedium,
            color = Slate500
        )

        Spacer(modifier = Modifier.height(32.dp))

        SaaSCard(containerColor = Slate50, elevation = 0.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                PremiumTextField(
                    value = ticketPrice,
                    onValueChange = onPriceChange,
                    label = "Ticket Price (MAD)",
                    leadingIcon = Icons.Outlined.Payments,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                PremiumTextField(
                    value = totalTickets,
                    onValueChange = onTotalTicketsChange,
                    label = "Total Capacity",
                    leadingIcon = Icons.Outlined.ConfirmationNumber,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (ticketPrice == "0" || ticketPrice == "0.0" || ticketPrice.isBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = Tertiary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CardGiftcard, null, tint = Tertiary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("This will be a free event", style = MaterialTheme.typography.labelMedium, color = TertiaryDark)
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            "Ticket Types Allocation",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Slate700
        )
        Text(
            "Optional — leave 0 to use only Standard tickets",
            style = MaterialTheme.typography.bodySmall,
            color = Slate400
        )

        Spacer(modifier = Modifier.height(16.dp))

        TicketTypeCard(
            icon = Icons.Default.Stars,
            label = "VIP Tickets",
            color = Color(0xFFD97706),
            value = vipTickets,
            onValueChange = onVipTicketsChange,
            description = "Premium access tickets"
        )

        Spacer(modifier = Modifier.height(12.dp))

        TicketTypeCard(
            icon = Icons.Default.Bolt,
            label = "Early Bird Tickets",
            color = Color(0xFF059669),
            value = earlyBirdTickets,
            onValueChange = onEarlyBirdTicketsChange,
            description = "Discounted early registration"
        )
    }
}

@Composable
private fun TicketTypeCard(
    icon: ImageVector,
    label: String,
    color: Color,
    value: String,
    onValueChange: (String) -> Unit,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.06f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Slate900)
                Text(description, style = MaterialTheme.typography.labelSmall, color = Slate400)
            }
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedTextField(
                value = value,
                onValueChange = { onValueChange(it.filter { c -> c.isDigit() }) },
                modifier = Modifier.width(80.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(10.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = Slate900
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = color,
                    unfocusedBorderColor = color.copy(alpha = 0.3f),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    title: String,
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val parts = initialTime.split(":").map { it.toIntOrNull() ?: 0 }
    val timePickerState = rememberTimePickerState(
        initialHour = parts.getOrElse(0) { 9 },
        initialMinute = parts.getOrElse(1) { 0 },
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            TimePicker(
                state = timePickerState,
                colors = TimePickerDefaults.colors(
                    clockDialSelectedContentColor = Color.White,
                    clockDialUnselectedContentColor = Slate700,
                    selectorColor = Primary,
                    timeSelectorSelectedContainerColor = Primary.copy(alpha = 0.12f),
                    timeSelectorSelectedContentColor = Primary
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                Text("Confirm", color = Primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
