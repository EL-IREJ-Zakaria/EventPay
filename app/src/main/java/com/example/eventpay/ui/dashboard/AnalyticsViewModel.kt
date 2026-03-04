package com.example.eventpay.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventpay.data.firebase.FirebaseService
import com.example.eventpay.data.firebase.FirestoreEventRepository
import com.example.eventpay.data.firebase.FirestoreTicketRepository
import com.example.eventpay.data.firebase.FirestoreTransactionRepository
import com.example.eventpay.data.model.PaymentMethod as DataPaymentMethod
import com.example.eventpay.data.model.TicketType as DataTicketType
import com.example.eventpay.data.model.TransactionType
import com.example.eventpay.domain.model.DateRange
import com.example.eventpay.domain.model.Event
import com.example.eventpay.domain.model.EventAnalytics
import com.example.eventpay.domain.model.HourlyCheckIn
import com.example.eventpay.domain.model.HourlySales
import com.example.eventpay.domain.model.PeakEntryTime
import com.example.eventpay.domain.model.PaymentMethod
import com.example.eventpay.domain.model.PaymentStats
import com.example.eventpay.domain.model.SalesTrend
import com.example.eventpay.domain.model.TicketType
import com.example.eventpay.domain.model.TicketTypeStats
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Dashboard UI State
 */
data class DashboardUiState(
    // Loading states
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    
    // Selected event
    val selectedEventId: String? = null,
    val selectedEvent: Event? = null,
    val events: List<Event> = emptyList(),
    
    // Analytics data
    val analytics: EventAnalytics? = null,
    
    // Filter options
    val dateRange: DateRange = DateRange.TODAY,
    val refreshIntervalSeconds: Int = 60,
    
    // Real-time updates
    val lastUpdated: Long? = null,
    val autoRefreshEnabled: Boolean = true,
    
    // Error handling
    val error: String? = null
)

/**
 * Analytics Dashboard ViewModel
 * 
 * Manages real-time analytics data for event organizers.
 */
class AnalyticsViewModel(
    private val eventRepository: FirestoreEventRepository,
    private val ticketRepository: FirestoreTicketRepository,
    private val transactionRepository: FirestoreTransactionRepository,
    private val firebaseService: FirebaseService
) : ViewModel() {
    
    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()
    
    private var refreshJob: Job? = null
    
    init {
        loadEvents()
    }
    
    /**
     * Load available events for the organizer
     */
    private fun loadEvents() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            val userId = firebaseService.getCurrentUserId()
            if (userId == null) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "User not authenticated"
                )
                return@launch
            }
            
            eventRepository.getEventsByOrganizer(userId).fold(
                onSuccess = { events ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        events = events
                    )
                    
                    // Auto-select first event if available
                    if (events.isNotEmpty() && _state.value.selectedEventId == null) {
                        selectEvent(events.first().id)
                    }
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
     * Select an event to view analytics
     */
    fun selectEvent(eventId: String) {
        viewModelScope.launch {
            val event = _state.value.events.find { it.id == eventId }
            
            _state.value = _state.value.copy(
                selectedEventId = eventId,
                selectedEvent = event
            )
            
            loadAnalytics()
            startAutoRefresh()
        }
    }
    
    /**
     * Load analytics data for selected event
     */
    fun loadAnalytics() {
        viewModelScope.launch {
            val eventId = _state.value.selectedEventId ?: return@launch
            val event = _state.value.selectedEvent ?: return@launch
            
            _state.value = _state.value.copy(isRefreshing = true, error = null)
            
            try {
                // Fetch all required data using Flow-based methods
                 val tickets = mutableListOf<com.example.eventpay.data.model.Ticket>()
                 val transactions = mutableListOf<com.example.eventpay.data.model.Transaction>()
                 
                 ticketRepository.getTicketsByEventFlow(eventId).collect { ticketList ->
                     tickets.clear()
                     tickets.addAll(ticketList)
                 }
                 
                 transactionRepository.getTransactionsByEventFlow(eventId).collect { txnList ->
                     transactions.clear()
                     transactions.addAll(txnList)
                 }

                // Calculate analytics
                val analytics = calculateAnalytics(event, tickets.toList(), transactions.toList())
                
                _state.value = _state.value.copy(
                    isRefreshing = false,
                    analytics = analytics,
                    lastUpdated = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isRefreshing = false,
                    error = e.message ?: "Failed to load analytics"
                )
            }
        }
    }
    
    /**
     * Calculate comprehensive analytics from raw data
     */
    private fun calculateAnalytics(
        event: Event,
        tickets: List<com.example.eventpay.data.model.Ticket>,
        transactions: List<com.example.eventpay.data.model.Transaction>
    ): EventAnalytics {
        // Attendance metrics
        val totalTicketsSold = tickets.size
        val checkedInCount = tickets.count { it.isCheckedIn }
        val totalAttendees = totalTicketsSold
        val pendingCheckIn = totalAttendees - checkedInCount
        val noShowCount = 0 // Would need event end time to calculate
        val checkInRate = if (totalAttendees > 0) (checkedInCount.toDouble() / totalAttendees) * 100 else 0.0
        
        // Revenue metrics
        val totalRevenue = transactions
            .filter { it.type == TransactionType.TICKET_PURCHASE }
            .sumOf { it.amount }
        
        val vipTickets = tickets.filter { it.ticketType == com.example.eventpay.data.model.TicketType.VIP }
        val standardTickets = tickets.filter { it.ticketType == com.example.eventpay.data.model.TicketType.STANDARD }
        
        val vipRevenue = vipTickets.sumOf { it.price }
        val standardRevenue = standardTickets.sumOf { it.price }
        val averageTicketPrice = if (totalTicketsSold > 0) totalRevenue / totalTicketsSold else 0.0
        val revenuePerAttendee = if (checkedInCount > 0) totalRevenue / checkedInCount else 0.0
        
        // Real-time metrics
        val now = System.currentTimeMillis()
        val oneHourAgo = now - (60 * 60 * 1000)
        val fifteenMinutesAgo = now - (15 * 60 * 1000)
        
        val recentCheckIns = tickets.filter { 
            it.isCheckedIn && (it.checkedInAt ?: 0) > oneHourAgo 
        }
        val last15MinCheckIns = tickets.filter { 
            it.isCheckedIn && (it.checkedInAt ?: 0) > fifteenMinutesAgo 
        }
        
        val lastCheckInTime = tickets.filter { it.isCheckedIn }
            .maxOfOrNull { it.checkedInAt ?: 0 }
        
        // Calculate entry velocity (check-ins per minute)
        val eventStartTime = event.date
        val eventDurationMinutes = if (eventStartTime > 0) {
            ((now - eventStartTime) / (60 * 1000)).coerceAtLeast(1)
        } else 1
        val entryVelocity = checkedInCount.toDouble() / eventDurationMinutes
        
        // Peak entry time calculation
        val peakEntryTime = calculatePeakEntryTime(tickets)
        
        // Hourly check-ins
        val hourlyCheckIns = calculateHourlyCheckIns(tickets)
        
        // Ticket type breakdown
        val ticketTypeBreakdown = calculateTicketTypeStats(tickets)
        
        // Payment method breakdown
        val paymentMethodBreakdown = calculatePaymentStats(transactions)
        
        // Hourly sales
        val salesByHour = calculateHourlySales(transactions)
        
        // Sales trend
        val salesTrend = calculateSalesTrend(salesByHour)
        
        return EventAnalytics(
            eventId = event.id,
            eventName = event.name,
            eventDate = event.date,
            venue = event.location,
            totalCapacity = event.totalTickets,
            totalTicketsSold = totalTicketsSold,
            totalAttendees = totalAttendees,
            checkedInCount = checkedInCount,
            pendingCheckIn = pendingCheckIn,
            noShowCount = noShowCount,
            checkInRate = checkInRate,
            totalRevenue = totalRevenue,
            ticketRevenue = totalRevenue,
            vipRevenue = vipRevenue,
            standardRevenue = standardRevenue,
            averageTicketPrice = averageTicketPrice,
            revenuePerAttendee = revenuePerAttendee,
            liveCheckIns = recentCheckIns.size,
            lastCheckInTime = lastCheckInTime,
            checkInsLastHour = recentCheckIns.size,
            checkInsLast15Minutes = last15MinCheckIns.size,
            currentQueueSize = 0, // Would need queue management system
            peakEntryTime = peakEntryTime,
            hourlyCheckIns = hourlyCheckIns,
            entryVelocity = entryVelocity,
            ticketTypeBreakdown = ticketTypeBreakdown,
            paymentMethodBreakdown = paymentMethodBreakdown,
            salesByHour = salesByHour,
            salesTrend = salesTrend,
            conversionRate = 0.0, // Would need page view data
            demographics = null
        )
    }
    
    /**
     * Calculate peak entry time
     */
    private fun calculatePeakEntryTime(
        tickets: List<com.example.eventpay.data.model.Ticket>
    ): PeakEntryTime? {
        val checkedInTickets = tickets.filter { it.isCheckedIn && it.checkedInAt != null }
        if (checkedInTickets.isEmpty()) return null
        
        // Group by hour
        val hourlyCounts = checkedInTickets.groupBy { ticket ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = ticket.checkedInAt!!
            calendar.get(Calendar.HOUR_OF_DAY)
        }.mapValues { it.value.size }
        
        val peakHour = hourlyCounts.maxByOrNull { it.value } ?: return null
        
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, peakHour.key)
        calendar.set(Calendar.MINUTE, 0)
        
        val percentage = if (checkedInTickets.isNotEmpty()) {
            (peakHour.value.toDouble() / checkedInTickets.size) * 100
        } else 0.0
        
        return PeakEntryTime(
            hour = peakHour.key,
            minute = 0,
            timestamp = calendar.timeInMillis,
            checkInCount = peakHour.value,
            percentage = percentage
        )
    }
    
    /**
     * Calculate hourly check-in distribution
     */
    private fun calculateHourlyCheckIns(
        tickets: List<com.example.eventpay.data.model.Ticket>
    ): List<HourlyCheckIn> {
        val checkedInTickets = tickets.filter { it.isCheckedIn && it.checkedInAt != null }
        if (checkedInTickets.isEmpty()) return emptyList()
        
        val calendar = Calendar.getInstance()
        val hourlyCounts = mutableMapOf<Int, Int>()
        
        checkedInTickets.forEach { ticket ->
            calendar.timeInMillis = ticket.checkedInAt!!
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            hourlyCounts[hour] = (hourlyCounts[hour] ?: 0) + 1
        }
        
        var cumulative = 0
        return (0..23).map { hour ->
            val count = hourlyCounts[hour] ?: 0
            cumulative += count
            val percentage = if (checkedInTickets.isNotEmpty()) {
                (count.toDouble() / checkedInTickets.size) * 100
            } else 0.0
            
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            
            HourlyCheckIn(
                hour = hour,
                timestamp = calendar.timeInMillis,
                checkInCount = count,
                cumulativeCount = cumulative,
                percentage = percentage
            )
        }
    }
    
    /**
     * Convert data model TicketType to domain model TicketType
     */
    private fun DataTicketType.toDomain(): TicketType = when (this) {
        DataTicketType.STANDARD -> TicketType.STANDARD
        DataTicketType.VIP -> TicketType.VIP
        DataTicketType.PREMIUM -> TicketType.PREMIUM
        DataTicketType.EARLY_BIRD -> TicketType.EARLY_BIRD
        DataTicketType.STUDENT -> TicketType.STUDENT
        DataTicketType.GROUP -> TicketType.GROUP
        DataTicketType.PASS -> TicketType.PASS
    }
    
    /**
     * Convert data model PaymentMethod to domain model PaymentMethod
     */
    private fun DataPaymentMethod.toDomain(): PaymentMethod = when (this) {
        DataPaymentMethod.WALLET -> PaymentMethod.WALLET
        DataPaymentMethod.CASH -> PaymentMethod.CASH
        DataPaymentMethod.CARD -> PaymentMethod.CARD
        DataPaymentMethod.MOBILE_MONEY -> PaymentMethod.MOBILE_MONEY
        DataPaymentMethod.BANK_TRANSFER -> PaymentMethod.BANK_TRANSFER
        DataPaymentMethod.CASHIER_SALE -> PaymentMethod.CASHIER_SALE
    }
    
    /**
     * Calculate ticket type statistics
     */
    private fun calculateTicketTypeStats(
        tickets: List<com.example.eventpay.data.model.Ticket>
    ): Map<TicketType, TicketTypeStats> {
        return tickets.groupBy { it.ticketType }.mapKeys { (type, _) -> type.toDomain() }.mapValues { (_, typeTickets) ->
            val sold = typeTickets.size
            val checkedIn = typeTickets.count { it.isCheckedIn }
            val revenue = typeTickets.sumOf { it.price }
            val averagePrice = if (sold > 0) revenue / sold else 0.0
            val checkInRate = if (sold > 0) (checkedIn.toDouble() / sold) * 100 else 0.0
            
            TicketTypeStats(
                type = typeTickets.first().ticketType.toDomain(),
                sold = sold,
                checkedIn = checkedIn,
                revenue = revenue,
                averagePrice = averagePrice,
                checkInRate = checkInRate
            )
        }
    }
    
    /**
     * Calculate payment method statistics
     */
    private fun calculatePaymentStats(
        transactions: List<com.example.eventpay.data.model.Transaction>
    ): Map<PaymentMethod, PaymentStats> {
        val salesTransactions = transactions.filter { 
            it.type == TransactionType.TICKET_PURCHASE 
        }
        
        val totalAmount = salesTransactions.sumOf { it.amount }
        
        return salesTransactions.groupBy { it.paymentMethod }.mapKeys { (method, _) -> method.toDomain() }.mapValues { (_, methodTransactions) ->
            val count = methodTransactions.size
            val amount = methodTransactions.sumOf { it.amount }
            val percentage = if (totalAmount > 0) (amount / totalAmount) * 100 else 0.0
            val average = if (count > 0) amount / count else 0.0
            
            PaymentStats(
                method = methodTransactions.first().paymentMethod.toDomain(),
                transactionCount = count,
                totalAmount = amount,
                percentage = percentage,
                averageTransaction = average
            )
        }
    }
    
    /**
     * Calculate hourly sales distribution
     */
    private fun calculateHourlySales(
        transactions: List<com.example.eventpay.data.model.Transaction>
    ): List<HourlySales> {
        val salesTransactions = transactions.filter { 
            it.type == TransactionType.TICKET_PURCHASE || 
            it.type == TransactionType.CASHIER_SALE 
        }
        
        val calendar = Calendar.getInstance()
        val hourlyData = mutableMapOf<Int, MutableList<com.example.eventpay.data.model.Transaction>>()
        
        salesTransactions.forEach { transaction ->
            calendar.timeInMillis = transaction.createdAt
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            hourlyData.getOrPut(hour) { mutableListOf() }.add(transaction)
        }
        
        return (0..23).map { hour ->
            val hourTransactions = hourlyData[hour] ?: emptyList()
            
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            
            HourlySales(
                hour = hour,
                timestamp = calendar.timeInMillis,
                ticketsSold = hourTransactions.size,
                revenue = hourTransactions.sumOf { it.amount },
                transactionCount = hourTransactions.size
            )
        }
    }
    
    /**
     * Determine sales trend
     */
    private fun calculateSalesTrend(hourlySales: List<HourlySales>): SalesTrend {
        val recentHours = hourlySales.takeLast(3)
        if (recentHours.size < 2) return SalesTrend.STABLE
        
        val avgRecent = recentHours.takeLast(2).map { it.revenue }.average()
        val avgPrevious = recentHours.first().revenue
        
        return when {
            avgRecent > avgPrevious * 1.2 -> SalesTrend.INCREASING
            avgRecent < avgPrevious * 0.8 -> SalesTrend.DECREASING
            avgRecent > avgPrevious * 1.5 -> SalesTrend.PEAK
            else -> SalesTrend.STABLE
        }
    }
    
    /**
     * Start auto-refresh job
     */
    private fun startAutoRefresh() {
        refreshJob?.cancel()
        
        if (!_state.value.autoRefreshEnabled) return
        
        refreshJob = viewModelScope.launch {
            while (isActive) {
                delay(_state.value.refreshIntervalSeconds * 1000L)
                loadAnalytics()
            }
        }
    }
    
    /**
     * Toggle auto-refresh
     */
    fun toggleAutoRefresh() {
        val newValue = !_state.value.autoRefreshEnabled
        _state.value = _state.value.copy(autoRefreshEnabled = newValue)
        
        if (newValue) {
            startAutoRefresh()
        } else {
            refreshJob?.cancel()
        }
    }
    
    /**
     * Set refresh interval
     */
    fun setRefreshInterval(seconds: Int) {
        _state.value = _state.value.copy(refreshIntervalSeconds = seconds)
        if (_state.value.autoRefreshEnabled) {
            startAutoRefresh()
        }
    }
    
    /**
     * Set date range filter
     */
    fun setDateRange(range: DateRange) {
        _state.value = _state.value.copy(dateRange = range)
        loadAnalytics()
    }
    
    /**
     * Manual refresh
     */
    fun refresh() {
        loadAnalytics()
    }
    
    /**
     * Clear error
     */
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
    
    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }
}
