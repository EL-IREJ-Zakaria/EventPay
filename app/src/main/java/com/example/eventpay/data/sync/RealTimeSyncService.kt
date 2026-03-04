package com.example.eventpay.data.sync

import com.example.eventpay.domain.sync.SyncResult as DomainSyncResult
import com.example.eventpay.domain.sync.SyncError
import com.example.eventpay.domain.sync.SyncErrorCode
import com.example.eventpay.domain.sync.SyncOperation
import com.example.eventpay.domain.sync.SyncOperationStatus
import com.example.eventpay.domain.sync.SyncOperationType
import com.example.eventpay.domain.sync.SyncEntityType
import com.example.eventpay.domain.sync.SyncMessage
import com.example.eventpay.domain.sync.SyncConflict
import com.example.eventpay.domain.sync.DeviceSyncState
import com.example.eventpay.domain.sync.SyncableCheckIn
import com.example.eventpay.domain.sync.ConnectionStatus
import com.example.eventpay.domain.sync.SyncMessageType
import com.example.eventpay.domain.sync.SyncPriority
import com.example.eventpay.domain.sync.EventSyncState
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction as FirebaseTransaction
import com.google.firebase.firestore.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Real-Time Sync Service
 * 
 * Firebase implementation of real-time synchronization for multi-device
 * check-in operations. Uses Firebase Realtime Database for presence and
 * real-time updates, and Firestore for persistent data storage.
 * 
 * Architecture:
 * - Realtime Database: Device presence, heartbeats, real-time broadcasts
 * - Firestore: Persistent operations, conflict resolution, audit logs
 * 
 * Data Structure:
 * ```
 * /events/{eventId}/
 *   ├── devices/{deviceId}     - Device presence
 *   ├── operations/{opId}      - Sync operations queue
 *   ├── locks/{lockId}         - Distributed locks
 *   └── state/                 - Global event state
 * ```
 */
@Singleton
class RealTimeSyncService @Inject constructor(
    private val json: Json,
    private val realtimeDb: FirebaseDatabase,
    private val firestore: FirebaseFirestore
) {
    // Active listeners
    private val activeListeners = mutableMapOf<String, ListenerRegistration>()
    private var devicePresenceRef: DatabaseReference? = null
    private var operationsRef: DatabaseReference? = null
    
    // Current session
    private var currentEventId: String? = null
    private var currentDeviceId: String? = null
    
    // Incoming message flow
    private val _messageFlow = MutableSharedFlow<SyncMessage>(extraBufferCapacity = 64)
    val messageFlow: SharedFlow<SyncMessage> = _messageFlow.asSharedFlow()
    
    // Connection state
    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()
    
    /**
     * Connect to sync service for an event
     */
    fun connect(
        eventId: String,
        deviceId: String,
        deviceState: DeviceSyncState
    ): Flow<RealTimeSyncState> = callbackFlow {
        currentEventId = eventId
        currentDeviceId = deviceId
        
        // Setup presence
        setupPresence(eventId, deviceId, deviceState)
        
        // Setup operation listener
        setupOperationListener(eventId, deviceId)
        
        // Setup device list listener
        setupDeviceListListener(eventId)
        
        // Setup connection state listener
        setupConnectionListener()
        
        // Initial state
        val initialState = RealTimeSyncState(
            connected = true,
            eventId = eventId,
            deviceId = deviceId,
            globalVersion = 0
        )
        
        trySend(initialState)
        
        awaitClose {
            disconnect()
        }
    }
    
    /**
     * Setup device presence in Realtime Database
     */
    private fun setupPresence(
        eventId: String,
        deviceId: String,
        deviceState: DeviceSyncState
    ) {
        val presenceRef = realtimeDb.getReference("events/$eventId/devices/$deviceId")
        devicePresenceRef = presenceRef
        
        // Set presence data
        val presenceData = mapOf(
            "deviceId" to deviceState.deviceId,
            "deviceName" to deviceState.deviceName,
            "staffId" to deviceState.staffId,
            "staffName" to deviceState.staffName,
            "lastHeartbeat" to ServerValue.TIMESTAMP,
            "connectionStatus" to deviceState.connectionStatus.name,
            "joinedAt" to ServerValue.TIMESTAMP
        )
        
        // Set on connect
        presenceRef.setValue(presenceData)
        
        // Setup disconnect handler
        presenceRef.onDisconnect().updateChildren(
            mapOf(
                "connectionStatus" to ConnectionStatus.OFFLINE.name,
                "disconnectedAt" to ServerValue.TIMESTAMP
            )
        )
        
        // Setup heartbeat
        setupHeartbeat(presenceRef)
    }
    
    /**
     * Setup automatic heartbeat
     */
    private fun setupHeartbeat(presenceRef: DatabaseReference) {
        val heartbeatRunnable = object : Runnable {
            override fun run() {
                presenceRef.updateChildren(
                    mapOf(
                        "lastHeartbeat" to ServerValue.TIMESTAMP,
                        "connectionStatus" to ConnectionStatus.ONLINE.name
                    )
                )
            }
        }
        
        // Schedule heartbeat every 5 seconds
        // In production, use a proper coroutine scope
        CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                heartbeatRunnable.run()
                delay(5000)
            }
        }
    }
    
    /**
     * Setup listener for operations from other devices
     */
    private fun setupOperationListener(eventId: String, deviceId: String) {
        val opsRef = realtimeDb.getReference("events/$eventId/operations")
        operationsRef = opsRef
        
        opsRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                handleOperationSnapshot(snapshot, deviceId)
            }
            
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                handleOperationSnapshot(snapshot, deviceId)
            }
            
            override fun onChildRemoved(snapshot: DataSnapshot) {
                // Operation completed/removed
            }
            
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            
            override fun onCancelled(error: DatabaseError) {
                _connectionState.value = false
            }
        })
    }
    
    /**
     * Handle incoming operation snapshot
     */
    private fun handleOperationSnapshot(snapshot: DataSnapshot, currentDeviceId: String) {
        try {
            val operationMap = snapshot.getValue(Map::class.java) as? Map<String, Any?> ?: return
            
            // Don't process our own operations
            val sourceDeviceId = operationMap["deviceId"] as? String
            if (sourceDeviceId == currentDeviceId) return
            
            val message = SyncMessage(
                id = snapshot.key ?: UUID.randomUUID().toString(),
                type = SyncMessageType.CHECK_IN_BROADCAST,
                timestamp = (operationMap["timestamp"] as? Long) ?: System.currentTimeMillis(),
                sourceDeviceId = sourceDeviceId ?: "",
                eventId = operationMap["eventId"] as? String ?: "",
                payload = json.encodeToString(operationMap),
                priority = SyncPriority.NORMAL
            )
            
            _messageFlow.tryEmit(message)
        } catch (e: Exception) {
            // Log error
        }
    }
    
    /**
     * Setup listener for device list changes
     */
    private fun setupDeviceListListener(eventId: String) {
        val devicesRef = realtimeDb.getReference("events/$eventId/devices")
        
        devicesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val devices = mutableListOf<DeviceSyncState>()
                
                snapshot.children.forEach { deviceSnapshot ->
                    try {
                        val device = parseDeviceState(deviceSnapshot)
                        devices.add(device)
                    } catch (e: Exception) {
                        // Skip invalid device
                    }
                }
                
                // Emit device list update
                val message = SyncMessage(
                    type = SyncMessageType.STATE_UPDATE,
                    sourceDeviceId = "SYSTEM",
                    eventId = eventId,
                    payload = json.encodeToString(mapOf("devices" to devices))
                )
                _messageFlow.tryEmit(message)
            }
            
            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }
    
    /**
     * Setup connection state listener
     */
    private fun setupConnectionListener() {
        val connectedRef = realtimeDb.getReference(".info/connected")
        
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                _connectionState.value = connected
            }
            
            override fun onCancelled(error: DatabaseError) {
                _connectionState.value = false
            }
        })
    }
    
    /**
     * Submit a check-in operation
     */
    suspend fun submitCheckIn(
        eventId: String,
        deviceId: String,
        checkIn: SyncableCheckIn
    ): DomainSyncResult = withContext(Dispatchers.IO) {
        try {
            // Create operation
            val operation = SyncOperation(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                deviceId = deviceId,
                staffId = checkIn.checkedInBy,
                eventId = eventId,
                operationType = SyncOperationType.CHECK_IN,
                entityType = SyncEntityType.TICKET,
                entityId = checkIn.ticketId,
                payload = json.encodeToString(checkIn),
                version = 1,
                checksum = generateChecksum(checkIn),
                status = SyncOperationStatus.PENDING
            )
            
            // Try to acquire lock first
            val lockAcquired = acquireLock(eventId, operation.entityType, operation.entityId, deviceId)
            
            if (!lockAcquired) {
                return@withContext DomainSyncResult(
                    success = false,
                    operationId = operation.id,
                    error = SyncError(
                        code = SyncErrorCode.LOCK_NOT_GRANTED,
                        message = "Another device is processing this ticket",
                        recoverable = true
                    ),
                    requiresRetry = true,
                    retryDelay = 1000
                )
            }
            
            // Write to Firestore with transaction
            val result = writeOperationWithTransaction(eventId, operation)
            
            // Release lock
            releaseLock(eventId, operation.entityType, operation.entityId, deviceId)
            
            // Broadcast to other devices
            if (result.success) {
                broadcastOperation(eventId, operation)
            }
            
            result
        } catch (e: Exception) {
            DomainSyncResult(
                success = false,
                operationId = UUID.randomUUID().toString(),
                error = SyncError(
                    code = SyncErrorCode.SERVER_ERROR,
                    message = e.message ?: "Unknown error",
                    recoverable = true
                ),
                requiresRetry = true
            )
        }
    }
    
    /**
     * Write operation using Firestore transaction
     */
    private suspend fun writeOperationWithTransaction(
        eventId: String,
        operation: SyncOperation
    ): DomainSyncResult {
        val ticketRef = firestore.collection("events")
            .document(eventId)
            .collection("tickets")
            .document(operation.entityId)
        
        return suspendCancellableCoroutine { continuation ->
            firestore.runTransaction { transaction ->
                // Read current ticket state
                val snapshot = transaction.get(ticketRef)
                
                // Check if already checked in
                val currentStatus = snapshot.getString("status")
                if (currentStatus == "CHECKED_IN") {
                    // Conflict - ticket already checked in
                    return@runTransaction null
                }
                
                // Update ticket
                transaction.update(
                    ticketRef,
                    mapOf(
                        "status" to "CHECKED_IN",
                        "checkedInAt" to FieldValue.serverTimestamp(),
                        "checkedInBy" to operation.staffId,
                        "checkedInDevice" to operation.deviceId,
                        "version" to FieldValue.increment(1)
                    )
                )
                
                // Add check-in record
                val checkInRef = ticketRef.collection("checkIns").document()
                transaction.set(
                    checkInRef,
                    mapOf(
                        "operationId" to operation.id,
                        "timestamp" to FieldValue.serverTimestamp(),
                        "deviceId" to operation.deviceId,
                        "staffId" to operation.staffId
                    )
                )
                
                operation.id
            }.addOnSuccessListener { result ->
                if (result != null) {
                    continuation.resume(
                        DomainSyncResult(
                            success = true,
                            operationId = result,
                            newVersion = 1
                        )
                    )
                } else {
                    continuation.resume(
                        DomainSyncResult(
                            success = false,
                            operationId = operation.id,
                            error = SyncError(
                                code = SyncErrorCode.CONFLICT_DETECTED,
                                message = "Ticket already checked in",
                                recoverable = false
                            )
                        )
                    )
                }
            }.addOnFailureListener { e ->
                continuation.resume(
                    DomainSyncResult(
                        success = false,
                        operationId = operation.id,
                        error = SyncError(
                            code = SyncErrorCode.SERVER_ERROR,
                            message = e.message ?: "Transaction failed",
                            recoverable = true
                        ),
                        requiresRetry = true
                    )
                )
            }
        }
    }
    
    /**
     * Acquire distributed lock
     */
    private suspend fun acquireLock(
        eventId: String,
        entityType: SyncEntityType,
        entityId: String,
        deviceId: String
    ): Boolean {
        val lockId = "${entityType.name}_$entityId"
        val lockRef = realtimeDb.getReference("events/$eventId/locks/$lockId")
        
        return suspendCancellableCoroutine { continuation ->
            lockRef.runTransaction(object : FirebaseTransaction.Handler {
                override fun doTransaction(currentData: MutableData): FirebaseTransaction.Result {
                    val currentLock = currentData.value as? Map<String, Any?>
                    
                    if (currentLock == null) {
                        // No lock exists, acquire it
                        currentData.value = mapOf(
                            "lockedBy" to deviceId,
                            "lockedAt" to ServerValue.TIMESTAMP,
                            "expiresAt" to System.currentTimeMillis() + 30000
                        )
                        return FirebaseTransaction.success(currentData)
                    }
                    
                    val expiresAt = currentLock["expiresAt"] as? Long ?: 0
                    val lockedBy = currentLock["lockedBy"] as? String
                    
                    // Check if lock expired or we own it
                    if (expiresAt < System.currentTimeMillis() || lockedBy == deviceId) {
                        currentData.value = mapOf(
                            "lockedBy" to deviceId,
                            "lockedAt" to ServerValue.TIMESTAMP,
                            "expiresAt" to System.currentTimeMillis() + 30000
                        )
                        return FirebaseTransaction.success(currentData)
                    }
                    
                    // Lock is held by another device
                    return FirebaseTransaction.abort()
                }
                
                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    continuation.resume(committed)
                }
            })
        }
    }
    
    /**
     * Release distributed lock
     */
    private suspend fun releaseLock(
        eventId: String,
        entityType: SyncEntityType,
        entityId: String,
        deviceId: String
    ) {
        val lockId = "${entityType.name}_$entityId"
        val lockRef = realtimeDb.getReference("events/$eventId/locks/$lockId")
        
        // Only release if we own it
        lockRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lockedBy = snapshot.child("lockedBy").getValue(String::class.java)
                if (lockedBy == deviceId) {
                    lockRef.removeValue()
                }
            }
            
            override fun onCancelled(error: DatabaseError) {}
        })
    }
    
    /**
     * Broadcast operation to other devices
     */
    private fun broadcastOperation(eventId: String, operation: SyncOperation) {
        val opsRef = realtimeDb.getReference("events/$eventId/operations/${operation.id}")
        
        val operationData = mapOf(
            "id" to operation.id,
            "timestamp" to operation.timestamp,
            "deviceId" to operation.deviceId,
            "staffId" to operation.staffId,
            "eventId" to operation.eventId,
            "operationType" to operation.operationType.name,
            "entityType" to operation.entityType.name,
            "entityId" to operation.entityId,
            "payload" to operation.payload,
            "status" to operation.status.name
        )
        
        opsRef.setValue(operationData)
        
        // Remove after 5 minutes (cleanup)
        opsRef.removeValue().isComplete
    }
    
    /**
     * Get event sync state
     */
    suspend fun getEventSyncState(eventId: String): EventSyncState? {
        return suspendCancellableCoroutine { continuation ->
            val eventRef = firestore.collection("events").document(eventId)
            
            eventRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val state = EventSyncState(
                        eventId = eventId,
                        globalVersion = document.getLong("syncVersion") ?: 0,
                        lastModified = document.getLong("lastModified") ?: 0,
                        totalCheckIns = document.getLong("totalCheckIns")?.toInt() ?: 0,
                        pendingConflicts = document.getLong("pendingConflicts")?.toInt() ?: 0
                    )
                    continuation.resume(state)
                } else {
                    continuation.resume(null)
                }
            }.addOnFailureListener {
                continuation.resume(null)
            }
        }
    }
    
    /**
     * Get active devices for an event
     */
    suspend fun getActiveDevices(eventId: String): List<DeviceSyncState> {
        return suspendCancellableCoroutine { continuation ->
            val devicesRef = realtimeDb.getReference("events/$eventId/devices")
            
            devicesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val devices = mutableListOf<DeviceSyncState>()
                    val now = System.currentTimeMillis()
                    
                    snapshot.children.forEach { deviceSnapshot ->
                        try {
                            val device = parseDeviceState(deviceSnapshot)
                            // Only include devices with recent heartbeat (last 15 seconds)
                            if (now - device.lastHeartbeat < 15000) {
                                devices.add(device)
                            }
                        } catch (e: Exception) {
                            // Skip invalid device
                        }
                    }
                    
                    continuation.resume(devices)
                }
                
                override fun onCancelled(error: DatabaseError) {
                    continuation.resume(emptyList())
                }
            })
        }
    }
    
    /**
     * Parse device state from snapshot
     */
    private fun parseDeviceState(snapshot: DataSnapshot): DeviceSyncState {
        return DeviceSyncState(
            deviceId = snapshot.child("deviceId").getValue(String::class.java) ?: "",
            deviceName = snapshot.child("deviceName").getValue(String::class.java) ?: "",
            staffId = snapshot.child("staffId").getValue(String::class.java) ?: "",
            staffName = snapshot.child("staffName").getValue(String::class.java) ?: "",
            eventId = currentEventId ?: "",
            lastHeartbeat = snapshot.child("lastHeartbeat").getValue(Long::class.java) ?: 0,
            connectionStatus = try {
                ConnectionStatus.valueOf(
                    snapshot.child("connectionStatus").getValue(String::class.java) ?: "OFFLINE"
                )
            } catch (e: Exception) { ConnectionStatus.OFFLINE }
        )
    }
    
    /**
     * Disconnect and cleanup
     */
    fun disconnect() {
        // Remove presence
        devicePresenceRef?.removeValue()
        
        // Remove all listeners
        activeListeners.values.forEach { it.remove() }
        activeListeners.clear()
        
        // Clear refs
        devicePresenceRef = null
        operationsRef = null
        currentEventId = null
        currentDeviceId = null
        
        _connectionState.value = false
    }
    
    /**
     * Generate checksum for data integrity
     */
    private fun generateChecksum(data: Any): String {
        val jsonStr = json.encodeToString(data)
        return jsonStr.hashCode().toString(16)
    }
}

/**
 * Sync state data class
 */
data class RealTimeSyncState(
    val connected: Boolean,
    val eventId: String,
    val deviceId: String,
    val globalVersion: Long,
    val activeDevices: List<DeviceSyncState> = emptyList(),
    val pendingOperations: Int = 0
)
