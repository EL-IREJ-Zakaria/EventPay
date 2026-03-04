package com.example.eventpay.ui.scanner

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventpay.domain.model.TicketScanResult
import com.example.eventpay.domain.model.Event
import com.example.eventpay.domain.model.Ticket
import com.example.eventpay.domain.model.TicketStatus
import com.example.eventpay.domain.qrcode.InvalidReason
import com.example.eventpay.domain.qrcode.QRCodeValidation
import com.example.eventpay.domain.qrcode.QuickValidationResult
import com.example.eventpay.domain.usecase.qrcode.QRCodeUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * QR Scanner ViewModel
 * 
 * Handles QR code scanning, validation, and check-in operations.
 * Provides real-time feedback for scanning results.
 * 
 * Features:
 * - CameraX integration for QR scanning
 * - Ticket validation (valid/used/expired)
 * - Double-scan prevention
 * - Real-time UI feedback
 * - Location tracking for check-ins
 */
@HiltViewModel
class QRScannerViewModel @Inject constructor(
    private val qrCodeUseCases: QRCodeUseCases
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow(QRScannerUiState())
    val uiState: StateFlow<QRScannerUiState> = _uiState.asStateFlow()
    
    // Events for one-time actions
    private val _events = MutableSharedFlow<ScannerEvent>()
    val events: SharedFlow<ScannerEvent> = _events.asSharedFlow()
    
    // Recently scanned codes to prevent double scanning
    private val recentlyScannedCodes = mutableSetOf<String>()
    private val scanTimestamps = mutableMapOf<String, Long>()
    
    // Cooldown period to prevent rapid scanning
    private var lastScanTime = 0L
    private val scanCooldownMs = 2000L // 2 seconds between scans
    
    /**
     * Process a scanned QR code
     * Called when CameraX detects a QR code
     */
    fun onQRCodeScanned(qrCode: String, eventId: String) {
        val currentTime = System.currentTimeMillis()
        
        // Check cooldown
        if (currentTime - lastScanTime < scanCooldownMs) {
            return
        }
        
        // Check for duplicate scan
        if (isRecentlyScanned(qrCode)) {
            showDuplicateScanError(qrCode)
            return
        }
        
        lastScanTime = currentTime
        
        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = true,
                scanningState = ScanningState.PROCESSING
            ) }
            
            // Quick validate first
            when (val result = qrCodeUseCases.quickValidateQRCode(qrCode, eventId)) {
                is QuickValidationResult.Valid -> {
                    // Show preview for confirmation
                    _uiState.update { it.copy(
                        isLoading = false,
                        scanningState = ScanningState.PREVIEW,
                        previewData = PreviewData(
                            ticket = result.ticket,
                            event = result.event,
                            qrCode = qrCode
                        )
                    ) }
                }
                is QuickValidationResult.Invalid -> {
                    showError(ScanError.InvalidQRCode(result.reason))
                }
                is QuickValidationResult.Expired -> {
                    showError(ScanError.ExpiredTicket(result.expiredAt))
                }
                is QuickValidationResult.Duplicate -> {
                    showError(ScanError.AlreadyCheckedIn(result.previousScanTime))
                }
                is QuickValidationResult.NotFound -> {
                    val message = buildString {
                        if (!result.ticketFound) append("Ticket not found. ")
                        if (!result.eventFound) append("Event not found.")
                    }
                    showError(ScanError.NotFound(message.trim()))
                }
            }
        }
    }
    
    /**
     * Confirm check-in after preview
     */
    fun confirmCheckIn(
        eventId: String,
        deviceId: String,
        userId: String,
        location: Location? = null
    ) {
        val previewData = _uiState.value.previewData ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = true,
                scanningState = ScanningState.CHECKING_IN
            ) }
            
            val locationPair = location?.let { 
                Pair(it.latitude, it.longitude) 
            }
            
            val result = qrCodeUseCases.checkInViaSecureQRCode(
                qrCodeString = previewData.qrCode,
                eventId = eventId,
                checkedInBy = userId,
                deviceId = deviceId,
                location = locationPair
            )
            
            when (result) {
                is TicketScanResult.Success -> {
                    // Mark as scanned to prevent double scanning
                    markAsScanned(previewData.qrCode)
                    
                    _uiState.update { it.copy(
                        isLoading = false,
                        scanningState = ScanningState.SUCCESS,
                        successData = SuccessData(
                            ticket = result.ticket,
                            event = result.event,
                            checkInTime = result.checkInTime
                        ),
                        previewData = null
                    ) }
                    
                    // Play success sound/vibration
                    _events.emit(ScannerEvent.CheckInSuccess(result.ticket))
                    
                    // Auto-reset after delay
                    autoResetScanner()
                }
                is TicketScanResult.AlreadyCheckedIn -> {
                    showError(ScanError.AlreadyCheckedIn(result.checkedInAt))
                }
                is TicketScanResult.InvalidTicket -> {
                    showError(ScanError.InvalidTicket(result.reason))
                }
                is TicketScanResult.WrongEvent -> {
                    showError(ScanError.WrongEvent(
                        ticketEventId = result.ticketEventId,
                        scannedEventId = result.scannedEventId
                    ))
                }
                is TicketScanResult.EventNotActive -> {
                    showError(ScanError.EventNotActive(result.eventStatus))
                }
                is TicketScanResult.Error -> {
                    showError(ScanError.CheckInFailed(result.message))
                }
            }
        }
    }
    
    /**
     * Cancel preview and return to scanning
     */
    fun cancelPreview() {
        _uiState.update { it.copy(
            scanningState = ScanningState.SCANNING,
            previewData = null,
            error = null
        ) }
    }
    
    /**
     * Dismiss error and return to scanning
     */
    fun dismissError() {
        _uiState.update { it.copy(
            scanningState = ScanningState.SCANNING,
            error = null,
            isLoading = false
        ) }
    }
    
    /**
     * Reset scanner to initial state
     */
    fun resetScanner() {
        _uiState.update { QRScannerUiState() }
    }
    
    /**
     * Toggle flashlight
     */
    fun toggleFlashlight() {
        _uiState.update { it.copy(
            isFlashlightOn = !it.isFlashlightOn
        ) }
    }
    
    /**
     * Check if QR code was recently scanned
     */
    private fun isRecentlyScanned(qrCode: String): Boolean {
        val currentTime = System.currentTimeMillis()
        
        // Clean up old timestamps (older than 5 minutes)
        scanTimestamps.entries.removeIf { currentTime - it.value > 5 * 60 * 1000 }
        
        return qrCode in recentlyScannedCodes || qrCode in scanTimestamps.keys
    }
    
    /**
     * Mark QR code as scanned
     */
    private fun markAsScanned(qrCode: String) {
        recentlyScannedCodes.add(qrCode)
        scanTimestamps[qrCode] = System.currentTimeMillis()
    }
    
    /**
     * Show duplicate scan error
     */
    private fun showDuplicateScanError(qrCode: String) {
        val scanTime = scanTimestamps[qrCode]
        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = false,
                scanningState = ScanningState.ERROR,
                error = ScanError.DuplicateScan(scanTime)
            ) }
            _events.emit(ScannerEvent.DuplicateScanDetected)
        }
    }
    
    /**
     * Show error state
     */
    private fun showError(error: ScanError) {
        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = false,
                scanningState = ScanningState.ERROR,
                error = error,
                previewData = null
            ) }
            _events.emit(ScannerEvent.ShowError(error))
            
            // Auto-dismiss after delay
            delay(3000)
            if (_uiState.value.error == error) {
                dismissError()
            }
        }
    }
    
    /**
     * Auto-reset scanner after successful check-in
     */
    private fun autoResetScanner() {
        viewModelScope.launch {
            delay(5000) // Show success for 5 seconds
            if (_uiState.value.scanningState == ScanningState.SUCCESS) {
                resetScanner()
            }
        }
    }
}

/**
 * UI State for QR Scanner
 */
data class QRScannerUiState(
    val isLoading: Boolean = false,
    val scanningState: ScanningState = ScanningState.SCANNING,
    val isFlashlightOn: Boolean = false,
    val previewData: PreviewData? = null,
    val successData: SuccessData? = null,
    val error: ScanError? = null,
    val scannedCount: Int = 0
)

/**
 * Scanning states
 */
enum class ScanningState {
    SCANNING,       // Camera is active, looking for QR codes
    PROCESSING,     // Processing scanned QR code
    PREVIEW,        // Showing ticket preview before check-in
    CHECKING_IN,    // Performing check-in
    SUCCESS,        // Check-in successful
    ERROR           // Error occurred
}

/**
 * Preview data before check-in confirmation
 */
data class PreviewData(
    val ticket: Ticket,
    val event: Event,
    val qrCode: String
)

/**
 * Success data after check-in
 */
data class SuccessData(
    val ticket: Ticket,
    val event: Event,
    val checkInTime: Long
) {
    fun formattedCheckInTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(checkInTime))
    }
}

/**
 * Scan error types
 */
sealed class ScanError {
    data class InvalidQRCode(val reason: String) : ScanError()
    data class InvalidTicket(val reason: String) : ScanError()
    data class ExpiredTicket(val expiredAt: Long) : ScanError() {
        fun formattedExpiry(): String {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(expiredAt))
        }
    }
    data class AlreadyCheckedIn(val checkedInAt: Long) : ScanError() {
        fun formattedCheckInTime(): String {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(Date(checkedInAt))
        }
    }
    data class NotFound(val message: String) : ScanError()
    data class WrongEvent(
        val ticketEventId: String,
        val scannedEventId: String
    ) : ScanError()
    data class EventNotActive(val eventStatus: com.example.eventpay.domain.model.EventStatus) : ScanError()
    data class CheckInFailed(val message: String) : ScanError()
    data class DuplicateScan(val previousScanTime: Long? = null) : ScanError() {
        fun formattedPreviousTime(): String? {
            return previousScanTime?.let {
                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                sdf.format(Date(it))
            }
        }
    }
    data class CameraError(val message: String) : ScanError()
    
    fun getDisplayMessage(): String = when (this) {
        is InvalidQRCode -> "QR Code invalide: $reason"
        is InvalidTicket -> "Ticket invalide: $reason"
        is ExpiredTicket -> "Ticket expiré le ${formattedExpiry()}"
        is AlreadyCheckedIn -> "Déjà enregistré à ${formattedCheckInTime()}"
        is NotFound -> message
        is WrongEvent -> "Ce ticket est pour un autre événement"
        is EventNotActive -> "L'événement n'est pas actif ($eventStatus)"
        is CheckInFailed -> "Échec de l'enregistrement: $message"
        is DuplicateScan -> "QR Code déjà scanné${formattedPreviousTime()?.let { " à $it" } ?: ""}"
        is CameraError -> "Erreur caméra: $message"
    }
    
    fun getIcon(): FeedbackIcon = when (this) {
        is InvalidQRCode, is InvalidTicket -> FeedbackIcon.ERROR
        is ExpiredTicket -> FeedbackIcon.EXPIRED
        is AlreadyCheckedIn, is DuplicateScan -> FeedbackIcon.WARNING
        is NotFound -> FeedbackIcon.ERROR
        is WrongEvent -> FeedbackIcon.WARNING
        is EventNotActive -> FeedbackIcon.WARNING
        is CheckInFailed -> FeedbackIcon.ERROR
        is CameraError -> FeedbackIcon.ERROR
    }
}

/**
 * Feedback icons for UI
 */
enum class FeedbackIcon {
    SUCCESS,
    WARNING,
    ERROR,
    EXPIRED,
    INFO
}

/**
 * One-time events for the scanner
 */
sealed class ScannerEvent {
    data class CheckInSuccess(val ticket: Ticket) : ScannerEvent()
    data class ShowError(val error: ScanError) : ScannerEvent()
    object DuplicateScanDetected : ScannerEvent()
    object Vibrate : ScannerEvent()
    object PlaySuccessSound : ScannerEvent()
    object PlayErrorSound : ScannerEvent()
}
