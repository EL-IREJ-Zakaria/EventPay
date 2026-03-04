package com.example.eventpay.domain.usecase.cashier

import com.example.eventpay.domain.model.CashierShiftSummary
import com.example.eventpay.domain.model.Event
import com.example.eventpay.domain.model.EventStatus
import com.example.eventpay.domain.model.PaymentMethod
import com.example.eventpay.domain.model.Ticket
import com.example.eventpay.domain.model.TicketType
import com.example.eventpay.domain.model.Transaction
import com.example.eventpay.domain.model.TransactionStatus
import com.example.eventpay.domain.model.TransactionType
import com.example.eventpay.domain.repository.EventRepository
import com.example.eventpay.domain.repository.TicketRepository
import com.example.eventpay.domain.repository.TransactionRepository
import com.example.eventpay.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

/**
 * Use Case: Sell Ticket (Cashier)
 * 
 * Processes on-site ticket sales by cashier.
 * Supports cash, card, and mobile money payments.
 */
class SellTicketUseCase @Inject constructor(
    private val ticketRepository: TicketRepository,
    private val eventRepository: EventRepository,
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(
        eventId: String,
        buyerId: String?,
        cashierId: String,
        ticketType: TicketType = TicketType.STANDARD,
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        quantity: Int = 1
    ): Result<SaleResult> {
        // Validate cashier permissions
        val cashier = userRepository.getUserById(cashierId)
            ?: return Result.failure(Exception("Cashier not found"))
        
        if (!cashier.canSellTickets()) {
            return Result.failure(Exception("Permission denied"))
        }
        
        // Validate event
        val event = eventRepository.getEventById(eventId)
            ?: return Result.failure(Exception("Event not found"))
        
        if (!event.status.canSellTickets()) {
            return Result.failure(Exception("Event is not available for ticket sales"))
        }
        
        // Check availability
        val availableTickets = if (ticketType == TicketType.VIP) {
            event.availableVipTickets()
        } else {
            event.availableTickets()
        }
        
        if (availableTickets < quantity) {
            return Result.failure(Exception("Only $availableTickets tickets available"))
        }
        
        // Calculate price
        val pricePerTicket = when (ticketType) {
            TicketType.VIP -> event.vipPrice ?: event.ticketPrice
            TicketType.EARLY_BIRD -> event.ticketPrice * 0.8
            else -> event.ticketPrice
        }
        val totalAmount = pricePerTicket * quantity
        
        // Create tickets
        val tickets = mutableListOf<Ticket>()
        for (i in 1..quantity) {
            val ticketId = UUID.randomUUID().toString()
            val ticket = Ticket(
                id = ticketId,
                eventId = eventId,
                userId = buyerId ?: "walk-in-${System.currentTimeMillis()}",
                ticketType = ticketType,
                price = pricePerTicket,
                qrCode = ticketRepository.generateQRCode(ticketId, eventId)
            )
            tickets.add(ticket)
        }
        
        // Create transaction
        val transactionId = UUID.randomUUID().toString()
        val transaction = Transaction(
            id = transactionId,
            userId = buyerId ?: cashierId,
            eventId = eventId,
            ticketId = tickets.first().id,
            type = TransactionType.CASHIER_SALE,
            amount = totalAmount,
            paymentMethod = paymentMethod,
            status = TransactionStatus.COMPLETED,
            cashierId = cashierId,
            receiptNumber = "TXN-${System.currentTimeMillis()}-${transactionId.take(6)}".uppercase(),
            completedAt = System.currentTimeMillis()
        )
        
        // Save all data
        val createdTickets = ticketRepository.createTickets(tickets)
            .getOrElse { return Result.failure(it) }
        
        transactionRepository.createTransaction(transaction)
        
        // Update event sales
        eventRepository.updateTicketSales(eventId, quantity)
        
        return Result.success(SaleResult(
            tickets = createdTickets,
            transaction = transaction,
            totalAmount = totalAmount
        ))
    }
}

/**
 * Result of a ticket sale
 */
data class SaleResult(
    val tickets: List<Ticket>,
    val transaction: Transaction,
    val totalAmount: Double
)

/**
 * Use Case: Process Refund
 * 
 * Processes a ticket refund.
 */
class ProcessRefundUseCase @Inject constructor(
    private val ticketRepository: TicketRepository,
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(
        ticketId: String,
        reason: String,
        processedBy: String
    ): Result<Transaction> {
        // Validate processor permissions
        val processor = userRepository.getUserById(processedBy)
            ?: return Result.failure(Exception("User not found"))
        
        if (!processor.canSellTickets()) {
            return Result.failure(Exception("Permission denied"))
        }
        
        // Get ticket
        val ticket = ticketRepository.getTicketById(ticketId)
            ?: return Result.failure(Exception("Ticket not found"))
        
        if (!ticket.canBeRefunded()) {
            return Result.failure(Exception("Ticket cannot be refunded"))
        }
        
        // Process refund
        ticketRepository.refundTicket(ticketId, reason)
        
        // Create refund transaction
        val refundTransaction = transactionRepository.processRefund(
            originalTransactionId = ticketId,
            reason = reason,
            processedBy = processedBy
        )
        
        return refundTransaction
    }
}

/**
 * Use Case: Get Cashier Stats
 * 
 * Gets sales statistics for a cashier.
 */
class GetCashierStatsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(
        cashierId: String,
        startTime: Long,
        endTime: Long = System.currentTimeMillis()
    ): CashierStats {
        val transactions = transactionRepository.getTransactionsByCashier(cashierId)
        
        // This would be implemented with proper filtering in the repository
        return CashierStats(
            totalSales = 0.0,
            ticketsSold = 0,
            refundsProcessed = 0,
            refundAmount = 0.0
        )
    }
}

/**
 * Cashier statistics
 */
data class CashierStats(
    val totalSales: Double,
    val ticketsSold: Int,
    val refundsProcessed: Int,
    val refundAmount: Double
)

/**
 * Use Case: Start Cashier Shift
 * 
 * Starts a new cashier shift.
 */
class StartCashierShiftUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(
        cashierId: String,
        startingCash: Double
    ): Result<Long> {
        return transactionRepository.startCashierShift(cashierId, startingCash)
    }
}

/**
 * Use Case: End Cashier Shift
 * 
 * Ends the current cashier shift and calculates totals.
 */
class EndCashierShiftUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(
        cashierId: String,
        shiftStart: Long,
        actualCash: Double
    ): Result<CashierShiftSummary> {
        return transactionRepository.endCashierShift(cashierId, shiftStart, actualCash)
    }
}

/**
 * Use Case: Get Shift Summary
 * 
 * Gets summary for a cashier's shift.
 */
class GetShiftSummaryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(
        cashierId: String,
        shiftStart: Long
    ): CashierShiftSummary {
        return transactionRepository.getCashierShiftSummary(cashierId, shiftStart)
    }
}

/**
 * Use Case: Get Events for Cashier
 * 
 * Gets events available for ticket sales.
 */
class GetEventsForCashierUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    operator fun invoke(): Flow<List<Event>> {
        return eventRepository.getEventsByStatus(EventStatus.PUBLISHED)
    }
}
