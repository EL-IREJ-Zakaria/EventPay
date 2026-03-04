package com.example.eventpay.domain.sync

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Multi-Device Sync Manager
 * 
 * Coordinates real-time synchronization of check-in operations across
 * multiple devices, ensuring data consistency and conflict prevention.
 * 
 * Key Features:
 * - Real-time sync via Firebase Realtime Database / Firestore
 * - Optimistic locking for conflict prevention
 * - Offline queue with automatic sync on reconnection
 * - Heartbeat-based device presence
 * - Distributed locking for critical operations
 */
@Singleton
class MultiDeviceSyncManager @Inject constructor(
    private val json: Json,
    private val conflictResolutionEngine: ConflictResolutionEngine
) {
    // Current device state
    private var currentDeviceId: String = generateDeviceId()
    private var currentStaffId: String = ""
    private var currentEventId: String = ""
    
    // Sync state tracking
    private val _syncState = MutableStateFlow<DeviceSyncState?>(null)
    val syncState: StateFlow<DeviceSyncState?> = _syncState.asStateFlow()
    
    private val _eventSyncState = MutableStateFlow<EventSyncState?>(null)
    val eventSyncState: StateFlow<EventSyncState?> = _eventSyncState.asStateFlow()
    
    // Connection status
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.OFFLINE)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()
    
    // Pending operations queue
    private val _pendingOperations = MutableStateFlow<List<SyncOperation>>(emptyList())
    val pendingOperations: StateFlow<List<SyncOperation>> = _pendingOperations.asStateFlow()
    
    // Operation results
    private val _operationResults = MutableSharedFlow<SyncResult>()
    val operationResults: SharedFlow<SyncResult> = _operationResults.asSharedFlow()
    
    // Incoming sync messages
    private val _incomingMessages = MutableSharedFlow<SyncMessage>()
    val incomingMessages: SharedFlow<SyncMessage> = _incomingMessages.asSharedFlow()
    
    // Active locks held by this device
    private val activeLocks = ConcurrentHashMap<String, DistributedLock>()
    
    // Version tracking for optimistic concurrency
    private val localVersion = AtomicLong(0)
    
    // Configuration
    private var config = SyncConfiguration()
    
    // Background jobs
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var heartbeatJob: Job? = null
    private var syncJob: Job? = null
    private var lockRefreshJob: Job? = null
    
    // Statistics
    private val _statistics = MutableStateFlow<SyncStatistics?>(null)
    val statistics: StateFlow<SyncStatistics?> = _statistics.asStateFlow()
    
    /**
     * Initialize sync for an event
     */
    fun initialize(
        eventId: String,
        staffId: String,
        staffName: String,
        deviceName: String = "Device-${currentDeviceId.take(4)}",
        configuration: SyncConfiguration = SyncConfiguration()
    ) {
        currentEventId = eventId
        currentStaffId = staffId
        config = configuration
        
        // Create device state
        val deviceState = DeviceSyncState(
            deviceId = currentDeviceId,
            deviceName = deviceName,
            staffId = staffId,
            staffName = staffName,
            eventId = eventId,
            connectionStatus = ConnectionStatus.ONLINE
        )
        
        _syncState.value = deviceState
        
        // Start background services
        startHeartbeat()
        startSyncProcessor()
        startLockRefresher()
        
        // Announce presence
        broadcastPresence(deviceState)
    }
    
    /**
     * Submit a check-in operation for sync
     */
    suspend fun submitCheckIn(checkIn: SyncableCheckIn): SyncResult {
        val operation = SyncOperation(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            deviceId = currentDeviceId,
            staffId = currentStaffId,
            eventId = currentEventId,
            operationType = SyncOperationType.CHECK_IN,
            entityType = SyncEntityType.TICKET,
            entityId = checkIn.ticketId,
            payload = json.encodeToString(checkIn),
            version = localVersion.incrementAndGet(),
            checksum = generateChecksum(checkIn),
            status = SyncOperationStatus.PENDING
        )
        
        return submitOperation(operation)
    }
    
    /**
     * Submit a sync operation
     */
    private suspend fun submitOperation(operation: SyncOperation): SyncResult {
        // Try to acquire lock first for critical operations
        if (operation.operationType in config.priorityOperations) {
            val lockAcquired = acquireLock(
                entityType = operation.entityType,
                entityId = operation.entityId
            )
            
            if (!lockAcquired) {
                return SyncResult(
                    success = false,
                    operationId = operation.id,
                    error = SyncError(
                        code = SyncErrorCode.LOCK_NOT_GRANTED,
                        message = "Could not acquire lock for ${operation.entityId}",
                        recoverable = true
                    ),
                    requiresRetry = true,
                    retryDelay = config.retryDelay
                )
            }
        }
        
        // Add to pending queue
        _pendingOperations.value = _pendingOperations.value + operation
        
        // Try immediate sync if online
        if (_connectionStatus.value == ConnectionStatus.ONLINE) {
            return syncOperation(operation)
        }
        
        // Queue for later sync
        return SyncResult(
            success = true,
            operationId = operation.id,
            requiresRetry = true
        )
    }
    
    /**
     * Sync a single operation to the server
     */
    private suspend fun syncOperation(operation: SyncOperation): SyncResult {
        try {
            // Update status
            updateOperationStatus(operation.id, SyncOperationStatus.PROCESSING)
            
            // Send to server (would be implemented with Firebase/Backend)
            val result = sendToServer(operation)
            
            when {
                result.success -> {
                    // Remove from pending
                    _pendingOperations.value = _pendingOperations.value.filter { it.id != operation.id }
                    
                    // Release lock if held
                    releaseLock(operation.entityType, operation.entityId)
                    
                    // Broadcast to other devices
                    broadcastOperation(operation)
                    
                    // Emit result
                    _operationResults.emit(result)
                    
                    return result
                }
                
                result.conflict != null -> {
                    // Handle conflict
                    val resolved = conflictResolutionEngine.autoResolve(result.conflict)
                    
                    if (resolved?.status == ConflictStatus.RESOLVED) {
                        // Retry with resolved data
                        return syncOperation(operation.copy(
                            status = SyncOperationStatus.RETRYING,
                            retryCount = operation.retryCount + 1
                        ))
                    }
                    
                    return result
                }
                
                result.requiresRetry && operation.retryCount < config.maxRetries -> {
                    // Schedule retry
                    delay(config.retryDelay * (operation.retryCount + 1))
                    return syncOperation(operation.copy(
                        status = SyncOperationStatus.RETRYING,
                        retryCount = operation.retryCount + 1
                    ))
                }
                
                else -> {
                    // Mark as failed
                    updateOperationStatus(operation.id, SyncOperationStatus.FAILED)
                    return result
                }
            }
        } catch (e: Exception) {
            return SyncResult(
                success = false,
                operationId = operation.id,
                error = SyncError(
                    code = SyncErrorCode.NETWORK_ERROR,
                    message = e.message ?: "Unknown error",
                    recoverable = true
                ),
                requiresRetry = true,
                retryDelay = config.retryDelay
            )
        }
    }
    
    /**
     * Acquire a distributed lock
     */
    private suspend fun acquireLock(
        entityType: SyncEntityType,
        entityId: String
    ): Boolean {
        val lockId = "${entityType.name}_$entityId"
        
        // Check if we already hold this lock
        if (activeLocks.containsKey(lockId)) {
            return true
        }
        
        val lock = DistributedLock(
            lockId = lockId,
            entityType = entityType,
            entityId = entityId,
            eventId = currentEventId,
            lockedBy = currentDeviceId,
            staffId = currentStaffId,
            expiresAt = System.currentTimeMillis() + config.lockTimeout
        )
        
        // Try to acquire lock on server
        val acquired = tryAcquireLockOnServer(lock)
        
        if (acquired) {
            activeLocks[lockId] = lock
            return true
        }
        
        return false
    }
    
    /**
     * Release a distributed lock
     */
    private suspend fun releaseLock(
        entityType: SyncEntityType,
        entityId: String
    ) {
        val lockId = "${entityType.name}_$entityId"
        val lock = activeLocks.remove(lockId) ?: return
        
        releaseLockOnServer(lock)
    }
    
    /**
     * Handle incoming sync message from another device
     */
    fun handleIncomingMessage(message: SyncMessage) {
        when (message.type) {
            SyncMessageType.CHECK_IN_BROADCAST -> {
                handleCheckInBroadcast(message)
            }
            SyncMessageType.DEVICE_JOIN -> {
                handleDeviceJoin(message)
            }
            SyncMessageType.DEVICE_LEAVE -> {
                handleDeviceLeave(message)
            }
            SyncMessageType.HEARTBEAT -> {
                handleHeartbeat(message)
            }
            SyncMessageType.CONFLICT_NOTIFY -> {
                handleConflictNotify(message)
            }
            SyncMessageType.LOCK_REQUEST -> {
                handleLockRequest(message)
            }
            SyncMessageType.LOCK_RELEASE -> {
                handleLockRelease(message)
            }
            SyncMessageType.STATE_UPDATE -> {
                handleStateUpdate(message)
            }
            else -> {
                // Forward to listeners
                _incomingMessages.tryEmit(message)
            }
        }
    }
    
    /**
     * Handle check-in broadcast from another device
     */
    private fun handleCheckInBroadcast(message: SyncMessage) {
        try {
            val checkIn = json.decodeFromString<SyncableCheckIn>(message.payload)
            
            // Update local state to reflect remote check-in
            // This prevents duplicate check-in attempts
            updateLocalCheckInState(checkIn)
            
            // Emit to listeners
            _incomingMessages.tryEmit(message)
        } catch (e: Exception) {
            // Log error
        }
    }
    
    /**
     * Handle device join notification
     */
    private fun handleDeviceJoin(message: SyncMessage) {
        try {
            val deviceState = json.decodeFromString<DeviceSyncState>(message.payload)
            
            // Update event sync state with new device
            _eventSyncState.value?.let { state ->
                val existingDevices = state.activeDevices.filter { it.deviceId != deviceState.deviceId }
                _eventSyncState.value = state.copy(
                    activeDevices = existingDevices + deviceState
                )
            }
        } catch (e: Exception) {
            // Log error
        }
    }
    
    /**
     * Handle device leave notification
     */
    private fun handleDeviceLeave(message: SyncMessage) {
        val leavingDeviceId = message.sourceDeviceId
        
        _eventSyncState.value?.let { state ->
            _eventSyncState.value = state.copy(
                activeDevices = state.activeDevices.filter { it.deviceId != leavingDeviceId }
            )
        }
    }
    
    /**
     * Handle heartbeat from another device
     */
    private fun handleHeartbeat(message: SyncMessage) {
        _eventSyncState.value?.let { state ->
            val updatedDevices = state.activeDevices.map { device ->
                if (device.deviceId == message.sourceDeviceId) {
                    device.copy(lastHeartbeat = message.timestamp)
                } else {
                    device
                }
            }
            _eventSyncState.value = state.copy(activeDevices = updatedDevices)
        }
    }
    
    /**
     * Handle conflict notification
     */
    private fun handleConflictNotify(message: SyncMessage) {
        try {
            val conflict = json.decodeFromString<SyncConflict>(message.payload)
            
            // Add to conflict engine for tracking
            _incomingMessages.tryEmit(message)
        } catch (e: Exception) {
            // Log error
        }
    }
    
    /**
     * Handle lock request from another device
     */
    private fun handleLockRequest(message: SyncMessage) {
        // Check if we hold this lock
        try {
            val lock = json.decodeFromString<DistributedLock>(message.payload)
            val lockId = lock.lockId
            
            if (activeLocks.containsKey(lockId)) {
                // We hold the lock, deny the request
                // Send LOCK_DENIED message
            }
        } catch (e: Exception) {
            // Log error
        }
    }
    
    /**
     * Handle lock release from another device
     */
    private fun handleLockRelease(message: SyncMessage) {
        // No action needed - lock is released
    }
    
    /**
     * Handle state update from server
     */
    private fun handleStateUpdate(message: SyncMessage) {
        try {
            val state = json.decodeFromString<EventSyncState>(message.payload)
            _eventSyncState.value = state
            localVersion.set(state.globalVersion)
        } catch (e: Exception) {
            // Log error
        }
    }
    
    // ========================================================================
    // BACKGROUND SERVICES
    // ========================================================================
    
    /**
     * Start heartbeat service
     */
    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = syncScope.launch {
            while (isActive) {
                sendHeartbeat()
                delay(config.heartbeatInterval)
            }
        }
    }
    
    /**
     * Start sync processor
     */
    private fun startSyncProcessor() {
        syncJob?.cancel()
        syncJob = syncScope.launch {
            while (isActive) {
                processPendingOperations()
                delay(config.batchInterval)
            }
        }
    }
    
    /**
     * Start lock refresher
     */
    private fun startLockRefresher() {
        lockRefreshJob?.cancel()
        lockRefreshJob = syncScope.launch {
            while (isActive) {
                refreshActiveLocks()
                delay(config.lockTimeout / 2)
            }
        }
    }
    
    /**
     * Process pending operations
     */
    private suspend fun processPendingOperations() {
        if (_connectionStatus.value != ConnectionStatus.ONLINE) return
        
        val pending = _pendingOperations.value
        if (pending.isEmpty()) return
        
        // Process in batches
        val batch = pending.take(config.batchSize)
        
        for (operation in batch) {
            if (operation.status == SyncOperationStatus.PENDING ||
                operation.status == SyncOperationStatus.RETRYING) {
                syncOperation(operation)
            }
        }
    }
    
    /**
     * Refresh active locks to prevent expiration
     */
    private suspend fun refreshActiveLocks() {
        val now = System.currentTimeMillis()
        
        activeLocks.forEach { (lockId, lock) ->
            if (lock.expiresAt - now < config.lockTimeout / 2) {
                // Extend lock
                val extended = extendLockOnServer(lock)
                if (extended) {
                    activeLocks[lockId] = lock.copy(
                        expiresAt = now + config.lockTimeout
                    )
                } else {
                    // Lock was lost
                    activeLocks.remove(lockId)
                }
            }
        }
    }
    
    // ========================================================================
    // SERVER COMMUNICATION (Stub methods - implement with Firebase/Backend)
    // ========================================================================
    
    private suspend fun sendToServer(operation: SyncOperation): SyncResult {
        // TODO: Implement with Firebase Firestore/Realtime Database
        // This would:
        // 1. Send operation to server
        // 2. Check for conflicts
        // 3. Return result
        
        return SyncResult(
            success = true,
            operationId = operation.id,
            newVersion = operation.version
        )
    }
    
    private suspend fun tryAcquireLockOnServer(lock: DistributedLock): Boolean {
        // TODO: Implement with Firebase
        // This would use a transaction to atomically create a lock document
        return true
    }
    
    private suspend fun releaseLockOnServer(lock: DistributedLock) {
        // TODO: Implement with Firebase
    }
    
    private suspend fun extendLockOnServer(lock: DistributedLock): Boolean {
        // TODO: Implement with Firebase
        return true
    }
    
    private fun broadcastPresence(deviceState: DeviceSyncState) {
        // TODO: Implement with Firebase Realtime Database
        // Set presence in /events/{eventId}/devices/{deviceId}
    }
    
    private fun broadcastOperation(operation: SyncOperation) {
        // TODO: Implement with Firebase Realtime Database
        // Broadcast to /events/{eventId}/operations
    }
    
    private fun sendHeartbeat() {
        // TODO: Implement with Firebase
        // Update heartbeat timestamp
    }
    
    private fun updateLocalCheckInState(checkIn: SyncableCheckIn) {
        // Update local database to reflect remote check-in
    }
    
    // ========================================================================
    // HELPER METHODS
    // ========================================================================
    
    private fun generateDeviceId(): String {
        return UUID.randomUUID().toString()
    }
    
    private fun generateChecksum(data: Any): String {
        val jsonStr = json.encodeToString(data)
        return jsonStr.hashCode().toString(16)
    }
    
    private fun updateOperationStatus(operationId: String, status: SyncOperationStatus) {
        _pendingOperations.value = _pendingOperations.value.map { op ->
            if (op.id == operationId) op.copy(status = status) else op
        }
    }
    
    /**
     * Get current sync statistics
     */
    fun getStatistics(): SyncStatistics {
        return SyncStatistics(
            eventId = currentEventId,
            periodStart = System.currentTimeMillis() - 3600000, // Last hour
            periodEnd = System.currentTimeMillis(),
            totalOperations = _pendingOperations.value.size,
            successfulSyncs = 0, // Track this
            failedSyncs = 0,
            conflictsDetected = conflictResolutionEngine.getConflictStatistics().values.sum(),
            conflictsResolved = conflictResolutionEngine.resolutionHistory.value.size,
            offlineOperations = _pendingOperations.value.count { 
                it.status == SyncOperationStatus.PENDING 
            }
        )
    }
    
    /**
     * Cleanup and disconnect
     */
    fun disconnect() {
        // Cancel background jobs
        heartbeatJob?.cancel()
        syncJob?.cancel()
        lockRefreshJob?.cancel()
        
        // Release all locks
        syncScope.launch {
            activeLocks.forEach { (_, lock) ->
                releaseLockOnServer(lock)
            }
            activeLocks.clear()
        }
        
        // Announce departure
        broadcastLeave()
        
        // Clear state
        _syncState.value = null
        _eventSyncState.value = null
        _pendingOperations.value = emptyList()
    }
    
    private fun broadcastLeave() {
        // TODO: Implement with Firebase
        // Remove presence and broadcast DEVICE_LEAVE
    }
    
    /**
     * Force sync all pending operations
     */
    suspend fun forceSync() {
        processPendingOperations()
    }
    
    /**
     * Get pending operations count
     */
    fun getPendingCount(): Int = _pendingOperations.value.size
    
    /**
     * Check if device is online
     */
    fun isOnline(): Boolean = _connectionStatus.value == ConnectionStatus.ONLINE
    
    /**
     * Update connection status
     */
    fun updateConnectionStatus(status: ConnectionStatus) {
        _connectionStatus.value = status
        
        _syncState.value?.let { state ->
            _syncState.value = state.copy(connectionStatus = status)
        }
        
        if (status == ConnectionStatus.ONLINE) {
            // Trigger immediate sync
            syncScope.launch {
                processPendingOperations()
            }
        }
    }
}
