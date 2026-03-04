package com.example.eventpay.domain.usecase.offline

import com.example.eventpay.data.local.dao.*
import com.example.eventpay.data.local.entity.*
import com.example.eventpay.data.sync.OfflineSyncManager
import com.example.eventpay.domain.model.CheckInResult
import com.example.eventpay.domain.model.Event
import com.example.eventpay.domain.model.Ticket
import com.example.eventpay.util.NetworkUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline Check-In Use Cases
 * 
 * Provides offline-first check-in functionality with automatic sync.
 */

/**
 * Use Case: Offline Check-In
 * 
 * Performs check-in operation that works both online and offline.
 * - Online: Immediate sync with server
 * - Offline: Local check-in with queued sync
 */
class OfflineCheckInUseCase @Inject constructor(
    private val ticketDao: OfflineTicketDao,
    private val recordDao: CheckInRecordDao,
    private val syncManager: OfflineSyncManager,
    private val networkUtils: NetworkUtils
) {
    /**
     * Perform offline-capable check-in
     */
    suspend operator fun invoke(
        qrCode: String,
        eventId: String,
        checkedInBy: String,
        deviceId: String,
        location: String? = null
    ): OfflineCheckInResult {
        // Step 1: Find ticket in local database
        val ticket = ticketDao.getTicketByQRCode(qrCode)
            ?: return OfflineCheckInResult.TicketNotFound(qrCode)
        
        // Step 2: Validate ticket
        if (ticket.eventId != eventId) {
            return OfflineCheckInResult.WrongEvent(
                ticketEventId = ticket.eventId,
                scannedEventId = eventId
            )
        }
        
        if (ticket.isCheckedIn) {
            return OfflineCheckInResult.AlreadyCheckedIn(
                ticketId = ticket.id,
                checkedInAt = ticket.checkedInAt!!
            )
        }
        
        if (ticket.status != "ACTIVE") {
            return OfflineCheckInResult.InvalidTicket(
                ticketId = ticket.id,
                reason = "Ticket status: ${ticket.status}"
            )
        }
        
        // Step 3: Perform check-in locally
        val now = System.currentTimeMillis()
        val isOffline = !networkUtils.isOnline()
        
        val updatedRows = ticketDao.checkInTicket(
            ticketId = ticket.id,
            checkedInAt = now,
            checkedInBy = checkedInBy,
            deviceId = deviceId,
            location = location
        )
        
        if (updatedRows == 0) {
            return OfflineCheckInResult.Error("Failed to update ticket")
        }
        
        // Step 4: Create check-in record
        val record = CheckInRecordEntity.success(
            ticketId = ticket.id,
            eventId = eventId,
            qrCode = qrCode,
            checkedInBy = checkedInBy,
            deviceId = deviceId,
            location = location,
            offlineMode = isOffline
        )
        recordDao.insert(record)
        
        // Step 5: Queue for sync
        val updatedTicket = ticketDao.getTicketById(ticket.id)!!
        syncManager.queueCheckIn(updatedTicket, deviceId, checkedInBy)
        
        return OfflineCheckInResult.Success(
            ticketId = ticket.id,
            checkedInAt = now,
            isOfflineMode = isOffline,
            pendingSync = isOffline
        )
    }
}

/**
 * Use Case: Prepare Event for Offline
 * 
 * Downloads all event data for offline operation.
 * Should be called before the event starts.
 */
class PrepareEventOfflineUseCase @Inject constructor(
    private val syncManager: OfflineSyncManager,
    private val snapshotDao: EventSnapshotDao
) {
    suspend operator fun invoke(
        eventId: String,
        deviceId: String
    ): PrepareResult {
        // Check if already prepared
        val existingSnapshot = snapshotDao.getValidSnapshot(eventId)
        if (existingSnapshot != null) {
            return PrepareResult.AlreadyPrepared(
                expiresAt = existingSnapshot.expiresAt
            )
        }
        
        return when (val result = syncManager.prepareEventForOffline(eventId, deviceId)) {
            is com.example.eventpay.data.sync.PrepareResult.Success -> 
                PrepareResult.Success(ticketCount = result.count)
            is com.example.eventpay.data.sync.PrepareResult.Offline -> 
                PrepareResult.Offline
            is com.example.eventpay.data.sync.PrepareResult.AlreadyPrepared -> 
                PrepareResult.AlreadyPrepared(Long.MAX_VALUE)
            is com.example.eventpay.data.sync.PrepareResult.Error -> 
                PrepareResult.Error(result.message)
        }
    }
}

/**
 * Use Case: Get Offline Status
 * 
 * Returns the current offline sync status.
 */
class GetOfflineStatusUseCase @Inject constructor(
    private val pendingSyncDao: PendingSyncDao,
    private val conflictDao: SyncConflictDao,
    private val snapshotDao: EventSnapshotDao,
    private val syncManager: OfflineSyncManager
) {
    operator fun invoke(): Flow<OfflineStatus> {
        return kotlinx.coroutines.flow.combine(
            syncManager.syncState,
            syncManager.pendingCount,
            syncManager.conflictCount
        ) { syncState, pendingCount, conflictCount ->
            OfflineStatus(
                syncState = when (syncState) {
                    is com.example.eventpay.data.sync.SyncState.Idle -> SyncState.IDLE
                    is com.example.eventpay.data.sync.SyncState.Syncing -> SyncState.SYNCING
                    is com.example.eventpay.data.sync.SyncState.Error -> SyncState.ERROR
                },
                pendingSyncCount = pendingCount,
                conflictCount = conflictCount
            )
        }
    }
    
    suspend fun isEventPrepared(eventId: String): Boolean {
        return snapshotDao.hasValidSnapshot(eventId)
    }
}

/**
 * Use Case: Resolve Conflict
 * 
 * Manually resolves a sync conflict.
 */
class ResolveConflictUseCase @Inject constructor(
    private val syncManager: OfflineSyncManager
) {
    suspend operator fun invoke(
        conflictId: Long,
        resolution: ConflictResolutionChoice
    ): Result<Unit> {
        val resolutionStrategy = when (resolution) {
            ConflictResolutionChoice.KEEP_LOCAL -> 
                com.example.eventpay.data.sync.ConflictResolution.LocalWins()
            ConflictResolutionChoice.KEEP_SERVER -> 
                com.example.eventpay.data.sync.ConflictResolution.ServerWins()
        }
        
        return syncManager.resolveConflict(conflictId, resolutionStrategy)
    }
}

/**
 * Use Case: Force Sync
 * 
 * Triggers immediate sync of all pending operations.
 */
class ForceSyncUseCase @Inject constructor(
    private val syncManager: OfflineSyncManager,
    private val networkUtils: NetworkUtils
) {
    suspend operator fun invoke(): SyncResult {
        if (!networkUtils.isOnline()) {
            return SyncResult.Offline
        }
        
        return when (val result = syncManager.processPendingSync()) {
            is com.example.eventpay.data.sync.SyncResult.Offline -> SyncResult.Offline
            is com.example.eventpay.data.sync.SyncResult.Completed -> SyncResult.Completed(
                successCount = result.successCount,
                failureCount = result.failureCount,
                conflictCount = result.conflictCount
            )
            is com.example.eventpay.data.sync.SyncResult.Error -> SyncResult.Error(result.message)
        }
    }
}

/**
 * Use Case: Get Check-In History
 * 
 * Returns check-in history for an event.
 */
class GetCheckInHistoryUseCase @Inject constructor(
    private val recordDao: CheckInRecordDao
) {
    fun byEvent(eventId: String): Flow<List<CheckInRecordEntity>> {
        return recordDao.getRecordsByEvent(eventId)
    }
    
    fun byDevice(deviceId: String): Flow<List<CheckInRecordEntity>> {
        return recordDao.getRecordsByDevice(deviceId)
    }
}

/**
 * Use Case: Validate Ticket Offline
 * 
 * Validates a ticket using only local data.
 */
class ValidateTicketOfflineUseCase @Inject constructor(
    private val ticketDao: OfflineTicketDao,
    private val snapshotDao: EventSnapshotDao
) {
    suspend operator fun invoke(
        qrCode: String,
        eventId: String
    ): OfflineValidationResult {
        // Check if event is prepared for offline
        val snapshot = snapshotDao.getValidSnapshot(eventId)
        
        // Find ticket
        val ticket = ticketDao.getTicketByQRCode(qrCode)
            ?: return OfflineValidationResult.NotFound
        
        // Validate
        return when {
            ticket.eventId != eventId -> OfflineValidationResult.WrongEvent(ticket.eventId)
            ticket.isCheckedIn -> OfflineValidationResult.AlreadyCheckedIn(ticket.checkedInAt!!)
            ticket.status != "ACTIVE" -> OfflineValidationResult.InvalidStatus(ticket.status)
            else -> OfflineValidationResult.Valid(
                ticket = ticket,
                isOfflinePrepared = snapshot != null
            )
        }
    }
}

// ============================================================================
// RESULT TYPES
// ============================================================================

sealed class OfflineCheckInResult {
    data class Success(
        val ticketId: String,
        val checkedInAt: Long,
        val isOfflineMode: Boolean,
        val pendingSync: Boolean
    ) : OfflineCheckInResult()
    
    data class AlreadyCheckedIn(
        val ticketId: String,
        val checkedInAt: Long
    ) : OfflineCheckInResult()
    
    data class TicketNotFound(val qrCode: String) : OfflineCheckInResult()
    
    data class WrongEvent(
        val ticketEventId: String,
        val scannedEventId: String
    ) : OfflineCheckInResult()
    
    data class InvalidTicket(
        val ticketId: String,
        val reason: String
    ) : OfflineCheckInResult()
    
    data class Error(val message: String) : OfflineCheckInResult()
}

sealed class PrepareResult {
    data class Success(val ticketCount: Int) : PrepareResult()
    data class AlreadyPrepared(val expiresAt: Long) : PrepareResult()
    object Offline : PrepareResult()
    data class Error(val message: String) : PrepareResult()
}

data class OfflineStatus(
    val syncState: SyncState,
    val pendingSyncCount: Int,
    val conflictCount: Int
) {
    fun hasPendingSync(): Boolean = pendingSyncCount > 0
    fun hasConflicts(): Boolean = conflictCount > 0
}

enum class SyncState {
    IDLE,
    SYNCING,
    ERROR
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

enum class ConflictResolutionChoice {
    KEEP_LOCAL,
    KEEP_SERVER
}

sealed class OfflineValidationResult {
    data class Valid(
        val ticket: OfflineTicketEntity,
        val isOfflinePrepared: Boolean
    ) : OfflineValidationResult()
    
    object NotFound : OfflineValidationResult()
    data class WrongEvent(val correctEventId: String) : OfflineValidationResult()
    data class AlreadyCheckedIn(val checkedInAt: Long) : OfflineValidationResult()
    data class InvalidStatus(val status: String) : OfflineValidationResult()
}

/**
 * Aggregated Offline Use Cases
 */
@Singleton
class OfflineUseCases @Inject constructor(
    val offlineCheckIn: OfflineCheckInUseCase,
    val prepareEventOffline: PrepareEventOfflineUseCase,
    val getOfflineStatus: GetOfflineStatusUseCase,
    val resolveConflict: ResolveConflictUseCase,
    val forceSync: ForceSyncUseCase,
    val getCheckInHistory: GetCheckInHistoryUseCase,
    val validateTicketOffline: ValidateTicketOfflineUseCase
)
