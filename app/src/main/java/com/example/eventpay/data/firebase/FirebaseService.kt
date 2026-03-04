package com.example.eventpay.data.firebase

import com.example.eventpay.data.model.User
import com.example.eventpay.data.model.UserRole
import com.example.eventpay.domain.model.CheckInResult
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// Note: CheckInRecord, CheckInResult, DashboardStats are imported from FirebaseModels.kt
// These classes are defined there to avoid redeclaration errors across multiple files

class FirebaseService {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val usersCollection = firestore.collection("users")
    private val eventsCollection = firestore.collection("events")
    private val ticketsCollection = firestore.collection("tickets")
    private val transactionsCollection = firestore.collection("transactions")
    private val checkInsCollection = firestore.collection("checkIns")

    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    // ─── Authentication ───────────────────────────────────────────────────────

    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let { Result.success(it) }
                ?: Result.failure(Exception("Login failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(
        email: String,
        password: String,
        fullName: String,
        role: UserRole
    ): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return Result.failure(Exception("Registration failed"))

            val userData = mapOf(
                "id" to firebaseUser.uid,
                "email" to email,
                "fullName" to fullName,
                "role" to role.name,
                "walletBalance" to 0.0,
                "isActive" to true,
                "createdAt" to System.currentTimeMillis(),
                "lastLoginAt" to System.currentTimeMillis()
            )
            usersCollection.document(firebaseUser.uid).set(userData).await()
            Result.success(firebaseUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isUserLoggedIn(): Boolean = auth.currentUser != null
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    // ─── User Operations ──────────────────────────────────────────────────────

    suspend fun getUser(userId: String): Result<User> {
        return try {
            val doc = usersCollection.document(userId).get().await()
            val user = doc.toObject(User::class.java)
            user?.let { Result.success(it) } ?: Result.failure(Exception("User not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUser(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.id).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUsersByRole(role: UserRole): Result<List<User>> {
        return try {
            val snapshot = usersCollection
                .whereEqualTo("role", role.name)
                .get()
                .await()
            val users = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            val snapshot = usersCollection.get().await()
            val users = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Admin creates a scanner account using a secondary FirebaseApp instance
     * so the admin's current session is NOT disrupted.
     */
    suspend fun createScannerAccount(
        email: String,
        password: String,
        fullName: String,
        createdByAdminId: String
    ): Result<String> {
        val secondaryAppName = "scanner_creation_app"
        var secondaryApp: FirebaseApp? = null
        return try {
            val primaryApp = FirebaseApp.getInstance()
            secondaryApp = try {
                FirebaseApp.getInstance(secondaryAppName)
            } catch (e: IllegalStateException) {
                FirebaseApp.initializeApp(primaryApp.applicationContext, primaryApp.options, secondaryAppName)
            }
            val secondaryAuth = FirebaseAuth.getInstance(secondaryApp!!)
            val result = secondaryAuth.createUserWithEmailAndPassword(email, password).await()
            val newUserId = result.user?.uid ?: return Result.failure(Exception("Failed to create account"))
            secondaryAuth.signOut()

            val userData = mapOf(
                "id" to newUserId,
                "email" to email,
                "fullName" to fullName,
                "role" to UserRole.SCANNER.name,
                "walletBalance" to 0.0,
                "isActive" to true,
                "createdAt" to System.currentTimeMillis(),
                "lastLoginAt" to null,
                "createdBy" to createdByAdminId
            )
            usersCollection.document(newUserId).set(userData).await()
            Result.success(newUserId)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try { secondaryApp?.delete() } catch (_: Exception) {}
        }
    }

    suspend fun toggleUserActive(userId: String, isActive: Boolean): Result<Unit> {
        return try {
            usersCollection.document(userId).update("isActive", isActive).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserWalletBalance(userId: String, newBalance: Double): Result<Unit> {
        return try {
            usersCollection.document(userId).update("walletBalance", newBalance).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Check-In Operations ──────────────────────────────────────────────────

    /**
     * Record a QR scan check-in to Firestore.
     * Atomically increments the event checkedInCount and marks the ticket as checked in.
     */
    suspend fun recordCheckIn(
        ticketId: String,
        eventId: String,
        scannedBy: String,
        scannedByName: String,
        result: CheckInResult
    ): Result<String> {
        return try {
            val checkInData = mapOf(
                "ticketId" to ticketId,
                "eventId" to eventId,
                "scannedBy" to scannedBy,
                "scannedByName" to scannedByName,
                "scannedAt" to System.currentTimeMillis(),
                "result" to result.name
            )
            val docRef = checkInsCollection.add(checkInData).await()

            if (result == CheckInResult.SUCCESS) {
                ticketsCollection.document(ticketId).update(
                    mapOf(
                        "status" to "CHECKED_IN",
                        "checkedInAt" to System.currentTimeMillis(),
                        "checkedInBy" to scannedBy
                    )
                ).await()

                eventsCollection.document(eventId)
                    .update("checkedInCount", FieldValue.increment(1))
                    .await()
            }

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCheckInsForEvent(eventId: String): Result<List<CheckInRecord>> {
        return try {
            val snapshot = checkInsCollection
                .whereEqualTo("eventId", eventId)
                .get()
                .await()
            val records = snapshot.documents.mapNotNull { doc ->
                CheckInRecord(
                    id = doc.id,
                    ticketId = doc.getString("ticketId") ?: "",
                    eventId = doc.getString("eventId") ?: "",
                    scannedBy = doc.getString("scannedBy") ?: "",
                    scannedByName = doc.getString("scannedByName") ?: "",
                    scannedAt = doc.getLong("scannedAt") ?: 0L,
                    result = doc.getString("result")?.let {
                        runCatching { CheckInResult.valueOf(it) }.getOrDefault(CheckInResult.INVALID)
                    } ?: CheckInResult.INVALID
                )
            }
            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCheckInsByScanner(scannerId: String): Result<List<CheckInRecord>> {
        return try {
            val snapshot = checkInsCollection
                .whereEqualTo("scannedBy", scannerId)
                .get()
                .await()
            val records = snapshot.documents.mapNotNull { doc ->
                CheckInRecord(
                    id = doc.id,
                    ticketId = doc.getString("ticketId") ?: "",
                    eventId = doc.getString("eventId") ?: "",
                    scannedBy = doc.getString("scannedBy") ?: "",
                    scannedByName = doc.getString("scannedByName") ?: "",
                    scannedAt = doc.getLong("scannedAt") ?: 0L,
                    result = doc.getString("result")?.let {
                        runCatching { CheckInResult.valueOf(it) }.getOrDefault(CheckInResult.INVALID)
                    } ?: CheckInResult.INVALID
                )
            }
            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Dashboard Stats ──────────────────────────────────────────────────────

    suspend fun getDashboardStats(): Result<DashboardStats> {
        return try {
            val eventsSnap = eventsCollection.get().await()
            val ticketsSnap = ticketsCollection.get().await()
            val checkInsSnap = checkInsCollection.get().await()
            val scannersSnap = usersCollection.whereEqualTo("role", UserRole.SCANNER.name).get().await()

            val totalEvents = eventsSnap.size()
            val totalTickets = ticketsSnap.size()
            val totalCheckIns = checkInsSnap.documents.count {
                it.getString("result") == CheckInResult.SUCCESS.name
            }
            val totalScanners = scannersSnap.size()

            Result.success(DashboardStats(totalEvents, totalTickets, totalCheckIns, totalScanners))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Note: DashboardStats, CheckInRecord, and CheckInResult are defined in FirebaseModels.kt
// These were removed from here to avoid redeclaration conflicts
