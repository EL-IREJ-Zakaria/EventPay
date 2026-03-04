package com.example.eventpay.data.local.dao

import androidx.room.*
import com.example.eventpay.data.model.Event
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY date DESC")
    fun getAllEvents(): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE id = :eventId")
    suspend fun getEventById(eventId: String): Event?

    @Query("SELECT * FROM events WHERE organizerId = :organizerId")
    fun getEventsByOrganizer(organizerId: String): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE date >= :currentTime ORDER BY date ASC")
    fun getUpcomingEvents(currentTime: Long): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE status = :status")
    fun getEventsByStatus(status: String): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE category = :category")
    fun getEventsByCategory(category: String): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE name LIKE :query OR description LIKE :query")
    fun searchEvents(query: String): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE isPublished = :isPublished")
    fun getPublishedEvents(isPublished: Boolean): Flow<List<Event>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: Event)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<Event>)

    @Update
    suspend fun updateEvent(event: Event)

    @Delete
    suspend fun deleteEvent(event: Event)

    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun deleteEventById(eventId: String)

    @Query("UPDATE events SET soldTickets = soldTickets + :ticketsSold, vipSold = vipSold + :vipTicketsSold WHERE id = :eventId")
    suspend fun updateTicketSales(eventId: String, ticketsSold: Int, vipTicketsSold: Int)

    @Query("UPDATE events SET checkedInCount = checkedInCount + :increment WHERE id = :eventId")
    suspend fun updateCheckInCount(eventId: String, increment: Int)

    @Query("UPDATE events SET soldTickets = soldTickets + 1 WHERE id = :eventId")
    suspend fun incrementSoldTickets(eventId: String)

    @Query("SELECT SUM(soldTickets * ticketPrice) FROM events WHERE organizerId = :organizerId")
    suspend fun getTotalRevenue(organizerId: String): Double?
}
