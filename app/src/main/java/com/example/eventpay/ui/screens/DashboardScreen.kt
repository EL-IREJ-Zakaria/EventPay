package com.example.eventpay.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
    viewModel: DashboardViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
        visible = true
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundLight)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(GradientStart, GradientMid, PrimaryDark))
                        )
                        .statusBarsPadding()
                        .padding(bottom = 32.dp)
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
                        Text(
                            "Analytics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { viewModel.refreshData() },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    DashAnimatedEntrance(visible = visible, delay = 0) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(top = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Total Revenue",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.75f),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "${String.format("%.2f", state.stats.totalRevenue)} MAD",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.18f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.TrendingUp,
                                        null,
                                        tint = TertiaryLight,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        "All time earnings",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary, strokeWidth = 3.dp)
                    }
                }
            } else {
                item {
                    DashAnimatedEntrance(visible = visible, delay = 150) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = (-20).dp)
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            DashStatCard(
                                modifier = Modifier.weight(1f),
                                title = "Tickets Sold",
                                value = "${state.stats.totalTicketsSold}",
                                icon = Icons.Outlined.ConfirmationNumber,
                                color = Secondary
                            )
                            DashStatCard(
                                modifier = Modifier.weight(1f),
                                title = "Active Events",
                                value = "${state.stats.totalEvents}",
                                icon = Icons.Outlined.Event,
                                color = Tertiary
                            )
                        }
                    }
                }

                item {
                    DashAnimatedEntrance(visible = visible, delay = 250) {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            DashSectionHeader(title = "Today's Performance")
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = 4.dp,
                                        shape = RoundedCornerShape(20.dp),
                                        ambientColor = Primary.copy(0.06f)
                                    ),
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DashPerformanceItem(
                                        "New Tickets",
                                        "${state.stats.todayTickets}",
                                        Primary,
                                        Icons.Outlined.ConfirmationNumber
                                    )
                                    Box(
                                        modifier = Modifier
                                            .height(50.dp)
                                            .width(1.dp)
                                            .background(OutlineVariantLight)
                                    )
                                    DashPerformanceItem(
                                        "Today Revenue",
                                        "${String.format("%.0f", state.stats.todayRevenue)} MAD",
                                        Tertiary,
                                        Icons.Default.TrendingUp
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                item {
                    DashAnimatedEntrance(visible = visible, delay = 350) {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            DashSectionHeader(title = "Revenue Breakdown")
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = 4.dp,
                                        shape = RoundedCornerShape(20.dp),
                                        ambientColor = Primary.copy(0.06f)
                                    ),
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    verticalArrangement = Arrangement.spacedBy(20.dp)
                                ) {
                                    val maxRev = maxOf(
                                        state.revenueBreakdown.cashRevenue,
                                        state.revenueBreakdown.cardRevenue,
                                        state.revenueBreakdown.mobileRevenue,
                                        state.revenueBreakdown.walletRevenue,
                                        1.0
                                    )
                                    DashRevenueBar(
                                        "Cash",
                                        state.revenueBreakdown.cashRevenue,
                                        maxRev,
                                        Tertiary,
                                        Icons.Outlined.Payments
                                    )
                                    DashRevenueBar(
                                        "Card",
                                        state.revenueBreakdown.cardRevenue,
                                        maxRev,
                                        Primary,
                                        Icons.Outlined.CreditCard
                                    )
                                    DashRevenueBar(
                                        "Mobile",
                                        state.revenueBreakdown.mobileRevenue,
                                        maxRev,
                                        Secondary,
                                        Icons.Outlined.PhoneAndroid
                                    )
                                    DashRevenueBar(
                                        "Wallet",
                                        state.revenueBreakdown.walletRevenue,
                                        maxRev,
                                        Accent,
                                        Icons.Outlined.AccountBalanceWallet
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                item {
                    DashAnimatedEntrance(visible = visible, delay = 450) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Recent Transactions",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = OnBackgroundLight
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PrimaryContainer
                            ) {
                                Text(
                                    "See All",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                items(state.recentTransactions.take(5)) { transaction ->
                    DashAnimatedEntrance(visible = visible, delay = 550) {
                        DashTransactionCard(
                            transaction = transaction,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
        color = OnBackgroundLight
    )
}

@Composable
private fun DashAnimatedEntrance(
    visible: Boolean,
    delay: Int,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(500, delayMillis = delay)) +
                slideInVertically(tween(500, delayMillis = delay)) { it / 3 }
    ) {
        content()
    }
}

@Composable
private fun DashStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Surface(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = color.copy(0.1f),
                spotColor = color.copy(0.15f)
            ),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = OnBackgroundLight
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariantLight
            )
        }
    }
}

@Composable
private fun DashPerformanceItem(
    label: String,
    value: String,
    color: Color,
    icon: ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariantLight
        )
    }
}

@Composable
private fun DashRevenueBar(
    label: String,
    amount: Double,
    maxAmount: Double,
    color: Color,
    icon: ImageVector
) {
    val progress = (amount / maxAmount).coerceIn(0.0, 1.0).toFloat()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                }
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OnBackgroundLight
                )
            }
            Text(
                "${String.format("%.2f", amount)} MAD",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.1f)
        )
    }
}

@Composable
private fun DashTransactionCard(
    transaction: com.example.eventpay.data.model.Transaction,
    modifier: Modifier = Modifier
) {
    val isCredit = transaction.amount >= 0
    val iconBg = if (isCredit) TertiaryContainer else ErrorContainer
    val iconTint = if (isCredit) Tertiary else Error
    val amountColor = if (isCredit) Tertiary else Error

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(0.04f)
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OnBackgroundLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault())
                        .format(Date(transaction.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariantLight
                )
            }
            Text(
                text = "${if (isCredit) "+" else ""}${String.format("%.2f", transaction.amount)} MAD",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = amountColor
            )
        }
    }
}
