package com.example.eventpay.data.local.dao

import androidx.room.*
import com.example.eventpay.data.local.PendingSyncOperation
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Pending Sync Operations
 * 
 * Manages the queue of operations waiting to be synchronized
 * with the remote server.
 */
@Dao
interface PendingSyncDao {
    
    @Query("SELECT * FROM pending_sync ORDER BY createdAt ASC")
    fun getAllPendingOperations(): Flow<List<PendingSyncOperation>>
    
    @Query("SELECT * FROM pending_sync WHERE entityType = :entityType ORDER BY createdAt ASC")
    fun getPendingOperationsByType(entityType: String): Flow<List<PendingSyncOperation>>
    
    @Query("SELECT * FROM pending_sync WHERE entityId = :entityId LIMIT 1")
    suspend fun getPendingOperationByEntityId(entityId: String): PendingSyncOperation?
    
    @Query("SELECT COUNT(*) FROM pending_sync")
    fun getPendingCount(): Flow<Int>
    
    @Query("SELECT * FROM pending_sync ORDER BY createdAt ASC LIMIT 1")
    suspend fun getNextOperation(): PendingSyncOperation?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: PendingSyncOperation): Long
    
    @Update
    suspend fun updateOperation(operation: PendingSyncOperation)
    
    @Delete
    suspend fun deleteOperation(operation: PendingSyncOperation)
    
    @Query("DELETE FROM pending_sync WHERE id = :id")
    suspend fun deleteOperationById(id: Long)
    
    @Query("DELETE FROM pending_sync WHERE entityId = :entityId")
    suspend fun deleteOperationsByEntityId(entityId: String)
    
    @Query("DELETE FROM pending_sync")
    suspend fun deleteAllOperations()
    
    @Transaction
    suspend fun insertOrUpdateOperation(operation: PendingSyncOperation) {
        val existing = getPendingOperationByEntityId(operation.entityId)
        if (existing != null) {
            updateOperation(operation.copy(id = existing.id))
        } else {
            insertOperation(operation)
        }
    }
}
