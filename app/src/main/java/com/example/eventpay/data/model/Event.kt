package com.example.eventpay.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class Event(
    @PrimaryKey
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val location: String = "",
    val date: Long = 0L,
    val endDate: Long = 0L,
    val totalTickets: Int = 0,
    val reservedTickets: Int = 0,
    val organizerId: String = "",
    val createdAt: Long = 0L,
    val imageUrl: String? = null,
    val category: EventCategory = EventCategory.GENERAL,
    val status: EventStatus = EventStatus.DRAFT,
    val startTime: String = "09:00",
    val endTime: String = "18:00",
    val capacity: Int = 0,
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
)

enum class EventCategory {
    CONFERENCE,
    WORKSHOP,
    SEMINAR,
    CONCERT,
    SPORTS,
    NETWORKING,
    EXHIBITION,
    FESTIVAL,
    GENERAL
}

enum class EventStatus {
    DRAFT,
    PUBLISHED,
    ONGOING,
    COMPLETED,
    CANCELLED
}
