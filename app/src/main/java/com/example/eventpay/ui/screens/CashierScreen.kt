package com.example.eventpay.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eventpay.domain.model.Event
import com.example.eventpay.domain.model.PaymentMethod
import com.example.eventpay.domain.model.TicketType
import com.example.eventpay.ui.cashier.CashierViewModel
import com.example.eventpay.ui.components.*
import com.example.eventpay.ui.theme.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierScreen(
    onNavigateBack: () -> Unit,
    viewModel: CashierViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(state.success) {
        state.success?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
            viewModel.clearMessages()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundLight)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (state.isShiftActive)
                            Brush.linearGradient(
                                colorStops = arrayOf(
                                    0f to Color(0xFF065F46),
                                    0.5f to Tertiary,
                                    1f to TertiaryDark
                                )
                            )
                        else
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Cashier Terminal",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        if (state.isShiftActive) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.25f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                    Text(
                                        "Shift Active",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { viewModel.showReportDialog() },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.Assessment, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        if (state.isShiftActive) {
                            IconButton(
                                onClick = { viewModel.showEndShiftDialog() },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                            ) {
                                Icon(Icons.Default.Logout, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            if (!state.isShiftActive) {
                CashierStartShiftPrompt(
                    onStartShift = { viewModel.showStartShiftDialog() },
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    CashierShiftStatsCard(
                        shiftSalesTotal = state.shiftSalesTotal,
                        shiftTicketCount = state.shiftTicketCount,
                        shiftCashTotal = state.shiftCashTotal,
                        shiftCardTotal = state.shiftCardTotal,
                        shiftMobileTotal = state.shiftMobileTotal,
                        startingCash = state.currentShift?.startingCash ?: 0.0
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Select Event",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = OnBackgroundLight
                        )
                        if (state.selectedEvent != null) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Primary,
                                onClick = { viewModel.showSellDialog() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "Sell Ticket",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (state.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Primary, strokeWidth = 3.dp)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(state.events) { event ->
                                CashierEventCard(
                                    event = event,
                                    isSelected = state.selectedEvent?.id == event.id,
                                    onClick = { viewModel.selectEvent(event) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (state.showStartShiftDialog) {
        CashierStartShiftDialog(
            onDismiss = { viewModel.hideStartShiftDialog() },
            onConfirm = { startingCash -> viewModel.startShift(startingCash) }
        )
    }
    if (state.showEndShiftDialog) {
        CashierEndShiftDialog(
            currentShift = state.currentShift,
            shiftCashTotal = state.shiftCashTotal,
            shiftSalesTotal = state.shiftSalesTotal,
            onDismiss = { viewModel.hideEndShiftDialog() },
            onConfirm = { actualCash, notes -> viewModel.endShift(actualCash, notes) }
        )
    }
    if (state.showSellDialog && state.selectedEvent != null) {
        CashierSellTicketDialog(
            event = state.selectedEvent!!,
            isProcessing = state.isProcessing,
            onDismiss = { viewModel.hideSellDialog() },
            onConfirm = { ticketType, paymentMethod, customerName, customerPhone ->
            val price = 0.0 // All tickets are free
                viewModel.sellTicket(
                    eventId = state.selectedEvent!!.id,
                    ticketType = ticketType,
                    price = price,
                    paymentMethod = paymentMethod,
                    customerName = customerName,
                    customerPhone = customerPhone
                )
            },
            getTicketPrice = { 0.0 } // All tickets are free
        )
    }
    if (state.showQRDialog && state.lastSoldTicket != null) {
        CashierQRCodeDialog(
            ticket = state.lastSoldTicket!!,
            qrCode = state.generatedQRCode,
            onDismiss = { viewModel.hideQRDialog() }
        )
    }
    if (state.showReportDialog) {
        CashierDailyReportDialog(
            report = viewModel.generateDailyReport(),
            onDismiss = { viewModel.hideReportDialog() }
        )
    }
}

@Composable
private fun CashierStartShiftPrompt(
    onStartShift: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .shadow(20.dp, RoundedCornerShape(36.dp), spotColor = Primary.copy(0.3f))
                .clip(RoundedCornerShape(36.dp))
                .background(Brush.linearGradient(listOf(GradientStart, AuroraBlue))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PointOfSale,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            "Ready to Sell?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = OnBackgroundLight
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Start your shift to begin selling tickets at the event",
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceVariantLight,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(36.dp))

        AnimatedGradientButton(
            onClick = onStartShift,
            text = "Start Shift",
            icon = Icons.Default.PlayArrow,
            modifier = Modifier.fillMaxWidth(0.7f)
        )
    }
}

@Composable
private fun CashierShiftStatsCard(
    shiftSalesTotal: Double,
    shiftTicketCount: Int,
    shiftCashTotal: Double,
    shiftCardTotal: Double,
    shiftMobileTotal: Double,
    startingCash: Double
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Tertiary.copy(0.12f),
                spotColor = Tertiary.copy(0.18f)
            ),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Shift Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OnBackgroundLight
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = TertiaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = Tertiary, modifier = Modifier.size(12.dp))
                        Text(
                            "Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = TertiaryDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CashierStatPill(
                    label = "Total Sales",
                    value = "${String.format("%.0f", shiftSalesTotal)} MAD",
                    icon = Icons.Outlined.Payments,
                    color = Primary
                )
                Box(
                    modifier = Modifier.height(50.dp).width(1.dp).background(OutlineVariantLight)
                )
                CashierStatPill(
                    label = "Tickets",
                    value = "$shiftTicketCount",
                    icon = Icons.Outlined.ConfirmationNumber,
                    color = Secondary
                )
                Box(
                    modifier = Modifier.height(50.dp).width(1.dp).background(OutlineVariantLight)
                )
                CashierStatPill(
                    label = "Cash Drawer",
                    value = "${String.format("%.0f", startingCash + shiftCashTotal)} MAD",
                    icon = Icons.Default.AccountBalanceWallet,
                    color = Tertiary
                )
            }

            if (shiftCashTotal > 0 || shiftCardTotal > 0 || shiftMobileTotal > 0) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DividerLight)
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CashierPaymentPill("Cash", shiftCashTotal, Tertiary)
                    CashierPaymentPill("Card", shiftCardTotal, Primary)
                    CashierPaymentPill("Mobile", shiftMobileTotal, Secondary)
                }
            }
        }
    }
}

@Composable
private fun CashierStatPill(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = OnBackgroundLight
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariantLight
        )
    }
}

@Composable
private fun CashierPaymentPill(label: String, value: Double, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            "$label: ${String.format("%.0f", value)} MAD",
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariantLight,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun CashierEventCard(
    event: Event,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isSelected) 8.dp else 2.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = if (isSelected) Primary.copy(0.12f) else Color.Black.copy(0.04f),
                spotColor = if (isSelected) Primary.copy(0.2f) else Color.Black.copy(0.04f)
            )
            .then(
                if (isSelected) Modifier.border(
                    width = 2.dp,
                    color = Primary,
                    shape = RoundedCornerShape(20.dp)
                ) else Modifier
            ),
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) PrimaryContainer else MaterialTheme.colorScheme.surface,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        event.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSelected) PrimaryDark else OnBackgroundLight,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Outlined.LocationOn,
                            null,
                            tint = if (isSelected) Primary else OnSurfaceVariantLight,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            event.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) Primary else OnSurfaceVariantLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CashierTicketPriceBadge(
                    "Standard",
                    "Free",
                    Primary,
                    PrimaryContainer
                )
                CashierTicketPriceBadge(
                    "VIP",
                    "Free",
                    AccentDark,
                    WarningContainer
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Primary.copy(0.12f) else SurfaceVariantLight
                ) {
                    Text(
                        "${event.totalTickets - event.reservedTickets}/${event.totalTickets}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) PrimaryDark else OnSurfaceVariantLight,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CashierTicketPriceBadge(
    label: String,
    price: String,
    textColor: Color,
    bgColor: Color
) {
    Surface(shape = RoundedCornerShape(10.dp), color = bgColor) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = textColor.copy(0.7f))
            Text(price, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

@Composable
private fun CashierStartShiftDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var startingCash by remember { mutableStateOf("") }
    var cashError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Primary, modifier = Modifier.size(28.dp))
            }
        },
        title = {
            Text(
                "Start Shift",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column {
                Text(
                    "Enter your starting cash amount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariantLight
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = startingCash,
                    onValueChange = {
                        startingCash = it.filter { c -> c.isDigit() || c == '.' }
                        cashError = null
                    },
                    label = { Text("Starting Cash (MAD)") },
                    leadingIcon = { Icon(Icons.Outlined.Payments, null, tint = Primary) },
                    isError = cashError != null,
                    supportingText = cashError?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = OutlineLight
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = startingCash.toDoubleOrNull()
                    if (amount == null || amount < 0) {
                        cashError = "Please enter a valid amount"
                    } else {
                        onConfirm(amount)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Start Shift", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun CashierEndShiftDialog(
    currentShift: com.example.eventpay.domain.model.CashierShift?,
    shiftCashTotal: Double,
    shiftSalesTotal: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double, String?) -> Unit
) {
    var actualCash by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var cashError by remember { mutableStateOf<String?>(null) }
    val expectedCash = (currentShift?.startingCash ?: 0.0) + shiftCashTotal

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ErrorContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Stop, null, tint = Error, modifier = Modifier.size(28.dp))
            }
        },
        title = {
            Text("End Shift", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceVariantLight
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Shift Summary", style = MaterialTheme.typography.labelLarge, color = OnSurfaceVariantLight)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Sales:", fontWeight = FontWeight.Medium)
                            Text("${String.format("%.2f", shiftSalesTotal)} MAD", fontWeight = FontWeight.Bold, color = Primary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Expected Cash:", fontWeight = FontWeight.Medium)
                            Text("${String.format("%.2f", expectedCash)} MAD", fontWeight = FontWeight.Bold, color = Tertiary)
                        }
                    }
                }
                OutlinedTextField(
                    value = actualCash,
                    onValueChange = {
                        actualCash = it.filter { c -> c.isDigit() || c == '.' }
                        cashError = null
                    },
                    label = { Text("Actual Cash Count (MAD)") },
                    isError = cashError != null,
                    supportingText = cashError?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = OutlineLight),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    placeholder = { Text("Any discrepancies or notes...") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = OutlineLight),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = actualCash.toDoubleOrNull()
                    if (amount == null || amount < 0) cashError = "Please enter a valid amount"
                    else onConfirm(amount, notes.ifBlank { null })
                },
                colors = ButtonDefaults.buttonColors(containerColor = Error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("End Shift", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CashierSellTicketDialog(
    event: Event,
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (com.example.eventpay.data.model.TicketType, com.example.eventpay.data.model.PaymentMethod, String, String?) -> Unit,
    getTicketPrice: (com.example.eventpay.data.model.TicketType) -> Double
) {
    var selectedTicketType by remember { mutableStateOf(com.example.eventpay.data.model.TicketType.STANDARD) }
    var selectedPaymentMethod by remember { mutableStateOf(com.example.eventpay.data.model.PaymentMethod.CASH) }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf(1) }
    val totalPrice = 0.0 // All tickets are free

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !isProcessing, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(PrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ConfirmationNumber, null, tint = Primary, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            "Sell Ticket",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = OnBackgroundLight
                        )
                        Text(
                            event.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariantLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    "Ticket Type",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurfaceVariantLight,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                com.example.eventpay.data.model.TicketType.values().forEach { type ->
                    val price = 0.0 // All tickets are free
                    val isSelected = selectedTicketType == type
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .selectable(selected = isSelected, onClick = { selectedTicketType = type })
                            .then(
                                if (isSelected) Modifier.border(1.5.dp, Primary, RoundedCornerShape(14.dp)) else Modifier
                            ),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) PrimaryContainer else SurfaceVariantLight
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedTicketType = type },
                                colors = RadioButtonDefaults.colors(selectedColor = Primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                type.name.replace("_", " "),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) PrimaryDark else OnBackgroundLight,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "Free",
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) Primary else OnSurfaceVariantLight
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Quantity",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceVariantLight
                    )
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceVariantLight
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            IconButton(
                                onClick = { if (quantity > 1) quantity-- },
                                enabled = quantity > 1,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Remove, null, modifier = Modifier.size(18.dp))
                            }
                            Text(
                                "$quantity",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            IconButton(
                                onClick = { if (quantity < 10) quantity++ },
                                enabled = quantity < 10,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Payment Method",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurfaceVariantLight,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple(com.example.eventpay.data.model.PaymentMethod.CASH, "Cash", Icons.Default.Money),
                        Triple(com.example.eventpay.data.model.PaymentMethod.CARD, "Card", Icons.Default.CreditCard),
                        Triple(com.example.eventpay.data.model.PaymentMethod.MOBILE_MONEY, "Mobile", Icons.Default.PhoneAndroid)
                    ).forEach { (method, _, icon) ->
                        FilterChip(
                            selected = selectedPaymentMethod == method,
                            onClick = { selectedPaymentMethod = method },
                            label = { Text(method.displayName(), fontWeight = FontWeight.SemiBold) },
                            leadingIcon = {
                                Icon(icon, null, modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Customer Name") },
                    leadingIcon = { Icon(Icons.Outlined.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = OutlineLight),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it },
                    label = { Text("Phone (optional)") },
                    leadingIcon = { Icon(Icons.Outlined.Phone, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                    ),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = OutlineLight),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PrimaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Total",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryDark
                        )
                        Text(
                            "Free",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineLight),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            if (customerName.isNotBlank()) {
                                onConfirm(
                                    selectedTicketType,
                                    selectedPaymentMethod,
                                    customerName,
                                    customerPhone.ifBlank { null }
                                )
                            }
                        },
                        enabled = customerName.isNotBlank() && !isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text("Confirm", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CashierQRCodeDialog(
    ticket: com.example.eventpay.data.model.Ticket,
    qrCode: String?,
    onDismiss: () -> Unit
) {
    val qrBitmap = remember(qrCode) {
        if (qrCode != null) generateQRCodeBitmap(qrCode) else null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(TertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = Tertiary, modifier = Modifier.size(36.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Ticket Sold!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = OnBackgroundLight
                )
                Text(
                    "Present this QR code at the entrance",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariantLight,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (qrBitmap != null) {
                    Surface(
                        shadowElevation = 6.dp,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceVariantLight),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceVariantLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CashierInfoRow("Ticket ID", ticket.id.take(8).uppercase())
                        HorizontalDivider(color = DividerLight)
                        CashierInfoRow("Type", ticket.ticketType.name)
                        HorizontalDivider(color = DividerLight)
                        CashierInfoRow("Price", "Free")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CashierInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariantLight)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = OnBackgroundLight)
    }
}

@Composable
private fun CashierDailyReportDialog(
    report: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Daily Report", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        },
        text = {
            Text(
                text = report,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = OnBackgroundLight
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

private fun generateQRCodeBitmap(content: String, size: Int = 512): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(
                    x, y,
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                )
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}
