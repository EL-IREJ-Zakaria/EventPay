package com.example.eventpay.data.auth

import com.example.eventpay.domain.auth.*
import com.example.eventpay.data.local.dao.UserDao
import com.example.eventpay.data.model.User as DataUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Authentication Repository Implementation
 * 
 * Implements authentication using Firebase Auth with Firestore for user data.
 * Provides secure session handling with automatic token refresh.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userDao: UserDao
) : AuthRepository {
    
    companion object {
        private const val USERS_COLLECTION = "users"
        private const val SESSION_TIMEOUT = 24 * 60 * 60 * 1000L // 24 hours
        private const val ACTIVITY_UPDATE_INTERVAL = 5 * 60 * 1000L // 5 minutes
    }
    
    private var lastActivityUpdate: Long = 0
    
    override fun getAuthState(): Flow<AuthState> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            
            if (firebaseUser == null) {
                trySend(AuthState.Unauthenticated)
            } else {
                trySend(AuthState.Loading)
                
                // Fetch user data from Firestore using a proper coroutine scope
                CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                    try {
                        val user = fetchUserData(firebaseUser.uid)
                        if (user != null) {
                            trySend(AuthState.Authenticated(user))
                        } else {
                            trySend(AuthState.Error("User data not found"))
                        }
                    } catch (e: Exception) {
                        trySend(AuthState.Error(e.message ?: "Authentication error"))
                    }
                }
            }
        }
        
        firebaseAuth.addAuthStateListener(authStateListener)
        
        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }
    
    override suspend fun getCurrentUser(): AuthenticatedUser? {
        val firebaseUser = firebaseAuth.currentUser ?: return null
        return fetchUserData(firebaseUser.uid)
    }
    
    override suspend fun login(request: LoginRequest): LoginResult {
        // Validate request
        val validation = request.validate()
        if (validation is ValidationResult.Error) {
            return LoginResult.InvalidCredentials(validation.messages.first())
        }
        
        return try {
            // Sign in with Firebase
            val result = firebaseAuth.signInWithEmailAndPassword(
                request.email.trim(),
                request.password
            ).await()
            
            val firebaseUser = result.user ?: return LoginResult.UserNotFound()
            
            // Check if email is verified (optional, can be disabled)
            // if (!firebaseUser.isEmailVerified) {
            //     return LoginResult.Error("Please verify your email first")
            // }
            
            // Fetch user data
            val user = fetchUserData(firebaseUser.uid)
                ?: return LoginResult.UserNotFound()
            
            // Check if account is active
            if (!user.isActive()) {
                firebaseAuth.signOut()
                return LoginResult.AccountDisabled()
            }
            
            // Update last login
            updateLastLogin(firebaseUser.uid)
            
            // Record session
            recordSession(firebaseUser.uid, request.deviceId)
            
            LoginResult.Success(user)
            
        } catch (e: FirebaseAuthException) {
            when (e.errorCode) {
                "ERROR_INVALID_EMAIL" -> LoginResult.InvalidCredentials("Invalid email format")
                "ERROR_WRONG_PASSWORD" -> LoginResult.InvalidCredentials()
                "ERROR_USER_NOT_FOUND" -> LoginResult.UserNotFound()
                "ERROR_USER_DISABLED" -> LoginResult.AccountDisabled()
                "ERROR_TOO_MANY_REQUESTS" -> LoginResult.TooManyAttempts()
                else -> LoginResult.Error(e.message ?: "Login failed")
            }
        } catch (e: Exception) {
            LoginResult.Error(e.message ?: "Login failed", e)
        }
    }
    
    override suspend fun register(request: RegistrationRequest): RegistrationResult {
        // Validate request
        val validation = request.validate()
        if (validation is ValidationResult.Error) {
            return RegistrationResult.Error(validation.messages.first())
        }
        
        return try {
            // Check if email already exists
            val existingUser = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("email", request.email.trim())
                .get()
                .await()
            
            if (!existingUser.isEmpty) {
                return RegistrationResult.EmailAlreadyExists()
            }
            
            // Create Firebase Auth user
            val result = firebaseAuth.createUserWithEmailAndPassword(
                request.email.trim(),
                request.password
            ).await()
            
            val firebaseUser = result.user ?: return RegistrationResult.Error("Failed to create user")
            
            // Create user document in Firestore
            val userData = mapOf(
                "id" to firebaseUser.uid,
                "email" to request.email.trim(),
                "fullName" to request.fullName.trim(),
                "role" to request.role.name,
                "isActive" to true,
                "createdAt" to System.currentTimeMillis(),
                "lastLoginAt" to System.currentTimeMillis(),
                "organization" to request.organization,
                "profileImageUrl" to null,
                "walletBalance" to 0.0
            )
            
            firestore.collection(USERS_COLLECTION)
                .document(firebaseUser.uid)
                .set(userData)
                .await()
            
            // Save to local database
            val localUser = DataUser(
                id = firebaseUser.uid,
                email = request.email.trim(),
                fullName = request.fullName.trim(),
                password = "", // Don't store password locally
                role = com.example.eventpay.data.model.UserRole.valueOf(request.role.name),
                createdAt = System.currentTimeMillis()
            )
            userDao.insertUser(localUser)
            
            // Create authenticated user
            val user = AuthenticatedUser(
                id = firebaseUser.uid,
                email = request.email.trim(),
                fullName = request.fullName.trim(),
                role = request.role,
                organization = request.organization,
                session = createSession(firebaseUser.uid)
            )
            
            RegistrationResult.Success(user)
            
        } catch (e: FirebaseAuthException) {
            when (e.errorCode) {
                "ERROR_EMAIL_ALREADY_IN_USE" -> RegistrationResult.EmailAlreadyExists()
                "ERROR_WEAK_PASSWORD" -> RegistrationResult.WeakPassword()
                else -> RegistrationResult.Error(e.message ?: "Registration failed")
            }
        } catch (e: Exception) {
            RegistrationResult.Error(e.message ?: "Registration failed", e)
        }
    }
    
    override suspend fun logout() {
        try {
            val userId = firebaseAuth.currentUser?.uid
            if (userId != null) {
                // Clear session data
                clearSession(userId)
            }
            
            // Sign out from Firebase
            firebaseAuth.signOut()
            
        } catch (e: Exception) {
            // Still sign out even if session clearing fails
            firebaseAuth.signOut()
        }
    }
    
    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: FirebaseAuthException) {
            Result.failure(Exception(e.message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun changePassword(request: PasswordChangeRequest): Result<Unit> {
        val validation = request.validate()
        if (validation is ValidationResult.Error) {
            return Result.failure(Exception(validation.messages.first()))
        }
        
        val user = firebaseAuth.currentUser ?: return Result.failure(Exception("Not authenticated"))
        
        return try {
            // Re-authenticate with current password
            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(
                user.email ?: "",
                request.currentPassword
            )
            user.reauthenticate(credential).await()
            
            // Update password
            user.updatePassword(request.newPassword).await()
            
            Result.success(Unit)
        } catch (e: FirebaseAuthException) {
            when (e.errorCode) {
                "ERROR_WRONG_PASSWORD" -> Result.failure(Exception("Current password is incorrect"))
                "ERROR_WEAK_PASSWORD" -> Result.failure(Exception("New password is too weak"))
                else -> Result.failure(Exception(e.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun isAuthenticated(): Boolean {
        return firebaseAuth.currentUser != null
    }
    
    override suspend fun getIdToken(): String? {
        return firebaseAuth.currentUser?.getIdToken(false)?.await()?.token
    }
    
    override suspend fun refreshSession(): Result<Unit> {
        return try {
            firebaseAuth.currentUser?.getIdToken(true)?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateActivity() {
        val now = System.currentTimeMillis()
        if (now - lastActivityUpdate > ACTIVITY_UPDATE_INTERVAL) {
            lastActivityUpdate = now
            firebaseAuth.currentUser?.uid?.let { userId ->
                firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .update("lastActivityAt", now)
            }
        }
    }
    
    override suspend fun isSessionValid(): Boolean {
        val user = getCurrentUser() ?: return false
        return user.isSessionValid()
    }
    
    // Private helper methods
    
    private suspend fun fetchUserData(userId: String): AuthenticatedUser? {
        return try {
            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            if (!document.exists()) return null
            
            val role = document.getString("role")?.let { 
                runCatching { UserRole.valueOf(it) }.getOrDefault(UserRole.SCANNER)
            } ?: UserRole.SCANNER
            val isActive = document.getBoolean("isActive") ?: true
            
            if (!isActive) return null
            
            AuthenticatedUser(
                id = userId,
                email = document.getString("email") ?: "",
                fullName = document.getString("fullName") ?: "",
                role = role,
                profileImageUrl = document.getString("profileImageUrl"),
                organization = document.getString("organization"),
                permissions = role.getDefaultPermissions(),
                session = createSession(userId)
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun createSession(userId: String): SessionInfo {
        val now = System.currentTimeMillis()
        return SessionInfo(
            sessionId = UUID.randomUUID().toString(),
            createdAt = now,
            expiresAt = now + SESSION_TIMEOUT,
            lastActivityAt = now
        )
    }
    
    private suspend fun updateLastLogin(userId: String) {
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .update("lastLoginAt", System.currentTimeMillis())
            .await()
    }
    
    private suspend fun recordSession(userId: String, deviceId: String?) {
        // Could record session in a separate collection for multi-device tracking
    }
    
    private suspend fun clearSession(userId: String) {
        // Clear session data from Firestore if tracking sessions
    }
}
