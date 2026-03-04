package com.example.eventpay.domain.usecase.user

import com.example.eventpay.domain.model.AuthState
import com.example.eventpay.domain.model.LoginResult
import com.example.eventpay.domain.model.RegistrationResult
import com.example.eventpay.domain.model.User
import com.example.eventpay.domain.model.UserRole
import com.example.eventpay.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use Case: Login
 * 
 * Authenticates a user with email and password.
 */
class LoginUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(email: String, password: String): LoginResult {
        if (email.isBlank()) {
            return LoginResult.InvalidCredentials("Email is required")
        }
        if (password.isBlank()) {
            return LoginResult.InvalidCredentials("Password is required")
        }
        
        val result = userRepository.login(email.trim(), password)
        
        // Update last login time on successful login
        if (result is LoginResult.Success) {
            userRepository.updateLastLogin(result.user.id)
        }
        
        return result
    }
}

/**
 * Use Case: Register
 * 
 * Registers a new user account.
 */
class RegisterUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        fullName: String,
        role: UserRole = UserRole.SCANNER
    ): RegistrationResult {
        // Validate inputs
        if (email.isBlank()) {
            return RegistrationResult.InvalidEmail("Email is required")
        }
        if (password.length < 6) {
            return RegistrationResult.WeakPassword("Password must be at least 6 characters")
        }
        if (fullName.isBlank()) {
            return RegistrationResult.Error("Full name is required")
        }
        
        // Check if email already exists
        if (userRepository.isEmailRegistered(email.trim())) {
            return RegistrationResult.EmailAlreadyExists()
        }
        
        return userRepository.register(email.trim(), password, fullName.trim(), role)
    }
}

/**
 * Use Case: Logout
 * 
 * Logs out the current user.
 */
class LogoutUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke() {
        userRepository.logout()
    }
}

/**
 * Use Case: Get Current User
 * 
 * Gets the currently authenticated user.
 */
class GetCurrentUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): User? {
        return userRepository.getCurrentUser()
    }
}

/**
 * Use Case: Get Auth State
 * 
 * Observes authentication state changes.
 */
class GetAuthStateUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(): Flow<AuthState> {
        return userRepository.getAuthState()
    }
}

/**
 * Use Case: Update User Profile
 * 
 * Updates user profile information.
 */
class UpdateUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(user: User): Result<Unit> {
        // Validate user data
        val validation = user.validate()
        if (!validation.isSuccess()) {
            return Result.failure(Exception((validation as com.example.eventpay.domain.model.ValidationResult.Error).messages.first()))
        }
        
        return userRepository.updateUser(user)
    }
}

/**
 * Use Case: Get User by ID
 * 
 * Retrieves a user by their ID.
 */
class GetUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: String): User? {
        return userRepository.getUserById(userId)
    }
}

/**
 * Use Case: Get All Users
 * 
 * Retrieves all users (admin only).
 */
class GetAllUsersUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(): Flow<List<User>> {
        return userRepository.getAllUsers()
    }
}

/**
 * Use Case: Change User Role
 * 
 * Changes a user's role (admin only).
 */
class ChangeUserRoleUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: String, newRole: UserRole): Result<Unit> {
        val currentUser = userRepository.getCurrentUser()
            ?: return Result.failure(Exception("Not authenticated"))
        
        if (!currentUser.canManageUsers()) {
            return Result.failure(Exception("Permission denied"))
        }
        
        return userRepository.changeUserRole(userId, newRole)
    }
}

/**
 * Use Case: Deactivate User
 * 
 * Deactivates a user account.
 */
class DeactivateUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: String): Result<Unit> {
        val currentUser = userRepository.getCurrentUser()
            ?: return Result.failure(Exception("Not authenticated"))
        
        if (!currentUser.canManageUsers()) {
            return Result.failure(Exception("Permission denied"))
        }
        
        return userRepository.deactivateUser(userId)
    }
}

/**
 * Use Case: Send Password Reset
 * 
 * Sends a password reset email.
 */
class SendPasswordResetUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        if (email.isBlank()) {
            return Result.failure(Exception("Email is required"))
        }
        
        return userRepository.sendPasswordReset(email.trim())
    }
}
