package com.example.eventpay.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class Transaction(
    @PrimaryKey
    val id: String = "",
    val userId: String = "",
    val type: TransactionType = TransactionType.TICKET_PURCHASE,
    val amount: Double = 0.0,
    val description: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.WALLET,
    val status: TransactionStatus = TransactionStatus.COMPLETED,
    val eventId: String? = null,
    val ticketId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

enum class TransactionType {
    TICKET_PURCHASE,
    WALLET_TOP_UP,
    MERCHANDISE_PURCHASE,
    REFUND,
    CASHIER_SALE
}

enum class PaymentMethod {
    WALLET,
    CASH,
    CARD,           // Credit/Debit card
    MOBILE_MONEY,   // Mobile payment (M-Pesa, etc.)
    BANK_TRANSFER,  // Bank transfer
    CASHIER_SALE;    // Sale through cashier

    fun displayName(): String {
        return when (this) {
            WALLET -> "Wallet"
            CASH -> "Cash"
            CARD -> "Card"
            MOBILE_MONEY -> "Mobile Money"
            BANK_TRANSFER -> "Bank Transfer"
            CASHIER_SALE -> "Cashier Sale"
        }
    }
}

enum class TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REFUNDED,
    CANCELLED
}
