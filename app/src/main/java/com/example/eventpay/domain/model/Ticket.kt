package com.example.eventpay.domain.model

/**
 * Domain Entity - Ticket
 * CMC School Event Management - Free tickets with VIP designation
 */
data class Ticket(
    val id: String,
    val eventId: String,
    val userId: String,
    val ticketType: TicketType = TicketType.STANDARD,
    val reservationDate: Long = System.currentTimeMillis(),
    val status: TicketStatus = TicketStatus.ACTIVE,
    val checkedInAt: Long? = null,
    val checkedInBy: String? = null,
    val qrCode: String,
    val seatNumber: String? = null,
    val notes: String? = null,
    val cancelledAt: Long? = null,
    val cancelReason: String? = null
) {
    val isUsed: Boolean get() = checkedInAt != null
    val usedAt: Long? get() = checkedInAt
    
    fun canCheckIn(): Boolean = status == TicketStatus.ACTIVE && checkedInAt == null
    fun isCheckedIn(): Boolean = checkedInAt != null
    fun isValid(): Boolean = status == TicketStatus.ACTIVE
    fun canBeCancelled(): Boolean = status == TicketStatus.ACTIVE && checkedInAt == null
    
    fun ticketTypeDisplayName(): String = when (ticketType) {
        TicketType.STANDARD -> "Standard"
        TicketType.VIP -> "VIP Guest"
        TicketType.EARLY_BIRD -> "Early Bird"
    }
    
    fun qrValidationKey(): String = "$eventId:$id:$qrCode"
    
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        if (eventId.isBlank()) errors.add("Event ID is required")
        if (userId.isBlank()) errors.add("User ID is required")
        if (qrCode.isBlank()) errors.add("QR code is required")
        return if (errors.isEmpty()) ValidationResult.Success else ValidationResult.Error(errors)
    }
}

enum class TicketType {
    STANDARD,
    VIP,
    EARLY_BIRD;
    
    fun isVip(): Boolean = this == VIP
    fun isEarlyBird(): Boolean = this == EARLY_BIRD
    
    fun displayName(): String = when (this) {
        STANDARD -> "Standard"
        VIP -> "VIP"
        EARLY_BIRD -> "Early Bird"
    }
    
    fun badgeColor(): Long = when (this) {
        STANDARD -> 0xFF4F46E5
        VIP -> 0xFFD97706
        EARLY_BIRD -> 0xFF059669
    }
}

enum class TicketStatus {
    ACTIVE,
    USED,
    CANCELLED,
    EXPIRED;
    
    fun isActive(): Boolean = this == ACTIVE
    fun canBeUsed(): Boolean = this == ACTIVE
}

sealed class TicketScanResult {
    data class Success(
        val ticket: Ticket,
        val event: Event,
        val checkInTime: Long = System.currentTimeMillis()
    ) : TicketScanResult()
    
    data class AlreadyCheckedIn(
        val ticket: Ticket,
        val checkedInAt: Long
    ) : TicketScanResult()
    
    data class InvalidTicket(
        val reason: String
    ) : TicketScanResult()
    
    data class WrongEvent(
        val ticketEventId: String,
        val scannedEventId: String
    ) : TicketScanResult()
    
    data class EventNotActive(
        val eventStatus: EventStatus
    ) : TicketScanResult()
    
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : TicketScanResult()
    
    fun isSuccess(): Boolean = this is Success
    fun isFailure(): Boolean = !isSuccess()
}
