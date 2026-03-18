package com.example.eventpay.di

import android.content.Context
import com.example.eventpay.data.firebase.FirebaseService
import com.example.eventpay.data.firebase.FirestoreEventRepository
import com.example.eventpay.data.firebase.FirestoreTicketRepository
import com.example.eventpay.data.firebase.FirestoreTransactionRepository
import com.example.eventpay.data.local.AppDatabase
import com.example.eventpay.data.repository.EventRepository
import com.example.eventpay.data.repository.TicketRepository
import com.example.eventpay.data.repository.TransactionRepository
import com.example.eventpay.data.repository.UserRepository
import com.example.eventpay.domain.qrcode.QRCodeGenerator
import com.example.eventpay.security.BiometricAuthManager
import com.example.eventpay.security.QRCryptoManager
import com.example.eventpay.ui.admin.AdminViewModel
import com.example.eventpay.ui.auth.AuthViewModel
import com.example.eventpay.ui.cashier.CashierViewModel
import com.example.eventpay.ui.dashboard.DashboardViewModel
import com.example.eventpay.ui.event.EventViewModel
import com.example.eventpay.ui.qrcode.QRCodeViewModel
import com.example.eventpay.ui.scanner.ScannerViewModel

class AppContainer(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)

    // DAOs
    private val userDao = database.userDao()
    private val eventDao = database.eventDao()
    private val ticketDao = database.ticketDao()

    // Local Repositories
    val userRepository = UserRepository(userDao)
    val eventRepository = EventRepository(eventDao)
    val ticketRepository = TicketRepository(ticketDao)
    
    // Firebase Services
    val firebaseService = FirebaseService()
    val firestoreEventRepository = FirestoreEventRepository()
    val firestoreTicketRepository = FirestoreTicketRepository()
    val firestoreTransactionRepository = FirestoreTransactionRepository()
    
    // Security
    private val qrCryptoManager = QRCryptoManager()
    val qrCodeGenerator = QRCodeGenerator(qrCryptoManager)
    val biometricAuthManager = BiometricAuthManager(context)

    // ViewModels
    val authViewModel = AuthViewModel(userRepository, firebaseService, biometricAuthManager)
    val eventViewModel = EventViewModel(
        eventRepository,
        ticketRepository,
        userRepository,
        firestoreEventRepository,
        firestoreTicketRepository,
        firestoreTransactionRepository
    )
    val qrCodeViewModel = QRCodeViewModel(
        ticketRepository, 
        eventRepository,
        firestoreTicketRepository
    )
val cashierViewModel = CashierViewModel(
        firestoreEventRepository,
        firestoreTicketRepository,
        firestoreTransactionRepository,
        firebaseService,
        qrCodeGenerator
    )
    val dashboardViewModel = DashboardViewModel(
        firestoreEventRepository,
        firestoreTicketRepository,
        firestoreTransactionRepository,
        firebaseService
    )
    val adminViewModel = AdminViewModel(firebaseService, firestoreEventRepository)
    val scannerViewModel = ScannerViewModel(firebaseService, firestoreEventRepository, firestoreTicketRepository)

    companion object {
        @Volatile
        private var INSTANCE: AppContainer? = null

        fun getInstance(context: Context): AppContainer {
            return INSTANCE ?: synchronized(this) {
                val instance = AppContainer(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }

        fun getInstance(): AppContainer {
            return INSTANCE ?: throw IllegalStateException("AppContainer must be initialized first")
        }
    }
}
