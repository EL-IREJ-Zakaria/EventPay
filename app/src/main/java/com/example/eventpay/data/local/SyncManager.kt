package com.example.eventpay.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.eventpay.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * Pending Sync Operation Entity
 * 
 * Tracks operations that need to be synchronized with the server.
 * Used for offline support - operations are queued when offline
 * and processed when connectivity is restored.
 */
@Entity(tableName = "pending_sync")
data class PendingSyncOperation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entityType: String,        // "event", "ticket", "transaction", "user"
    val entityId: String,          // ID of the entity
    val operationType: String,     // "create", "update", "delete"
    val payload: String,           // JSON serialized entity data
    val createdAt: Long = System.currentTimeMillis(),
    val attempts: Int = 0,
    val lastAttemptAt: Long? = null,
    val errorMessage: String? = null
) {
    fun incrementAttempt(): PendingSyncOperation = copy(
        attempts = attempts + 1,
        lastAttemptAt = System.currentTimeMillis()
    )
    
    fun withError(message: String): PendingSyncOperation = copy(
        errorMessage = message
    )
    
    fun clearError(): PendingSyncOperation = copy(
        errorMessage = null
    )
}

/**
 * Sync Operation Types
 */
object SyncOperationType {
    const val CREATE = "create"
    const val UPDATE = "update"
    const val DELETE = "delete"
}

/**
 * Sync Entity Types
 */
object SyncEntityType {
    const val EVENT = "event"
    const val TICKET = "ticket"
    const val TRANSACTION = "transaction"
    const val USER = "user"
}

/**
 * Sync Status for UI display
 */
data class SyncStatusInfo(
    val isSyncing: Boolean = false,
    val pendingOperations: Int = 0,
    val lastSyncTime: Long? = null,
    val syncError: String? = null,
    val isOnline: Boolean = true
) {
    fun hasPendingChanges(): Boolean = pendingOperations > 0
    fun needsSync(): Boolean = hasPendingChanges() && isOnline && !isSyncing
}
