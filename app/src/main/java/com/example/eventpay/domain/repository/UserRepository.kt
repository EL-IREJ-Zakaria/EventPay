package com.example.eventpay.domain.repository

import com.example.eventpay.domain.model.AuthState
import com.example.eventpay.domain.model.LoginResult
import com.example.eventpay.domain.model.RegistrationResult
import com.example.eventpay.domain.model.User
import com.example.eventpay.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain Repository Interface - UserRepository
 * 
 * Defines the contract for user data access and authentication operations.
 * Handles user registration, login, profile management, and wallet operations.
 */
interface UserRepository {
    
    /**
     * Get current authentication state as a Flow
     * @return Flow of AuthState
     */
    fun getAuthState(): Flow<AuthState>
    
    /**
     * Get the currently logged-in user
     * @return The current user or null if not logged in
     */
    suspend fun getCurrentUser(): User?
    
    /**
     * Get a user by ID
     * @param userId The user ID
     * @return The user or null if not found
     */
    suspend fun getUserById(userId: String): User?
    
    /**
     * Get a user by email
     * @param email The user's email
     * @return The user or null if not found
     */
    suspend fun getUserByEmail(email: String): User?
    
    /**
     * Get all users (admin only)
     * @return Flow of all users
     */
    fun getAllUsers(): Flow<List<User>>
    
    /**
     * Get users by role
     * @param role The role to filter by
     * @return Flow of users with the specified role
     */
    fun getUsersByRole(role: UserRole): Flow<List<User>>
    
    /**
     * Login a user with email and password
     * @param email The user's email
     * @param password The user's password
     * @return LoginResult indicating success or failure
     */
    suspend fun login(email: String, password: String): LoginResult
    
    /**
     * Register a new user
     * @param email The user's email
     * @param password The user's password
     * @param fullName The user's full name
     * @param role The user's role
     * @return RegistrationResult indicating success or failure
     */
    suspend fun register(
        email: String,
        password: String,
        fullName: String,
        role: UserRole = UserRole.SCANNER
    ): RegistrationResult
    
    /**
     * Logout the current user
     */
    suspend fun logout()
    
    /**
     * Update user profile
     * @param user The user with updated data
     * @return Result indicating success or failure
     */
    suspend fun updateUser(user: User): Result<Unit>
    
    /**
     * Update user's wallet balance
     * @param userId The user ID
     * @param amount The amount to add (positive) or subtract (negative)
     * @return Result with the new balance or error
     */
    suspend fun updateWalletBalance(userId: String, amount: Double): Result<Double>
    
    /**
     * Get user's wallet balance
     * @param userId The user ID
     * @return The current wallet balance
     */
    suspend fun getWalletBalance(userId: String): Double
    
    /**
     * Check if user has sufficient balance
     * @param userId The user ID
     * @param amount The amount to check
     * @return True if user has sufficient balance
     */
    suspend fun hasSufficientBalance(userId: String, amount: Double): Boolean
    
    /**
     * Update user's last login timestamp
     * @param userId The user ID
     */
    suspend fun updateLastLogin(userId: String)
    
    /**
     * Deactivate a user account
     * @param userId The user ID to deactivate
     * @return Result indicating success or failure
     */
    suspend fun deactivateUser(userId: String): Result<Unit>
    
    /**
     * Activate a user account
     * @param userId The user ID to activate
     * @return Result indicating success or failure
     */
    suspend fun activateUser(userId: String): Result<Unit>
    
    /**
     * Change user's role (admin only)
     * @param userId The user ID
     * @param newRole The new role
     * @return Result indicating success or failure
     */
    suspend fun changeUserRole(userId: String, newRole: UserRole): Result<Unit>
    
    /**
     * Update user preferences
     * @param userId The user ID
     * @param key Preference key
     * @param value Preference value
     * @return Result indicating success or failure
     */
    suspend fun updatePreference(userId: String, key: String, value: String): Result<Unit>
    
    /**
     * Check if email is already registered
     * @param email The email to check
     * @return True if email exists
     */
    suspend fun isEmailRegistered(email: String): Boolean
    
    /**
     * Send password reset email
     * @param email The user's email
     * @return Result indicating success or failure
     */
    suspend fun sendPasswordReset(email: String): Result<Unit>
    
    /**
     * Sync local user data with remote server
     * @return Result indicating sync success or failure
     */
    suspend fun syncWithRemote(): Result<Unit>
}
