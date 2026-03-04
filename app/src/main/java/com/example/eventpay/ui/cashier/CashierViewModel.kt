package com.example.eventpay.ui.cashier

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventpay.data.firebase.FirebaseService
import com.example.eventpay.data.firebase.FirestoreEventRepository
import com.example.eventpay.data.firebase.FirestoreTicketRepository
import com.example.eventpay.data.firebase.FirestoreTransactionRepository
import com.example.eventpay.data.model.PaymentMethod
import com.example.eventpay.data.model.Ticket
import com.example.eventpay.data.model.TicketType
import com.example.eventpay.data.model.Transaction
import com.example.eventpay.data.model.TransactionType
import com.example.eventpay.domain.model.CashierShift
import com.example.eventpay.domain.model.Event
import com.example.eventpay.domain.model.ShiftReport
import com.example.eventpay.domain.model.ShiftStatus
import com.example.eventpay.domain.qrcode.QRCodeGenerationRequest
import com.example.eventpay.domain.qrcode.QRCodeGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Cashier UI State
 */
data class CashierUiState(
    // Loading states
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    
    // Shift management
    val currentShift: CashierShift? = null,
    val isShiftActive: Boolean = false,
    val shiftStartCash: Double = 0.0,
    val shiftEndCash: Double = 0.0,
    
    // Events
    val events: List<Event> = emptyList(),
    val selectedEvent: Event? = null,
    
    // Sales
    val tickets: List<Ticket> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val lastSoldTicket: Ticket? = null,
    val lastTransaction: Transaction? = null,
    
    // Daily stats
    val dailySalesTotal: Double = 0.0,
    val dailyTicketCount: Int = 0,
    val cashTotal: Double = 0.0,
    val cardTotal: Double = 0.0,
    val mobileTotal: Double = 0.0,
    
    // Shift stats
    val shiftSalesTotal: Double = 0.0,
    val shiftTicketCount: Int = 0,
    val shiftCashTotal: Double = 0.0,
    val shiftCardTotal: Double = 0.0,
    val shiftMobileTotal: Double = 0.0,
    
    // QR Code
    val generatedQRCode: String? = null,
    val showQRDialog: Boolean = false,
    
    // Dialogs
    val showStartShiftDialog: Boolean = false,
    val showEndShiftDialog: Boolean = false,
    val showSellDialog: Boolean = false,
    val showRefundDialog: Boolean = false,
    val showReportDialog: Boolean = false,
    
    // Messages
    val error: String? = null,
    val success: String? = null
)

/**
 * Cashier ViewModel
 * 
 * Manages cashier operations including:
 * - Shift management (start/end)
 * - Ticket sales
 * - Payment processing
 * - QR code generation
 * - Daily closing reports
 */
class CashierViewModel(
    private val eventRepository: FirestoreEventRepository,
    private val ticketRepository: FirestoreTicketRepository,
    private val transactionRepository: FirestoreTransactionRepository,
    private val firebaseService: FirebaseService,
    private val qrCodeGenerator: QRCodeGenerator
) : ViewModel() {
    
    private val _state = MutableStateFlow(CashierUiState())
    val state: StateFlow<CashierUiState> = _state.asStateFlow()
    
    private val userId: String?
        get() = firebaseService.getCurrentUserId()
    
    init {
        loadEvents()
        checkActiveShift()
    }
    
    /**
     * Load available events for ticket sales
     */
    private fun loadEvents() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            eventRepository.getAllEvents().fold(
                onSuccess = { events ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        events = events.filter { 
                            it.totalTickets > it.soldTickets 
                        }
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load events"
                    )
                }
            )
        }
    }
    
    /**
     * Check if there's an active shift for the current cashier
     */
    private fun checkActiveShift() {
        viewModelScope.launch {
            val cashierId = userId ?: return@launch
            
            // Check for active shift in local storage or Firestore
            // For now, we'll assume no active shift on app start
            // In production, this would check Firestore for an active shift
        }
    }
    
    /**
     * Start a new cashier shift
     */
    fun startShift(startingCash: Double) {
        viewModelScope.launch {
            val cashierId = userId ?: return@launch
            
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            val shiftId = UUID.randomUUID().toString()
            val shift = CashierShift(
                id = shiftId,
                cashierId = cashierId,
                cashierName = "Cashier", // Would get from user profile
                shiftStart = System.currentTimeMillis(),
                startingCash = startingCash,
                status = ShiftStatus.ACTIVE
            )
            
            // Save shift to Firestore
            // For now, update local state
            _state.value = _state.value.copy(
                isLoading = false,
                currentShift = shift,
                isShiftActive = true,
                shiftStartCash = startingCash,
                showStartShiftDialog = false,
                success = "Shift started successfully"
            )
            
            loadDailyTransactions()
        }
    }
    
    /**
     * End the current shift
     */
    fun endShift(actualCash: Double, notes: String?) {
        viewModelScope.launch {
            val currentShift = _state.value.currentShift ?: return@launch
            
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            val completedShift = currentShift.copy(
                shiftEnd = System.currentTimeMillis(),
                actualCash = actualCash,
                cashSales = _state.value.shiftCashTotal,
                cardSales = _state.value.shiftCardTotal,
                mobileSales = _state.value.shiftMobileTotal,
                totalSales = _state.value.shiftSalesTotal,
                ticketsSold = _state.value.shiftTicketCount,
                transactionsCount = _state.value.transactions.size,
                status = ShiftStatus.COMPLETED,
                notes = notes
            )
            
            // Generate shift report
            val report = completedShift.generateReport()
            
            // Save to Firestore
            // For now, update local state
            _state.value = _state.value.copy(
                isLoading = false,
                currentShift = null,
                isShiftActive = false,
                shiftEndCash = actualCash,
                showEndShiftDialog = false,
                success = "Shift ended. Total sales: ${completedShift.totalSales} MAD"
            )
        }
    }
    
    /**
     * Select an event for ticket sales
     */
    fun selectEvent(event: Event) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                selectedEvent = event,
                isLoading = true
            )
            loadEventTickets(event.id)
            loadDailyTransactions()
        }
    }
    
    /**
     * Load tickets for a specific event
     */
    private suspend fun loadEventTickets(eventId: String) {
        ticketRepository.getTicketsByEventFlow(eventId).collect { tickets ->
            _state.value = _state.value.copy(tickets = tickets)
        }
    }
    
    /**
     * Load daily transactions for the current cashier
     */
    fun loadDailyTransactions() {
        viewModelScope.launch {
            val cashierId = userId ?: return@launch
            
            // Get start and end of today
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val endOfDay = calendar.timeInMillis
            
            transactionRepository.getDailyTransactions(cashierId, startOfDay, endOfDay).fold(
                onSuccess = { transactions ->
                    val salesTransactions = transactions.filter { 
                        it.type == TransactionType.TICKET_PURCHASE ||
                        it.type == TransactionType.CASHIER_SALE
                    }
                    
                    var cashTotal = 0.0
                    var cardTotal = 0.0
                    var mobileTotal = 0.0
                    
                    salesTransactions.forEach { txn ->
                        when (txn.paymentMethod) {
                            PaymentMethod.CASH -> cashTotal += txn.amount
                            PaymentMethod.CARD -> cardTotal += txn.amount
                            PaymentMethod.MOBILE_MONEY -> mobileTotal += txn.amount
                            else -> {}
                        }
                    }
                    
                    _state.value = _state.value.copy(
                        isLoading = false,
                        transactions = transactions,
                        dailySalesTotal = salesTransactions.sumOf { it.amount },
                        dailyTicketCount = salesTransactions.size,
                        cashTotal = cashTotal,
                        cardTotal = cardTotal,
                        mobileTotal = mobileTotal
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load transactions"
                    )
                }
            )
        }
    }
    
    /**
     * Sell a ticket
     */
    fun sellTicket(
        eventId: String,
        ticketType: com.example.eventpay.data.model.TicketType,
        price: Double,
        paymentMethod: com.example.eventpay.data.model.PaymentMethod,
        customerName: String,
        customerPhone: String? = null
    ) {
        viewModelScope.launch {
            val cashierId = userId ?: return@launch
            
            _state.value = _state.value.copy(isProcessing = true, error = null)
            
            // Create ticket
            ticketRepository.createTicket(eventId, cashierId, ticketType, price).fold(
                onSuccess = { ticket ->
                    // Generate QR code for the ticket
                    val qrResult = qrCodeGenerator.generateQRCode(
                        QRCodeGenerationRequest(
                            ticketId = ticket.id,
                            eventId = eventId,
                            userId = cashierId,
                            ticketType = ticketType.name
                        )
                    )
                    val qrCode = when (qrResult) {
                        is com.example.eventpay.domain.qrcode.QRCodeGenerationResult.Success -> qrResult.qrCodeString
                        is com.example.eventpay.domain.qrcode.QRCodeGenerationResult.Failure -> null
                    }
                    
                    // Create transaction
                    transactionRepository.createTransaction(
                        userId = cashierId,
                        type = TransactionType.CASHIER_SALE,
                        amount = price,
                        description = buildString {
                            append("$ticketType ticket")
                            if (customerName.isNotBlank()) {
                                append(" - $customerName")
                            }
                            if (!customerPhone.isNullOrBlank()) {
                                append(" ($customerPhone)")
                            }
                        },
                        paymentMethod = paymentMethod,
                        eventId = eventId,
                        ticketId = ticket.id
                    ).fold(
                        onSuccess = { transaction ->
                            // Update event sold tickets count
                            eventRepository.incrementSoldTickets(eventId)
                            
                            // Update shift stats
                            updateShiftStats(price, paymentMethod)
                            
                            _state.value = _state.value.copy(
                                isProcessing = false,
                                lastSoldTicket = ticket.copy(qrCode = qrCode ?: ticket.qrCode),
                                lastTransaction = transaction,
                                generatedQRCode = qrCode,
                                showQRDialog = true,
                                showSellDialog = false,
                                success = "Ticket sold successfully!"
                            )
                            
                            loadDailyTransactions()
                        },
                        onFailure = { error ->
                            _state.value = _state.value.copy(
                                isProcessing = false,
                                error = error.message ?: "Failed to create transaction"
                            )
                        }
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isProcessing = false,
                        error = error.message ?: "Failed to create ticket"
                    )
                }
            )
        }
    }
    
    /**
     * Update shift statistics after a sale
     */
    private fun updateShiftStats(amount: Double, paymentMethod: PaymentMethod) {
        val currentState = _state.value
        
        when (paymentMethod) {
            PaymentMethod.CASH -> {
                _state.value = currentState.copy(
                    shiftCashTotal = currentState.shiftCashTotal + amount,
                    shiftSalesTotal = currentState.shiftSalesTotal + amount,
                    shiftTicketCount = currentState.shiftTicketCount + 1
                )
            }
            PaymentMethod.CARD -> {
                _state.value = currentState.copy(
                    shiftCardTotal = currentState.shiftCardTotal + amount,
                    shiftSalesTotal = currentState.shiftSalesTotal + amount,
                    shiftTicketCount = currentState.shiftTicketCount + 1
                )
            }
            PaymentMethod.MOBILE_MONEY -> {
                _state.value = currentState.copy(
                    shiftMobileTotal = currentState.shiftMobileTotal + amount,
                    shiftSalesTotal = currentState.shiftSalesTotal + amount,
                    shiftTicketCount = currentState.shiftTicketCount + 1
                )
            }
            else -> {
                _state.value = currentState.copy(
                    shiftSalesTotal = currentState.shiftSalesTotal + amount,
                    shiftTicketCount = currentState.shiftTicketCount + 1
                )
            }
        }
    }
    
    /**
     * Process a refund
     */
    fun processRefund(
        ticketId: String,
        amount: Double,
        reason: String
    ) {
        viewModelScope.launch {
            val cashierId = userId ?: return@launch
            
            _state.value = _state.value.copy(isProcessing = true, error = null)
            
            transactionRepository.createTransaction(
                userId = cashierId,
                type = TransactionType.REFUND,
                amount = -amount,
                description = "Refund: $reason",
                paymentMethod = PaymentMethod.CASH,
                ticketId = ticketId
            ).fold(
                onSuccess = {
                    _state.value = _state.value.copy(
                        isProcessing = false,
                        showRefundDialog = false,
                        success = "Refund processed successfully"
                    )
                    loadDailyTransactions()
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isProcessing = false,
                        error = error.message ?: "Failed to process refund"
                    )
                }
            )
        }
    }
    
    /**
     * Get ticket price based on type and event
     */
    fun getTicketPrice(ticketType: com.example.eventpay.data.model.TicketType, event: Event): Double {
        return when (ticketType) {
            com.example.eventpay.data.model.TicketType.STANDARD -> event.ticketPrice
            com.example.eventpay.data.model.TicketType.VIP -> event.vipPrice ?: (event.ticketPrice * 2.0)
            com.example.eventpay.data.model.TicketType.PREMIUM -> event.ticketPrice * 2.5
            com.example.eventpay.data.model.TicketType.EARLY_BIRD -> event.ticketPrice * 0.8
            com.example.eventpay.data.model.TicketType.STUDENT -> event.ticketPrice * 0.7
            com.example.eventpay.data.model.TicketType.GROUP -> event.ticketPrice * 0.75
            com.example.eventpay.data.model.TicketType.PASS -> event.ticketPrice * 3.0
        }
    }
    
    /**
     * Generate daily report
     */
    fun generateDailyReport(): String {
        val state = _state.value
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        
        return buildString {
            appendLine("═══════════════════════════════════════")
            appendLine("         DAILY CASHIER REPORT")
            appendLine("═══════════════════════════════════════")
            appendLine()
            appendLine("Date: ${dateFormat.format(Date())}")
            appendLine("Cashier ID: ${userId ?: "N/A"}")
            appendLine()
            appendLine("───────────────────────────────────────")
            appendLine("           SALES SUMMARY")
            appendLine("───────────────────────────────────────")
            appendLine("Total Sales:        ${String.format("%.2f", state.dailySalesTotal)} MAD")
            appendLine("Tickets Sold:       ${state.dailyTicketCount}")
            appendLine()
            appendLine("───────────────────────────────────────")
            appendLine("        PAYMENT BREAKDOWN")
            appendLine("───────────────────────────────────────")
            appendLine("Cash:               ${String.format("%.2f", state.cashTotal)} MAD")
            appendLine("Card:               ${String.format("%.2f", state.cardTotal)} MAD")
            appendLine("Mobile:             ${String.format("%.2f", state.mobileTotal)} MAD")
            appendLine()
            
            if (state.isShiftActive && state.currentShift != null) {
                appendLine("───────────────────────────────────────")
                appendLine("           SHIFT DETAILS")
                appendLine("───────────────────────────────────────")
                appendLine("Shift Started:      ${dateFormat.format(Date(state.currentShift.shiftStart))}")
                appendLine("Starting Cash:      ${String.format("%.2f", state.currentShift.startingCash)} MAD")
                appendLine("Shift Sales:        ${String.format("%.2f", state.shiftSalesTotal)} MAD")
                appendLine("Expected Cash:      ${String.format("%.2f", state.currentShift.startingCash + state.shiftCashTotal)} MAD")
                appendLine()
            }
            
            appendLine("───────────────────────────────────────")
            appendLine("        RECENT TRANSACTIONS")
            appendLine("───────────────────────────────────────")
            state.transactions.take(10).forEach { txn ->
                appendLine("• ${txn.description}: ${String.format("%.2f", txn.amount)} MAD (${txn.paymentMethod.displayName()})")
            }
            appendLine()
            appendLine("═══════════════════════════════════════")
            appendLine("          END OF REPORT")
            appendLine("═══════════════════════════════════════")
        }
    }
    
    /**
     * Generate shift closing report
     */
    fun generateShiftClosingReport(): String {
        val state = _state.value
        val shift = state.currentShift ?: return "No active shift"
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        
        val expectedCash = shift.startingCash + state.shiftCashTotal
        
        return buildString {
            appendLine("═══════════════════════════════════════")
            appendLine("         SHIFT CLOSING REPORT")
            appendLine("═══════════════════════════════════════")
            appendLine()
            appendLine("Shift ID: ${shift.id}")
            appendLine("Cashier: ${shift.cashierName}")
            appendLine()
            appendLine("───────────────────────────────────────")
            appendLine("           SHIFT TIMING")
            appendLine("───────────────────────────────────────")
            appendLine("Started:  ${dateFormat.format(Date(shift.shiftStart))}")
            appendLine("Ended:    ${dateFormat.format(Date())}")
            val durationMinutes = (System.currentTimeMillis() - shift.shiftStart) / (1000 * 60)
            appendLine("Duration: ${durationMinutes / 60}h ${durationMinutes % 60}m")
            appendLine()
            appendLine("───────────────────────────────────────")
            appendLine("           CASH RECONCILIATION")
            appendLine("───────────────────────────────────────")
            appendLine("Starting Cash:      ${String.format("%.2f", shift.startingCash)} MAD")
            appendLine("Cash Sales:         ${String.format("%.2f", state.shiftCashTotal)} MAD")
            appendLine("Expected Cash:      ${String.format("%.2f", expectedCash)} MAD")
            appendLine()
            appendLine("───────────────────────────────────────")
            appendLine("           SALES SUMMARY")
            appendLine("───────────────────────────────────────")
            appendLine("Total Sales:        ${String.format("%.2f", state.shiftSalesTotal)} MAD")
            appendLine("Tickets Sold:       ${state.shiftTicketCount}")
            appendLine()
            appendLine("───────────────────────────────────────")
            appendLine("        PAYMENT BREAKDOWN")
            appendLine("───────────────────────────────────────")
            appendLine("Cash:               ${String.format("%.2f", state.shiftCashTotal)} MAD")
            appendLine("Card:               ${String.format("%.2f", state.shiftCardTotal)} MAD")
            appendLine("Mobile:             ${String.format("%.2f", state.shiftMobileTotal)} MAD")
            appendLine()
            appendLine("═══════════════════════════════════════")
        }
    }
    
    // Dialog visibility methods
    fun showStartShiftDialog() {
        _state.value = _state.value.copy(showStartShiftDialog = true)
    }
    
    fun hideStartShiftDialog() {
        _state.value = _state.value.copy(showStartShiftDialog = false)
    }
    
    fun showEndShiftDialog() {
        _state.value = _state.value.copy(showEndShiftDialog = true)
    }
    
    fun hideEndShiftDialog() {
        _state.value = _state.value.copy(showEndShiftDialog = false)
    }
    
    fun showSellDialog() {
        if (!_state.value.isShiftActive) {
            _state.value = _state.value.copy(
                error = "Please start a shift before selling tickets"
            )
            return
        }
        _state.value = _state.value.copy(showSellDialog = true)
    }
    
    fun hideSellDialog() {
        _state.value = _state.value.copy(showSellDialog = false)
    }
    
    fun showRefundDialog() {
        _state.value = _state.value.copy(showRefundDialog = true)
    }
    
    fun hideRefundDialog() {
        _state.value = _state.value.copy(showRefundDialog = false)
    }
    
    fun showReportDialog() {
        _state.value = _state.value.copy(showReportDialog = true)
    }
    
    fun hideReportDialog() {
        _state.value = _state.value.copy(showReportDialog = false)
    }
    
    fun hideQRDialog() {
        _state.value = _state.value.copy(
            showQRDialog = false,
            generatedQRCode = null,
            lastSoldTicket = null
        )
    }
    
    fun clearMessages() {
        _state.value = _state.value.copy(error = null, success = null)
    }
}
