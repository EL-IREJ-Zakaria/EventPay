package com.example.eventpay.ui.qrcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventpay.data.firebase.FirestoreEventRepository
import com.example.eventpay.data.firebase.FirestoreTicketRepository
import com.example.eventpay.data.model.Ticket
import com.example.eventpay.data.repository.EventRepository
import com.example.eventpay.data.repository.TicketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class QRCodeState(
    val isLoading: Boolean = false,
    val scanResult: ScanResult? = null,
    val error: String? = null,
    val success: String? = null
)

sealed class ScanResult {
    data class Success(val ticket: Ticket, val eventName: String) : ScanResult()
    data class AlreadyCheckedIn(val ticket: Ticket, val checkedInAt: Long?) : ScanResult()
    data class Invalid(val message: String) : ScanResult()
}

class QRCodeViewModel(
    private val ticketRepository: TicketRepository,
    private val eventRepository: EventRepository,
    private val firestoreTicketRepository: FirestoreTicketRepository
) : ViewModel() {

    private val _qrCodeState = MutableStateFlow(QRCodeState())
    val qrCodeState: StateFlow<QRCodeState> = _qrCodeState.asStateFlow()

    fun processQRCode(qrCode: String) {
        viewModelScope.launch {
            _qrCodeState.value = _qrCodeState.value.copy(isLoading = true, error = null)
            
            // Get ticket by QR code from Firestore
            firestoreTicketRepository.getTicketByQRCode(qrCode).fold(
                onSuccess = { ticket ->
                    if (ticket.isCheckedIn) {
                        _qrCodeState.value = _qrCodeState.value.copy(
                            isLoading = false,
                            scanResult = ScanResult.AlreadyCheckedIn(ticket, ticket.checkedInAt)
                        )
                        return@launch
                    }
                    
                    // Perform check-in
                    firestoreTicketRepository.checkInTicket(qrCode).fold(
                        onSuccess = { checkedInTicket ->
                            // Get event name
                            firestoreTicketRepository.getTicketByQRCode(qrCode).fold(
                                onSuccess = { updatedTicket ->
                                    // Get event details
                                    com.example.eventpay.data.firebase.FirestoreEventRepository()
                                        .getEvent(updatedTicket.eventId).fold(
                                            onSuccess = { event ->
                                                _qrCodeState.value = _qrCodeState.value.copy(
                                                    isLoading = false,
                                                    scanResult = ScanResult.Success(updatedTicket, event.name),
                                                    success = "Check-in successful!"
                                                )
                                            },
                                            onFailure = {
                                                _qrCodeState.value = _qrCodeState.value.copy(
                                                    isLoading = false,
                                                    scanResult = ScanResult.Success(updatedTicket, "Unknown Event"),
                                                    success = "Check-in successful!"
                                                )
                                            }
                                        )
                                },
                                onFailure = { error ->
                                    _qrCodeState.value = _qrCodeState.value.copy(
                                        isLoading = false,
                                        error = error.message ?: "Error getting ticket details"
                                    )
                                }
                            )
                        },
                        onFailure = { error ->
                            _qrCodeState.value = _qrCodeState.value.copy(
                                isLoading = false,
                                error = error.message ?: "Check-in failed"
                            )
                        }
                    )
                },
                onFailure = { error ->
                    _qrCodeState.value = _qrCodeState.value.copy(
                        isLoading = false,
                        scanResult = ScanResult.Invalid("Invalid QR code - ${error.message}")
                    )
                }
            )
        }
    }
    
    fun validateTicket(qrCode: String, onResult: (Ticket?) -> Unit) {
        viewModelScope.launch {
            _qrCodeState.value = _qrCodeState.value.copy(isLoading = true)
            
            firestoreTicketRepository.getTicketByQRCode(qrCode).fold(
                onSuccess = { ticket ->
                    _qrCodeState.value = _qrCodeState.value.copy(isLoading = false)
                    onResult(ticket)
                },
                onFailure = {
                    _qrCodeState.value = _qrCodeState.value.copy(isLoading = false)
                    onResult(null)
                }
            )
        }
    }

    fun clearResult() {
        _qrCodeState.value = QRCodeState()
    }

    fun clearError() {
        _qrCodeState.value = _qrCodeState.value.copy(error = null)
    }
    
    fun clearSuccess() {
        _qrCodeState.value = _qrCodeState.value.copy(success = null)
    }
}
