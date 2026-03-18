package com.example.eventpay.di

import com.example.eventpay.data.local.AppDatabase
import com.example.eventpay.data.local.dao.EventDao
import com.example.eventpay.data.local.dao.TicketDao
import com.example.eventpay.data.local.dao.UserDao
import com.example.eventpay.data.firebase.FirestoreEventRepository
import com.example.eventpay.data.firebase.FirestoreTicketRepository
import com.example.eventpay.data.firebase.FirestoreTransactionRepository
import com.example.eventpay.data.repository.EventRepositoryImpl
import com.example.eventpay.data.repository.TicketRepository
import com.example.eventpay.data.repository.UserRepository
import com.example.eventpay.util.NetworkUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Simple DI container (no Hilt/Dagger - manual DI)
 * 
 * Provides all dependencies for the application including:
 * - Database and DAOs
 * - Firebase services
 * - Repositories
 */
object DatabaseModule {
    
    fun provideEventDao(database: AppDatabase): EventDao {
        return database.eventDao()
    }
    
    fun provideTicketDao(database: AppDatabase): TicketDao {
        return database.ticketDao()
    }
    
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }
}

object FirebaseModule {
    
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
    
    fun provideFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }
    
    fun provideFirestoreEventRepository(): FirestoreEventRepository {
        return FirestoreEventRepository()
    }
    
    fun provideFirestoreTicketRepository(): FirestoreTicketRepository {
        return FirestoreTicketRepository()
    }
    
    fun provideFirestoreTransactionRepository(): FirestoreTransactionRepository {
        return FirestoreTransactionRepository()
    }
}

object RepositoryModule {
    
    fun provideEventRepository(
        eventDao: EventDao,
        firestoreEventRepository: FirestoreEventRepository,
        networkUtils: NetworkUtils
    ): EventRepositoryImpl {
        return EventRepositoryImpl(eventDao, firestoreEventRepository, networkUtils)
    }
    
    fun provideTicketRepository(
        ticketDao: TicketDao
    ): TicketRepository {
        return TicketRepository(ticketDao)
    }
    
    fun provideUserRepository(
        userDao: UserDao
    ): UserRepository {
        return UserRepository(userDao)
    }
}
