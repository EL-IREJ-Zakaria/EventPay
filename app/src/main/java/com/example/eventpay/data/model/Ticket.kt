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
    val isCheckedIn: Boolean = false,
    val checkedInAt: Long? = null,
    val reservationDate: Long = System.currentTimeMillis(),
    val seatNumber: String? = null,
    val status: TicketStatus = TicketStatus.ACTIVE,
    val notes: String? = null,
    val checkedInBy: String? = null
)

enum class TicketType {
    STANDARD,
    VIP,
    EARLY_BIRD
}

enum class TicketStatus {
    ACTIVE,
    USED,
    EXPIRED,
    CANCELLED
}
