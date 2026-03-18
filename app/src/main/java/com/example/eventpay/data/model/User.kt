package com.example.eventpay.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String = "",
    val email: String = "",
    val fullName: String = "",
    val password: String = "",
    val role: UserRole = UserRole.SCANNER,
    val createdAt: Long = 0L,
    val phone: String? = null,
    val profileImageUrl: String? = null,
    val organization: String? = null,
    val isActive: Boolean = true,
    val lastLoginAt: Long? = null,
    val assignedEvents: List<String> = emptyList(),
    val preferences: UserPreferences = UserPreferences()
)

data class UserPreferences(
    val notificationsEnabled: Boolean = true,
    val emailNotifications: Boolean = true,
    val darkMode: Boolean = false,
    val language: String = "en",
    val dateFormat: String = "MM/dd/yyyy",
    val timeFormat: String = "12h"
)

enum class UserRole {
    ADMIN,
    SCANNER;

    fun canManageEvents(): Boolean = this == ADMIN
    fun canScanQR(): Boolean = true
    fun canAccessDashboard(): Boolean = this == ADMIN
    fun canManageUsers(): Boolean = this == ADMIN
    fun canViewReports(): Boolean = this == ADMIN
}
