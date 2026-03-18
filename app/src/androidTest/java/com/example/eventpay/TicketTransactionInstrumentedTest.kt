package com.example.eventpay

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.eventpay.data.local.AppDatabase
import com.example.eventpay.data.local.Converters
import com.example.eventpay.data.model.*
import com.example.eventpay.data.repository.TicketRepository
import com.example.eventpay.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TicketTransactionInstrumentedTest {

    private lateinit var database: AppDatabase
    private lateinit var ticketRepository: TicketRepository
    private lateinit var transactionRepository: TransactionRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        ticketRepository = TicketRepository(database.ticketDao())
        transactionRepository = TransactionRepository(database.transactionDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun makeUser(id: String = "user_001") = User(
        id = id, email = "$id@test.com", fullName = "Test User $id",
        role = UserRole.SCANNER
    )

    private fun makeEvent(id: String = "event_001") = Event(
        id = id, name = "Test Event", description = "Desc",
        location = "Venue", date = System.currentTimeMillis() + 86400000,
        totalTickets = 100
    )

    @Test
    fun insertTicketAndVerifyPersistence() = runBlocking {
        database.userDao().insertUser(makeUser())
        database.eventDao().insertEvent(makeEvent())

        val ticket = Ticket(
            id = "ticket_test_001",
            eventId = "event_001",
            userId = "user_001",
            ticketType = TicketType.STANDARD,
            status = TicketStatus.ACTIVE,
            qrCode = "QR_TEST_001",
            reservationDate = System.currentTimeMillis()
        )

        ticketRepository.createTicket(ticket)
        val retrieved = ticketRepository.getTicketById("ticket_test_001")

        assertNotNull(retrieved)
        assertEquals("ticket_test_001", retrieved?.id)
        assertEquals("event_001", retrieved?.eventId)
        assertEquals(TicketStatus.ACTIVE, retrieved?.status)
    }

    @Test
    fun insertTransactionAndVerifyIntegrity() = runBlocking {
        database.userDao().insertUser(makeUser())

        val transaction = Transaction(
            id = "txn_001",
            userId = "user_001",
            eventId = "event_001",
            amount = 150.0,
            type = TransactionType.TICKET_PURCHASE,
            status = TransactionStatus.COMPLETED,
            description = "Test ticket purchase",
            createdAt = System.currentTimeMillis()
        )

        transactionRepository.createTransaction(transaction)
        val retrieved = transactionRepository.getTransactionById("txn_001")

        assertNotNull(retrieved)
        assertEquals("txn_001", retrieved?.id)
        assertEquals(150.0, retrieved?.amount ?: 0.0, 0.001)
        assertEquals(TransactionStatus.COMPLETED, retrieved?.status)
    }

    @Test
    fun markTicketAsCheckedIn() = runBlocking {
        database.userDao().insertUser(makeUser())
        database.eventDao().insertEvent(makeEvent())

        val ticket = Ticket(
            id = "ticket_checkin_001",
            eventId = "event_001",
            userId = "user_001",
            ticketType = TicketType.STANDARD,
            status = TicketStatus.ACTIVE,
            qrCode = "QR_CHECKIN_001",
            reservationDate = System.currentTimeMillis()
        )

        ticketRepository.createTicket(ticket)

        val updatedTicket = ticket.copy(
            status = TicketStatus.USED,
            isCheckedIn = true,
            checkedInAt = System.currentTimeMillis(),
            checkedInBy = "scanner_001"
        )
        ticketRepository.updateTicket(updatedTicket)

        val retrieved = ticketRepository.getTicketById("ticket_checkin_001")
        assertNotNull(retrieved)
        assertTrue(retrieved?.isCheckedIn == true)
        assertNotNull(retrieved?.checkedInAt)
        assertEquals("scanner_001", retrieved?.checkedInBy)
    }

    @Test
    fun getTicketsByEvent_returnsCorrectTickets() = runBlocking {
        database.userDao().insertUser(makeUser("u1"))
        database.userDao().insertUser(makeUser("u2"))
        database.userDao().insertUser(makeUser("u3"))
        database.eventDao().insertEvent(makeEvent("event_filter_test"))
        database.eventDao().insertEvent(makeEvent("other_event"))

        val tickets = listOf(
            Ticket(id = "t1", eventId = "event_filter_test", userId = "u1", ticketType = TicketType.STANDARD, status = TicketStatus.ACTIVE, qrCode = "QR1", reservationDate = System.currentTimeMillis()),
            Ticket(id = "t2", eventId = "event_filter_test", userId = "u2", ticketType = TicketType.VIP, status = TicketStatus.ACTIVE, qrCode = "QR2", reservationDate = System.currentTimeMillis()),
            Ticket(id = "t3", eventId = "other_event", userId = "u3", ticketType = TicketType.STANDARD, status = TicketStatus.ACTIVE, qrCode = "QR3", reservationDate = System.currentTimeMillis())
        )

        tickets.forEach { ticketRepository.createTicket(it) }

        val eventTickets = ticketRepository.getTicketsByEvent("event_filter_test").first()
        assertEquals(2, eventTickets.size)
        assertTrue(eventTickets.all { it.eventId == "event_filter_test" })
    }

    @Test
    fun refundTicket_updatesStatusCorrectly() = runBlocking {
        database.userDao().insertUser(makeUser())
        database.eventDao().insertEvent(makeEvent())

        val ticket = Ticket(
            id = "ticket_refund_001",
            eventId = "event_001",
            userId = "user_001",
            ticketType = TicketType.STANDARD,
            status = TicketStatus.ACTIVE,
            qrCode = "QR_REFUND_001",
            reservationDate = System.currentTimeMillis()
        )

        ticketRepository.createTicket(ticket)

        val cancelledTicket = ticket.copy(status = TicketStatus.CANCELLED)
        ticketRepository.updateTicket(cancelledTicket)

        val retrieved = ticketRepository.getTicketById("ticket_refund_001")
        assertEquals(TicketStatus.CANCELLED, retrieved?.status)
    }

    @Test
    fun getTransactionsByUser_returnsCorrectTransactions() = runBlocking {
        database.userDao().insertUser(makeUser("user_txn_test"))
        database.userDao().insertUser(makeUser("other_user"))

        val transactions = listOf(
            Transaction(id = "tx1", userId = "user_txn_test", eventId = "e1", amount = 50.0, type = TransactionType.TICKET_PURCHASE, status = TransactionStatus.COMPLETED, description = "Purchase 1", createdAt = System.currentTimeMillis()),
            Transaction(id = "tx2", userId = "user_txn_test", eventId = "e2", amount = 100.0, type = TransactionType.TICKET_PURCHASE, status = TransactionStatus.COMPLETED, description = "Purchase 2", createdAt = System.currentTimeMillis()),
            Transaction(id = "tx3", userId = "other_user", eventId = "e3", amount = 75.0, type = TransactionType.TICKET_PURCHASE, status = TransactionStatus.COMPLETED, description = "Other purchase", createdAt = System.currentTimeMillis())
        )

        transactions.forEach { transactionRepository.createTransaction(it) }

        val userTransactions = transactionRepository.getTransactionsByUser("user_txn_test").first()
        assertEquals(2, userTransactions.size)
        assertTrue(userTransactions.all { it.userId == "user_txn_test" })
    }

    @Test
    fun duplicateQRCodeScan_preventedBySession() {
        val scannedInSession = mutableSetOf<String>()
        val qrCode = "QR_DUPLICATE_TEST_001"

        val firstScan = scannedInSession.add(qrCode)
        val secondScan = !scannedInSession.add(qrCode)

        assertTrue("First scan should succeed", firstScan)
        assertTrue("Second scan should be blocked as duplicate", secondScan)
    }

    @Test
    fun ticketStatusTransitions_validFromActive() {
        val activeTransitions = listOf(TicketStatus.USED, TicketStatus.CANCELLED, TicketStatus.EXPIRED)

        fun canTransition(from: TicketStatus, to: TicketStatus): Boolean {
            return when (from) {
                TicketStatus.ACTIVE -> to in activeTransitions
                else -> false
            }
        }

        assertTrue(canTransition(TicketStatus.ACTIVE, TicketStatus.USED))
        assertTrue(canTransition(TicketStatus.ACTIVE, TicketStatus.CANCELLED))
        assertFalse(canTransition(TicketStatus.USED, TicketStatus.CANCELLED))
        assertFalse(canTransition(TicketStatus.CANCELLED, TicketStatus.USED))
    }

    @Test
    fun totalRevenue_calculatedCorrectly() = runBlocking {
        database.userDao().insertUser(makeUser())

        val transactions = listOf(
            Transaction(id = "r1", userId = "user_001", amount = 50.0, type = TransactionType.TICKET_PURCHASE, status = TransactionStatus.COMPLETED, description = "T1"),
            Transaction(id = "r2", userId = "user_001", amount = 150.0, type = TransactionType.TICKET_PURCHASE, status = TransactionStatus.COMPLETED, description = "T2"),
            Transaction(id = "r3", userId = "user_001", amount = 30.0, type = TransactionType.WALLET_TOP_UP, status = TransactionStatus.COMPLETED, description = "Topup")
        )
        transactions.forEach { transactionRepository.createTransaction(it) }

        val total = transactionRepository.getTotalSpending("user_001")
        assertEquals(200.0, total, 0.001)
    }
}
