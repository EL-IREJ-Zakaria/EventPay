package com.example.eventpay.domain.usecase.ticket

import com.example.eventpay.domain.model.TicketScanResult
import com.example.eventpay.domain.model.Event
import com.example.eventpay.domain.model.EventStatus
import com.example.eventpay.domain.model.Ticket
import com.example.eventpay.domain.model.TicketStatus
import com.example.eventpay.domain.model.TicketType
import com.example.eventpay.domain.model.ValidationResult
import com.example.eventpay.domain.repository.EventRepository
import com.example.eventpay.domain.repository.TicketRepository
import com.example.eventpay.domain.repository.TransactionRepository
import com.example.eventpay.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

/**
 * Use Case: Purchase Ticket
 * 
 * Handles ticket purchase with payment processing.
 * Business rules:
 * - Event must be published or ongoing
 * - Tickets must be available
 * - User must have sufficient balance (for wallet payment)
 */
class PurchaseTicketUseCase @Inject constructor(
    private val ticketRepository: TicketRepository,
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(
        eventId: String,
        userId: String,
        ticketType: TicketType = TicketType.STANDARD,
        quantity: Int = 1
    ): Result<List<Ticket>> {
        // Validate event exists and can sell tickets
        val event = eventRepository.getEventById(eventId)
            ?: return Result.failure(Exception("Event not found"))
        
        if (!event.status.canSellTickets()) {
            return Result.failure(Exception("Event is not available for ticket purchase"))
        }
        
        // Check ticket availability
        val availableTickets = event.availableTickets()
        if (availableTickets < quantity) {
            return Result.failure(Exception("Only $availableTickets tickets available"))
        }
        
        // Calculate price
        val pricePerTicket = when (ticketType) {
            TicketType.VIP -> event.vipPrice ?: return Result.failure(Exception("VIP tickets not available for this event"))
            TicketType.EARLY_BIRD -> event.ticketPrice * (1 - ticketType.discountPercentage() / 100.0)
            else -> event.ticketPrice
        }
        
        val totalPrice = pricePerTicket * quantity
        
        // Check user balance
        val user = userRepository.getUserById(userId)
            ?: return Result.failure(Exception("User not found"))
        
        if (!user.hasSufficientBalance(totalPrice)) {
            return Result.failure(Exception("Insufficient balance. Required: $$totalPrice, Available: $${user.walletBalance}"))
        }
        
        // Create tickets
        val tickets = mutableListOf<Ticket>()
        for (i in 1..quantity) {
            val ticketId = UUID.randomUUID().toString()
            val ticket = Ticket(
                id = ticketId,
                eventId = eventId,
                userId = userId,
                ticketType = ticketType,
                price = pricePerTicket,
                qrCode = ticketRepository.generateQRCode(ticketId, eventId)
            )
            tickets.add(ticket)
        }
        
        // Create tickets in repository
        val createdTickets = ticketRepository.createTickets(tickets)
            .getOrElse { return Result.failure(it) }
        
        // Deduct from user wallet
        userRepository.updateWalletBalance(userId, -totalPrice)
        
        // Update event sales count
        eventRepository.updateTicketSales(eventId, quantity)
        
        return Result.success(createdTickets)
    }
}

/**
 * Use Case: Validate Ticket
 * 
 * Validates a ticket without checking in.
 */
class ValidateTicketUseCase @Inject constructor(
    private val ticketRepository: TicketRepository,
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(qrCode: String, eventId: String): TicketScanResult {
        return ticketRepository.validateTicket(qrCode, eventId)
    }
}

/**
 * Use Case: Check In Ticket
 * 
 * Performs ticket check-in with full validation.
 * Business rules:
 * - Ticket must be valid and active
 * - Ticket must belong to the event
 * - Event must be ongoing
 * - Ticket must not be already checked in
 */
class CheckInTicketUseCase @Inject constructor(
    private val ticketRepository: TicketRepository,
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(
        qrCode: String,
        eventId: String,
        checkedInBy: String
    ): TicketScanResult {
        // Get event and validate status
        val event = eventRepository.getEventById(eventId)
        if (event == null) {
            return TicketScanResult.InvalidTicket("Event not found")
        }
        
        if (!event.status.canCheckIn()) {
            return TicketScanResult.EventNotActive(event.status)
        }
        
        // Perform check-in
        val result = ticketRepository.checkInTicket(qrCode, eventId, checkedInBy)
        
        // Update event check-in count on success
        if (result is TicketScanResult.Success) {
            eventRepository.updateCheckInCount(eventId)
        }
        
        return result
    }
}

/**
 * Use Case: Get User Tickets
 * 
 * Retrieves all tickets for a user.
 */
class GetUserTicketsUseCase @Inject constructor(
    private val ticketRepository: TicketRepository
) {
    operator fun invoke(userId: String): Flow<List<Ticket>> {
        return ticketRepository.getTicketsByUser(userId)
    }
}

/**
 * Use Case: Get Event Tickets
 * 
 * Retrieves all tickets for an event.
 */
class GetEventTicketsUseCase @Inject constructor(
    private val ticketRepository: TicketRepository
) {
    operator fun invoke(eventId: String): Flow<List<Ticket>> {
        return ticketRepository.getTicketsByEvent(eventId)
    }
}

/**
 * Use Case: Get Ticket by ID
 * 
 * Retrieves a single ticket.
 */
class GetTicketUseCase @Inject constructor(
    private val ticketRepository: TicketRepository
) {
    suspend operator fun invoke(ticketId: String): Ticket? {
        return ticketRepository.getTicketById(ticketId)
    }
}

/**
 * Use Case: Get Ticket by QR Code
 * 
 * Retrieves a ticket by its QR code.
 */
class GetTicketByQRCodeUseCase @Inject constructor(
    private val ticketRepository: TicketRepository
) {
    suspend operator fun invoke(qrCode: String): Ticket? {
        return ticketRepository.getTicketByQRCode(qrCode)
    }
}

/**
 * Use Case: Refund Ticket
 * 
 * Processes a ticket refund.
 * Business rules:
 * - Ticket must be active and not checked in
 * - Refund amount is credited to user's wallet
 */
class RefundTicketUseCase @Inject constructor(
    private val ticketRepository: TicketRepository,
    private val userRepository: UserRepository,
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(ticketId: String, reason: String): Result<Unit> {
        val ticket = ticketRepository.getTicketById(ticketId)
            ?: return Result.failure(Exception("Ticket not found"))
        
        if (!ticket.canBeRefunded()) {
            return Result.failure(Exception("Ticket cannot be refunded"))
        }
        
        // Process refund
        ticketRepository.refundTicket(ticketId, reason)
        
        // Credit user wallet
        userRepository.updateWalletBalance(ticket.userId, ticket.price)
        
        // Update event sales count
        eventRepository.updateTicketSales(ticket.eventId, -1)
        
        return Result.success(Unit)
    }
}

/**
 * Use Case: Get Ticket Statistics
 * 
 * Gets ticket statistics for an event.
 */
class GetTicketStatsUseCase @Inject constructor(
    private val ticketRepository: TicketRepository
) {
    suspend operator fun invoke(eventId: String) = ticketRepository.getTicketStats(eventId)
}

/**
 * Use Case: Get Checked In Tickets
 * 
 * Gets all checked-in tickets for an event.
 */
class GetCheckedInTicketsUseCase @Inject constructor(
    private val ticketRepository: TicketRepository
) {
    operator fun invoke(eventId: String): Flow<List<Ticket>> {
        return ticketRepository.getCheckedInTickets(eventId)
    }
}
