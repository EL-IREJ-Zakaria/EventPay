package com.example.eventpay.data.local.dao

import androidx.room.*
import com.example.eventpay.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    suspend fun getTransactionById(transactionId: String): Transaction?

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getTransactionsByUser(userId: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE eventId = :eventId ORDER BY timestamp DESC")
    fun getTransactionsByEvent(eventId: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE isSynced = 0")
    suspend fun getUnsyncedTransactions(): List<Transaction>

    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND type = :type")
    suspend fun getTotalByType(userId: String, type: String): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("UPDATE transactions SET isSynced = 1 WHERE id = :transactionId")
    suspend fun markAsSynced(transactionId: String)

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>>
}
