package com.example.eventpay.domain.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Conflict Resolution Engine
 * 
 * Handles detection, analysis, and resolution of sync conflicts
 * in a multi-device check-in environment.
 * 
 * Resolution Strategies:
 * 1. First-Wins: The operation with the earliest timestamp wins
 * 2. Last-Wins: The operation with the latest timestamp wins
 * 3. Priority-Based: Operations from higher-priority sources win
 * 4. Merge: Combine compatible operations
 * 5. Manual: Escalate to admin for decision
 */
@Singleton
class ConflictResolutionEngine @Inject constructor(
    private val json: Json
) {
    // Active conflicts being tracked
    private val _activeConflicts = MutableStateFlow<Map<String, SyncConflict>>(emptyMap())
    val activeConflicts: StateFlow<Map<String, SyncConflict>> = _activeConflicts.asStateFlow()
    
    // Resolution history for audit
    private val _resolutionHistory = MutableStateFlow<List<ConflictResolution>>(emptyList())
    val resolutionHistory: StateFlow<List<ConflictResolution>> = _resolutionHistory.asStateFlow()
    
    // Pending resolutions waiting for manual intervention
    private val _pendingManualResolutions = MutableStateFlow<List<SyncConflict>>(emptyList())
    val pendingManualResolutions: StateFlow<List<SyncConflict>> = _pendingManualResolutions.asStateFlow()
    
    // Conflict statistics
    private val conflictStats = ConcurrentHashMap<ConflictType, Int>()
    
    // Configuration
    private var autoResolveEnabled = true
    private var manualTimeoutMs: Long = 60_000L // 1 minute
    
    /**
     * Detect if a conflict exists between operations
     */
    fun detectConflict(
        localOperation: SyncOperation,
        remoteOperation: SyncOperation
    ): SyncConflict? {
        // Check if operations affect the same entity
        if (localOperation.entityId != remoteOperation.entityId) {
            return null
        }
        
        // Check if operations are of conflicting types
        val conflictType = determineConflictType(localOperation, remoteOperation)
            ?: return null
        
        // Create conflict record
        val conflict = SyncConflict(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            eventId = localOperation.eventId,
            entityType = localOperation.entityType,
            entityId = localOperation.entityId,
            conflictType = conflictType,
            localOperation = localOperation,
            remoteOperation = remoteOperation,
            localDeviceId = localOperation.deviceId,
            remoteDeviceId = remoteOperation.deviceId,
            status = ConflictStatus.DETECTED
        )
        
        // Track the conflict
        _activeConflicts.value = _activeConflicts.value + (conflict.id to conflict)
        updateConflictStats(conflictType)
        
        return conflict
    }
    
    /**
     * Analyze a conflict and determine the best resolution strategy
     */
    fun analyzeConflict(conflict: SyncConflict): ConflictAnalysis {
        val analysis = when (conflict.conflictType) {
            ConflictType.CONCURRENT_CHECK_IN -> {
                analyzeConcurrentCheckIn(conflict)
            }
            ConflictType.VERSION_MISMATCH -> {
                analyzeVersionMismatch(conflict)
            }
            ConflictType.DATA_DIVERGENCE -> {
                analyzeDataDivergence(conflict)
            }
            ConflictType.DELETE_MODIFY -> {
                analyzeDeleteModify(conflict)
            }
            ConflictType.DOUBLE_CHECK_IN -> {
                analyzeDoubleCheckIn(conflict)
            }
            ConflictType.SHIFT_OVERLAP -> {
                analyzeShiftOverlap(conflict)
            }
            ConflictType.PERMISSION_CONFLICT -> {
                analyzePermissionConflict(conflict)
            }
        }
        
        // Update conflict status
        updateConflictStatus(conflict.id, ConflictStatus.ANALYZING)
        
        return analysis
    }
    
    /**
     * Automatically resolve a conflict if possible
     */
    fun autoResolve(conflict: SyncConflict): SyncConflict? {
        if (!autoResolveEnabled) {
            return escalateToManual(conflict)
        }
        
        val analysis = analyzeConflict(conflict)
        
        // Check if auto-resolution is possible
        if (!analysis.canAutoResolve) {
            return escalateToManual(conflict)
        }
        
        val resolution = when (analysis.recommendedStrategy) {
            ResolutionStrategy.FIRST_WINS -> resolveFirstWins(conflict)
            ResolutionStrategy.LAST_WINS -> resolveLastWins(conflict)
            ResolutionStrategy.MERGE -> resolveMerge(conflict, analysis.mergeData)
            ResolutionStrategy.PRIORITY_BASED -> resolvePriorityBased(conflict, analysis.priorityData)
            ResolutionStrategy.REJECT_BOTH -> resolveRejectBoth(conflict, analysis.rejectionReason ?: "No reason provided")
            ResolutionStrategy.MANUAL -> null // Will be handled by returning null
        }
        
        if (resolution != null) {
            val resolvedConflict = conflict.copy(
                status = ConflictStatus.RESOLVED,
                resolution = resolution,
                resolvedAt = System.currentTimeMillis(),
                resolvedBy = "AUTO"
            )
            
            // Update tracking
            _activeConflicts.value = _activeConflicts.value - conflict.id
            _resolutionHistory.value = _resolutionHistory.value + resolution
            
            return resolvedConflict
        }
        
        // Handle manual escalation
        return escalateToManual(conflict)
    }
    
    /**
     * Manually resolve a conflict
     */
    fun manualResolve(
        conflictId: String,
        resolution: ConflictResolution.ManualResolution
    ): SyncConflict? {
        val conflict = _activeConflicts.value[conflictId] ?: return null
        
        val resolvedConflict = conflict.copy(
            status = ConflictStatus.RESOLVED,
            resolution = resolution,
            resolvedAt = System.currentTimeMillis(),
            resolvedBy = resolution.resolvedBy
        )
        
        // Update tracking
        _activeConflicts.value = _activeConflicts.value - conflictId
        _resolutionHistory.value = _resolutionHistory.value + resolution
        _pendingManualResolutions.value = _pendingManualResolutions.value.filter { it.id != conflictId }
        
        return resolvedConflict
    }
    
    /**
     * Resolve using first-wins strategy
     */
    private fun resolveFirstWins(conflict: SyncConflict): ConflictResolution.FirstWins {
        val winningOp = if (conflict.localOperation.timestamp < conflict.remoteOperation.timestamp) {
            conflict.localOperation
        } else {
            conflict.remoteOperation
        }
        
        return ConflictResolution.FirstWins(
            winningOperationId = winningOp.id
        )
    }
    
    /**
     * Resolve using last-wins strategy
     */
    private fun resolveLastWins(conflict: SyncConflict): ConflictResolution.LastWins {
        val winningOp = if (conflict.localOperation.timestamp >= conflict.remoteOperation.timestamp) {
            conflict.localOperation
        } else {
            conflict.remoteOperation
        }
        
        return ConflictResolution.LastWins(
            winningOperationId = winningOp.id
        )
    }
    
    /**
     * Resolve by merging operations
     */
    private fun resolveMerge(
        conflict: SyncConflict,
        mergeData: MergeData?
    ): ConflictResolution.Merge? {
        if (mergeData == null) return null
        
        return ConflictResolution.Merge(
            mergedPayload = mergeData.mergedPayload,
            sourceOperations = listOf(conflict.localOperation.id, conflict.remoteOperation.id)
        )
    }
    
    /**
     * Resolve based on priority
     */
    private fun resolvePriorityBased(
        conflict: SyncConflict,
        priorityData: PriorityData?
    ): ConflictResolution.FirstWins? {
        if (priorityData == null) return null
        
        val winningOp = if (priorityData.winningDeviceId == conflict.localDeviceId) {
            conflict.localOperation
        } else {
            conflict.remoteOperation
        }
        
        return ConflictResolution.FirstWins(
            winningOperationId = winningOp.id
        )
    }
    
    /**
     * Resolve by rejecting both operations
     */
    private fun resolveRejectBoth(
        conflict: SyncConflict,
        reason: String
    ): ConflictResolution.RejectBoth {
        return ConflictResolution.RejectBoth(reason = reason)
    }
    
    /**
     * Escalate conflict to manual resolution
     */
    private fun escalateToManual(conflict: SyncConflict): SyncConflict {
        val escalatedConflict = conflict.copy(status = ConflictStatus.PENDING_RESOLUTION)
        
        _pendingManualResolutions.value = _pendingManualResolutions.value + escalatedConflict
        updateConflictStatus(conflict.id, ConflictStatus.PENDING_RESOLUTION)
        
        return escalatedConflict
    }
    
    // ========================================================================
    // CONFLICT TYPE ANALYSIS
    // ========================================================================
    
    private fun determineConflictType(
        local: SyncOperation,
        remote: SyncOperation
    ): ConflictType? {
        // Both are check-ins for the same ticket
        if (local.operationType == SyncOperationType.CHECK_IN && 
            remote.operationType == SyncOperationType.CHECK_IN) {
            
            // Check if within the same time window (concurrent)
            val timeDiff = kotlin.math.abs(local.timestamp - remote.timestamp)
            if (timeDiff < CONCURRENT_THRESHOLD_MS) {
                return ConflictType.CONCURRENT_CHECK_IN
            }
            
            return ConflictType.DOUBLE_CHECK_IN
        }
        
        // Version mismatch
        if (local.version != remote.version) {
            return ConflictType.VERSION_MISMATCH
        }
        
        // Data divergence
        if (local.payload != remote.payload) {
            return ConflictType.DATA_DIVERGENCE
        }
        
        return null
    }
    
    private fun analyzeConcurrentCheckIn(conflict: SyncConflict): ConflictAnalysis {
        // For concurrent check-ins, use first-wins by default
        // The first device to scan should win
        return ConflictAnalysis(
            conflictId = conflict.id,
            conflictType = conflict.conflictType,
            canAutoResolve = true,
            recommendedStrategy = ResolutionStrategy.FIRST_WINS,
            confidence = 0.95,
            reasoning = "Concurrent check-in detected. First scan wins to prevent duplicate entry.",
            affectedEntities = listOf(conflict.entityId),
            riskLevel = RiskLevel.MEDIUM
        )
    }
    
    private fun analyzeVersionMismatch(conflict: SyncConflict): ConflictAnalysis {
        // Version mismatch usually indicates stale data
        // The higher version should win
        val localVersion = conflict.localOperation.version
        val remoteVersion = conflict.remoteOperation.version
        
        return ConflictAnalysis(
            conflictId = conflict.id,
            conflictType = conflict.conflictType,
            canAutoResolve = true,
            recommendedStrategy = if (remoteVersion > localVersion) {
                ResolutionStrategy.LAST_WINS
            } else {
                ResolutionStrategy.FIRST_WINS
            },
            confidence = 0.90,
            reasoning = "Version mismatch detected. Higher version indicates more recent data.",
            affectedEntities = listOf(conflict.entityId),
            riskLevel = RiskLevel.LOW
        )
    }
    
    private fun analyzeDataDivergence(conflict: SyncConflict): ConflictAnalysis {
        // Data divergence requires careful analysis
        // Try to merge if possible
        val mergeData = attemptMerge(conflict)
        
        return ConflictAnalysis(
            conflictId = conflict.id,
            conflictType = conflict.conflictType,
            canAutoResolve = mergeData != null,
            recommendedStrategy = if (mergeData != null) {
                ResolutionStrategy.MERGE
            } else {
                ResolutionStrategy.MANUAL
            },
            confidence = if (mergeData != null) 0.85 else 0.50,
            reasoning = if (mergeData != null) {
                "Data divergence can be merged automatically."
            } else {
                "Data divergence requires manual review."
            },
            affectedEntities = listOf(conflict.entityId),
            riskLevel = RiskLevel.HIGH,
            mergeData = mergeData
        )
    }
    
    private fun analyzeDeleteModify(conflict: SyncConflict): ConflictAnalysis {
        // Delete-modify conflicts are tricky
        // Usually the modification should be preserved
        return ConflictAnalysis(
            conflictId = conflict.id,
            conflictType = conflict.conflictType,
            canAutoResolve = false,
            recommendedStrategy = ResolutionStrategy.MANUAL,
            confidence = 0.60,
            reasoning = "Delete-modify conflict requires manual review to prevent data loss.",
            affectedEntities = listOf(conflict.entityId),
            riskLevel = RiskLevel.HIGH
        )
    }
    
    private fun analyzeDoubleCheckIn(conflict: SyncConflict): ConflictAnalysis {
        // Double check-in means the ticket was already scanned
        // This should be rejected
        return ConflictAnalysis(
            conflictId = conflict.id,
            conflictType = conflict.conflictType,
            canAutoResolve = true,
            recommendedStrategy = ResolutionStrategy.FIRST_WINS,
            confidence = 1.0,
            reasoning = "Ticket already checked in. First check-in is valid, second is rejected.",
            affectedEntities = listOf(conflict.entityId),
            riskLevel = RiskLevel.LOW
        )
    }
    
    private fun analyzeShiftOverlap(conflict: SyncConflict): ConflictAnalysis {
        // Shift overlap requires manual resolution
        return ConflictAnalysis(
            conflictId = conflict.id,
            conflictType = conflict.conflictType,
            canAutoResolve = false,
            recommendedStrategy = ResolutionStrategy.MANUAL,
            confidence = 0.70,
            reasoning = "Shift overlap requires supervisor review.",
            affectedEntities = listOf(conflict.entityId),
            riskLevel = RiskLevel.MEDIUM
        )
    }
    
    private fun analyzePermissionConflict(conflict: SyncConflict): ConflictAnalysis {
        // Permission conflicts need admin review
        return ConflictAnalysis(
            conflictId = conflict.id,
            conflictType = conflict.conflictType,
            canAutoResolve = false,
            recommendedStrategy = ResolutionStrategy.MANUAL,
            confidence = 0.50,
            reasoning = "Permission conflict requires admin review.",
            affectedEntities = listOf(conflict.entityId),
            riskLevel = RiskLevel.HIGH
        )
    }
    
    // ========================================================================
    // HELPER METHODS
    // ========================================================================
    
    private fun attemptMerge(conflict: SyncConflict): MergeData? {
        // Try to merge the payloads if they're compatible
        return try {
            val localPayload = conflict.localOperation.payload
            val remotePayload = conflict.remoteOperation.payload
            
            // For check-ins, we can't really merge - one wins
            if (conflict.conflictType == ConflictType.CONCURRENT_CHECK_IN ||
                conflict.conflictType == ConflictType.DOUBLE_CHECK_IN) {
                return null
            }
            
            // For other types, attempt a smart merge
            // This is a simplified example - real implementation would be more sophisticated
            val merged = mapOf(
                "local" to localPayload,
                "remote" to remotePayload,
                "merged_at" to System.currentTimeMillis().toString()
            )
            
            MergeData(
                mergedPayload = json.encodeToString(merged),
                mergeStrategy = "COMBINE"
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun updateConflictStatus(conflictId: String, status: ConflictStatus) {
        _activeConflicts.value[conflictId]?.let { conflict ->
            _activeConflicts.value = _activeConflicts.value + (conflictId to conflict.copy(status = status))
        }
    }
    
    private fun updateConflictStats(type: ConflictType) {
        conflictStats[type] = (conflictStats[type] ?: 0) + 1
    }
    
    /**
     * Get conflict statistics
     */
    fun getConflictStatistics(): Map<ConflictType, Int> {
        return conflictStats.toMap()
    }
    
    /**
     * Clear resolved conflicts older than specified age
     */
    fun cleanupOldConflicts(maxAgeMs: Long = 24 * 60 * 60 * 1000L) {
        val cutoff = System.currentTimeMillis() - maxAgeMs
        
        _activeConflicts.value = _activeConflicts.value.filter { (_, conflict) ->
            conflict.timestamp > cutoff && conflict.status != ConflictStatus.RESOLVED
        }
        
        _resolutionHistory.value = _resolutionHistory.value.filter { resolution ->
            resolution.timestamp > cutoff
        }
    }
    
    /**
     * Configure the engine
     */
    fun configure(
        autoResolve: Boolean = true,
        manualTimeout: Long = 60_000L
    ) {
        autoResolveEnabled = autoResolve
        manualTimeoutMs = manualTimeout
    }
    
    companion object {
        // Time threshold for considering operations concurrent (in milliseconds)
        private const val CONCURRENT_THRESHOLD_MS = 5000L // 5 seconds
    }
}

// ============================================================================
// SUPPORTING DATA CLASSES
// ============================================================================

/**
 * Analysis result for a conflict
 */
data class ConflictAnalysis(
    val conflictId: String,
    val conflictType: ConflictType,
    val canAutoResolve: Boolean,
    val recommendedStrategy: ResolutionStrategy,
    val confidence: Double,  // 0.0 to 1.0
    val reasoning: String,
    val affectedEntities: List<String>,
    val riskLevel: RiskLevel,
    val mergeData: MergeData? = null,
    val priorityData: PriorityData? = null,
    val rejectionReason: String? = null
)

/**
 * Resolution strategies
 */
enum class ResolutionStrategy {
    FIRST_WINS,         // Earliest timestamp wins
    LAST_WINS,          // Latest timestamp wins
    MERGE,              // Merge both operations
    PRIORITY_BASED,     // Higher priority wins
    REJECT_BOTH,        // Reject both operations
    MANUAL              // Requires manual resolution
}

/**
 * Risk levels for conflicts
 */
enum class RiskLevel {
    LOW,        // Can be auto-resolved safely
    MEDIUM,     // Auto-resolve with caution
    HIGH        // Requires manual review
}

/**
 * Data for merge resolution
 */
data class MergeData(
    val mergedPayload: String,
    val mergeStrategy: String
)

/**
 * Data for priority-based resolution
 */
data class PriorityData(
    val winningDeviceId: String,
    val winningPriority: Int,
    val reason: String
)
