package com.example.eventpay.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventpay.data.firebase.CheckInAttemptResult
import com.example.eventpay.data.firebase.FirebaseService
import com.example.eventpay.data.firebase.FirestoreEventRepository
import com.example.eventpay.data.firebase.FirestoreTicketRepository
import com.example.eventpay.domain.model.Event
import com.example.eventpay.domain.model.CheckInResult as DomainCheckInResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ScanStatus { IDLE, SCANNING, SUCCESS, ALREADY_SCANNED, INVALID, NOT_FOUND, ERROR }

data class ScannerUiState(
    val isLoading: Boolean = false,
    val events: List<Event> = emptyList(),
    val selectedEvent: Event? = null,
    val scanStatus: ScanStatus = ScanStatus.IDLE,
    val lastScannedTicketId: String? = null,
    val lastScannedName: String? = null,
    val sessionCheckInCount: Int = 0,
    val error: String? = null
)

class ScannerViewModel(
    private val firebaseService: FirebaseService,
    private val eventRepository: FirestoreEventRepository,
    private val ticketRepository: FirestoreTicketRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private val scannedInSession = mutableSetOf<String>()
    private var lastScanTime = 0L
    private val cooldownMs = 2000L

    init {
        loadActiveEvents()
    }

    /**
     * CRITICAL SECURITY FIX: Only load events assigned to this scanner
     * This ensures scanners can ONLY see events they're authorized for
     */
    fun loadActiveEvents() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // Get current scanner ID - if null, we can't load events
            val scannerId = firebaseService.getCurrentUserId()
            if (scannerId == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Not authenticated. Please log in again."
                )
                return@launch
            }
            
            // Use the security-aware method that only returns assigned events
            eventRepository.getEventsForScanner(scannerId).fold(
                onSuccess = { events ->
                    _uiState.value = _uiState.value.copy(
                        events = events, 
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load events"
                    )
                }
            )
        }
    }

    fun selectEvent(event: Event) {
        _uiState.value = _uiState.value.copy(
            selectedEvent = event,
            scanStatus = ScanStatus.IDLE,
            lastScannedTicketId = null,
            lastScannedName = null
        )
        scannedInSession.clear()
    }

    fun clearSelectedEvent() {
        _uiState.value = _uiState.value.copy(selectedEvent = null, scanStatus = ScanStatus.IDLE)
    }

    /**
     * Process QR code scan using atomic transaction for data consistency
     * This ensures ticket status, event count, and check-in record are updated atomically
     */
    fun processQRCode(
        rawQrData: String,
        scannerId: String,
        scannerName: String
    ) {
        val now = System.currentTimeMillis()
        if (now - lastScanTime < cooldownMs) return
        if (rawQrData in scannedInSession) {
            _uiState.value = _uiState.value.copy(scanStatus = ScanStatus.ALREADY_SCANNED)
            autoResetAfterDelay()
            return
        }

        val eventId = _uiState.value.selectedEvent?.id ?: return
        lastScanTime = now

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Use atomic check-in operation with transaction
            val checkInResult = ticketRepository.recordCheckInAtomic(
                qrCode = rawQrData,
                eventId = eventId,
                scannerId = scannerId,
                scannerName = scannerName,
                deviceId = android.os.Build.DEVICE
            )

            when (checkInResult) {
                is CheckInAttemptResult.Success -> {
                    scannedInSession.add(rawQrData)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        scanStatus = ScanStatus.SUCCESS,
                        lastScannedTicketId = checkInResult.ticket.id,
                        lastScannedName = checkInResult.ticket.userId, // Use userId instead of attendeeName
                        sessionCheckInCount = _uiState.value.sessionCheckInCount + 1,
                        error = null
                    )
                }
                is CheckInAttemptResult.AlreadyCheckedIn -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        scanStatus = ScanStatus.ALREADY_SCANNED,
                        lastScannedName = checkInResult.ticket.userId, // Use userId instead of attendeeName
                        error = "Ticket already checked in"
                    )
                }
                is CheckInAttemptResult.WrongEvent -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        scanStatus = ScanStatus.INVALID,
                        error = "This ticket is for a different event"
                    )
                }
                is CheckInAttemptResult.InvalidTicket -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        scanStatus = ScanStatus.INVALID,
                        error = checkInResult.reason
                    )
                }
                is CheckInAttemptResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        scanStatus = ScanStatus.ERROR,
                        error = checkInResult.exception.message ?: "Error processing ticket"
                    )
                }
            }
            autoResetAfterDelay()
        }
    }

    private fun autoResetAfterDelay() {
        viewModelScope.launch {
            delay(3500)
            _uiState.value = _uiState.value.copy(
                scanStatus = ScanStatus.IDLE,
                error = null
            )
        }
    }

    fun resetScan() {
        _uiState.value = _uiState.value.copy(
            scanStatus = ScanStatus.IDLE,
            error = null,
            lastScannedTicketId = null,
            lastScannedName = null
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
