package com.example.eventpay.domain.antifraud

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Screenshot Protection Manager
 * 
 * Comprehensive screenshot detection and protection system for QR code tickets.
 * 
 * Protection Mechanisms:
 * - FLAG_SECURE: Prevents screenshot capture on most devices
 * - Content Observer: Detects when screenshots are saved to gallery
 * - Screen Capture Detection: Detects screen recording/capture APIs
 * - Visual Watermarking: Adds dynamic watermarks to QR codes
 * - Time-based Animation: Makes screenshots less useful
 * 
 * Detection Methods:
 * - File system monitoring for new screenshots
 * - Screen capture callback (Android 5.0+)
 * - Metadata analysis of captured images
 */
@Singleton
class ScreenshotProtectionManager @Inject constructor(
    private val context: Context
) {
    
    private val _screenshotDetection = MutableStateFlow<ScreenshotDetectionResult?>(null)
    val screenshotDetection: StateFlow<ScreenshotDetectionResult?> = _screenshotDetection.asStateFlow()
    
    private val _warningCount = MutableStateFlow(0)
    val warningCount: StateFlow<Int> = _warningCount.asStateFlow()
    
    private var contentObserver: ContentObserver? = null
    private var isMonitoring = false
    private var currentTicketId: String? = null
    private var currentEventId: String? = null
    private var currentUserId: String? = null
    private var currentDeviceId: String? = null
    
    private val screenshotHashes = mutableSetOf<String>()
    private val detectionListeners = mutableListOf<ScreenshotDetectionListener>()
    
    /**
     * Enable screenshot protection for an activity
     * 
     * @param activity The activity to protect
     * @param config Protection configuration
     */
    fun enableProtection(activity: Activity, config: ScreenshotProtectionConfig = ScreenshotProtectionConfig()) {
        // Set FLAG_SECURE to prevent screenshots
        if (config.detectionMethods.contains(ScreenshotDetectionMethod.DISPLAY_SECURE_FLAG)) {
            activity.window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
        
        // Start monitoring for screenshots
        if (config.detectionMethods.contains(ScreenshotDetectionMethod.CONTENT_OBSERVER)) {
            startScreenshotMonitoring(activity, config)
        }
    }
    
    /**
     * Disable screenshot protection for an activity
     */
    fun disableProtection(activity: Activity) {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        stopScreenshotMonitoring()
    }
    
    /**
     * Set the current ticket context for detection
     */
    fun setTicketContext(
        ticketId: String,
        eventId: String,
        userId: String,
        deviceId: String
    ) {
        this.currentTicketId = ticketId
        this.currentEventId = eventId
        this.currentUserId = userId
        this.currentDeviceId = deviceId
    }
    
    /**
     * Clear the current ticket context
     */
    fun clearTicketContext() {
        this.currentTicketId = null
        this.currentEventId = null
        this.currentUserId = null
        this.currentDeviceId = null
    }
    
    /**
     * Add a screenshot detection listener
     */
    fun addDetectionListener(listener: ScreenshotDetectionListener) {
        detectionListeners.add(listener)
    }
    
    /**
     * Remove a screenshot detection listener
     */
    fun removeDetectionListener(listener: ScreenshotDetectionListener) {
        detectionListeners.remove(listener)
    }
    
    /**
     * Apply watermark to a QR code image
     * 
     * @param bitmap The original QR code bitmap
     * @param userId User ID for watermark
     * @param timestamp Current timestamp
     * @return Watermarked bitmap
     */
    fun applyWatermark(
        bitmap: Bitmap,
        userId: String,
        timestamp: Long = System.currentTimeMillis()
    ): Bitmap {
        val watermarkedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(watermarkedBitmap)
        
        // Create semi-transparent watermark
        val paint = Paint().apply {
            color = Color.argb(50, 255, 0, 0) // Semi-transparent red
            textSize = 20f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        
        // Add timestamp watermark
        val timestampText = formatTimestamp(timestamp)
        canvas.drawText(
            timestampText,
            watermarkedBitmap.width / 2f,
            watermarkedBitmap.height - 30f,
            paint
        )
        
        // Add user ID hash watermark (for tracking)
        val userHash = hashString(userId).take(8)
        paint.textSize = 12f
        canvas.drawText(
            "ID: $userHash",
            watermarkedBitmap.width / 2f,
            watermarkedBitmap.height - 10f,
            paint
        )
        
        // Add diagonal watermark lines
        paint.color = Color.argb(20, 128, 128, 128)
        paint.strokeWidth = 1f
        paint.style = Paint.Style.STROKE
        
        for (i in -watermarkedBitmap.width..watermarkedBitmap.width step 50) {
            canvas.drawLine(
                i.toFloat(),
                0f,
                (i + watermarkedBitmap.height).toFloat(),
                watermarkedBitmap.height.toFloat(),
                paint
            )
        }
        
        return watermarkedBitmap
    }
    
    /**
     * Create a dynamic overlay for QR code display
     * Makes screenshots less useful by adding time-sensitive elements
     * 
     * @param view The view to apply overlay to
     * @param timestamp Current timestamp
     */
    fun applyDynamicOverlay(view: View, timestamp: Long) {
        // Add a time-based visual element that changes frequently
        // This makes screenshots quickly outdated
        val overlay = createDynamicOverlayView(view.context, timestamp)
        
        // The overlay would be added to the view hierarchy
        // Implementation depends on specific UI requirements
    }
    
    /**
     * Check if screen capture is currently active
     */
    fun isScreenCaptureActive(): Boolean {
        // Check for screen recording/capture
        return try {
            // This is a heuristic check - not 100% reliable
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) 
                as android.app.ActivityManager
            val runningProcesses = activityManager.runningAppProcesses
            
            runningProcesses?.any { processInfo ->
                val processName = processInfo.processName.lowercase()
                processName.contains("screenrecord") ||
                processName.contains("screencapture") ||
                processName.contains("screenrecorder") ||
                processName.contains("du_recorder") ||
                processName.contains("az_screen_recorder") ||
                processName.contains("mobizen") ||
                processName.contains("recme")
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if the app is being projected/mirrored
     */
    fun isScreenProjectionActive(): Boolean {
        // Check for display mirroring
        return try {
            val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) 
                as android.hardware.display.DisplayManager
            val displays = displayManager.displays
            
            displays.size > 1 // More than one display suggests mirroring
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get screenshot warning count for current session
     */
    fun getWarningCount(): Int = _warningCount.value
    
    /**
     * Reset warning count
     */
    fun resetWarningCount() {
        _warningCount.value = 0
    }
    
    /**
     * Check if a bitmap matches known screenshot patterns
     */
    fun analyzeBitmapForScreenshot(bitmap: Bitmap): ScreenshotAnalysisResult {
        val width = bitmap.width
        val height = bitmap.height
        
        // Check for common screenshot dimensions
        val isCommonScreenshotSize = isCommonScreenshotDimension(width, height)
        
        // Check for status bar presence (common in screenshots)
        val hasStatusBar = detectStatusBar(bitmap)
        
        // Check for navigation bar
        val hasNavigationBar = detectNavigationBar(bitmap)
        
        // Calculate confidence score
        var confidence = 0.0
        if (isCommonScreenshotSize) confidence += 0.3
        if (hasStatusBar) confidence += 0.3
        if (hasNavigationBar) confidence += 0.2
        
        // Check for our watermark
        val hasWatermark = detectWatermark(bitmap)
        if (hasWatermark) confidence += 0.2
        
        return ScreenshotAnalysisResult(
            isLikelyScreenshot = confidence > 0.5,
            confidence = confidence,
            hasWatermark = hasWatermark,
            dimensions = Pair(width, height),
            analysisDetails = mapOf(
                "commonSize" to isCommonScreenshotSize,
                "hasStatusBar" to hasStatusBar,
                "hasNavigationBar" to hasNavigationBar
            )
        )
    }
    
    // ============================================================================
    // PRIVATE METHODS
    // ============================================================================
    
    private fun startScreenshotMonitoring(activity: Activity, config: ScreenshotProtectionConfig) {
        if (isMonitoring) return
        
        val contentResolver = activity.contentResolver
        
        contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                
                if (uri != null) {
                    handleMediaStoreChange(uri, contentResolver, config)
                }
            }
        }
        
        // Register for media store changes
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver!!
        )
        
        isMonitoring = true
    }
    
    private fun stopScreenshotMonitoring() {
        contentObserver?.let {
            context.contentResolver.unregisterContentObserver(it)
        }
        contentObserver = null
        isMonitoring = false
    }
    
    private fun handleMediaStoreChange(
        uri: Uri,
        contentResolver: ContentResolver,
        config: ScreenshotProtectionConfig
    ) {
        try {
            // Query the new image
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DATA
            )
            
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                    val dateAddedIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                    val dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                    
                    val displayName = cursor.getString(displayNameIndex)
                    val dateAdded = cursor.getLong(dateAddedIndex)
                    val filePath = cursor.getString(dataIndex)
                    
                    // Check if this is a screenshot
                    if (isScreenshot(displayName, filePath)) {
                        handleScreenshotDetected(displayName, filePath, dateAdded, config)
                    }
                }
            }
        } catch (e: Exception) {
            // Handle error silently - some devices don't allow access
        }
    }
    
    private fun isScreenshot(displayName: String, filePath: String): Boolean {
        val lowerName = displayName.lowercase()
        val lowerPath = filePath.lowercase()
        
        // Check common screenshot naming patterns
        val screenshotPatterns = listOf(
            "screenshot",
            "screen_shot",
            "screen-shot",
            "screencapture",
            "screen_capture",
            "screen-capture",
            "img_",
            "image_"
        )
        
        return screenshotPatterns.any { pattern ->
            lowerName.contains(pattern) || lowerPath.contains(pattern)
        }
    }
    
    private fun handleScreenshotDetected(
        displayName: String,
        filePath: String,
        timestamp: Long,
        config: ScreenshotProtectionConfig
    ) {
        // Create hash of the screenshot for tracking
        val screenshotHash = hashString("$displayName:$filePath:$timestamp")
        
        // Avoid duplicate detections
        if (screenshotHashes.contains(screenshotHash)) {
            return
        }
        screenshotHashes.add(screenshotHash)
        
        // Increment warning count
        val currentWarnings = _warningCount.value + 1
        _warningCount.value = currentWarnings
        
        // Create detection result
        val result = ScreenshotDetectionResult(
            detected = true,
            timestamp = System.currentTimeMillis(),
            ticketId = currentTicketId,
            eventId = currentEventId,
            userId = currentUserId,
            deviceId = currentDeviceId,
            detectionMethod = ScreenshotDetectionMethod.CONTENT_OBSERVER,
            confidence = 0.9,
            actionTaken = determineAction(config, currentWarnings),
            additionalInfo = mapOf(
                "fileName" to displayName,
                "filePath" to filePath,
                "warningCount" to currentWarnings.toString()
            )
        )
        
        // Emit detection
        _screenshotDetection.value = result
        
        // Notify listeners
        notifyListeners(result)
    }
    
    private fun determineAction(config: ScreenshotProtectionConfig, warningCount: Int): ScreenshotAction {
        return when {
            warningCount >= config.blockAfterWarnings -> ScreenshotAction.BLOCK_TICKET
            config.alertAdmin -> ScreenshotAction.ALERT_ADMIN
            config.alertUser -> ScreenshotAction.ALERT_USER
            config.invalidateQR -> ScreenshotAction.INVALIDATE_QR
            else -> ScreenshotAction.SHOW_WARNING
        }
    }
    
    private fun notifyListeners(result: ScreenshotDetectionResult) {
        detectionListeners.forEach { listener ->
            try {
                listener.onScreenshotDetected(result)
            } catch (e: Exception) {
                // Handle listener error
            }
        }
    }
    
    private fun createDynamicOverlayView(context: Context, timestamp: Long): View {
        // Create a view with time-sensitive elements
        val imageView = ImageView(context)
        val bitmap = Bitmap.createBitmap(200, 50, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Draw time-based pattern
        val paint = Paint().apply {
            color = Color.argb(100, 0, 0, 0)
            textSize = 14f
            isAntiAlias = true
        }
        
        canvas.drawColor(Color.TRANSPARENT)
        canvas.drawText(
            formatTimestamp(timestamp),
            10f,
            30f,
            paint
        )
        
        // Add random pattern for uniqueness
        val random = java.util.Random(timestamp)
        for (i in 0..5) {
            paint.color = Color.argb(50, random.nextInt(256), random.nextInt(256), random.nextInt(256))
            canvas.drawCircle(
                random.nextFloat() * 200,
                random.nextFloat() * 50,
                random.nextFloat() * 10 + 2,
                paint
            )
        }
        
        imageView.setImageBitmap(bitmap)
        return imageView
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
    
    private fun hashString(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    private fun isCommonScreenshotDimension(width: Int, height: Int): Boolean {
        // Common screenshot dimensions (width, height pairs)
        val commonDimensions = listOf(
            Pair(1080, 1920),  // Full HD
            Pair(1440, 2560),  // QHD
            Pair(720, 1280),   // HD
            Pair(1080, 2160),  // 18:9
            Pair(1080, 2220),  // 18.5:9
            Pair(1080, 2240),  // 18.7:9
            Pair(1080, 2280),  // 19:9
            Pair(1080, 2340),  // 19.5:9
            Pair(1080, 2400),  // 20:9
            Pair(1440, 3200),  // QHD 20:9
            Pair(1440, 3120),  // QHD 19.5:9
        )
        
        return commonDimensions.any { (w, h) ->
            (width == w && height == h) || (width == h && height == w)
        }
    }
    
    private fun detectStatusBar(bitmap: Bitmap): Boolean {
        // Check top portion of bitmap for status bar characteristics
        val statusBarHeight = (bitmap.height * 0.03).toInt() // ~3% of height
        var darkPixels = 0
        var totalPixels = 0
        
        for (y in 0 until statusBarHeight) {
            for (x in 0 until bitmap.width step 10) {
                val pixel = bitmap.getPixel(x, y)
                val brightness = Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)
                if (brightness < 128) darkPixels++
                totalPixels++
            }
        }
        
        // Status bar typically has high contrast
        return darkPixels.toFloat() / totalPixels > 0.5f
    }
    
    private fun detectNavigationBar(bitmap: Bitmap): Boolean {
        // Check bottom portion of bitmap for navigation bar
        val navBarHeight = (bitmap.height * 0.03).toInt()
        var uniformPixels = 0
        var totalPixels = 0
        var lastPixel = bitmap.getPixel(0, bitmap.height - navBarHeight)
        
        for (y in bitmap.height - navBarHeight until bitmap.height) {
            for (x in 0 until bitmap.width step 10) {
                val pixel = bitmap.getPixel(x, y)
                if (pixel == lastPixel) uniformPixels++
                lastPixel = pixel
                totalPixels++
            }
        }
        
        // Navigation bar typically has uniform color
        return uniformPixels.toFloat() / totalPixels > 0.8f
    }
    
    private fun detectWatermark(bitmap: Bitmap): Boolean {
        // Check for our watermark patterns
        // This is a simplified check - real implementation would be more sophisticated
        val bottomRegion = Bitmap.createBitmap(
            bitmap,
            0,
            bitmap.height - 50,
            bitmap.width,
            50
        )
        
        // Check for red tint in bottom region (our watermark color)
        var redPixels = 0
        for (y in 0 until bottomRegion.height) {
            for (x in 0 until bottomRegion.width step 5) {
                val pixel = bottomRegion.getPixel(x, y)
                if (Color.red(pixel) > 200 && Color.green(pixel) < 50 && Color.blue(pixel) < 50) {
                    redPixels++
                }
            }
        }
        
        return redPixels > 5
    }
}

/**
 * Screenshot detection listener interface
 */
interface ScreenshotDetectionListener {
    fun onScreenshotDetected(result: ScreenshotDetectionResult)
}

/**
 * Screenshot analysis result
 */
data class ScreenshotAnalysisResult(
    val isLikelyScreenshot: Boolean,
    val confidence: Double,
    val hasWatermark: Boolean,
    val dimensions: Pair<Int, Int>,
    val analysisDetails: Map<String, Boolean>
)

/**
 * Secure QR code display helper
 */
class SecureQRDisplay(
    private val context: Context,
    private val screenshotProtectionManager: ScreenshotProtectionManager
) {
    private var isDisplaying = false
    private var updateJob: kotlinx.coroutines.Job? = null
    
    /**
     * Start secure display of QR code
     * Applies dynamic updates to make screenshots less useful
     */
    suspend fun startSecureDisplay(
        imageView: ImageView,
        baseBitmap: Bitmap,
        userId: String,
        updateIntervalMs: Long = 1000
    ) {
        isDisplaying = true
        
        withContext(Dispatchers.Main) {
            while (isDisplaying) {
                val timestamp = System.currentTimeMillis()
                val watermarkedBitmap = screenshotProtectionManager.applyWatermark(
                    baseBitmap,
                    userId,
                    timestamp
                )
                imageView.setImageBitmap(watermarkedBitmap)
                
                kotlinx.coroutines.delay(updateIntervalMs)
            }
        }
    }
    
    /**
     * Stop secure display
     */
    fun stopSecureDisplay() {
        isDisplaying = false
        updateJob?.cancel()
        updateJob = null
    }
}
