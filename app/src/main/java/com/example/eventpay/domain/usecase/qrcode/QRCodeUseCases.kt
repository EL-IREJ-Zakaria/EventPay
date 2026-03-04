package com.example.eventpay.domain.usecase.qrcode

import android.graphics.Bitmap
import com.example.eventpay.domain.model.Ticket
import com.example.eventpay.domain.model.TicketScanResult
import com.example.eventpay.domain.qrcode.*
import com.example.eventpay.domain.repository.TicketRepository
import com.example.eventpay.security.QRCryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use Case: Generate Secure QR Code
 * 
 * Generates a secure, encrypted QR code for a ticket.
 * Each QR code is unique and cannot be duplicated.
 */
class GenerateSecureQRCodeUseCase @Inject constructor(
    private val qrCodeGenerator: QRCodeGenerator,
    private val ticketRepository: TicketRepository
) {
    /**
     * Generate a secure QR code for a ticket
     * 
     * @param ticketId The ticket ID
     * @param eventId The event ID
     * @param userId The user ID who owns the ticket
     * @param ticketType The type of ticket
     * @param seatNumber Optional seat number
     * @param qrSize Size of the QR code bitmap
     * @return Pair of QR code string and bitmap
     */
    suspend operator fun invoke(
        ticketId: String,
        eventId: String,
        userId: String,
        ticketType: String,
        seatNumber: String? = null,
        qrSize: Int = 512
    ): Result<SecureQRCodeResult> = withContext(Dispatchers.Default) {
        
        // Generate the QR code
        val request = QRCodeGenerationRequest(
            ticketId = ticketId,
            eventId = eventId,
            userId = userId,
            ticketType = ticketType,
            seatNumber = seatNumber
        )
        
        when (val result = qrCodeGenerator.generateQRCode(request)) {
            is QRCodeGenerationResult.Success -> {
                // Generate bitmap
                val bitmap = qrCodeGenerator.generateQRCodeBitmap(result.qrCodeString, qrSize)
                
                if (bitmap != null) {
                    Result.success(
                        SecureQRCodeResult(
                            qrCodeString = result.qrCodeString,
                            bitmap = bitmap,
                            payload = result.payload,
                            generatedAt = result.generatedAt,
                            expiresAt = result.expiresAt
                        )
                    )
                } else {
                    Result.failure(Exception("Failed to generate QR code bitmap"))
                }
            }
            is QRCodeGenerationResult.Failure -> {
                Result.failure(Exception("${result.error}: ${result.message}"))
            }
        }
    }
    
    /**
     * Generate QR code for an existing ticket
     */
    suspend operator fun invoke(ticket: Ticket): Result<SecureQRCodeResult> {
        return invoke(
            ticketId = ticket.id,
            eventId = ticket.eventId,
            userId = ticket.userId,
            ticketType = ticket.ticketType.name,
            seatNumber = ticket.seatNumber
        )
    }
}

/**
 * Use Case: Validate Secure QR Code
 * 
 * Validates a scanned QR code and returns the ticket information.
 */
class ValidateSecureQRCodeUseCase @Inject constructor(
    private val qrCodeValidator: QRCodeValidator
) {
    /**
     * Validate a scanned QR code
     * 
     * @param qrCodeString The scanned QR code content
     * @param expectedEventId The event ID being scanned for
     * @return Validation result
     */
    suspend operator fun invoke(
        qrCodeString: String,
        expectedEventId: String
    ): QRCodeValidation {
        return qrCodeValidator.validateQRCode(qrCodeString, expectedEventId)
    }
}

/**
 * Use Case: Check In via Secure QR Code
 * 
 * Performs check-in using a scanned QR code with full validation.
 */
class CheckInViaSecureQRCodeUseCase @Inject constructor(
    private val qrCodeValidator: QRCodeValidator
) {
    /**
     * Check in a ticket using QR code
     * 
     * @param qrCodeString The scanned QR code content
     * @param eventId The event ID being scanned for
     * @param checkedInBy The user ID of the person checking in
     * @param deviceId The device ID used for scanning
     * @param location Optional GPS coordinates (latitude, longitude)
     * @return TicketScanResult
     */
    suspend operator fun invoke(
        qrCodeString: String,
        eventId: String,
        checkedInBy: String,
        deviceId: String,
        location: Pair<Double, Double>? = null
    ): TicketScanResult {
        return qrCodeValidator.validateAndCheckIn(
            qrCodeString = qrCodeString,
            expectedEventId = eventId,
            checkedInBy = checkedInBy,
            deviceId = deviceId,
            location = location
        )
    }
}

/**
 * Use Case: Quick Validate QR Code
 * 
 * Quickly validates a QR code without performing check-in.
 * Used for previewing ticket info before confirming.
 */
class QuickValidateQRCodeUseCase @Inject constructor(
    private val qrCodeValidator: QRCodeValidator
) {
    suspend operator fun invoke(
        qrCodeString: String,
        expectedEventId: String
    ): QuickValidationResult {
        return qrCodeValidator.quickValidate(qrCodeString, expectedEventId)
    }
}

/**
 * Use Case: Regenerate QR Code
 * 
 * Regenerates a QR code for a ticket (e.g., if expired or lost).
 * This creates a new QR code with a new nonce.
 */
class RegenerateQRCodeUseCase @Inject constructor(
    private val qrCodeGenerator: QRCodeGenerator,
    private val ticketRepository: TicketRepository
) {
    suspend operator fun invoke(
        ticketId: String,
        qrSize: Int = 512
    ): Result<SecureQRCodeResult> = withContext(Dispatchers.Default) {
        
        // Get the ticket
        val ticket = ticketRepository.getTicketById(ticketId)
            ?: return@withContext Result.failure(Exception("Ticket not found"))
        
        // Generate new QR code
        val request = QRCodeGenerationRequest(
            ticketId = ticket.id,
            eventId = ticket.eventId,
            userId = ticket.userId,
            ticketType = ticket.ticketType.name,
            seatNumber = ticket.seatNumber
        )
        
        when (val result = qrCodeGenerator.generateQRCode(request)) {
            is QRCodeGenerationResult.Success -> {
                val bitmap = qrCodeGenerator.generateQRCodeBitmap(result.qrCodeString, qrSize)
                
                if (bitmap != null) {
                    // Update ticket with new QR code
                    ticketRepository.updateTicket(
                        ticket.copy(qrCode = result.qrCodeString)
                    )
                    
                    Result.success(
                        SecureQRCodeResult(
                            qrCodeString = result.qrCodeString,
                            bitmap = bitmap,
                            payload = result.payload,
                            generatedAt = result.generatedAt,
                            expiresAt = result.expiresAt
                        )
                    )
                } else {
                    Result.failure(Exception("Failed to generate QR code bitmap"))
                }
            }
            is QRCodeGenerationResult.Failure -> {
                Result.failure(Exception("${result.error}: ${result.message}"))
            }
        }
    }
}

/**
 * Use Case: Verify QR Code Ownership
 * 
 * Verifies that a QR code belongs to a specific user.
 */
class VerifyQRCodeOwnershipUseCase @Inject constructor(
    private val qrCodeValidator: QRCodeValidator
) {
    suspend operator fun invoke(
        qrCodeString: String,
        expectedTicketId: String,
        expectedUserId: String
    ): Boolean {
        return qrCodeValidator.verifyTicketOwnership(
            qrCodeString = qrCodeString,
            expectedTicketId = expectedTicketId,
            expectedUserId = expectedUserId
        )
    }
}

/**
 * Use Case: Initialize QR Code Security
 * 
 * Initializes the cryptographic keys for QR code generation.
 * Should be called during app startup.
 */
class InitializeQRSecurityUseCase @Inject constructor(
    private val cryptoManager: QRCryptoManager
) {
    operator fun invoke(): Result<Unit> {
        return cryptoManager.initializeKeys()
    }
}

/**
 * Result of secure QR code generation
 */
data class SecureQRCodeResult(
    val qrCodeString: String,
    val bitmap: Bitmap,
    val payload: QRCodePayload.TicketPayload,
    val generatedAt: Long,
    val expiresAt: Long
) {
    /**
     * Check if the QR code is still valid
     */
    fun isValid(): Boolean = System.currentTimeMillis() < expiresAt
    
    /**
     * Get remaining validity time in milliseconds
     */
    fun remainingValidity(): Long = (expiresAt - System.currentTimeMillis()).coerceAtLeast(0)
}

/**
 * QR Code parsing utilities (legacy support)
 */
object QRCodeParser {
    
    /**
     * Parse legacy QR code content
     * Format: "eventId:ticketId:signature"
     */
    fun parse(qrCode: String): LegacyQRCodeData? {
        val parts = qrCode.split(":")
        if (parts.size >= 2) {
            return LegacyQRCodeData(
                eventId = parts[0],
                ticketId = parts[1],
                signature = parts.getOrNull(2)
            )
        }
        return null
    }
    
    /**
     * Build legacy QR code content
     */
    fun build(eventId: String, ticketId: String, signature: String? = null): String {
        return if (signature != null) {
            "$eventId:$ticketId:$signature"
        } else {
            "$eventId:$ticketId"
        }
    }
    
    /**
     * Check if QR code is in legacy format
     */
    fun isLegacyFormat(qrCode: String): Boolean {
        return !qrCode.contains("\"prefix\"") && qrCode.split(":").size >= 2
    }
}

/**
 * Legacy QR code data for backward compatibility
 */
data class LegacyQRCodeData(
    val eventId: String,
    val ticketId: String,
    val signature: String? = null
)

/**
 * Aggregated QR Code Use Cases
 */
@Singleton
class QRCodeUseCases @Inject constructor(
    val generateSecureQRCode: GenerateSecureQRCodeUseCase,
    val validateSecureQRCode: ValidateSecureQRCodeUseCase,
    val checkInViaSecureQRCode: CheckInViaSecureQRCodeUseCase,
    val quickValidateQRCode: QuickValidateQRCodeUseCase,
    val regenerateQRCode: RegenerateQRCodeUseCase,
    val verifyQRCodeOwnership: VerifyQRCodeOwnershipUseCase,
    val initializeQRSecurity: InitializeQRSecurityUseCase
)
