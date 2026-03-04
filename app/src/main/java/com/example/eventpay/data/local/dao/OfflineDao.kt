package com.example.eventpay.data.local.dao

import androidx.room.*
import com.example.eventpay.data.local.entity.*
import kotlinx.coroutines.flow.Flow

/**
 * Offline Ticket DAO
 * 
 * Data Access Object for offline ticket operations.
 * Supports offline check-in with sync tracking.
 */
@Dao
interface OfflineTicketDao {
    
    // =========================================================================
    // QUERY OPERATIONS
    // =========================================================================
    
    @Query("SELECT * FROM offline_tickets WHERE id = :ticketId")
    suspend fun getTicketById(ticketId: String): OfflineTicketEntity?
    
    @Query("SELECT * FROM offline_tickets WHERE qrCode = :qrCode")
    suspend fun getTicketByQRCode(qrCode: String): OfflineTicketEntity?
    
    @Query("SELECT * FROM offline_tickets WHERE eventId = :eventId")
    fun getTicketsByEvent(eventId: String): Flow<List<OfflineTicketEntity>>
    
    @Query("SELECT * FROM offline_tickets WHERE userId = :userId")
    fun getTicketsByUser(userId: String): Flow<List<OfflineTicketEntity>>
    
    @Query("SELECT * FROM offline_tickets WHERE eventId = :eventId AND isCheckedIn = 1")
    fun getCheckedInTickets(eventId: String): Flow<List<OfflineTicketEntity>>
    
    @Query("SELECT COUNT(*) FROM offline_tickets WHERE eventId = :eventId AND isCheckedIn = 1")
    suspend fun getCheckedInCount(eventId: String): Int
    
    @Query("SELECT COUNT(*) FROM offline_tickets WHERE eventId = :eventId")
    suspend fun getTotalCount(eventId: String): Int
    
    @Query("SELECT * FROM offline_tickets WHERE syncStatus = :status")
    suspend fun getTicketsBySyncStatus(status: String): List<OfflineTicketEntity>
    
    @Query("SELECT * FROM offline_tickets WHERE syncStatus IN ('PENDING', 'FAILED')")
    fun getPendingSyncTickets(): Flow<List<OfflineTicketEntity>>
    
    @Query("SELECT * FROM offline_tickets WHERE offlineCheckIn = 1")
    suspend fun getOfflineCheckedInTickets(): List<OfflineTicketEntity>
    
    // =========================================================================
    // INSERT/UPDATE OPERATIONS
    // =========================================================================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: OfflineTicketEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTickets(tickets: List<OfflineTicketEntity>)
    
    @Update
    suspend fun updateTicket(ticket: OfflineTicketEntity)
    
    @Query("UPDATE offline_tickets SET syncStatus = :status, syncError = :error WHERE id = :ticketId")
    suspend fun updateSyncStatus(ticketId: String, status: String, error: String? = null)
    
    @Query("UPDATE offline_tickets SET syncStatus = 'SYNCED', serverVersion = :version WHERE id = :ticketId")
    suspend fun markSynced(ticketId: String, version: Int)
    
    @Query("""
        UPDATE offline_tickets 
        SET isCheckedIn = 1, 
            checkedInAt = :checkedInAt,
            checkedInBy = :checkedInBy,
            checkedInDeviceId = :deviceId,
            checkedInLocation = :location,
            status = 'USED',
            syncStatus = 'PENDING',
            lastModified = :checkedInAt,
            localVersion = localVersion + 1,
            offlineCheckIn = 1,
            offlineCheckInTime = :checkedInAt
        WHERE id = :ticketId AND isCheckedIn = 0
    """)
    suspend fun checkInTicket(
        ticketId: String,
        checkedInAt: Long,
        checkedInBy: String,
        deviceId: String,
        location: String?
    ): Int
    
    // =========================================================================
    // DELETE OPERATIONS
    // =========================================================================
    
    @Delete
    suspend fun deleteTicket(ticket: OfflineTicketEntity)
    
    @Query("DELETE FROM offline_tickets WHERE eventId = :eventId")
    suspend fun deleteTicketsForEvent(eventId: String)
    
    @Query("DELETE FROM offline_tickets")
    suspend fun deleteAll()
}

/**
 * Pending Sync Queue DAO
 * 
 * Manages the queue of operations waiting to be synchronized.
 */
@Dao
interface PendingSyncQueueDao {
    
    // =========================================================================
    // QUERY OPERATIONS
    // =========================================================================
    
    @Query("SELECT * FROM pending_sync_queue ORDER BY priority DESC, createdAt ASC")
    fun getAllPending(): Flow<List<PendingSyncEntity>>
    
    @Query("SELECT * FROM pending_sync_queue WHERE syncStatus = 'PENDING' ORDER BY priority DESC, createdAt ASC")
    suspend fun getPendingItems(): List<PendingSyncEntity>
    
    @Query("SELECT * FROM pending_sync_queue WHERE syncStatus = 'PENDING' ORDER BY priority DESC, createdAt ASC LIMIT :limit")
    suspend fun getPendingItems(limit: Int): List<PendingSyncEntity>
    
    @Query("SELECT * FROM pending_sync_queue WHERE id = :id")
    suspend fun getById(id: Long): PendingSyncEntity?
    
    @Query("SELECT * FROM pending_sync_queue WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun getByEntity(entityType: String, entityId: String): PendingSyncEntity?
    
    @Query("SELECT COUNT(*) FROM pending_sync_queue WHERE syncStatus = 'PENDING'")
    suspend fun getPendingCount(): Int
    
    @Query("SELECT COUNT(*) FROM pending_sync_queue WHERE syncStatus = 'FAILED'")
    suspend fun getFailedCount(): Int
    
    @Query("SELECT EXISTS(SELECT 1 FROM pending_sync_queue WHERE entityType = :entityType AND entityId = :entityId AND syncStatus = 'PENDING')")
    suspend fun hasPendingOperation(entityType: String, entityId: String): Boolean
    
    // =========================================================================
    // INSERT/UPDATE OPERATIONS
    // =========================================================================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PendingSyncEntity): Long
    
    @Update
    suspend fun update(item: PendingSyncEntity)
    
    @Query("UPDATE pending_sync_queue SET syncStatus = 'SYNCING' WHERE id = :id")
    suspend fun markSyncing(id: Long)
    
    @Query("UPDATE pending_sync_queue SET syncStatus = 'SYNCED', updatedAt = :timestamp WHERE id = :id")
    suspend fun markSynced(id: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE pending_sync_queue SET syncStatus = 'FAILED', lastError = :error, attempts = attempts + 1 WHERE id = :id")
    suspend fun markFailed(id: Long, error: String)
    
    @Query("UPDATE pending_sync_queue SET syncStatus = 'CONFLICT' WHERE id = :id")
    suspend fun markConflict(id: Long)
    
    // =========================================================================
    // DELETE OPERATIONS
    // =========================================================================
    
    @Delete
    suspend fun delete(item: PendingSyncEntity)
    
    @Query("DELETE FROM pending_sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM pending_sync_queue WHERE syncStatus = 'SYNCED'")
    suspend fun deleteSynced()
    
    @Query("DELETE FROM pending_sync_queue WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun deleteByEntity(entityType: String, entityId: String)
}

/**
 * Sync Conflict DAO
 * 
 * Manages detected sync conflicts.
 */
@Dao
interface SyncConflictDao {
    
    @Query("SELECT * FROM sync_conflicts WHERE resolved = 0 ORDER BY detectedAt DESC")
    fun getUnresolvedConflicts(): Flow<List<SyncConflictEntity>>
    
    @Query("SELECT * FROM sync_conflicts WHERE resolved = 1 ORDER BY resolvedAt DESC")
    fun getResolvedConflicts(): Flow<List<SyncConflictEntity>>
    
    @Query("SELECT * FROM sync_conflicts WHERE id = :id")
    suspend fun getById(id: Long): SyncConflictEntity?
    
    @Query("SELECT * FROM sync_conflicts WHERE entityType = :entityType AND entityId = :entityId AND resolved = 0")
    suspend fun getUnresolvedConflict(entityType: String, entityId: String): SyncConflictEntity?
    
    @Query("SELECT COUNT(*) FROM sync_conflicts WHERE resolved = 0")
    suspend fun getUnresolvedCount(): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conflict: SyncConflictEntity): Long
    
    @Update
    suspend fun update(conflict: SyncConflictEntity)
    
    @Query("""
        UPDATE sync_conflicts 
        SET resolved = 1, 
            resolution = :resolution, 
            resolvedAt = :timestamp,
            resolvedData = :resolvedData
        WHERE id = :id
    """)
    suspend fun resolve(id: Long, resolution: String, resolvedData: String?, timestamp: Long = System.currentTimeMillis())
    
    @Delete
    suspend fun delete(conflict: SyncConflictEntity)
    
    @Query("DELETE FROM sync_conflicts WHERE resolved = 1 AND resolvedAt < :olderThan")
    suspend fun deleteOldResolved(olderThan: Long)
}

/**
 * Event Snapshot DAO
 * 
 * Manages cached event data for offline validation.
 */
@Dao
interface EventSnapshotDao {
    
    @Query("SELECT * FROM event_snapshots WHERE eventId = :eventId")
    suspend fun getSnapshot(eventId: String): EventSnapshotEntity?
    
    @Query("SELECT * FROM event_snapshots WHERE eventId = :eventId AND expiresAt > :now")
    suspend fun getValidSnapshot(eventId: String, now: Long = System.currentTimeMillis()): EventSnapshotEntity?
    
    @Query("SELECT * FROM event_snapshots WHERE expiresAt < :now")
    suspend fun getExpiredSnapshots(now: Long = System.currentTimeMillis()): List<EventSnapshotEntity>
    
    @Query("SELECT EXISTS(SELECT 1 FROM event_snapshots WHERE eventId = :eventId AND expiresAt > :now)")
    suspend fun hasValidSnapshot(eventId: String, now: Long = System.currentTimeMillis()): Boolean
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: EventSnapshotEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(snapshots: List<EventSnapshotEntity>)
    
    @Update
    suspend fun update(snapshot: EventSnapshotEntity)
    
    @Delete
    suspend fun delete(snapshot: EventSnapshotEntity)
    
    @Query("DELETE FROM event_snapshots WHERE eventId = :eventId")
    suspend fun deleteByEventId(eventId: String)
    
    @Query("DELETE FROM event_snapshots WHERE expiresAt < :now")
    suspend fun deleteExpired(now: Long = System.currentTimeMillis())
}

/**
 * Check-in Record DAO
 * 
 * Manages audit trail of check-in attempts.
 */
@Dao
interface CheckInRecordDao {
    
    @Query("SELECT * FROM check_in_records ORDER BY checkedInAt DESC")
    fun getAllRecords(): Flow<List<CheckInRecordEntity>>
    
    @Query("SELECT * FROM check_in_records WHERE eventId = :eventId ORDER BY checkedInAt DESC")
    fun getRecordsByEvent(eventId: String): Flow<List<CheckInRecordEntity>>
    
    @Query("SELECT * FROM check_in_records WHERE ticketId = :ticketId ORDER BY checkedInAt DESC")
    fun getRecordsByTicket(ticketId: String): Flow<List<CheckInRecordEntity>>
    
    @Query("SELECT * FROM check_in_records WHERE deviceId = :deviceId ORDER BY checkedInAt DESC")
    fun getRecordsByDevice(deviceId: String): Flow<List<CheckInRecordEntity>>
    
    @Query("SELECT * FROM check_in_records WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncRecords(): List<CheckInRecordEntity>
    
    @Query("SELECT COUNT(*) FROM check_in_records WHERE eventId = :eventId AND success = 1")
    suspend fun getSuccessfulCheckInCount(eventId: String): Int
    
    @Query("SELECT COUNT(*) FROM check_in_records WHERE eventId = :eventId AND success = 0")
    suspend fun getFailedCheckInCount(eventId: String): Int
    
    @Query("SELECT COUNT(*) FROM check_in_records WHERE eventId = :eventId AND offlineMode = 1")
    suspend fun getOfflineCheckInCount(eventId: String): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: CheckInRecordEntity): Long
    
    @Update
    suspend fun update(record: CheckInRecordEntity)
    
    @Query("UPDATE check_in_records SET syncStatus = 'SYNCED', syncedAt = :timestamp WHERE id = :id")
    suspend fun markSynced(id: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM check_in_records WHERE checkedInAt < :olderThan")
    suspend fun deleteOldRecords(olderThan: Long)
}

/**
 * Sync Metadata DAO
 * 
 * Tracks sync state for incremental sync.
 */
@Dao
interface SyncMetadataDao {
    
    @Query("SELECT * FROM sync_metadata WHERE entityType = :entityType")
    suspend fun getMetadata(entityType: String): SyncMetadataEntity?
    
    @Query("SELECT * FROM sync_metadata")
    suspend fun getAllMetadata(): List<SyncMetadataEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metadata: SyncMetadataEntity)
    
    @Update
    suspend fun update(metadata: SyncMetadataEntity)
    
    @Query("""
        UPDATE sync_metadata 
        SET lastSyncAt = :timestamp, 
            syncToken = :token,
            syncVersion = :version,
            entityCount = :entityCount,
            pendingCount = :pendingCount,
            conflictCount = :conflictCount
        WHERE entityType = :entityType
    """)
    suspend fun updateSyncState(
        entityType: String,
        timestamp: Long,
        token: String?,
        version: Int,
        entityCount: Int,
        pendingCount: Int,
        conflictCount: Int
    )
    
    @Query("UPDATE sync_metadata SET pendingCount = pendingCount + 1 WHERE entityType = :entityType")
    suspend fun incrementPending(entityType: String)
    
    @Query("UPDATE sync_metadata SET pendingCount = pendingCount - 1 WHERE entityType = :entityType AND pendingCount > 0")
    suspend fun decrementPending(entityType: String)
    
    @Query("UPDATE sync_metadata SET conflictCount = conflictCount + 1 WHERE entityType = :entityType")
    suspend fun incrementConflict(entityType: String)
}

/**
 * Device Registration DAO
 */
@Dao
interface DeviceRegistrationDao {
    
    @Query("SELECT * FROM device_registrations WHERE deviceId = :deviceId")
    suspend fun getDevice(deviceId: String): DeviceRegistrationEntity?
    
    @Query("SELECT * FROM device_registrations WHERE userId = :userId AND isActive = 1")
    suspend fun getActiveDevicesForUser(userId: String): List<DeviceRegistrationEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: DeviceRegistrationEntity)
    
    @Update
    suspend fun update(device: DeviceRegistrationEntity)
    
    @Query("UPDATE device_registrations SET lastSyncAt = :timestamp WHERE deviceId = :deviceId")
    suspend fun updateLastSync(deviceId: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE device_registrations SET isActive = 0 WHERE deviceId = :deviceId")
    suspend fun deactivate(deviceId: String)
}
