package com.example.eventpay.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = PrimaryDark,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = SecondaryDark,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = TertiaryDark,
    error = Error,
    onError = Color.White,
    errorContainer = ErrorContainer,
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
    inversePrimary = PrimaryLight,
    scrim = Color(0xFF000000)
)

@Composable
fun EventPayTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}

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

fun getTicketTypeColor(ticketType: com.example.eventpay.data.model.TicketType): Color {
    return when (ticketType) {
        com.example.eventpay.data.model.TicketType.STANDARD -> StandardTicketColor
        com.example.eventpay.data.model.TicketType.VIP -> VIPTicketColor
        com.example.eventpay.data.model.TicketType.EARLY_BIRD -> Color(0xFF059669)
    }
}

fun getStatusColor(isCheckedIn: Boolean, status: com.example.eventpay.data.model.TicketStatus? = null): Color {
    return when {
        isCheckedIn -> StatusCompleted
        status != null -> when (status) {
            com.example.eventpay.data.model.TicketStatus.ACTIVE -> StatusActive
            com.example.eventpay.data.model.TicketStatus.USED -> StatusCompleted
            com.example.eventpay.data.model.TicketStatus.EXPIRED -> StatusCancelled
            com.example.eventpay.data.model.TicketStatus.CANCELLED -> StatusCancelled
        }
        else -> StatusActive
    }
}
