package com.example.eventpay.ui.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventpay.data.firebase.FirestoreEventRepository
import com.example.eventpay.data.firebase.FirestoreTicketRepository
import com.example.eventpay.data.firebase.FirestoreTransactionRepository
import com.example.eventpay.data.model.PaymentMethod
import com.example.eventpay.data.model.TicketType
import com.example.eventpay.data.model.TransactionType
import com.example.eventpay.data.repository.EventRepository
import com.example.eventpay.data.repository.TicketRepository
import com.example.eventpay.data.repository.TransactionRepository
import com.example.eventpay.data.repository.UserRepository
import com.example.eventpay.domain.model.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class EventState(
    val isLoading: Boolean = false,
    val events: List<Event> = emptyList(),
    val currentEvent: Event? = null,
    val error: String? = null,
    val success: String? = null,
    val checkedInCount: Int = 0,
    val ticketsSoldCount: Int = 0
)

class EventViewModel(
    private val eventRepository: EventRepository,
    private val ticketRepository: TicketRepository,
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository,
    private val firestoreEventRepository: FirestoreEventRepository,
    private val firestoreTicketRepository: FirestoreTicketRepository,
    private val firestoreTransactionRepository: FirestoreTransactionRepository
) : ViewModel() {

    private val _eventState = MutableStateFlow(EventState())
    val eventState: StateFlow<EventState> = _eventState.asStateFlow()

    fun loadEvents() {
        viewModelScope.launch {
            _eventState.value = _eventState.value.copy(isLoading = true)
            firestoreEventRepository.getAllEventsFlow().collect { events ->
                _eventState.value = _eventState.value.copy(
                    isLoading = false,
                    events = events
                )
            }
        }
    }

    fun loadOrganizerEvents(organizerId: String) {
        viewModelScope.launch {
            _eventState.value = _eventState.value.copy(isLoading = true)
            firestoreEventRepository.getEventsByOrganizerFlow(organizerId).collect { events ->
                _eventState.value = _eventState.value.copy(
                    isLoading = false,
                    events = events
                )
            }
        }
    }

    fun loadUpcomingEvents() {
        viewModelScope.launch {
            _eventState.value = _eventState.value.copy(isLoading = true)
            firestoreEventRepository.getAllEvents().fold(
                onSuccess = { events ->
                    val currentTime = System.currentTimeMillis()
                    val upcomingEvents = events.filter { it.date > currentTime }
                    _eventState.value = _eventState.value.copy(
                        isLoading = false,
                        events = upcomingEvents
                    )
                },
                onFailure = { error ->
                    _eventState.value = _eventState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }

    fun getEventById(eventId: String) {
        viewModelScope.launch {
            _eventState.value = _eventState.value.copy(isLoading = true)
            
            firestoreEventRepository.getEvent(eventId).fold(
                onSuccess = { event ->
                    // Get checked in count
                    firestoreTicketRepository.getCheckedInCount(eventId).fold(
                        onSuccess = { checkedIn ->
                            // Get tickets sold count
                            firestoreTicketRepository.getTicketsSoldCount(eventId).fold(
                                onSuccess = { sold ->
                                    _eventState.value = _eventState.value.copy(
                                        isLoading = false,
                                        currentEvent = event,
                                        checkedInCount = checkedIn,
                                        ticketsSoldCount = sold
                                    )
                                },
                                onFailure = {
                                    _eventState.value = _eventState.value.copy(
                                        isLoading = false,
                                        currentEvent = event,
                                        checkedInCount = checkedIn
                                    )
                                }
                            )
                        },
                        onFailure = {
                            _eventState.value = _eventState.value.copy(
                                isLoading = false,
                                currentEvent = event
                            )
                        }
                    )
                },
                onFailure = { error ->
                    _eventState.value = _eventState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }

    fun createEvent(
        name: String,
        description: String,
        location: String,
        date: Long,
        ticketPrice: Double,
        totalTickets: Int,
        organizerId: String
    ) {
        viewModelScope.launch {
            _eventState.value = _eventState.value.copy(isLoading = true, error = null)
            
            val event = Event(
                id = UUID.randomUUID().toString(),
                name = name,
                description = description,
                location = location,
                date = date,
                endDate = date + (24 * 60 * 60 * 1000), // Default to 1 day after start
                ticketPrice = ticketPrice,
                totalTickets = totalTickets,
                organizerId = organizerId
            )
            
            firestoreEventRepository.createEvent(event).fold(
                onSuccess = {
                    _eventState.value = _eventState.value.copy(
                        isLoading = false,
                        success = "Event created successfully"
                    )
                },
                onFailure = { error ->
                    _eventState.value = _eventState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }

    fun purchaseTicket(
        userId: String, 
        eventId: String,
        ticketType: TicketType = TicketType.STANDARD,
        paymentMethod: PaymentMethod = PaymentMethod.WALLET
    ) {
        viewModelScope.launch {
            _eventState.value = _eventState.value.copy(isLoading = true, error = null)
            
            // Get event
            firestoreEventRepository.getEvent(eventId).fold(
                onSuccess = { event ->
                    // Check if tickets available
                    if (event.soldTickets >= event.totalTickets) {
                        _eventState.value = _eventState.value.copy(
                            isLoading = false,
                            error = "No tickets available"
                        )
                        return@launch
                    }
                    
                    // Calculate price based on ticket type
                    val price = when (ticketType) {
                        TicketType.STANDARD -> event.ticketPrice
                        TicketType.VIP -> event.vipPrice ?: (event.ticketPrice * 2.0)
                        TicketType.PREMIUM -> event.ticketPrice * 2.5
                        TicketType.EARLY_BIRD -> event.ticketPrice * 0.8
                        TicketType.STUDENT -> event.ticketPrice * 0.7
                        TicketType.GROUP -> event.ticketPrice * 0.75
                        TicketType.PASS -> event.ticketPrice * 3.0
                    }
                    
                    // For wallet payment, check balance
                    if (paymentMethod == PaymentMethod.WALLET) {
                        val user = userRepository.getUserById(userId)
                        if (user == null) {
                            _eventState.value = _eventState.value.copy(
                                isLoading = false,
                                error = "User not found"
                            )
                            return@launch
                        }
                        
                        if (user.walletBalance < price) {
                            _eventState.value = _eventState.value.copy(
                                isLoading = false,
                                error = "Insufficient wallet balance"
                            )
                            return@launch
                        }
                        
                        // Deduct from wallet
                        val newBalance = user.walletBalance - price
                        userRepository.updateWalletBalance(userId, newBalance)
                    }
                    
                    // Create ticket
                    firestoreTicketRepository.createTicket(eventId, userId, ticketType, price).fold(
                        onSuccess = { ticket ->
                            // Create transaction
                            firestoreTransactionRepository.createTransaction(
                                userId = userId,
                                type = TransactionType.TICKET_PURCHASE,
                                amount = price,
                                description = "$ticketType ticket for ${event.name}",
                                paymentMethod = paymentMethod,
                                eventId = eventId,
                                ticketId = ticket.id
                            )
                            
                            // Increment sold tickets
                            firestoreEventRepository.incrementSoldTickets(eventId)
                            
                            _eventState.value = _eventState.value.copy(
                                isLoading = false,
                                success = "Ticket purchased successfully! QR: ${ticket.qrCode}"
                            )
                        },
                        onFailure = { error ->
                            _eventState.value = _eventState.value.copy(
                                isLoading = false,
                                error = error.message
                            )
                        }
                    )
                },
                onFailure = { error ->
                    _eventState.value = _eventState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Event not found"
                    )
                }
            )
        }
    }

    fun updateEvent(event: Event) {
        viewModelScope.launch {
            _eventState.value = _eventState.value.copy(isLoading = true, error = null)
            
            firestoreEventRepository.updateEvent(event).fold(
                onSuccess = {
                    _eventState.value = _eventState.value.copy(
                        isLoading = false,
                        success = "Event updated successfully"
                    )
                },
                onFailure = { error ->
                    _eventState.value = _eventState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            firestoreEventRepository.deleteEvent(eventId)
        }
    }

    fun clearError() {
        _eventState.value = _eventState.value.copy(error = null)
    }
    
    fun clearSuccess() {
        _eventState.value = _eventState.value.copy(success = null)
    }
}
