package com.example.eventpay.domain.model

/**
 * Domain Entity - Event
 * CMC School Event Management - Free events with VIP allocation
 */
data class Event(
    val id: String,
    val name: String,
    val description: String,
    val location: String,
    val date: Long,
    val endDate: Long,
    val totalTickets: Int,
    val reservedTickets: Int = 0,
    val organizerId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val imageUrl: String? = null,
    val category: EventCategory = EventCategory.GENERAL,
    val status: EventStatus = EventStatus.DRAFT,
    val startTime: String = "09:00",
    val endTime: String = "18:00",
    val capacity: Int = totalTickets,
    val checkedInCount: Int = 0,
    val vipTickets: Int = 0,
    val vipReserved: Int = 0,
    val earlyBirdTickets: Int = 0,
    val earlyBirdReserved: Int = 0,
    val ticketPrice: Double = 0.0,
    val isPublished: Boolean = false,
    val tags: List<String> = emptyList(),
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    val website: String? = null,
    val assignedScanners: List<String> = emptyList()
) {
    fun hasAvailableTickets(): Boolean = reservedTickets < totalTickets
    fun availableTickets(): Int = totalTickets - reservedTickets
    fun hasVipTicketsAvailable(): Boolean = vipTickets > 0 && vipReserved < vipTickets
    fun availableVipTickets(): Int = vipTickets - vipReserved
    fun isUpcoming(): Boolean = System.currentTimeMillis() < date
    fun isOngoing(): Boolean {
        val now = System.currentTimeMillis()
        return now in date..endDate
    }
    fun hasEnded(): Boolean = System.currentTimeMillis() > endDate
    
    fun checkInPercentage(): Float {
        if (reservedTickets == 0) return 0f
        return (checkedInCount.toFloat() / reservedTickets) * 100
    }
    
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        if (name.isBlank()) errors.add("Event name is required")
        if (description.isBlank()) errors.add("Description is required")
        if (location.isBlank()) errors.add("Location is required")
        if (date <= 0) errors.add("Valid date is required")
        if (endDate <= date) errors.add("End date must be after start date")
        if (totalTickets <= 0) errors.add("Total tickets must be greater than 0")
        if (capacity < totalTickets) errors.add("Capacity cannot be less than total tickets")
        if (vipTickets < 0) errors.add("VIP tickets cannot be negative")
        return if (errors.isEmpty()) ValidationResult.Success else ValidationResult.Error(errors)
    }
}

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

enum class EventStatus {
    DRAFT,
    PUBLISHED,
    ONGOING,
    COMPLETED,
    CANCELLED;
    
    fun canBeEdited(): Boolean = this == DRAFT || this == PUBLISHED
    fun canBePublished(): Boolean = this == DRAFT
    fun canReserveTickets(): Boolean = this == PUBLISHED || this == ONGOING
    fun canCheckIn(): Boolean = this == ONGOING
}
