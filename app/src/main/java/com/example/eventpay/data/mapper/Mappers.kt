package com.example.eventpay.data.mapper

import com.example.eventpay.data.model.Event as DataEvent
import com.example.eventpay.data.model.EventCategory as DataEventCategory
import com.example.eventpay.data.model.EventStatus as DataEventStatus
import com.example.eventpay.domain.model.Event as DomainEvent
import com.example.eventpay.domain.model.EventCategory as DomainEventCategory
import com.example.eventpay.domain.model.EventStatus as DomainEventStatus

/**
 * Mapper for Event entity
 * 
 * Converts between domain and data layer representations.
 * Domain models are pure Kotlin classes.
 * Data models are Room entities with persistence annotations.
 */
object EventMapper {
    
    /**
     * Convert from Data layer to Domain layer
     */
    fun DataEvent.toDomain(): DomainEvent {
        return DomainEvent(
            id = id,
            name = name,
            description = description,
            location = location,
            date = date,
            endDate = endDate,
            ticketPrice = ticketPrice,
            totalTickets = totalTickets,
            soldTickets = soldTickets,
            organizerId = organizerId,
            createdAt = createdAt,
            imageUrl = imageUrl,
            category = category.toDomain(),
            status = status.toDomain(),
            startTime = startTime,
            endTime = endTime,
            capacity = capacity,
            checkedInCount = checkedInCount,
            vipPrice = vipPrice,
            vipTickets = vipTickets,
            vipSold = vipSold,
            isPublished = isPublished,
            tags = tags,
            contactEmail = contactEmail,
            contactPhone = contactPhone,
            website = website,
            assignedScanners = assignedScanners
        )
    }
    
    /**
     * Convert from Domain layer to Data layer
     */
    fun DomainEvent.toData(): DataEvent {
        return DataEvent(
            id = id,
            name = name,
            description = description,
            location = location,
            date = date,
            endDate = endDate,
            ticketPrice = ticketPrice,
            totalTickets = totalTickets,
            soldTickets = soldTickets,
            organizerId = organizerId,
            createdAt = createdAt,
            imageUrl = imageUrl,
            category = category.toData(),
            status = status.toData(),
            startTime = startTime,
            endTime = endTime,
            capacity = capacity,
            checkedInCount = checkedInCount,
            vipPrice = vipPrice,
            vipTickets = vipTickets,
            vipSold = vipSold,
            isPublished = isPublished,
            tags = tags,
            contactEmail = contactEmail,
            contactPhone = contactPhone,
            website = website,
            assignedScanners = assignedScanners
        )
    }
    
    /**
     * Convert list of data events to domain
     */
    fun List<DataEvent>.toDomainList(): List<DomainEvent> {
        return map { it.toDomain() }
    }
    
    /**
     * Convert list of domain events to data
     */
    fun List<DomainEvent>.toDataList(): List<DataEvent> {
        return map { it.toData() }
    }
    
    // Category mapping
    private fun DataEventCategory.toDomain(): DomainEventCategory {
        return DomainEventCategory.valueOf(name)
    }
    
    private fun DomainEventCategory.toData(): DataEventCategory {
        return DataEventCategory.valueOf(name)
    }
    
    // Status mapping
    private fun DataEventStatus.toDomain(): DomainEventStatus {
        return DomainEventStatus.valueOf(name)
    }
    
    private fun DomainEventStatus.toData(): DataEventStatus {
        return DataEventStatus.valueOf(name)
    }
}
