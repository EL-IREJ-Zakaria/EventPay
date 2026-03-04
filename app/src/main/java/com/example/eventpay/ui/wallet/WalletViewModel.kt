package com.example.eventpay.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventpay.data.firebase.FirebaseService
import com.example.eventpay.data.firebase.FirestoreTransactionRepository
import com.example.eventpay.data.model.Transaction
import com.example.eventpay.data.repository.TransactionRepository
import com.example.eventpay.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class WalletState(
    val isLoading: Boolean = false,
    val walletBalance: Double = 0.0,
    val transactions: List<Transaction> = emptyList(),
    val error: String? = null,
    val success: String? = null
)

class WalletViewModel(
    private val userRepository: UserRepository,
    private val transactionRepository: TransactionRepository,
    private val firebaseService: FirebaseService,
    private val firestoreTransactionRepository: FirestoreTransactionRepository
) : ViewModel() {

    private val _walletState = MutableStateFlow(WalletState())
    val walletState: StateFlow<WalletState> = _walletState.asStateFlow()

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun loadWallet(userId: String) {
        viewModelScope.launch {
            _walletState.value = _walletState.value.copy(isLoading = true)
            try {
                // Try to get from Firestore first
                val userDoc = firestore.collection("users").document(userId).get().await()
                val balance = userDoc.getDouble("walletBalance") ?: 0.0
                
                _walletState.value = _walletState.value.copy(
                    isLoading = false,
                    walletBalance = balance
                )
            } catch (e: Exception) {
                // Fallback to local database
                try {
                    val user = userRepository.getUserById(userId)
                    _walletState.value = _walletState.value.copy(
                        isLoading = false,
                        walletBalance = user?.walletBalance ?: 0.0
                    )
                } catch (e2: Exception) {
                    _walletState.value = _walletState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun loadTransactions(userId: String) {
        viewModelScope.launch {
            try {
                // Get transactions from Firestore
                firestoreTransactionRepository.getTransactionsByUser(userId).collect { transactions ->
                    _walletState.value = _walletState.value.copy(transactions = transactions)
                }
            } catch (e: Exception) {
                // Fallback to local repository
                try {
                    transactionRepository.getTransactionsByUser(userId).collect { transactions ->
                        _walletState.value = _walletState.value.copy(transactions = transactions)
                    }
                } catch (e2: Exception) {
                    _walletState.value = _walletState.value.copy(
                        error = e2.message
                    )
                }
            }
        }
    }

    fun topUpWallet(userId: String, amount: Double, paymentMethod: String = "CARD") {
        viewModelScope.launch {
            _walletState.value = _walletState.value.copy(isLoading = true, error = null, success = null)
            
            if (amount <= 0) {
                _walletState.value = _walletState.value.copy(
                    isLoading = false,
                    error = "Invalid amount"
                )
                return@launch
            }

            try {
                // Get current balance from Firestore
                val userRef = firestore.collection("users").document(userId)
                val userDoc = userRef.get().await()
                
                if (!userDoc.exists()) {
                    _walletState.value = _walletState.value.copy(
                        isLoading = false,
                        error = "User not found"
                    )
                    return@launch
                }

                val currentBalance = userDoc.getDouble("walletBalance") ?: 0.0
                val newBalance = currentBalance + amount

                // Update balance in Firestore
                userRef.update("walletBalance", newBalance).await()

                // Create transaction record
                val transaction = Transaction(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = userId,
                    type = com.example.eventpay.data.model.TransactionType.WALLET_TOP_UP,
                    amount = amount,
                    paymentMethod = try {
                        com.example.eventpay.data.model.PaymentMethod.valueOf(paymentMethod)
                    } catch (e: IllegalArgumentException) {
                        com.example.eventpay.data.model.PaymentMethod.CARD
                    },
                    status = com.example.eventpay.data.model.TransactionStatus.COMPLETED,
                    description = "Wallet top-up",
                    createdAt = System.currentTimeMillis()
                )

                firestore.collection("transactions").document(transaction.id).set(transaction).await()

                _walletState.value = _walletState.value.copy(
                    isLoading = false,
                    walletBalance = newBalance,
                    success = "Wallet topped up successfully!"
                )
            } catch (e: Exception) {
                _walletState.value = _walletState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to top up wallet"
                )
            }
        }
    }

    fun makePurchase(
        userId: String, 
        amount: Double, 
        description: String, 
        eventId: String? = null,
        ticketId: String? = null
    ) {
        viewModelScope.launch {
            _walletState.value = _walletState.value.copy(isLoading = true, error = null, success = null)
            
            try {
                // Get current balance from Firestore
                val userRef = firestore.collection("users").document(userId)
                val userDoc = userRef.get().await()
                
                if (!userDoc.exists()) {
                    _walletState.value = _walletState.value.copy(
                        isLoading = false,
                        error = "User not found"
                    )
                    return@launch
                }

                val currentBalance = userDoc.getDouble("walletBalance") ?: 0.0

                if (currentBalance < amount) {
                    _walletState.value = _walletState.value.copy(
                        isLoading = false,
                        error = "Insufficient balance"
                    )
                    return@launch
                }

                val newBalance = currentBalance - amount

                // Update balance in Firestore
                userRef.update("walletBalance", newBalance).await()

                // Create transaction record
                val transaction = Transaction(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = userId,
                    eventId = eventId,
                    ticketId = ticketId,
                    type = com.example.eventpay.data.model.TransactionType.TICKET_PURCHASE,
                    amount = amount,
                    paymentMethod = com.example.eventpay.data.model.PaymentMethod.WALLET,
                    status = com.example.eventpay.data.model.TransactionStatus.COMPLETED,
                    description = description,
                    createdAt = System.currentTimeMillis()
                )

                firestore.collection("transactions").document(transaction.id).set(transaction).await()

                _walletState.value = _walletState.value.copy(
                    isLoading = false,
                    walletBalance = newBalance,
                    success = "Purchase completed successfully!"
                )
            } catch (e: Exception) {
                _walletState.value = _walletState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Purchase failed"
                )
            }
        }
    }

    fun requestRefund(transactionId: String, userId: String, reason: String) {
        viewModelScope.launch {
            _walletState.value = _walletState.value.copy(isLoading = true, error = null, success = null)
            
            try {
                val transactionRef = firestore.collection("transactions").document(transactionId)
                val transactionDoc = transactionRef.get().await()
                
                if (!transactionDoc.exists()) {
                    _walletState.value = _walletState.value.copy(
                        isLoading = false,
                        error = "Transaction not found"
                    )
                    return@launch
                }

                val transaction = transactionDoc.toObject(Transaction::class.java)
                if (transaction == null) {
                    _walletState.value = _walletState.value.copy(
                        isLoading = false,
                        error = "Invalid transaction"
                    )
                    return@launch
                }

                // Check if transaction can be refunded (within 24 hours)
                val hoursSinceTransaction = (System.currentTimeMillis() - transaction.createdAt) / (1000 * 60 * 60)
                if (hoursSinceTransaction > 24) {
                    _walletState.value = _walletState.value.copy(
                        isLoading = false,
                        error = "Refund period expired (24 hours)"
                    )
                    return@launch
                }

                // Update transaction status
                transactionRef.update(
                    "status", com.example.eventpay.data.model.TransactionStatus.REFUNDED.name,
                    "refundReason", reason,
                    "refundedAt", System.currentTimeMillis()
                ).await()

                // Refund to wallet
                val userRef = firestore.collection("users").document(userId)
                val userDoc = userRef.get().await()
                val currentBalance = userDoc.getDouble("walletBalance") ?: 0.0
                val newBalance = currentBalance + transaction.amount

                userRef.update("walletBalance", newBalance).await()

                // Create refund transaction
                val refundTransaction = Transaction(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = userId,
                    type = com.example.eventpay.data.model.TransactionType.REFUND,
                    amount = transaction.amount,
                    paymentMethod = com.example.eventpay.data.model.PaymentMethod.WALLET,
                    status = com.example.eventpay.data.model.TransactionStatus.COMPLETED,
                    description = "Refund for transaction $transactionId: $reason",
                    createdAt = System.currentTimeMillis()
                )

                firestore.collection("transactions").document(refundTransaction.id).set(refundTransaction).await()

                _walletState.value = _walletState.value.copy(
                    isLoading = false,
                    walletBalance = newBalance,
                    success = "Refund processed successfully!"
                )
            } catch (e: Exception) {
                _walletState.value = _walletState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Refund failed"
                )
            }
        }
    }

    fun getTransactionHistory(userId: String, limit: Long = 50) {
        viewModelScope.launch {
            _walletState.value = _walletState.value.copy(isLoading = true)
            
            try {
                val transactions = mutableListOf<Transaction>()
                val querySnapshot = firestore.collection("transactions")
                    .whereEqualTo("userId", userId)
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(limit)
                    .get()
                    .await()

                for (doc in querySnapshot.documents) {
                    doc.toObject(Transaction::class.java)?.let { transactions.add(it) }
                }

                _walletState.value = _walletState.value.copy(
                    isLoading = false,
                    transactions = transactions
                )
            } catch (e: Exception) {
                _walletState.value = _walletState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun clearError() {
        _walletState.value = _walletState.value.copy(error = null)
    }

    fun clearSuccess() {
        _walletState.value = _walletState.value.copy(success = null)
    }
}
