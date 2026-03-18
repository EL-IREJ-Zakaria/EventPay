package com.example.eventpay.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// EventPay · Gen-Z Pro Color System · 2025
// ─────────────────────────────────────────────────────────────────────────────

// Primary — Ultra Violet (premium SaaS / fintech)
val Primary = Color(0xFF7C3AED)
val PrimaryDark = Color(0xFF5B21B6)
val PrimaryLight = Color(0xFFA78BFA)
val PrimaryUltraLight = Color(0xFFEDE9FE)
val PrimaryContainer = Color(0xFFF5F3FF)

// Secondary — Electric Cyan
val Secondary = Color(0xFF06B6D4)
val SecondaryDark = Color(0xFF0E7490)
val SecondaryLight = Color(0xFF67E8F9)
val SecondaryContainer = Color(0xFFECFEFF)

// Tertiary — Neon Emerald
val Tertiary = Color(0xFF10B981)
val TertiaryDark = Color(0xFF047857)
val TertiaryLight = Color(0xFF6EE7B7)
val TertiaryContainer = Color(0xFFECFDF5)

// Accent — Amber / Gold
val Accent = Color(0xFFF59E0B)
val AccentDark = Color(0xFFB45309)
val AccentLight = Color(0xFFFCD34D)

// Rose — Premium Pink
val Rose = Color(0xFFF43F5E)
val RoseLight = Color(0xFFFDA4AF)
val RoseContainer = Color(0xFFFFF1F2)

// ─────────────────────────────────────────────────────────────────────────────
// Neutral — Zinc (crisp, modern)
// ─────────────────────────────────────────────────────────────────────────────
val Zinc50  = Color(0xFFFAFAFA)
val Zinc100 = Color(0xFFF4F4F5)
val Zinc200 = Color(0xFFE4E4E7)
val Zinc300 = Color(0xFFD4D4D8)
val Zinc400 = Color(0xFFA1A1AA)
val Zinc500 = Color(0xFF71717A)
val Zinc600 = Color(0xFF52525B)
val Zinc700 = Color(0xFF3F3F46)
val Zinc800 = Color(0xFF27272A)
val Zinc900 = Color(0xFF18181B)
val Zinc950 = Color(0xFF09090B)

// ─────────────────────────────────────────────────────────────────────────────
// Keep Slate aliases for backward compat
// ─────────────────────────────────────────────────────────────────────────────
val Slate50  = Zinc50
val Slate100 = Zinc100
val Slate200 = Zinc200
val Slate300 = Zinc300
val Slate400 = Zinc400
val Slate500 = Zinc500
val Slate600 = Zinc600
val Slate700 = Zinc700
val Slate800 = Zinc800
val Slate900 = Zinc900
val Slate950 = Zinc950

// ─────────────────────────────────────────────────────────────────────────────
// Semantic
// ─────────────────────────────────────────────────────────────────────────────
val Error          = Color(0xFFEF4444)
val ErrorLight     = Color(0xFFFEE2E2)
val ErrorDark      = Color(0xFF991B1B)
val ErrorContainer = Color(0xFFFEF2F2)
val Warning        = Color(0xFFF59E0B)
val WarningContainer = Color(0xFFFFFBEB)
val Success        = Color(0xFF10B981)
val SuccessContainer = Color(0xFFF0FDF4)

// ─────────────────────────────────────────────────────────────────────────────
// Light Surface
// ─────────────────────────────────────────────────────────────────────────────
val BackgroundLight       = Color(0xFFF8F7FF)
val SurfaceLight          = Color.White
val SurfaceVariantLight   = Zinc100
val OnBackgroundLight     = Zinc900
val OnSurfaceLight        = Zinc900
val OnSurfaceVariantLight = Zinc500

// ─────────────────────────────────────────────────────────────────────────────
// Dark Surface — OLED-first
// ─────────────────────────────────────────────────────────────────────────────
val BackgroundDark       = Color(0xFF08060F)
val SurfaceDark          = Color(0xFF0F0C1A)
val SurfaceVariantDark   = Color(0xFF1A1626)
val OnBackgroundDark     = Color(0xFFF0EEFF)
val OnSurfaceDark        = Color(0xFFF0EEFF)
val OnSurfaceVariantDark = Color(0xFF9D8FBF)

// Text
val OnPrimary   = Color.White
val OnSecondary = Color.White
val OnTertiary  = Color.White

// Outline
val OutlineLight        = Zinc200
val OutlineVariantLight = Zinc200
val OutlineDark         = Color(0xFF2D2541)
val OutlineVariantDark  = Color(0xFF241E35)
val DividerLight        = Zinc100
val DividerDark         = Color(0xFF1E1A2E)

// ─────────────────────────────────────────────────────────────────────────────
// Glassmorphism
// ─────────────────────────────────────────────────────────────────────────────
val GlassWhite        = Color(0xFFFFFFFF)
val GlassBorderLight  = Color(0x33FFFFFF)
val GlassBorderDark   = Color(0x22FFFFFF)
val GlassSurfaceLight = Color(0xCCFFFFFF)
val GlassSurfaceDark  = Color(0x1AFFFFFF)

// ─────────────────────────────────────────────────────────────────────────────
// Gradient Definitions
// ─────────────────────────────────────────────────────────────────────────────
val GradientStart  = Color(0xFF7C3AED)   // Violet
val GradientMid    = Color(0xFF4F46E5)   // Indigo
val GradientEnd    = Color(0xFF1E1B4B)   // Deep Indigo
val GradientAccent = Color(0xFFF59E0B)   // Amber

// Aurora / Mesh Gradient
val AuroraViolet  = Color(0xFF8B5CF6)
val AuroraBlue    = Color(0xFF3B82F6)
val AuroraCyan    = Color(0xFF06B6D4)
val AuroraGreen   = Color(0xFF10B981)
val AuroraPink    = Color(0xFFF43F5E)
val AuroraAmber   = Color(0xFFF59E0B)

// ─────────────────────────────────────────────────────────────────────────────
// Event Category Colors (vivid)
// ─────────────────────────────────────────────────────────────────────────────
val ConferenceColor  = Color(0xFF7C3AED)
val WorkshopColor    = Color(0xFFF59E0B)
val SeminarColor     = Color(0xFF8B5CF6)
val ConcertColor     = Color(0xFFF43F5E)
val SportsColor      = Color(0xFF10B981)
val NetworkingColor  = Color(0xFF3B82F6)
val ExhibitionColor  = Color(0xFFF97316)
val FestivalColor    = Color(0xFFEC4899)
val GeneralColor     = Color(0xFF71717A)

// Ticket Type Colors
val StandardTicketColor  = Primary
val VIPTicketColor       = Accent
val PremiumTicketColor   = Color(0xFF8B5CF6)
val EarlyBirdTicketColor = Tertiary
val StudentTicketColor   = Color(0xFF3B82F6)
val GroupTicketColor     = Color(0xFFF97316)
val PassTicketColor      = Color(0xFFEC4899)

// Status Colors
val StatusActive    = Tertiary
val StatusPending   = Accent
val StatusCancelled = Error
val StatusCompleted = Primary

// QR Scanner
val QrScannerOverlay = Color(0xCC000000)
val QrScannerCorner  = Primary
val QrScannerLine    = Primary

// Card
val CardBackgroundLight = Color.White
val CardBackgroundDark  = Color(0xFF120E1E)
val CardShadowLight     = Color(0x0A7C3AED)
val CardShadowDark      = Color(0x407C3AED)

// Shimmer
val ShimmerLight     = Zinc100
val ShimmerDark      = Color(0xFF1A1626)
val ShimmerHighlight = Color.White
