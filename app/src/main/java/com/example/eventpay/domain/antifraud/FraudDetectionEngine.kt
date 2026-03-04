package com.example.eventpay.domain.antifraud

import android.location.Location
import android.util.Base64
import com.example.eventpay.domain.qrcode.QRCodePayload
import com.example.eventpay.domain.qrcode.QRCodeScanRecord
import com.example.eventpay.security.QRCryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Fraud Detection Engine
 * 
 * Core fraud detection system that analyzes QR code scans and user behavior
 * to identify potential fraudulent activities.
 * 
 * Detection Capabilities:
 * - Duplicate scan detection
 * - Replay attack prevention
 * - Timing anomaly detection
 * - Location-based fraud detection
 * - Device fingerprinting
 * - Pattern analysis
 * - Velocity checks
 */
@Singleton
class FraudDetectionEngine @Inject constructor(
    private val cryptoManager: QRCryptoManager,
    private val detectionConfig: FraudDetectionConfig = FraudDetectionConfig()
) {
    
    // In-memory caches for detection (would use database in production)
    private val scanHistory = mutableMapOf<String, MutableList<ScanContext>>()
    private val nonceHistory = mutableMapOf<String, Long>()
    private val deviceFingerprints = mutableMapOf<String, DeviceFingerprint>()
    private val failedAttempts = mutableMapOf<String, MutableList<Long>>()
    private val userWarnings = mutableMapOf<String, Int>()
    
    /**
     * Analyze a QR scan for potential fraud
     * 
     * @param scanContext The context of the current scan
     * @param payload The decrypted QR code payload
     * @param previousScan Optional previous scan record for comparison
     * @return FraudDetectionResult with analysis results
     */
    suspend fun analyzeScan(
        scanContext: ScanContext,
        payload: QRCodePayload.TicketPayload,
        previousScan: QRCodeScanRecord? = null
    ): FraudDetectionResult = withContext(Dispatchers.Default) {
        
        val indicators = mutableListOf<FraudIndicator>()
        val evidence = mutableListOf<FraudEvidence>()
        val riskFactors = mutableListOf<RiskFactor>()
        
        // Run all detection checks
        val duplicateCheck = checkDuplicateScan(scanContext, payload)
        val replayCheck = checkReplayAttack(scanContext, payload)
        val timingCheck = checkTimingAnomalies(scanContext, payload)
        val locationCheck = checkLocationAnomalies(scanContext, payload)
        val deviceCheck = checkDeviceAnomalies(scanContext, payload)
        val patternCheck = checkSuspiciousPatterns(scanContext, payload)
        val velocityCheck = checkVelocityAbuse(scanContext, payload)
        
        // Collect all indicators
        indicators.addAll(duplicateCheck.indicators)
        indicators.addAll(replayCheck.indicators)
        indicators.addAll(timingCheck.indicators)
        indicators.addAll(locationCheck.indicators)
        indicators.addAll(deviceCheck.indicators)
        indicators.addAll(patternCheck.indicators)
        indicators.addAll(velocityCheck.indicators)
        
        // Collect evidence
        evidence.addAll(collectEvidence(scanContext, payload))
        
        // Calculate risk score
        val riskScore = calculateRiskScore(indicators)
        riskFactors.addAll(assessRiskFactors(indicators, scanContext))
        
        // Determine fraud type and severity
        val (fraudType, severity) = determineFraudTypeAndSeverity(indicators, riskScore)
        
        // Create detection result
        FraudDetectionResult(
            timestamp = System.currentTimeMillis(),
            fraudType = fraudType,
            severity = severity,
            ticketId = payload.ticketId,
            eventId = payload.eventId,
            userId = payload.userId,
            deviceId = scanContext.deviceId,
            confidence = calculateConfidence(indicators),
            indicators = indicators,
            evidence = evidence,
            riskScore = riskScore,
            riskFactors = riskFactors
        )
    }
    
    /**
     * Quick fraud check for real-time validation
     * Returns immediately with basic fraud assessment
     */
    fun quickFraudCheck(
        scanContext: ScanContext,
        payload: QRCodePayload.TicketPayload
    ): QuickFraudAssessment {
        val criticalIndicators = mutableListOf<FraudIndicator>()
        
        // Check for critical fraud indicators only
        val nonceKey = "${payload.ticketId}:${payload.nonce}"
        if (nonceHistory.containsKey(nonceKey)) {
            criticalIndicators.add(
                FraudIndicator(
                    type = IndicatorType.NONCE_REUSE,
                    description = "QR code nonce has been used before",
                    weight = 1.0,
                    value = payload.nonce,
                    threshold = "Unique nonce required"
                )
            )
        }
        
        // Check for duplicate scan
        val ticketKey = "${payload.eventId}:${payload.ticketId}"
        val history = scanHistory[ticketKey]
        if (history != null && history.isNotEmpty()) {
            criticalIndicators.add(
                FraudIndicator(
                    type = IndicatorType.NONCE_REUSE,
                    description = "Ticket has been scanned before",
                    weight = 0.9,
                    value = "${history.size} previous scans",
                    threshold = "0 previous scans"
                )
            )
        }
        
        // Check device blacklist
        val deviceFingerprint = deviceFingerprints[scanContext.deviceId]
        if (deviceFingerprint?.isBlacklisted == true) {
            criticalIndicators.add(
                FraudIndicator(
                    type = IndicatorType.BLACKLISTED_ITEM,
                    description = "Device is blacklisted: ${deviceFingerprint.blacklistReason}",
                    weight = 1.0,
                    value = scanContext.deviceId,
                    threshold = "Not blacklisted"
                )
            )
        }
        
        val riskScore = criticalIndicators.sumOf { it.weight * 100 }.toInt().coerceIn(0, 100)
        
        return QuickFraudAssessment(
            isSafe = criticalIndicators.isEmpty() && riskScore < detectionConfig.criticalThreshold,
            riskScore = riskScore,
            criticalIndicators = criticalIndicators,
            requiresFullAnalysis = riskScore >= detectionConfig.fullAnalysisThreshold
        )
    }
    
    /**
     * Record a scan for future fraud detection
     */
    fun recordScan(scanContext: ScanContext, payload: QRCodePayload.TicketPayload) {
        // Record nonce usage
        val nonceKey = "${payload.ticketId}:${payload.nonce}"
        nonceHistory[nonceKey] = System.currentTimeMillis()
        
        // Record scan history
        val ticketKey = "${payload.eventId}:${payload.ticketId}"
        val history = scanHistory.getOrPut(ticketKey) { mutableListOf() }
        history.add(scanContext)
        
        // Update device fingerprint
        updateDeviceFingerprint(scanContext)
        
        // Clean old records
        cleanOldRecords()
    }
    
    /**
     * Record a failed scan attempt
     */
    fun recordFailedAttempt(deviceId: String, reason: String) {
        val attempts = failedAttempts.getOrPut(deviceId) { mutableListOf() }
        attempts.add(System.currentTimeMillis())
        
        // Check for brute force
        val recentAttempts = attempts.filter { 
            System.currentTimeMillis() - it < detectionConfig.bruteForceWindowMs 
        }
        if (recentAttempts.size >= detectionConfig.bruteForceThreshold) {
            // Flag device for brute force
            deviceFingerprints[deviceId]?.let { fp ->
                deviceFingerprints[deviceId] = fp.copy(
                    riskScore = 100,
                    riskFactors = fp.riskFactors + "BRUTE_FORCE_DETECTED"
                )
            }
        }
    }
    
    /**
     * Issue a warning to a user
     */
    fun issueWarning(userId: String): Int {
        val warnings = (userWarnings[userId] ?: 0) + 1
        userWarnings[userId] = warnings
        return warnings
    }
    
    // ============================================================================
    // DETECTION CHECKS
    // ============================================================================
    
    private fun checkDuplicateScan(
        scanContext: ScanContext,
        payload: QRCodePayload.TicketPayload
    ): DetectionCheckResult {
        val indicators = mutableListOf<FraudIndicator>()
        
        // Check nonce reuse
        val nonceKey = "${payload.ticketId}:${payload.nonce}"
        if (nonceHistory.containsKey(nonceKey)) {
            val usedAt = nonceHistory[nonceKey]!!
            indicators.add(
                FraudIndicator(
                    type = IndicatorType.NONCE_REUSE,
                    description = "QR code nonce was already used",
                    weight = 1.0,
                    value = "Used at ${usedAt}",
                    threshold = "Never used"
                )
            )
        }
        
        // Check ticket scan history
        val ticketKey = "${payload.eventId}:${payload.ticketId}"
        val history = scanHistory[ticketKey]
        if (history != null && history.isNotEmpty()) {
            val lastScan = history.last()
            indicators.add(
                FraudIndicator(
                    type = IndicatorType.REPEATED_FAILURES,
                    description = "Ticket has ${history.size} previous scan(s)",
                    weight = 0.8,
                    value = "${history.size} scans",
                    threshold = "0 scans"
                )
            )
            
            // Check if same device
            if (lastScan.deviceId != scanContext.deviceId) {
                indicators.add(
                    FraudIndicator(
                        type = IndicatorType.MULTIPLE_DEVICES,
                        description = "Ticket scanned from different device",
                        weight = 0.7,
                        value = "Devices: ${lastScan.deviceId}, ${scanContext.deviceId}",
                        threshold = "Same device"
                    )
                )
            }
        }
        
        return DetectionCheckResult(indicators)
    }
    
    private fun checkReplayAttack(
        scanContext: ScanContext,
        payload: QRCodePayload.TicketPayload
    ): DetectionCheckResult {
        val indicators = mutableListOf<FraudIndicator>()
        
        // Check timestamp validity
        val now = System.currentTimeMillis()
        val qrAge = now - payload.timestamp
        
        if (qrAge > detectionConfig.qrMaxAgeMs) {
            indicators.add(
                FraudIndicator(
                    type = IndicatorType.TIMESTAMP_ANOMALY,
                    description = "QR code is too old",
                    weight = 0.9,
                    value = "${qrAge / 1000} seconds old",
                    threshold = "${detectionConfig.qrMaxAgeMs / 1000} seconds max"
                )
            )
        }
        
        // Check for future timestamp (clock manipulation)
        if (payload.timestamp > now + detectionConfig.clockSkewToleranceMs) {
            indicators.add(
                FraudIndicator(
                    type = IndicatorType.TIMESTAMP_ANOMALY,
                    description = "QR code timestamp is in the future",
                    weight = 1.0,
                    value = "${(payload.timestamp - now) / 1000} seconds in future",
                    threshold = "Not in future"
                )
            )
        }
        
        // Check for timestamp manipulation (compared to previous scans)
        val ticketKey = "${payload.eventId}:${payload.ticketId}"
        val history = scanHistory[ticketKey]
        if (history != null && history.isNotEmpty()) {
            val lastPayload = history.last()
            // If same nonce but different timestamp, possible replay
            // This would require storing the payload data
        }
        
        return DetectionCheckResult(indicators)
    }
    
    private fun checkTimingAnomalies(
        scanContext: ScanContext,
        payload: QRCodePayload.TicketPayload
    ): DetectionCheckResult {
        val indicators = mutableListOf<FraudIndicator>()
        
        // Check scan frequency
        val deviceScans = scanHistory.values
            .flatten()
            .filter { it.deviceId == scanContext.deviceId }
            .filter { System.currentTimeMillis() - it.timestamp < detectionConfig.rapidScanWindowMs }
        
        if (deviceScans.size >= detectionConfig.rapidScanThreshold) {
            indicators.add(
                FraudIndicator(
                    type = IndicatorType.SCAN_FREQUENCY,
                    description = "Rapid scanning detected",
                    weight = 0.6,
                    value = "${deviceScans.size} scans in ${detectionConfig.rapidScanWindowMs / 1000}s",
                    threshold = "${detectionConfig.rapidScanThreshold} max"
                )
            )
        }
        
        // Check time between scans for same ticket
        val ticketKey = "${payload.eventId}:${payload.ticketId}"
        val history = scanHistory[ticketKey]
        if (history != null && history.isNotEmpty()) {
            val lastScan = history.last()
            val timeDiff = scanContext.timestamp - lastScan.timestamp
            
            if (timeDiff < detectionConfig.minTimeBetweenScansMs) {
                indicators.add(
                    FraudIndicator(
                        type = IndicatorType.TIME_BETWEEN_SCANS,
                        description = "Scans too close together",
                        weight = 0.5,
                        value = "${timeDiff}ms between scans",
                        threshold = "${detectionConfig.minTimeBetweenScansMs}ms minimum"
                    )
                )
            }
        }
        
        // Check for off-hours activity
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        if (hour < 6 || hour > 23) {
            indicators.add(
                FraudIndicator(
                    type = IndicatorType.TIME_OF_DAY,
                    description = "Activity during unusual hours",
                    weight = 0.2,
                    value = "$hour:00",
                    threshold = "06:00-23:00"
                )
            )
        }
        
        return DetectionCheckResult(indicators)
    }
    
    private fun checkLocationAnomalies(
        scanContext: ScanContext,
        payload: QRCodePayload.TicketPayload
    ): DetectionCheckResult {
        val indicators = mutableListOf<FraudIndicator>()
        
        val currentLat = scanContext.latitude
        val currentLon = scanContext.longitude
        
        if (currentLat != null && currentLon != null) {
            // Check for impossible travel
            val ticketKey = "${payload.eventId}:${payload.ticketId}"
            val history = scanHistory[ticketKey]
            
            if (history != null && history.isNotEmpty()) {
                val lastScan = history.last()
                val lastLat = lastScan.latitude
                val lastLon = lastScan.longitude
                
                if (lastLat != null && lastLon != null) {
                    val distance = calculateDistance(currentLat, currentLon, lastLat, lastLon)
                    val timeDiff = (scanContext.timestamp - lastScan.timestamp) / 1000.0 // seconds
                    
                    // Calculate required speed (km/h)
                    val requiredSpeed = if (timeDiff > 0) (distance / timeDiff) * 3600 else 0.0
                    
                    if (requiredSpeed > detectionConfig.maxPossibleSpeedKmh) {
                        indicators.add(
                            FraudIndicator(
                                type = IndicatorType.DISTANCE_VELOCITY,
                                description = "Impossible travel detected",
                                weight = 0.9,
                                value = "${String.format("%.1f", requiredSpeed)} km/h required",
                                threshold = "${detectionConfig.maxPossibleSpeedKmh} km/h max"
                            )
                        )
                    }
                }
            }
            
            // Check for GPS spoofing indicators
            if (scanContext.accuracy != null && scanContext.accuracy > detectionConfig.gpsAccuracyThreshold) {
                indicators.add(
                    FraudIndicator(
                        type = IndicatorType.GPS_SPOOFING,
                        description = "GPS accuracy suspiciously low",
                        weight = 0.4,
                        value = "${scanContext.accuracy}m accuracy",
                        threshold = "${detectionConfig.gpsAccuracyThreshold}m"
                    )
                )
            }
        }
        
        // Check for VPN/Proxy usage
        if (scanContext.isVpn || scanContext.isProxy) {
            indicators.add(
                FraudIndicator(
                    type = IndicatorType.LOCATION_MISMATCH,
                    description = "VPN or Proxy detected",
                    weight = 0.3,
                    value = "VPN: ${scanContext.isVpn}, Proxy: ${scanContext.isProxy}",
                    threshold = "No VPN/Proxy"
                )
            )
        }
        
        return DetectionCheckResult(indicators)
    }
    
    private fun checkDeviceAnomalies(
        scanContext: ScanContext,
        payload: QRCodePayload.TicketPayload
    ): DetectionCheckResult {
        val indicators = mutableListOf<FraudIndicator>()
        
        // Check for rooted device
        if (scanContext.isRooted) {
            indicators.add(
                FraudIndicator(
                    type = IndicatorType.ROOT_DETECTION,
                    description = "Device is rooted",
                    weight = 0.5,
                    value = "Rooted",
                    threshold = "Not rooted"
                )
            )
        }
        
        // Check for emulator
        if (scanContext.isEmulator) {
            indicators.add(
                FraudIndicator(
                    type = IndicatorType.EMULATOR_USAGE,
                    description = "App running on emulator",
                    weight = 0.7,
                    value = "Emulator detected",
                    threshold = "Physical device"
                )
            )
        }
        
        // Check for screen capture
        if (scanContext.screenCaptureActive || scanContext.screenRecordingActive) {
            indicators.add(
                FraudIndicator(
                    type = IndicatorType.SCREENSHOT_DETECTED,
                    description = "Screen capture/recording active",
                    weight = 0.6,
                    value = "Capture: ${scanContext.screenCaptureActive}, Recording: ${scanContext.screenRecordingActive}",
                    threshold = "No capture"
                )
            )
        }
        
        // Check for debug mode
        if (scanContext.debugModeEnabled) {
            indicators.add(
                FraudIndicator(
                    type = IndicatorType.DEVICE_FINGERPRINT,
                    description = "Debug mode enabled",
                    weight = 0.3,
                    value = "Debug enabled",
                    threshold = "Debug disabled"
                )
            )
        }
        
        // Check device fingerprint
        val fingerprint = deviceFingerprints[scanContext.deviceId]
        if (fingerprint != null) {
            if (fingerprint.isBlacklisted) {
                indicators.add(
                    FraudIndicator(
                        type = IndicatorType.BLACKLISTED_ITEM,
                        description = "Device is blacklisted",
                        weight = 1.0,
                        value = fingerprint.blacklistReason ?: "Unknown",
                        threshold = "Not blacklisted"
                    )
                )
            }
            
            if (fingerprint.riskScore > 70) {
                indicators.add(
                    FraudIndicator(
                        type = IndicatorType.DEVICE_FINGERPRINT,
                        description = "Device has high risk score",
                        weight = 0.6,
                        value = "Risk: ${fingerprint.riskScore}",
                        threshold = "Risk < 70"
                    )
                )
            }
        }
        
        return DetectionCheckResult(indicators)
    }
    
    private fun checkSuspiciousPatterns(
        scanContext: ScanContext,
        payload: QRCodePayload.TicketPayload
    ): DetectionCheckResult {
        val indicators = mutableListOf<FraudIndicator>()
        
        // Check for brute force attempts
        val attempts = failedAttempts[scanContext.deviceId]
        if (attempts != null) {
            val recentAttempts = attempts.filter {
                System.currentTimeMillis() - it < detectionConfig.bruteForceWindowMs
            }
            if (recentAttempts.size >= detectionConfig.bruteForceThreshold) {
                indicators.add(
                    FraudIndicator(
                        type = IndicatorType.REPEATED_FAILURES,
                        description = "Multiple failed attempts detected",
                        weight = 0.8,
                        value = "${recentAttempts.size} failed attempts",
                        threshold = "${detectionConfig.bruteForceThreshold} max"
                    )
                )
            }
        }
        
        // Check user warnings
        val warnings = userWarnings[payload.userId] ?: 0
        if (warnings >= detectionConfig.warningThreshold) {
            indicators.add(
                FraudIndicator(
                    type = IndicatorType.UNUSUAL_PATTERN,
                    description = "User has multiple warnings",
                    weight = 0.5,
                    value = "$warnings warnings",
                    threshold = "${detectionConfig.warningThreshold} max"
                )
            )
        }
        
        // Check for ticket sharing pattern
        val ticketKey = "${payload.eventId}:${payload.ticketId}"
        val history = scanHistory[ticketKey]
        if (history != null && history.size > 1) {
            val uniqueDevices = history.map { it.deviceId }.distinct()
            if (uniqueDevices.size >= detectionConfig.deviceSharingThreshold) {
                indicators.add(
                    FraudIndicator(
                        type = IndicatorType.MULTIPLE_DEVICES,
                        description = "Ticket used on multiple devices",
                        weight = 0.7,
                        value = "${uniqueDevices.size} different devices",
                        threshold = "${detectionConfig.deviceSharingThreshold} max"
                    )
                )
            }
        }
        
        return DetectionCheckResult(indicators)
    }
    
    private fun checkVelocityAbuse(
        scanContext: ScanContext,
        payload: QRCodePayload.TicketPayload
    ): DetectionCheckResult {
        val indicators = mutableListOf<FraudIndicator>()
        
        // Check global scan velocity
        val allRecentScans = scanHistory.values
            .flatten()
            .filter { System.currentTimeMillis() - it.timestamp < detectionConfig.velocityWindowMs }
        
        if (allRecentScans.size > detectionConfig.globalVelocityLimit) {
            indicators.add(
                FraudIndicator(
                    type = IndicatorType.SCAN_FREQUENCY,
                    description = "Global scan velocity exceeded",
                    weight = 0.4,
                    value = "${allRecentScans.size} scans in window",
                    threshold = "${detectionConfig.globalVelocityLimit}"
                )
            )
        }
        
        // Check per-user velocity
        val userScans = allRecentScans.filter { 
            // Would need userId in ScanContext
            true 
        }
        
        // Check per-device velocity
        val deviceScans = allRecentScans.filter { it.deviceId == scanContext.deviceId }
        if (deviceScans.size > detectionConfig.deviceVelocityLimit) {
            indicators.add(
                FraudIndicator(
                    type = IndicatorType.SCAN_FREQUENCY,
                    description = "Device scan velocity exceeded",
                    weight = 0.5,
                    value = "${deviceScans.size} scans from this device",
                    threshold = "${detectionConfig.deviceVelocityLimit}"
                )
            )
        }
        
        return DetectionCheckResult(indicators)
    }
    
    // ============================================================================
    // HELPER METHODS
    // ============================================================================
    
    private fun collectEvidence(
        scanContext: ScanContext,
        payload: QRCodePayload.TicketPayload
    ): List<FraudEvidence> {
        val evidence = mutableListOf<FraudEvidence>()
        
        // QR code data evidence
        evidence.add(
            FraudEvidence(
                type = EvidenceType.QR_CODE_DATA,
                description = "QR code payload hash",
                data = hashData(payload.toString()),
                integrity = calculateIntegrity(payload.toString())
            )
        )
        
        // Scan metadata evidence
        evidence.add(
            FraudEvidence(
                type = EvidenceType.SCAN_METADATA,
                description = "Scan context information",
                data = hashData(scanContext.toString()),
                integrity = calculateIntegrity(scanContext.toString())
            )
        )
        
        // Device info evidence
        evidence.add(
            FraudEvidence(
                type = EvidenceType.DEVICE_INFO,
                description = "Device fingerprint",
                data = scanContext.deviceFingerprint,
                integrity = calculateIntegrity(scanContext.deviceFingerprint)
            )
        )
        
        // Location data evidence
        if (scanContext.latitude != null && scanContext.longitude != null) {
            evidence.add(
                FraudEvidence(
                    type = EvidenceType.LOCATION_DATA,
                    description = "GPS coordinates",
                    data = "${scanContext.latitude},${scanContext.longitude}",
                    integrity = calculateIntegrity("${scanContext.latitude},${scanContext.longitude}")
                )
            )
        }
        
        return evidence
    }
    
    private fun calculateRiskScore(indicators: List<FraudIndicator>): Int {
        if (indicators.isEmpty()) return 0
        
        val weightedSum = indicators.sumOf { it.weight * 100 }
        val maxPossible = indicators.size * 100.0
        
        // Normalize to 0-100
        return ((weightedSum / maxPossible) * 100).toInt().coerceIn(0, 100)
    }
    
    private fun calculateConfidence(indicators: List<FraudIndicator>): Double {
        if (indicators.isEmpty()) return 0.0
        
        // Higher confidence with more indicators and higher weights
        val avgWeight = indicators.map { it.weight }.average()
        val indicatorCount = indicators.size.toDouble()
        
        // Confidence increases with more indicators and higher average weight
        return (avgWeight * (1.0 - (1.0 / (indicatorCount + 1.0)))).coerceIn(0.0, 1.0)
    }
    
    private fun assessRiskFactors(
        indicators: List<FraudIndicator>,
        scanContext: ScanContext
    ): List<RiskFactor> {
        val factors = mutableListOf<RiskFactor>()
        
        // Group indicators by type
        val groupedIndicators = indicators.groupBy { it.type }
        
        // Create risk factors from indicator groups
        groupedIndicators.forEach { (type, typeIndicators) ->
            val category = when (type) {
                IndicatorType.SCAN_FREQUENCY, IndicatorType.TIME_BETWEEN_SCANS, IndicatorType.TIME_OF_DAY -> RiskCategory.TEMPORAL
                IndicatorType.LOCATION_MISMATCH, IndicatorType.DISTANCE_VELOCITY, IndicatorType.GPS_SPOOFING -> RiskCategory.LOCATION
                IndicatorType.DEVICE_CHANGE, IndicatorType.MULTIPLE_DEVICES, IndicatorType.DEVICE_FINGERPRINT -> RiskCategory.DEVICE
                IndicatorType.NONCE_REUSE, IndicatorType.TIMESTAMP_ANOMALY, IndicatorType.SIGNATURE_INVALID, IndicatorType.CHECKSUM_MISMATCH -> RiskCategory.TECHNICAL
                IndicatorType.SCREENSHOT_DETECTED, IndicatorType.SCREEN_RECORDING, IndicatorType.EMULATOR_USAGE, IndicatorType.ROOT_DETECTION -> RiskCategory.BEHAVIORAL
                IndicatorType.REPEATED_FAILURES, IndicatorType.UNUSUAL_PATTERN, IndicatorType.BLACKLISTED_ITEM -> RiskCategory.HISTORICAL
            }
            
            val maxWeight = typeIndicators.maxOf { it.weight }
            val avgScore = typeIndicators.map { (it.weight * 100).toInt() }.average().toInt()
            
            factors.add(
                RiskFactor(
                    category = category,
                    name = type.name,
                    description = typeIndicators.first().description,
                    weight = (maxWeight * 10).toInt(),
                    score = avgScore
                )
            )
        }
        
        return factors
    }
    
    private fun determineFraudTypeAndSeverity(
        indicators: List<FraudIndicator>,
        riskScore: Int
    ): Pair<FraudType, FraudSeverity> {
        // Determine fraud type based on indicators
        val fraudType = when {
            indicators.any { it.type == IndicatorType.NONCE_REUSE } -> FraudType.REPLAY_ATTACK
            indicators.any { it.type == IndicatorType.BLACKLISTED_ITEM } -> FraudType.FORGED_QR
            indicators.any { it.type == IndicatorType.DISTANCE_VELOCITY } -> FraudType.IMPOSSIBLE_TRAVEL
            indicators.any { it.type == IndicatorType.SCREENSHOT_DETECTED } -> FraudType.SCREENSHOT_DETECTED
            indicators.any { it.type == IndicatorType.MULTIPLE_DEVICES } -> FraudType.MULTIPLE_DEVICE_SCAN
            indicators.any { it.type == IndicatorType.REPEATED_FAILURES } -> FraudType.BRUTE_FORCE_ATTEMPT
            indicators.any { it.type == IndicatorType.SCAN_FREQUENCY } -> FraudType.RAPID_SCANS
            indicators.any { it.type == IndicatorType.TIMESTAMP_ANOMALY } -> FraudType.EXPIRED_QR_REUSE
            indicators.any { it.type == IndicatorType.ROOT_DETECTION || it.type == IndicatorType.EMULATOR_USAGE } -> FraudType.TAMPERED_QR
            indicators.isNotEmpty() -> FraudType.PATTERN_ABUSE
            else -> FraudType.PATTERN_ABUSE
        }
        
        // Determine severity based on risk score
        val severity = when {
            riskScore >= 80 -> FraudSeverity.CRITICAL
            riskScore >= 60 -> FraudSeverity.HIGH
            riskScore >= 40 -> FraudSeverity.MEDIUM
            riskScore > 0 -> FraudSeverity.LOW
            else -> FraudSeverity.LOW
        }
        
        return Pair(fraudType, severity)
    }
    
    private fun updateDeviceFingerprint(scanContext: ScanContext) {
        val existing = deviceFingerprints[scanContext.deviceId]
        
        if (existing != null) {
            deviceFingerprints[scanContext.deviceId] = existing.copy(
                lastSeen = System.currentTimeMillis(),
                seenCount = existing.seenCount + 1,
                isRooted = existing.isRooted || scanContext.isRooted,
                isEmulator = existing.isEmulator || scanContext.isEmulator
            )
        } else {
            deviceFingerprints[scanContext.deviceId] = DeviceFingerprint(
                deviceId = scanContext.deviceId,
                fingerprint = scanContext.deviceFingerprint,
                isRooted = scanContext.isRooted,
                isEmulator = scanContext.isEmulator
            )
        }
    }
    
    private fun cleanOldRecords() {
        val now = System.currentTimeMillis()
        
        // Clean old nonce history
        nonceHistory.entries.removeIf { now - it.value > detectionConfig.nonceRetentionMs }
        
        // Clean old scan history
        scanHistory.forEach { (_, scans) ->
            scans.removeIf { now - it.timestamp > detectionConfig.scanRetentionMs }
        }
        
        // Clean old failed attempts
        failedAttempts.forEach { (_, attempts) ->
            attempts.removeIf { now - it > detectionConfig.bruteForceWindowMs }
        }
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0] / 1000.0 // Convert to kilometers
    }
    
    private fun hashData(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data.toByteArray())
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }
    
    private fun calculateIntegrity(data: String): String {
        return hashData(data + System.currentTimeMillis())
    }
    
    /**
     * Clear all detection data (for testing)
     */
    fun clearAllData() {
        scanHistory.clear()
        nonceHistory.clear()
        deviceFingerprints.clear()
        failedAttempts.clear()
        userWarnings.clear()
    }
}

/**
 * Detection check result
 */
data class DetectionCheckResult(
    val indicators: List<FraudIndicator>
)

/**
 * Quick fraud assessment for real-time validation
 */
data class QuickFraudAssessment(
    val isSafe: Boolean,
    val riskScore: Int,
    val criticalIndicators: List<FraudIndicator>,
    val requiresFullAnalysis: Boolean
)

/**
 * Fraud Detection Configuration
 */
data class FraudDetectionConfig(
    // QR Code validity
    val qrMaxAgeMs: Long = 24 * 60 * 60 * 1000, // 24 hours
    val clockSkewToleranceMs: Long = 5 * 60 * 1000, // 5 minutes
    
    // Timing thresholds
    val rapidScanWindowMs: Long = 60 * 1000, // 1 minute
    val rapidScanThreshold: Int = 10,
    val minTimeBetweenScansMs: Long = 1000, // 1 second
    
    // Location thresholds
    val maxPossibleSpeedKmh: Double = 300.0, // Max travel speed
    val gpsAccuracyThreshold: Float = 100.0f, // meters
    
    // Brute force detection
    val bruteForceWindowMs: Long = 5 * 60 * 1000, // 5 minutes
    val bruteForceThreshold: Int = 5,
    
    // Velocity limits
    val velocityWindowMs: Long = 60 * 1000, // 1 minute
    val globalVelocityLimit: Int = 100,
    val deviceVelocityLimit: Int = 20,
    
    // Pattern detection
    val warningThreshold: Int = 3,
    val deviceSharingThreshold: Int = 2,
    
    // Risk thresholds
    val criticalThreshold: Int = 80,
    val highThreshold: Int = 60,
    val fullAnalysisThreshold: Int = 30,
    
    // Data retention
    val nonceRetentionMs: Long = 7 * 24 * 60 * 60 * 1000, // 7 days
    val scanRetentionMs: Long = 30 * 24 * 60 * 60 * 1000 // 30 days
)
