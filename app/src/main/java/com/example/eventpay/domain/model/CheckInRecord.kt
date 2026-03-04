package com.example.eventpay.domain.model

import com.google.firebase.firestore.GeoPoint

/**
 * Domain Entity - CheckInRecord
 * 
 * Represents a check-in event when a scanner validates a ticket.
 * This is an append-only audit record for attendance tracking.
 * 
 * Security: Only admins and assigned scanners can create check-ins.
 * No updates or deletes are allowed (immutable audit trail).
 */
data class CheckInRecord(
    val id: String = "",
    val ticketId: String = "",
    val eventId: String = "",
    val userId: String = "", // Attendee who owns the ticket
    
    // Scanner Info
    val scannedBy: String = "", // Scanner UID
    val scannedByName: String = "",
    val scannedByRole: UserRole = UserRole.SCANNER,
    
    // Scan Details
    val scannedAt: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val location: GeoPoint? = null, // Optional geolocation
    
    // Result
    val result: CheckInResult = CheckInResult.SUCCESS,
    val message: String? = null, // Additional info (e.g., "Already scanned at XYZ")
    
    // For duplicate scans - reference to previous scan
    val previousScanId: String? = null,
    val previousScanTime: Long? = null
) {
    /**
     * Check if this check-in was successful
     */
    fun isSuccessful(): Boolean = result == CheckInResult.SUCCESS
    
    /**
     * Check if this was a duplicate scan
     */
    fun isDuplicate(): Boolean = result == CheckInResult.ALREADY_SCANNED
}

/**
 * Enum representing the possible results of a check-in attempt
 */
enum class CheckInResult {
    SUCCESS,           // Valid ticket, first scan
    ALREADY_SCANNED,   // Ticket was already checked in
    INVALID,           // QR code is invalid or corrupted
    NOT_FOUND,         // Ticket doesn't exist in database
    EXPIRED,           // Ticket has expired
    WRONG_EVENT,       // Ticket is for a different event
    CANCELLED,         // Ticket was cancelled/refunded
    NO_PERMISSION,     // Scanner not assigned to this event
    ERROR              // General error
}

/**
 * Extension function to get display text for check-in result
 */
fun CheckInResult.displayText(): String = when (this) {
    CheckInResult.SUCCESS -> "Check-in successful!"
    CheckInResult.ALREADY_SCANNED -> "Ticket already checked in"
    CheckInResult.INVALID -> "Invalid ticket"
    CheckInResult.NOT_FOUND -> "Ticket not found"
    CheckInResult.EXPIRED -> "Ticket expired"
    CheckInResult.WRONG_EVENT -> "Wrong event"
    CheckInResult.CANCELLED -> "Ticket cancelled"
    CheckInResult.NO_PERMISSION -> "No permission"
    CheckInResult.ERROR -> "Error processing ticket"
}

/**
 * Extension function to get color code for UI
 */
fun CheckInResult.isSuccess(): Boolean = this == CheckInResult.SUCCESS
