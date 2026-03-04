package com.example.eventpay.domain.usecase.antifraud

import com.example.eventpay.domain.antifraud.*
import com.example.eventpay.domain.qrcode.QRCodePayload
import com.example.eventpay.domain.qrcode.QRCodeScanRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Anti-Fraud Use Cases
 * 
 * Business logic layer for the anti-fraud system.
 * Orchestrates fraud detection, security rules, screenshot protection,
 * and audit logging to provide comprehensive fraud prevention.
 */
@Singleton
class AntiFraudUseCases @Inject constructor(
    private val fraudDetectionEngine: FraudDetectionEngine,
    private val securityRulesEngine: SecurityRulesEngine,
    private val screenshotProtectionManager: ScreenshotProtectionManager,
    private val auditLogManager: AuditLogManager
) {
    
    /**
     * Perform comprehensive fraud analysis on a QR scan
     * 
     * This is the main entry point for fraud detection during ticket scanning.
     * It combines all fraud detection mechanisms and returns a comprehensive result.
     * 
     * @param scanContext The context of the scan
     * @param payload The decrypted QR code payload
     * @param previousScan Optional previous scan record
     * @return Comprehensive fraud analysis result
     */
    suspend fun analyzeQRScan(
        scanContext: ScanContext,
        payload: QRCodePayload.TicketPayload,
        previousScan: QRCodeScanRecord? = null
    ): ComprehensiveFraudResult = withContext(Dispatchers.Default) {
        
        // Step 1: Quick fraud check for immediate blocking
        val quickCheck = fraudDetectionEngine.quickFraudCheck(scanContext, payload)
        
        if (!quickCheck.isSafe && quickCheck.riskScore >= 80) {
            // Critical fraud detected - block immediately
            val result = FraudDetectionResult(
                fraudType = FraudType.REPLAY_ATTACK,
                severity = FraudSeverity.CRITICAL,
                ticketId = payload.ticketId,
                eventId = payload.eventId,
                userId = payload.userId,
                deviceId = scanContext.deviceId,
                confidence = 1.0,
                indicators = quickCheck.criticalIndicators,
                evidence = emptyList(),
                riskScore = quickCheck.riskScore,
                riskFactors = emptyList()
            )
            
            // Log the fraud detection
            auditLogManager.logFraudDetection(result, scanContext.deviceId)
            
            return@withContext ComprehensiveFraudResult(
                isAllowed = false,
                requiresAdditionalVerification = false,
                fraudDetectionResult = result,
                ruleEvaluationResult = null,
                actions = listOf(FraudAction.BlockTicket(ticketId = payload.ticketId, reason = "Critical fraud detected"))
            )
        }
        
        // Step 2: Full fraud analysis
        val fraudResult = fraudDetectionEngine.analyzeScan(scanContext, payload, previousScan)
        
        // Step 3: Evaluate security rules
        val ruleResult = securityRulesEngine.evaluateRules(scanContext, payload, mapOf(
            "riskScore" to fraudResult.riskScore.toString(),
            "fraudType" to fraudResult.fraudType.name,
            "confidence" to fraudResult.confidence.toString()
        ))
        
        // Step 4: Determine actions
        val actions = determineActions(fraudResult, ruleResult)
        
        // Step 5: Determine if scan is allowed
        val isAllowed = determineIfAllowed(fraudResult, ruleResult)
        
        // Step 6: Determine if additional verification is needed
        val requiresVerification = determineIfVerificationNeeded(fraudResult, ruleResult)
        
        // Step 7: Record the scan for future detection
        if (isAllowed) {
            fraudDetectionEngine.recordScan(scanContext, payload)
        } else {
            fraudDetectionEngine.recordFailedAttempt(scanContext.deviceId, "Fraud detected")
        }
        
        // Step 8: Log the analysis
        if (fraudResult.severity != FraudSeverity.LOW) {
            auditLogManager.logFraudDetection(fraudResult, scanContext.deviceId)
        }
        
        ComprehensiveFraudResult(
            isAllowed = isAllowed,
            requiresAdditionalVerification = requiresVerification,
            fraudDetectionResult = fraudResult,
            ruleEvaluationResult = ruleResult,
            actions = actions
        )
    }
    
    /**
     * Validate QR code with fraud checks
     * 
     * Combines QR validation with fraud detection for a complete validation flow.
     * 
     * @param qrCodeString The QR code string to validate
     * @param scanContext The scan context
     * @param payload The decrypted payload
     * @return Validation result with fraud analysis
     */
    suspend fun validateWithFraudCheck(
        qrCodeString: String,
        scanContext: ScanContext,
        payload: QRCodePayload.TicketPayload
    ): SecureValidationResult = withContext(Dispatchers.Default) {
        
        // Perform fraud analysis
        val fraudAnalysis = analyzeQRScan(scanContext, payload)
        
        // Determine overall validation status
        val status = when {
            !fraudAnalysis.isAllowed -> SecureValidationStatus.BLOCKED
            fraudAnalysis.requiresAdditionalVerification -> SecureValidationStatus.VERIFICATION_REQUIRED
            (fraudAnalysis.fraudDetectionResult?.riskScore ?: 0) > 50 -> SecureValidationStatus.WARNING
            else -> SecureValidationStatus.VALID
        }
        
        SecureValidationResult(
            status = status,
            fraudAnalysis = fraudAnalysis,
            message = generateValidationMessage(fraudAnalysis)
        )
    }
    
    /**
     * Report suspicious activity
     * 
     * Creates an alert for suspicious activity detected by users or admins.
     * 
     * @param alertType Type of alert
     * @param severity Severity level
     * @param title Alert title
     * @param description Alert description
     * @param ticketId Related ticket ID
     * @param eventId Related event ID
     * @param userId Related user ID
     * @param deviceId Related device ID
     * @param indicators List of suspicious indicators
     * @return Created alert
     */
    suspend fun reportSuspiciousActivity(
        alertType: AlertType,
        severity: FraudSeverity,
        title: String,
        description: String,
        ticketId: String? = null,
        eventId: String? = null,
        userId: String? = null,
        deviceId: String? = null,
        indicators: List<String> = emptyList()
    ): SuspiciousActivityAlert {
        return auditLogManager.createAlert(
            alertType = alertType,
            severity = severity,
            title = title,
            description = description,
            ticketId = ticketId,
            eventId = eventId,
            userId = userId,
            deviceId = deviceId,
            indicators = indicators
        )
    }
    
    /**
     * Resolve an alert
     * 
     * @param alertId The alert ID to resolve
     * @param resolvedBy Who resolved the alert
     * @param resolution Resolution details
     * @param resolutionType Type of resolution
     * @return Whether resolution was successful
     */
    suspend fun resolveAlert(
        alertId: String,
        resolvedBy: String,
        resolution: String,
        resolutionType: ResolutionType
    ): Boolean {
        return auditLogManager.resolveAlert(alertId, resolvedBy, resolution, resolutionType)
    }
    
    /**
     * Get fraud statistics for an event
     * 
     * @param eventId The event ID
     * @return Fraud statistics
     */
    suspend fun getEventFraudStatistics(eventId: String): EventFraudStatistics = withContext(Dispatchers.Default) {
        val auditStats = auditLogManager.getStatistics()
        
        // Filter for specific event (would query database in production)
        EventFraudStatistics(
            eventId = eventId,
            totalScans = auditStats.entriesLast24h, // Placeholder
            fraudAttempts = auditStats.fraudEventsCount,
            blockedScans = auditStats.entriesByResult[ActionResult.BLOCKED] ?: 0,
            alertsGenerated = auditStats.securityAlertsCount,
            averageRiskScore = 0.0, // Would calculate from actual data
            topFraudTypes = emptyList(), // Would calculate from actual data
            recentAlerts = auditLogManager.recentAlerts.value.take(10)
        )
    }
    
    /**
     * Add a custom security rule
     * 
     * @param rule The rule to add
     * @return Whether the rule was added successfully
     */
    fun addSecurityRule(rule: SecurityRule): Boolean {
        return securityRulesEngine.addRule(rule)
    }
    
    /**
     * Update a security rule
     * 
     * @param rule The rule to update
     * @return Whether the rule was updated successfully
     */
    fun updateSecurityRule(rule: SecurityRule): Boolean {
        return securityRulesEngine.updateRule(rule)
    }
    
    /**
     * Remove a security rule
     * 
     * @param ruleId The rule ID to remove
     * @return Whether the rule was removed successfully
     */
    fun removeSecurityRule(ruleId: String): Boolean {
        return securityRulesEngine.removeRule(ruleId)
    }
    
    /**
     * Get all security rules
     * 
     * @return List of all security rules
     */
    fun getAllSecurityRules(): List<SecurityRule> {
        return securityRulesEngine.getAllRules()
    }
    
    /**
     * Get audit log entries
     * 
     * @param eventType Optional event type filter
     * @param startTime Optional start time filter
     * @param endTime Optional end time filter
     * @return List of audit log entries
     */
    fun getAuditLogs(
        eventType: AuditEventType? = null,
        startTime: Long? = null,
        endTime: Long? = null
    ): List<AuditLogEntry> {
        return when {
            eventType != null -> auditLogManager.getEntriesByType(eventType)
            startTime != null && endTime != null -> auditLogManager.getEntriesByTimeRange(startTime, endTime)
            else -> auditLogManager.logEntries.value
        }
    }
    
    /**
     * Search audit logs
     * 
     * @param query Search query
     * @return Matching audit log entries
     */
    fun searchAuditLogs(query: String): List<AuditLogEntry> {
        return auditLogManager.searchEntries(query)
    }
    
    /**
     * Export audit logs
     * 
     * @param startTime Optional start time
     * @param endTime Optional end time
     * @param format Export format
     * @return Exported log data
     */
    suspend fun exportAuditLogs(
        startTime: Long? = null,
        endTime: Long? = null,
        format: ExportFormat = ExportFormat.JSON
    ): String {
        return auditLogManager.exportLogs(startTime, endTime, format)
    }
    
    /**
     * Verify audit log integrity
     * 
     * @return Integrity check result
     */
    fun verifyAuditIntegrity(): IntegrityCheckResult {
        return auditLogManager.verifyIntegrity()
    }
    
    /**
     * Handle screenshot detection
     * 
     * @param result Screenshot detection result
     */
    suspend fun handleScreenshotDetection(result: ScreenshotDetectionResult) {
        // Log the detection
        auditLogManager.logScreenshotDetection(result)
        
        // Create alert if needed
        if (result.confidence > 0.8) {
            auditLogManager.createAlert(
                alertType = AlertType.SECURITY_VIOLATION,
                severity = FraudSeverity.MEDIUM,
                title = "Screenshot Detected",
                description = "Screenshot of ticket QR code was detected",
                ticketId = result.ticketId,
                eventId = result.eventId,
                userId = result.userId,
                deviceId = result.deviceId,
                indicators = listOf("Screenshot captured", "Confidence: ${result.confidence}")
            )
        }
    }
    
    /**
     * Get device risk assessment
     * 
     * @param deviceId The device ID
     * @return Device risk assessment
     */
    suspend fun getDeviceRiskAssessment(deviceId: String): DeviceRiskAssessment = withContext(Dispatchers.Default) {
        // Would query from stored device fingerprints
        DeviceRiskAssessment(
            deviceId = deviceId,
            riskScore = 0,
            riskFactors = emptyList(),
            isBlacklisted = false,
            lastSeen = System.currentTimeMillis(),
            totalScans = 0,
            fraudAttempts = 0
        )
    }
    
    /**
     * Blacklist a device
     * 
     * @param deviceId The device ID to blacklist
     * @param reason Reason for blacklisting
     * @param blacklistedBy Who blacklisted the device
     */
    suspend fun blacklistDevice(
        deviceId: String,
        reason: String,
        blacklistedBy: String
    ) {
        // Would update device fingerprint in storage
        auditLogManager.logEvent(
            eventType = AuditEventType.SECURITY_CONFIG_CHANGED,
            severity = AuditSeverity.WARNING,
            actorType = ActorType.ADMIN,
            actorId = blacklistedBy,
            action = "DEVICE_BLACKLISTED",
            actionDetails = mapOf("deviceId" to deviceId, "reason" to reason),
            targetType = TargetType.USER,
            targetId = deviceId,
            result = ActionResult.SUCCESS
        )
    }
    
    // ============================================================================
    // PRIVATE HELPER METHODS
    // ============================================================================
    
    private fun determineActions(
        fraudResult: FraudDetectionResult,
        ruleResult: RuleEvaluationResult?
    ): List<FraudAction> {
        val actions = mutableListOf<FraudAction>()
        
        // Add actions based on fraud severity
        when (fraudResult.severity) {
            FraudSeverity.CRITICAL -> {
                actions.add(FraudAction.BlockTicket(
                    ticketId = fraudResult.ticketId ?: "",
                    reason = "Critical fraud detected: ${fraudResult.fraudType}"
                ))
                actions.add(FraudAction.AlertSecurity(
                    alertLevel = AlertLevel.EMERGENCY,
                    message = "Critical fraud detected: ${fraudResult.fraudType}",
                    fraudResultId = fraudResult.id
                ))
            }
            FraudSeverity.HIGH -> {
                actions.add(FraudAction.BlockTicket(
                    ticketId = fraudResult.ticketId ?: "",
                    reason = "High risk fraud detected: ${fraudResult.fraudType}"
                ))
                actions.add(FraudAction.AlertSecurity(
                    alertLevel = AlertLevel.CRITICAL,
                    message = "High risk fraud detected: ${fraudResult.fraudType}",
                    fraudResultId = fraudResult.id
                ))
            }
            FraudSeverity.MEDIUM -> {
                actions.add(FraudAction.RequireVerification(
                    verificationType = VerificationType.BIOMETRIC_VERIFICATION,
                    ticketId = fraudResult.ticketId
                ))
                actions.add(FraudAction.FlagUser(
                    userId = fraudResult.userId ?: "",
                    reason = "Medium risk fraud detected",
                    restrictions = listOf(UserRestriction.FLAGGED_FOR_REVIEW)
                ))
            }
            FraudSeverity.LOW -> {
                actions.add(FraudAction.LogOnly(
                    message = "Low risk fraud indicator: ${fraudResult.fraudType}",
                    monitoringLevel = MonitoringLevel.ENHANCED
                ))
            }
        }
        
        // Add actions from rule evaluation
        ruleResult?.actions?.forEach { ruleAction ->
            when (ruleAction.type) {
                ActionType.BLOCK -> actions.add(FraudAction.BlockTicket(
                    ticketId = fraudResult.ticketId ?: "",
                    reason = ruleAction.parameters["reason"] ?: "Blocked by security rule"
                ))
                ActionType.FLAG -> actions.add(FraudAction.FlagUser(
                    userId = fraudResult.userId ?: "",
                    reason = "Flagged by security rule",
                    restrictions = listOf(UserRestriction.FLAGGED_FOR_REVIEW)
                ))
                ActionType.REQUIRE_VERIFICATION -> actions.add(FraudAction.RequireVerification(
                    verificationType = VerificationType.valueOf(ruleAction.parameters["type"] ?: "BIOMETRIC_VERIFICATION"),
                    ticketId = fraudResult.ticketId
                ))
                ActionType.ALERT -> actions.add(FraudAction.AlertSecurity(
                    alertLevel = AlertLevel.valueOf(ruleAction.parameters["level"] ?: "WARNING"),
                    message = ruleAction.parameters["message"] ?: "Security alert",
                    fraudResultId = fraudResult.id
                ))
                else -> { /* Other action types handled differently */ }
            }
        }
        
        return actions.distinctBy { it::class.simpleName }
    }
    
    private fun determineIfAllowed(
        fraudResult: FraudDetectionResult,
        ruleResult: RuleEvaluationResult?
    ): Boolean {
        // Block if severity is high or critical
        if (fraudResult.severity == FraudSeverity.CRITICAL || 
            fraudResult.severity == FraudSeverity.HIGH) {
            return false
        }
        
        // Block if rules indicate blocking
        if (ruleResult?.hasBlockAction == true) {
            return false
        }
        
        // Block if risk score is too high
        if (fraudResult.riskScore >= 80) {
            return false
        }
        
        return true
    }
    
    private fun determineIfVerificationNeeded(
        fraudResult: FraudDetectionResult,
        ruleResult: RuleEvaluationResult?
    ): Boolean {
        // Need verification for medium severity
        if (fraudResult.severity == FraudSeverity.MEDIUM) {
            return true
        }
        
        // Need verification if risk score is elevated
        if (fraudResult.riskScore in 50..79) {
            return true
        }
        
        // Check if rules require verification
        if (ruleResult?.actions?.any { it.type == ActionType.REQUIRE_VERIFICATION } == true) {
            return true
        }
        
        return false
    }
    
    private fun generateValidationMessage(fraudAnalysis: ComprehensiveFraudResult): String {
        return when {
            !fraudAnalysis.isAllowed -> "Access denied: ${fraudAnalysis.fraudDetectionResult?.fraudType?.name ?: "Security violation"}"
            fraudAnalysis.requiresAdditionalVerification -> "Additional verification required"
            fraudAnalysis.fraudDetectionResult?.riskScore?.let { it > 50 } == true -> "Warning: Elevated risk detected"
            else -> "Validation successful"
        }
    }
}

/**
 * Comprehensive fraud analysis result
 */
data class ComprehensiveFraudResult(
    val isAllowed: Boolean,
    val requiresAdditionalVerification: Boolean,
    val fraudDetectionResult: FraudDetectionResult?,
    val ruleEvaluationResult: RuleEvaluationResult?,
    val actions: List<FraudAction>
)

/**
 * Secure validation result
 */
data class SecureValidationResult(
    val status: SecureValidationStatus,
    val fraudAnalysis: ComprehensiveFraudResult,
    val message: String
)

/**
 * Secure validation status
 */
enum class SecureValidationStatus {
    VALID,                  // QR is valid and safe
    WARNING,                // QR is valid but has elevated risk
    VERIFICATION_REQUIRED,  // QR needs additional verification
    BLOCKED,               // QR is blocked due to fraud
    ERROR                  // Error during validation
}

/**
 * Event fraud statistics
 */
data class EventFraudStatistics(
    val eventId: String,
    val totalScans: Int,
    val fraudAttempts: Int,
    val blockedScans: Int,
    val alertsGenerated: Int,
    val averageRiskScore: Double,
    val topFraudTypes: List<Pair<FraudType, Int>>,
    val recentAlerts: List<SuspiciousActivityAlert>
)

/**
 * Device risk assessment
 */
data class DeviceRiskAssessment(
    val deviceId: String,
    val riskScore: Int,
    val riskFactors: List<String>,
    val isBlacklisted: Boolean,
    val lastSeen: Long,
    val totalScans: Int,
    val fraudAttempts: Int
)
