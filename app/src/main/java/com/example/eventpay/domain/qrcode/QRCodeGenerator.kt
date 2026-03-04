package com.example.eventpay.domain.qrcode

import android.graphics.Bitmap
import android.graphics.Color
import com.example.eventpay.security.QRCryptoManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * QR Code Generator Service
 * 
 * Handles the complete QR code generation process:
 * 1. Creates secure payload with all ticket data
 * 2. Encrypts the payload using AES-256-GCM
 * 3. Signs the encrypted data with HMAC-SHA256
 * 4. Generates QR code bitmap for display
 * 
 * Security Features:
 * - Unique QR per ticket (nonce-based)
 * - Encrypted data (AES-256-GCM)
 * - Tamper-proof (HMAC signature)
 * - Time-limited validity
 * - Cannot be duplicated (unique nonce per ticket)
 */
@Singleton
class QRCodeGenerator @Inject constructor(
    private val cryptoManager: QRCryptoManager
) {
    companion object {
        const val DEFAULT_QR_SIZE = 512
        const val MIN_QR_SIZE = 256
        const val MAX_QR_SIZE = 1024
    }
    
    /**
     * Generate a secure QR code for a ticket
     * 
     * @param request The QR code generation request
     * @return QRCodeGenerationResult containing the QR string and payload
     */
    fun generateQRCode(request: QRCodeGenerationRequest): QRCodeGenerationResult {
        return try {
            // Validate input
            val validationError = validateRequest(request)
            if (validationError != null) {
                return QRCodeGenerationResult.Failure(
                    error = GenerationError.INVALID_INPUT_DATA,
                    message = validationError
                )
            }
            
            // Generate unique nonce for this ticket
            val nonce = cryptoManager.generateNonce()
            
            // Calculate checksum for data integrity
            val checksum = cryptoManager.calculateChecksum(
                request.ticketId,
                request.eventId,
                request.userId,
                nonce
            )
            
            // Create the payload
            val timestamp = System.currentTimeMillis()
            val payload = QRCodePayload.TicketPayload(
                ticketId = request.ticketId,
                eventId = request.eventId,
                userId = request.userId,
                ticketType = request.ticketType,
                seatNumber = request.seatNumber,
                nonce = nonce,
                checksum = checksum,
                timestamp = timestamp,
                signature = "" // Will be set after encryption
            )
            
            // Create the signed QR code string
            val qrCodeString = cryptoManager.createQRCodeString(payload).getOrElse { error ->
                return QRCodeGenerationResult.Failure(
                    error = GenerationError.ENCRYPTION_FAILED,
                    message = error.message ?: "Encryption failed"
                )
            }
            
            // Calculate expiration time
            val validityMs = request.validityMs ?: QRCodeConfig().qrCodeValidityMs
            val expiresAt = timestamp + validityMs
            
            QRCodeGenerationResult.Success(
                qrCodeString = qrCodeString,
                payload = payload,
                generatedAt = timestamp,
                expiresAt = expiresAt
            )
        } catch (e: Exception) {
            QRCodeGenerationResult.Failure(
                error = GenerationError.SYSTEM_ERROR,
                message = e.message ?: "Unknown error occurred"
            )
        }
    }
    
    /**
     * Generate QR code bitmap from string
     * 
     * @param qrCodeString The QR code content
     * @param size Size of the bitmap in pixels
     * @return Bitmap of the QR code
     */
    fun generateQRCodeBitmap(
        qrCodeString: String,
        size: Int = DEFAULT_QR_SIZE
    ): Bitmap? {
        val actualSize = size.coerceIn(MIN_QR_SIZE, MAX_QR_SIZE)
        
        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M
            )
            
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(
                qrCodeString,
                BarcodeFormat.QR_CODE,
                actualSize,
                actualSize,
                hints
            )
            
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            
            for (y in 0 until height) {
                for (x in 0 until width) {
                    pixels[y * width + x] = if (bitMatrix[x, y]) {
                        Color.BLACK
                    } else {
                        Color.WHITE
                    }
                }
            }
            
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Generate QR code bitmap with custom colors
     * 
     * @param qrCodeString The QR code content
     * @param size Size of the bitmap in pixels
     * @param darkColor Color for dark modules
     * @param lightColor Color for light modules
     * @return Bitmap of the QR code
     */
    fun generateQRCodeBitmapWithColors(
        qrCodeString: String,
        size: Int = DEFAULT_QR_SIZE,
        darkColor: Int = Color.BLACK,
        lightColor: Int = Color.WHITE
    ): Bitmap? {
        val actualSize = size.coerceIn(MIN_QR_SIZE, MAX_QR_SIZE)
        
        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M
            )
            
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(
                qrCodeString,
                BarcodeFormat.QR_CODE,
                actualSize,
                actualSize,
                hints
            )
            
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            
            for (y in 0 until height) {
                for (x in 0 until width) {
                    pixels[y * width + x] = if (bitMatrix[x, y]) {
                        darkColor
                    } else {
                        lightColor
                    }
                }
            }
            
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Generate QR code asynchronously
     */
    suspend fun generateQRCodeAsync(request: QRCodeGenerationRequest): QRCodeGenerationResult {
        return withContext(Dispatchers.Default) {
            generateQRCode(request)
        }
    }
    
    /**
     * Generate QR code bitmap asynchronously
     */
    suspend fun generateQRCodeBitmapAsync(
        qrCodeString: String,
        size: Int = DEFAULT_QR_SIZE
    ): Bitmap? {
        return withContext(Dispatchers.Default) {
            generateQRCodeBitmap(qrCodeString, size)
        }
    }
    
    /**
     * Validate the generation request
     */
    private fun validateRequest(request: QRCodeGenerationRequest): String? {
        if (request.ticketId.isBlank()) {
            return "Ticket ID is required"
        }
        if (request.eventId.isBlank()) {
            return "Event ID is required"
        }
        if (request.userId.isBlank()) {
            return "User ID is required"
        }
        if (request.ticketType.isBlank()) {
            return "Ticket type is required"
        }
        return null
    }
    
    /**
     * Regenerate QR code for an existing ticket
     * Used when QR code needs to be refreshed
     */
    fun regenerateQRCode(
        ticketId: String,
        eventId: String,
        userId: String,
        ticketType: String,
        seatNumber: String? = null
    ): QRCodeGenerationResult {
        return generateQRCode(
            QRCodeGenerationRequest(
                ticketId = ticketId,
                eventId = eventId,
                userId = userId,
                ticketType = ticketType,
                seatNumber = seatNumber
            )
        )
    }
}
