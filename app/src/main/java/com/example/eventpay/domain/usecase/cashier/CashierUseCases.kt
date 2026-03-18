package com.example.eventpay.domain.usecase.cashier

import com.example.eventpay.domain.model.Event
import com.example.eventpay.domain.model.EventStatus
import com.example.eventpay.domain.model.Ticket
import com.example.eventpay.domain.model.TicketType
import com.example.eventpay.domain.repository.EventRepository
import com.example.eventpay.domain.repository.TicketRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

/**
 * CMC School Event Management - Free Ticket Reservation
 * No payment processing - tickets are free
 */
class ReserveTicketUseCase @Inject constructor(
    private val ticketRepository: TicketRepository,
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(
        eventId: String,
        userId: String,
        ticketType: TicketType = TicketType.STANDARD,
        quantity: Int = 1
    ): Result<List<Ticket>> {
        val event = eventRepository.getEventById(eventId)
            ?: return Result.failure(Exception("Event not found"))
        
        val availableTickets = if (ticketType == TicketType.VIP) {
            event.availableVipTickets()
        } else {
            event.availableTickets()
        }
        
        if (availableTickets < quantity) {
            return Result.failure(Exception("Only $availableTickets tickets available"))
        }
        
        val tickets = mutableListOf<Ticket>()
        for (i in 1..quantity) {
            val ticketId = UUID.randomUUID().toString()
            val ticket = Ticket(
                id = ticketId,
                eventId = eventId,
                userId = userId,
                ticketType = ticketType,
                qrCode = ticketRepository.generateQRCode(ticketId, eventId),
                reservationDate = System.currentTimeMillis()
            )
            tickets.add(ticket)
        }
        
        return ticketRepository.createTickets(tickets)
    }
}

class GetEventsForCashierUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    operator fun invoke(): Flow<List<Event>> {
        return eventRepository.getEventsByStatus(EventStatus.PUBLISHED)
    }
}
