package com.example.eventpay.data.repository

import com.example.eventpay.data.local.dao.TransactionDao
import com.example.eventpay.data.model.Transaction
import com.example.eventpay.data.model.TransactionStatus
import com.example.eventpay.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class TransactionRepository(private val transactionDao: TransactionDao) {
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    suspend fun getTransactionById(transactionId: String): Transaction? {
        return transactionDao.getTransactionById(transactionId)
    }

    fun getTransactionsByUser(userId: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByUser(userId)
    }

    fun getTransactionsByEvent(eventId: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByEvent(eventId)
    }

    fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(startTime, endTime)
    }

    suspend fun getUnsyncedTransactions(): List<Transaction> {
        return transactionDao.getUnsyncedTransactions()
    }

    suspend fun createTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun createTicketPurchase(
        userId: String,
        eventId: String,
        ticketId: String,
        amount: Double,
        description: String
    ): Transaction {
        val currentTime = System.currentTimeMillis()
        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            userId = userId,
            type = TransactionType.TICKET_PURCHASE,
            amount = amount,
            description = description,
            status = TransactionStatus.COMPLETED,
            eventId = eventId,
            ticketId = ticketId,
            timestamp = currentTime,
            createdAt = currentTime
        )
        transactionDao.insertTransaction(transaction)
        return transaction
    }

    suspend fun createWalletTopUp(
        userId: String,
        amount: Double,
        description: String
    ): Transaction {
        val currentTime = System.currentTimeMillis()
        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            userId = userId,
            type = TransactionType.WALLET_TOP_UP,
            amount = amount,
            description = description,
            status = TransactionStatus.COMPLETED,
            timestamp = currentTime,
            createdAt = currentTime
        )
        transactionDao.insertTransaction(transaction)
        return transaction
    }

    suspend fun createPurchase(
        userId: String,
        amount: Double,
        description: String,
        eventId: String? = null
    ): Transaction {
        val currentTime = System.currentTimeMillis()
        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            userId = userId,
            type = TransactionType.MERCHANDISE_PURCHASE,
            amount = amount,
            description = description,
            status = TransactionStatus.COMPLETED,
            eventId = eventId,
            timestamp = currentTime,
            createdAt = currentTime
        )
        transactionDao.insertTransaction(transaction)
        return transaction
    }

    suspend fun markAsSynced(transactionId: String) {
        transactionDao.markAsSynced(transactionId)
    }

    suspend fun getTotalSpending(userId: String): Double {
        val ticketTotal = transactionDao.getTotalByType(userId, TransactionType.TICKET_PURCHASE.name) ?: 0.0
        val merchTotal = transactionDao.getTotalByType(userId, TransactionType.MERCHANDISE_PURCHASE.name) ?: 0.0
        return ticketTotal + merchTotal
    }

    suspend fun getTotalTopUps(userId: String): Double {
        return transactionDao.getTotalByType(userId, TransactionType.WALLET_TOP_UP.name) ?: 0.0
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }
}
