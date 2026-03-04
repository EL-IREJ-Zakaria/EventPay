package com.example.eventpay.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eventpay.domain.model.Event
import com.example.eventpay.domain.model.EventAnalytics
import com.example.eventpay.domain.model.PaymentMethod
import com.example.eventpay.domain.model.TicketType
import com.example.eventpay.domain.model.HourlyCheckIn
import com.example.eventpay.domain.model.HourlySales
import com.example.eventpay.domain.model.PeakEntryTime
import com.example.eventpay.domain.model.SalesTrend
import com.example.eventpay.ui.dashboard.AnalyticsViewModel
import com.example.eventpay.ui.dashboard.DashboardUiState
import com.example.eventpay.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsDashboardScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(BackgroundLight)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(GradientStart, GradientMid, PrimaryDark))
                    )
                    .statusBarsPadding()
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
                            "Analytics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        state.selectedEvent?.let { event ->
                            Text(
                                event.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.72f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { viewModel.toggleAutoRefresh() },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (state.autoRefreshEnabled)
                                        Color.White.copy(alpha = 0.28f)
                                    else
                                        Color.White.copy(alpha = 0.15f)
                                )
                        ) {
                            Icon(
                                if (state.autoRefreshEnabled) Icons.Default.Sync else Icons.Default.SyncDisabled,
                                contentDescription = "Auto-refresh",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.refresh() },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            if (state.isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            CircularProgressIndicator(color = Primary, strokeWidth = 3.dp, modifier = Modifier.size(48.dp))
                            Text("Loading analytics...", color = OnSurfaceVariantLight, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                state.events.isEmpty() -> {
                    AnalyticsEmptyState(modifier = Modifier.fillMaxSize())
                }
                state.analytics == null -> {
                    AnalyticsEventSelectionContent(
                        events = state.events,
                        selectedEventId = state.selectedEventId,
                        onEventSelected = { viewModel.selectEvent(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    AnalyticsContent(
                        state = state,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsContent(
    state: DashboardUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val analytics = state.analytics!!

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AnalyticsLastUpdatedRow(lastUpdated = state.lastUpdated, isRefreshing = state.isRefreshing)
        }
        item {
            AnalyticsKeyMetricsRow(analytics = analytics)
        }
        item {
            AnalyticsCheckInProgressCard(
                checkedIn = analytics.checkedInCount,
                total = analytics.totalAttendees,
                percentage = analytics.checkInRate
            )
        }
        item {
            AnalyticsRevenueCard(
                totalRevenue = analytics.totalRevenue,
                averageTicketPrice = analytics.averageTicketPrice,
                salesTrend = analytics.salesTrend
            )
        }
        item {
            AnalyticsPeakEntryCard(
                peakEntryTime = analytics.peakEntryTime,
                entryVelocity = analytics.entryVelocity
            )
        }
        item {
            AnalyticsHourlyChart(hourlyCheckIns = analytics.hourlyCheckIns)
        }
        item {
            AnalyticsTicketBreakdownCard(ticketTypeBreakdown = analytics.ticketTypeBreakdown)
        }
        item {
            AnalyticsPaymentBreakdownCard(paymentMethodBreakdown = analytics.paymentMethodBreakdown)
        }
        item {
            AnalyticsRealTimeCard(
                liveCheckIns = analytics.liveCheckIns,
                checkInsLast15Minutes = analytics.checkInsLast15Minutes,
                checkInsLastHour = analytics.checkInsLastHour,
                lastCheckInTime = analytics.lastCheckInTime
            )
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun AnalyticsLastUpdatedRow(lastUpdated: Long?, isRefreshing: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.Schedule, null, tint = OnSurfaceVariantLight, modifier = Modifier.size(14.dp))
            Text(
                text = if (lastUpdated != null)
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastUpdated))
                else "Not updated",
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariantLight
            )
        }
        if (isRefreshing) {
            Surface(shape = RoundedCornerShape(8.dp), color = PrimaryContainer) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Primary, strokeWidth = 1.5.dp)
                    Text("Updating...", style = MaterialTheme.typography.labelSmall, color = Primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AnalyticsKeyMetricsRow(analytics: EventAnalytics) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            AnalyticsMetricCard(
                title = "Total Attendees",
                value = analytics.totalAttendees.toString(),
                icon = Icons.Outlined.People,
                color = Tertiary,
                containerColor = TertiaryContainer
            )
        }
        item {
            AnalyticsMetricCard(
                title = "Checked In",
                value = analytics.checkedInCount.toString(),
                icon = Icons.Default.CheckCircle,
                color = Primary,
                containerColor = PrimaryContainer
            )
        }
        item {
            AnalyticsMetricCard(
                title = "Revenue",
                value = "${String.format("%.0f", analytics.totalRevenue)} MAD",
                icon = Icons.Outlined.Payments,
                color = Accent,
                containerColor = WarningContainer
            )
        }
        item {
            AnalyticsMetricCard(
                title = "Occupancy",
                value = "${String.format("%.1f", analytics.occupancyRate())}%",
                icon = Icons.Default.EventSeat,
                color = Secondary,
                containerColor = SecondaryContainer
            )
        }
    }
}

@Composable
private fun AnalyticsMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    containerColor: Color
) {
    Surface(
        modifier = Modifier
            .width(148.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = color.copy(0.1f)),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = OnBackgroundLight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariantLight
            )
        }
    }
}

@Composable
private fun AnalyticsCheckInProgressCard(
    checkedIn: Int,
    total: Int,
    percentage: Double
) {
    val progressColor = when {
        percentage >= 75 -> Tertiary
        percentage >= 50 -> Accent
        else -> Error
    }
    val progressBg = when {
        percentage >= 75 -> TertiaryContainer
        percentage >= 50 -> WarningContainer
        else -> ErrorContainer
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = Primary.copy(0.06f)),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Check-in Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OnBackgroundLight
                )
                Surface(shape = RoundedCornerShape(20.dp), color = progressBg) {
                    Text(
                        "${String.format("%.1f", percentage)}%",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = progressColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(progressBg)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((percentage / 100).toFloat().coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.horizontalGradient(listOf(progressColor, progressColor.copy(0.7f))))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Tertiary))
                    Text("$checkedIn checked in", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariantLight)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(OutlineLight))
                    Text("${total - checkedIn} remaining", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariantLight)
                }
            }
        }
    }
}

@Composable
private fun AnalyticsRevenueCard(
    totalRevenue: Double,
    averageTicketPrice: Double,
    salesTrend: SalesTrend
) {
    val trendColor = when (salesTrend) {
        SalesTrend.INCREASING, SalesTrend.PEAK -> Tertiary
        SalesTrend.DECREASING -> Error
        SalesTrend.STABLE -> OnSurfaceVariantLight
    }
    val trendBg = when (salesTrend) {
        SalesTrend.INCREASING, SalesTrend.PEAK -> TertiaryContainer
        SalesTrend.DECREASING -> ErrorContainer
        SalesTrend.STABLE -> SurfaceVariantLight
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp), ambientColor = Primary.copy(0.1f), spotColor = Primary.copy(0.15f)),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.verticalGradient(listOf(PrimaryContainer.copy(0.5f), Color.Transparent)))
            )
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            "Total Revenue",
                            style = MaterialTheme.typography.labelLarge,
                            color = OnSurfaceVariantLight,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${String.format("%.2f", totalRevenue)} MAD",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Primary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Surface(shape = RoundedCornerShape(14.dp), color = trendBg) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(salesTrend.icon(), style = MaterialTheme.typography.titleMedium)
                            Text(
                                salesTrend.displayName(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = trendColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = DividerLight)
                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    Column {
                        Text("Avg Ticket", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariantLight)
                        Text(
                            "${String.format("%.2f", averageTicketPrice)} MAD",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = OnBackgroundLight
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsPeakEntryCard(
    peakEntryTime: PeakEntryTime?,
    entryVelocity: Double
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = Primary.copy(0.06f)),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                "Peak Entry Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = OnBackgroundLight
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(PrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Schedule, null, tint = Primary, modifier = Modifier.size(26.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        peakEntryTime?.formattedTime() ?: "—",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = OnBackgroundLight
                    )
                    Text("Peak Time", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariantLight)
                    peakEntryTime?.let {
                        Surface(shape = RoundedCornerShape(8.dp), color = PrimaryContainer) {
                            Text(
                                "${it.checkInCount} entries",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Box(modifier = Modifier.height(80.dp).width(1.dp).background(OutlineVariantLight))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(WarningContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Speed, null, tint = AccentDark, modifier = Modifier.size(26.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        String.format("%.1f", entryVelocity),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = OnBackgroundLight
                    )
                    Text("Entry Velocity", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariantLight)
                    Surface(shape = RoundedCornerShape(8.dp), color = WarningContainer) {
                        Text(
                            "entries/min",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsHourlyChart(hourlyCheckIns: List<HourlyCheckIn>) {
    val filtered = hourlyCheckIns.filter { it.checkInCount > 0 }.take(12)
    val maxCount = filtered.maxOfOrNull { it.checkInCount }?.coerceAtLeast(1) ?: 1

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = Primary.copy(0.06f)),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Hourly Check-ins",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OnBackgroundLight
                )
                Surface(shape = RoundedCornerShape(8.dp), color = PrimaryContainer) {
                    Text(
                        "${filtered.sumOf { it.checkInCount }} total",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No check-in data yet", color = OnSurfaceVariantLight, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().height(130.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    filtered.forEach { hourData ->
                        val fraction = hourData.checkInCount.toFloat() / maxCount
                        val barHeightDp = (fraction * 100f).coerceAtLeast(4f)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (fraction == 1f) {
                                Text(
                                    "${hourData.checkInCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(barHeightDp.dp)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        if (fraction == 1f)
                                            Brush.verticalGradient(listOf(GradientStart, GradientMid))
                                        else
                                            Brush.verticalGradient(listOf(Primary.copy(0.8f), Primary.copy(0.4f)))
                                    )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "${hourData.hour}h",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariantLight,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsTicketBreakdownCard(
    ticketTypeBreakdown: Map<TicketType, com.example.eventpay.domain.model.TicketTypeStats>
) {
    val totalSold = ticketTypeBreakdown.values.sumOf { it.sold }.coerceAtLeast(1)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = Primary.copy(0.06f)),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                "Ticket Type Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = OnBackgroundLight
            )
            Spacer(modifier = Modifier.height(16.dp))
            ticketTypeBreakdown.entries.forEach { (type, stats) ->
                val color = getAnalyticsTicketColor(type)
                val fraction = stats.sold.toFloat() / totalSold

                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Text(
                                type.name.replace("_", " "),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = OnBackgroundLight
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${stats.sold} sold",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = OnBackgroundLight
                            )
                            Text(
                                "${String.format("%.0f", stats.revenue)} MAD",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariantLight
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color.copy(alpha = 0.12f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(color)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsPaymentBreakdownCard(
    paymentMethodBreakdown: Map<PaymentMethod, com.example.eventpay.domain.model.PaymentStats>
) {
    val totalAmount = paymentMethodBreakdown.values.sumOf { it.totalAmount }.coerceAtLeast(0.01)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = Primary.copy(0.06f)),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                "Payment Methods",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = OnBackgroundLight
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (paymentMethodBreakdown.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    Text("No payment data yet", color = OnSurfaceVariantLight, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                paymentMethodBreakdown.entries.forEach { (method, stats) ->
                    val color = getAnalyticsPaymentColor(method)
                    val icon = getAnalyticsPaymentIcon(method)
                    val fraction = (stats.totalAmount / totalAmount).toFloat()

                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(color.copy(0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    method.displayName(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnBackgroundLight
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${String.format("%.0f", stats.totalAmount)} MAD",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = OnBackgroundLight
                                )
                                Text(
                                    "${stats.transactionCount} txns",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnSurfaceVariantLight
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color.copy(alpha = 0.1f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(color)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsRealTimeCard(
    liveCheckIns: Int,
    checkInsLast15Minutes: Int,
    checkInsLastHour: Int,
    lastCheckInTime: Long?
) {
    val infiniteTransition = rememberInfiniteTransition(label = "livePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp), ambientColor = Tertiary.copy(0.12f), spotColor = Tertiary.copy(0.18f)),
        shape = RoundedCornerShape(20.dp),
        color = TertiaryContainer.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Live Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OnBackgroundLight
                )
                Surface(shape = RoundedCornerShape(20.dp), color = TertiaryContainer) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Tertiary.copy(alpha = pulseAlpha))
                        )
                        Text(
                            "LIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = TertiaryDark,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AnalyticsRealTimeStatItem("Last 15 min", checkInsLast15Minutes.toString(), Primary)
                Box(modifier = Modifier.height(50.dp).width(1.dp).background(OutlineVariantLight))
                AnalyticsRealTimeStatItem("Last Hour", checkInsLastHour.toString(), Secondary)
                Box(modifier = Modifier.height(50.dp).width(1.dp).background(OutlineVariantLight))
                AnalyticsRealTimeStatItem("Live Total", liveCheckIns.toString(), Tertiary)
            }

            lastCheckInTime?.let { time ->
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = TertiaryContainer)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AccessTime, null, tint = OnSurfaceVariantLight, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Last check-in: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(time))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariantLight
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsRealTimeStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariantLight
        )
    }
}

@Composable
private fun AnalyticsEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(PrimaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.EventBusy,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = Primary
            )
        }
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            "No Events Found",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = OnBackgroundLight
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Create an event to start seeing analytics data",
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceVariantLight,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AnalyticsEventSelectionContent(
    events: List<Event>,
    selectedEventId: String?,
    onEventSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    "Select an Event",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = OnBackgroundLight
                )
                Text(
                    "Choose an event to view its analytics",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariantLight
                )
            }
        }
        items(events) { event ->
            val isSelected = selectedEventId == event.id
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = if (isSelected) 6.dp else 2.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = if (isSelected) Primary.copy(0.12f) else Color.Black.copy(0.04f)
                    )
                    .then(
                        if (isSelected) Modifier.border(2.dp, Primary, RoundedCornerShape(20.dp)) else Modifier
                    ),
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) PrimaryContainer else MaterialTheme.colorScheme.surface,
                onClick = { onEventSelected(event.id) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) Primary else PrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Event,
                            null,
                            tint = if (isSelected) Color.White else Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            event.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) PrimaryDark else OnBackgroundLight,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Outlined.LocationOn, null, tint = OnSurfaceVariantLight, modifier = Modifier.size(12.dp))
                            Text(event.location, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariantLight, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(shape = RoundedCornerShape(8.dp), color = if (isSelected) Primary.copy(0.12f) else SurfaceVariantLight) {
                                Text(
                                    "${event.soldTickets}/${event.totalTickets} sold",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) PrimaryDark else OnSurfaceVariantLight,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Surface(shape = RoundedCornerShape(8.dp), color = if (isSelected) TertiaryContainer else SurfaceVariantLight) {
                                Text(
                                    "${event.ticketPrice.toInt()} MAD",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) TertiaryDark else OnSurfaceVariantLight,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    if (isSelected) {
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape).background(Primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

private fun getAnalyticsTicketColor(type: TicketType): Color {
    return when (type) {
        TicketType.STANDARD -> StandardTicketColor
        TicketType.VIP -> VIPTicketColor
        TicketType.PREMIUM -> PremiumTicketColor
        TicketType.EARLY_BIRD -> EarlyBirdTicketColor
        TicketType.STUDENT -> StudentTicketColor
        TicketType.GROUP -> GroupTicketColor
        TicketType.PASS -> PassTicketColor
    }
}

@Composable
private fun getAnalyticsPaymentIcon(method: PaymentMethod): ImageVector {
    return when (method) {
        PaymentMethod.CASH -> Icons.Default.Money
        PaymentMethod.CARD -> Icons.Default.CreditCard
        PaymentMethod.MOBILE_MONEY -> Icons.Default.PhoneAndroid
        PaymentMethod.WALLET -> Icons.Outlined.AccountBalanceWallet
        PaymentMethod.BANK_TRANSFER -> Icons.Default.AccountBalance
        PaymentMethod.CASHIER_SALE -> Icons.Default.PointOfSale
    }
}

private fun getAnalyticsPaymentColor(method: PaymentMethod): Color {
    return when (method) {
        PaymentMethod.CASH -> Tertiary
        PaymentMethod.CARD -> Primary
        PaymentMethod.MOBILE_MONEY -> Accent
        PaymentMethod.WALLET -> Secondary
        PaymentMethod.BANK_TRANSFER -> Color(0xFF607D8B)
        PaymentMethod.CASHIER_SALE -> Color(0xFFE91E63)
    }
}
