package com.example.eventpay.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eventpay.ui.components.*
import com.example.eventpay.ui.dashboard.DashboardViewModel
import com.example.eventpay.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateBack: () -> Unit,
    viewModel: DashboardViewModel
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadDashboardData() }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundLight)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
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
                        .padding(bottom = 48.dp)
                ) {
                    Column {
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
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                "Analytics",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .clickable { viewModel.refreshData() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Total Revenue",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.65f),
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "${String.format("%.2f", state.stats.totalRevenue)} MAD",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = (-0.5).sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            StatusBadge(label = "Live", color = Tertiary)
                        }
                    }
                }
            }

            if (state.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(80.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary, strokeWidth = 2.5.dp, modifier = Modifier.size(36.dp))
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-28).dp)
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PremiumStatTile(
                            modifier = Modifier.weight(1f),
                            label = "Tickets Sold",
                            value = "${state.stats.totalTicketsSold}",
                            icon = Icons.Outlined.ConfirmationNumber,
                            gradient = listOf(Secondary, AuroraCyan)
                        )
                        PremiumStatTile(
                            modifier = Modifier.weight(1f),
                            label = "Active Events",
                            value = "${state.stats.totalEvents}",
                            icon = Icons.Outlined.Event,
                            gradient = listOf(Tertiary, TertiaryDark)
                        )
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .offset(y = (-12).dp)
                    ) {
                        SectionHeader(title = "Today's Performance", modifier = Modifier.padding(bottom = 12.dp))

                        SaaSCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TodayPerformanceItem(
                                    label = "New Tickets",
                                    value = "${state.stats.todayTickets}",
                                    color = Primary,
                                    icon = Icons.Outlined.ConfirmationNumber
                                )
                                Box(
                                    modifier = Modifier
                                        .height(48.dp)
                                        .width(1.dp)
                                        .background(Zinc100)
                                )
                                TodayPerformanceItem(
                                    label = "Revenue",
                                    value = "${String.format("%.0f", state.stats.todayRevenue)} MAD",
                                    color = Tertiary,
                                    icon = Icons.Default.TrendingUp
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SectionHeader(title = "Revenue by Method", modifier = Modifier.padding(bottom = 12.dp))

                        SaaSCard {
                            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                val maxRev = maxOf(
                                    state.revenueBreakdown.cashRevenue,
                                    state.revenueBreakdown.cardRevenue,
                                    state.revenueBreakdown.mobileRevenue,
                                    state.revenueBreakdown.walletRevenue,
                                    1.0
                                )
                                RevenueBar("Cash", state.revenueBreakdown.cashRevenue, maxRev, Tertiary, Icons.Outlined.Payments)
                                RevenueBar("Card", state.revenueBreakdown.cardRevenue, maxRev, Primary, Icons.Outlined.CreditCard)
                                RevenueBar("Mobile", state.revenueBreakdown.mobileRevenue, maxRev, Secondary, Icons.Outlined.PhoneAndroid)
                                RevenueBar("Wallet", state.revenueBreakdown.walletRevenue, maxRev, Accent, Icons.Outlined.AccountBalanceWallet)
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                item {
                    SectionHeader(
                        title = "Recent Transactions",
                        onAction = {},
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(state.recentTransactions.take(5)) { transaction ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)) {
                        RecentTransactionCard(transaction)
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayPerformanceItem(
    label: String,
    value: String,
    color: Color,
    icon: ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Zinc900)
        Text(label, style = MaterialTheme.typography.labelMedium, color = Zinc500)
    }
}

@Composable
private fun RevenueBar(
    label: String,
    value: Double,
    max: Double,
    color: Color,
    icon: ImageVector
) {
    val progress = (value / max).toFloat().coerceIn(0.04f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "revenueBar"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
                }
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Zinc700)
            }
            Text(
                "${String.format("%.0f", value)} MAD",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Zinc900
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(CircleShape)
                .background(Zinc100)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.6f)))
                    )
            )
        }
    }
}

@Composable
private fun RecentTransactionCard(transaction: com.example.eventpay.data.model.Transaction) {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    SaaSCard(padding = 14.dp, cornerRadius = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(listOf(GradientStart, AuroraBlue))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    transaction.description.ifEmpty { "Transaction" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Zinc900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    sdf.format(Date(transaction.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = Zinc400
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${String.format("%.2f", transaction.amount)} MAD",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (transaction.amount > 0) Tertiary else Error
                )
                Spacer(modifier = Modifier.height(3.dp))
                StatusBadge(
                    label = if (transaction.amount > 0) "Credit" else "Debit",
                    color = if (transaction.amount > 0) Tertiary else Error
                )
            }
        }
    }
}
