package com.example.eventpay.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = PrimaryLight,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryDark,
    onSecondaryContainer = SecondaryLight,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryDark,
    onTertiaryContainer = TertiaryLight,
    error = Error,
    onError = Color.White,
    errorContainer = ErrorDark,
    onErrorContainer = ErrorLight,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    inverseSurface = SurfaceLight,
    inverseOnSurface = OnSurfaceLight,
    inversePrimary = PrimaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = PrimaryDark,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryLight,
    onSecondaryContainer = SecondaryDark,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryLight,
    onTertiaryContainer = TertiaryDark,
    error = Error,
    onError = Color.White,
    errorContainer = ErrorLight,
    onErrorContainer = ErrorDark,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    inverseSurface = SurfaceDark,
    inverseOnSurface = OnSurfaceDark,
    inversePrimary = PrimaryLight
)

@Composable
fun EventPayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled by default to use custom brand colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Extension to get category color (data model)
fun getCategoryColor(category: com.example.eventpay.data.model.EventCategory): Color {
    return when (category) {
        com.example.eventpay.data.model.EventCategory.CONFERENCE -> ConferenceColor
        com.example.eventpay.data.model.EventCategory.WORKSHOP -> WorkshopColor
        com.example.eventpay.data.model.EventCategory.SEMINAR -> SeminarColor
        com.example.eventpay.data.model.EventCategory.CONCERT -> ConcertColor
        com.example.eventpay.data.model.EventCategory.SPORTS -> SportsColor
        com.example.eventpay.data.model.EventCategory.NETWORKING -> NetworkingColor
        com.example.eventpay.data.model.EventCategory.EXHIBITION -> ExhibitionColor
        com.example.eventpay.data.model.EventCategory.FESTIVAL -> FestivalColor
        com.example.eventpay.data.model.EventCategory.GENERAL -> GeneralColor
    }
}

// Extension to get category color (domain model)
fun getCategoryColor(category: com.example.eventpay.domain.model.EventCategory): Color {
    return when (category) {
        com.example.eventpay.domain.model.EventCategory.CONFERENCE -> ConferenceColor
        com.example.eventpay.domain.model.EventCategory.WORKSHOP -> WorkshopColor
        com.example.eventpay.domain.model.EventCategory.SEMINAR -> SeminarColor
        com.example.eventpay.domain.model.EventCategory.CONCERT -> ConcertColor
        com.example.eventpay.domain.model.EventCategory.SPORTS -> SportsColor
        com.example.eventpay.domain.model.EventCategory.NETWORKING -> NetworkingColor
        com.example.eventpay.domain.model.EventCategory.EXHIBITION -> ExhibitionColor
        com.example.eventpay.domain.model.EventCategory.FESTIVAL -> FestivalColor
        com.example.eventpay.domain.model.EventCategory.GENERAL -> GeneralColor
    }
}

// Extension to get ticket type color
fun getTicketTypeColor(ticketType: com.example.eventpay.data.model.TicketType): Color {
    return when (ticketType) {
        com.example.eventpay.data.model.TicketType.STANDARD -> StandardTicketColor
        com.example.eventpay.data.model.TicketType.VIP -> VIPTicketColor
        com.example.eventpay.data.model.TicketType.PREMIUM -> PremiumTicketColor
        com.example.eventpay.data.model.TicketType.EARLY_BIRD -> EarlyBirdTicketColor
        com.example.eventpay.data.model.TicketType.STUDENT -> StudentTicketColor
        com.example.eventpay.data.model.TicketType.GROUP -> GroupTicketColor
        com.example.eventpay.data.model.TicketType.PASS -> PassTicketColor
    }
}

// Extension to get status color
fun getStatusColor(isCheckedIn: Boolean, status: com.example.eventpay.data.model.TicketStatus? = null): Color {
    return when {
        isCheckedIn -> StatusCompleted
        status != null -> when (status) {
            com.example.eventpay.data.model.TicketStatus.ACTIVE -> StatusActive
            com.example.eventpay.data.model.TicketStatus.USED -> StatusCompleted
            com.example.eventpay.data.model.TicketStatus.EXPIRED -> StatusCancelled
            com.example.eventpay.data.model.TicketStatus.CANCELLED -> StatusCancelled
            com.example.eventpay.data.model.TicketStatus.REFUNDED -> StatusCancelled
            com.example.eventpay.data.model.TicketStatus.PENDING -> StatusPending
        }
        else -> StatusActive
    }
}
