package com.example.eventpay.domain.antifraud

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Anti-Fraud Domain Models
 * 
 * Comprehensive data structures for fraud detection, prevention, and auditing
 * in the QR code ticketing system.
 */

// ============================================================================
// FRAUD DETECTION TYPES
// ============================================================================

/**
 * Types of fraud that can be detected in the system
 */
enum class FraudType {
    // QR Code Related
    DUPLICATE_QR_SCAN,          // Same QR scanned multiple times
    REPLAY_ATTACK,              // Attempt to reuse captured QR data
    TAMPERED_QR,                // QR code has been modified
    FORGED_QR,                  // Completely fake QR code
    EXPIRED_QR_REUSE,           // Attempt to use expired QR
    
    // Screenshot Related
    SCREENSHOT_DETECTED,        // Screenshot of QR captured
    SCREENSHOT_SHARING,         // Screenshot shared to another device
    MULTIPLE_DEVICE_SCAN,       // Same ticket scanned from different devices
    
    // Timing Related
    RAPID_SCANS,                // Too many scans in short time
    IMPOSSIBLE_TRAVEL,          // Scans from distant locations too quickly
    OFF_HOURS_ACTIVITY,         // Suspicious activity outside normal hours
    
    // Pattern Related
    BRUTE_FORCE_ATTEMPT,        // Multiple invalid QR attempts
    PATTERN_ABUSE,              // Repeated suspicious patterns
    VELOCITY_ABUSE,             // Rate limit violations
    
    // User Behavior
    ACCOUNT_TAKEOVER,           // Suspicious account activity
    TICKET_SHARING,             // Same ticket used by multiple users
    PROXY_USAGE                 // Use of proxy/VPN to mask location
}

/**
 * Severity level of detected fraud
 */
enum class FraudSeverity {
    LOW,        // Minor violation, log only
    MEDIUM,     // Suspicious activity, alert and monitor
    HIGH,       // Likely fraud, block and alert
    CRITICAL    // Confirmed fraud, immediate action required
}

/**
 * Status of fraud detection
 */
enum class FraudDetectionStatus {
    DETECTED,           // Fraud detected, pending review
    CONFIRMED,          // Fraud confirmed by system/admin
    FALSE_POSITIVE,     // Flagged as fraud but legitimate
    UNDER_REVIEW,       // Being investigated
    RESOLVED,           // Issue resolved
    ESCALATED           // Escalated to higher authority
}

/**
 * Source of fraud detection
 */
enum class DetectionSource {
    AUTOMATED_SYSTEM,   // Detected by algorithms
    MANUAL_REVIEW,      // Found during manual check
    USER_REPORT,        // Reported by user
    EXTERNAL_SYSTEM,    // Flagged by external service
    AUDIT_LOG           // Found during audit
}

// ============================================================================
// FRAUD DETECTION RESULT
// ============================================================================

/**
 * Result of fraud detection analysis
 */
@Serializable
data class FraudDetectionResult(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val fraudType: FraudType,
    val severity: FraudSeverity,
    val status: FraudDetectionStatus = FraudDetectionStatus.DETECTED,
    val detectionSource: DetectionSource = DetectionSource.AUTOMATED_SYSTEM,
    
    // Context
    val ticketId: String? = null,
    val eventId: String? = null,
    val userId: String? = null,
    val deviceId: String? = null,
    
    // Detection Details
    val confidence: Double,         // 0.0 to 1.0
    val indicators: List<FraudIndicator>,
    val evidence: List<FraudEvidence>,
    
    // Risk Assessment
    val riskScore: Int,             // 0-100
    val riskFactors: List<RiskFactor>,
    
    // Resolution
    val actionTaken: FraudAction? = null,
    val resolvedAt: Long? = null,
    val resolvedBy: String? = null,
    val resolutionNotes: String? = null
)

/**
 * Individual indicator of potential fraud
 */
@Serializable
data class FraudIndicator(
    val type: IndicatorType,
    val description: String,
    val weight: Double,             // Contribution to overall fraud score
    val value: String,              // The detected value
    val threshold: String,          // The expected/normal value
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Types of fraud indicators
 */
enum class IndicatorType {
    // Timing Indicators
    SCAN_FREQUENCY,             // Rate of scans
    TIME_BETWEEN_SCANS,         // Gap between consecutive scans
    TIME_OF_DAY,                // Unusual timing
    
    // Location Indicators
    LOCATION_MISMATCH,          // Expected vs actual location
    DISTANCE_VELOCITY,          // Impossible travel speed
    GPS_SPOOFING,               // Signs of location spoofing
    
    // Device Indicators
    DEVICE_CHANGE,              // Different device than usual
    MULTIPLE_DEVICES,           // Multiple devices for same ticket
    DEVICE_FINGERPRINT,         // Suspicious device characteristics
    
    // QR Code Indicators
    NONCE_REUSE,                // Same nonce used twice
    TIMESTAMP_ANOMALY,          // Timestamp manipulation
    SIGNATURE_INVALID,          // Invalid cryptographic signature
    CHECKSUM_MISMATCH,          // Data integrity failure
    
    // Behavior Indicators
    SCREENSHOT_DETECTED,        // Screenshot capture detected
    SCREEN_RECORDING,           // Screen recording active
    EMULATOR_USAGE,             // App running on emulator
    ROOT_DETECTION,             // Device is rooted
    
    // Pattern Indicators
    REPEATED_FAILURES,          // Multiple failed attempts
    UNUSUAL_PATTERN,            // Abnormal usage pattern
    BLACKLISTED_ITEM            // Known bad actor/item
}

/**
 * Evidence collected for fraud investigation
 */
@Serializable
data class FraudEvidence(
    val type: EvidenceType,
    val description: String,
    val data: String,               // JSON or base64 encoded data
    val collectedAt: Long = System.currentTimeMillis(),
    val integrity: String           // Hash for evidence integrity
)

/**
 * Types of evidence
 */
enum class EvidenceType {
    QR_CODE_DATA,               // Original QR code content
    SCAN_METADATA,              // Scan details (time, location, device)
    DEVICE_INFO,                // Device fingerprint
    NETWORK_INFO,               // IP, network details
    LOCATION_DATA,              // GPS coordinates
    SCREENSHOT_HASH,            // Hash of detected screenshot
    LOG_ENTRIES,                // Relevant log entries
    USER_AGENT,                 // Client information
    SESSION_DATA,               // Session information
    BIOMETRIC_DATA              // Biometric verification results
}

/**
 * Risk factor contributing to fraud assessment
 */
@Serializable
data class RiskFactor(
    val category: RiskCategory,
    val name: String,
    val description: String,
    val weight: Int,                // 1-10
    val score: Int,                 // 0-100
    val details: String? = null
)

/**
 * Categories of risk factors
 */
enum class RiskCategory {
    BEHAVIORAL,                 // User behavior patterns
    TECHNICAL,                  // Technical indicators
    CONTEXTUAL,                 // Context and environment
    HISTORICAL,                 // Past activity
    DEVICE,                     // Device characteristics
    LOCATION,                   // Geographic factors
    TEMPORAL                    // Time-based factors
}

// ============================================================================
// FRAUD ACTIONS
// ============================================================================

/**
 * Actions that can be taken in response to fraud
 */
@Serializable
sealed class FraudAction {
    abstract val actionId: String
    abstract val timestamp: Long
    abstract val automated: Boolean
    
    /**
     * Block the ticket from being used
     */
    @Serializable
    data class BlockTicket(
        override val actionId: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        override val automated: Boolean = true,
        val ticketId: String,
        val reason: String,
        val duration: Long? = null,      // null = permanent
        val allowAppeal: Boolean = true
    ) : FraudAction()
    
    /**
     * Flag user account for review
     */
    @Serializable
    data class FlagUser(
        override val actionId: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        override val automated: Boolean = true,
        val userId: String,
        val reason: String,
        val restrictions: List<UserRestriction>
    ) : FraudAction()
    
    /**
     * Require additional verification
     */
    @Serializable
    data class RequireVerification(
        override val actionId: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        override val automated: Boolean = true,
        val verificationType: VerificationType,
        val ticketId: String? = null,
        val userId: String? = null
    ) : FraudAction()
    
    /**
     * Alert security team
     */
    @Serializable
    data class AlertSecurity(
        override val actionId: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        override val automated: Boolean = true,
        val alertLevel: AlertLevel,
        val message: String,
        val fraudResultId: String
    ) : FraudAction()
    
    /**
     * Invalidate QR code
     */
    @Serializable
    data class InvalidateQR(
        override val actionId: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        override val automated: Boolean = true,
        val ticketId: String,
        val reason: String,
        val generateNew: Boolean = false
    ) : FraudAction()
    
    /**
     * Log for monitoring
     */
    @Serializable
    data class LogOnly(
        override val actionId: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        override val automated: Boolean = true,
        val message: String,
        val monitoringLevel: MonitoringLevel
    ) : FraudAction()
}

/**
 * Types of user restrictions
 */
enum class UserRestriction {
    TICKET_PURCHASE_BLOCKED,
    TICKET_TRANSFER_BLOCKED,
    REQUIRES_ID_VERIFICATION,
    REQUIRES_2FA,
    ACCOUNT_FROZEN,
    FLAGGED_FOR_REVIEW,
    TEMPORARY_BAN,
    PERMANENT_BAN
}

/**
 * Types of verification
 */
enum class VerificationType {
    ID_VERIFICATION,
    BIOMETRIC_VERIFICATION,
    TWO_FACTOR_AUTH,
    EMAIL_VERIFICATION,
    PHONE_VERIFICATION,
    SECURITY_QUESTIONS,
    MANUAL_REVIEW
}

/**
 * Alert levels
 */
enum class AlertLevel {
    INFO,           // Informational, no action needed
    WARNING,        // Potential issue, monitor
    ALERT,          // Significant issue, review needed
    CRITICAL,       // Urgent, immediate action required
    EMERGENCY       // System-wide alert
}

/**
 * Monitoring levels
 */
enum class MonitoringLevel {
    STANDARD,       // Normal logging
    ENHANCED,       // Additional logging
    INTENSIVE,      // Full activity tracking
    QUARANTINE      // Isolated monitoring
}

// ============================================================================
// SECURITY RULES
// ============================================================================

/**
 * Security rule configuration
 */
@Serializable
data class SecurityRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val enabled: Boolean = true,
    val priority: Int = 0,              // Higher = more important
    val conditions: List<RuleCondition>,
    val actions: List<RuleAction>,
    val validFrom: Long? = null,
    val validUntil: Long? = null,
    val appliesTo: RuleScope = RuleScope.GLOBAL,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Condition for a security rule
 */
@Serializable
data class RuleCondition(
    val field: String,
    val operator: ConditionOperator,
    val value: String,
    val logicalOperator: LogicalOperator? = null  // For chaining conditions
)

/**
 * Operators for condition evaluation
 */
enum class ConditionOperator {
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    LESS_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN_OR_EQUAL,
    CONTAINS,
    NOT_CONTAINS,
    STARTS_WITH,
    ENDS_WITH,
    MATCHES_REGEX,
    IN_LIST,
    NOT_IN_LIST,
    IS_NULL,
    IS_NOT_NULL
}

/**
 * Logical operators for combining conditions
 */
enum class LogicalOperator {
    AND,
    OR,
    NOT
}

/**
 * Action to take when rule matches
 */
@Serializable
data class RuleAction(
    val type: ActionType,
    val parameters: Map<String, String> = emptyMap(),
    val order: Int = 0
)

/**
 * Types of rule actions
 */
enum class ActionType {
    BLOCK,
    ALLOW,
    FLAG,
    ALERT,
    LOG,
    REQUIRE_VERIFICATION,
    INCREMENT_COUNTER,
    SET_FLAG,
    NOTIFY_USER,
    NOTIFY_ADMIN,
    EXECUTE_CUSTOM
}

/**
 * Scope of rule application
 */
enum class RuleScope {
    GLOBAL,             // Applies to all events/users
    EVENT_SPECIFIC,     // Applies to specific events
    USER_SPECIFIC,      // Applies to specific users
    DEVICE_SPECIFIC,    // Applies to specific devices
    LOCATION_SPECIFIC   // Applies to specific locations
}

// ============================================================================
// AUDIT LOG
// ============================================================================

/**
 * Audit log entry for comprehensive tracking
 */
@Serializable
data class AuditLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: AuditEventType,
    val severity: AuditSeverity,
    
    // Actor information
    val actorType: ActorType,
    val actorId: String,
    val actorIp: String? = null,
    val actorDevice: String? = null,
    val actorLocation: String? = null,
    
    // Action details
    val action: String,
    val actionDetails: Map<String, String> = emptyMap(),
    
    // Target information
    val targetType: TargetType? = null,
    val targetId: String? = null,
    
    // Result
    val result: ActionResult,
    val resultMessage: String? = null,
    val errorCode: String? = null,
    
    // Context
    val sessionId: String? = null,
    val requestId: String? = null,
    val correlationId: String? = null,
    
    // Additional data
    val metadata: Map<String, String> = emptyMap(),
    val previousValue: String? = null,      // For change tracking
    val newValue: String? = null,
    
    // Integrity
    val checksum: String                    // Hash for tamper detection
)

/**
 * Types of auditable events
 */
enum class AuditEventType {
    // Authentication
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGOUT,
    SESSION_EXPIRED,
    PASSWORD_CHANGE,
    TWO_FACTOR_ENABLED,
    TWO_FACTOR_DISABLED,
    
    // QR Code Operations
    QR_GENERATED,
    QR_SCANNED,
    QR_VALIDATED,
    QR_EXPIRED,
    QR_INVALIDATED,
    QR_TAMPERED,
    
    // Ticket Operations
    TICKET_PURCHASED,
    TICKET_TRANSFERRED,
    TICKET_REFUNDED,
    TICKET_CANCELLED,
    TICKET_CHECKED_IN,
    TICKET_BLOCKED,
    
    // Fraud Detection
    FRAUD_DETECTED,
    FRAUD_CONFIRMED,
    FRAUD_FALSE_POSITIVE,
    FRAUD_ESCALATED,
    FRAUD_RESOLVED,
    
    // Security Events
    SECURITY_ALERT,
    SECURITY_RULE_TRIGGERED,
    SECURITY_CONFIG_CHANGED,
    ACCESS_DENIED,
    PERMISSION_GRANTED,
    PERMISSION_REVOKED,
    
    // System Events
    SYSTEM_STARTUP,
    SYSTEM_SHUTDOWN,
    CONFIG_CHANGE,
    DATA_EXPORT,
    DATA_IMPORT,
    BACKUP_CREATED,
    RESTORE_PERFORMED,
    
    // User Management
    USER_CREATED,
    USER_UPDATED,
    USER_DELETED,
    ROLE_ASSIGNED,
    ROLE_REMOVED,
    
    // Event Management
    EVENT_CREATED,
    EVENT_UPDATED,
    EVENT_CANCELLED,
    EVENT_COMPLETED
}

/**
 * Severity levels for audit events
 */
enum class AuditSeverity {
    DEBUG,      // Detailed debugging information
    INFO,       // Normal operational information
    NOTICE,     // Significant but normal event
    WARNING,    // Potential issue detected
    ERROR,      // Error occurred
    CRITICAL,   // Critical system issue
    ALERT       // Immediate attention required
}

/**
 * Types of actors in the system
 */
enum class ActorType {
    USER,
    ADMIN,
    SYSTEM,
    SERVICE_ACCOUNT,
    API_CLIENT,
    AUTOMATED_PROCESS
}

/**
 * Types of targets that can be acted upon
 */
enum class TargetType {
    USER,
    TICKET,
    EVENT,
    QR_CODE,
    TRANSACTION,
    SHIFT,
    REPORT,
    CONFIGURATION,
    SECURITY_RULE
}

/**
 * Result of an audited action
 */
enum class ActionResult {
    SUCCESS,
    FAILURE,
    PARTIAL,
    PENDING,
    CANCELLED,
    TIMEOUT,
    BLOCKED
}

// ============================================================================
// SUSPICIOUS ACTIVITY ALERT
// ============================================================================

/**
 * Alert for suspicious activity
 */
@Serializable
data class SuspiciousActivityAlert(
    val id: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val alertType: AlertType,
    val severity: FraudSeverity,
    val status: AlertStatus = AlertStatus.NEW,
    
    // What triggered the alert
    val trigger: AlertTrigger,
    val fraudResultId: String? = null,
    
    // Context
    val ticketId: String? = null,
    val eventId: String? = null,
    val userId: String? = null,
    val deviceId: String? = null,
    
    // Details
    val title: String,
    val description: String,
    val indicators: List<String>,
    val recommendedActions: List<String>,
    
    // Assignment
    val assignedTo: String? = null,
    val assignedAt: Long? = null,
    
    // Resolution
    val resolvedAt: Long? = null,
    val resolvedBy: String? = null,
    val resolution: String? = null,
    val resolutionType: ResolutionType? = null,
    
    // Notifications
    val notificationsSent: List<NotificationRecord> = emptyList(),
    
    // Related alerts
    val relatedAlertIds: List<String> = emptyList()
)

/**
 * Types of alerts
 */
enum class AlertType {
    FRAUD_DETECTED,
    SUSPICIOUS_PATTERN,
    SECURITY_VIOLATION,
    SYSTEM_ANOMALY,
    USER_REPORT,
    AUTOMATED_BLOCK,
    THRESHOLD_EXCEEDED,
    POLICY_VIOLATION
}

/**
 * Status of an alert
 */
enum class AlertStatus {
    NEW,            // Just created, not yet reviewed
    ACKNOWLEDGED,   // Seen by someone
    INVESTIGATING,  // Being actively investigated
    ESCALATED,      // Escalated to higher level
    RESOLVED,       // Issue resolved
    DISMISSED,      // False positive or not actionable
    CLOSED          // Final state
}

/**
 * What triggered the alert
 */
@Serializable
data class AlertTrigger(
    val source: DetectionSource,
    val ruleId: String? = null,
    val threshold: String? = null,
    val actualValue: String? = null,
    val triggeredAt: Long = System.currentTimeMillis(),
    val fraudResultId: String? = null
)

/**
 * Types of resolution
 */
enum class ResolutionType {
    CONFIRMED_FRAUD,
    FALSE_POSITIVE,
    USER_ERROR,
    SYSTEM_ERROR,
    POLICY_EXCEPTION,
    NO_ACTION_NEEDED,
    ESCALATED_FURTHER
}

/**
 * Record of notification sent
 */
@Serializable
data class NotificationRecord(
    val type: NotificationType,
    val recipient: String,
    val sentAt: Long = System.currentTimeMillis(),
    val delivered: Boolean = false,
    val readAt: Long? = null
)

/**
 * Types of notifications
 */
enum class NotificationType {
    EMAIL,
    SMS,
    PUSH_NOTIFICATION,
    IN_APP,
    WEBHOOK,
    SLACK,
    PAGERDUTY
}

// ============================================================================
// SCREENSHOT PROTECTION
// ============================================================================

/**
 * Screenshot detection result
 */
@Serializable
data class ScreenshotDetectionResult(
    val detected: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val ticketId: String? = null,
    val eventId: String? = null,
    val userId: String? = null,
    val deviceId: String? = null,
    val detectionMethod: ScreenshotDetectionMethod,
    val confidence: Double,
    val actionTaken: ScreenshotAction? = null,
    val additionalInfo: Map<String, String> = emptyMap()
)

/**
 * Methods for detecting screenshots
 */
enum class ScreenshotDetectionMethod {
    CONTENT_OBSERVER,       // Android content observer for screenshots
    FILE_SYSTEM_MONITOR,    // Monitoring screenshot directory
    SCREEN_CAPTURE_API,     // Android screen capture callback
    METADATA_ANALYSIS,      // Analyzing image metadata
    WATERMARK_DETECTION,    // Checking for embedded watermarks
    TIMING_ANALYSIS,        // Analyzing timing patterns
    DISPLAY_SECURE_FLAG     // Using FLAG_SECURE
}

/**
 * Actions taken when screenshot detected
 */
enum class ScreenshotAction {
    LOG_ONLY,
    SHOW_WARNING,
    INVALIDATE_QR,
    ALERT_USER,
    ALERT_ADMIN,
    BLOCK_TICKET,
    REQUIRE_REAUTH
}

/**
 * Screenshot protection configuration
 */
@Serializable
data class ScreenshotProtectionConfig(
    val enabled: Boolean = true,
    val detectionMethods: List<ScreenshotDetectionMethod> = listOf(
        ScreenshotDetectionMethod.DISPLAY_SECURE_FLAG,
        ScreenshotDetectionMethod.CONTENT_OBSERVER
    ),
    val actionsOnDetection: List<ScreenshotAction> = listOf(
        ScreenshotAction.LOG_ONLY,
        ScreenshotAction.SHOW_WARNING
    ),
    val invalidateQR: Boolean = false,
    val alertUser: Boolean = true,
    val alertAdmin: Boolean = false,
    val blockAfterWarnings: Int = 3,
    val warningMessage: String = "Screenshots of tickets are not allowed for security reasons.",
    val watermarkEnabled: Boolean = true,
    val dynamicOverlayEnabled: Boolean = true,
    val timeBasedAnimationEnabled: Boolean = true
)

// ============================================================================
// SCAN CONTEXT
// ============================================================================

/**
 * Context information for a QR scan
 */
@Serializable
data class ScanContext(
    val scanId: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    
    // Device information
    val deviceId: String,
    val deviceFingerprint: String,
    val deviceModel: String? = null,
    val osVersion: String? = null,
    val appVersion: String? = null,
    val isRooted: Boolean = false,
    val isEmulator: Boolean = false,
    
    // Location information
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Float? = null,
    val locationSource: String? = null,
    
    // Network information
    val ipAddress: String? = null,
    val networkType: String? = null,
    val wifiSsid: String? = null,
    val isVpn: Boolean = false,
    val isProxy: Boolean = false,
    
    // Scan details
    val scanType: ScanType,
    val qrCodeData: String? = null,     // Hashed for security
    val scannedBy: String,
    val eventId: String,
    
    // Security state
    val screenCaptureActive: Boolean = false,
    val screenRecordingActive: Boolean = false,
    val debugModeEnabled: Boolean = false,
    val biometricVerified: Boolean = false,
    
    // Previous scan context (for comparison)
    val previousScanId: String? = null,
    val timeSinceLastScan: Long? = null,
    val distanceFromLastScan: Double? = null
)

/**
 * Types of scans
 */
enum class ScanType {
    CHECK_IN,
    VERIFICATION,
    PREVIEW,
    TRANSFER,
    REFUND
}

// ============================================================================
// DEVICE FINGERPRINT
// ============================================================================

/**
 * Device fingerprint for tracking
 */
@Serializable
data class DeviceFingerprint(
    val deviceId: String,
    val fingerprint: String,            // Unique hash
    val createdAt: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis(),
    val seenCount: Int = 1,
    
    // Device characteristics
    val manufacturer: String? = null,
    val model: String? = null,
    val brand: String? = null,
    val device: String? = null,
    val product: String? = null,
    val hardware: String? = null,
    
    // OS information
    val osVersion: String? = null,
    val sdkVersion: Int? = null,
    val securityPatch: String? = null,
    
    // App information
    val appVersion: String? = null,
    val appVersionCode: Int? = null,
    
    // Security status
    val isRooted: Boolean = false,
    val hasMagisk: Boolean = false,
    val hasXposed: Boolean = false,
    val isEmulator: Boolean = false,
    val hasDebugBridge: Boolean = false,
    
    // Risk assessment
    val riskScore: Int = 0,
    val riskFactors: List<String> = emptyList(),
    val isBlacklisted: Boolean = false,
    val blacklistReason: String? = null
)
