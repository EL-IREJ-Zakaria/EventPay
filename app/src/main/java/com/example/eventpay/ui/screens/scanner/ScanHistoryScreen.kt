package com.example.eventpay.ui.screens.scanner

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.eventpay.domain.model.CheckInRecord
import com.example.eventpay.domain.model.CheckInResult
import com.example.eventpay.domain.model.displayText
import com.example.eventpay.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanHistoryScreen(
    checkIns: List<CheckInRecord>,
    isLoading: Boolean,
    onBack: () -> Unit
) {
    var filterType by remember { mutableStateOf<CheckInResult?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredCheckIns = remember(checkIns, filterType, searchQuery) {
        checkIns.filter { checkIn ->
            val matchesFilter = filterType == null || checkIn.result == filterType
            val matchesSearch = searchQuery.isBlank() ||
                checkIn.ticketId.contains(searchQuery, ignoreCase = true) ||
                checkIn.userId.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    val successCount = checkIns.count { it.result == CheckInResult.SUCCESS }
    val alreadyScannedCount = checkIns.count { it.result == CheckInResult.ALREADY_SCANNED }
    val failedCount = checkIns.size - successCount
    val successRate = if (checkIns.isNotEmpty()) (successCount.toFloat() / checkIns.size) * 100 else 0f

    Box(modifier = Modifier.fillMaxSize().background(BackgroundLight)) {
        Column(modifier = Modifier.fillMaxSize()) {

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
                    .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Scan History",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            "${checkIns.size} total scan${if (checkIns.size != 1) "s" else ""} recorded",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HistoryStatCard(
                    title = "Successful",
                    value = successCount.toString(),
                    icon = Icons.Outlined.CheckCircle,
                    gradientColors = listOf(Tertiary, TertiaryDark),
                    modifier = Modifier.weight(1f)
                )
                HistoryStatCard(
                    title = "Duplicate",
                    value = alreadyScannedCount.toString(),
                    icon = Icons.Outlined.Warning,
                    gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
                    modifier = Modifier.weight(1f)
                )
                HistoryStatCard(
                    title = "Success Rate",
                    value = "${successRate.toInt()}%",
                    icon = Icons.Outlined.TrendingUp,
                    gradientColors = listOf(Primary, PrimaryDark),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(52.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Primary.copy(alpha = 0.08f))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Search, null, tint = Primary, modifier = Modifier.size(20.dp))
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "Search by ticket ID…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Zinc400
                        )
                    },
                    singleLine = true,
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
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                if (searchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Zinc100)
                            .clickable { searchQuery = "" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, null, tint = Zinc500, modifier = Modifier.size(14.dp))
                    }
                }
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val chips = listOf(
                    Triple(null, "All", checkIns.size),
                    Triple(CheckInResult.SUCCESS, "Success", successCount),
                    Triple(CheckInResult.ALREADY_SCANNED, "Duplicate", alreadyScannedCount),
                    Triple(CheckInResult.INVALID, "Invalid", checkIns.count { it.result == CheckInResult.INVALID }),
                    Triple(CheckInResult.NOT_FOUND, "Not Found", checkIns.count { it.result == CheckInResult.NOT_FOUND }),
                    Triple(CheckInResult.EXPIRED, "Expired", checkIns.count { it.result == CheckInResult.EXPIRED })
                ).filter { (key, _, count) -> key == null || count > 0 }

                items(chips) { (type, label, count) ->
                    val isSelected = filterType == type
                    val chipColor by animateColorAsState(
                        targetValue = if (isSelected) Primary else Color.Transparent,
                        animationSpec = tween(200),
                        label = "chipColor"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else OnSurfaceVariantLight,
                        animationSpec = tween(200),
                        label = "textColor"
                    )
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = chipColor,
                        modifier = Modifier
                            .shadow(
                                elevation = if (isSelected) 4.dp else 0.dp,
                                shape = RoundedCornerShape(20.dp),
                                spotColor = Primary.copy(alpha = 0.3f)
                            )
                            .border(
                                width = if (isSelected) 0.dp else 1.dp,
                                color = if (isSelected) Color.Transparent else OutlineLight,
                                shape = RoundedCornerShape(20.dp)
                            ),
                        onClick = { filterType = type }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = textColor
                            )
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) Color.White.copy(alpha = 0.2f)
                                        else OutlineLight.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    "$count",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else OnSurfaceVariantLight
                                )
                            }
                        }
                    }
                }
            }

            when {
                isLoading -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(5) { index ->
                            CheckInCardShimmer(index)
                        }
                    }
                }
                filteredCheckIns.isEmpty() -> {
                    HistoryEmptyState(
                        hasSearch = searchQuery.isNotEmpty() || filterType != null,
                        onClear = { searchQuery = ""; filterType = null }
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 4.dp,
                            bottom = 32.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredCheckIns, key = { it.id }) { checkIn ->
                            var visible by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                kotlinx.coroutines.delay(40L)
                                visible = true
                            }
                            AnimatedVisibility(
                                visible = visible,
                                enter = fadeIn(tween(220)) + slideInVertically(
                                    initialOffsetY = { it / 5 },
                                    animationSpec = tween(260)
                                )
                            ) {
                                PremiumCheckInCard(checkIn)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.shadow(
            elevation = 12.dp,
            shape = RoundedCornerShape(20.dp),
            spotColor = gradientColors.first().copy(alpha = 0.25f),
            ambientColor = gradientColors.first().copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceLight
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(gradientColors.first().copy(alpha = 0.08f), gradientColors.last().copy(alpha = 0.03f))
                    ),
                    RoundedCornerShape(20.dp)
                )
                .padding(14.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(gradientColors)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = gradientColors.first()
                )
                Text(
                    title,
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariantLight,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PremiumCheckInCard(checkIn: CheckInRecord) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }

    val (accentColor, bgColor, icon, badgeText) = when (checkIn.result) {
        CheckInResult.SUCCESS -> CheckInStyle(
            accent = Tertiary,
            bg = TertiaryContainer,
            icon = Icons.Default.CheckCircle,
            badge = "SUCCESS"
        )
        CheckInResult.ALREADY_SCANNED -> CheckInStyle(
            accent = Color(0xFFF59E0B),
            bg = Color(0xFFFEF3C7),
            icon = Icons.Default.Warning,
            badge = "DUPLICATE"
        )
        CheckInResult.INVALID -> CheckInStyle(
            accent = Error,
            bg = ErrorContainer,
            icon = Icons.Default.Cancel,
            badge = "INVALID"
        )
        CheckInResult.NOT_FOUND -> CheckInStyle(
            accent = Error,
            bg = ErrorContainer,
            icon = Icons.Default.SearchOff,
            badge = "NOT FOUND"
        )
        CheckInResult.EXPIRED -> CheckInStyle(
            accent = Color(0xFF9B59B6),
            bg = Color(0xFFF3E8FF),
            icon = Icons.Outlined.TimerOff,
            badge = "EXPIRED"
        )
        CheckInResult.WRONG_EVENT -> CheckInStyle(
            accent = Color(0xFF3498DB),
            bg = Color(0xFFEBF5FF),
            icon = Icons.Outlined.EventBusy,
            badge = "WRONG EVENT"
        )
        else -> CheckInStyle(
            accent = OnSurfaceVariantLight,
            bg = SurfaceVariantLight,
            icon = Icons.Outlined.Info,
            badge = checkIn.result.name
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = accentColor.copy(alpha = 0.12f),
                ambientColor = Color.Black.copy(alpha = 0.04f)
            ),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceLight
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(listOf(accentColor, accentColor.copy(alpha = 0.5f))),
                        RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
                    )
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(accentColor.copy(alpha = 0.15f), bgColor))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        checkIn.result.displayText(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = OnSurfaceLight
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.ConfirmationNumber,
                            null,
                            tint = OnSurfaceVariantLight,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            checkIn.ticketId.take(16) + if (checkIn.ticketId.length > 16) "…" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariantLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Schedule,
                            null,
                            tint = OnSurfaceVariantLight,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            "${dateFormat.format(Date(checkIn.scannedAt))} • ${timeFormat.format(Date(checkIn.scannedAt))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariantLight
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        badgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

private data class CheckInStyle(
    val accent: Color,
    val bg: Color,
    val icon: ImageVector,
    val badge: String
)

@Composable
private fun CheckInCardShimmer(index: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 70L)
        visible = true
    }
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(200))) {
        Surface(
            modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            color = SurfaceLight
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(72.dp)
                        .background(ShimmerLight.copy(alpha = shimmerAlpha))
                )
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(46.dp).clip(RoundedCornerShape(13.dp))
                            .background(ShimmerLight.copy(alpha = shimmerAlpha))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier.width(140.dp).height(13.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(ShimmerLight.copy(alpha = shimmerAlpha))
                        )
                        Box(
                            modifier = Modifier.width(100.dp).height(10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(ShimmerLight.copy(alpha = shimmerAlpha))
                        )
                        Box(
                            modifier = Modifier.width(80.dp).height(10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(ShimmerLight.copy(alpha = shimmerAlpha))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryEmptyState(hasSearch: Boolean, onClear: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(Primary.copy(alpha = 0.12f), PrimaryContainer))
                )
                .border(2.dp, Primary.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (hasSearch) Icons.Outlined.SearchOff else Icons.Outlined.History,
                null,
                tint = Primary,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            if (hasSearch) "No matching scans" else "No scan history yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = OnBackgroundLight
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            if (hasSearch) "Try adjusting your search or filters"
            else "Scan history will appear here\nonce you start scanning tickets",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariantLight,
            textAlign = TextAlign.Center
        )
        if (hasSearch) {
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = onClear,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Primary)
            ) {
                Icon(Icons.Default.FilterAltOff, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear Filters", fontWeight = FontWeight.Bold, color = Primary)
            }
        }
    }
}
