package com.example.eventpay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.runtime.*
import com.example.eventpay.data.model.UserRole
import com.example.eventpay.di.AppContainer
import com.example.eventpay.ui.auth.AuthViewModel
import com.example.eventpay.ui.screens.*
import com.example.eventpay.ui.screens.admin.AdminEventListScreen
import com.example.eventpay.ui.screens.admin.AdminHomeScreen
import com.example.eventpay.ui.screens.admin.AdminUserManagementScreen
import com.example.eventpay.ui.screens.admin.ParticipantsScreen
import com.example.eventpay.ui.screens.scanner.ScannerHomeScreen
import com.example.eventpay.ui.theme.EventPayTheme

class MainActivity : ComponentActivity() {
    lateinit var container: AppContainer
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = AppContainer.getInstance(applicationContext)

        setContent {
            EventPayTheme {
                EventPayApp(container = container)
            }
        }
    }
}

sealed class Screen {
    object Splash : Screen()
    object Login : Screen()
    object Register : Screen()

    object AdminHome : Screen()
    object AdminEvents : Screen()
    object AdminUsers : Screen()
    data class AdminParticipants(val eventId: String) : Screen()

    object ScannerHome : Screen()
}

@Composable
fun EventPayApp(container: AppContainer) {
    val authViewModel = container.authViewModel
    val authState by authViewModel.authState.collectAsState()

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }

    LaunchedEffect(authState.isLoggedIn, authState.isInitialized) {
        if (!authState.isInitialized) return@LaunchedEffect
        if (authState.isLoggedIn) {
            val role = authState.currentUser?.role
            currentScreen = if (role == UserRole.ADMIN) Screen.AdminHome else Screen.ScannerHome
        } else {
            if (currentScreen !is Screen.Login && currentScreen !is Screen.Register) {
                currentScreen = Screen.Login
            }
        }
    }

    when (currentScreen) {
        is Screen.Splash -> {
            SplashScreen(
                onSplashComplete = {
                    if (!authState.isInitialized || !authState.isLoggedIn) {
                        currentScreen = Screen.Login
                    }
                }
            )
        }

        is Screen.Login -> {
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToRegister = { currentScreen = Screen.Register },
                onLoginSuccess = {
                    val role = authState.currentUser?.role
                    currentScreen = if (role == UserRole.ADMIN) Screen.AdminHome else Screen.ScannerHome
                }
            )
        }

        is Screen.Register -> {
            RegisterScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = { currentScreen = Screen.Login },
                onRegisterSuccess = {
                    val role = authState.currentUser?.role
                    currentScreen = if (role == UserRole.ADMIN) Screen.AdminHome else Screen.ScannerHome
                }
            )
        }

        is Screen.AdminHome -> {
            authState.currentUser?.let { user ->
                AdminHomeScreen(
                    currentUser = user,
                    adminViewModel = container.adminViewModel,
                    onNavigateToEvents = { currentScreen = Screen.AdminEvents },
                    onNavigateToUsers = { currentScreen = Screen.AdminUsers },
                    onNavigateToScanner = { currentScreen = Screen.ScannerHome },
                    onLogout = {
                        authViewModel.logout()
                        currentScreen = Screen.Login
                    }
                )
            }
        }

        is Screen.AdminEvents -> {
            authState.currentUser?.let { user ->
                AdminEventListScreen(
                    adminViewModel = container.adminViewModel,
                    currentUserId = user.id,
                    onBack = { currentScreen = Screen.AdminHome },
                    onViewParticipants = { eventId ->
                        currentScreen = Screen.AdminParticipants(eventId)
                    }
                )
            }
        }

        is Screen.AdminParticipants -> {
            val screen = currentScreen as Screen.AdminParticipants
            val uiState by container.adminViewModel.uiState.collectAsState()
            val event = uiState.events.find { it.id == screen.eventId }
            if (event != null) {
                LaunchedEffect(screen.eventId) {
                    container.adminViewModel.loadParticipants(screen.eventId)
                }
                ParticipantsScreen(
                    event = event,
                    participants = uiState.participants,
                    isLoading = uiState.participantsLoading,
                    onBack = { currentScreen = Screen.AdminEvents },
                    onAddParticipant = { name, email, phone, ticketType ->
                        authState.currentUser?.let { user ->
                            container.adminViewModel.addManualParticipant(
                                eventId = screen.eventId,
                                name = name,
                                email = email,
                                phone = phone,
                                ticketType = com.example.eventpay.data.model.TicketType.valueOf(ticketType.name),
                                adminId = user.id
                            )
                        }
                    },
                    onRefresh = { container.adminViewModel.loadParticipants(screen.eventId) }
                )
            }
        }

        is Screen.AdminUsers -> {
            authState.currentUser?.let { user ->
                AdminUserManagementScreen(
                    adminViewModel = container.adminViewModel,
                    currentAdminId = user.id,
                    onBack = { currentScreen = Screen.AdminHome }
                )
            }
        }

        is Screen.ScannerHome -> {
            authState.currentUser?.let { user ->
                ScannerHomeScreen(
                    currentUser = user,
                    scannerViewModel = container.scannerViewModel,
                    onLogout = {
                        authViewModel.logout()
                        currentScreen = Screen.Login
                    }
                )
            }
        }
    }
}
