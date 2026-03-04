package com.example.eventpay.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventpay.data.firebase.CheckInRecord
import com.example.eventpay.data.firebase.DashboardStats
import com.example.eventpay.data.firebase.FirebaseService
import com.example.eventpay.data.firebase.FirestoreEventRepository
import com.example.eventpay.data.model.User
import com.example.eventpay.data.model.UserRole
import com.example.eventpay.domain.model.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminUiState(
    val isLoading: Boolean = false,
    val stats: DashboardStats = DashboardStats(),
    val events: List<Event> = emptyList(),
    val scanners: List<User> = emptyList(),
    val checkIns: List<CheckInRecord> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
)

class AdminViewModel(
    private val firebaseService: FirebaseService,
    private val eventRepository: FirestoreEventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            firebaseService.getDashboardStats().fold(
                onSuccess = { stats ->
                    _uiState.value = _uiState.value.copy(stats = stats, isLoading = false)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }

    fun loadEvents() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            eventRepository.getAllEvents().fold(
                onSuccess = { events ->
                    _uiState.value = _uiState.value.copy(events = events, isLoading = false)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }

    fun loadScanners() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            firebaseService.getUsersByRole(UserRole.SCANNER).fold(
                onSuccess = { scanners ->
                    _uiState.value = _uiState.value.copy(scanners = scanners, isLoading = false)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }

    fun loadCheckInsForEvent(eventId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            firebaseService.getCheckInsForEvent(eventId).fold(
                onSuccess = { checkIns ->
                    _uiState.value = _uiState.value.copy(checkIns = checkIns, isLoading = false)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = error.message)
                }
            )
        }
    }

    fun createEvent(event: Event) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            eventRepository.createEvent(event).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Event created successfully"
                    )
                    loadEvents()
                    loadDashboard()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to create event"
                    )
                }
            )
        }
    }

    fun updateEvent(event: Event) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            eventRepository.updateEvent(event).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Event updated successfully"
                    )
                    loadEvents()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to update event"
                    )
                }
            )
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            eventRepository.deleteEvent(eventId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Event deleted"
                    )
                    loadEvents()
                    loadDashboard()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to delete event"
                    )
                }
            )
        }
    }

    /**
     * Create a new scanner account with event assignments
     * 
     * @param email Scanner's email address
     * @param password Generated password
     * @param fullName Scanner's full name
     * @param adminId ID of the admin creating this scanner
     * @param assignedEventIds List of event IDs to assign this scanner to
     */
    fun createScannerWithEvents(
        email: String,
        password: String,
        fullName: String,
        adminId: String,
        assignedEventIds: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // Step 1: Create the scanner account
            firebaseService.createScannerAccount(email, password, fullName, adminId).fold(
                onSuccess = { scannerId ->
                    // Step 2: Assign scanner to selected events
                    if (assignedEventIds.isNotEmpty()) {
                        assignedEventIds.forEach { eventId ->
                            eventRepository.assignScanner(eventId, scannerId)
                        }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Scanner $fullName created with ${assignedEventIds.size} event assignments"
                    )
                    loadScanners()
                    loadDashboard()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to create scanner account"
                    )
                }
            )
        }
    }

    /**
     * Legacy method - kept for backward compatibility
     */
    fun createScanner(
        email: String,
        password: String,
        fullName: String,
        adminId: String
    ) {
        createScannerWithEvents(email, password, fullName, adminId, emptyList())
    }

    /**
     * Assign a scanner to an event
     */
    fun assignScannerToEvent(scannerId: String, eventId: String) {
        viewModelScope.launch {
            eventRepository.assignScanner(eventId, scannerId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Scanner assigned to event"
                    )
                    loadEvents()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "Failed to assign scanner"
                    )
                }
            )
        }
    }

    /**
     * Publish an event - makes it visible to attendees and scanners
     */
    fun publishEvent(eventId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            eventRepository.publishEvent(eventId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Event published successfully"
                    )
                    loadEvents()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to publish event"
                    )
                }
            )
        }
    }

    /**
     * Cancel an event
     */
    fun cancelEvent(eventId: String, reason: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            eventRepository.cancelEvent(eventId, reason).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Event cancelled"
                    )
                    loadEvents()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to cancel event"
                    )
                }
            )
        }
    }

    /**
     * Remove a scanner from an event
     */
    fun removeScannerFromEvent(scannerId: String, eventId: String) {
        viewModelScope.launch {
            eventRepository.removeScanner(eventId, scannerId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Scanner removed from event"
                    )
                    loadEvents()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "Failed to remove scanner"
                    )
                }
            )
        }
    }

    fun toggleScannerActive(userId: String, isActive: Boolean) {
        viewModelScope.launch {
            firebaseService.toggleUserActive(userId, isActive).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        successMessage = if (isActive) "Account activated" else "Account deactivated"
                    )
                    loadScanners()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }
}
