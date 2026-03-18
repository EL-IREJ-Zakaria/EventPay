package com.example.eventpay.domain.model

/**
 * Domain Entity - User
 * CMC School Event Management System
 */
data class User(
    val id: String,
    val email: String,
    val fullName: String,
    val role: UserRole,
    val createdAt: Long = System.currentTimeMillis(),
    val phone: String? = null,
    val profileImageUrl: String? = null,
    val organization: String? = null,
    val isActive: Boolean = true,
    val lastLoginAt: Long? = null,
    val assignedEvents: List<String> = emptyList(),
    val preferences: UserPreferences = UserPreferences()
) {
    fun canManageEvents(): Boolean = role.canManageEvents()
    fun canScanQR(): Boolean = role.canScanQR()
    fun canAccessDashboard(): Boolean = role.canAccessDashboard()
    fun canManageUsers(): Boolean = role.canManageUsers()
    fun canViewReports(): Boolean = role.canViewReports()
    fun isAccountActive(): Boolean = isActive
    
    val displayName: String
        get() = fullName.split(" ").firstOrNull() ?: fullName
    
    fun initials(): String {
        val names = fullName.split(" ")
        return when {
            names.size >= 2 -> "${names[0].firstOrNull()}${names[1].firstOrNull()}"
            names.size == 1 -> names[0].take(2)
            else -> "U"
        }.uppercase()
    }
    
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        if (email.isBlank()) errors.add("Email is required")
        if (!isValidEmail(email)) errors.add("Invalid email format")
        if (fullName.isBlank()) errors.add("Full name is required")
        return if (errors.isEmpty()) ValidationResult.Success else ValidationResult.Error(errors)
    }
    
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}

enum class UserRole {
    ADMIN,
    SCANNER;

    fun canManageEvents(): Boolean = this == ADMIN
    fun canScanQR(): Boolean = true
    fun canAccessDashboard(): Boolean = this == ADMIN
    fun canManageUsers(): Boolean = this == ADMIN
    fun canViewReports(): Boolean = this == ADMIN

    fun displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }

    fun description(): String = when (this) {
        ADMIN -> "Full system access — manage events, users and analytics"
        SCANNER -> "Staff user — scan QR codes and process check-ins"
    }
}

data class UserPreferences(
    val notificationsEnabled: Boolean = true,
    val emailNotifications: Boolean = true,
    val darkMode: Boolean = false,
    val language: String = "en",
    val dateFormat: String = "MM/dd/yyyy",
    val timeFormat: String = "12h"
)

sealed class AuthState {
    object Unauthenticated : AuthState()
    data class Authenticated(val user: User) : AuthState()
    object Loading : AuthState()
    data class Error(val message: String) : AuthState()
    
    fun isAuthenticated(): Boolean = this is Authenticated
    fun isLoading(): Boolean = this is Loading
    fun authenticatedUser(): User? = (this as? Authenticated)?.user
}

sealed class LoginResult {
    data class Success(val user: User) : LoginResult()
    data class InvalidCredentials(val message: String = "Invalid email or password") : LoginResult()
    data class UserNotFound(val message: String = "User not found") : LoginResult()
    data class AccountDisabled(val message: String = "Account is disabled") : LoginResult()
    data class Error(val message: String, val exception: Throwable? = null) : LoginResult()
    
    fun isSuccess(): Boolean = this is Success
}

sealed class RegistrationResult {
    data class Success(val user: User) : RegistrationResult()
    data class EmailAlreadyExists(val message: String = "Email already registered") : RegistrationResult()
    data class WeakPassword(val message: String = "Password is too weak") : RegistrationResult()
    data class InvalidEmail(val message: String = "Invalid email format") : RegistrationResult()
    data class Error(val message: String, val exception: Throwable? = null) : RegistrationResult()
    
    fun isSuccess(): Boolean = this is Success
}
