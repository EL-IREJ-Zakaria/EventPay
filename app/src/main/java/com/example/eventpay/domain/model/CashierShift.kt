package com.example.eventpay.domain.model

/**
 * Domain Entity - CashierShift
 * 
 * Represents a cashier's work shift with sales tracking and cash management.
 * Used for daily closing and reconciliation.
 */
data class CashierShift(
    val id: String,
    val cashierId: String,
    val cashierName: String,
    val eventId: String? = null,
    val eventName: String? = null,
    val shiftStart: Long,
    val shiftEnd: Long? = null,
    val startingCash: Double,
    val actualCash: Double? = null,
    val cashSales: Double = 0.0,
    val cardSales: Double = 0.0,
    val mobileSales: Double = 0.0,
    val walletSales: Double = 0.0,
    val totalSales: Double = cashSales + cardSales + mobileSales + walletSales,
    val ticketsSold: Int = 0,
    val transactionsCount: Int = 0,
    val refundsProcessed: Int = 0,
    val refundAmount: Double = 0.0,
    val status: ShiftStatus = ShiftStatus.ACTIVE,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Expected cash in drawer
     */
    val expectedCash: Double
        get() = startingCash + cashSales - refundAmount
    
    /**
     * Cash difference (over/short)
     */
    val cashDifference: Double?
        get() = actualCash?.let { it - expectedCash }
    
    /**
     * Check if shift is active
     */
    fun isActive(): Boolean = status == ShiftStatus.ACTIVE && shiftEnd == null
    
    /**
     * Check if shift is completed
     */
    fun isCompleted(): Boolean = status == ShiftStatus.COMPLETED && shiftEnd != null
    
    /**
     * Calculate shift duration in minutes
     */
    fun durationMinutes(): Long {
        val end = shiftEnd ?: System.currentTimeMillis()
        return (end - shiftStart) / (1000 * 60)
    }
    
    /**
     * Calculate average sale value
     */
    fun averageSaleValue(): Double {
        return if (transactionsCount > 0) totalSales / transactionsCount else 0.0
    }
    
    /**
     * Calculate refund rate
     */
    fun refundRate(): Double {
        return if (ticketsSold > 0) (refundsProcessed.toDouble() / ticketsSold) * 100 else 0.0
    }
    
    /**
     * Get sales breakdown by payment method
     */
    fun salesByPaymentMethod(): Map<PaymentMethod, Double> {
        return mapOf(
            PaymentMethod.CASH to cashSales,
            PaymentMethod.CARD to cardSales,
            PaymentMethod.MOBILE_MONEY to mobileSales,
            PaymentMethod.WALLET to walletSales
        ).filterValues { it > 0 }
    }
    
    /**
     * Validate shift data
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (cashierId.isBlank()) errors.add("Cashier ID is required")
        if (startingCash < 0) errors.add("Starting cash cannot be negative")
        if (shiftEnd != null && shiftEnd <= shiftStart) {
            errors.add("Shift end must be after shift start")
        }
        if (actualCash != null && actualCash < 0) {
            errors.add("Actual cash cannot be negative")
        }
        
        return if (errors.isEmpty()) ValidationResult.Success else ValidationResult.Error(errors)
    }
    
    /**
     * Generate shift report
     */
    fun generateReport(): ShiftReport {
        return ShiftReport(
            shiftId = id,
            cashierName = cashierName,
            eventName = eventName,
            shiftStart = shiftStart,
            shiftEnd = shiftEnd ?: System.currentTimeMillis(),
            durationMinutes = durationMinutes(),
            startingCash = startingCash,
            expectedCash = expectedCash,
            actualCash = actualCash,
            cashDifference = cashDifference,
            totalSales = totalSales,
            ticketsSold = ticketsSold,
            transactionsCount = transactionsCount,
            refundsProcessed = refundsProcessed,
            refundAmount = refundAmount,
            salesByPaymentMethod = salesByPaymentMethod(),
            averageSaleValue = averageSaleValue(),
            refundRate = refundRate(),
            notes = notes
        )
    }
}

/**
 * Shift status
 */
enum class ShiftStatus {
    ACTIVE,     // Shift is currently active
    COMPLETED,  // Shift has been completed
    CANCELLED;  // Shift was cancelled
    
    fun displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }
}

/**
 * Shift report for daily closing
 */
data class ShiftReport(
    val shiftId: String,
    val cashierName: String,
    val eventName: String?,
    val shiftStart: Long,
    val shiftEnd: Long,
    val durationMinutes: Long,
    val startingCash: Double,
    val expectedCash: Double,
    val actualCash: Double?,
    val cashDifference: Double?,
    val totalSales: Double,
    val ticketsSold: Int,
    val transactionsCount: Int,
    val refundsProcessed: Int,
    val refundAmount: Double,
    val salesByPaymentMethod: Map<PaymentMethod, Double>,
    val averageSaleValue: Double,
    val refundRate: Double,
    val notes: String?
) {
    /**
     * Format duration as hours and minutes
     */
    fun formattedDuration(): String {
        val hours = durationMinutes / 60
        val minutes = durationMinutes % 60
        return "${hours}h ${minutes}m"
    }
    
    /**
     * Check if cash reconciliation has discrepancy
     */
    fun hasCashDiscrepancy(): Boolean {
        return cashDifference != null && cashDifference != 0.0
    }
    
    /**
     * Get discrepancy type
     */
    fun discrepancyType(): DiscrepancyType? {
        return cashDifference?.let {
            when {
                it > 0 -> DiscrepancyType.OVER
                it < 0 -> DiscrepancyType.SHORT
                else -> null
            }
        }
    }
}

/**
 * Cash discrepancy type
 */
enum class DiscrepancyType {
    OVER,   // More cash than expected
    SHORT;  // Less cash than expected
    
    fun displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }
}

/**
 * Cashier performance metrics
 */
data class CashierPerformance(
    val cashierId: String,
    val cashierName: String,
    val totalShifts: Int,
    val totalSales: Double,
    val totalTicketsSold: Int,
    val totalTransactions: Int,
    val totalRefunds: Int,
    val averageSalesPerShift: Double,
    val averageTicketsPerShift: Double,
    val averageTransactionValue: Double,
    val refundRate: Double,
    val totalDiscrepancies: Int,
    val averageDiscrepancy: Double
) {
    /**
     * Performance rating based on metrics
     */
    fun performanceRating(): PerformanceRating {
        val score = calculatePerformanceScore()
        return when {
            score >= 90 -> PerformanceRating.EXCELLENT
            score >= 75 -> PerformanceRating.GOOD
            score >= 60 -> PerformanceRating.AVERAGE
            score >= 40 -> PerformanceRating.BELOW_AVERAGE
            else -> PerformanceRating.POOR
        }
    }
    
    private fun calculatePerformanceScore(): Double {
        var score = 100.0
        
        // Deduct for high refund rate
        if (refundRate > 5) score -= (refundRate - 5) * 2
        
        // Deduct for discrepancies
        if (totalDiscrepancies > 0) {
            score -= totalDiscrepancies * 5
        }
        
        // Deduct for high average discrepancy
        if (averageDiscrepancy > 10) {
            score -= (averageDiscrepancy - 10) * 0.5
        }
        
        return score.coerceIn(0.0, 100.0)
    }
}

/**
 * Performance rating
 */
enum class PerformanceRating {
    EXCELLENT,
    GOOD,
    AVERAGE,
    BELOW_AVERAGE,
    POOR;
    
    fun displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }
}
