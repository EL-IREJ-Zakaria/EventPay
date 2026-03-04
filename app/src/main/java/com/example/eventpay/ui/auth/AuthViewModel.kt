package com.example.eventpay.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventpay.data.firebase.FirebaseService
import com.example.eventpay.data.model.User
import com.example.eventpay.data.model.UserRole
import com.example.eventpay.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val currentUser: User? = null,
    val error: String? = null,
    val isInitialized: Boolean = false
)

class AuthViewModel(
    private val userRepository: UserRepository,
    private val firebaseService: FirebaseService
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            firebaseService.authStateFlow.collect { firebaseUser ->
                if (firebaseUser != null) {
                    fetchUserDetails(firebaseUser.uid)
                } else {
                    _authState.value = _authState.value.copy(
                        isLoggedIn = false,
                        currentUser = null,
                        isInitialized = true
                    )
                }
            }
        }
    }

    private fun fetchUserDetails(userId: String) {
        viewModelScope.launch {
            firebaseService.getUser(userId).fold(
                onSuccess = { user ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        currentUser = user,
                        isInitialized = true
                    )
                    userRepository.insertUser(user)
                },
                onFailure = { error ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to fetch user details",
                        isInitialized = true
                    )
                }
            )
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            firebaseService.login(email, password).fold(
                onSuccess = { },
                onFailure = { error ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Login failed"
                    )
                }
            )
        }
    }

    fun register(email: String, password: String, fullName: String, role: UserRole = UserRole.SCANNER) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            firebaseService.register(email, password, fullName, role).fold(
                onSuccess = { },
                onFailure = { error ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Registration failed"
                    )
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            firebaseService.logout()
            _authState.value = AuthState(isInitialized = true)
        }
    }

    fun resetPassword(email: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            firebaseService.resetPassword(email).fold(
                onSuccess = {
                    _authState.value = _authState.value.copy(isLoading = false)
                    onResult(true, "Password reset email sent")
                },
                onFailure = { error ->
                    _authState.value = _authState.value.copy(isLoading = false)
                    onResult(false, error.message ?: "Failed to send reset email")
                }
            )
        }
    }

    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }

    fun getCurrentUser(): User? = _authState.value.currentUser
    fun getCurrentUserId(): String? = firebaseService.getCurrentUserId()
    fun isUserLoggedIn(): Boolean = firebaseService.isUserLoggedIn()

    fun isAdmin(): Boolean = _authState.value.currentUser?.role == UserRole.ADMIN
    fun isScanner(): Boolean = _authState.value.currentUser?.role == UserRole.SCANNER
    fun hasRole(role: UserRole): Boolean = _authState.value.currentUser?.role == role

    fun canManageEvents(): Boolean = isAdmin()
    fun canScanQR(): Boolean = true
    fun canViewReports(): Boolean = isAdmin()
    fun canManageUsers(): Boolean = isAdmin()
}
