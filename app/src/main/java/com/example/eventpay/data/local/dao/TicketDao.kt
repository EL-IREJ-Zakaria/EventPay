package com.example.eventpay.data.local.dao

import androidx.room.*
import com.example.eventpay.data.model.Ticket
import kotlinx.coroutines.flow.Flow

@Dao
interface TicketDao {
    @Query("SELECT * FROM tickets")
    fun getAllTickets(): Flow<List<Ticket>>

    @Query("SELECT * FROM tickets WHERE id = :ticketId")
    suspend fun getTicketById(ticketId: String): Ticket?

    @Query("SELECT * FROM tickets WHERE qrCode = :qrCode")
    suspend fun getTicketByQRCode(qrCode: String): Ticket?

    @Query("SELECT * FROM tickets WHERE eventId = :eventId")
    fun getTicketsByEvent(eventId: String): Flow<List<Ticket>>

    @Query("SELECT * FROM tickets WHERE userId = :userId")
    fun getTicketsByUser(userId: String): Flow<List<Ticket>>

    @Query("SELECT * FROM tickets WHERE userId = :userId AND eventId = :eventId")
    suspend fun getTicketByUserAndEvent(userId: String, eventId: String): Ticket?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: Ticket)

    @Update
    suspend fun updateTicket(ticket: Ticket)

    @Delete
    suspend fun deleteTicket(ticket: Ticket)

    @Query("UPDATE tickets SET isCheckedIn = 1, checkedInAt = :timestamp WHERE id = :ticketId")
    suspend fun checkInTicket(ticketId: String, timestamp: Long)

    @Query("SELECT COUNT(*) FROM tickets WHERE eventId = :eventId AND isCheckedIn = 1")
    suspend fun getCheckedInCount(eventId: String): Int
}
