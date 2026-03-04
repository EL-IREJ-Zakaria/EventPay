package com.example.eventpay.domain.repository

import com.example.eventpay.domain.model.CashierShiftSummary
import com.example.eventpay.domain.model.PaymentMethod
import com.example.eventpay.domain.model.Transaction
import com.example.eventpay.domain.model.TransactionStatus
import com.example.eventpay.domain.model.TransactionSummary
import com.example.eventpay.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

/**
 * Domain Repository Interface - TransactionRepository
 * 
 * Defines the contract for transaction data access operations.
 * Handles financial transactions, reporting, and offline sync.
 */
interface TransactionRepository {
    
    /**
     * Get all transactions
     * @return Flow of all transactions
     */
    fun getAllTransactions(): Flow<List<Transaction>>
    
    /**
     * Get transactions for a specific user
     * @param userId The user ID
     * @return Flow of user's transactions
     */
    fun getTransactionsByUser(userId: String): Flow<List<Transaction>>
    
    /**
     * Get transactions for a specific event
     * @param eventId The event ID
     * @return Flow of event's transactions
     */
    fun getTransactionsByEvent(eventId: String): Flow<List<Transaction>>
    
    /**
     * Get a single transaction by ID
     * @param transactionId The transaction ID
     * @return The transaction or null if not found
     */
    suspend fun getTransactionById(transactionId: String): Transaction?
    
    /**
     * Get transaction by receipt number
     * @param receiptNumber The receipt number
     * @return The transaction or null if not found
     */
    suspend fun getTransactionByReceipt(receiptNumber: String): Transaction?
    
    /**
     * Get transactions by status
     * @param status The transaction status
     * @return Flow of filtered transactions
     */
    fun getTransactionsByStatus(status: TransactionStatus): Flow<List<Transaction>>
    
    /**
     * Get transactions by type
     * @param type The transaction type
     * @return Flow of filtered transactions
     */
    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>>
    
    /**
     * Get transactions by payment method
     * @param paymentMethod The payment method
     * @return Flow of filtered transactions
     */
    fun getTransactionsByPaymentMethod(paymentMethod: PaymentMethod): Flow<List<Transaction>>
    
    /**
     * Get transactions within a date range
     * @param startDate Start timestamp
     * @param endDate End timestamp
     * @return Flow of transactions in the range
     */
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>
    
    /**
     * Get transactions by cashier
     * @param cashierId The cashier's user ID
     * @return Flow of cashier's transactions
     */
    fun getTransactionsByCashier(cashierId: String): Flow<List<Transaction>>
    
    /**
     * Create a new transaction
     * @param transaction The transaction to create
     * @return Result containing the created transaction or error
     */
    suspend fun createTransaction(transaction: Transaction): Result<Transaction>
    
    /**
     * Update an existing transaction
     * @param transaction The transaction with updated data
     * @return Result indicating success or failure
     */
    suspend fun updateTransaction(transaction: Transaction): Result<Unit>
    
    /**
     * Complete a pending transaction
     * @param transactionId The transaction ID
     * @return Result indicating success or failure
     */
    suspend fun completeTransaction(transactionId: String): Result<Unit>
    
    /**
     * Cancel a pending transaction
     * @param transactionId The transaction ID
     * @param reason The reason for cancellation
     * @return Result indicating success or failure
     */
    suspend fun cancelTransaction(transactionId: String, reason: String): Result<Unit>
    
    /**
     * Process a refund
     * @param originalTransactionId The original transaction to refund
     * @param reason The reason for refund
     * @param processedBy The user ID processing the refund
     * @return Result containing the refund transaction or error
     */
    suspend fun processRefund(
        originalTransactionId: String,
        reason: String,
        processedBy: String
    ): Result<Transaction>
    
    /**
     * Get transaction summary for a period
     * @param startDate Start timestamp
     * @param endDate End timestamp
     * @return TransactionSummary for the period
     */
    suspend fun getTransactionSummary(startDate: Long, endDate: Long): TransactionSummary
    
    /**
     * Get transaction summary for an event
     * @param eventId The event ID
     * @return TransactionSummary for the event
     */
    suspend fun getEventTransactionSummary(eventId: String): TransactionSummary
    
    /**
     * Get cashier shift summary
     * @param cashierId The cashier's user ID
     * @param shiftStart The shift start time
     * @return CashierShiftSummary for the shift
     */
    suspend fun getCashierShiftSummary(cashierId: String, shiftStart: Long): CashierShiftSummary
    
    /**
     * Start a new cashier shift
     * @param cashierId The cashier's user ID
     * @param startingCash The starting cash amount
     * @return Result with shift start time or error
     */
    suspend fun startCashierShift(cashierId: String, startingCash: Double): Result<Long>
    
    /**
     * End a cashier shift
     * @param cashierId The cashier's user ID
     * @param shiftStart The shift start time
     * @param actualCash The actual cash on hand
     * @return Result with CashierShiftSummary or error
     */
    suspend fun endCashierShift(
        cashierId: String,
        shiftStart: Long,
        actualCash: Double
    ): Result<CashierShiftSummary>
    
    /**
     * Get pending transactions that need sync
     * @return List of transactions pending sync
     */
    suspend fun getPendingSyncTransactions(): List<Transaction>
    
    /**
     * Sync local transactions with remote server
     * @return Result indicating sync success or failure
     */
    suspend fun syncWithRemote(): Result<Unit>
    
    /**
     * Get total revenue for an event
     * @param eventId The event ID
     * @return Total revenue amount
     */
    suspend fun getEventRevenue(eventId: String): Double
    
    /**
     * Get daily revenue
     * @param date The date timestamp (midnight)
     * @return Total revenue for the day
     */
    suspend fun getDailyRevenue(date: Long): Double
}
