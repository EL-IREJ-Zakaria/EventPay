package com.example.eventpay.data.firebase

import com.example.eventpay.data.model.Transaction
import com.example.eventpay.data.model.TransactionType
import com.example.eventpay.data.model.TransactionStatus
import com.example.eventpay.data.model.PaymentMethod
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreTransactionRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val transactionsCollection = firestore.collection("transactions")
    
    // Generate transaction ID
    private fun generateTransactionId(): String {
        return "TXN-${UUID.randomUUID().toString().take(10).uppercase()}"
    }
    
    // Get transactions by user
    fun getTransactionsByUserFlow(userId: String): Flow<List<Transaction>> = callbackFlow {
        val listener = transactionsCollection
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val transactions = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(transactions)
            }
        awaitClose { listener.remove() }
    }
    
    // Get transactions by user (alias for Flow version)
    fun getTransactionsByUser(userId: String): Flow<List<Transaction>> = getTransactionsByUserFlow(userId)
    
    // Get all transactions (for admin/cashier)
    fun getAllTransactionsFlow(): Flow<List<Transaction>> = callbackFlow {
        val listener = transactionsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val transactions = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(transactions)
            }
        awaitClose { listener.remove() }
    }
    
    // Get transactions by event
    fun getTransactionsByEventFlow(eventId: String): Flow<List<Transaction>> = callbackFlow {
        val listener = transactionsCollection
            .whereEqualTo("eventId", eventId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val transactions = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(transactions)
            }
        awaitClose { listener.remove() }
    }
    
    // Create transaction
    suspend fun createTransaction(
        userId: String,
        type: TransactionType,
        amount: Double,
        description: String,
        paymentMethod: PaymentMethod,
        eventId: String? = null,
        ticketId: String? = null
    ): Result<Transaction> {
        return try {
            val currentTime = System.currentTimeMillis()
            val transaction = Transaction(
                id = generateTransactionId(),
                userId = userId,
                type = type,
                amount = amount,
                description = description,
                paymentMethod = paymentMethod,
                status = TransactionStatus.COMPLETED,
                eventId = eventId,
                ticketId = ticketId,
                timestamp = currentTime,
                createdAt = currentTime,
                isSynced = true
            )
            
            transactionsCollection.document(transaction.id).set(transaction).await()
            Result.success(transaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get single transaction
    suspend fun getTransaction(transactionId: String): Result<Transaction> {
        return try {
            val document = transactionsCollection.document(transactionId).get().await()
            val transaction = document.toObject(Transaction::class.java)
            if (transaction != null) {
                Result.success(transaction.copy(id = document.id))
            } else {
                Result.failure(Exception("Transaction not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get total revenue for event
    suspend fun getEventRevenue(eventId: String): Result<Double> {
        return try {
            val snapshot = transactionsCollection
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("type", TransactionType.TICKET_PURCHASE.name)
                .get()
                .await()
            
            var total = 0.0
            snapshot.documents.forEach { doc ->
                val amount = doc.getDouble("amount") ?: 0.0
                total += amount
            }
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get total revenue for date range
    suspend fun getRevenueByDateRange(startDate: Long, endDate: Long): Result<Double> {
        return try {
            val snapshot = transactionsCollection
                .whereGreaterThanOrEqualTo("timestamp", startDate)
                .whereLessThanOrEqualTo("timestamp", endDate)
                .whereEqualTo("type", TransactionType.TICKET_PURCHASE.name)
                .get()
                .await()
            
            var total = 0.0
            snapshot.documents.forEach { doc ->
                val amount = doc.getDouble("amount") ?: 0.0
                total += amount
            }
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get daily transactions for cashier closing
    suspend fun getDailyTransactions(userId: String, startOfDay: Long, endOfDay: Long): Result<List<Transaction>> {
        return try {
            val snapshot = transactionsCollection
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("timestamp", startOfDay)
                .whereLessThanOrEqualTo("timestamp", endOfDay)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val transactions = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Transaction::class.java)?.copy(id = doc.id)
            }
            Result.success(transactions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get transaction count by type
    suspend fun getTransactionCountByType(type: TransactionType): Result<Int> {
        return try {
            val snapshot = transactionsCollection
                .whereEqualTo("type", type.name)
                .get()
                .await()
            Result.success(snapshot.size())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get total sales by payment method
    suspend fun getTotalByPaymentMethod(paymentMethod: PaymentMethod): Result<Double> {
        return try {
            val snapshot = transactionsCollection
                .whereEqualTo("paymentMethod", paymentMethod.name)
                .get()
                .await()
            
            var total = 0.0
            snapshot.documents.forEach { doc ->
                val amount = doc.getDouble("amount") ?: 0.0
                total += amount
            }
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get all transactions (one-time fetch)
    suspend fun getAllTransactions(): Result<List<Transaction>> {
        return try {
            val snapshot = transactionsCollection.get().await()
            val transactions = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Transaction::class.java)?.copy(id = doc.id)
            }
            Result.success(transactions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}