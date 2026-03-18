package com.example.eventpay.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eventpay.domain.model.Event
import com.example.eventpay.domain.model.EventAnalytics
import com.example.eventpay.domain.model.SalesTrend
import com.example.eventpay.ui.dashboard.AnalyticsViewModel
import com.example.eventpay.ui.dashboard.DashboardUiState
import com.example.eventpay.ui.theme.*
import com.example.eventpay.ui.components.*
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
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 48.dp)) {

            item {
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
                        .padding(bottom = 52.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable { onNavigateBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Event Analytics",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            state.selectedEvent?.let {
                                Text(
                                    it.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable { viewModel.refresh() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.isRefreshing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, "Refresh", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            when {
                state.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = (-32).dp)
                                .padding(horizontal = 20.dp),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                repeat(3) {
                                    Box(modifier = Modifier.weight(1f).height(90.dp).clip(RoundedCornerShape(20.dp)).background(Zinc100))
                                }
                            }
                        }
                        Box(modifier = Modifier.fillMaxWidth().padding(80.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Primary, strokeWidth = 3.dp)
                        }
                    }
                }
                state.events.isEmpty() -> {
                    item { AnalyticsEmptyState(modifier = Modifier.fillMaxWidth().height(400.dp)) }
                }
                state.analytics == null -> {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = (-32).dp)
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PremiumStatTile(modifier = Modifier.weight(1f), label = "Events", value = "${state.events.size}", icon = Icons.Outlined.Event, gradient = listOf(Primary, AuroraViolet))
                            PremiumStatTile(modifier = Modifier.weight(1f), label = "Select Event", value = "→", icon = Icons.Outlined.TouchApp, gradient = listOf(Secondary, AuroraCyan))
                        }
                    }
                    item {
                        AnalyticsEventSelectionContent(
                            events = state.events,
                            selectedEventId = state.selectedEventId,
                            onEventSelected = { viewModel.selectEvent(it) }
                        )
                    }
                }
                else -> {
                    val analytics = state.analytics!!

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = (-32).dp)
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PremiumStatTile(
                                modifier = Modifier.weight(1f),
                                label = "Attendees",
                                value = "${analytics.totalAttendees}",
                                icon = Icons.Outlined.People,
                                gradient = listOf(Primary, AuroraViolet)
                            )
                            PremiumStatTile(
                                modifier = Modifier.weight(1f),
                                label = "Checked In",
                                value = "${analytics.checkedInCount}",
                                icon = Icons.Default.CheckCircle,
                                gradient = listOf(Tertiary, TertiaryDark)
                            )
                            PremiumStatTile(
                                modifier = Modifier.weight(1f),
                                label = "Revenue",
                                value = "${String.format("%.0f", analytics.totalRevenue)}",
                                icon = Icons.Outlined.Payments,
                                gradient = listOf(Accent, Color(0xFFD97706))
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        state.lastUpdated?.let { ts ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Schedule, null, tint = Zinc400, modifier = Modifier.size(13.dp))
                                Text(
                                    "Updated: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Zinc400
                                )
                            }
                        }
                    }

                    item {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            SectionHeader(title = "Check-in Progress", modifier = Modifier.padding(bottom = 12.dp))
                            AnalyticsCheckInCard(
                                checkedIn = analytics.checkedInCount,
                                total = analytics.totalAttendees,
                                percentage = analytics.checkInRate
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }

                    item {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            SectionHeader(title = "Revenue Analysis", modifier = Modifier.padding(bottom = 12.dp))
                            AnalyticsRevenueCard(
                                totalRevenue = analytics.totalRevenue,
                                averageTicketPrice = analytics.averageTicketPrice,
                                salesTrend = analytics.salesTrend
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsCheckInCard(checkedIn: Int, total: Int, percentage: Double) {
    val progressColor = when {
        percentage >= 75 -> Tertiary
        percentage >= 40 -> Accent
        else -> Error
    }
    val animPct by animateFloatAsState(
        targetValue = (percentage / 100).toFloat(),
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "checkIn"
    )

    SaaSCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Check-in Progress", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Zinc900)
                Surface(shape = RoundedCornerShape(20.dp), color = progressColor.copy(alpha = 0.1f)) {
                    Text(
                        "${String.format("%.1f", percentage)}%",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = progressColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(Zinc100)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animPct)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(progressColor, progressColor.copy(alpha = 0.65f))))
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Checked In", style = MaterialTheme.typography.labelSmall, color = Zinc400)
                    Text("$checkedIn", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = progressColor)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Remaining", style = MaterialTheme.typography.labelSmall, color = Zinc400)
                    Text("${total - checkedIn}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Zinc700)
                }
            }
        }
    }
}

@Composable
private fun AnalyticsRevenueCard(totalRevenue: Double, averageTicketPrice: Double, salesTrend: SalesTrend) {
    SaaSCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Total Revenue", style = MaterialTheme.typography.labelSmall, color = Zinc400)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${String.format("%.0f", totalRevenue)} MAD", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Primary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Avg. Ticket", style = MaterialTheme.typography.labelSmall, color = Zinc400)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${String.format("%.0f", averageTicketPrice)} MAD", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Zinc900)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                color = when (salesTrend) {
                    SalesTrend.INCREASING, SalesTrend.PEAK -> TertiaryContainer
                    SalesTrend.DECREASING -> ErrorContainer
                    SalesTrend.STABLE -> Zinc100
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = when (salesTrend) {
                            SalesTrend.INCREASING, SalesTrend.PEAK -> Icons.Default.TrendingUp
                            SalesTrend.DECREASING -> Icons.Default.TrendingDown
                            SalesTrend.STABLE -> Icons.Default.TrendingFlat
                        },
                        contentDescription = null,
                        tint = when (salesTrend) {
                            SalesTrend.INCREASING, SalesTrend.PEAK -> Tertiary
                            SalesTrend.DECREASING -> Error
                            SalesTrend.STABLE -> Zinc500
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        when (salesTrend) {
                            SalesTrend.INCREASING -> "Sales trending up"
                            SalesTrend.DECREASING -> "Sales trending down"
                            SalesTrend.PEAK -> "Sales at peak"
                            SalesTrend.STABLE -> "Sales are stable"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = when (salesTrend) {
                            SalesTrend.INCREASING, SalesTrend.PEAK -> Tertiary
                            SalesTrend.DECREASING -> Error
                            SalesTrend.STABLE -> Zinc500
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(24.dp)).background(PrimaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Analytics, null, modifier = Modifier.size(40.dp), tint = Primary.copy(alpha = 0.5f))
            }
            Text("No data available", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Zinc600)
            Text("Create events to see analytics", style = MaterialTheme.typography.bodySmall, color = Zinc400)
        }
    }
}

@Composable
private fun AnalyticsEventSelectionContent(
    events: List<Event>,
    selectedEventId: String?,
    onEventSelected: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        SectionHeader(title = "Select an Event", modifier = Modifier.padding(bottom = 16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            events.forEach { event ->
                val isSelected = event.id == selectedEventId
                SaaSCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEventSelected(event.id) },
                    elevation = if (isSelected) 4.dp else 1.dp,
                    containerColor = if (isSelected) Primary.copy(alpha = 0.05f) else SurfaceLight
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) Primary.copy(alpha = 0.12f) else Zinc100),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Event, null, tint = if (isSelected) Primary else Zinc400)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(event.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = if (isSelected) Primary else Zinc900)
                            Text(event.location, style = MaterialTheme.typography.bodySmall, color = Zinc400)
                        }
                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, null, tint = Primary, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.ChevronRight, null, tint = Zinc300, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
