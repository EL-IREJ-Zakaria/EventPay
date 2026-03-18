package com.example.eventpay.data.sync

import android.content.Context
import androidx.work.*
import com.example.eventpay.data.local.dao.*
import com.example.eventpay.data.local.entity.*
import com.example.eventpay.util.NetworkUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-First Sync Manager
 * 
 * Coordinates synchronization between local database and remote server.
 * Implements conflict resolution strategies for offline check-ins.
 * 
 * Sync Strategy:
 * 1. Local-first: All operations succeed locally immediately
 * 2. Queue: Operations are queued for sync when network available
 * 3. Retry: Failed syncs are retried with exponential backoff
 * 4. Conflict Resolution: Automatic or manual based on conflict type
 */
@Singleton
class OfflineSyncManager @Inject constructor(
    private val ticketDao: OfflineTicketDao,
    private val pendingSyncDao: PendingSyncQueueDao,
    private val conflictDao: SyncConflictDao,
    private val snapshotDao: EventSnapshotDao,
    private val recordDao: CheckInRecordDao,
    private val metadataDao: SyncMetadataDao,
    private val deviceDao: DeviceRegistrationDao,
    private val networkUtils: NetworkUtils,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    companion object {
        const val SYNC_WORK_NAME = "offline_sync_work"
        const val MAX_RETRY_ATTEMPTS = 5
        const val SYNC_BATCH_SIZE = 50
    }
    
    // Sync state flow
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    // Pending count flow
    val pendingCount: Flow<Int> = pendingSyncDao.getAllPending()
        .map { items -> items.count { item -> item.syncStatus == "PENDING" } }
    
    // Conflict count flow
    val conflictCount: Flow<Int> = conflictDao.getUnresolvedConflicts()
        .map { it.size }
    
    /**
     * Queue a check-in operation for sync
     */
    suspend fun queueCheckIn(
        ticket: OfflineTicketEntity,
        deviceId: String,
        userId: String
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val payload = json.encodeToString(
                CheckInPayload(
                    ticketId = ticket.id,
                    eventId = ticket.eventId,
                    checkedInAt = ticket.checkedInAt!!,
                    checkedInBy = ticket.checkedInBy!!,
                    deviceId = deviceId,
                    location = ticket.checkedInLocation
                )
            )
            
            val pendingItem = PendingSyncEntity(
                entityType = "ticket",
                entityId = ticket.id,
                operation = SyncOperation.CHECK_IN.name,
                payload = payload,
                priority = 10, // High priority for check-ins
                deviceId = deviceId,
                userId = userId
            )
            
            val id = pendingSyncDao.insert(pendingItem)
            metadataDao.incrementPending("ticket")
            
            // Try immediate sync if online
            if (networkUtils.isOnline()) {
                triggerSync()
            }
            
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Process pending sync items
     */
    suspend fun processPendingSync(): SyncResult = withContext(Dispatchers.IO) {
        if (!networkUtils.isOnline()) {
            return@withContext SyncResult.Offline
        }
        
        _syncState.value = SyncState.Syncing
        
        try {
            val pendingItems = pendingSyncDao.getPendingItems(SYNC_BATCH_SIZE)
            var successCount = 0
            var failureCount = 0
            var conflictCount = 0
            
            for (item in pendingItems) {
                when (val result = processSyncItem(item)) {
                    is SyncItemResult.Success -> {
                        pendingSyncDao.markSynced(item.id)
                        successCount++
                    }
                    is SyncItemResult.Failure -> {
                        pendingSyncDao.markFailed(item.id, result.error)
                        failureCount++
                    }
                    is SyncItemResult.Conflict -> {
                        pendingSyncDao.markConflict(item.id)
                        conflictCount++
                    }
                }
            }
            
            _syncState.value = SyncState.Idle
            
            SyncResult.Completed(
                successCount = successCount,
                failureCount = failureCount,
                conflictCount = conflictCount
            )
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Unknown error")
            SyncResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Process a single sync item
     */
    private suspend fun processSyncItem(item: PendingSyncEntity): SyncItemResult {
        return try {
            when (SyncOperation.valueOf(item.operation)) {
                SyncOperation.CHECK_IN -> processCheckInSync(item)
                SyncOperation.CREATE -> processCreateSync(item)
                SyncOperation.UPDATE -> processUpdateSync(item)
                SyncOperation.DELETE -> processDeleteSync(item)
            }
        } catch (e: Exception) {
            SyncItemResult.Failure(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Process check-in sync with conflict detection
     */
    private suspend fun processCheckInSync(item: PendingSyncEntity): SyncItemResult {
        val payload = json.decodeFromString<CheckInPayload>(item.payload)
        
        // Check for conflicts with server
        val serverTicket = fetchServerTicket(payload.ticketId)
        
        if (serverTicket != null && serverTicket.isCheckedIn) {
            // Conflict: ticket already checked in on server
            if (serverTicket.checkedInAt != payload.checkedInAt) {
                // Different check-in time = conflict
                return handleCheckInConflict(item, payload, serverTicket)
            }
            // Same check-in = already synced, mark success
            return SyncItemResult.Success
        }
        
        // No conflict, proceed with sync
        return try {
            val success = pushCheckInToServer(payload)
            if (success) {
                ticketDao.markSynced(payload.ticketId, 1)
                SyncItemResult.Success
            } else {
                SyncItemResult.Failure("Failed to push check-in to server")
            }
        } catch (e: Exception) {
            SyncItemResult.Failure(e.message ?: "Network error")
        }
    }
    
    /**
     * Handle check-in conflict
     */
    private suspend fun handleCheckInConflict(
        item: PendingSyncEntity,
        localPayload: CheckInPayload,
        serverTicket: ServerTicketInfo
    ): SyncItemResult {
        // Create conflict record
        val conflict = SyncConflictEntity(
            entityType = "ticket",
            entityId = localPayload.ticketId,
            localData = json.encodeToString(localPayload),
            serverData = json.encodeToString(serverTicket),
            conflictType = "check_in_conflict",
            deviceId = item.deviceId,
            userId = item.userId
        )
        
        conflictDao.insert(conflict)
        metadataDao.incrementConflict("ticket")
        
        // Apply conflict resolution strategy
        val resolution = resolveCheckInConflict(localPayload, serverTicket)
        
        return when (resolution) {
            is ConflictResolution.LocalWins -> {
                // Force local check-in to server
                pushCheckInToServer(localPayload, force = true)
                SyncItemResult.Success
            }
            is ConflictResolution.ServerWins -> {
                // Accept server state
                ticketDao.updateTicket(
                    ticketDao.getTicketById(localPayload.ticketId)!!.copy(
                        checkedInAt = serverTicket.checkedInAt,
                        checkedInBy = serverTicket.checkedInBy,
                        syncStatus = SyncStatus.SYNCED.name
                    )
                )
                SyncItemResult.Success
            }
            is ConflictResolution.Manual -> {
                SyncItemResult.Conflict
            }
        }
    }
    
    /**
     * Resolve check-in conflict based on business rules
     */
    private fun resolveCheckInConflict(
        local: CheckInPayload,
        server: ServerTicketInfo
    ): ConflictResolution {
        // Rule 1: If both check-ins are within 5 minutes, accept earlier one
        val timeDiff = kotlin.math.abs(local.checkedInAt - (server.checkedInAt ?: 0))
        if (timeDiff < 5 * 60 * 1000) {
            // Accept the earlier check-in
            return if ((local.checkedInAt) < (server.checkedInAt ?: Long.MAX_VALUE)) {
                ConflictResolution.LocalWins("Earlier check-in time")
            } else {
                ConflictResolution.ServerWins("Earlier check-in time on server")
            }
        }
        
        // Rule 2: If significant time difference, require manual resolution
        return ConflictResolution.Manual(resolvedData = null)
    }
    
    /**
     * Pull updates from server
     */
    suspend fun pullFromServer(eventId: String): PullResult = withContext(Dispatchers.IO) {
        if (!networkUtils.isOnline()) {
            return@withContext PullResult.Offline
        }
        
        try {
            // Get last sync token
            val metadata = metadataDao.getMetadata("ticket")
            val lastSyncToken = metadata?.syncToken
            
            // Fetch updates from server
            val updates = fetchServerUpdates(eventId, lastSyncToken)
            
            // Apply updates to local database
            for (ticket in updates.tickets) {
                val localTicket = ticketDao.getTicketById(ticket.id)
                
                if (localTicket == null) {
                    // New ticket, insert
                    ticketDao.insertTicket(ticket.toOfflineEntity())
                } else {
                    // Check for conflict
                    if (localTicket.syncStatus == SyncStatus.PENDING.name) {
                        // Local has pending changes, check for conflict
                        if (ticket.version > (localTicket.serverVersion ?: 0)) {
                            // Server has newer version
                            handlePullConflict(localTicket, ticket)
                        }
                    } else {
                        // No local changes, accept server version
                        ticketDao.insertTicket(ticket.toOfflineEntity())
                    }
                }
            }
            
            // Update sync metadata
            metadataDao.updateSyncState(
                entityType = "ticket",
                timestamp = System.currentTimeMillis(),
                token = updates.syncToken,
                version = updates.version,
                entityCount = updates.tickets.size,
                pendingCount = pendingSyncDao.getPendingCount(),
                conflictCount = conflictDao.getUnresolvedCount()
            )
            
            
            PullResult.Success(updates.tickets.size)
        } catch (e: Exception) {
            PullResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Handle conflict during pull
     */
    private suspend fun handlePullConflict(
        local: OfflineTicketEntity,
        server: TicketServerData
    ) {
        val conflict = SyncConflictEntity(
            entityType = "ticket",
            entityId = local.id,
            localData = json.encodeToString(local),
            serverData = json.encodeToString(server),
            conflictType = "update_update",
            deviceId = local.checkedInDeviceId ?: "",
            userId = local.userId
        )
        
        conflictDao.insert(conflict)
        metadataDao.incrementConflict("ticket")
        
        // Auto-resolve based on version
        if (server.version > (local.serverVersion ?: 0)) {
            // Server wins for non-check-in fields
            val resolved = local.copy(
                status = server.status,
                serverVersion = server.version,
                syncStatus = SyncStatus.CONFLICT.name
            )
            ticketDao.updateTicket(resolved)
        }
    }
    
    /**
     * Prepare event for offline operation
     * Downloads all tickets for the event
     */
    suspend fun prepareEventForOffline(
        eventId: String,
        deviceId: String
    ): PrepareResult = withContext(Dispatchers.IO) {
        try {
            // Check if already have valid snapshot
            if (snapshotDao.hasValidSnapshot(eventId)) {
                return@withContext PrepareResult.AlreadyPrepared
            }
            
            if (!networkUtils.isOnline()) {
                return@withContext PrepareResult.Offline
            }
            
            // Fetch all tickets for event
            val tickets = fetchEventTickets(eventId)
            
            // Save tickets locally
            ticketDao.insertTickets(tickets.map { it.toOfflineEntity() })
            
            // Create event snapshot
            val snapshot = EventSnapshotEntity(
                eventId = eventId,
                eventName = tickets.firstOrNull()?.eventName ?: "",
                eventDate = 0, // Would be fetched from event
                eventStatus = "ONGOING",
                ticketData = json.encodeToString(tickets.map { it.id to it.qrCode }),
                expiresAt = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000), // 7 days
                deviceId = deviceId
            )
            
            snapshotDao.insert(snapshot)
            
            PrepareResult.Success(tickets.size)
        } catch (e: Exception) {
            PrepareResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Trigger immediate sync
     */
    suspend fun triggerSync() {
        processPendingSync()
    }
    
    /**
     * Resolve a conflict manually
     */
    suspend fun resolveConflict(
        conflictId: Long,
        resolution: ConflictResolution
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val conflict = conflictDao.getById(conflictId) ?: return@withContext Result.failure(
                Exception("Conflict not found")
            )
            
            when (resolution) {
                is ConflictResolution.LocalWins -> {
                    // Push local data to server
                    val localData = json.decodeFromString<CheckInPayload>(conflict.localData)
                    pushCheckInToServer(localData, force = true)
                }
                is ConflictResolution.ServerWins -> {
                    // Accept server data
                    val serverData = json.decodeFromString<TicketServerData>(conflict.serverData)
                    val local = ticketDao.getTicketById(conflict.entityId)
                    local?.let {
                        ticketDao.updateTicket(it.copy(
                            checkedInAt = serverData.checkedInAt,
                            checkedInBy = serverData.checkedInBy,
                            syncStatus = SyncStatus.SYNCED.name
                        ))
                    }
                }
                is ConflictResolution.Manual -> {
                    // Apply custom resolution
                    resolution.resolvedData?.let { data ->
                        val local = ticketDao.getTicketById(conflict.entityId)
                        local?.let {
                            ticketDao.updateTicket(it.copy(
                                checkedInAt = data.checkedInAt,
                                checkedInBy = data.checkedInBy,
                                syncStatus = SyncStatus.PENDING.name
                            ))
                        }
                    }
                }
            }
            
            conflictDao.resolve(conflictId, resolution.javaClass.simpleName, null)
            metadataDao.decrementPending("ticket")
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Placeholder methods for server communication
    private suspend fun fetchServerTicket(ticketId: String): ServerTicketInfo? {
        // Would implement actual API call
        return null
    }
    
    private suspend fun pushCheckInToServer(payload: CheckInPayload, force: Boolean = false): Boolean {
        // Would implement actual API call
        return true
    }
    
    private suspend fun fetchServerUpdates(eventId: String, syncToken: String?): ServerUpdates {
        // Would implement actual API call
        return ServerUpdates(emptyList(), null, 0)
    }
    
    private suspend fun fetchEventTickets(eventId: String): List<TicketServerData> {
        // Would implement actual API call
        return emptyList()
    }
    
    private suspend fun processCreateSync(item: PendingSyncEntity): SyncItemResult = SyncItemResult.Success
    private suspend fun processUpdateSync(item: PendingSyncEntity): SyncItemResult = SyncItemResult.Success
    private suspend fun processDeleteSync(item: PendingSyncEntity): SyncItemResult = SyncItemResult.Success
}

// ============================================================================
// DATA CLASSES
// ============================================================================

@kotlinx.serialization.Serializable
data class CheckInPayload(
    val ticketId: String,
    val eventId: String,
    val checkedInAt: Long,
    val checkedInBy: String,
    val deviceId: String,
    val location: String? = null
)

@kotlinx.serialization.Serializable
data class TicketServerData(
    val id: String,
    val eventId: String,
    val userId: String,
    val status: String,
    val isCheckedIn: Boolean,
    val checkedInAt: Long? = null,
    val checkedInBy: String? = null,
    val version: Int,
    val qrCode: String,
    val eventName: String = "",
    val ticketType: String = "GENERAL",
    val price: Double = 0.0,
    val reservationDate: Long = System.currentTimeMillis()
) {
    fun toOfflineEntity(): OfflineTicketEntity = OfflineTicketEntity(
        id = id,
        eventId = eventId,
        userId = userId,
        status = status,
        isCheckedIn = isCheckedIn,
        checkedInAt = checkedInAt,
        checkedInBy = checkedInBy,
        qrCode = qrCode,
        serverVersion = version,
        syncStatus = SyncStatus.SYNCED.name,
        ticketType = ticketType,
        price = price,
        reservationDate = reservationDate
    )
}

data class ServerTicketInfo(
    val id: String,
    val isCheckedIn: Boolean,
    val checkedInAt: Long? = null,
    val checkedInBy: String? = null,
    val version: Int
)

data class ServerUpdates(
    val tickets: List<TicketServerData>,
    val syncToken: String?,
    val version: Int
)

// ============================================================================
// SYNC STATE
// ============================================================================

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Error(val message: String) : SyncState()
}

sealed class SyncResult {
    object Offline : SyncResult()
    data class Completed(
        val successCount: Int,
        val failureCount: Int,
        val conflictCount: Int
    ) : SyncResult()
    data class Error(val message: String) : SyncResult()
}

sealed class SyncItemResult {
    object Success : SyncItemResult()
    data class Failure(val error: String) : SyncItemResult()
    object Conflict : SyncItemResult()
}

sealed class PullResult {
    object Offline : PullResult()
    data class Success(val count: Int) : PullResult()
    data class Error(val message: String) : PullResult()
}

sealed class PrepareResult {
    object Offline : PrepareResult()
    object AlreadyPrepared : PrepareResult()
    data class Success(val count: Int) : PrepareResult()
    data class Error(val message: String) : PrepareResult()
}

sealed class ConflictResolution {
    data class LocalWins(val reason: String = "") : ConflictResolution()
    data class ServerWins(val reason: String = "") : ConflictResolution()
    data class Manual(val resolvedData: CheckInPayload? = null) : ConflictResolution()
}
