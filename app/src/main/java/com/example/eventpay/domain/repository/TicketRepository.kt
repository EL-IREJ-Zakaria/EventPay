package com.example.eventpay.domain.repository

import com.example.eventpay.domain.model.TicketScanResult
import com.example.eventpay.domain.model.Ticket
import com.example.eventpay.domain.model.TicketStatus
import com.example.eventpay.domain.model.TicketType
import kotlinx.coroutines.flow.Flow

/**
 * Domain Repository Interface - TicketRepository
 * 
 * Defines the contract for ticket data access operations.
 * Handles ticket creation, validation, and check-in functionality.
 */
interface TicketRepository {
    
    /**
     * Get all tickets for a specific event
     * @param eventId The event ID
     * @return Flow of tickets for the event
     */
    fun getTicketsByEvent(eventId: String): Flow<List<Ticket>>
    
    /**
     * Get all tickets owned by a user
     * @param userId The user ID
     * @return Flow of user's tickets
     */
    fun getTicketsByUser(userId: String): Flow<List<Ticket>>
    
    /**
     * Get a single ticket by ID
     * @param ticketId The ticket ID
     * @return The ticket or null if not found
     */
    suspend fun getTicketById(ticketId: String): Ticket?
    
    /**
     * Get a ticket by its QR code
     * @param qrCode The QR code string
     * @return The ticket or null if not found
     */
    suspend fun getTicketByQRCode(qrCode: String): Ticket?
    
    /**
     * Get tickets by status
     * @param eventId The event ID
     * @param status The ticket status
     * @return Flow of filtered tickets
     */
    fun getTicketsByStatus(eventId: String, status: TicketStatus): Flow<List<Ticket>>
    
    /**
     * Get tickets by type
     * @param eventId The event ID
     * @param type The ticket type
     * @return Flow of filtered tickets
     */
    fun getTicketsByType(eventId: String, type: TicketType): Flow<List<Ticket>>
    
    /**
     * Get checked-in tickets for an event
     * @param eventId The event ID
     * @return Flow of checked-in tickets
     */
    fun getCheckedInTickets(eventId: String): Flow<List<Ticket>>
    
    /**
     * Create a new ticket
     * @param ticket The ticket to create
     * @return Result containing the created ticket or error
     */
    suspend fun createTicket(ticket: Ticket): Result<Ticket>
    
    /**
     * Create multiple tickets (batch operation)
     * @param tickets List of tickets to create
     * @return Result containing the created tickets or error
     */
    suspend fun createTickets(tickets: List<Ticket>): Result<List<Ticket>>
    
    /**
     * Update an existing ticket
     * @param ticket The ticket with updated data
     * @return Result indicating success or failure
     */
    suspend fun updateTicket(ticket: Ticket): Result<Unit>
    
    /**
     * Check in a ticket using QR code
     * @param qrCode The QR code to validate
     * @param eventId The event ID for validation
     * @param checkedInBy The user ID of the person checking in
     * @return CheckInResult indicating success or failure reason
     */
    suspend fun checkInTicket(
        qrCode: String,
        eventId: String,
        checkedInBy: String
    ): TicketScanResult
    
    /**
     * Validate a ticket without checking in
     * @param qrCode The QR code to validate
     * @param eventId The event ID for validation
     * @return TicketScanResult with validation status
     */
    suspend fun validateTicket(qrCode: String, eventId: String): TicketScanResult
    
    /**
     * Refund a ticket
     * @param ticketId The ticket ID to refund
     * @param reason The reason for refund
     * @return Result indicating success or failure
     */
    suspend fun refundTicket(ticketId: String, reason: String): Result<Unit>
    
    /**
     * Cancel tickets for an event (when event is cancelled)
     * @param eventId The event ID
     * @return Result indicating success or failure
     */
    suspend fun cancelTicketsForEvent(eventId: String): Result<Unit>
    
    /**
     * Get ticket statistics for an event
     * @param eventId The event ID
     * @return TicketStats containing sales and check-in statistics
     */
    suspend fun getTicketStats(eventId: String): TicketStats
    
    /**
     * Generate a unique QR code for a ticket
     * @param ticketId The ticket ID
     * @param eventId The event ID
     * @return Generated QR code string
     */
    fun generateQRCode(ticketId: String, eventId: String): String
    
    /**
     * Sync local data with remote server
     * @return Result indicating sync success or failure
     */
    suspend fun syncWithRemote(): Result<Unit>
}

/**
 * Ticket statistics for an event
 */
data class TicketStats(
    val totalTickets: Int = 0,
    val reservedTickets: Int = 0,
    val checkedInTickets: Int = 0,
    val refundedTickets: Int = 0,
    val regularTickets: Int = 0,
    val vipTickets: Int = 0,
    val earlyBirdTickets: Int = 0,
    val totalRevenue: Double = 0.0,
    val refundAmount: Double = 0.0
) {
    fun checkInRate(): Float {
        return if (reservedTickets > 0) (checkedInTickets.toFloat() / reservedTickets) * 100 else 0f
    }
    
    fun salesRate(): Float {
        return if (totalTickets > 0) (reservedTickets.toFloat() / totalTickets) * 100 else 0f
    }
    
    fun refundRate(): Float {
        return if (reservedTickets > 0) (refundedTickets.toFloat() / reservedTickets) * 100 else 0f
    }
}
