package com.example.eventpay.data.firebase

import com.example.eventpay.data.model.Ticket
import com.example.eventpay.data.model.TicketStatus
import com.example.eventpay.data.model.TicketType
import com.example.eventpay.domain.model.CheckInRecord
import com.example.eventpay.domain.model.CheckInResult
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Result of a check-in attempt
 */
sealed class CheckInAttemptResult {
    data class Success(val ticket: Ticket, val checkInRecord: CheckInRecord) : CheckInAttemptResult()
    data class AlreadyCheckedIn(val ticket: Ticket, val previousScanTime: Long?) : CheckInAttemptResult()
    data class WrongEvent(val ticket: Ticket, val expectedEventId: String) : CheckInAttemptResult()
    data class InvalidTicket(val reason: String) : CheckInAttemptResult()
    data class Error(val exception: Exception) : CheckInAttemptResult()
}

class FirestoreTicketRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val ticketsCollection = firestore.collection("tickets")
    
    // Generate unique QR Code
    fun generateQRCode(): String {
        return "TKT-${UUID.randomUUID().toString().take(8).uppercase()}"
    }
    
    // Get tickets by user
    fun getTicketsByUserFlow(userId: String): Flow<List<Ticket>> = callbackFlow {
        val listener = ticketsCollection
            .whereEqualTo("userId", userId)
            .orderBy("reservationDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val tickets = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Ticket::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(tickets)
            }
        awaitClose { listener.remove() }
    }
    
    // Get tickets by event
    fun getTicketsByEventFlow(eventId: String): Flow<List<Ticket>> = callbackFlow {
        val listener = ticketsCollection
            .whereEqualTo("eventId", eventId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val tickets = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Ticket::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(tickets)
            }
        awaitClose { listener.remove() }
    }
    
    // Get ticket by QR Code
    suspend fun getTicketByQRCode(qrCode: String): Result<Ticket> {
        return try {
            val snapshot = ticketsCollection
                .whereEqualTo("qrCode", qrCode)
                .limit(1)
                .get()
                .await()
            val ticket = snapshot.documents.firstOrNull()?.toObject(Ticket::class.java)
            if (ticket != null) {
                Result.success(ticket.copy(id = snapshot.documents.first().id))
            } else {
                Result.failure(Exception("Ticket not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get single ticket
    suspend fun getTicket(ticketId: String): Result<Ticket> {
        return try {
            val document = ticketsCollection.document(ticketId).get().await()
            val ticket = document.toObject(Ticket::class.java)?.copy(id = document.id)
            if (ticket != null) {
                Result.success(ticket)
            } else {
                Result.failure(Exception("Ticket not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Create ticket
    suspend fun createTicket(
        eventId: String,
        userId: String,
        ticketType: TicketType,
        price: Double
    ): Result<Ticket> {
        return try {
            val qrCode = generateQRCode()
            val ticket = Ticket(
                id = "", // Will be set after creation
                eventId = eventId,
                userId = userId,
                qrCode = qrCode,
                ticketType = ticketType,
                /* price = */ //  price,
                isCheckedIn = false,
                checkedInAt = null,
                reservationDate = System.currentTimeMillis()
            )
            
            val docRef = ticketsCollection.add(ticket).await()
            val createdTicket = ticket.copy(id = docRef.id)
            ticketsCollection.document(docRef.id).set(createdTicket).await()
            
            Result.success(createdTicket)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Check-in ticket
    suspend fun checkInTicket(qrCode: String): Result<Ticket> {
        return try {
            val snapshot = ticketsCollection
                .whereEqualTo("qrCode", qrCode)
                .limit(1)
                .get()
                .await()
            
            val doc = snapshot.documents.firstOrNull()
            if (doc == null) {
                return Result.failure(Exception("Ticket not found"))
            }
            
            val ticket = doc.toObject(Ticket::class.java)
            if (ticket == null) {
                return Result.failure(Exception("Invalid ticket"))
            }
            
            if (ticket.isCheckedIn) {
                return Result.failure(Exception("Ticket already checked in"))
            }
            
            val updatedTicket = ticket.copy(
                id = doc.id,
                isCheckedIn = true,
                checkedInAt = System.currentTimeMillis()
            )
            
            ticketsCollection.document(doc.id).set(updatedTicket).await()
            Result.success(updatedTicket)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get checked-in count for event
    suspend fun getCheckedInCount(eventId: String): Result<Int> {
        return try {
            val snapshot = ticketsCollection
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("isCheckedIn", true)
                .get()
                .await()
            Result.success(snapshot.size())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get total tickets sold for event
    suspend fun getTicketsSoldCount(eventId: String): Result<Int> {
        return try {
            val snapshot = ticketsCollection
                .whereEqualTo("eventId", eventId)
                .get()
                .await()
            Result.success(snapshot.size())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get all tickets (for admin)
    suspend fun getAllTickets(): Result<List<Ticket>> {
        return try {
            val snapshot = ticketsCollection.get().await()
            val tickets = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Ticket::class.java)?.copy(id = doc.id)
            }
            Result.success(tickets)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTicketsByEvent(eventId: String): Result<List<Ticket>> {
        return try {
            val snapshot = ticketsCollection
                .whereEqualTo("eventId", eventId)
                .orderBy("reservationDate", Query.Direction.DESCENDING)
                .get()
                .await()
            val tickets = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Ticket::class.java)?.copy(id = doc.id)
            }
            Result.success(tickets)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTicket(ticketId: String): Result<Unit> {
        return try {
            ticketsCollection.document(ticketId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // ATOMIC CHECK-IN OPERATIONS WITH TRANSACTIONS
    // ═══════════════════════════════════════════════════════════════════════════════

    private val checkInsCollection = firestore.collection("checkIns")
    private val eventsCollection = firestore.collection("events")

    /**
     * CRITICAL: Atomic check-in operation using Firestore transaction
     * 
     * This ensures:
     * 1. Check for duplicate scans (prevent double entry)
     * 2. Validate ticket status
     * 3. Update ticket status to USED
     * 4. Update event check-in count
     * 5. Create check-in record
     * 
     * ALL operations succeed or ALL fail (atomic guarantee)
     */
    suspend fun recordCheckInAtomic(
        qrCode: String,
        eventId: String,
        scannerId: String,
        scannerName: String,
        deviceId: String = "",
        location: GeoPoint? = null
    ): CheckInAttemptResult {
        return try {
            // First, find the ticket by QR code
            val ticketQuery = ticketsCollection
                .whereEqualTo("qrCode", qrCode)
                .limit(1)
                .get()
                .await()

            if (ticketQuery.isEmpty) {
                // Record failed check-in attempt
                recordFailedCheckIn(
                    ticketId = qrCode,
                    eventId = eventId,
                    scannerId = scannerId,
                    scannerName = scannerName,
                    deviceId = deviceId,
                    result = CheckInResult.NOT_FOUND,
                    message = "Ticket not found for QR code"
                )
                return CheckInAttemptResult.InvalidTicket("Ticket not found")
            }

            val ticketDoc = ticketQuery.documents.first()
            val ticket = ticketDoc.toObject(Ticket::class.java)?.copy(id = ticketDoc.id)
                ?: return CheckInAttemptResult.Error(Exception("Failed to parse ticket"))

            // Validate ticket
            when {
                ticket.eventId != eventId -> {
                    recordFailedCheckIn(
                        ticketId = ticket.id,
                        eventId = eventId,
                        scannerId = scannerId,
                        scannerName = scannerName,
                        deviceId = deviceId,
                        result = CheckInResult.WRONG_EVENT,
                        message = "Ticket is for event ${ticket.eventId}, not $eventId"
                    )
                    return CheckInAttemptResult.WrongEvent(ticket, eventId)
                }
                ticket.status == TicketStatus.USED || ticket.isCheckedIn -> {
                    return CheckInAttemptResult.AlreadyCheckedIn(ticket, ticket.checkedInAt)
                }
                ticket.status == TicketStatus.CANCELLED -> {
                    recordFailedCheckIn(
                        ticketId = ticket.id,
                        eventId = eventId,
                        scannerId = scannerId,
                        scannerName = scannerName,
                        deviceId = deviceId,
                        result = CheckInResult.CANCELLED,
                        message = "Ticket was cancelled"
                    )
                    return CheckInAttemptResult.InvalidTicket("Ticket was cancelled")
                }
                ticket.status == /* TicketStatus.REFUNDED */ TicketStatus.CANCELLED -> {
                    recordFailedCheckIn(
                        ticketId = ticket.id,
                        eventId = eventId,
                        scannerId = scannerId,
                        scannerName = scannerName,
                        deviceId = deviceId,
                        result = CheckInResult.CANCELLED,
                        message = "Ticket was refunded"
                    )
                    return CheckInAttemptResult.InvalidTicket("Ticket was refunded")
                }
                ticket.status == TicketStatus.EXPIRED -> {
                    recordFailedCheckIn(
                        ticketId = ticket.id,
                        eventId = eventId,
                        scannerId = scannerId,
                        scannerName = scannerName,
                        deviceId = deviceId,
                        result = CheckInResult.EXPIRED,
                        message = "Ticket has expired"
                    )
                    return CheckInAttemptResult.InvalidTicket("Ticket has expired")
                }
            }

            // All validations passed - proceed with atomic transaction
            val now = System.currentTimeMillis()
            val checkInId = "CI-${UUID.randomUUID().toString().take(8).uppercase()}"
            
            firestore.runTransaction { transaction ->
                // 1. Update ticket status
                transaction.update(
                    ticketsCollection.document(ticket.id),
                    mapOf(
                        "status" to TicketStatus.USED.name,
                        "isCheckedIn" to true,
                        "checkedInAt" to now,
                        "checkedInBy" to scannerId,
                        "deviceId" to deviceId
                    )
                )

                // 2. Increment event check-in count
                val eventDoc = eventsCollection.document(eventId)
                val eventSnapshot = transaction.get(eventDoc)
                val currentCount = eventSnapshot.getLong("checkedInCount") ?: 0
                transaction.update(eventDoc, "checkedInCount", currentCount + 1)

                // 3. Create check-in record
                val checkInData: HashMap<String, Any> = hashMapOf(
                    "id" to checkInId,
                    "ticketId" to ticket.id,
                    "eventId" to eventId,
                    "userId" to ticket.userId,
                    "scannedBy" to scannerId,
                    "scannedByName" to scannerName,
                    "scannedByRole" to "SCANNER",
                    "scannedAt" to now,
                    "deviceId" to deviceId,
                    "result" to CheckInResult.SUCCESS.name,
                    "message" to "Check-in successful"
                )
                location?.let { loc ->
                    val locationMap = hashMapOf(
                        "latitude" to loc.latitude,
                        "longitude" to loc.longitude
                    )
                    checkInData["location"] = locationMap
                }
                
                transaction.set(checkInsCollection.document(checkInId), checkInData)
            }.await()

            // Create the updated ticket and check-in record for return
            val updatedTicket = ticket.copy(
                id = ticket.id,
                status = TicketStatus.USED,
                isCheckedIn = true,
                checkedInAt = now
            )

            val checkInRecord = CheckInRecord(
                id = checkInId,
                ticketId = ticket.id,
                eventId = eventId,
                userId = ticket.userId,
                scannedBy = scannerId,
                scannedByName = scannerName,
                scannedAt = now,
                deviceId = deviceId,
                result = CheckInResult.SUCCESS,
                message = "Check-in successful"
            )

            CheckInAttemptResult.Success(updatedTicket, checkInRecord)

        } catch (e: Exception) {
            CheckInAttemptResult.Error(e)
        }
    }

    /**
     * Record a failed check-in attempt for audit purposes
     */
    private suspend fun recordFailedCheckIn(
        ticketId: String,
        eventId: String,
        scannerId: String,
        scannerName: String,
        deviceId: String,
        result: CheckInResult,
        message: String
    ) {
        try {
            val checkInId = "CI-${UUID.randomUUID().toString().take(8).uppercase()}"
            val checkInData = hashMapOf(
                "id" to checkInId,
                "ticketId" to ticketId,
                "eventId" to eventId,
                "userId" to "",
                "scannedBy" to scannerId,
                "scannedByName" to scannerName,
                "scannedByRole" to "SCANNER",
                "scannedAt" to System.currentTimeMillis(),
                "deviceId" to deviceId,
                "result" to result.name,
                "message" to message
            )
            checkInsCollection.document(checkInId).set(checkInData).await()
        } catch (e: Exception) {
            // Log but don't fail - this is best-effort audit logging
            e.printStackTrace()
        }
    }

    /**
     * Get check-ins for a specific scanner (for scan history)
     */
    fun getCheckInsForScannerFlow(scannerId: String): Flow<List<CheckInRecord>> = callbackFlow {
        val listener = checkInsCollection
            .whereEqualTo("scannedBy", scannerId)
            .orderBy("scannedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val checkIns = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(CheckInRecord::class.java)
                } ?: emptyList()
                trySend(checkIns)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Get today's check-ins for a scanner
     */
    suspend fun getTodaysCheckIns(scannerId: String): Result<List<CheckInRecord>> {
        return try {
            val startOfDay = getStartOfDay()
            val snapshot = checkInsCollection
                .whereEqualTo("scannedBy", scannerId)
                .whereGreaterThanOrEqualTo("scannedAt", startOfDay)
                .orderBy("scannedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val checkIns = snapshot.documents.mapNotNull { doc ->
                doc.toObject(CheckInRecord::class.java)
            }
            Result.success(checkIns)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get check-in statistics for a scanner's session
     */
    suspend fun getScannerStats(scannerId: String, sessionStart: Long): Result<ScannerSessionStats> {
        return try {
            val snapshot = checkInsCollection
                .whereEqualTo("scannedBy", scannerId)
                .whereGreaterThanOrEqualTo("scannedAt", sessionStart)
                .get()
                .await()

            val checkIns = snapshot.documents.mapNotNull { doc ->
                doc.toObject(CheckInRecord::class.java)
            }

            val totalScans = checkIns.size
            val successfulScans = checkIns.count { it.result == CheckInResult.SUCCESS }
            val failedScans = totalScans - successfulScans
            val successRate = if (totalScans > 0) (successfulScans.toFloat() / totalScans) * 100 else 0f

            Result.success(
                ScannerSessionStats(
                    totalScans = totalScans,
                    successfulScans = successfulScans,
                    failedScans = failedScans,
                    successRate = successRate
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getStartOfDay(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}

/**
 * Statistics for a scanner's session
 */
data class ScannerSessionStats(
    val totalScans: Int,
    val successfulScans: Int,
    val failedScans: Int,
    val successRate: Float
)
