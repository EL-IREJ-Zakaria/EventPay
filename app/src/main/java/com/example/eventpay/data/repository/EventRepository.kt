package com.example.eventpay.data.repository

import com.example.eventpay.data.local.dao.EventDao
import com.example.eventpay.data.model.Event
import kotlinx.coroutines.flow.Flow

class EventRepository(private val eventDao: EventDao) {
    val allEvents: Flow<List<Event>> = eventDao.getAllEvents()

    suspend fun getEventById(eventId: String): Event? {
        return eventDao.getEventById(eventId)
    }

    fun getEventsByOrganizer(organizerId: String): Flow<List<Event>> {
        return eventDao.getEventsByOrganizer(organizerId)
    }

    fun getUpcomingEvents(): Flow<List<Event>> {
        return eventDao.getUpcomingEvents(System.currentTimeMillis())
    }

    suspend fun createEvent(event: Event) {
        eventDao.insertEvent(event)
    }

    suspend fun updateEvent(event: Event) {
        eventDao.updateEvent(event)
    }

    suspend fun deleteEvent(event: Event) {
        eventDao.deleteEvent(event)
    }

    suspend fun incrementReservedTickets(eventId: String) {
        eventDao.incrementReservedTickets(eventId)
    }
}
