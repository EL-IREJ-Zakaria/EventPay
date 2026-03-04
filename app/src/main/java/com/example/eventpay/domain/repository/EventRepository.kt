package com.example.eventpay.domain.repository

import com.example.eventpay.domain.model.Event
import com.example.eventpay.domain.model.EventCategory
import com.example.eventpay.domain.model.EventStatus
import kotlinx.coroutines.flow.Flow

/**
 * Domain Repository Interface - EventRepository
 * 
 * Defines the contract for event data access operations.
 * This interface is implemented by the Data layer.
 * 
 * Following the Repository Pattern:
 * - Abstracts data source details from the domain layer
 * - Provides a clean API for use cases
 * - Supports both online and offline data access
 */
interface EventRepository {
    
    /**
     * Get all events as a Flow for reactive updates
     * @return Flow of list of events
     */
    fun getEvents(): Flow<List<Event>>
    
    /**
     * Get events filtered by status
     * @param status The status to filter by
     * @return Flow of filtered events
     */
    fun getEventsByStatus(status: EventStatus): Flow<List<Event>>
    
    /**
     * Get events filtered by category
     * @param category The category to filter by
     * @return Flow of filtered events
     */
    fun getEventsByCategory(category: EventCategory): Flow<List<Event>>
    
    /**
     * Get events organized by a specific user
     * @param organizerId The organizer's user ID
     * @return Flow of events organized by the user
     */
    fun getEventsByOrganizer(organizerId: String): Flow<List<Event>>
    
    /**
     * Get a single event by ID
     * @param eventId The event ID
     * @return The event or null if not found
     */
    suspend fun getEventById(eventId: String): Event?
    
    /**
     * Search events by name or description
     * @param query The search query
     * @return Flow of matching events
     */
    fun searchEvents(query: String): Flow<List<Event>>
    
    /**
     * Get published events (visible to attendees)
     * @return Flow of published events
     */
    fun getPublishedEvents(): Flow<List<Event>>
    
    /**
     * Get upcoming events (not yet started)
     * @return Flow of upcoming events
     */
    fun getUpcomingEvents(): Flow<List<Event>>
    
    /**
     * Create a new event
     * @param event The event to create
     * @return Result containing the created event or error
     */
    suspend fun createEvent(event: Event): Result<Event>
    
    /**
     * Update an existing event
     * @param event The event with updated data
     * @return Result indicating success or failure
     */
    suspend fun updateEvent(event: Event): Result<Unit>
    
    /**
     * Delete an event
     * @param eventId The ID of the event to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteEvent(eventId: String): Result<Unit>
    
    /**
     * Publish an event (make it visible to attendees)
     * @param eventId The ID of the event to publish
     * @return Result indicating success or failure
     */
    suspend fun publishEvent(eventId: String): Result<Unit>
    
    /**
     * Update ticket sales count
     * @param eventId The event ID
     * @param ticketsSold Number of additional tickets sold
     * @param vipTicketsSold Number of additional VIP tickets sold
     * @return Result indicating success or failure
     */
    suspend fun updateTicketSales(
        eventId: String,
        ticketsSold: Int,
        vipTicketsSold: Int = 0
    ): Result<Unit>
    
    /**
     * Update check-in count
     * @param eventId The event ID
     * @param increment Number of additional check-ins
     * @return Result indicating success or failure
     */
    suspend fun updateCheckInCount(eventId: String, increment: Int = 1): Result<Unit>
    
    /**
     * Sync local data with remote server
     * @return Result indicating sync success or failure
     */
    suspend fun syncWithRemote(): Result<Unit>
    
    /**
     * Get events that need to be synced
     * @return List of events pending sync
     */
    suspend fun getPendingSyncEvents(): List<Event>
}
