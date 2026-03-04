package com.example.eventpay.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.eventpay.domain.model.TicketStatus
import com.example.eventpay.domain.model.TicketType

/**
 * Offline-First Database Schema
 * 
 * This file contains all Room entities designed for offline-first operation.
 * Each entity includes sync status tracking for eventual consistency with remote server.
 */

// ============================================================================
// SYNC STATUS TRACKING
// ============================================================================

/**
 * Sync status for tracking pending operations
 */
enum class SyncStatus {
    SYNCED,          // Synchronized with server
    PENDING,         // Waiting to sync
    SYNCING,         // Currently syncing
    FAILED,          // Sync failed (will retry)
    CONFLICT         // Conflict detected (needs resolution)
}

/**
 * Operation type for pending sync queue
 */
enum class SyncOperation {
    CREATE,
    UPDATE,
    DELETE,
    CHECK_IN         // Special operation for check-ins
}

// ============================================================================
// OFFLINE TICKET ENTITY
// ============================================================================

/**
 * Offline Ticket Entity
 * 
 * Extends the base ticket model with sync tracking fields.
 * Supports offline check-in with eventual sync to server.
 */
@Entity(
    tableName = "offline_tickets",
    indices = [
        Index(value = ["qrCode"], unique = true),
        Index(value = ["eventId"]),
        Index(value = ["userId"]),
        Index(value = ["syncStatus"])
    ]
)
data class OfflineTicketEntity(
    @PrimaryKey
    val id: String,
    val eventId: String,
    val userId: String,
    val ticketType: String,
    val price: Double,
    val purchaseDate: Long,
    val status: String,
    val qrCode: String,
    val seatNumber: String? = null,
    val notes: String? = null,
    
    // Check-in fields
    val isCheckedIn: Boolean = false,
    val checkedInAt: Long? = null,
    val checkedInBy: String? = null,
    val checkedInDeviceId: String? = null,
    val checkedInLocation: String? = null, // JSON: {"lat": x, "lng": y}
    
    // Sync tracking
    val syncStatus: String = SyncStatus.SYNCED.name,
    val lastModified: Long = System.currentTimeMillis(),
    val localVersion: Int = 1,
    val serverVersion: Int? = null,
    val syncError: String? = null,
    val retryCount: Int = 0,
    
    // Offline check-in specific
    val offlineCheckIn: Boolean = false,
    val offlineCheckInTime: Long? = null
) {
    /**
     * Check if this ticket can be checked in offline
     */
    fun canCheckInOffline(): Boolean {
        return !isCheckedIn && status == TicketStatus.ACTIVE.name
    }
    
    /**
     * Create a checked-in version of this ticket
     */
    fun checkIn(
        checkedInBy: String,
        deviceId: String,
        location: String? = null
    ): OfflineTicketEntity {
        val now = System.currentTimeMillis()
        return copy(
            isCheckedIn = true,
            checkedInAt = now,
            checkedInBy = checkedInBy,
            checkedInDeviceId = deviceId,
            checkedInLocation = location,
            status = TicketStatus.USED.name,
            syncStatus = SyncStatus.PENDING.name,
            lastModified = now,
            localVersion = localVersion + 1,
            offlineCheckIn = true,
            offlineCheckInTime = now
        )
    }
}

// ============================================================================
// PENDING SYNC QUEUE
// ============================================================================

/**
 * Pending Sync Item
 * 
 * Queue of operations waiting to be synchronized with the server.
 * Processed by WorkManager when network is available.
 */
@Entity(
    tableName = "pending_sync_queue",
    indices = [
        Index(value = ["entityType", "entityId"]),
        Index(value = ["priority"]),
        Index(value = ["createdAt"])
    ]
)
data class PendingSyncEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entityType: String,        // "ticket", "event", "check_in"
    val entityId: String,          // ID of the entity
    val operation: String,         // CREATE, UPDATE, DELETE, CHECK_IN
    val payload: String,           // JSON payload of the operation
    val priority: Int = 0,         // Higher = more urgent
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val attempts: Int = 0,
    val maxAttempts: Int = 5,
    val lastError: String? = null,
    val syncStatus: String = SyncStatus.PENDING.name,
    val deviceId: String,          // Device that created this operation
    val userId: String             // User who performed the operation
) {
    /**
     * Check if this item can be retried
     */
    fun canRetry(): Boolean = attempts < maxAttempts && syncStatus != SyncStatus.SYNCING.name
    
    /**
     * Increment attempt count
     */
    fun incrementAttempt(error: String? = null): PendingSyncEntity {
        return copy(
            attempts = attempts + 1,
            lastError = error,
            updatedAt = System.currentTimeMillis(),
            syncStatus = if (attempts + 1 >= maxAttempts) SyncStatus.FAILED.name else SyncStatus.PENDING.name
        )
    }
    
    /**
     * Mark as syncing
     */
    fun markSyncing(): PendingSyncEntity {
        return copy(syncStatus = SyncStatus.SYNCING.name, updatedAt = System.currentTimeMillis())
    }
    
    /**
     * Mark as synced
     */
    fun markSynced(): PendingSyncEntity {
        return copy(syncStatus = SyncStatus.SYNCED.name, updatedAt = System.currentTimeMillis())
    }
}

// ============================================================================
// SYNC CONFLICT
// ============================================================================

/**
 * Sync Conflict Entity
 * 
 * Records conflicts between local and server data.
 * Requires manual or automatic resolution.
 */
@Entity(
    tableName = "sync_conflicts",
    indices = [
        Index(value = ["entityType", "entityId"]),
        Index(value = ["resolved"])
    ]
)
data class SyncConflictEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entityType: String,
    val entityId: String,
    val localData: String,         // JSON of local version
    val serverData: String,        // JSON of server version
    val conflictType: String,      // "update_update", "delete_update", etc.
    val detectedAt: Long = System.currentTimeMillis(),
    val resolved: Boolean = false,
    val resolution: String? = null, // "local_wins", "server_wins", "merged"
    val resolvedAt: Long? = null,
    val resolvedData: String? = null, // JSON of resolved data
    val deviceId: String,
    val userId: String
) {
    /**
     * Resolution strategies
     */
    enum class Resolution {
        LOCAL_WINS,    // Keep local version
        SERVER_WINS,   // Accept server version
        MERGED,        // Combined/merged data
        MANUAL         // Requires manual intervention
    }
}

// ============================================================================
// EVENT SNAPSHOT (for offline validation)
// ============================================================================

/**
 * Event Snapshot Entity
 * 
 * Cached event data for offline validation of tickets.
 * Downloaded before event for offline operation.
 */
@Entity(
    tableName = "event_snapshots",
    indices = [
        Index(value = ["eventId"], unique = true),
        Index(value = ["syncedAt"])
    ]
)
data class EventSnapshotEntity(
    @PrimaryKey
    val eventId: String,
    val eventName: String,
    val eventDate: Long,
    val eventStatus: String,
    val ticketData: String,        // JSON: List of valid ticket IDs and QR codes
    val syncedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long,           // When this snapshot becomes stale
    val deviceId: String,
    val version: Int = 1
) {
    /**
     * Check if snapshot is still valid
     */
    fun isValid(): Boolean = System.currentTimeMillis() < expiresAt
    
    /**
     * Check if snapshot is stale (needs refresh)
     */
    fun isStale(): Boolean = System.currentTimeMillis() > (expiresAt - (24 * 60 * 60 * 1000)) // 24 hours before expiry
}

// ============================================================================
// DEVICE REGISTRATION
// ============================================================================

/**
 * Device Registration Entity
 * 
 * Tracks device registration for multi-device sync.
 * Used to identify source of offline operations.
 */
@Entity(
    tableName = "device_registrations"
)
data class DeviceRegistrationEntity(
    @PrimaryKey
    val deviceId: String,
    val deviceName: String,
    val userId: String,
    val registeredAt: Long = System.currentTimeMillis(),
    val lastSyncAt: Long? = null,
    val isActive: Boolean = true,
    val syncToken: String? = null  // Token for incremental sync
)

// ============================================================================
// SYNC METADATA
// ============================================================================

/**
 * Sync Metadata Entity
 * 
 * Tracks last sync times and tokens for each entity type.
 * Used for incremental sync optimization.
 */
@Entity(
    tableName = "sync_metadata"
)
data class SyncMetadataEntity(
    @PrimaryKey
    val entityType: String,
    val lastSyncAt: Long? = null,
    val syncToken: String? = null,
    val syncVersion: Int = 0,
    val entityCount: Int = 0,
    val pendingCount: Int = 0,
    val conflictCount: Int = 0
)

// ============================================================================
// CHECK-IN RECORD (Audit Trail)
// ============================================================================

/**
 * Check-in Record Entity
 * 
 * Immutable record of all check-in attempts (successful and failed).
 * Used for audit trail and analytics.
 */
@Entity(
    tableName = "check_in_records",
    indices = [
        Index(value = ["ticketId"]),
        Index(value = ["eventId"]),
        Index(value = ["checkedInAt"]),
        Index(value = ["deviceId"])
    ]
)
data class CheckInRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ticketId: String,
    val eventId: String,
    val qrCodeScanned: String,
    val success: Boolean,
    val failureReason: String? = null,
    val checkedInAt: Long = System.currentTimeMillis(),
    val checkedInBy: String,
    val deviceId: String,
    val location: String? = null,
    val offlineMode: Boolean = false,
    val syncedAt: Long? = null,
    val syncStatus: String = SyncStatus.PENDING.name
) {
    /**
     * Create a successful check-in record
     */
    companion object {
        fun success(
            ticketId: String,
            eventId: String,
            qrCode: String,
            checkedInBy: String,
            deviceId: String,
            location: String? = null,
            offlineMode: Boolean = false
        ): CheckInRecordEntity {
            return CheckInRecordEntity(
                ticketId = ticketId,
                eventId = eventId,
                qrCodeScanned = qrCode,
                success = true,
                checkedInBy = checkedInBy,
                deviceId = deviceId,
                location = location,
                offlineMode = offlineMode
            )
        }
        
        fun failure(
            ticketId: String?,
            eventId: String,
            qrCode: String,
            reason: String,
            checkedInBy: String,
            deviceId: String,
            location: String? = null
        ): CheckInRecordEntity {
            return CheckInRecordEntity(
                ticketId = ticketId ?: "",
                eventId = eventId,
                qrCodeScanned = qrCode,
                success = false,
                failureReason = reason,
                checkedInBy = checkedInBy,
                deviceId = deviceId,
                location = location,
                offlineMode = false
            )
        }
    }
}
