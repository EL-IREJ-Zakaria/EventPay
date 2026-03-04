package com.example.eventpay.domain.antifraud

import com.example.eventpay.domain.qrcode.QRCodePayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Security Rules Engine
 * 
 * Configurable rule-based system for fraud detection and prevention.
 * Supports dynamic rule evaluation, prioritization, and action execution.
 * 
 * Features:
 * - Declarative rule definition
 * - Multiple condition operators
 * - Priority-based execution
 * - Time-based rule validity
 * - Scope-based rule application
 * - Action chaining
 */
@Singleton
class SecurityRulesEngine @Inject constructor() {
    
    // Rule storage (would use database in production)
    private val rules = mutableMapOf<String, SecurityRule>()
    private val ruleExecutionHistory = mutableMapOf<String, MutableList<RuleExecutionRecord>>()
    
    init {
        // Initialize with default rules
        initializeDefaultRules()
    }
    
    /**
     * Evaluate all applicable rules against a scan context
     * 
     * @param scanContext The scan context to evaluate
     * @param payload The QR code payload
     * @param additionalContext Additional context data
     * @return RuleEvaluationResult with matched rules and actions
     */
    suspend fun evaluateRules(
        scanContext: ScanContext,
        payload: QRCodePayload.TicketPayload,
        additionalContext: Map<String, Any> = emptyMap()
    ): RuleEvaluationResult = withContext(Dispatchers.Default) {
        
        val context = buildEvaluationContext(scanContext, payload, additionalContext)
        val matchedRules = mutableListOf<RuleMatch>()
        val allActions = mutableListOf<RuleAction>()
        
        // Get applicable rules sorted by priority
        val applicableRules = getApplicableRules(scanContext, payload)
            .sortedByDescending { it.priority }
        
        for (rule in applicableRules) {
            if (!rule.enabled) continue
            
            // Check time validity
            if (!isRuleTimeValid(rule)) continue
            
            // Evaluate conditions
            val matchResult = evaluateRuleConditions(rule, context)
            
            if (matchResult.matched) {
                matchedRules.add(
                    RuleMatch(
                        rule = rule,
                        matchedConditions = matchResult.matchedConditions,
                        evaluationTime = System.currentTimeMillis()
                    )
                )
                allActions.addAll(rule.actions.sortedBy { it.order })
                
                // Record execution
                recordRuleExecution(rule, scanContext, matchResult)
            }
        }
        
        RuleEvaluationResult(
            matchedRules = matchedRules,
            actions = deduplicateActions(allActions),
            evaluationContext = context
        )
    }
    
    /**
     * Evaluate a single rule
     */
    fun evaluateRule(
        rule: SecurityRule,
        context: Map<String, Any>
    ): RuleMatchResult {
        return evaluateRuleConditions(rule, context)
    }
    
    /**
     * Add a new security rule
     */
    fun addRule(rule: SecurityRule): Boolean {
        if (rules.containsKey(rule.id)) {
            return false
        }
        rules[rule.id] = rule
        return true
    }
    
    /**
     * Update an existing rule
     */
    fun updateRule(rule: SecurityRule): Boolean {
        if (!rules.containsKey(rule.id)) {
            return false
        }
        rules[rule.id] = rule
        return true
    }
    
    /**
     * Remove a rule
     */
    fun removeRule(ruleId: String): Boolean {
        return rules.remove(ruleId) != null
    }
    
    /**
     * Get all rules
     */
    fun getAllRules(): List<SecurityRule> = rules.values.toList()
    
    /**
     * Get rules by scope
     */
    fun getRulesByScope(scope: RuleScope): List<SecurityRule> =
        rules.values.filter { it.appliesTo == scope }
    
    /**
     * Get rule by ID
     */
    fun getRule(ruleId: String): SecurityRule? = rules[ruleId]
    
    /**
     * Enable/disable a rule
     */
    fun setRuleEnabled(ruleId: String, enabled: Boolean): Boolean {
        val rule = rules[ruleId] ?: return false
        rules[ruleId] = rule.copy(enabled = enabled)
        return true
    }
    
    /**
     * Get execution history for a rule
     */
    fun getRuleExecutionHistory(ruleId: String): List<RuleExecutionRecord> =
        ruleExecutionHistory[ruleId]?.toList() ?: emptyList()
    
    // ============================================================================
    // PRIVATE METHODS
    // ============================================================================
    
    private fun initializeDefaultRules() {
        // Rule 1: Block duplicate QR scans
        addRule(
            SecurityRule(
                id = "rule_duplicate_scan",
                name = "Duplicate Scan Block",
                description = "Block tickets that have already been scanned",
                enabled = true,
                priority = 100,
                conditions = listOf(
                    RuleCondition(
                        field = "scanCount",
                        operator = ConditionOperator.GREATER_THAN,
                        value = "0"
                    )
                ),
                actions = listOf(
                    RuleAction(
                        type = ActionType.BLOCK,
                        parameters = mapOf("reason" to "Ticket already scanned"),
                        order = 1
                    ),
                    RuleAction(
                        type = ActionType.ALERT,
                        parameters = mapOf(
                            "level" to "WARNING",
                            "message" to "Duplicate scan attempt detected"
                        ),
                        order = 2
                    )
                )
            )
        )
        
        // Rule 2: Block expired QR codes
        addRule(
            SecurityRule(
                id = "rule_expired_qr",
                name = "Expired QR Block",
                description = "Block QR codes that have expired",
                enabled = true,
                priority = 99,
                conditions = listOf(
                    RuleCondition(
                        field = "qrExpired",
                        operator = ConditionOperator.EQUALS,
                        value = "true"
                    )
                ),
                actions = listOf(
                    RuleAction(
                        type = ActionType.BLOCK,
                        parameters = mapOf("reason" to "QR code has expired"),
                        order = 1
                    )
                )
            )
        )
        
        // Rule 3: Flag rooted devices
        addRule(
            SecurityRule(
                id = "rule_rooted_device",
                name = "Rooted Device Flag",
                description = "Flag scans from rooted devices for additional verification",
                enabled = true,
                priority = 80,
                conditions = listOf(
                    RuleCondition(
                        field = "deviceRooted",
                        operator = ConditionOperator.EQUALS,
                        value = "true"
                    )
                ),
                actions = listOf(
                    RuleAction(
                        type = ActionType.FLAG,
                        parameters = mapOf(
                            "flag" to "ROOTED_DEVICE",
                            "severity" to "MEDIUM"
                        ),
                        order = 1
                    ),
                    RuleAction(
                        type = ActionType.LOG,
                        parameters = mapOf("level" to "WARNING"),
                        order = 2
                    )
                )
            )
        )
        
        // Rule 4: Block emulator scans
        addRule(
            SecurityRule(
                id = "rule_emulator_block",
                name = "Emulator Block",
                description = "Block scans from emulators",
                enabled = true,
                priority = 95,
                conditions = listOf(
                    RuleCondition(
                        field = "deviceEmulator",
                        operator = ConditionOperator.EQUALS,
                        value = "true"
                    )
                ),
                actions = listOf(
                    RuleAction(
                        type = ActionType.BLOCK,
                        parameters = mapOf("reason" to "Emulator detected"),
                        order = 1
                    ),
                    RuleAction(
                        type = ActionType.ALERT,
                        parameters = mapOf(
                            "level" to "WARNING",
                            "message" to "Scan attempt from emulator"
                        ),
                        order = 2
                    )
                )
            )
        )
        
        // Rule 5: Flag screenshot detection
        addRule(
            SecurityRule(
                id = "rule_screenshot_flag",
                name = "Screenshot Detection",
                description = "Flag when screenshot is detected",
                enabled = true,
                priority = 85,
                conditions = listOf(
                    RuleCondition(
                        field = "screenshotDetected",
                        operator = ConditionOperator.EQUALS,
                        value = "true"
                    )
                ),
                actions = listOf(
                    RuleAction(
                        type = ActionType.FLAG,
                        parameters = mapOf(
                            "flag" to "SCREENSHOT_DETECTED",
                            "severity" to "HIGH"
                        ),
                        order = 1
                    ),
                    RuleAction(
                        type = ActionType.NOTIFY_USER,
                        parameters = mapOf(
                            "message" to "Screenshots are not allowed for security"
                        ),
                        order = 2
                    )
                )
            )
        )
        
        // Rule 6: Rapid scan detection
        addRule(
            SecurityRule(
                id = "rule_rapid_scan",
                name = "Rapid Scan Detection",
                description = "Detect and throttle rapid scanning",
                enabled = true,
                priority = 70,
                conditions = listOf(
                    RuleCondition(
                        field = "scanVelocity",
                        operator = ConditionOperator.GREATER_THAN,
                        value = "10"
                    ),
                    RuleCondition(
                        field = "velocityWindow",
                        operator = ConditionOperator.EQUALS,
                        value = "60",
                        logicalOperator = LogicalOperator.AND
                    )
                ),
                actions = listOf(
                    RuleAction(
                        type = ActionType.FLAG,
                        parameters = mapOf(
                            "flag" to "RAPID_SCANNING",
                            "severity" to "MEDIUM"
                        ),
                        order = 1
                    ),
                    RuleAction(
                        type = ActionType.ALERT,
                        parameters = mapOf(
                            "level" to "WARNING",
                            "message" to "Rapid scanning detected"
                        ),
                        order = 2
                    )
                )
            )
        )
        
        // Rule 7: Impossible travel detection
        addRule(
            SecurityRule(
                id = "rule_impossible_travel",
                name = "Impossible Travel",
                description = "Detect impossible travel between scans",
                enabled = true,
                priority = 90,
                conditions = listOf(
                    RuleCondition(
                        field = "travelSpeed",
                        operator = ConditionOperator.GREATER_THAN,
                        value = "300"
                    )
                ),
                actions = listOf(
                    RuleAction(
                        type = ActionType.BLOCK,
                        parameters = mapOf("reason" to "Impossible travel detected"),
                        order = 1
                    ),
                    RuleAction(
                        type = ActionType.ALERT,
                        parameters = mapOf(
                            "level" to "CRITICAL",
                            "message" to "Impossible travel detected - possible fraud"
                        ),
                        order = 2
                    ),
                    RuleAction(
                        type = ActionType.REQUIRE_VERIFICATION,
                        parameters = mapOf("type" to "BIOMETRIC"),
                        order = 3
                    )
                )
            )
        )
        
        // Rule 8: Brute force detection
        addRule(
            SecurityRule(
                id = "rule_brute_force",
                name = "Brute Force Detection",
                description = "Detect brute force attempts",
                enabled = true,
                priority = 95,
                conditions = listOf(
                    RuleCondition(
                        field = "failedAttempts",
                        operator = ConditionOperator.GREATER_THAN_OR_EQUAL,
                        value = "5"
                    ),
                    RuleCondition(
                        field = "attemptWindow",
                        operator = ConditionOperator.EQUALS,
                        value = "300",
                        logicalOperator = LogicalOperator.AND
                    )
                ),
                actions = listOf(
                    RuleAction(
                        type = ActionType.BLOCK,
                        parameters = mapOf(
                            "reason" to "Too many failed attempts",
                            "duration" to "300000"
                        ),
                        order = 1
                    ),
                    RuleAction(
                        type = ActionType.ALERT,
                        parameters = mapOf(
                            "level" to "HIGH",
                            "message" to "Brute force attempt detected"
                        ),
                        order = 2
                    )
                )
            )
        )
        
        // Rule 9: Multiple device detection
        addRule(
            SecurityRule(
                id = "rule_multiple_device",
                name = "Multiple Device Detection",
                description = "Detect ticket sharing across devices",
                enabled = true,
                priority = 75,
                conditions = listOf(
                    RuleCondition(
                        field = "deviceCount",
                        operator = ConditionOperator.GREATER_THAN,
                        value = "1"
                    )
                ),
                actions = listOf(
                    RuleAction(
                        type = ActionType.FLAG,
                        parameters = mapOf(
                            "flag" to "MULTIPLE_DEVICES",
                            "severity" to "MEDIUM"
                        ),
                        order = 1
                    ),
                    RuleAction(
                        type = ActionType.NOTIFY_ADMIN,
                        parameters = mapOf(
                            "message" to "Ticket used on multiple devices"
                        ),
                        order = 2
                    )
                )
            )
        )
        
        // Rule 10: VPN/Proxy detection
        addRule(
            SecurityRule(
                id = "rule_vpn_proxy",
                name = "VPN/Proxy Detection",
                description = "Flag scans through VPN or proxy",
                enabled = true,
                priority = 60,
                conditions = listOf(
                    RuleCondition(
                        field = "isVpn",
                        operator = ConditionOperator.EQUALS,
                        value = "true"
                    )
                ),
                actions = listOf(
                    RuleAction(
                        type = ActionType.FLAG,
                        parameters = mapOf(
                            "flag" to "VPN_DETECTED",
                            "severity" to "LOW"
                        ),
                        order = 1
                    ),
                    RuleAction(
                        type = ActionType.LOG,
                        parameters = mapOf("level" to "INFO"),
                        order = 2
                    )
                )
            )
        )
        
        // Rule 11: Off-hours activity
        addRule(
            SecurityRule(
                id = "rule_off_hours",
                name = "Off-Hours Activity",
                description = "Flag activity during unusual hours",
                enabled = true,
                priority = 40,
                conditions = listOf(
                    RuleCondition(
                        field = "hourOfDay",
                        operator = ConditionOperator.LESS_THAN,
                        value = "6"
                    )
                ),
                actions = listOf(
                    RuleAction(
                        type = ActionType.LOG,
                        parameters = mapOf("level" to "NOTICE"),
                        order = 1
                    )
                )
            )
        )
        
        // Rule 12: Blacklisted device
        addRule(
            SecurityRule(
                id = "rule_blacklisted_device",
                name = "Blacklisted Device Block",
                description = "Block scans from blacklisted devices",
                enabled = true,
                priority = 100,
                conditions = listOf(
                    RuleCondition(
                        field = "deviceBlacklisted",
                        operator = ConditionOperator.EQUALS,
                        value = "true"
                    )
                ),
                actions = listOf(
                    RuleAction(
                        type = ActionType.BLOCK,
                        parameters = mapOf("reason" to "Device is blacklisted"),
                        order = 1
                    ),
                    RuleAction(
                        type = ActionType.ALERT,
                        parameters = mapOf(
                            "level" to "CRITICAL",
                            "message" to "Blacklisted device attempted scan"
                        ),
                        order = 2
                    )
                )
            )
        )
    }
    
    private fun buildEvaluationContext(
        scanContext: ScanContext,
        payload: QRCodePayload.TicketPayload,
        additionalContext: Map<String, Any>
    ): Map<String, Any> {
        val context = mutableMapOf<String, Any>()
        
        // Add scan context fields
        context["deviceId"] = scanContext.deviceId
        context["deviceFingerprint"] = scanContext.deviceFingerprint
        context["deviceRooted"] = scanContext.isRooted
        context["deviceEmulator"] = scanContext.isEmulator
        context["screenshotDetected"] = scanContext.screenCaptureActive
        context["screenRecording"] = scanContext.screenRecordingActive
        context["debugMode"] = scanContext.debugModeEnabled
        context["biometricVerified"] = scanContext.biometricVerified
        context["isVpn"] = scanContext.isVpn
        context["isProxy"] = scanContext.isProxy
        
        // Add location fields
        scanContext.latitude?.let { context["latitude"] = it }
        scanContext.longitude?.let { context["longitude"] = it }
        scanContext.accuracy?.let { context["gpsAccuracy"] = it }
        
        // Add payload fields
        context["ticketId"] = payload.ticketId
        context["eventId"] = payload.eventId
        context["userId"] = payload.userId
        context["qrTimestamp"] = payload.timestamp
        context["qrNonce"] = payload.nonce
        
        // Calculate derived fields
        val qrAge = System.currentTimeMillis() - payload.timestamp
        context["qrAgeMs"] = qrAge
        context["qrExpired"] = qrAge > (24 * 60 * 60 * 1000) // 24 hours
        
        // Add time fields
        val calendar = java.util.Calendar.getInstance()
        context["hourOfDay"] = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        context["dayOfWeek"] = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        
        // Add additional context
        context.putAll(additionalContext)
        
        return context
    }
    
    private fun getApplicableRules(
        scanContext: ScanContext,
        payload: QRCodePayload.TicketPayload
    ): List<SecurityRule> {
        return rules.values.filter { rule ->
            when (rule.appliesTo) {
                RuleScope.GLOBAL -> true
                RuleScope.EVENT_SPECIFIC -> {
                    // Check if rule has event metadata matching current event
                    rule.metadata["eventId"]?.let { it == payload.eventId } ?: true
                }
                RuleScope.USER_SPECIFIC -> {
                    rule.metadata["userId"]?.let { it == payload.userId } ?: true
                }
                RuleScope.DEVICE_SPECIFIC -> {
                    rule.metadata["deviceId"]?.let { it == scanContext.deviceId } ?: true
                }
                RuleScope.LOCATION_SPECIFIC -> {
                    // Check location-based scope
                    true
                }
            }
        }
    }
    
    private fun isRuleTimeValid(rule: SecurityRule): Boolean {
        val now = System.currentTimeMillis()
        
        if (rule.validFrom != null && now < rule.validFrom) return false
        if (rule.validUntil != null && now > rule.validUntil) return false
        
        return true
    }
    
    private fun evaluateRuleConditions(
        rule: SecurityRule,
        context: Map<String, Any>
    ): RuleMatchResult {
        if (rule.conditions.isEmpty()) {
            return RuleMatchResult(matched = true, matchedConditions = emptyList())
        }
        
        val matchedConditions = mutableListOf<RuleCondition>()
        var currentLogicalOp: LogicalOperator? = null
        var result = true
        
        for (condition in rule.conditions) {
            val conditionResult = evaluateCondition(condition, context)
            
            if (conditionResult) {
                matchedConditions.add(condition)
            }
            
            // Handle logical operators
            when (currentLogicalOp) {
                LogicalOperator.AND -> result = result && conditionResult
                LogicalOperator.OR -> result = result || conditionResult
                LogicalOperator.NOT -> result = result && !conditionResult
                null -> result = conditionResult
            }
            
            currentLogicalOp = condition.logicalOperator
        }
        
        return RuleMatchResult(
            matched = result,
            matchedConditions = matchedConditions
        )
    }
    
    private fun evaluateCondition(
        condition: RuleCondition,
        context: Map<String, Any>
    ): Boolean {
        val fieldValue = context[condition.field] ?: return false
        val conditionValue = condition.value
        
        return when (condition.operator) {
            ConditionOperator.EQUALS -> fieldValue.toString() == conditionValue
            ConditionOperator.NOT_EQUALS -> fieldValue.toString() != conditionValue
            ConditionOperator.GREATER_THAN -> compareNumeric(fieldValue, conditionValue) > 0
            ConditionOperator.LESS_THAN -> compareNumeric(fieldValue, conditionValue) < 0
            ConditionOperator.GREATER_THAN_OR_EQUAL -> compareNumeric(fieldValue, conditionValue) >= 0
            ConditionOperator.LESS_THAN_OR_EQUAL -> compareNumeric(fieldValue, conditionValue) <= 0
            ConditionOperator.CONTAINS -> fieldValue.toString().contains(conditionValue)
            ConditionOperator.NOT_CONTAINS -> !fieldValue.toString().contains(conditionValue)
            ConditionOperator.STARTS_WITH -> fieldValue.toString().startsWith(conditionValue)
            ConditionOperator.ENDS_WITH -> fieldValue.toString().endsWith(conditionValue)
            ConditionOperator.MATCHES_REGEX -> fieldValue.toString().matches(Regex(conditionValue))
            ConditionOperator.IN_LIST -> conditionValue.split(",").any { it.trim() == fieldValue.toString() }
            ConditionOperator.NOT_IN_LIST -> conditionValue.split(",").none { it.trim() == fieldValue.toString() }
            ConditionOperator.IS_NULL -> fieldValue.toString().isEmpty()
            ConditionOperator.IS_NOT_NULL -> fieldValue.toString().isNotEmpty()
        }
    }
    
    private fun compareNumeric(fieldValue: Any, conditionValue: String): Int {
        val fieldNum = when (fieldValue) {
            is Number -> fieldValue.toDouble()
            is String -> fieldValue.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
        val conditionNum = conditionValue.toDoubleOrNull() ?: 0.0
        return fieldNum.compareTo(conditionNum)
    }
    
    private fun deduplicateActions(actions: List<RuleAction>): List<RuleAction> {
        // Remove duplicate action types, keeping the first occurrence
        val seen = mutableSetOf<ActionType>()
        return actions.filter { action ->
            if (seen.contains(action.type)) {
                false
            } else {
                seen.add(action.type)
                true
            }
        }
    }
    
    private fun recordRuleExecution(
        rule: SecurityRule,
        scanContext: ScanContext,
        matchResult: RuleMatchResult
    ) {
        val record = RuleExecutionRecord(
            ruleId = rule.id,
            executedAt = System.currentTimeMillis(),
            matched = matchResult.matched,
            matchedConditionCount = matchResult.matchedConditions.size,
            deviceId = scanContext.deviceId,
            ticketId = null // Would extract from context
        )
        
        val history = ruleExecutionHistory.getOrPut(rule.id) { mutableListOf() }
        history.add(record)
        
        // Keep only last 1000 records per rule
        if (history.size > 1000) {
            history.removeAt(0)
        }
    }
}

/**
 * Rule evaluation result
 */
data class RuleEvaluationResult(
    val matchedRules: List<RuleMatch>,
    val actions: List<RuleAction>,
    val evaluationContext: Map<String, Any>
) {
    val hasBlockAction: Boolean
        get() = actions.any { it.type == ActionType.BLOCK }
    
    val hasAlertAction: Boolean
        get() = actions.any { it.type == ActionType.ALERT }
    
    val hasFlagAction: Boolean
        get() = actions.any { it.type == ActionType.FLAG }
    
    val highestPriorityRule: SecurityRule?
        get() = matchedRules.maxByOrNull { it.rule.priority }?.rule
}

/**
 * Rule match information
 */
data class RuleMatch(
    val rule: SecurityRule,
    val matchedConditions: List<RuleCondition>,
    val evaluationTime: Long
)

/**
 * Rule match result
 */
data class RuleMatchResult(
    val matched: Boolean,
    val matchedConditions: List<RuleCondition>
)

/**
 * Rule execution record for history tracking
 */
data class RuleExecutionRecord(
    val ruleId: String,
    val executedAt: Long,
    val matched: Boolean,
    val matchedConditionCount: Int,
    val deviceId: String?,
    val ticketId: String?
)

/**
 * Rule builder for creating rules programmatically
 */
class SecurityRuleBuilder {
    private var id: String = java.util.UUID.randomUUID().toString()
    private var name: String = ""
    private var description: String = ""
    private var enabled: Boolean = true
    private var priority: Int = 0
    private val conditions = mutableListOf<RuleCondition>()
    private val actions = mutableListOf<RuleAction>()
    private var scope: RuleScope = RuleScope.GLOBAL
    private val metadata = mutableMapOf<String, String>()
    
    fun id(id: String) = apply { this.id = id }
    fun name(name: String) = apply { this.name = name }
    fun description(description: String) = apply { this.description = description }
    fun enabled(enabled: Boolean) = apply { this.enabled = enabled }
    fun priority(priority: Int) = apply { this.priority = priority }
    fun scope(scope: RuleScope) = apply { this.scope = scope }
    
    fun condition(
        field: String,
        operator: ConditionOperator,
        value: String,
        logicalOperator: LogicalOperator? = null
    ) = apply {
        conditions.add(RuleCondition(field, operator, value, logicalOperator))
    }
    
    fun action(
        type: ActionType,
        parameters: Map<String, String> = emptyMap(),
        order: Int = actions.size
    ) = apply {
        actions.add(RuleAction(type, parameters, order))
    }
    
    fun metadata(key: String, value: String) = apply {
        metadata[key] = value
    }
    
    fun build(): SecurityRule {
        return SecurityRule(
            id = id,
            name = name,
            description = description,
            enabled = enabled,
            priority = priority,
            conditions = conditions,
            actions = actions,
            appliesTo = scope,
            metadata = metadata
        )
    }
}

/**
 * DSL for creating security rules
 */
fun securityRule(init: SecurityRuleBuilder.() -> Unit): SecurityRule {
    return SecurityRuleBuilder().apply(init).build()
}
