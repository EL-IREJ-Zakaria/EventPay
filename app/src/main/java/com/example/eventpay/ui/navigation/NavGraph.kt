package com.example.eventpay.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.eventpay.ui.screens.*
import com.example.eventpay.ui.auth.AuthViewModel
import com.example.eventpay.ui.event.EventViewModel
import com.example.eventpay.ui.scanner.QRScannerScreen

/**
 * Main Navigation Graph
 * 
 * Defines the complete navigation structure for the app.
 * Uses Jetpack Navigation Compose for type-safe navigation.
 */
@Composable
fun EventPayNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    eventViewModel: EventViewModel,
    currentUser: com.example.eventpay.data.model.User,
    startDestination: String = NavRoute.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth Routes
        composable(route = NavRoute.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(NavRoute.Home.route) {
                        popUpTo(NavRoute.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(NavRoute.Register.route)
                }
            )
        }
        
        composable(route = NavRoute.Register.route) {
            RegisterScreen(
                authViewModel = authViewModel,
                onRegisterSuccess = {
                    navController.navigate(NavRoute.Home.route) {
                        popUpTo(NavRoute.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
        
        // Main Routes
        composable(route = NavRoute.Home.route) {
            val dataUser = currentUser
            val user = com.example.eventpay.domain.model.User(
                id = dataUser.id,
                email = dataUser.email,
                fullName = dataUser.fullName,
                role = com.example.eventpay.domain.model.UserRole.valueOf(dataUser.role.name),
                walletBalance = dataUser.walletBalance,
                createdAt = dataUser.createdAt,
                phone = dataUser.phone,
                profileImageUrl = dataUser.profileImageUrl,
                organization = dataUser.organization,
                isActive = dataUser.isActive,
                lastLoginAt = dataUser.lastLoginAt,
                preferences = com.example.eventpay.domain.model.UserPreferences(
                    notificationsEnabled = dataUser.preferences.notificationsEnabled,
                    emailNotifications = dataUser.preferences.emailNotifications,
                    darkMode = dataUser.preferences.darkMode,
                    language = dataUser.preferences.language
                )
            )
            HomeScreen(
                currentUser = user,
                eventViewModel = eventViewModel,
                authViewModel = authViewModel,
                onEventClick = { eventId ->
                    navController.navigate(NavRoute.EventDetail.createRoute(eventId))
                },
                onCreateEvent = {
                    navController.navigate(NavRoute.CreateEvent.route)
                },
                onScanQR = {
                    // Navigate to QR Scanner - use eventId from current event
                    navController.navigate(NavRoute.QRScanner.createRoute(""))
                },
                onWallet = {
                    navController.navigate(NavRoute.Wallet.route)
                },
                onCashier = {
                    navController.navigate(NavRoute.Cashier.route)
                },
                onDashboard = {
                    navController.navigate(NavRoute.Dashboard.route)
                },
                onLogout = {
                    navController.navigate(NavRoute.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(route = NavRoute.Dashboard.route) {
            DashboardScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Event Routes
        composable(route = NavRoute.CreateEvent.route) {
            val dataUser = currentUser
            val user = com.example.eventpay.domain.model.User(
                id = dataUser.id,
                email = dataUser.email,
                fullName = dataUser.fullName,
                role = com.example.eventpay.domain.model.UserRole.valueOf(dataUser.role.name),
                walletBalance = dataUser.walletBalance,
                createdAt = dataUser.createdAt,
                phone = dataUser.phone,
                profileImageUrl = dataUser.profileImageUrl,
                organization = dataUser.organization,
                isActive = dataUser.isActive,
                lastLoginAt = dataUser.lastLoginAt,
                preferences = com.example.eventpay.domain.model.UserPreferences(
                    notificationsEnabled = dataUser.preferences.notificationsEnabled,
                    emailNotifications = dataUser.preferences.emailNotifications,
                    darkMode = dataUser.preferences.darkMode,
                    language = dataUser.preferences.language
                )
            )
            CreateEventScreen(
                currentUser = user,
                eventViewModel = eventViewModel,
                onBack = {
                    navController.popBackStack()
                },
                onEventCreated = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = NavRoute.EventDetail.route,
            arguments = listOf(
                navArgument("eventId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            val eventState by eventViewModel.eventState.collectAsState()
            val event = eventState.events.find { it.id == eventId } ?: return@composable
            val dataUser = currentUser
            val user = com.example.eventpay.domain.model.User(
                id = dataUser.id,
                email = dataUser.email,
                fullName = dataUser.fullName,
                role = com.example.eventpay.domain.model.UserRole.valueOf(dataUser.role.name),
                walletBalance = dataUser.walletBalance,
                createdAt = dataUser.createdAt,
                phone = dataUser.phone,
                profileImageUrl = dataUser.profileImageUrl,
                organization = dataUser.organization,
                isActive = dataUser.isActive,
                lastLoginAt = dataUser.lastLoginAt,
                preferences = com.example.eventpay.domain.model.UserPreferences(
                    notificationsEnabled = dataUser.preferences.notificationsEnabled,
                    emailNotifications = dataUser.preferences.emailNotifications,
                    darkMode = dataUser.preferences.darkMode,
                    language = dataUser.preferences.language
                )
            )
            EventDetailScreen(
                event = event,
                currentUser = user,
                checkedInCount = 0, // TODO: Get from ViewModel
                eventViewModel = eventViewModel,
                onBack = {
                    navController.popBackStack()
                },
                onPurchaseTicket = {
                    // TODO: Navigate to purchase screen
                },
                onViewTickets = {
                    // TODO: Navigate to tickets screen
                }
            )
        }
        
        composable(
            route = NavRoute.EditEvent.route,
            arguments = listOf(
                navArgument("eventId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            val dataUser = currentUser
            val user = com.example.eventpay.domain.model.User(
                id = dataUser.id,
                email = dataUser.email,
                fullName = dataUser.fullName,
                role = com.example.eventpay.domain.model.UserRole.valueOf(dataUser.role.name),
                walletBalance = dataUser.walletBalance,
                createdAt = dataUser.createdAt,
                phone = dataUser.phone,
                profileImageUrl = dataUser.profileImageUrl,
                organization = dataUser.organization,
                isActive = dataUser.isActive,
                lastLoginAt = dataUser.lastLoginAt,
                preferences = com.example.eventpay.domain.model.UserPreferences(
                    notificationsEnabled = dataUser.preferences.notificationsEnabled,
                    emailNotifications = dataUser.preferences.emailNotifications,
                    darkMode = dataUser.preferences.darkMode,
                    language = dataUser.preferences.language
                )
            )
            // Using CreateEventScreen as edit - it should handle both
            CreateEventScreen(
                currentUser = user,
                eventViewModel = eventViewModel,
                onBack = {
                    navController.popBackStack()
                },
                onEventCreated = {
                    navController.popBackStack()
                }
            )
        }
        
        // QR Scanner Route
        composable(
            route = NavRoute.QRScanner.route,
            arguments = listOf(
                navArgument("eventId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            val context = androidx.compose.ui.platform.LocalContext.current
            QRScannerScreen(
                qrCodeViewModel = com.example.eventpay.ui.qrcode.QRCodeViewModel(
                    com.example.eventpay.di.AppContainer.getInstance(context).ticketRepository,
                    com.example.eventpay.di.AppContainer.getInstance(context).eventRepository,
                    com.example.eventpay.di.AppContainer.getInstance(context).firestoreTicketRepository
                ),
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // QR Result Route
        composable(
            route = NavRoute.QRResult.route,
            arguments = listOf(
                navArgument("success") { type = NavType.BoolType },
                navArgument("message") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val success = backStackEntry.arguments?.getBoolean("success") ?: false
            val message = backStackEntry.arguments?.getString("message") ?: ""
            val context = androidx.compose.ui.platform.LocalContext.current
            // Show result in QRScannerScreen or create simple result screen
            QRScannerScreen(
                qrCodeViewModel = com.example.eventpay.ui.qrcode.QRCodeViewModel(
                    com.example.eventpay.di.AppContainer.getInstance(context).ticketRepository,
                    com.example.eventpay.di.AppContainer.getInstance(context).eventRepository,
                    com.example.eventpay.di.AppContainer.getInstance(context).firestoreTicketRepository
                ),
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Cashier Route
        composable(route = NavRoute.Cashier.route) {
            CashierScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Wallet Route
        composable(route = NavRoute.Wallet.route) {
            WalletScreen(
                userId = currentUser.id,
                walletViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Top Up Route
        composable(route = NavRoute.TopUp.route) {
            WalletScreen(
                userId = currentUser.id,
                walletViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Transaction History Route
        composable(route = NavRoute.TransactionHistory.route) {
            WalletScreen(
                userId = currentUser.id,
                walletViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
