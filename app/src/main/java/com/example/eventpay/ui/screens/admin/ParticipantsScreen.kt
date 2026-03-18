package com.example.eventpay.ui.screens.admin

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eventpay.data.model.Ticket
import com.example.eventpay.data.model.TicketStatus
import com.example.eventpay.data.model.TicketType
import com.example.eventpay.domain.model.Event
import com.example.eventpay.ui.components.SaaSCard
import com.example.eventpay.ui.theme.*
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

data class ParticipantInfo(
    val ticketId: String,
    val userId: String,
    val fullName: String,
    val email: String,
    val ticketType: TicketType,
    val status: TicketStatus,
    val checkedIn: Boolean,
    val checkedInAt: Long?,
    val registrationDate: Long,
    val phone: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantsScreen(
    event: Event,
    participants: List<ParticipantInfo>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onAddParticipant: (name: String, email: String, phone: String, ticketType: TicketType) -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<ParticipantFilter>(ParticipantFilter.ALL) }
    var showAddDialog by remember { mutableStateOf(false) }
    var exportSuccess by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    val filteredParticipants = remember(participants, searchQuery, selectedFilter) {
        participants.filter { p ->
            val matchesSearch = searchQuery.isBlank() ||
                p.fullName.contains(searchQuery, ignoreCase = true) ||
                p.email.contains(searchQuery, ignoreCase = true) ||
                p.ticketId.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                ParticipantFilter.ALL -> true
                ParticipantFilter.CHECKED_IN -> p.checkedIn
                ParticipantFilter.NOT_CHECKED_IN -> !p.checkedIn
                ParticipantFilter.VIP -> p.ticketType == TicketType.VIP
                ParticipantFilter.EARLY_BIRD -> p.ticketType == TicketType.EARLY_BIRD
                ParticipantFilter.STANDARD -> p.ticketType == TicketType.STANDARD
            }
            matchesSearch && matchesFilter
        }
    }

    val checkedInCount = participants.count { it.checkedIn }
    val attendanceRate = if (participants.isNotEmpty()) {
        (checkedInCount.toFloat() / participants.size * 100).toInt()
    } else 0

    LaunchedEffect(exportSuccess) {
        if (exportSuccess) {
            snackbarHostState.showSnackbar("Participants exported to CSV successfully!")
            exportSuccess = false
        }
    }

    if (showAddDialog) {
        AddParticipantDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, email, phone, type ->
                onAddParticipant(name, email, phone, type)
                showAddDialog = false
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    .padding(bottom = 20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Participants",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                event.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.PersonAdd, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ParticipantStatChip(
                            label = "Total",
                            value = "${participants.size}",
                            color = Color.White
                        )
                        ParticipantStatChip(
                            label = "Checked In",
                            value = "$checkedInCount",
                            color = Color(0xFF34D399)
                        )
                        ParticipantStatChip(
                            label = "Attendance",
                            value = "$attendanceRate%",
                            color = Color(0xFFFBBF24)
                        )
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(16.dp))
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .height(52.dp)
                        .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Primary.copy(alpha = 0.08f))
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, null, tint = Primary, modifier = Modifier.size(20.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search name, email, ticket ID...", color = Zinc400, style = MaterialTheme.typography.bodyMedium) },
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
                    if (searchQuery.isNotBlank()) {
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape).background(Zinc100).clickable { searchQuery = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, null, tint = Zinc500, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ParticipantFilter.entries) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter.label, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
            } else if (filteredParticipants.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.PeopleAlt,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = Slate200
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (searchQuery.isNotBlank()) "No participants match your search"
                            else "No participants yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate500,
                            textAlign = TextAlign.Center
                        )
                        if (searchQuery.isBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Tap + to add participants manually",
                                style = MaterialTheme.typography.labelMedium,
                                color = Primary
                            )
                        }
                    }
                }
            } else {
                item {
                    Padding(horizontal = 20.dp, vertical = 4.dp) {
                        Text(
                            "${filteredParticipants.size} participant${if (filteredParticipants.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Slate500
                        )
                    }
                }
                itemsIndexed(filteredParticipants) { index, participant ->
                    ParticipantCard(
                        participant = participant,
                        index = index,
                        sdf = sdf
                    )
                }
            }
        }
    }
}

@Composable
private fun Padding(
    horizontal: androidx.compose.ui.unit.Dp,
    vertical: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.padding(horizontal = horizontal, vertical = vertical)) {
        content()
    }
}

@Composable
private fun ParticipantStatChip(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ParticipantCard(
    participant: ParticipantInfo,
    index: Int,
    sdf: SimpleDateFormat
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(300, delayMillis = index * 30),
        label = "alpha"
    )

    val typeColor = when (participant.ticketType) {
        TicketType.VIP -> Color(0xFFD97706)
        TicketType.EARLY_BIRD -> Color(0xFF059669)
        TicketType.STANDARD -> Primary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = typeColor.copy(alpha = 0.1f),
                ambientColor = Color.Black.copy(alpha = 0.04f)
            )
            .graphicsLayer { alpha = animatedAlpha },
        shape = RoundedCornerShape(18.dp),
        color = Color.White
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(listOf(typeColor, typeColor.copy(alpha = 0.3f))),
                        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(typeColor, typeColor.copy(alpha = 0.7f)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = participant.fullName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            participant.fullName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Zinc900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        TicketTypeBadge(participant.ticketType)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        participant.email,
                        style = MaterialTheme.typography.labelSmall,
                        color = Zinc500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        "Registered ${sdf.format(Date(participant.registrationDate))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Zinc400
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (participant.checkedIn) Tertiary.copy(alpha = 0.1f) else Zinc100
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (participant.checkedIn) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                null,
                                tint = if (participant.checkedIn) Tertiary else Zinc400,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (participant.checkedIn) "In" else "Pending",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (participant.checkedIn) Tertiary else Zinc500
                            )
                        }
                    }
                    if (participant.checkedIn && participant.checkedInAt != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(participant.checkedInAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = Zinc400
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TicketTypeBadge(type: TicketType) {
    val (color, label) = when (type) {
        TicketType.VIP -> Color(0xFFD97706) to "VIP"
        TicketType.EARLY_BIRD -> Color(0xFF059669) to "Early Bird"
        TicketType.STANDARD -> Primary to "Standard"
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            letterSpacing = 0.3.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddParticipantDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, email: String, phone: String, type: TicketType) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TicketType.STANDARD) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PersonAdd, null, tint = Primary, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Add Participant", fontWeight = FontWeight.Black, color = Slate900)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    leadingIcon = { Icon(Icons.Outlined.Person, null, tint = Slate400, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Slate200
                    )
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Outlined.Email, null, tint = Slate400, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Slate200
                    )
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone (optional)") },
                    leadingIcon = { Icon(Icons.Outlined.Phone, null, tint = Slate400, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Slate200
                    )
                )

                Text("Ticket Type", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Slate700)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TicketType.entries.forEach { type ->
                        val (color, label, icon) = when (type) {
                            TicketType.STANDARD -> Triple(Primary, "Standard", Icons.Default.ConfirmationNumber)
                            TicketType.VIP -> Triple(Color(0xFFD97706), "VIP", Icons.Default.Stars)
                            TicketType.EARLY_BIRD -> Triple(Color(0xFF059669), "Early Bird", Icons.Default.Bolt)
                        }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedType = type },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedType == type) color.copy(alpha = 0.12f) else Slate50,
                            border = androidx.compose.foundation.BorderStroke(
                                if (selectedType == type) 1.5.dp else 1.dp,
                                if (selectedType == type) color else Slate200
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(icon, null, tint = if (selectedType == type) color else Slate400, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selectedType == type) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedType == type) color else Slate500,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && email.isNotBlank()) onAdd(name, email, phone, selectedType) },
                enabled = name.isNotBlank() && email.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Participant", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate500)
            }
        }
    )
}

enum class ParticipantFilter(val label: String) {
    ALL("All"),
    CHECKED_IN("Checked In"),
    NOT_CHECKED_IN("Pending"),
    STANDARD("Standard"),
    VIP("VIP"),
    EARLY_BIRD("Early Bird")
}

private fun exportParticipantsToCSV(
    context: Context,
    participants: List<ParticipantInfo>,
    eventName: String,
    onResult: (Boolean) -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val fileName = "participants_${eventName.replace(" ", "_")}_${System.currentTimeMillis()}.csv"
    val csvContent = buildString {
        appendLine("Ticket ID,Name,Email,Phone,Ticket Type,Status,Checked In,Check-in Time,Registration Date")
        participants.forEach { p ->
            appendLine(
                "${p.ticketId}," +
                "\"${p.fullName}\"," +
                "\"${p.email}\"," +
                "\"${p.phone ?: ""}\"," +
                "${p.ticketType.name}," +
                "${p.status.name}," +
                "${p.checkedIn}," +
                "\"${if (p.checkedInAt != null) sdf.format(Date(p.checkedInAt)) else ""}\"," +
                "\"${sdf.format(Date(p.registrationDate))}\""
            )
        }
    }

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            )
            uri?.let { u ->
                context.contentResolver.openOutputStream(u)?.use { stream ->
                    stream.write(csvContent.toByteArray())
                }
                onResult(true)
            } ?: onResult(false)
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = java.io.File(downloadsDir, fileName)
            file.writeText(csvContent)
            onResult(true)
        }
    } catch (e: IOException) {
        onResult(false)
    }
}
