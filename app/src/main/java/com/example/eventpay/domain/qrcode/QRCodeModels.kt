package com.example.eventpay.domain.qrcode

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * QR Code Data Structure for Event Tickets
 * 
 * This sealed class represents the complete QR code payload structure
 * with versioning support for future enhancements.
 */
@Serializable
sealed class QRCodePayload {
    abstract val version: Int
    abstract val timestamp: Long
    abstract val signature: String
    
    /**
     * Ticket QR Code Payload
     * Contains all necessary data for ticket validation
     */
    @Serializable
    data class TicketPayload(
        override val version: Int = CURRENT_VERSION,
        override val timestamp: Long = System.currentTimeMillis(),
        override val signature: String,
        val ticketId: String,
        val eventId: String,
        val userId: String,
        val ticketType: String,
        val seatNumber: String? = null,
        val nonce: String,
        val checksum: String
    ) : QRCodePayload() {
        
        /**
         * Generate a unique validation key for this ticket
         */
        fun validationKey(): String = "$eventId:$ticketId:$nonce"
        
        /**
         * Verify the checksum matches the data
         */
        fun verifyChecksum(expectedChecksum: String): Boolean {
            return checksum == expectedChecksum
        }
    }
    
    companion object {
        const val CURRENT_VERSION = 1
        const val PREFIX = "EVP"  // EventPay prefix
        
        /**
         * Parse JSON string to QRCodePayload
         */
        fun fromJson(json: String): QRCodePayload? {
            return try {
                Json.decodeFromString<TicketPayload>(json)
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * QR Code Data Transfer Object
 * Used for encoding into the QR code
 */
@Serializable
data class QRCodeDTO(
    val prefix: String = QRCodePayload.PREFIX,
    val version: Int,
    val encryptedData: String,
    val hmac: String
) {
    fun toJson(): String = Json.encodeToString(this)
    
    companion object {
        fun fromJson(json: String): QRCodeDTO? {
            return try {
                Json.decodeFromString<QRCodeDTO>(json)
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * QR Code Validation Result
 */
sealed class QRCodeValidation {
    data class Valid(
        val payload: QRCodePayload.TicketPayload,
        val decryptedData: String
    ) : QRCodeValidation()
    
    data class Invalid(
        val reason: InvalidReason,
        val message: String
    ) : QRCodeValidation()
    
    data class Expired(
        val payload: QRCodePayload.TicketPayload,
        val expiredAt: Long
    ) : QRCodeValidation()
    
    data class DuplicateAttempt(
        val payload: QRCodePayload.TicketPayload,
        val previousScanTime: Long
    ) : QRCodeValidation()
}

/**
 * Reasons for QR code validation failure
 */
enum class InvalidReason {
    INVALID_FORMAT,          // QR code format is not recognized
    INVALID_SIGNATURE,       // HMAC signature verification failed
    INVALID_CHECKSUM,        // Data checksum mismatch
    DECRYPTION_FAILED,       // Unable to decrypt the data
    TAMPERED_DATA,           // Data appears to be tampered with
    EXPIRED_QR_CODE,         // QR code has expired
    INVALID_VERSION,         // Unsupported version
    MISSING_FIELDS,          // Required fields are missing
    INVALID_PREFIX,          // Invalid prefix for EventPay QR code
    CLOCK_SKEW_DETECTED      // Timestamp is outside acceptable range
}

/**
 * QR Code Configuration
 */
data class QRCodeConfig(
    val encryptionKeyAlias: String = "eventpay_qr_key",
    val signatureKeyAlias: String = "eventpay_signature_key",
    val qrCodeValidityMs: Long = 24 * 60 * 60 * 1000, // 24 hours
    val clockSkewToleranceMs: Long = 5 * 60 * 1000,   // 5 minutes
    val maxScanAttempts: Int = 3,
    val nonceLength: Int = 16,
    val checksumAlgorithm: String = "SHA-256"
)

/**
 * QR Code Generation Request
 */
data class QRCodeGenerationRequest(
    val ticketId: String,
    val eventId: String,
    val userId: String,
    val ticketType: String,
    val seatNumber: String? = null,
    val validityMs: Long? = null  // Override default validity
)

/**
 * QR Code Generation Result
 */
sealed class QRCodeGenerationResult {
    data class Success(
        val qrCodeString: String,
        val payload: QRCodePayload.TicketPayload,
        val generatedAt: Long,
        val expiresAt: Long
    ) : QRCodeGenerationResult()
    
    data class Failure(
        val error: GenerationError,
        val message: String
    ) : QRCodeGenerationResult()
}

/**
 * QR Code Generation Errors
 */
enum class GenerationError {
    ENCRYPTION_FAILED,
    KEY_GENERATION_FAILED,
    INVALID_INPUT_DATA,
    SIGNING_FAILED,
    SYSTEM_ERROR
}

/**
 * QR Code Scan Record
 * Tracks scan attempts for duplicate detection
 */
@Serializable
data class QRCodeScanRecord(
    val ticketId: String,
    val eventId: String,
    val nonce: String,
    val firstScanTime: Long,
    val lastScanTime: Long,
    val scanCount: Int,
    val scanLocations: List<ScanLocation> = emptyList()
)

/**
 * Scan location for audit trail
 */
@Serializable
data class ScanLocation(
    val latitude: Double?,
    val longitude: Double?,
    val deviceId: String,
    val scannedBy: String,
    val timestamp: Long
)
