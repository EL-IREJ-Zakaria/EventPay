package com.example.eventpay.domain.model

/**
 * Domain Entity - Transaction
 * 
 * Represents a financial transaction in the system.
 * Supports ticket purchases, refunds, and wallet operations.
 */
data class Transaction(
    val id: String,
    val userId: String,
    val eventId: String? = null,
    val ticketId: String? = null,
    val type: TransactionType,
    val amount: Double,
    val status: TransactionStatus = TransactionStatus.PENDING,
    val paymentMethod: PaymentMethod = PaymentMethod.WALLET,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val description: String? = null,
    val receiptNumber: String? = null,
    val cashierId: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val syncStatus: SyncStatus = SyncStatus.SYNCED
) {
    /**
     * Check if transaction is completed
     */
    fun isCompleted(): Boolean = status == TransactionStatus.COMPLETED
    
    /**
     * Check if transaction is pending
     */
    fun isPending(): Boolean = status == TransactionStatus.PENDING
    
    /**
     * Check if transaction failed
     */
    fun isFailed(): Boolean = status == TransactionStatus.FAILED
    
    /**
     * Check if transaction is a refund
     */
    fun isRefund(): Boolean = type == TransactionType.REFUND
    
    /**
     * Check if transaction is a purchase
     */
    fun isPurchase(): Boolean = type == TransactionType.TICKET_PURCHASE
    
    /**
     * Check if transaction needs to be synced
     */
    fun needsSync(): Boolean = syncStatus == SyncStatus.PENDING
    
    /**
     * Get formatted amount with currency symbol
     */
    fun formattedAmount(currencySymbol: String = "$"): String {
        val prefix = if (amount < 0 || isRefund()) "-" else ""
        return "$prefix$currencySymbol${kotlin.math.abs(amount)}"
    }
    
    /**
     * Generate receipt number if not exists
     */
    fun generateReceiptNumber(): String {
        return receiptNumber ?: "TXN-${System.currentTimeMillis()}-$id.take(6)".uppercase()
    }
    
    /**
     * Validate transaction data
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (userId.isBlank()) errors.add("User ID is required")
        if (amount <= 0 && type != TransactionType.REFUND) {
            errors.add("Amount must be greater than 0")
        }
        if (type == TransactionType.TICKET_PURCHASE && eventId == null) {
            errors.add("Event ID is required for ticket purchases")
        }
        if (type == TransactionType.TICKET_PURCHASE && ticketId == null) {
            errors.add("Ticket ID is required for ticket purchases")
        }
        
        return if (errors.isEmpty()) ValidationResult.Success else ValidationResult.Error(errors)
    }
}

/**
 * Types of transactions
 */
enum class TransactionType {
    TICKET_PURCHASE,    // Buying a ticket
    REFUND,             // Refunding a ticket
    WALLET_TOP_UP,      // Adding funds to wallet
    WALLET_WITHDRAWAL,  // Withdrawing funds from wallet
    CASHIER_SALE,       // Sale processed by cashier
    PROMOTION;          // Promotional credit
    
    fun isCredit(): Boolean = this == WALLET_TOP_UP || this == PROMOTION
    fun isDebit(): Boolean = !isCredit()
    
    fun displayName(): String = when (this) {
        TICKET_PURCHASE -> "Ticket Purchase"
        REFUND -> "Refund"
        WALLET_TOP_UP -> "Wallet Top-up"
        WALLET_WITHDRAWAL -> "Withdrawal"
        CASHIER_SALE -> "Cashier Sale"
        PROMOTION -> "Promotion"
    }
}

/**
 * Transaction lifecycle status
 */
enum class TransactionStatus {
    PENDING,    // Transaction initiated but not completed
    PROCESSING, // Transaction is being processed
    COMPLETED,  // Transaction successfully completed
    FAILED,     // Transaction failed
    CANCELLED;  // Transaction was cancelled
    
    fun isFinal(): Boolean = this == COMPLETED || this == FAILED || this == CANCELLED
}

/**
 * Payment methods supported
 */
enum class PaymentMethod {
    WALLET,     // Pay from user's wallet
    CASH,       // Cash payment (cashier only)
    CARD,       // Credit/Debit card
    MOBILE_MONEY, // Mobile payment (M-Pesa, etc.)
    BANK_TRANSFER, // Bank transfer
    CASHIER_SALE;  // Sale through cashier
    
    fun displayName(): String = name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }
}

/**
 * Sync status for offline support
 */
enum class SyncStatus {
    SYNCED,     // Synchronized with server
    PENDING,    // Pending sync
    FAILED;     // Sync failed
    
    fun needsSync(): Boolean = this == PENDING || this == FAILED
}

/**
 * Transaction summary for reporting
 */
data class TransactionSummary(
    val totalTransactions: Int = 0,
    val totalAmount: Double = 0.0,
    val totalRefunds: Int = 0,
    val refundAmount: Double = 0.0,
    val netAmount: Double = totalAmount - refundAmount,
    val byPaymentMethod: Map<PaymentMethod, Double> = emptyMap(),
    val byType: Map<TransactionType, Int> = emptyMap(),
    val periodStart: Long,
    val periodEnd: Long
) {
    /**
     * Calculate average transaction value
     */
    fun averageTransactionValue(): Double {
        return if (totalTransactions > 0) totalAmount / totalTransactions else 0.0
    }
    
    /**
     * Calculate refund rate
     */
    fun refundRate(): Double {
        return if (totalTransactions > 0) (totalRefunds.toDouble() / totalTransactions) * 100 else 0.0
    }
}

/**
 * Cashier shift summary
 */
data class CashierShiftSummary(
    val cashierId: String,
    val shiftStart: Long,
    val shiftEnd: Long? = null,
    val startingCash: Double = 0.0,
    val cashSales: Double = 0.0,
    val cardSales: Double = 0.0,
    val mobileSales: Double = 0.0,
    val totalSales: Double = cashSales + cardSales + mobileSales,
    val ticketsSold: Int = 0,
    val refundsProcessed: Int = 0,
    val refundAmount: Double = 0.0,
    val expectedCash: Double = startingCash + cashSales - refundAmount
) {
    /**
     * Check if shift is active
     */
    fun isActive(): Boolean = shiftEnd == null
    
    /**
     * Calculate shift duration in minutes
     */
    fun durationMinutes(): Long {
        val end = shiftEnd ?: System.currentTimeMillis()
        return (end - shiftStart) / (1000 * 60)
    }
}
