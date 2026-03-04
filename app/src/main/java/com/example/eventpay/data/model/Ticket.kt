package com.example.eventpay.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tickets",
    foreignKeys = [
        ForeignKey(
            entity = Event::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("eventId"), Index("userId")]
)
data class Ticket(
    @PrimaryKey
    val id: String = "",
    val eventId: String = "",
    val userId: String = "",
    val qrCode: String = "",
    val ticketType: TicketType = TicketType.STANDARD,
    val price: Double = 0.0,
    val isCheckedIn: Boolean = false,
    val checkedInAt: Long? = null,
    val purchaseDate: Long = System.currentTimeMillis(),
    // Enhanced fields for professional ticket management
    val seatNumber: String? = null,
    val section: String? = null,
    val row: String? = null,
    val attendeeName: String? = null,
    val attendeeEmail: String? = null,
    val attendeePhone: String? = null,
    val status: TicketStatus = TicketStatus.ACTIVE,
    val notes: String? = null,
    val issuedBy: String? = null, // Cashier/Organizer who issued the ticket
    val lastModified: Long = System.currentTimeMillis(),
    val checkedInBy: String? = null,
    val deviceId: String? = null
)

enum class TicketType {
    STANDARD,
    VIP,
    PREMIUM,
    EARLY_BIRD,
    STUDENT,
    GROUP,
    PASS
}

enum class TicketStatus {
    ACTIVE,
    USED,
    EXPIRED,
    CANCELLED,
    REFUNDED,
    PENDING
}
