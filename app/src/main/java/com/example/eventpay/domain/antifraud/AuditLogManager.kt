package com.example.eventpay.domain.antifraud

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Audit Log Manager
 * 
 * Comprehensive audit logging system for security events and compliance.
 * 
 * Features:
 * - Immutable log entries with integrity verification
 * - Tamper detection via checksum chains
 * - Configurable retention policies
 * - Real-time log streaming
 * - Search and filtering capabilities
 * - Export for compliance reporting
 */
@Singleton
class AuditLogManager @Inject constructor(
    private val context: Context
) {
    
    private val _logEntries = MutableStateFlow<List<AuditLogEntry>>(emptyList())
    val logEntries: StateFlow<List<AuditLogEntry>> = _logEntries.asStateFlow()
    
    private val _recentAlerts = MutableStateFlow<List<SuspiciousActivityAlert>>(emptyList())
    val recentAlerts: StateFlow<List<SuspiciousActivityAlert>> = _recentAlerts.asStateFlow()
    
    // In-memory storage (would use database in production)
    private val logStorage = mutableListOf<AuditLogEntry>()
    private val alertStorage = mutableListOf<SuspiciousActivityAlert>()
    private var lastChecksum = "GENESIS"
    
    private val config = AuditLogConfig()
    
    /**
     * Log an audit event
     * 
     * @param eventType Type of audit event
     * @param severity Severity level
     * @param actorType Type of actor performing the action
     * @param actorId ID of the actor
     * @param action The action performed
     * @param actionDetails Additional details about the action
     * @param targetType Type of target (optional)
     * @param targetId ID of target (optional)
     * @param result Result of the action
     * @param metadata Additional metadata
     * @return The created audit log entry
     */
    suspend fun logEvent(
        eventType: AuditEventType,
        severity: AuditSeverity,
        actorType: ActorType,
        actorId: String,
        action: String,
        actionDetails: Map<String, String> = emptyMap(),
        targetType: TargetType? = null,
        targetId: String? = null,
        result: ActionResult,
        resultMessage: String? = null,
        metadata: Map<String, String> = emptyMap(),
        actorIp: String? = null,
        actorDevice: String? = null,
        actorLocation: String? = null,
        sessionId: String? = null,
        requestId: String? = null,
        previousValue: String? = null,
        newValue: String? = null
    ): AuditLogEntry = withContext(Dispatchers.Default) {
        
        val timestamp = System.currentTimeMillis()
        
        // Create entry with checksum chain
        val entry = AuditLogEntry(
            id = UUID.randomUUID().toString(),
            timestamp = timestamp,
            eventType = eventType,
            severity = severity,
            actorType = actorType,
            actorId = actorId,
            actorIp = actorIp,
            actorDevice = actorDevice,
            actorLocation = actorLocation,
            action = action,
            actionDetails = actionDetails,
            targetType = targetType,
            targetId = targetId,
            result = result,
            sessionId = sessionId,
            requestId = requestId,
            metadata = metadata,
            previousValue = previousValue,
            newValue = newValue,
            checksum = calculateChecksum(timestamp, eventType, actorId, action, lastChecksum)
        )
        
        // Update chain
        lastChecksum = entry.checksum
        
        // Store entry
        logStorage.add(entry)
        
        // Update state flow
        _logEntries.value = logStorage.toList().takeLast(config.maxFlowEntries)
        
        // Check for alerts
        checkForAlerts(entry)
        
        // Clean old entries
        cleanOldEntries()
        
        entry
    }
    
    /**
     * Log a QR code scan event
     */
    suspend fun logQRScan(
        ticketId: String,
        eventId: String,
        userId: String,
        deviceId: String,
        result: ActionResult,
        resultMessage: String? = null,
        fraudDetected: Boolean = false,
        metadata: Map<String, String> = emptyMap()
    ): AuditLogEntry {
        return logEvent(
            eventType = if (fraudDetected) AuditEventType.FRAUD_DETECTED else AuditEventType.QR_SCANNED,
            severity = if (fraudDetected) AuditSeverity.WARNING else AuditSeverity.INFO,
            actorType = ActorType.USER,
            actorId = userId,
            action = "QR_SCAN",
            actionDetails = mapOf(
                "ticketId" to ticketId,
                "eventId" to eventId,
                "fraudDetected" to fraudDetected.toString()
            ) + metadata,
            targetType = TargetType.TICKET,
            targetId = ticketId,
            result = result,
            resultMessage = resultMessage,
            actorDevice = deviceId
        )
    }
    
    /**
     * Log a fraud detection event
     */
    suspend fun logFraudDetection(
        fraudResult: FraudDetectionResult,
        deviceId: String
    ): AuditLogEntry {
        return logEvent(
            eventType = AuditEventType.FRAUD_DETECTED,
            severity = when (fraudResult.severity) {
                FraudSeverity.LOW -> AuditSeverity.NOTICE
                FraudSeverity.MEDIUM -> AuditSeverity.WARNING
                FraudSeverity.HIGH -> AuditSeverity.ERROR
                FraudSeverity.CRITICAL -> AuditSeverity.CRITICAL
            },
            actorType = ActorType.AUTOMATED_PROCESS,
            actorId = "FRAUD_DETECTION_ENGINE",
            action = "FRAUD_DETECTED",
            actionDetails = mapOf(
                "fraudType" to fraudResult.fraudType.name,
                "riskScore" to fraudResult.riskScore.toString(),
                "confidence" to fraudResult.confidence.toString(),
                "indicators" to fraudResult.indicators.size.toString()
            ),
            targetType = if (fraudResult.ticketId != null) TargetType.TICKET else TargetType.USER,
            targetId = fraudResult.ticketId ?: fraudResult.userId ?: "UNKNOWN",
            result = ActionResult.BLOCKED,
            actorDevice = deviceId,
            metadata = mapOf(
                "fraudResultId" to fraudResult.id
            )
        )
    }
    
    /**
     * Log a security alert
     */
    suspend fun logSecurityAlert(
        alert: SuspiciousActivityAlert
    ): AuditLogEntry {
        // Store alert
        alertStorage.add(alert)
        _recentAlerts.value = alertStorage.toList().takeLast(50)
        
        return logEvent(
            eventType = AuditEventType.SECURITY_ALERT,
            severity = when (alert.severity) {
                FraudSeverity.LOW -> AuditSeverity.NOTICE
                FraudSeverity.MEDIUM -> AuditSeverity.WARNING
                FraudSeverity.HIGH -> AuditSeverity.ERROR
                FraudSeverity.CRITICAL -> AuditSeverity.ALERT
            },
            actorType = ActorType.SYSTEM,
            actorId = "SECURITY_SYSTEM",
            action = "SECURITY_ALERT",
            actionDetails = mapOf(
                "alertType" to alert.alertType.name,
                "title" to alert.title
            ),
            targetType = TargetType.TICKET,
            targetId = alert.ticketId,
            result = ActionResult.SUCCESS,
            metadata = mapOf(
                "alertId" to alert.id
            )
        )
    }
    
    /**
     * Log a ticket check-in
     */
    suspend fun logCheckIn(
        ticketId: String,
        eventId: String,
        userId: String,
        deviceId: String,
        checkedInBy: String,
        result: ActionResult,
        resultMessage: String? = null
    ): AuditLogEntry {
        return logEvent(
            eventType = AuditEventType.TICKET_CHECKED_IN,
            severity = AuditSeverity.NOTICE,
            actorType = ActorType.USER,
            actorId = checkedInBy,
            action = "CHECK_IN",
            actionDetails = mapOf(
                "ticketId" to ticketId,
                "eventId" to eventId,
                "userId" to userId
            ),
            targetType = TargetType.TICKET,
            targetId = ticketId,
            result = result,
            resultMessage = resultMessage,
            actorDevice = deviceId
        )
    }
    
    /**
     * Log a screenshot detection
     */
    suspend fun logScreenshotDetection(
        result: ScreenshotDetectionResult
    ): AuditLogEntry {
        return logEvent(
            eventType = AuditEventType.SECURITY_ALERT,
            severity = AuditSeverity.WARNING,
            actorType = ActorType.USER,
            actorId = result.userId ?: "UNKNOWN",
            action = "SCREENSHOT_DETECTED",
            actionDetails = mapOf(
                "detectionMethod" to result.detectionMethod.name,
                "confidence" to result.confidence.toString(),
                "actionTaken" to (result.actionTaken?.name ?: "NONE")
            ),
            targetType = TargetType.TICKET,
            targetId = result.ticketId,
            result = ActionResult.SUCCESS,
            actorDevice = result.deviceId
        )
    }
    
    /**
     * Log authentication event
     */
    suspend fun logAuthEvent(
        eventType: AuditEventType,
        userId: String,
        result: ActionResult,
        deviceId: String? = null,
        ipAddress: String? = null,
        metadata: Map<String, String> = emptyMap()
    ): AuditLogEntry {
        return logEvent(
            eventType = eventType,
            severity = when (result) {
                ActionResult.SUCCESS -> AuditSeverity.INFO
                ActionResult.FAILURE -> AuditSeverity.WARNING
                else -> AuditSeverity.NOTICE
            },
            actorType = ActorType.USER,
            actorId = userId,
            action = eventType.name,
            targetType = TargetType.USER,
            targetId = userId,
            result = result,
            actorDevice = deviceId,
            actorIp = ipAddress,
            metadata = metadata
        )
    }
    
    /**
     * Get log entries by type
     */
    fun getEntriesByType(eventType: AuditEventType): List<AuditLogEntry> =
        logStorage.filter { it.eventType == eventType }
    
    /**
     * Get log entries by actor
     */
    fun getEntriesByActor(actorId: String): List<AuditLogEntry> =
        logStorage.filter { it.actorId == actorId }
    
    /**
     * Get log entries by target
     */
    fun getEntriesByTarget(targetId: String): List<AuditLogEntry> =
        logStorage.filter { it.targetId == targetId }
    
    /**
     * Get log entries by time range
     */
    fun getEntriesByTimeRange(startTime: Long, endTime: Long): List<AuditLogEntry> =
        logStorage.filter { it.timestamp in startTime..endTime }
    
    /**
     * Get log entries by severity
     */
    fun getEntriesBySeverity(minSeverity: AuditSeverity): List<AuditLogEntry> {
        val severityOrder = listOf(
            AuditSeverity.DEBUG,
            AuditSeverity.INFO,
            AuditSeverity.NOTICE,
            AuditSeverity.WARNING,
            AuditSeverity.ERROR,
            AuditSeverity.CRITICAL,
            AuditSeverity.ALERT
        )
        val minIndex = severityOrder.indexOf(minSeverity)
        return logStorage.filter { 
            severityOrder.indexOf(it.severity) >= minIndex 
        }
    }
    
    /**
     * Search log entries
     */
    fun searchEntries(query: String): List<AuditLogEntry> {
        val lowerQuery = query.lowercase()
        return logStorage.filter { entry ->
            entry.action.lowercase().contains(lowerQuery) ||
            entry.actorId.lowercase().contains(lowerQuery) ||
            entry.targetId?.lowercase()?.contains(lowerQuery) == true ||
            entry.actionDetails.values.any { it.lowercase().contains(lowerQuery) }
        }
    }
    
    /**
     * Verify log integrity
     * Checks that the checksum chain is valid
     */
    fun verifyIntegrity(): IntegrityCheckResult {
        var previousChecksum = "GENESIS"
        var validCount = 0
        var invalidCount = 0
        val invalidEntries = mutableListOf<String>()
        
        for (entry in logStorage) {
            val expectedChecksum = calculateChecksum(
                entry.timestamp,
                entry.eventType,
                entry.actorId,
                entry.action,
                previousChecksum
            )
            
            if (entry.checksum == expectedChecksum) {
                validCount++
                previousChecksum = entry.checksum
            } else {
                invalidCount++
                invalidEntries.add(entry.id)
            }
        }
        
        return IntegrityCheckResult(
            isValid = invalidCount == 0,
            totalEntries = logStorage.size,
            validEntries = validCount,
            invalidEntries = invalidCount,
            invalidEntryIds = invalidEntries
        )
    }
    
    /**
     * Export logs for compliance
     */
    suspend fun exportLogs(
        startTime: Long? = null,
        endTime: Long? = null,
        format: ExportFormat = ExportFormat.JSON
    ): String = withContext(Dispatchers.Default) {
        val entries = if (startTime != null && endTime != null) {
            getEntriesByTimeRange(startTime, endTime)
        } else {
            logStorage.toList()
        }
        
        when (format) {
            ExportFormat.JSON -> exportAsJson(entries)
            ExportFormat.CSV -> exportAsCsv(entries)
            ExportFormat.XML -> exportAsXml(entries)
        }
    }
    
    /**
     * Get statistics about audit logs
     */
    fun getStatistics(): AuditStatistics {
        val now = System.currentTimeMillis()
        val last24h = now - (24 * 60 * 60 * 1000)
        val last7d = now - (7 * 24 * 60 * 60 * 1000)
        val last30d = now - (30 * 24 * 60 * 60 * 1000)
        
        return AuditStatistics(
            totalEntries = logStorage.size,
            entriesLast24h = logStorage.count { it.timestamp >= last24h },
            entriesLast7d = logStorage.count { it.timestamp >= last7d },
            entriesLast30d = logStorage.count { it.timestamp >= last30d },
            entriesByType = logStorage.groupingBy { it.eventType }.eachCount(),
            entriesBySeverity = logStorage.groupingBy { it.severity }.eachCount(),
            entriesByResult = logStorage.groupingBy { it.result }.eachCount(),
            fraudEventsCount = logStorage.count { 
                it.eventType == AuditEventType.FRAUD_DETECTED || 
                it.eventType == AuditEventType.FRAUD_CONFIRMED 
            },
            securityAlertsCount = alertStorage.size
        )
    }
    
    /**
     * Create a suspicious activity alert
     */
    suspend fun createAlert(
        alertType: AlertType,
        severity: FraudSeverity,
        title: String,
        description: String,
        ticketId: String? = null,
        eventId: String? = null,
        userId: String? = null,
        deviceId: String? = null,
        indicators: List<String> = emptyList(),
        recommendedActions: List<String> = emptyList(),
        fraudResultId: String? = null
    ): SuspiciousActivityAlert {
        val alert = SuspiciousActivityAlert(
            alertType = alertType,
            severity = severity,
            trigger = AlertTrigger(
                source = DetectionSource.AUTOMATED_SYSTEM,
                fraudResultId = fraudResultId
            ),
            ticketId = ticketId,
            eventId = eventId,
            userId = userId,
            deviceId = deviceId,
            title = title,
            description = description,
            indicators = indicators,
            recommendedActions = recommendedActions
        )
        
        alertStorage.add(alert)
        _recentAlerts.value = alertStorage.toList().takeLast(50)
        
        logSecurityAlert(alert)
        
        return alert
    }
    
    /**
     * Resolve an alert
     */
    suspend fun resolveAlert(
        alertId: String,
        resolvedBy: String,
        resolution: String,
        resolutionType: ResolutionType
    ): Boolean {
        val index = alertStorage.indexOfFirst { it.id == alertId }
        if (index == -1) return false
        
        val alert = alertStorage[index]
        alertStorage[index] = alert.copy(
            status = AlertStatus.RESOLVED,
            resolvedAt = System.currentTimeMillis(),
            resolvedBy = resolvedBy,
            resolution = resolution,
            resolutionType = resolutionType
        )
        
        _recentAlerts.value = alertStorage.toList().takeLast(50)
        
        logEvent(
            eventType = AuditEventType.FRAUD_RESOLVED,
            severity = AuditSeverity.INFO,
            actorType = ActorType.ADMIN,
            actorId = resolvedBy,
            action = "ALERT_RESOLVED",
            targetType = TargetType.TICKET,
            targetId = alert.ticketId,
            result = ActionResult.SUCCESS,
            metadata = mapOf(
                "alertId" to alertId,
                "resolutionType" to resolutionType.name
            )
        )
        
        return true
    }
    
    /**
     * Get alert by ID
     */
    fun getAlert(alertId: String): SuspiciousActivityAlert? =
        alertStorage.find { it.id == alertId }
    
    /**
     * Get alerts by status
     */
    fun getAlertsByStatus(status: AlertStatus): List<SuspiciousActivityAlert> =
        alertStorage.filter { it.status == status }
    
    // ============================================================================
    // PRIVATE METHODS
    // ============================================================================
    
    private fun calculateChecksum(
        timestamp: Long,
        eventType: AuditEventType,
        actorId: String,
        action: String,
        previousChecksum: String
    ): String {
        val data = "$timestamp:$eventType:$actorId:$action:$previousChecksum"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data.toByteArray())
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }
    
    private fun checkForAlerts(entry: AuditLogEntry) {
        // Check for patterns that should trigger alerts
        when (entry.eventType) {
            AuditEventType.FRAUD_DETECTED -> {
                // Auto-create alert for fraud detection
                // This would be handled by the alert system
            }
            AuditEventType.ACCESS_DENIED -> {
                // Check for repeated access denied
                val recentDenied = logStorage.count {
                    it.eventType == AuditEventType.ACCESS_DENIED &&
                    it.actorId == entry.actorId &&
                    it.timestamp > System.currentTimeMillis() - 300000 // 5 minutes
                }
                if (recentDenied >= 3) {
                    // Would trigger alert
                }
            }
            else -> {}
        }
    }
    
    private fun cleanOldEntries() {
        if (config.maxEntries > 0 && logStorage.size > config.maxEntries) {
            // Remove oldest entries
            val toRemove = logStorage.size - config.maxEntries
            repeat(toRemove) {
                if (logStorage.isNotEmpty()) {
                    logStorage.removeAt(0)
                }
            }
        }
    }
    
    private fun exportAsJson(entries: List<AuditLogEntry>): String {
        val sb = StringBuilder()
        sb.append("[")
        entries.forEachIndexed { index, entry ->
            if (index > 0) sb.append(",")
            sb.append("\n  {")
            sb.append("\n    \"id\": \"${entry.id}\",")
            sb.append("\n    \"timestamp\": ${entry.timestamp},")
            sb.append("\n    \"eventType\": \"${entry.eventType}\",")
            sb.append("\n    \"severity\": \"${entry.severity}\",")
            sb.append("\n    \"actorType\": \"${entry.actorType}\",")
            sb.append("\n    \"actorId\": \"${entry.actorId}\",")
            sb.append("\n    \"action\": \"${entry.action}\",")
            sb.append("\n    \"result\": \"${entry.result}\"")
            sb.append("\n  }")
        }
        sb.append("\n]")
        return sb.toString()
    }
    
    private fun exportAsCsv(entries: List<AuditLogEntry>): String {
        val sb = StringBuilder()
        sb.append("id,timestamp,eventType,severity,actorType,actorId,action,result\n")
        entries.forEach { entry ->
            sb.append("${entry.id},")
            sb.append("${entry.timestamp},")
            sb.append("${entry.eventType},")
            sb.append("${entry.severity},")
            sb.append("${entry.actorType},")
            sb.append("${entry.actorId},")
            sb.append("${entry.action},")
            sb.append("${entry.result}\n")
        }
        return sb.toString()
    }
    
    private fun exportAsXml(entries: List<AuditLogEntry>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<auditLogs>\n")
        entries.forEach { entry ->
            sb.append("  <entry>\n")
            sb.append("    <id>${entry.id}</id>\n")
            sb.append("    <timestamp>${entry.timestamp}</timestamp>\n")
            sb.append("    <eventType>${entry.eventType}</eventType>\n")
            sb.append("    <severity>${entry.severity}</severity>\n")
            sb.append("    <actorType>${entry.actorType}</actorType>\n")
            sb.append("    <actorId>${entry.actorId}</actorId>\n")
            sb.append("    <action>${entry.action}</action>\n")
            sb.append("    <result>${entry.result}</result>\n")
            sb.append("  </entry>\n")
        }
        sb.append("</auditLogs>")
        return sb.toString()
    }
}

/**
 * Audit log configuration
 */
data class AuditLogConfig(
    val maxEntries: Int = 10000,
    val maxFlowEntries: Int = 100,
    val retentionDays: Int = 90,
    val enableIntegrityCheck: Boolean = true
)

/**
 * Integrity check result
 */
data class IntegrityCheckResult(
    val isValid: Boolean,
    val totalEntries: Int,
    val validEntries: Int,
    val invalidEntries: Int,
    val invalidEntryIds: List<String>
)

/**
 * Export format options
 */
enum class ExportFormat {
    JSON,
    CSV,
    XML
}

/**
 * Audit statistics
 */
data class AuditStatistics(
    val totalEntries: Int,
    val entriesLast24h: Int,
    val entriesLast7d: Int,
    val entriesLast30d: Int,
    val entriesByType: Map<AuditEventType, Int>,
    val entriesBySeverity: Map<AuditSeverity, Int>,
    val entriesByResult: Map<ActionResult, Int>,
    val fraudEventsCount: Int,
    val securityAlertsCount: Int
)
