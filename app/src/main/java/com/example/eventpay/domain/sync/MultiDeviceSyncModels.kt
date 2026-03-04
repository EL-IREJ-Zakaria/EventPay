package com.example.eventpay.domain.sync

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Multi-Device Sync Domain Models
 * 
 * Data structures for coordinating check-ins across multiple devices
 * with real-time synchronization and conflict prevention.
 */

// ============================================================================
// SYNC STATE MODELS
// ============================================================================

/**
 * Represents the sync state of a device
 */
@Serializable
data class DeviceSyncState(
    val deviceId: String,
    val deviceName: String,
    val staffId: String,
    val staffName: String,
    val eventId: String,
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val syncVersion: Long = 0,
    val connectionStatus: ConnectionStatus = ConnectionStatus.ONLINE,
    val pendingOperations: Int = 0,
    val lastHeartbeat: Long = System.currentTimeMillis(),
    val batteryLevel: Int = 100,
    val networkType: String? = null
)

/**
 * Connection status of a device
 */
enum class ConnectionStatus {
    ONLINE,         // Connected and syncing
    OFFLINE,        // Disconnected, working locally
    RECONNECTING,   // Attempting to reconnect
    SYNCING,        // Currently syncing data
    ERROR           // Connection error
}

/**
 * Represents the global sync state for an event
 */
@Serializable
data class EventSyncState(
    val eventId: String,
    val globalVersion: Long = 0,
    val lastModified: Long = System.currentTimeMillis(),
    val activeDevices: List<DeviceSyncState> = emptyList(),
    val totalCheckIns: Int = 0,
    val pendingConflicts: Int = 0,
    val syncMode: SyncMode = SyncMode.REAL_TIME
)

/**
 * Sync mode configuration
 */
enum class SyncMode {
    REAL_TIME,      // Immediate sync on every operation
    BATCH,          // Batch sync at intervals
    HYBRID          // Real-time for critical, batch for others
}

// ============================================================================
// SYNC OPERATION MODELS
// ============================================================================

/**
 * Represents a sync operation to be propagated
 */
@Serializable
data class SyncOperation(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val deviceId: String,
    val staffId: String,
    val eventId: String,
    val operationType: SyncOperationType,
    val entityType: SyncEntityType,
    val entityId: String,
    val payload: String,            // JSON serialized data
    val version: Long,
    val checksum: String,
    val status: SyncOperationStatus = SyncOperationStatus.PENDING,
    val retryCount: Int = 0,
    val errorMessage: String? = null,
    val dependencies: List<String> = emptyList()  // IDs of operations that must complete first
)

/**
 * Types of sync operations
 */
enum class SyncOperationType {
    CHECK_IN,           // Ticket check-in
    CHECK_IN_REVERSE,   // Reverse a check-in
    TICKET_UPDATE,      // Update ticket data
    SHIFT_START,        // Start staff shift
    SHIFT_END,          // End staff shift
    SYNC_REQUEST,       // Request full sync
    SYNC_RESPONSE,      // Full sync response
    HEARTBEAT,          // Device heartbeat
    CONFLICT_RESOLVE,   // Conflict resolution
    STATE_SYNC          // Full state synchronization
}

/**
 * Entity types for sync
 */
enum class SyncEntityType {
    TICKET,
    EVENT,
    SHIFT,
    STAFF,
    DEVICE_STATE
}

/**
 * Status of a sync operation
 */
enum class SyncOperationStatus {
    PENDING,            // Waiting to be processed
    PROCESSING,         // Currently being processed
    COMPLETED,          // Successfully completed
    FAILED,             // Failed to complete
    CONFLICT,           // Conflict detected
    RETRYING,           // Retrying after failure
    CANCELLED           // Operation cancelled
}

// ============================================================================
// CONFLICT MODELS
// ============================================================================

/**
 * Represents a sync conflict
 */
@Serializable
data class SyncConflict(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val eventId: String,
    val entityType: SyncEntityType,
    val entityId: String,
    val conflictType: ConflictType,
    val localOperation: SyncOperation,
    val remoteOperation: SyncOperation,
    val localDeviceId: String,
    val remoteDeviceId: String,
    val status: ConflictStatus = ConflictStatus.DETECTED,
    val resolution: ConflictResolution? = null,
    val resolvedAt: Long? = null,
    val resolvedBy: String? = null
)

/**
 * Types of conflicts
 */
enum class ConflictType {
    CONCURRENT_CHECK_IN,        // Two devices checking in same ticket
    VERSION_MISMATCH,           // Version numbers don't match
    DATA_DIVERGENCE,            // Same entity, different data
    DELETE_MODIFY,              // One deleted, other modified
    DOUBLE_CHECK_IN,            // Ticket already checked in elsewhere
    SHIFT_OVERLAP,              // Overlapping shift times
    PERMISSION_CONFLICT         // Permission level conflict
}

/**
 * Status of a conflict
 */
enum class ConflictStatus {
    DETECTED,           // Conflict just detected
    ANALYZING,          // Being analyzed for resolution
    PENDING_RESOLUTION, // Waiting for resolution
    RESOLVED,           // Successfully resolved
    ESCALATED,          // Escalated to admin
    TIMEOUT             // Resolution timed out
}

/**
 * Conflict resolution strategy
 */
@Serializable
sealed class ConflictResolution {
    abstract val resolutionId: String
    abstract val timestamp: Long
    abstract val automated: Boolean
    
    /**
     * Use the operation that came first (by timestamp)
     */
    @Serializable
    data class FirstWins(
        override val resolutionId: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        override val automated: Boolean = true,
        val winningOperationId: String
    ) : ConflictResolution()
    
    /**
     * Use the operation that came last (by timestamp)
     */
    @Serializable
    data class LastWins(
        override val resolutionId: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        override val automated: Boolean = true,
        val winningOperationId: String
    ) : ConflictResolution()
    
    /**
     * Merge both operations
     */
    @Serializable
    data class Merge(
        override val resolutionId: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        override val automated: Boolean = true,
        val mergedPayload: String,
        val sourceOperations: List<String>
    ) : ConflictResolution()
    
    /**
     * Manual resolution by admin
     */
    @Serializable
    data class ManualResolution(
        override val resolutionId: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        override val automated: Boolean = false,
        val resolvedBy: String,
        val resolution: String,
        val finalOperationId: String
    ) : ConflictResolution()
    
    /**
     * Reject both operations
     */
    @Serializable
    data class RejectBoth(
        override val resolutionId: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        override val automated: Boolean = true,
        val reason: String
    ) : ConflictResolution()
}

// ============================================================================
// CHECK-IN SYNC MODELS
// ============================================================================

/**
 * Represents a check-in operation with sync metadata
 */
@Serializable
data class SyncableCheckIn(
    val id: String = UUID.randomUUID().toString(),
    val ticketId: String,
    val eventId: String,
    val userId: String,
    val checkedInBy: String,
    val deviceId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val location: GeoLocation? = null,
    val syncStatus: CheckInSyncStatus = CheckInSyncStatus.LOCAL,
    val version: Long = 1,
    val serverTimestamp: Long? = null,
    val conflictId: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Geographic location
 */
@Serializable
data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Sync status of a check-in
 */
enum class CheckInSyncStatus {
    LOCAL,              // Only on local device
    PENDING_SYNC,       // Queued for sync
    SYNCING,            // Currently syncing
    SYNCED,             // Synced to server
    CONFLICT,           // Has conflict
    REJECTED,           // Rejected by server
    OFFLINE_QUEUED      // Queued due to offline mode
}

// ============================================================================
// REAL-TIME SYNC MODELS
// ============================================================================

/**
 * Real-time sync message
 */
@Serializable
data class SyncMessage(
    val id: String = UUID.randomUUID().toString(),
    val type: SyncMessageType,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceDeviceId: String,
    val targetDeviceId: String? = null,  // null for broadcast
    val eventId: String,
    val payload: String,
    val requiresAck: Boolean = false,
    val ackTimeout: Long? = null,
    val priority: SyncPriority = SyncPriority.NORMAL
)

/**
 * Types of sync messages
 */
enum class SyncMessageType {
    // Operations
    CHECK_IN_BROADCAST,     // Broadcast check-in to all devices
    CHECK_IN_ACK,           // Acknowledge check-in received
    STATE_UPDATE,           // State update notification
    FULL_SYNC_REQUEST,      // Request full sync
    FULL_SYNC_RESPONSE,     // Full sync data
    
    // Presence
    DEVICE_JOIN,            // Device joined event
    DEVICE_LEAVE,           // Device left event
    HEARTBEAT,              // Heartbeat ping
    HEARTBEAT_ACK,          // Heartbeat acknowledgment
    
    // Conflicts
    CONFLICT_NOTIFY,        // Notify of conflict
    CONFLICT_RESOLVE,       // Conflict resolution
    
    // Control
    LOCK_REQUEST,           // Request lock on entity
    LOCK_GRANTED,           // Lock granted
    LOCK_DENIED,            // Lock denied
    LOCK_RELEASE,           // Release lock
    
    // Status
    ERROR,                  // Error message
    RETRY,                  // Retry request
    CANCEL                  // Cancel operation
}

/**
 * Priority levels for sync messages
 */
enum class SyncPriority {
    LOW,        // Can be delayed
    NORMAL,     // Normal priority
    HIGH,       // Should be processed quickly
    CRITICAL    // Must be processed immediately
}

// ============================================================================
// LOCK MODELS
// ============================================================================

/**
 * Distributed lock for preventing concurrent modifications
 */
@Serializable
data class DistributedLock(
    val lockId: String = UUID.randomUUID().toString(),
    val entityType: SyncEntityType,
    val entityId: String,
    val eventId: String,
    val lockedBy: String,           // Device ID
    val staffId: String,
    val lockedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long,
    val status: LockStatus = LockStatus.ACTIVE,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Lock status
 */
enum class LockStatus {
    ACTIVE,         // Lock is active
    RELEASED,       // Lock was released
    EXPIRED,        // Lock expired
    BROKEN          // Lock was broken by admin
}

// ============================================================================
// SYNC STATISTICS
// ============================================================================

/**
 * Sync statistics for monitoring
 */
@Serializable
data class SyncStatistics(
    val eventId: String,
    val periodStart: Long,
    val periodEnd: Long,
    val totalOperations: Int = 0,
    val successfulSyncs: Int = 0,
    val failedSyncs: Int = 0,
    val conflictsDetected: Int = 0,
    val conflictsResolved: Int = 0,
    val averageLatency: Double = 0.0,
    val maxLatency: Long = 0,
    val minLatency: Long = Long.MAX_VALUE,
    val offlineOperations: Int = 0,
    val reconnections: Int = 0,
    val bytesTransferred: Long = 0,
    val deviceStats: Map<String, DeviceSyncStats> = emptyMap()
)

/**
 * Per-device sync statistics
 */
@Serializable
data class DeviceSyncStats(
    val deviceId: String,
    val operationsSubmitted: Int = 0,
    val operationsCompleted: Int = 0,
    val operationsFailed: Int = 0,
    val conflictsCaused: Int = 0,
    val averageLatency: Double = 0.0,
    val uptime: Long = 0,
    val offlineTime: Long = 0
)

// ============================================================================
// SYNC CONFIGURATION
// ============================================================================

/**
 * Configuration for sync behavior
 */
@Serializable
data class SyncConfiguration(
    val syncMode: SyncMode = SyncMode.HYBRID,
    val heartbeatInterval: Long = 5000,             // 5 seconds
    val lockTimeout: Long = 30000,                  // 30 seconds
    val lockExtension: Long = 15000,                // 15 seconds
    val maxRetries: Int = 3,
    val retryDelay: Long = 1000,                    // 1 second
    val batchSize: Int = 50,
    val batchInterval: Long = 2000,                 // 2 seconds
    val conflictTimeout: Long = 60000,              // 1 minute
    val offlineQueueLimit: Int = 1000,
    val compressionEnabled: Boolean = true,
    val encryptionEnabled: Boolean = true,
    val priorityOperations: List<SyncOperationType> = listOf(
        SyncOperationType.CHECK_IN,
        SyncOperationType.CONFLICT_RESOLVE
    )
)

// ============================================================================
// SYNC RESULT MODELS
// ============================================================================

/**
 * Result of a sync operation
 */
@Serializable
data class SyncResult(
    val success: Boolean,
    val operationId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val newVersion: Long? = null,
    val conflict: SyncConflict? = null,
    val error: SyncError? = null,
    val requiresRetry: Boolean = false,
    val retryDelay: Long? = null
)

/**
 * Sync error details
 */
@Serializable
data class SyncError(
    val code: SyncErrorCode,
    val message: String,
    val details: String? = null,
    val recoverable: Boolean = false
)

/**
 * Sync error codes
 */
enum class SyncErrorCode {
    NETWORK_ERROR,
    TIMEOUT,
    VERSION_MISMATCH,
    CONFLICT_DETECTED,
    PERMISSION_DENIED,
    ENTITY_NOT_FOUND,
    INVALID_OPERATION,
    SERVER_ERROR,
    LOCK_NOT_GRANTED,
    QUOTA_EXCEEDED,
    VALIDATION_ERROR
}

// ============================================================================
// BATCH SYNC MODELS
// ============================================================================

/**
 * Batch of sync operations
 */
@Serializable
data class SyncBatch(
    val batchId: String = UUID.randomUUID().toString(),
    val eventId: String,
    val deviceId: String,
    val operations: List<SyncOperation>,
    val createdAt: Long = System.currentTimeMillis(),
    val status: BatchStatus = BatchStatus.PENDING,
    val processedAt: Long? = null,
    val results: List<SyncResult> = emptyList()
)

/**
 * Status of a sync batch
 */
enum class BatchStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    PARTIAL_FAILURE,
    FAILED,
    CANCELLED
}
