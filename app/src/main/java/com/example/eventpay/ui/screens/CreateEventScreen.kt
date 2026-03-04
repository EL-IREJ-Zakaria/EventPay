package com.example.eventpay.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.eventpay.domain.model.User
import com.example.eventpay.ui.components.*
import com.example.eventpay.ui.event.EventViewModel
import com.example.eventpay.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    currentUser: User,
    eventViewModel: EventViewModel,
    onBack: () -> Unit,
    onEventCreated: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var ticketPrice by remember { mutableStateOf("") }
    var totalTickets by remember { mutableStateOf("") }
    var isEntered by remember { mutableStateOf(false) }

    val eventState by eventViewModel.eventState.collectAsState()

    LaunchedEffect(Unit) { isEntered = true }

    val cardSlide by animateFloatAsState(
        targetValue = if (isEntered) 0f else 800f,
        animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow),
        label = "cardSlide"
    )
    val heroAlpha by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "heroAlpha"
    )

    val isFormValid = name.isNotBlank() && description.isNotBlank() &&
            location.isNotBlank() && ticketPrice.isNotBlank() &&
            totalTickets.isNotBlank() && !eventState.isLoading

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(GradientStart, GradientMid, PrimaryDark)))
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.28f)
                    .graphicsLayer { alpha = heroAlpha },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.18f))
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White.copy(alpha = 0.9f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.AddCircle,
                                contentDescription = null,
                                tint = GradientStart,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "Create Event",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Fill in the details below",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.72f)
                    .graphicsLayer { translationY = cardSlide }
                    .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                    .background(Color.White)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 14.dp, bottom = 28.dp)
                        .size(width = 44.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(OutlineVariantLight)
                )

                Text(
                    "Event Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = OnBackgroundLight
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Tell attendees about your event",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariantLight
                )

                Spacer(modifier = Modifier.height(28.dp))

                PremiumTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Event Name",
                    leadingIcon = Icons.Outlined.Event,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Description",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 110.dp),
                    placeholder = {
                        Text(
                            "Describe your event to potential attendees...",
                            color = OnSurfaceVariantLight.copy(alpha = 0.6f)
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.Description, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = OutlineLight,
                        focusedContainerColor = SurfaceVariantLight.copy(alpha = 0.3f),
                        unfocusedContainerColor = SurfaceVariantLight.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                PremiumTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = "Location / Venue",
                    leadingIcon = Icons.Outlined.LocationOn,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(2.dp.times(16))
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(GradientStart, GradientEnd))
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Ticket Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = OnBackgroundLight
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Price (MAD)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = ticketPrice,
                            onValueChange = { ticketPrice = it.filter { c -> c.isDigit() || c == '.' } },
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            leadingIcon = {
                                Icon(Icons.Outlined.Payments, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = OutlineLight,
                                focusedContainerColor = SurfaceVariantLight.copy(alpha = 0.3f),
                                unfocusedContainerColor = SurfaceVariantLight.copy(alpha = 0.3f)
                            ),
                            singleLine = true
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Total Tickets",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = totalTickets,
                            onValueChange = { totalTickets = it.filter { c -> c.isDigit() } },
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            leadingIcon = {
                                Icon(Icons.Outlined.ConfirmationNumber, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            placeholder = { Text("100") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = OutlineLight,
                                focusedContainerColor = SurfaceVariantLight.copy(alpha = 0.3f),
                                unfocusedContainerColor = SurfaceVariantLight.copy(alpha = 0.3f)
                            ),
                            singleLine = true
                        )
                    }
                }

                if (ticketPrice == "0" || ticketPrice == "0.0" || ticketPrice == "0.00") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = TertiaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, null, tint = Tertiary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "This event will be listed as Free Entry",
                                style = MaterialTheme.typography.bodySmall,
                                color = TertiaryDark,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                AnimatedVisibility(
                    visible = eventState.error != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        color = ErrorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Error, null, tint = Error, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                eventState.error ?: "",
                                color = ErrorDark,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                AnimatedGradientButton(
                    onClick = {
                        val price = ticketPrice.toDoubleOrNull() ?: 0.0
                        val tickets = totalTickets.toIntOrNull() ?: 0
                        eventViewModel.createEvent(
                            name = name,
                            description = description,
                            location = location,
                            date = System.currentTimeMillis() + 86400000L,
                            ticketPrice = price,
                            totalTickets = tickets,
                            organizerId = currentUser.id
                        )
                        onEventCreated()
                    },
                    text = "Publish Event",
                    icon = Icons.Default.Rocket,
                    enabled = isFormValid,
                    isLoading = eventState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Discard Changes",
                        color = OnSurfaceVariantLight,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
