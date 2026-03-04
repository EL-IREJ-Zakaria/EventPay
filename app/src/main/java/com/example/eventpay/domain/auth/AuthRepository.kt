package com.example.eventpay.domain.auth

import kotlinx.coroutines.flow.Flow

/**
 * Authentication Repository Interface
 * 
 * Defines the contract for authentication operations.
 * Implemented by the Data layer using Firebase Auth.
 */
interface AuthRepository {
    
    /**
     * Get current authentication state as a Flow
     */
    fun getAuthState(): Flow<AuthState>
    
    /**
     * Get the currently authenticated user
     */
    suspend fun getCurrentUser(): AuthenticatedUser?
    
    /**
     * Login with email and password
     */
    suspend fun login(request: LoginRequest): LoginResult
    
    /**
     * Register a new user
     */
    suspend fun register(request: RegistrationRequest): RegistrationResult
    
    /**
     * Logout the current user
     */
    suspend fun logout()
    
    /**
     * Send password reset email
     */
    suspend fun sendPasswordReset(email: String): Result<Unit>
    
    /**
     * Change password for current user
     */
    suspend fun changePassword(request: PasswordChangeRequest): Result<Unit>
    
    /**
     * Check if user is authenticated
     */
    suspend fun isAuthenticated(): Boolean
    
    /**
     * Get current user's ID token (for API calls)
     */
    suspend fun getIdToken(): String?
    
    /**
     * Refresh the current session
     */
    suspend fun refreshSession(): Result<Unit>
    
    /**
     * Update last activity timestamp
     */
    suspend fun updateActivity()
    
    /**
     * Check if session is valid
     */
    suspend fun isSessionValid(): Boolean
}
