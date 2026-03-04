package com.example.eventpay.domain.auth

/**
 * Authentication Domain Models
 * 
 * Contains all authentication-related domain entities.
 * These are pure Kotlin classes independent of any framework.
 */

/**
 * User Role Enumeration
 *
 * Two roles:
 * - ADMIN: Full system access — manage events, users, analytics
 * - SCANNER: Staff user — can only scan QR codes for event check-in
 */
enum class UserRole {
    ADMIN,
    SCANNER;

    fun displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }

    fun canManageUsers(): Boolean = this == ADMIN
    fun canManageEvents(): Boolean = this == ADMIN
    fun canScanQR(): Boolean = true
    fun canViewReports(): Boolean = this == ADMIN
    fun canAccessAllEvents(): Boolean = this == ADMIN
    fun canViewFinancials(): Boolean = this == ADMIN
}

/**
 * Authenticated User Domain Model
 * 
 * Represents a fully authenticated user with session information.
 */
data class AuthenticatedUser(
    val id: String,
    val email: String,
    val fullName: String,
    val role: UserRole,
    val profileImageUrl: String? = null,
    val organization: String? = null,
    val permissions: Set<Permission> = emptySet(),
    val session: SessionInfo,
    val active: Boolean = true
) {
    /**
     * Check if user has a specific permission
     */
    fun hasPermission(permission: Permission): Boolean {
        return permissions.contains(permission) || role.getDefaultPermissions().contains(permission)
    }
    
    /**
     * Check if user has any of the specified permissions
     */
    fun hasAnyPermission(vararg permissions: Permission): Boolean {
        return permissions.any { hasPermission(it) }
    }
    
    /**
     * Check if session is valid
     */
    fun isSessionValid(): Boolean {
        return session.isValid()
    }
    
    /**
     * Check if user account is active
     */
    fun isActive(): Boolean = active
}

/**
 * Session Information
 * 
 * Contains session metadata for security tracking.
 */
data class SessionInfo(
    val sessionId: String,
    val createdAt: Long,
    val expiresAt: Long,
    val lastActivityAt: Long,
    val deviceId: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null
) {
    /**
     * Check if session is expired
     */
    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
    
    /**
     * Check if session is valid
     */
    fun isValid(): Boolean = !isExpired()
    
    /**
     * Get remaining session time in milliseconds
     */
    fun remainingTime(): Long = maxOf(0L, expiresAt - System.currentTimeMillis())
    
    /**
     * Check if session needs refresh (less than 5 minutes remaining)
     */
    fun needsRefresh(): Boolean = remainingTime() < (5 * 60 * 1000)
}

/**
 * Permission Enumeration
 * 
 * Fine-grained permissions for role-based access control.
 */
enum class Permission {
    // User Management
    CREATE_USER,
    READ_USER,
    UPDATE_USER,
    DELETE_USER,
    MANAGE_ROLES,
    
    // Event Management
    CREATE_EVENT,
    READ_EVENT,
    UPDATE_EVENT,
    DELETE_EVENT,
    PUBLISH_EVENT,
    
    // Ticket Management
    CREATE_TICKET,
    READ_TICKET,
    UPDATE_TICKET,
    REFUND_TICKET,
    
    // Check-in
    SCAN_QR,
    CHECK_IN_ATTENDEE,
    VIEW_CHECK_IN_HISTORY,
    
    // Financial
    VIEW_TRANSACTIONS,
    PROCESS_SALE,
    PROCESS_REFUND,
    VIEW_REPORTS,
    MANAGE_WALLET,
    
    // System
    MANAGE_SETTINGS,
    VIEW_ANALYTICS,
    EXPORT_DATA
}

/**
 * Get default permissions for each role
 */
fun UserRole.getDefaultPermissions(): Set<Permission> {
    return when (this) {
        UserRole.ADMIN -> Permission.values().toSet()

        UserRole.SCANNER -> setOf(
            Permission.READ_EVENT,
            Permission.READ_TICKET,
            Permission.SCAN_QR,
            Permission.CHECK_IN_ATTENDEE,
            Permission.VIEW_CHECK_IN_HISTORY
        )
    }
}

/**
 * Login Request Domain Model
 */
data class LoginRequest(
    val email: String,
    val password: String,
    val deviceId: String? = null,
    val rememberMe: Boolean = false
) {
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (email.isBlank()) errors.add("Email is required")
        if (!isValidEmail(email)) errors.add("Invalid email format")
        if (password.isBlank()) errors.add("Password is required")
        if (password.length < 6) errors.add("Password must be at least 6 characters")
        
        return if (errors.isEmpty()) ValidationResult.Success else ValidationResult.Error(errors)
    }
    
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}

/**
 * Registration Request Domain Model
 */
data class RegistrationRequest(
    val email: String,
    val password: String,
    val confirmPassword: String,
    val fullName: String,
    val role: UserRole = UserRole.SCANNER,
    val organization: String? = null,
    val invitationCode: String? = null // For admin-created accounts
) {
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (email.isBlank()) errors.add("Email is required")
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) 
            errors.add("Invalid email format")
        if (password.length < 8) errors.add("Password must be at least 8 characters")
        if (password != confirmPassword) errors.add("Passwords do not match")
        if (fullName.isBlank()) errors.add("Full name is required")
        if (fullName.length < 2) errors.add("Name must be at least 2 characters")
        
        // Password strength validation
        if (!isStrongPassword(password)) {
            errors.add("Password must contain uppercase, lowercase, number, and special character")
        }
        
        return if (errors.isEmpty()) ValidationResult.Success else ValidationResult.Error(errors)
    }
    
    private fun isStrongPassword(password: String): Boolean {
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }
        return hasUppercase && hasLowercase && hasDigit && hasSpecial
    }
}

/**
 * Validation Result
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val messages: List<String>) : ValidationResult()
    
    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error
}

/**
 * Authentication State for UI
 */
sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: AuthenticatedUser) : AuthState()
    data class Error(val message: String) : AuthState()
    
    fun isAuthenticated(): Boolean = this is Authenticated
    fun isLoading(): Boolean = this is Loading
    fun authenticatedUser(): AuthenticatedUser? = (this as? Authenticated)?.user
}

/**
 * Login Result
 */
sealed class LoginResult {
    data class Success(val user: AuthenticatedUser) : LoginResult()
    data class InvalidCredentials(val message: String = "Invalid email or password") : LoginResult()
    data class UserNotFound(val message: String = "User not found") : LoginResult()
    data class AccountDisabled(val message: String = "Account is disabled") : LoginResult()
    data class TooManyAttempts(val message: String = "Too many failed attempts. Try again later") : LoginResult()
    data class NetworkError(val message: String = "Network error. Please check your connection") : LoginResult()
    data class Error(val message: String, val exception: Throwable? = null) : LoginResult()
    
    fun isSuccess(): Boolean = this is Success
}

/**
 * Registration Result
 */
sealed class RegistrationResult {
    data class Success(val user: AuthenticatedUser) : RegistrationResult()
    data class EmailAlreadyExists(val message: String = "Email already registered") : RegistrationResult()
    data class WeakPassword(val message: String = "Password is too weak") : RegistrationResult()
    data class InvalidInvitationCode(val message: String = "Invalid invitation code") : RegistrationResult()
    data class Error(val message: String, val exception: Throwable? = null) : RegistrationResult()
    
    fun isSuccess(): Boolean = this is Success
}

/**
 * Password Reset Request
 */
data class PasswordResetRequest(
    val email: String
) {
    fun validate(): ValidationResult {
        return if (email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(listOf("Valid email is required"))
        }
    }
}

/**
 * Password Change Request
 */
data class PasswordChangeRequest(
    val currentPassword: String,
    val newPassword: String,
    val confirmNewPassword: String
) {
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (currentPassword.isBlank()) errors.add("Current password is required")
        if (newPassword.length < 8) errors.add("New password must be at least 8 characters")
        if (newPassword != confirmNewPassword) errors.add("New passwords do not match")
        if (currentPassword == newPassword) errors.add("New password must be different from current")
        
        return if (errors.isEmpty()) ValidationResult.Success else ValidationResult.Error(errors)
    }
}
