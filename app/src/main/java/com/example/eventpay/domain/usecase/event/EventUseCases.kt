package com.example.eventpay.domain.usecase.event

import com.example.eventpay.domain.model.Event
import com.example.eventpay.domain.model.EventCategory
import com.example.eventpay.domain.model.EventStatus
import com.example.eventpay.domain.model.ValidationResult
import com.example.eventpay.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

/**
 * Use Case: Create Event
 * 
 * Creates a new event with validation.
 * Business rules:
 * - Event must have valid data
 * - Organizer must have permission
 * - Event starts in DRAFT status
 */
class CreateEventUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(
        name: String,
        description: String,
        location: String,
        date: Long,
        endDate: Long,
        ticketPrice: Double,
        totalTickets: Int,
        organizerId: String,
        category: EventCategory = EventCategory.GENERAL,
        imageUrl: String? = null,
        vipPrice: Double? = null,
        vipTickets: Int = 0
    ): Result<Event> {
        val event = Event(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            location = location,
            date = date,
            endDate = endDate,
            /* ticket/* price = */ //  */ //  ticketPrice,
            totalTickets = totalTickets,
            organizerId = organizerId,
            category = category,
            imageUrl = imageUrl,
            /* vip/* price = */ //  */ //  vipPrice,
            vipTickets = vipTickets,
            status = EventStatus.DRAFT,
            createdAt = System.currentTimeMillis()
        )
        
        // Validate event data
        val validation = event.validate()
        if (validation is ValidationResult.Error) {
            return Result.failure(Exception(validation.messages.joinToString(", ")))
        }
        
        return eventRepository.createEvent(event)
    }
}

/**
 * Use Case: Update Event
 * 
 * Updates an existing event with validation.
 * Business rules:
 * - Event must exist
 * - Event must be editable (DRAFT or PUBLISHED)
 * - Cannot reduce total tickets below sold count
 */
class UpdateEventUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(event: Event): Result<Unit> {
        // Get existing event
        val existingEvent = eventRepository.getEventById(event.id)
            ?: return Result.failure(Exception("Event not found"))
        
        // Check if event can be edited
        if (!existingEvent.status.canBeEdited()) {
            return Result.failure(Exception("Event cannot be edited in ${existingEvent.status} status"))
        }
        
        // Validate new data
        val validation = event.validate()
        if (validation is ValidationResult.Error) {
            return Result.failure(Exception(validation.messages.joinToString(", ")))
        }
        
        // Cannot reduce total tickets below sold count
        if (event.totalTickets < existingEvent.reservedTickets) {
            return Result.failure(Exception("Cannot reduce total tickets below sold count (${existingEvent.reservedTickets})"))
        }
        
        return eventRepository.updateEvent(event)
    }
}

/**
 * Use Case: Delete Event
 * 
 * Deletes or cancels an event.
 * Business rules:
 * - Can delete DRAFT events
 * - PUBLISHED events are cancelled instead
 * - Refunds are processed for sold tickets
 */
class DeleteEventUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(eventId: String): Result<Unit> {
        val event = eventRepository.getEventById(eventId)
            ?: return Result.failure(Exception("Event not found"))
        
        // If event has sold tickets, cancel instead of delete
        if (event.reservedTickets > 0) {
            val cancelledEvent = event.copy(status = EventStatus.CANCELLED)
            return eventRepository.updateEvent(cancelledEvent)
        }
        
        return eventRepository.deleteEvent(eventId)
    }
}

/**
 * Use Case: Get Event
 * 
 * Retrieves a single event by ID.
 */
class GetEventUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(eventId: String): Event? {
        return eventRepository.getEventById(eventId)
    }
}

/**
 * Use Case: Get Events
 * 
 * Retrieves all events as a Flow for reactive updates.
 */
class GetEventsUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    operator fun invoke(): Flow<List<Event>> {
        return eventRepository.getEvents()
    }
}

/**
 * Use Case: Get Events by Organizer
 * 
 * Retrieves events organized by a specific user.
 */
class GetEventsByOrganizerUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    operator fun invoke(organizerId: String): Flow<List<Event>> {
        return eventRepository.getEventsByOrganizer(organizerId)
    }
}

/**
 * Use Case: Get Published Events
 * 
 * Retrieves events visible to attendees.
 */
class GetPublishedEventsUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    operator fun invoke(): Flow<List<Event>> {
        return eventRepository.getPublishedEvents()
    }
}

/**
 * Use Case: Publish Event
 * 
 * Publishes an event to make it visible to attendees.
 * Business rules:
 * - Event must be in DRAFT status
 * - Event must have valid data
 */
class PublishEventUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(eventId: String): Result<Unit> {
        val event = eventRepository.getEventById(eventId)
            ?: return Result.failure(Exception("Event not found"))
        
        if (!event.status.canBePublished()) {
            return Result.failure(Exception("Event cannot be published from ${event.status} status"))
        }
        
        // Validate event before publishing
        val validation = event.validate()
        if (validation is ValidationResult.Error) {
            return Result.failure(Exception("Event validation failed: ${validation.messages.joinToString(", ")}"))
        }
        
        return eventRepository.publishEvent(eventId)
    }
}

/**
 * Use Case: Search Events
 * 
 * Searches events by name or description.
 */
class SearchEventsUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    operator fun invoke(query: String): Flow<List<Event>> {
        if (query.isBlank()) {
            return eventRepository.getPublishedEvents()
        }
        return eventRepository.searchEvents(query)
    }
}

/**
 * Use Case: Get Events by Category
 * 
 * Filters events by category.
 */
class GetEventsByCategoryUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    operator fun invoke(category: EventCategory): Flow<List<Event>> {
        return eventRepository.getEventsByCategory(category)
    }
}

/**
 * Use Case: Sync Events
 * 
 * Syncs local event data with remote server.
 */
class SyncEventsUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return eventRepository.syncWithRemote()
    }
}
