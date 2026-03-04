package com.example.eventpay.domain.model

/**
 * Domain Entity - Ticket
 * 
 * Represents a ticket purchased for an event.
 * Contains business logic for ticket validation and check-in.
 */
data class Ticket(
    val id: String,
    val eventId: String,
    val userId: String,
    val ticketType: TicketType = TicketType.STANDARD,
    val price: Double,
    val purchaseDate: Long = System.currentTimeMillis(),
    val status: TicketStatus = TicketStatus.ACTIVE,
    val checkedInAt: Long? = null,
    val checkedInBy: String? = null,
    val qrCode: String,
    val seatNumber: String? = null,
    val notes: String? = null,
    val refundedAt: Long? = null,
    val refundReason: String? = null
) {
    /**
     * Check if the ticket has been used
     */
    val isUsed: Boolean
        get() = checkedInAt != null
    
    /**
     * Get the time when the ticket was used
     */
    val usedAt: Long?
        get() = checkedInAt
    
    /**
     * Check if the ticket can be used for check-in
     */
    fun canCheckIn(): Boolean {
        return status == TicketStatus.ACTIVE && checkedInAt == null
    }
    
    /**
     * Check if the ticket has been used
     */
    fun isCheckedIn(): Boolean = checkedInAt != null
    
    /**
     * Check if the ticket is valid (not expired, cancelled, or refunded)
     */
    fun isValid(): Boolean = status == TicketStatus.ACTIVE
    
    /**
     * Check if the ticket can be refunded
     */
    fun canBeRefunded(): Boolean {
        return status == TicketStatus.ACTIVE && checkedInAt == null
    }
    
    /**
     * Get the display name for the ticket type
     */
    fun ticketTypeDisplayName(): String = when (ticketType) {
        TicketType.STANDARD -> "Standard"
        TicketType.VIP -> "VIP"
        TicketType.PREMIUM -> "Premium"
        TicketType.EARLY_BIRD -> "Early Bird"
        TicketType.STUDENT -> "Student"
        TicketType.GROUP -> "Group"
        TicketType.PASS -> "Pass"
    }
    
    /**
     * Generate a unique identifier for QR code validation
     */
    fun qrValidationKey(): String = "$eventId:$id:$qrCode"
    
    /**
     * Validate ticket data
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (eventId.isBlank()) errors.add("Event ID is required")
        if (userId.isBlank()) errors.add("User ID is required")
        if (price < 0) errors.add("Price cannot be negative")
        if (qrCode.isBlank()) errors.add("QR code is required")
        
        return if (errors.isEmpty()) ValidationResult.Success else ValidationResult.Error(errors)
    }
}

/**
 * Types of tickets available
 */
enum class TicketType {
    STANDARD,       // Standard ticket
    VIP,            // VIP access with perks
    PREMIUM,        // Premium ticket
    EARLY_BIRD,     // Discounted early purchase
    STUDENT,        // Student discount ticket
    GROUP,          // Group discount ticket
    PASS;           // Pass ticket
    
    fun hasVipAccess(): Boolean = this == VIP || this == PREMIUM
    
    fun discountPercentage(): Int = when (this) {
        EARLY_BIRD -> 20
        STUDENT -> 15
        GROUP -> 15
        else -> 0
    }
}

/**
 * Ticket lifecycle status
 */
enum class TicketStatus {
    ACTIVE,         // Valid and can be used
    USED,           // Already checked in
    CANCELLED,      // Event was cancelled
    REFUNDED,       // Refunded to buyer
    EXPIRED;        // Past event date without use
    
    fun isActive(): Boolean = this == ACTIVE
    fun canBeUsed(): Boolean = this == ACTIVE
}

/**
 * Result of a QR code check-in operation (domain/legacy layer)
 */
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
