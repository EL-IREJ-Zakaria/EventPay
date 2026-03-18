package com.example.eventpay.domain.usecase.ticket

import com.example.eventpay.domain.model.Ticket
import com.example.eventpay.domain.model.TicketType
import com.example.eventpay.domain.repository.EventRepository
import com.example.eventpay.domain.repository.TicketRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

/**
 * CMC School - Free Ticket Reservation Use Cases
 */
class ReserveTicketUseCase @Inject constructor(
    private val ticketRepository: TicketRepository,
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(
        eventId: String,
        userId: String,
        ticketType: TicketType = TicketType.STANDARD
    ): Result<Ticket> {
        val event = eventRepository.getEventById(eventId)
            ?: return Result.failure(Exception("Event not found"))
        
        if (!event.hasAvailableTickets()) {
            return Result.failure(Exception("No tickets available"))
        }
        
        val ticketId = UUID.randomUUID().toString()
        val ticket = Ticket(
            id = ticketId,
            eventId = eventId,
            userId = userId,
            ticketType = ticketType,
            qrCode = ticketRepository.generateQRCode(ticketId, eventId),
            reservationDate = System.currentTimeMillis()
        )
        
        return ticketRepository.createTicket(ticket)
    }
}

class GetUserTicketsUseCase @Inject constructor(
    private val ticketRepository: TicketRepository
) {
    operator fun invoke(userId: String): Flow<List<Ticket>> {
        return ticketRepository.getTicketsByUser(userId)
    }
}

class GetTicketByIdUseCase @Inject constructor(
    private val ticketRepository: TicketRepository
) {
    suspend operator fun invoke(ticketId: String): Ticket? {
        return ticketRepository.getTicketById(ticketId)
    }
}

class CancelTicketUseCase @Inject constructor(
    private val ticketRepository: TicketRepository
) {
    suspend operator fun invoke(ticketId: String, reason: String): Result<Unit> {
        // Ticket cancellation removed - tickets are free and non-refundable
        return Result.failure(Exception("Ticket cancellation not supported"))
    }
}
