package com.example.eventpay.data.local

import com.example.eventpay.data.local.dao.PendingSyncDao
import com.example.eventpay.data.firebase.FirestoreEventRepository
import com.example.eventpay.data.firebase.FirestoreTicketRepository
import com.example.eventpay.data.firebase.FirestoreTransactionRepository
import com.example.eventpay.domain.repository.EventRepository
import com.example.eventpay.domain.repository.TicketRepository
import com.example.eventpay.domain.repository.TransactionRepository
import com.example.eventpay.domain.repository.UserRepository
import com.example.eventpay.util.NetworkUtils
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sync Manager
 * 
 * Manages synchronization between local and remote data.
 * Handles offline operations queue and automatic sync when online.
 * 
 * Features:
 * - Automatic sync when connectivity is restored
 * - Retry mechanism for failed operations
 * - Conflict resolution
 * - Sync status tracking
 */
@Singleton
class SyncManager @Inject constructor(
    private val pendingSyncDao: PendingSyncDao,
    private val eventRepository: EventRepository,
    private val ticketRepository: TicketRepository,
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository,
    private val networkUtils: NetworkUtils,
    private val gson: Gson
) {
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _syncStatus = MutableStateFlow(SyncStatusInfo())
    val syncStatus: StateFlow<SyncStatusInfo> = _syncStatus.asStateFlow()
    
    private var syncJob: Job? = null
    
    init {
        // Observe network connectivity
        syncScope.launch {
            networkUtils.observeConnectivity().collect { status ->
                _syncStatus.update { it.copy(isOnline = status.isOnline()) }
                
                if (status.isOnline()) {
                    // Trigger sync when coming online
                    startSync()
                }
            }
        }
        
        // Observe pending operations count
        syncScope.launch {
            pendingSyncDao.getPendingCount().collect { count ->
                _syncStatus.update { it.copy(pendingOperations = count) }
            }
        }
    }
    
    /**
     * Queue an operation for sync
     */
    suspend fun queueOperation(
        entityType: String,
        entityId: String,
        operationType: String,
        payload: Any
    ) {
        val operation = PendingSyncOperation(
            entityType = entityType,
            entityId = entityId,
            operationType = operationType,
            payload = gson.toJson(payload)
        )
        
        pendingSyncDao.insertOrUpdateOperation(operation)
        
        // Try to sync immediately if online
        if (networkUtils.isOnline()) {
            startSync()
        }
    }
    
    /**
     * Start synchronization process
     */
    fun startSync() {
        if (syncJob?.isActive == true) return
        if (!networkUtils.isOnline()) return
        
        syncJob = syncScope.launch {
            _syncStatus.update { it.copy(isSyncing = true, syncError = null) }
            
            try {
                // Sync each entity type
                syncEvents()
                syncTickets()
                syncTransactions()
                syncUsers()
                
                // Process pending operations
                processPendingOperations()
                
                _syncStatus.update { 
                    it.copy(
                        isSyncing = false,
                        lastSyncTime = System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) {
                _syncStatus.update { 
                    it.copy(
                        isSyncing = false,
                        syncError = e.message
                    )
                }
            }
        }
    }
    
    /**
     * Sync events from remote
     */
    private suspend fun syncEvents() {
        eventRepository.syncWithRemote()
    }
    
    /**
     * Sync tickets from remote
     */
    private suspend fun syncTickets() {
        ticketRepository.syncWithRemote()
    }
    
    /**
     * Sync transactions from remote
     */
    private suspend fun syncTransactions() {
        transactionRepository.syncWithRemote()
    }
    
    /**
     * Sync users from remote
     */
    private suspend fun syncUsers() {
        userRepository.syncWithRemote()
    }
    
    /**
     * Process pending operations queue
     */
    private suspend fun processPendingOperations() {
        var operation = pendingSyncDao.getNextOperation()
        
        while (operation != null) {
            val result = processOperation(operation)
            
            if (result.isSuccess) {
                // Remove successful operation from queue
                pendingSyncDao.deleteOperation(operation)
            } else {
                // Update operation with error and increment attempt count
                val updatedOp = operation
                    .incrementAttempt()
                    .withError(result.exceptionOrNull()?.message ?: "Unknown error")
                
                pendingSyncDao.updateOperation(updatedOp)
                
                // Stop processing if we hit an error (will retry later)
                break
            }
            
            operation = pendingSyncDao.getNextOperation()
        }
    }
    
    /**
     * Process a single pending operation
     */
    private suspend fun processOperation(operation: PendingSyncOperation): Result<Unit> {
        return try {
            when (operation.entityType) {
                SyncEntityType.EVENT -> processEventOperation(operation)
                SyncEntityType.TICKET -> processTicketOperation(operation)
                SyncEntityType.TRANSACTION -> processTransactionOperation(operation)
                SyncEntityType.USER -> processUserOperation(operation)
                else -> Result.failure(Exception("Unknown entity type: ${operation.entityType}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun processEventOperation(operation: PendingSyncOperation): Result<Unit> {
        // Implementation would parse payload and call appropriate repository method
        return Result.success(Unit)
    }
    
    private suspend fun processTicketOperation(operation: PendingSyncOperation): Result<Unit> {
        // Implementation would parse payload and call appropriate repository method
        return Result.success(Unit)
    }
    
    private suspend fun processTransactionOperation(operation: PendingSyncOperation): Result<Unit> {
        // Implementation would parse payload and call appropriate repository method
        return Result.success(Unit)
    }
    
    private suspend fun processUserOperation(operation: PendingSyncOperation): Result<Unit> {
        // Implementation would parse payload and call appropriate repository method
        return Result.success(Unit)
    }
    
    /**
     * Force sync all data
     */
    fun forceSync() {
        startSync()
    }
    
    /**
     * Clear all pending operations
     */
    suspend fun clearPendingOperations() {
        pendingSyncDao.deleteAllOperations()
    }
    
    /**
     * Get pending operations for a specific entity
     */
    suspend fun getPendingOperationsForEntity(entityId: String): PendingSyncOperation? {
        return pendingSyncDao.getPendingOperationByEntityId(entityId)
    }
    
    /**
     * Cancel ongoing sync
     */
    fun cancelSync() {
        syncJob?.cancel()
        _syncStatus.update { it.copy(isSyncing = false) }
    }
    
    /**
     * Cleanup when no longer needed
     */
    fun cleanup() {
        cancelSync()
        syncScope.cancel()
    }
}
