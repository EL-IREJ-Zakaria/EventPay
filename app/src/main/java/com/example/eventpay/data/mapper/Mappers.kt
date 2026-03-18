package com.example.eventpay.data.mapper

import com.example.eventpay.data.model.Event as DataEvent
import com.example.eventpay.data.model.EventCategory as DataEventCategory
import com.example.eventpay.data.model.EventStatus as DataEventStatus
import com.example.eventpay.domain.model.Event as DomainEvent
import com.example.eventpay.domain.model.EventCategory as DomainEventCategory
import com.example.eventpay.domain.model.EventStatus as DomainEventStatus

object EventMapper {
    
    fun DataEvent.toDomain(): DomainEvent {
        return DomainEvent(
            id = id,
            name = name,
            description = description,
            location = location,
            date = date,
            endDate = endDate,
            totalTickets = totalTickets,
            reservedTickets = reservedTickets,
            organizerId = organizerId,
            createdAt = createdAt,
            imageUrl = imageUrl,
            category = category.toDomain(),
            status = status.toDomain(),
            startTime = startTime,
            endTime = endTime,
            capacity = capacity,
            checkedInCount = checkedInCount,
            vipTickets = vipTickets,
            vipReserved = vipReserved,
            earlyBirdTickets = earlyBirdTickets,
            earlyBirdReserved = earlyBirdReserved,
            ticketPrice = ticketPrice,
            isPublished = isPublished,
            tags = tags,
            contactEmail = contactEmail,
            contactPhone = contactPhone,
            website = website,
            assignedScanners = assignedScanners
        )
    }
    
    fun DomainEvent.toData(): DataEvent {
        return DataEvent(
            id = id,
            name = name,
            description = description,
            location = location,
            date = date,
            endDate = endDate,
            totalTickets = totalTickets,
            reservedTickets = reservedTickets,
            organizerId = organizerId,
            createdAt = createdAt,
            imageUrl = imageUrl,
            category = category.toData(),
            status = status.toData(),
            startTime = startTime,
            endTime = endTime,
            capacity = capacity,
            checkedInCount = checkedInCount,
            vipTickets = vipTickets,
            vipReserved = vipReserved,
            earlyBirdTickets = earlyBirdTickets,
            earlyBirdReserved = earlyBirdReserved,
            ticketPrice = ticketPrice,
            isPublished = isPublished,
            tags = tags,
            contactEmail = contactEmail,
            contactPhone = contactPhone,
            website = website,
            assignedScanners = assignedScanners
        )
    }
    
    fun List<DataEvent>.toDomainList(): List<DomainEvent> = map { it.toDomain() }
    fun List<DomainEvent>.toDataList(): List<DataEvent> = map { it.toData() }
    
    private fun DataEventCategory.toDomain(): DomainEventCategory = DomainEventCategory.valueOf(name)
    private fun DomainEventCategory.toData(): DataEventCategory = DataEventCategory.valueOf(name)
    private fun DataEventStatus.toDomain(): DomainEventStatus = DomainEventStatus.valueOf(name)
    private fun DomainEventStatus.toData(): DataEventStatus = DataEventStatus.valueOf(name)
}
