package com.example.eventpay.domain.model

/**
 * Domain Entity - EventAnalytics
 * 
 * Real-time analytics data for event organizers.
 * Provides comprehensive metrics for monitoring event performance.
 */
data class EventAnalytics(
    val eventId: String,
    val eventName: String,
    val eventDate: Long,
    val venue: String,
    val totalCapacity: Int,
    val lastUpdated: Long = System.currentTimeMillis(),
    
    // Attendance Metrics
    val totalTicketsSold: Int,
    val totalAttendees: Int,
    val checkedInCount: Int,
    val pendingCheckIn: Int,
    val noShowCount: Int,
    val checkInRate: Double,
    
    // Revenue Metrics
    val totalRevenue: Double,
    val ticketRevenue: Double,
    val vipRevenue: Double,
    val standardRevenue: Double,
    val averageTicketPrice: Double,
    val revenuePerAttendee: Double,
    
    // Real-time Metrics
    val liveCheckIns: Int,
    val lastCheckInTime: Long?,
    val checkInsLastHour: Int,
    val checkInsLast15Minutes: Int,
    val currentQueueSize: Int,
    
    // Time-based Analytics
    val peakEntryTime: PeakEntryTime?,
    val hourlyCheckIns: List<HourlyCheckIn>,
    val entryVelocity: Double, // Check-ins per minute
    
    // Ticket Type Breakdown
    val ticketTypeBreakdown: Map<TicketType, TicketTypeStats>,
    
    // Payment Method Breakdown
    val paymentMethodBreakdown: Map<PaymentMethod, PaymentStats>,
    
    // Sales Analytics
    val salesByHour: List<HourlySales>,
    val salesTrend: SalesTrend,
    val conversionRate: Double,
    
    // Demographics (if collected)
    val demographics: Demographics?
) {
    /**
     * Calculate occupancy percentage
     */
    fun occupancyRate(): Double {
        return if (totalCapacity > 0) {
            (checkedInCount.toDouble() / totalCapacity) * 100
        } else 0.0
    }
    
    /**
     * Calculate ticket sales percentage
     */
    fun salesRate(): Double {
        return if (totalCapacity > 0) {
            (totalTicketsSold.toDouble() / totalCapacity) * 100
        } else 0.0
    }
    
    /**
     * Get check-in status summary
     */
    fun checkInSummary(): CheckInSummary {
        return CheckInSummary(
            total = totalAttendees,
            checkedIn = checkedInCount,
            pending = pendingCheckIn,
            noShow = noShowCount,
            percentage = checkInRate
        )
    }
    
    /**
     * Get revenue breakdown
     */
    fun revenueBreakdown(): RevenueBreakdown {
        return RevenueBreakdown(
            total = totalRevenue,
            tickets = ticketRevenue,
            vip = vipRevenue,
            standard = standardRevenue,
            byType = ticketTypeBreakdown.mapValues { it.value.revenue }
        )
    }
    
    /**
     * Get real-time status
     */
    fun realTimeStatus(): RealTimeStatus {
        return RealTimeStatus(
            liveCheckIns = liveCheckIns,
            lastCheckIn = lastCheckInTime,
            velocity = entryVelocity,
            queueSize = currentQueueSize,
            last15Min = checkInsLast15Minutes,
            lastHour = checkInsLastHour
        )
    }
}

/**
 * Peak entry time information
 */
data class PeakEntryTime(
    val hour: Int,          // 0-23
    val minute: Int,        // 0-59
    val timestamp: Long,
    val checkInCount: Int,
    val percentage: Double  // Percentage of total check-ins
) {
    fun formattedTime(): String {
        val hourFormatted = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        val amPm = if (hour < 12) "AM" else "PM"
        return "$hourFormatted:${minute.toString().padStart(2, '0')} $amPm"
    }
}

/**
 * Hourly check-in data
 */
data class HourlyCheckIn(
    val hour: Int,          // 0-23
    val timestamp: Long,
    val checkInCount: Int,
    val cumulativeCount: Int,
    val percentage: Double
)

/**
 * Ticket type statistics
 */
data class TicketTypeStats(
    val type: TicketType,
    val sold: Int,
    val checkedIn: Int,
    val revenue: Double,
    val averagePrice: Double,
    val checkInRate: Double
)

/**
 * Payment method statistics
 */
data class PaymentStats(
    val method: PaymentMethod,
    val transactionCount: Int,
    val totalAmount: Double,
    val percentage: Double,
    val averageTransaction: Double
)

/**
 * Hourly sales data
 */
data class HourlySales(
    val hour: Int,
    val timestamp: Long,
    val ticketsSold: Int,
    val revenue: Double,
    val transactionCount: Int
)

/**
 * Sales trend indicator
 */
enum class SalesTrend {
    INCREASING,
    STABLE,
    DECREASING,
    PEAK;
    
    fun displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }
    
    fun icon(): String = when (this) {
        INCREASING -> "📈"
        STABLE -> "➡️"
        DECREASING -> "📉"
        PEAK -> "🔝"
    }
}

/**
 * Check-in summary
 */
data class CheckInSummary(
    val total: Int,
    val checkedIn: Int,
    val pending: Int,
    val noShow: Int,
    val percentage: Double
)

/**
 * Revenue breakdown
 */
data class RevenueBreakdown(
    val total: Double,
    val tickets: Double,
    val vip: Double,
    val standard: Double,
    val byType: Map<TicketType, Double>
)

/**
 * Real-time status
 */
data class RealTimeStatus(
    val liveCheckIns: Int,
    val lastCheckIn: Long?,
    val velocity: Double,
    val queueSize: Int,
    val last15Min: Int,
    val lastHour: Int
)

/**
 * Demographics data (optional)
 */
data class Demographics(
    val ageGroups: Map<AgeGroup, Int>,
    val genderDistribution: Map<Gender, Int>,
    val topLocations: List<LocationStat>
)

enum class AgeGroup(val range: String) {
    UNDER_18("Under 18"),
    AGE_18_24("18-24"),
    AGE_25_34("25-34"),
    AGE_35_44("35-44"),
    AGE_45_54("45-54"),
    AGE_55_PLUS("55+");
    
    fun displayName(): String = range
}

enum class Gender {
    MALE,
    FEMALE,
    OTHER,
    PREFER_NOT_TO_SAY;
    
    fun displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }
}

data class LocationStat(
    val location: String,
    val count: Int,
    val percentage: Double
)

/**
 * Dashboard filter options
 */
data class DashboardFilter(
    val eventId: String? = null,
    val dateRange: DateRange = DateRange.TODAY,
    val ticketType: TicketType? = null,
    val paymentMethod: PaymentMethod? = null,
    val refreshInterval: RefreshInterval = RefreshInterval.MINUTE_1
)

enum class DateRange(val displayName: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    THIS_MONTH("This Month"),
    CUSTOM("Custom Range");
    
    fun toMillis(): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        
        return when (this) {
            TODAY -> {
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, now)
            }
            YESTERDAY -> {
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                val end = calendar.timeInMillis
                calendar.add(java.util.Calendar.DAY_OF_MONTH, -1)
                Pair(calendar.timeInMillis, end)
            }
            LAST_7_DAYS -> {
                calendar.add(java.util.Calendar.DAY_OF_MONTH, -7)
                Pair(calendar.timeInMillis, now)
            }
            LAST_30_DAYS -> {
                calendar.add(java.util.Calendar.DAY_OF_MONTH, -30)
                Pair(calendar.timeInMillis, now)
            }
            THIS_MONTH -> {
                calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, now)
            }
            CUSTOM -> Pair(0L, now)
        }
    }
}

enum class RefreshInterval(val seconds: Int, val displayName: String) {
    SECONDS_30(30, "30 seconds"),
    MINUTE_1(60, "1 minute"),
    MINUTES_5(300, "5 minutes"),
    MINUTES_15(900, "15 minutes"),
    MANUAL(0, "Manual refresh");
}

/**
 * Analytics alert configuration
 */
data class AnalyticsAlert(
    val id: String,
    val eventId: String,
    val type: AlertType,
    val threshold: Double,
    val comparison: ComparisonOperator,
    val isActive: Boolean = true,
    val lastTriggered: Long? = null
)

enum class AlertType {
    CHECK_IN_RATE_LOW,
    CHECK_IN_RATE_HIGH,
    REVENUE_TARGET,
    QUEUE_SIZE_HIGH,
    ENTRY_VELOCITY_LOW,
    ENTRY_VELOCITY_HIGH,
    CAPACITY_THRESHOLD;
    
    fun displayName(): String = name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }
}

enum class ComparisonOperator {
    GREATER_THAN,
    LESS_THAN,
    EQUALS,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN_OR_EQUAL
}

/**
 * Dashboard widget configuration
 */
data class DashboardWidget(
    val id: String,
    val type: WidgetType,
    val title: String,
    val position: Int,
    val size: WidgetSize,
    val isVisible: Boolean = true,
    val refreshEnabled: Boolean = true
)

enum class WidgetType {
    ATTENDANCE_COUNTER,
    REVENUE_COUNTER,
    CHECK_IN_PROGRESS,
    PEAK_TIMES_CHART,
    HOURLY_CHECK_INS_CHART,
    TICKET_TYPE_BREAKDOWN,
    PAYMENT_METHOD_BREAKDOWN,
    REAL_TIME_FEED,
    ENTRY_VELOCITY,
    CAPACITY_GAUGE;
    
    fun displayName(): String = name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }
}

enum class WidgetSize {
    SMALL,      // 1x1
    MEDIUM,     // 2x1
    LARGE,      // 2x2
    FULL_WIDTH  // 4x1
}
