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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eventpay.data.model.Transaction
import com.example.eventpay.data.model.TransactionType
import com.example.eventpay.ui.components.*
import com.example.eventpay.ui.theme.*
import com.example.eventpay.ui.wallet.WalletViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    userId: String,
    walletViewModel: WalletViewModel,
    onBack: () -> Unit
) {
    var showTopUpSheet by remember { mutableStateOf(false) }
    var topUpAmount by remember { mutableStateOf("") }
    var isEntered by remember { mutableStateOf(false) }

    val walletState by walletViewModel.walletState.collectAsState()

    LaunchedEffect(userId) {
        walletViewModel.loadWallet(userId)
        walletViewModel.loadTransactions(userId)
        isEntered = true
    }

    val balanceSlide by animateFloatAsState(
        targetValue = if (isEntered) 0f else -60f,
        animationSpec = tween(550, easing = FastOutSlowInEasing),
        label = "balanceSlide"
    )
    val balanceAlpha by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0f,
        animationSpec = tween(550, easing = FastOutSlowInEasing),
        label = "balanceAlpha"
    )

    Box(modifier = Modifier.fillMaxSize().background(BackgroundLight)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(GradientStart, GradientMid, PrimaryDark))
                        )
                        .statusBarsPadding()
                        .padding(bottom = 40.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onBack,
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
                                "My Wallet",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Box(modifier = Modifier.size(44.dp))
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    translationY = balanceSlide
                                    alpha = balanceAlpha
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Total Balance",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.72f),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${String.format("%.2f", walletState.walletBalance)} MAD",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-28).dp)
                        .padding(horizontal = 20.dp)
                ) {
                    WalletActionRow(
                        onTopUp = { showTopUpSheet = true }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Transaction History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = OnBackgroundLight
                    )
                    if (walletState.transactions.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryContainer
                        ) {
                            Text(
                                "${walletState.transactions.size} records",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (walletState.transactions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(PrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Receipt,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Primary
                            )
                        }
                        Text(
                            "No Transactions Yet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = OnBackgroundLight
                        )
                        Text(
                            "Your transaction history will appear here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariantLight,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(walletState.transactions) { transaction ->
                    WalletTransactionCard(
                        transaction = transaction,
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 5.dp)
                    )
                }
            }

            if (walletState.error != null) {
                item {
                    Surface(
                        color = ErrorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, null, tint = Error, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                walletState.error!!,
                                color = ErrorDark,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTopUpSheet) {
        TopUpBottomSheet(
            amount = topUpAmount,
            onAmountChange = { topUpAmount = it.filter { c -> c.isDigit() || c == '.' } },
            onConfirm = {
                val amount = topUpAmount.toDoubleOrNull()
                if (amount != null && amount > 0) {
                    walletViewModel.topUpWallet(userId, amount)
                    showTopUpSheet = false
                    topUpAmount = ""
                }
            },
            onDismiss = {
                showTopUpSheet = false
                topUpAmount = ""
            },
            isLoading = walletState.isLoading
        )
    }
}

@Composable
private fun WalletActionRow(onTopUp: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Primary.copy(alpha = 0.12f),
                spotColor = Primary.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WalletActionItem(
                icon = Icons.Default.AddCard,
                label = "Top Up",
                containerColor = PrimaryContainer,
                iconTint = Primary,
                onClick = onTopUp
            )
            WalletActionDivider()
            WalletActionItem(
                icon = Icons.Outlined.Send,
                label = "Send",
                containerColor = SecondaryContainer,
                iconTint = Secondary,
                onClick = {}
            )
            WalletActionDivider()
            WalletActionItem(
                icon = Icons.Outlined.History,
                label = "History",
                containerColor = TertiaryContainer,
                iconTint = Tertiary,
                onClick = {}
            )
        }
    }
}

@Composable
private fun WalletActionItem(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(26.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = OnSurfaceVariantLight
        )
    }
}

@Composable
private fun WalletActionDivider() {
    Box(
        modifier = Modifier
            .height(44.dp)
            .width(1.dp)
            .background(OutlineVariantLight)
    )
}

@Composable
fun WalletTransactionCard(
    transaction: Transaction,
    modifier: Modifier = Modifier
) {
    val isCredit = transaction.type == TransactionType.WALLET_TOP_UP ||
            transaction.type == TransactionType.REFUND
    val iconBg = if (isCredit) TertiaryContainer else ErrorContainer
    val iconTint = if (isCredit) Tertiary else Error
    val amountColor = if (isCredit) Tertiary else Error
    val amountText = if (isCredit)
        "+${String.format("%.2f", transaction.amount)} MAD"
    else
        "-${String.format("%.2f", transaction.amount)} MAD"

    val icon = when (transaction.type) {
        TransactionType.WALLET_TOP_UP -> Icons.Default.AddCard
        TransactionType.TICKET_PURCHASE -> Icons.Outlined.ConfirmationNumber
        TransactionType.REFUND -> Icons.Default.Refresh
        TransactionType.CASHIER_SALE -> Icons.Outlined.PointOfSale
        TransactionType.MERCHANDISE_PURCHASE -> Icons.Outlined.ShoppingCart
    }

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
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OnBackgroundLight,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault())
                        .format(Date(transaction.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariantLight
                )
            }
            Text(
                text = amountText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = amountColor
            )
        }
    }
}

@Composable
private fun TopUpBottomSheet(
    amount: String,
    onAmountChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val quickAmounts = listOf("50", "100", "200", "500")

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it },
        exit = fadeOut() + slideOutVertically { it }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .clickable(enabled = false) {}
                    .padding(horizontal = 28.dp, vertical = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 20.dp)
                        .size(width = 44.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(OutlineVariantLight)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(PrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AddCard, null, tint = Primary, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            "Top Up Wallet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = OnBackgroundLight
                        )
                        Text(
                            "Add funds to your EventPay wallet",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariantLight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Quick Select",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurfaceVariantLight
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    quickAmounts.forEach { q ->
                        val isSelected = amount == q
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onAmountChange(q) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Primary else SurfaceVariantLight
                        ) {
                            Text(
                                "$q MAD",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else OnSurfaceVariantLight
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                PremiumTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    label = "Custom Amount (MAD)",
                    leadingIcon = Icons.Outlined.Payments,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                AnimatedGradientButton(
                    onClick = onConfirm,
                    text = "Confirm Top Up",
                    icon = Icons.Default.Check,
                    enabled = amount.toDoubleOrNull()?.let { it > 0 } ?: false && !isLoading,
                    isLoading = isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = OnSurfaceVariantLight, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
