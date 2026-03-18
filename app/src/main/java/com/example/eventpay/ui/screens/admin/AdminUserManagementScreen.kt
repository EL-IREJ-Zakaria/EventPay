package com.example.eventpay.ui.screens.admin

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eventpay.data.model.User
import com.example.eventpay.ui.admin.AdminViewModel
import com.example.eventpay.ui.admin.CreateScannerDialog
import com.example.eventpay.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

private val ScannerPurple = Color(0xFF7B2FBE)
private val ScannerPurpleLight = Color(0xFFF3E8FF)
private val ScannerPurpleDark = Color(0xFF5A1E8A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserManagementScreen(
    adminViewModel: AdminViewModel,
    currentAdminId: String,
    onBack: () -> Unit
) {
    val uiState by adminViewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var filterActive by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        adminViewModel.loadScanners()
        adminViewModel.loadEvents()
    }

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            kotlinx.coroutines.delay(2500)
            adminViewModel.clearMessages()
        }
    }

    val filteredScanners = remember(uiState.scanners, searchQuery, filterActive) {
        uiState.scanners.filter { scanner ->
            val matchesSearch = searchQuery.isBlank() ||
                scanner.fullName.contains(searchQuery, ignoreCase = true) ||
                scanner.email.contains(searchQuery, ignoreCase = true)
            val matchesFilter = filterActive == null || scanner.isActive == filterActive
            matchesSearch && matchesFilter
        }
    }

    val activeCount = remember(uiState.scanners) { uiState.scanners.count { it.isActive } }
    val inactiveCount = remember(uiState.scanners) { uiState.scanners.count { !it.isActive } }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundLight)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                ScannerTopBar(
                    scannerCount = uiState.scanners.size,
                    activeCount = activeCount,
                    onBack = onBack
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = ScannerPurple,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(18.dp),
                        spotColor = ScannerPurple.copy(alpha = 0.5f)
                    ),
                    icon = {
                        Icon(Icons.Default.PersonAdd, null, tint = Color.White)
                    },
                    text = {
                        Text(
                            "Add Scanner",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                AnimatedVisibility(
                    visible = uiState.successMessage != null,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                ) {
                    uiState.successMessage?.let { msg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.horizontalGradient(listOf(Tertiary, TertiaryDark))
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                msg,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = uiState.error != null,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                ) {
                    uiState.error?.let { err ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ErrorContainer)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                null,
                                tint = Error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                err,
                                color = ErrorDark,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.scanners.isNotEmpty()) {
                    ScannerSummaryRow(
                        totalScanners = uiState.scanners.size,
                        activeCount = activeCount,
                        inactiveCount = inactiveCount,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ScannerSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScannerFilterChip(
                            label = "All",
                            count = uiState.scanners.size,
                            isSelected = filterActive == null,
                            color = ScannerPurple,
                            onClick = { filterActive = null }
                        )
                        ScannerFilterChip(
                            label = "Active",
                            count = activeCount,
                            isSelected = filterActive == true,
                            color = Tertiary,
                            onClick = { filterActive = true }
                        )
                        ScannerFilterChip(
                            label = "Inactive",
                            count = inactiveCount,
                            isSelected = filterActive == false,
                            color = Error,
                            onClick = { filterActive = false }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                when {
                    uiState.isLoading -> {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(4) { index ->
                                ScannerCardShimmer(index)
                            }
                        }
                    }
                    uiState.scanners.isEmpty() -> {
                        ScannerEmptyState(onAddScanner = { showCreateDialog = true })
                    }
                    filteredScanners.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                modifier = Modifier.size(80.dp),
                                shape = CircleShape,
                                color = ScannerPurpleLight
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.SearchOff,
                                        null,
                                        modifier = Modifier.size(36.dp),
                                        tint = ScannerPurple
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No results found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = OnBackgroundLight
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Try a different search or filter",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariantLight
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            OutlinedButton(
                                onClick = {
                                    searchQuery = ""
                                    filterActive = null
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.FilterListOff,
                                    null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Clear Filters")
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                top = 4.dp,
                                bottom = 96.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredScanners, key = { it.id }) { scanner ->
                                var visible by remember { mutableStateOf(false) }
                                LaunchedEffect(Unit) {
                                    kotlinx.coroutines.delay(50L)
                                    visible = true
                                }
                                AnimatedVisibility(
                                    visible = visible,
                                    enter = fadeIn(tween(250)) + slideInVertically(
                                        initialOffsetY = { it / 4 },
                                        animationSpec = tween(300)
                                    )
                                ) {
                                    ScannerUserCard(
                                        user = scanner,
                                        onToggleActive = { newActive ->
                                            adminViewModel.toggleScannerActive(scanner.id, newActive)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateScannerDialog(
            events = uiState.events,
            onDismiss = { showCreateDialog = false },
            onCreate = { email, password, fullName, selectedEventIds ->
                adminViewModel.createScannerWithEvents(
                    email,
                    password,
                    fullName,
                    currentAdminId,
                    selectedEventIds
                )
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun ScannerTopBar(
    scannerCount: Int,
    activeCount: Int,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF1A0A3D),
                        0.5f to ScannerPurple,
                        1f to ScannerPurpleDark
                    )
                )
            )
            .padding(start = 4.dp, end = 20.dp, top = 8.dp, bottom = 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Scanner Staff",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    "$scannerCount staff · $activeCount active",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f)
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.18f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.QrCodeScanner, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        "Staff",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ScannerSummaryRow(
    totalScanners: Int,
    activeCount: Int,
    inactiveCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ScannerMiniStatCard(
            value = totalScanners.toString(),
            label = "Total",
            color = ScannerPurple,
            bg = ScannerPurpleLight,
            modifier = Modifier.weight(1f)
        )
        ScannerMiniStatCard(
            value = activeCount.toString(),
            label = "Active",
            color = Tertiary,
            bg = TertiaryContainer,
            modifier = Modifier.weight(1f)
        )
        ScannerMiniStatCard(
            value = inactiveCount.toString(),
            label = "Inactive",
            color = if (inactiveCount > 0) Error else OnSurfaceVariantLight,
            bg = if (inactiveCount > 0) ErrorContainer else SurfaceVariantLight,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ScannerMiniStatCard(
    value: String,
    label: String,
    color: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.shadow(8.dp, RoundedCornerShape(16.dp), spotColor = color.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceLight
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(color.copy(alpha = 0.06f), bg.copy(alpha = 0.5f))),
                    RoundedCornerShape(16.dp)
                )
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariantLight,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ScannerSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = ScannerPurple.copy(alpha = 0.08f))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = ScannerPurple, modifier = Modifier.size(20.dp))
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    "Search by name or email…",
                    color = Zinc400,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedTextColor = Zinc900,
                unfocusedTextColor = Zinc900,
                cursorColor = ScannerPurple
            ),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium
        )
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Zinc100)
                    .clickable { onQueryChange("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, null, tint = Zinc500, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun ScannerFilterChip(
    label: String,
    count: Int,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) color else SurfaceLight,
        label = "chipBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else OnSurfaceVariantLight,
        label = "chipText"
    )

    Surface(
        modifier = Modifier
            .shadow(
                elevation = if (isSelected) 6.dp else 2.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = if (isSelected) color.copy(alpha = 0.4f) else Color.Transparent
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = textColor
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color.White.copy(alpha = 0.25f)
                        else color.copy(alpha = 0.12f)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    "$count",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else color
                )
            }
        }
    }
}

@Composable
private fun ScannerUserCard(
    user: User,
    onToggleActive: (Boolean) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val initials = user.fullName
        .split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()

    val infiniteTransition = rememberInfiniteTransition(label = "activePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = if (user.isActive) ScannerPurple.copy(alpha = 0.08f)
                else Color.Black.copy(alpha = 0.04f)
            ),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceLight
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(
                            if (user.isActive) listOf(ScannerPurple, ScannerPurple.copy(alpha = 0.3f))
                            else listOf(OutlineLight, OutlineLight.copy(alpha = 0.3f))
                        ),
                        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
            )
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(54.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                if (user.isActive)
                                    Brush.linearGradient(
                                        listOf(ScannerPurple, ScannerPurpleDark)
                                    )
                                else Brush.linearGradient(
                                    listOf(OutlineLight, OutlineVariantLight)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            initials,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (user.isActive) Color.White
                            else OnSurfaceVariantLight
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(SurfaceLight)
                            .border(2.dp, SurfaceLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(
                                    if (user.isActive) Tertiary.copy(alpha = if (user.isActive) pulseAlpha else 1f)
                                    else OutlineLight
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            user.fullName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = OnSurfaceLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = if (user.isActive) Tertiary.copy(alpha = 0.12f) else ErrorContainer
                        ) {
                            Text(
                                if (user.isActive) "ACTIVE" else "INACTIVE",
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (user.isActive) Tertiary else Error,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Email,
                            null,
                            modifier = Modifier.size(12.dp),
                            tint = OnSurfaceVariantLight
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            user.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariantLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    user.lastLoginAt?.let { lastLogin ->
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.AccessTime,
                                null,
                                modifier = Modifier.size(12.dp),
                                tint = OnSurfaceVariantLight.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Last login: ${dateFormat.format(Date(lastLogin))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariantLight.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = user.isActive,
                    onCheckedChange = onToggleActive,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Tertiary,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = OutlineLight
                    )
                )
            }

            if (user.isActive) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = DividerLight,
                    thickness = 0.8.dp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.QrCodeScanner,
                        null,
                        modifier = Modifier.size(14.dp),
                        tint = ScannerPurple
                    )
                    Text(
                        "Scanner Account",
                        style = MaterialTheme.typography.labelSmall,
                        color = ScannerPurple,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ScannerPurpleLight
                    ) {
                        Text(
                            "SCANNER",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = ScannerPurple,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannerEmptyState(onAddScanner: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(ScannerPurple.copy(alpha = 0.15f), ScannerPurpleLight)
                    )
                )
                .border(2.dp, ScannerPurple.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.QrCodeScanner,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = ScannerPurple
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "No scanner accounts yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = OnBackgroundLight
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            "Create scanner accounts so your event\nstaff can validate tickets at the entrance",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariantLight,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onAddScanner,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ScannerPurple),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
            modifier = Modifier.shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(14.dp),
                spotColor = ScannerPurple.copy(alpha = 0.5f)
            )
        ) {
            Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add First Scanner", fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun ScannerCardShimmer(index: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 100L)
        visible = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    AnimatedVisibility(visible = visible, enter = fadeIn(tween(200))) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = SurfaceLight
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(ShimmerLight.copy(alpha = shimmerAlpha))
                )
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(ShimmerLight.copy(alpha = shimmerAlpha))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(ShimmerLight.copy(alpha = shimmerAlpha))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .height(11.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(ShimmerLight.copy(alpha = shimmerAlpha))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(26.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(ShimmerLight.copy(alpha = shimmerAlpha))
                    )
                }
            }
        }
    }
}
