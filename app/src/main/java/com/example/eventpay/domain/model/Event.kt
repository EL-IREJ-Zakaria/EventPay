package com.example.eventpay.domain.model

/**
 * Domain Entity - Event
 * 
 * Pure Kotlin data class representing an event in the domain layer.
 * This is independent of any persistence framework or external library.
 * 
 * Domain entities contain business logic and validation rules.
 */
data class Event(
    val id: String,
    val name: String,
    val description: String,
    val location: String,
    val date: Long,
    val endDate: Long,
    val ticketPrice: Double,
    val totalTickets: Int,
    val soldTickets: Int = 0,
    val organizerId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val imageUrl: String? = null,
    val category: EventCategory = EventCategory.GENERAL,
    val status: EventStatus = EventStatus.DRAFT,
    val startTime: String = "09:00",
    val endTime: String = "18:00",
    val capacity: Int = totalTickets,
    val checkedInCount: Int = 0,
    val vipPrice: Double? = null,
    val vipTickets: Int = 0,
    val vipSold: Int = 0,
    val isPublished: Boolean = false,
    val tags: List<String> = emptyList(),
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    val website: String? = null,
    // Scanner assignment - CRITICAL for security
    val assignedScanners: List<String> = emptyList()
) {
    /**
     * Check if the event has available tickets
     */
    fun hasAvailableTickets(): Boolean = soldTickets < totalTickets
    
    /**
     * Get the number of available tickets
     */
    fun availableTickets(): Int = totalTickets - soldTickets
    
    /**
     * Check if the event has VIP tickets available
     */
    fun hasVipTicketsAvailable(): Boolean = vipTickets > 0 && vipSold < vipTickets
    
    /**
     * Get available VIP tickets count
     */
    fun availableVipTickets(): Int = vipTickets - vipSold
    
    /**
     * Check if the event is upcoming (not started yet)
     */
    fun isUpcoming(): Boolean = System.currentTimeMillis() < date
    
    /**
     * Check if the event is currently ongoing
     */
    fun isOngoing(): Boolean {
        val now = System.currentTimeMillis()
        return now in date..endDate
    }
    
    /**
     * Check if the event has ended
     */
    fun hasEnded(): Boolean = System.currentTimeMillis() > endDate
    
    /**
     * Calculate revenue from ticket sales
     */
    fun calculateRevenue(): Double {
        val regularRevenue = soldTickets * ticketPrice
        val vipRevenue = vipSold * (vipPrice ?: 0.0)
        return regularRevenue + vipRevenue
    }
    
    /**
     * Calculate check-in percentage
     */
    fun checkInPercentage(): Float {
        if (soldTickets == 0) return 0f
        return (checkedInCount.toFloat() / soldTickets) * 100
    }
    
    /**
     * Validate event data for creation/update
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (name.isBlank()) errors.add("Event name is required")
        if (description.isBlank()) errors.add("Description is required")
        if (location.isBlank()) errors.add("Location is required")
        if (date <= 0) errors.add("Valid date is required")
        if (endDate <= date) errors.add("End date must be after start date")
        if (ticketPrice < 0) errors.add("Ticket price cannot be negative")
        if (totalTickets <= 0) errors.add("Total tickets must be greater than 0")
        if (capacity < totalTickets) errors.add("Capacity cannot be less than total tickets")
        if (vipPrice != null && vipPrice < 0) errors.add("VIP price cannot be negative")
        if (vipTickets < 0) errors.add("VIP tickets cannot be negative")
        
        return if (errors.isEmpty()) ValidationResult.Success else ValidationResult.Error(errors)
    }
}

/**
 * Event categories for classification
 */
enum class EventCategory {
    CONFERENCE,
    WORKSHOP,
    SEMINAR,
    CONCERT,
    SPORTS,
    NETWORKING,
    EXHIBITION,
    FESTIVAL,
    GENERAL;
    
    fun displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }
}

/**
 * Event lifecycle status
 */
enum class EventStatus {
    DRAFT,        // Created but not published
    PUBLISHED,    // Published and visible to attendees
    ONGOING,      // Event is currently happening
    COMPLETED,    // Event has ended
    CANCELLED;    // Event was cancelled
    
    fun canBeEdited(): Boolean = this == DRAFT || this == PUBLISHED
    fun canBePublished(): Boolean = this == DRAFT
    fun canSellTickets(): Boolean = this == PUBLISHED || this == ONGOING
    fun canCheckIn(): Boolean = this == ONGOING
}

/**
 * Validation result sealed class
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val messages: List<String>) : ValidationResult()
    
    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error
}
