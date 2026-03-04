package com.example.eventpay.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventpay.data.firebase.FirebaseService
import com.example.eventpay.data.firebase.FirestoreEventRepository
import com.example.eventpay.data.firebase.FirestoreTicketRepository
import com.example.eventpay.data.firebase.FirestoreTransactionRepository
import com.example.eventpay.data.model.PaymentMethod
import com.example.eventpay.data.model.Transaction
import com.example.eventpay.data.model.TransactionType
import com.example.eventpay.domain.model.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class DashboardStats(
    val totalEvents: Int = 0,
    val activeEvents: Int = 0,
    val totalTicketsSold: Int = 0,
    val totalRevenue: Double = 0.0,
    val todayRevenue: Double = 0.0,
    val todayTickets: Int = 0,
    val checkedInCount: Int = 0,
    val pendingCheckIns: Int = 0
)

data class RevenueBreakdown(
    val cashRevenue: Double = 0.0,
    val cardRevenue: Double = 0.0,
    val mobileRevenue: Double = 0.0,
    val walletRevenue: Double = 0.0
)

data class DashboardState(
    val isLoading: Boolean = false,
    val stats: DashboardStats = DashboardStats(),
    val revenueBreakdown: RevenueBreakdown = RevenueBreakdown(),
    val recentTransactions: List<Transaction> = emptyList(),
    val topEvents: List<Event> = emptyList(),
    val error: String? = null
)

class DashboardViewModel(
    private val eventRepository: FirestoreEventRepository,
    private val ticketRepository: FirestoreTicketRepository,
    private val transactionRepository: FirestoreTransactionRepository,
    private val firebaseService: FirebaseService
) : ViewModel() {
    
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()
    
    init {
        loadDashboardData()
    }
    
    fun loadDashboardData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            // Load all data in parallel
            loadEventsStats()
            loadRevenueStats()
            loadRecentTransactions()
        }
    }
    
    private suspend fun loadEventsStats() {
        eventRepository.getAllEvents().fold(
            onSuccess = { events ->
                val currentTime = System.currentTimeMillis()
                val activeEvents = events.filter { it.date > currentTime }
                
                // Calculate total tickets sold across all events
                var totalTickets = 0
                var totalCheckedIn = 0
                
                events.forEach { event ->
                    ticketRepository.getTicketsSoldCount(event.id).fold(
                        onSuccess = { count ->
                            totalTickets += count
                        },
                        onFailure = { /* ignore */ }
                    )
                    
                    ticketRepository.getCheckedInCount(event.id).fold(
                        onSuccess = { count ->
                            totalCheckedIn += count
                        },
                        onFailure = { /* ignore */ }
                    )
                }
                
                // Sort events by sold tickets to get top events
                val sortedEvents = events.sortedByDescending { it.soldTickets }.take(5)
                
                _state.value = _state.value.copy(
                    stats = _state.value.stats.copy(
                        totalEvents = events.size,
                        activeEvents = activeEvents.size,
                        totalTicketsSold = totalTickets,
                        checkedInCount = totalCheckedIn,
                        pendingCheckIns = totalTickets - totalCheckedIn
                    ),
                    topEvents = sortedEvents
                )
            },
            onFailure = { error ->
                _state.value = _state.value.copy(
                    error = error.message ?: "Failed to load events"
                )
            }
        )
    }
    
    private suspend fun loadRevenueStats() {
        // Get today's date range
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
        
        // Load total revenue
        transactionRepository.getAllTransactions().fold(
            onSuccess = { transactions ->
                val purchaseTransactions = transactions.filter { 
                    it.type == TransactionType.TICKET_PURCHASE 
                }
                
                val totalRevenue = purchaseTransactions.sumOf { it.amount }
                
                // Today's revenue
                val todayTransactions = purchaseTransactions.filter {
                    it.timestamp in startOfDay..endOfDay
                }
                val todayRevenue = todayTransactions.sumOf { it.amount }
                
                // Revenue by payment method
                var cashRevenue = 0.0
                var cardRevenue = 0.0
                var mobileRevenue = 0.0
                var walletRevenue = 0.0
                
                purchaseTransactions.forEach { txn ->
                    when (txn.paymentMethod) {
                        PaymentMethod.CASH -> cashRevenue += txn.amount
                        PaymentMethod.CARD -> cardRevenue += txn.amount
                        PaymentMethod.MOBILE_MONEY -> mobileRevenue += txn.amount
                        PaymentMethod.WALLET -> walletRevenue += txn.amount
                        PaymentMethod.BANK_TRANSFER -> { } // Handle bank transfer if needed
                        PaymentMethod.CASHIER_SALE -> cashRevenue += txn.amount // Count as cash for cashier sales
                    }
                }
                
                _state.value = _state.value.copy(
                    stats = _state.value.stats.copy(
                        totalRevenue = totalRevenue,
                        todayRevenue = todayRevenue,
                        todayTickets = todayTransactions.size
                    ),
                    revenueBreakdown = RevenueBreakdown(
                        cashRevenue = cashRevenue,
                        cardRevenue = cardRevenue,
                        mobileRevenue = mobileRevenue,
                        walletRevenue = walletRevenue
                    ),
                    isLoading = false
                )
            },
            onFailure = { error ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = error.message ?: "Failed to load revenue"
                )
            }
        )
    }
    
    private suspend fun loadRecentTransactions() {
        transactionRepository.getAllTransactions().fold(
            onSuccess = { transactions ->
                val recentTransactions = transactions
                    .sortedByDescending { it.timestamp }
                    .take(10)
                
                _state.value = _state.value.copy(
                    recentTransactions = recentTransactions
                )
            },
            onFailure = { /* ignore */ }
        )
    }
    
    fun getEventRevenue(eventId: String, onResult: (Double) -> Unit) {
        viewModelScope.launch {
            transactionRepository.getEventRevenue(eventId).fold(
                onSuccess = { revenue ->
                    onResult(revenue)
                },
                onFailure = {
                    onResult(0.0)
                }
            )
        }
    }
    
    fun getEventAttendance(eventId: String, onResult: (Int, Int) -> Unit) {
        viewModelScope.launch {
            var sold = 0
            var checkedIn = 0
            
            ticketRepository.getTicketsSoldCount(eventId).fold(
                onSuccess = { sold = it },
                onFailure = { }
            )
            
            ticketRepository.getCheckedInCount(eventId).fold(
                onSuccess = { checkedIn = it },
                onFailure = { }
            )
            
            onResult(sold, checkedIn)
        }
    }
    
    fun refreshData() {
        loadDashboardData()
    }
    
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
