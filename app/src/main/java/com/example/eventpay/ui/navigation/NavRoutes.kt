package com.example.eventpay.ui.navigation

/**
 * Navigation Routes
 * 
 * Defines all navigation destinations in the app.
 * Uses sealed class for type-safe navigation.
 */
sealed class NavRoute(val route: String) {
    
    // Auth routes
    object Login : NavRoute("login")
    object Register : NavRoute("register")
    
    // Main routes
    object Home : NavRoute("home")
    object Dashboard : NavRoute("dashboard")
    object Profile : NavRoute("profile")
    
    // Event routes
    object EventList : NavRoute("events")
    object EventDetail : NavRoute("events/{eventId}") {
        fun createRoute(eventId: String) = "events/$eventId"
    }
    object CreateEvent : NavRoute("events/create")
    object EditEvent : NavRoute("events/{eventId}/edit") {
        fun createRoute(eventId: String) = "events/$eventId/edit"
    }
    
    // Ticket routes
    object TicketList : NavRoute("tickets")
    object TicketDetail : NavRoute("tickets/{ticketId}") {
        fun createRoute(ticketId: String) = "tickets/$ticketId"
    }
    
    // QR Code routes
    object QRScanner : NavRoute("qr-scanner/{eventId}") {
        fun createRoute(eventId: String) = "qr-scanner/$eventId"
    }
    object QRResult : NavRoute("qr-result/{success}/{message}") {
        fun createRoute(success: Boolean, message: String) = 
            "qr-result/$success/${java.net.URLEncoder.encode(message, "UTF-8")}"
    }
    
    // Cashier routes
    object Cashier : NavRoute("cashier")
    object CashierShift : NavRoute("cashier/shift")
    
    // Wallet routes
    object Wallet : NavRoute("wallet")
    object TopUp : NavRoute("wallet/top-up")
    
    // Transaction routes
    object TransactionHistory : NavRoute("transactions")
    object TransactionDetail : NavRoute("transactions/{transactionId}") {
        fun createRoute(transactionId: String) = "transactions/$transactionId"
    }
}

/**
 * Navigation arguments for type-safe argument passing
 */
object NavArgs {
    const val EventId = "eventId"
    const val TicketId = "ticketId"
    const val TransactionId = "transactionId"
    const val Success = "success"
    const val Message = "message"
}

/**
 * Navigation groups for organizing screens
 */
sealed class NavGroup(val routes: List<NavRoute>) {
    object Auth : NavGroup(listOf(
        NavRoute.Login,
        NavRoute.Register
    ))
    
    object Main : NavGroup(listOf(
        NavRoute.Home,
        NavRoute.Dashboard,
        NavRoute.Profile
    ))
    
    object Events : NavGroup(listOf(
        NavRoute.EventList,
        NavRoute.EventDetail,
        NavRoute.CreateEvent,
        NavRoute.EditEvent
    ))
    
    object Tickets : NavGroup(listOf(
        NavRoute.TicketList,
        NavRoute.TicketDetail
    ))
    
    object QR : NavGroup(listOf(
        NavRoute.QRScanner,
        NavRoute.QRResult
    ))
    
    object Cashier : NavGroup(listOf(
        NavRoute.Cashier,
        NavRoute.CashierShift
    ))
    
    object Wallet : NavGroup(listOf(
        NavRoute.Wallet,
        NavRoute.TopUp
    ))
}
