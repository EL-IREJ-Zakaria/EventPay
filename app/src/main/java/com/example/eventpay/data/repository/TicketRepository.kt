package com.example.eventpay.data.repository

import com.example.eventpay.data.local.dao.TicketDao
import com.example.eventpay.data.model.Ticket
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class TicketRepository(private val ticketDao: TicketDao) {
    val allTickets: Flow<List<Ticket>> = ticketDao.getAllTickets()

    suspend fun getTicketById(ticketId: String): Ticket? {
        return ticketDao.getTicketById(ticketId)
    }

    suspend fun getTicketByQRCode(qrCode: String): Ticket? {
        return ticketDao.getTicketByQRCode(qrCode)
    }

    fun getTicketsByEvent(eventId: String): Flow<List<Ticket>> {
        return ticketDao.getTicketsByEvent(eventId)
    }

    fun getTicketsByUser(userId: String): Flow<List<Ticket>> {
        return ticketDao.getTicketsByUser(userId)
    }

    suspend fun getTicketByUserAndEvent(userId: String, eventId: String): Ticket? {
        return ticketDao.getTicketByUserAndEvent(userId, eventId)
    }

    suspend fun createTicket(ticket: Ticket) {
        ticketDao.insertTicket(ticket)
    }

    suspend fun purchaseTicket(
        eventId: String,
        userId: String,
        price: Double
    ): Ticket {
        val ticket = Ticket(
            id = UUID.randomUUID().toString(),
            eventId = eventId,
            userId = userId,
            qrCode = generateQRCode(),
            /* price = */ //  price,
            reservationDate = System.currentTimeMillis()
        )
        ticketDao.insertTicket(ticket)
        return ticket
    }

    suspend fun checkInTicket(ticketId: String): Boolean {
        val ticket = ticketDao.getTicketById(ticketId)
        return if (ticket != null && !ticket.isCheckedIn) {
            ticketDao.checkInTicket(ticketId, System.currentTimeMillis())
            true
        } else {
            false
        }
    }

    suspend fun getCheckedInCount(eventId: String): Int {
        return ticketDao.getCheckedInCount(eventId)
    }

    private fun generateQRCode(): String {
        return "TKT-${UUID.randomUUID()}"
    }

    suspend fun updateTicket(ticket: Ticket) {
        ticketDao.updateTicket(ticket)
    }

    suspend fun deleteTicket(ticket: Ticket) {
        ticketDao.deleteTicket(ticket)
    }
}
