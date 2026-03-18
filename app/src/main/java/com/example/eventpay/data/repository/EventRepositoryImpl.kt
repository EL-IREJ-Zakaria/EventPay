package com.example.eventpay.data.repository

import com.example.eventpay.data.local.dao.EventDao
import com.example.eventpay.data.mapper.EventMapper.toData
import com.example.eventpay.data.mapper.EventMapper.toDomain
import com.example.eventpay.data.mapper.EventMapper.toDomainList
import com.example.eventpay.domain.model.Event
import com.example.eventpay.domain.model.EventCategory
import com.example.eventpay.domain.model.EventStatus
import com.example.eventpay.domain.repository.EventRepository
import com.example.eventpay.data.firebase.FirestoreEventRepository
import com.example.eventpay.util.NetworkUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EventRepository Implementation
 * 
 * Implements the domain repository interface using:
 * - Room database for local storage (offline-first)
 * - Firestore for remote storage
 * 
 * Strategy:
 * 1. Read from local database first (fast, offline-capable)
 * 2. Sync with remote when online
 * 3. Queue changes when offline
 */
@Singleton
class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao,
    private val firestoreEventRepository: FirestoreEventRepository,
    private val networkUtils: NetworkUtils
) : EventRepository {
    
    override fun getEvents(): Flow<List<Event>> {
        // Return local data immediately, sync in background
        return eventDao.getAllEvents().map { it.toDomainList() }
    }
    
    override fun getEventsByStatus(status: EventStatus): Flow<List<Event>> {
        return eventDao.getEventsByStatus(status.name).map { it.toDomainList() }
    }
    
    override fun getEventsByCategory(category: EventCategory): Flow<List<Event>> {
        return eventDao.getEventsByCategory(category.name).map { it.toDomainList() }
    }
    
    override fun getEventsByOrganizer(organizerId: String): Flow<List<Event>> {
        return eventDao.getEventsByOrganizer(organizerId).map { it.toDomainList() }
    }
    
    override suspend fun getEventById(eventId: String): Event? {
        return eventDao.getEventById(eventId)?.toDomain()
    }
    
    override fun searchEvents(query: String): Flow<List<Event>> {
        val searchQuery = "%$query%"
        return eventDao.searchEvents(searchQuery).map { it.toDomainList() }
    }
    
    override fun getPublishedEvents(): Flow<List<Event>> {
        return eventDao.getPublishedEvents(true).map { it.toDomainList() }
    }
    
    override fun getUpcomingEvents(): Flow<List<Event>> {
        val currentTime = System.currentTimeMillis()
        return eventDao.getUpcomingEvents(currentTime).map { it.toDomainList() }
    }
    
    override suspend fun createEvent(event: Event): Result<Event> {
        return try {
            // Save to local database
            eventDao.insertEvent(event.toData())
            
            // Sync with remote if online
            if (networkUtils.isOnline()) {
                firestoreEventRepository.createEvent(event)
                    .onFailure { 
                        // Mark for sync later
                        // Could implement a sync queue here
                    }
            }
            
            Result.success(event)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateEvent(event: Event): Result<Unit> {
        return try {
            // Update local database
            eventDao.updateEvent(event.toData())
            
            // Sync with remote if online
            if (networkUtils.isOnline()) {
                firestoreEventRepository.updateEvent(event)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteEvent(eventId: String): Result<Unit> {
        return try {
            // Delete from local database
            eventDao.deleteEventById(eventId)
            
            // Delete from remote if online
            if (networkUtils.isOnline()) {
                firestoreEventRepository.deleteEvent(eventId)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun publishEvent(eventId: String): Result<Unit> {
        return try {
            val event = eventDao.getEventById(eventId)
                ?: return Result.failure(Exception("Event not found"))
            
            val publishedEvent = event.copy(
                status = com.example.eventpay.data.model.EventStatus.PUBLISHED,
                isPublished = true
            )
            
            eventDao.updateEvent(publishedEvent)
            
            if (networkUtils.isOnline()) {
                firestoreEventRepository.updateEvent(publishedEvent.toDomain())
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateTicketSales(
        eventId: String,
        ticketsSold: Int,
        vipTicketsSold: Int
    ): Result<Unit> {
        return try {
            eventDao.updateTicketReservations(eventId, ticketsSold, vipTicketsSold)
            
            if (networkUtils.isOnline()) {
                firestoreEventRepository.incrementreservedTickets(eventId)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateCheckInCount(eventId: String, increment: Int): Result<Unit> {
        return try {
            eventDao.updateCheckInCount(eventId, increment)
            
            if (networkUtils.isOnline()) {
                firestoreEventRepository.updateCheckInCount(eventId, increment)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun syncWithRemote(): Result<Unit> {
        if (!networkUtils.isOnline()) {
            return Result.failure(Exception("No network connection"))
        }
        
        return try {
            // Fetch remote events
            val remoteEvents = firestoreEventRepository.getEvents().first()
            
            // Update local database
            eventDao.insertEvents(remoteEvents.map { it.toData() })
            
            // Sync pending local changes
            // This would involve checking for unsynced items
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getPendingSyncEvents(): List<Event> {
        // This would query events marked for sync
        // For now, return empty list
        return emptyList()
    }
}
