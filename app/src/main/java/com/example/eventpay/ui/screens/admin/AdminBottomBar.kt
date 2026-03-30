package com.example.eventpay.ui.screens.admin

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.eventpay.ui.theme.Primary
import com.example.eventpay.ui.theme.SurfaceLight

internal enum class AdminBottomDestination {
    Home,
    Scanners
}

@Composable
internal fun AdminBottomBar(
    selectedDestination: AdminBottomDestination,
    onHomeClick: () -> Unit,
    onScannersClick: () -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(76.dp)
            .clip(RoundedCornerShape(24.dp)),
        containerColor = SurfaceLight,
        tonalElevation = 10.dp
    ) {
        NavigationBarItem(
            selected = selectedDestination == AdminBottomDestination.Home,
            onClick = onHomeClick,
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text("Home") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Primary,
                indicatorColor = Primary,
                unselectedIconColor = Primary.copy(alpha = 0.55f),
                unselectedTextColor = Primary.copy(alpha = 0.7f)
            )
        )
        NavigationBarItem(
            selected = selectedDestination == AdminBottomDestination.Scanners,
            onClick = onScannersClick,
            icon = { Icon(Icons.Filled.QrCodeScanner, contentDescription = null) },
            label = { Text("Scanners") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Primary,
                indicatorColor = Primary,
                unselectedIconColor = Primary.copy(alpha = 0.55f),
                unselectedTextColor = Primary.copy(alpha = 0.7f)
            )
        )
    }
}
