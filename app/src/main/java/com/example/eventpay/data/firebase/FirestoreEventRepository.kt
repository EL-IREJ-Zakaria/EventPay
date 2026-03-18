package com.example.eventpay.data.firebase

import com.example.eventpay.data.mapper.EventMapper.toData
import com.example.eventpay.data.mapper.EventMapper.toDomain
import com.example.eventpay.data.model.Event as DataEvent
import com.example.eventpay.domain.model.Event as DomainEvent
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreEventRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val eventsCollection = firestore.collection("events")
    
    // Get all events as Flow
    fun getAllEventsFlow(): Flow<List<DomainEvent>> = callbackFlow {
        val listener = eventsCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val events = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(DataEvent::class.java)?.copy(id = doc.id)?.toDomain()
                } ?: emptyList()
                trySend(events)
            }
        awaitClose { listener.remove() }
    }
    
    // Get events by organizer
    fun getEventsByOrganizerFlow(organizerId: String): Flow<List<DomainEvent>> = callbackFlow {
        val listener = eventsCollection
            .whereEqualTo("organizerId", organizerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val events = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(DataEvent::class.java)?.copy(id = doc.id)?.toDomain()
                } ?: emptyList()
                trySend(events)
            }
        awaitClose { listener.remove() }
    }
    
    // Get single event
    suspend fun getEvent(eventId: String): Result<DomainEvent> {
        return try {
            val document = eventsCollection.document(eventId).get().await()
            val event = document.toObject(DataEvent::class.java)?.copy(id = document.id)?.toDomain()
            if (event != null) {
                Result.success(event)
            } else {
                Result.failure(Exception("Event not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Create event
    suspend fun createEvent(event: DomainEvent): Result<String> {
        return try {
            val dataEvent = event.toData()
            val docRef = eventsCollection.add(dataEvent).await()
            // Update with the generated ID
            eventsCollection.document(docRef.id).update("id", docRef.id).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Update event
    suspend fun updateEvent(event: DomainEvent): Result<Unit> {
        return try {
            eventsCollection.document(event.id).set(event.toData()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Delete event
    suspend fun deleteEvent(eventId: String): Result<Unit> {
        return try {
            eventsCollection.document(eventId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Update sold tickets count
    suspend fun incrementreservedTickets(eventId: String, count: Int = 1): Result<Unit> {
        return try {
            val eventDoc = eventsCollection.document(eventId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(eventDoc)
                val currentSold = snapshot.getLong("reservedTickets") ?: 0
                transaction.update(eventDoc, "reservedTickets", currentSold + count)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get all events (one-time fetch) - returns Flow for compatibility
    fun getEvents(): Flow<List<DomainEvent>> = callbackFlow {
        val listener = eventsCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val events = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(DataEvent::class.java)?.copy(id = doc.id)?.toDomain()
                } ?: emptyList()
                trySend(events)
            }
        awaitClose { listener.remove() }
    }
    
    // Get all events (one-time fetch) - returns Result for direct use
    suspend fun getAllEvents(): Result<List<DomainEvent>> {
        return try {
            val snapshot = eventsCollection.get().await()
            val events = snapshot.documents.mapNotNull { doc ->
                doc.toObject(DataEvent::class.java)?.copy(id = doc.id)?.toDomain()
            }
            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get events by organizer (one-time fetch) - returns Result for direct use
    suspend fun getEventsByOrganizer(organizerId: String): Result<List<DomainEvent>> {
        return try {
            val snapshot = eventsCollection
                .whereEqualTo("organizerId", organizerId)
                .get()
                .await()
            val events = snapshot.documents.mapNotNull { doc ->
                doc.toObject(DataEvent::class.java)?.copy(id = doc.id)?.toDomain()
            }
            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get active events (events that haven't passed)
    suspend fun getActiveEvents(): Result<List<DomainEvent>> {
        return try {
            val currentTime = System.currentTimeMillis()
            val snapshot = eventsCollection
                .whereGreaterThan("date", currentTime)
                .get()
                .await()
            val events = snapshot.documents.mapNotNull { doc ->
                doc.toObject(DataEvent::class.java)?.copy(id = doc.id)?.toDomain()
            }
            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Update ticket sales
    suspend fun updateTicketSales(eventId: String, ticketsSold: Int, vipTicketsSold: Int): Result<Unit> {
        return try {
            val eventDoc = eventsCollection.document(eventId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(eventDoc)
                val currentSold = snapshot.getLong("reservedTickets") ?: 0
                val currentvipReserved = snapshot.getLong("vipReserved") ?: 0
                transaction.update(eventDoc, mapOf(
                    "reservedTickets" to currentSold + ticketsSold,
                    "vipReserved" to currentvipReserved + vipTicketsSold
                ))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Update check-in count
    suspend fun updateCheckInCount(eventId: String, increment: Int): Result<Unit> {
        return try {
            val eventDoc = eventsCollection.document(eventId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(eventDoc)
                val currentCount = snapshot.getLong("checkedInCount") ?: 0
                transaction.update(eventDoc, "checkedInCount", currentCount + increment)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * CRITICAL SECURITY FIX: Get events only for assigned scanner
     * This ensures scanners can ONLY see events they're assigned to
     */
    suspend fun getEventsForScanner(scannerId: String): Result<List<DomainEvent>> {
        return try {
            // Get current time and subtract 1 day (86400000 ms) for events from last 24h
            val oneDayAgo = System.currentTimeMillis() - 86400000
            
            val snapshot = eventsCollection
                .whereArrayContains("assignedScanners", scannerId)
                .whereGreaterThan("date", oneDayAgo)
                .orderBy("date", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .await()
            
            val events = snapshot.documents.mapNotNull { doc ->
                doc.toObject(DataEvent::class.java)?.copy(id = doc.id)?.toDomain()
            }
            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get events for scanner as Flow for real-time updates
     */
    fun getEventsForScannerFlow(scannerId: String): Flow<List<DomainEvent>> = callbackFlow {
        val oneDayAgo = System.currentTimeMillis() - 86400000
        
        val listener = eventsCollection
            .whereArrayContains("assignedScanners", scannerId)
            .whereGreaterThan("date", oneDayAgo)
            .orderBy("date", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val events = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(DataEvent::class.java)?.copy(id = doc.id)?.toDomain()
                } ?: emptyList()
                trySend(events)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Publish an event - changes status from DRAFT to PUBLISHED
     */
    suspend fun publishEvent(eventId: String): Result<Unit> {
        return try {
            eventsCollection.document(eventId).update(mapOf(
                "status" to "PUBLISHED",
                "isPublished" to true,
                "updatedAt" to System.currentTimeMillis()
            )).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cancel an event
     */
    suspend fun cancelEvent(eventId: String, reason: String? = null): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to "CANCELLED",
                "updatedAt" to System.currentTimeMillis()
            )
            reason?.let { updates["cancellationReason"] = it }
            
            eventsCollection.document(eventId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Assign a scanner to an event
     */
    suspend fun assignScanner(eventId: String, scannerId: String): Result<Unit> {
        return try {
            eventsCollection.document(eventId)
                .update("assignedScanners", com.google.firebase.firestore.FieldValue.arrayUnion(scannerId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Remove a scanner from an event
     */
    suspend fun removeScanner(eventId: String, scannerId: String): Result<Unit> {
        return try {
            eventsCollection.document(eventId)
                .update("assignedScanners", com.google.firebase.firestore.FieldValue.arrayRemove(scannerId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
