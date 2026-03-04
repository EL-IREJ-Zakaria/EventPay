package com.example.eventpay.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.eventpay.domain.qrcode.QRCodeConfig
import com.example.eventpay.domain.qrcode.QRCodeDTO
import com.example.eventpay.domain.qrcode.QRCodePayload
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * QR Code Cryptography Manager
 * 
 * Handles all cryptographic operations for QR code generation and validation:
 * - AES-256-GCM encryption/decryption
 * - HMAC-SHA256 signing/verification
 * - Secure nonce generation
 * - Checksum calculation
 * 
 * Security Features:
 * - Uses Android Keystore for key storage
 * - AES-256-GCM for authenticated encryption
 * - HMAC-SHA256 for message authentication
 * - Unique nonce per QR code (prevents replay attacks)
 * - Timestamp validation (prevents expired QR usage)
 */
@Singleton
class QRCryptoManager @Inject constructor(
    private val config: QRCodeConfig = QRCodeConfig()
) {
    
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val HMAC_ALGORITHM = "HmacSHA256"
        private const val TAG_LENGTH_BITS = 128
        private const val IV_LENGTH_BYTES = 12
        
        // Fallback key for devices without proper Keystore support
        private const val FALLBACK_KEY_ALIAS = "eventpay_fallback_key"
    }
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }
    
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }
    
    // Cache for encryption key
    @Volatile
    private var encryptionKey: SecretKey? = null
    
    // Cache for HMAC key
    @Volatile
    private var hmacKey: SecretKey? = null
    
    /**
     * Initialize cryptographic keys
     * Should be called during app initialization
     */
    fun initializeKeys(): Result<Unit> {
        return try {
            getOrCreateEncryptionKey()
            getOrCreateHmacKey()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Generate a secure random nonce
     * @param length Number of bytes for the nonce
     * @return Base64 encoded nonce string
     */
    fun generateNonce(length: Int = config.nonceLength): String {
        val nonce = ByteArray(length)
        SecureRandom().nextBytes(nonce)
        return Base64.encodeToString(nonce, Base64.NO_WRAP)
    }
    
    /**
     * Calculate SHA-256 checksum of the payload data
     * @param data The data to checksum
     * @return Hex string of the checksum
     */
    fun calculateChecksum(vararg data: String): String {
        val combined = data.joinToString("|")
        val digest = MessageDigest.getInstance(config.checksumAlgorithm)
        val hash = digest.digest(combined.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Encrypt the QR code payload
     * @param payload The payload to encrypt
     * @return Base64 encoded encrypted data
     */
    fun encryptPayload(payload: QRCodePayload.TicketPayload): Result<String> {
        return try {
            val key = getOrCreateEncryptionKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            
            // Generate random IV
            val iv = ByteArray(IV_LENGTH_BYTES)
            SecureRandom().nextBytes(iv)
            
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
            
            val payloadJson = json.encodeToString(payload)
            val encryptedData = cipher.doFinal(payloadJson.toByteArray(Charsets.UTF_8))
            
            // Combine IV + encrypted data
            val combined = iv + encryptedData
            Result.success(Base64.encodeToString(combined, Base64.NO_WRAP))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Decrypt the QR code payload
     * @param encryptedData Base64 encoded encrypted data
     * @return Decrypted payload or error
     */
    fun decryptPayload(encryptedData: String): Result<QRCodePayload.TicketPayload> {
        return try {
            val key = getOrCreateEncryptionKey()
            val combined = Base64.decode(encryptedData, Base64.NO_WRAP)
            
            // Extract IV and encrypted data
            val iv = combined.copyOfRange(0, IV_LENGTH_BYTES)
            val cipherText = combined.copyOfRange(IV_LENGTH_BYTES, combined.size)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
            
            val decryptedBytes = cipher.doFinal(cipherText)
            val payloadJson = String(decryptedBytes, Charsets.UTF_8)
            
            val payload = json.decodeFromString<QRCodePayload.TicketPayload>(payloadJson)
            Result.success(payload)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sign the QR code data with HMAC
     * @param data The data to sign
     * @return Base64 encoded HMAC signature
     */
    fun signData(vararg data: String): String {
        val key = getOrCreateHmacKey()
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(key)
        
        val combined = data.joinToString("|")
        val signature = mac.doFinal(combined.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(signature, Base64.NO_WRAP)
    }
    
    /**
     * Verify the HMAC signature
     * @param signature The signature to verify
     * @param data The data to verify against
     * @return True if signature is valid
     */
    fun verifySignature(signature: String, vararg data: String): Boolean {
        return try {
            val expectedSignature = signData(*data)
            signature == expectedSignature
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Create the final QR code string
     * @param payload The ticket payload
     * @return Complete QR code string ready for encoding
     */
    fun createQRCodeString(payload: QRCodePayload.TicketPayload): Result<String> {
        return try {
            // Encrypt the payload
            val encryptedData = encryptPayload(payload).getOrThrow()
            
            // Sign the encrypted data
            val hmac = signData(encryptedData, payload.timestamp.toString())
            
            // Create the DTO
            val dto = QRCodeDTO(
                prefix = QRCodePayload.PREFIX,
                version = payload.version,
                encryptedData = encryptedData,
                hmac = hmac
            )
            
            // Return as JSON string
            Result.success(dto.toJson())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Parse and decrypt QR code string
     * @param qrCodeString The QR code string to parse
     * @return Decrypted payload or error
     */
    fun parseQRCodeString(qrCodeString: String): Result<QRCodePayload.TicketPayload> {
        return try {
            // Parse the DTO
            val dto = QRCodeDTO.fromJson(qrCodeString)
                ?: return Result.failure(IllegalArgumentException("Invalid QR code format"))
            
            // Verify prefix
            if (dto.prefix != QRCodePayload.PREFIX) {
                return Result.failure(IllegalArgumentException("Invalid QR code prefix"))
            }
            
            // Verify HMAC
            val payload = decryptPayload(dto.encryptedData).getOrThrow()
            if (!verifySignature(dto.hmac, dto.encryptedData, payload.timestamp.toString())) {
                return Result.failure(IllegalArgumentException("Invalid signature"))
            }
            
            Result.success(payload)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Validate timestamp is within acceptable range
     * @param timestamp The timestamp to validate
     * @return True if timestamp is valid
     */
    fun isTimestampValid(timestamp: Long): Boolean {
        val now = System.currentTimeMillis()
        val minTime = now - config.clockSkewToleranceMs
        val maxTime = now + config.clockSkewToleranceMs + config.qrCodeValidityMs
        
        return timestamp in minTime..maxTime
    }
    
    /**
     * Check if QR code has expired
     * @param timestamp The QR code timestamp
     * @return True if expired
     */
    fun isExpired(timestamp: Long): Boolean {
        val now = System.currentTimeMillis()
        return now > (timestamp + config.qrCodeValidityMs)
    }
    
    /**
     * Get or create the encryption key
     */
    private fun getOrCreateEncryptionKey(): SecretKey {
        return encryptionKey ?: synchronized(this) {
            encryptionKey ?: run {
                try {
                    // Try to get key from Keystore
                    if (keyStore.containsAlias(config.encryptionKeyAlias)) {
                        val entry = keyStore.getEntry(config.encryptionKeyAlias, null)
                            as KeyStore.SecretKeyEntry
                        entry.secretKey
                    } else {
                        // Generate new key
                        createEncryptionKey()
                    }
                } catch (e: Exception) {
                    // Fallback to generated key (less secure but works on all devices)
                    createFallbackEncryptionKey()
                }.also { encryptionKey = it }
            }
        }
    }
    
    /**
     * Get or create the HMAC key
     */
    private fun getOrCreateHmacKey(): SecretKey {
        return hmacKey ?: synchronized(this) {
            hmacKey ?: run {
                try {
                    if (keyStore.containsAlias(config.signatureKeyAlias)) {
                        val entry = keyStore.getEntry(config.signatureKeyAlias, null)
                            as KeyStore.SecretKeyEntry
                        entry.secretKey
                    } else {
                        createHmacKey()
                    }
                } catch (e: Exception) {
                    createFallbackHmacKey()
                }.also { hmacKey = it }
            }
        }
    }
    
    /**
     * Create encryption key in Android Keystore
     */
    private fun createEncryptionKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        
        val spec = KeyGenParameterSpec.Builder(
            config.encryptionKeyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
    
    /**
     * Create HMAC key in Android Keystore
     */
    private fun createHmacKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_HMAC_SHA256,
            ANDROID_KEYSTORE
        )
        
        val spec = KeyGenParameterSpec.Builder(
            config.signatureKeyAlias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setKeySize(256)
            .build()
        
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
    
    /**
     * Fallback encryption key for devices without proper Keystore support
     * Note: This is less secure than Keystore-backed keys
     */
    private fun createFallbackEncryptionKey(): SecretKey {
        // Generate a deterministic key from the alias (for consistency)
        val keyBytes = MessageDigest.getInstance("SHA-256")
            .digest(FALLBACK_KEY_ALIAS.toByteArray())
        return SecretKeySpec(keyBytes, "AES")
    }
    
    /**
     * Fallback HMAC key for devices without proper Keystore support
     */
    private fun createFallbackHmacKey(): SecretKey {
        val keyBytes = MessageDigest.getInstance("SHA-256")
            .digest((FALLBACK_KEY_ALIAS + "_hmac").toByteArray())
        return SecretKeySpec(keyBytes, HMAC_ALGORITHM)
    }
    
    /**
     * Clear cached keys (for testing or key rotation)
     */
    fun clearCachedKeys() {
        encryptionKey = null
        hmacKey = null
    }
    
    /**
     * Check if keys are properly initialized
     */
    fun areKeysInitialized(): Boolean {
        return try {
            encryptionKey != null && hmacKey != null
        } catch (e: Exception) {
            false
        }
    }
}
