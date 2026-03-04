package com.example.eventpay.domain.qrcode

import com.example.eventpay.domain.model.TicketScanResult
import com.example.eventpay.domain.model.Event
import com.example.eventpay.domain.model.Ticket
import com.example.eventpay.domain.model.TicketStatus
import com.example.eventpay.domain.repository.EventRepository
import com.example.eventpay.domain.repository.TicketRepository
import com.example.eventpay.security.QRCryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * QR Code Validator Service
 * 
 * Handles the complete QR code validation process:
 * 1. Parses and decrypts the QR code
 * 2. Verifies HMAC signature
 * 3. Validates timestamp (not expired)
 * 4. Checks for duplicate scans
 * 5. Verifies ticket and event data
 * 6. Performs check-in if valid
 * 
 * Security Features:
 * - Signature verification prevents tampering
 * - Timestamp validation prevents replay attacks
 * - Nonce tracking prevents duplicate scans
 * - Checksum verification ensures data integrity
 */
@Singleton
class QRCodeValidator @Inject constructor(
    private val cryptoManager: QRCryptoManager,
    private val ticketRepository: TicketRepository,
    private val eventRepository: EventRepository,
    private val scanRecordManager: QRScanRecordManager
) {
    
    /**
     * Validate a scanned QR code
     * 
     * @param qrCodeString The scanned QR code content
     * @param expectedEventId The event ID being scanned for
     * @return QRCodeValidation result
     */
    suspend fun validateQRCode(
        qrCodeString: String,
        expectedEventId: String
    ): QRCodeValidation = withContext(Dispatchers.Default) {
        
        // Step 1: Parse and decrypt the QR code
        val payload = try {
            cryptoManager.parseQRCodeString(qrCodeString).getOrElse { error ->
                return@withContext QRCodeValidation.Invalid(
                    reason = InvalidReason.DECRYPTION_FAILED,
                    message = "Unable to decrypt QR code: ${error.message}"
                )
            }
        } catch (e: Exception) {
            return@withContext QRCodeValidation.Invalid(
                reason = InvalidReason.INVALID_FORMAT,
                message = "Invalid QR code format"
            )
        }
        
        // Step 2: Verify checksum
        val expectedChecksum = cryptoManager.calculateChecksum(
            payload.ticketId,
            payload.eventId,
            payload.userId,
            payload.nonce
        )
        
        if (!payload.verifyChecksum(expectedChecksum)) {
            return@withContext QRCodeValidation.Invalid(
                reason = InvalidReason.INVALID_CHECKSUM,
                message = "QR code data has been tampered with"
            )
        }
        
        // Step 3: Check timestamp validity
        if (cryptoManager.isExpired(payload.timestamp)) {
            return@withContext QRCodeValidation.Expired(
                payload = payload,
                expiredAt = payload.timestamp + QRCodeConfig().qrCodeValidityMs
            )
        }
        
        // Step 4: Verify event match
        if (payload.eventId != expectedEventId) {
            return@withContext QRCodeValidation.Invalid(
                reason = InvalidReason.TAMPERED_DATA,
                message = "QR code is for a different event"
            )
        }
        
        // Step 5: Check for duplicate scan attempts
        val scanRecord = scanRecordManager.getScanRecord(payload.ticketId, payload.eventId)
        if (scanRecord != null && scanRecord.scanCount > 0) {
            return@withContext QRCodeValidation.DuplicateAttempt(
                payload = payload,
                previousScanTime = scanRecord.lastScanTime
            )
        }
        
        // Step 6: Validate nonce uniqueness
        if (!scanRecordManager.isNonceUnique(payload.nonce, payload.ticketId)) {
            return@withContext QRCodeValidation.Invalid(
                reason = InvalidReason.TAMPERED_DATA,
                message = "QR code has already been used"
            )
        }
        
        // All validations passed
        QRCodeValidation.Valid(
            payload = payload,
            decryptedData = qrCodeString
        )
    }
    
    /**
     * Validate and check in a ticket
     * 
     * @param qrCodeString The scanned QR code content
     * @param expectedEventId The event ID being scanned for
     * @param checkedInBy The user ID of the person checking in
     * @param deviceId The device ID used for scanning
     * @param location Optional GPS coordinates
     * @return CheckInResult with the result of the operation
     */
    suspend fun validateAndCheckIn(
        qrCodeString: String,
        expectedEventId: String,
        checkedInBy: String,
        deviceId: String,
        location: Pair<Double, Double>? = null
    ): TicketScanResult = withContext(Dispatchers.Default) {
        
        // Validate the QR code
        when (val validation = validateQRCode(qrCodeString, expectedEventId)) {
            is QRCodeValidation.Valid -> {
                // Get the ticket from repository
                val ticket = ticketRepository.getTicketById(validation.payload.ticketId)
                if (ticket == null) {
                    return@withContext TicketScanResult.InvalidTicket(
                        reason = "Ticket not found in system"
                    )
                }
                
                // Verify ticket belongs to the correct user
                if (ticket.userId != validation.payload.userId) {
                    return@withContext TicketScanResult.InvalidTicket(
                        reason = "Ticket user mismatch"
                    )
                }
                
                // Check ticket status
                if (!ticket.canCheckIn()) {
                    return@withContext when {
                        ticket.isCheckedIn() -> TicketScanResult.AlreadyCheckedIn(
                            ticket = ticket,
                            checkedInAt = ticket.checkedInAt ?: System.currentTimeMillis()
                        )
                        ticket.status == TicketStatus.CANCELLED -> TicketScanResult.InvalidTicket(
                            reason = "Ticket has been cancelled"
                        )
                        ticket.status == TicketStatus.REFUNDED -> TicketScanResult.InvalidTicket(
                            reason = "Ticket has been refunded"
                        )
                        ticket.status == TicketStatus.EXPIRED -> TicketScanResult.InvalidTicket(
                            reason = "Ticket has expired"
                        )
                        else -> TicketScanResult.InvalidTicket(
                            reason = "Ticket is not valid for check-in"
                        )
                    }
                }
                
                // Get the event
                val event = eventRepository.getEventById(expectedEventId)
                if (event == null) {
                    return@withContext TicketScanResult.Error(
                        message = "Event not found"
                    )
                }
                
                // Check event status
                if (!event.status.canCheckIn()) {
                    return@withContext TicketScanResult.EventNotActive(
                        eventStatus = event.status
                    )
                }
                
                // Perform check-in
                val checkInResult = ticketRepository.checkInTicket(
                    qrCode = qrCodeString,
                    eventId = expectedEventId,
                    checkedInBy = checkedInBy
                )
                
                // Record the scan
                if (checkInResult is TicketScanResult.Success) {
                    scanRecordManager.recordScan(
                        ticketId = validation.payload.ticketId,
                        eventId = expectedEventId,
                        nonce = validation.payload.nonce,
                        deviceId = deviceId,
                        scannedBy = checkedInBy,
                        location = location
                    )
                }
                
                checkInResult
            }
            
            is QRCodeValidation.Invalid -> {
                TicketScanResult.InvalidTicket(
                    reason = validation.message
                )
            }
            
            is QRCodeValidation.Expired -> {
                TicketScanResult.InvalidTicket(
                    reason = "QR code has expired"
                )
            }
            
            is QRCodeValidation.DuplicateAttempt -> {
                TicketScanResult.AlreadyCheckedIn(
                    ticket = ticketRepository.getTicketById(validation.payload.ticketId) ?: return@withContext TicketScanResult.Error(
                        message = "Ticket not found"
                    ),
                    checkedInAt = validation.previousScanTime
                )
            }
        }
    }
    
    /**
     * Quick validation without check-in
     * Used for previewing ticket info before confirming check-in
     */
    suspend fun quickValidate(
        qrCodeString: String,
        expectedEventId: String
    ): QuickValidationResult = withContext(Dispatchers.Default) {
        when (val validation = validateQRCode(qrCodeString, expectedEventId)) {
            is QRCodeValidation.Valid -> {
                val ticket = ticketRepository.getTicketById(validation.payload.ticketId)
                val event = eventRepository.getEventById(expectedEventId)
                
                if (ticket != null && event != null) {
                    QuickValidationResult.Valid(
                        ticket = ticket,
                        event = event,
                        payload = validation.payload
                    )
                } else {
                    QuickValidationResult.NotFound(
                        ticketFound = ticket != null,
                        eventFound = event != null
                    )
                }
            }
            is QRCodeValidation.Invalid -> {
                QuickValidationResult.Invalid(validation.message)
            }
            is QRCodeValidation.Expired -> {
                QuickValidationResult.Expired(validation.expiredAt)
            }
            is QRCodeValidation.DuplicateAttempt -> {
                QuickValidationResult.Duplicate(validation.previousScanTime)
            }
        }
    }
    
    /**
     * Verify QR code belongs to a specific ticket
     */
    suspend fun verifyTicketOwnership(
        qrCodeString: String,
        expectedTicketId: String,
        expectedUserId: String
    ): Boolean = withContext(Dispatchers.Default) {
        val payload = cryptoManager.parseQRCodeString(qrCodeString).getOrNull() ?: return@withContext false
        
        payload.ticketId == expectedTicketId && payload.userId == expectedUserId
    }
}

/**
 * Quick validation result for preview
 */
sealed class QuickValidationResult {
    data class Valid(
        val ticket: Ticket,
        val event: Event,
        val payload: QRCodePayload.TicketPayload
    ) : QuickValidationResult()
    
    data class NotFound(
        val ticketFound: Boolean,
        val eventFound: Boolean
    ) : QuickValidationResult()
    
    data class Invalid(val reason: String) : QuickValidationResult()
    data class Expired(val expiredAt: Long) : QuickValidationResult()
    data class Duplicate(val previousScanTime: Long) : QuickValidationResult()
}

/**
 * Scan Record Manager
 * Handles tracking of QR code scans for duplicate detection
 */
@Singleton
class QRScanRecordManager @Inject constructor(
    // Would inject a local database DAO for persistence
) {
    // In-memory cache for demo - would use Room database in production
    private val scanRecords = mutableMapOf<String, QRCodeScanRecord>()
    private val usedNonces = mutableSetOf<String>()
    
    /**
     * Get scan record for a ticket
     */
    fun getScanRecord(ticketId: String, eventId: String): QRCodeScanRecord? {
        val key = "$eventId:$ticketId"
        return scanRecords[key]
    }
    
    /**
     * Check if nonce has been used before
     */
    fun isNonceUnique(nonce: String, ticketId: String): Boolean {
        val key = "$ticketId:$nonce"
        return !usedNonces.contains(key)
    }
    
    /**
     * Record a successful scan
     */
    fun recordScan(
        ticketId: String,
        eventId: String,
        nonce: String,
        deviceId: String,
        scannedBy: String,
        location: Pair<Double, Double>?
    ) {
        val key = "$eventId:$ticketId"
        val nonceKey = "$ticketId:$nonce"
        val now = System.currentTimeMillis()
        
        // Mark nonce as used
        usedNonces.add(nonceKey)
        
        // Update or create scan record
        val existing = scanRecords[key]
        val scanLocation = ScanLocation(
            latitude = location?.first,
            longitude = location?.second,
            deviceId = deviceId,
            scannedBy = scannedBy,
            timestamp = now
        )
        
        if (existing != null) {
            scanRecords[key] = existing.copy(
                lastScanTime = now,
                scanCount = existing.scanCount + 1,
                scanLocations = existing.scanLocations + scanLocation
            )
        } else {
            scanRecords[key] = QRCodeScanRecord(
                ticketId = ticketId,
                eventId = eventId,
                nonce = nonce,
                firstScanTime = now,
                lastScanTime = now,
                scanCount = 1,
                scanLocations = listOf(scanLocation)
            )
        }
    }
    
    /**
     * Clear scan records (for testing)
     */
    fun clearRecords() {
        scanRecords.clear()
        usedNonces.clear()
    }
}
