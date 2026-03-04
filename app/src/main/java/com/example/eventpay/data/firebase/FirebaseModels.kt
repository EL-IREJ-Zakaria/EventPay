package com.example.eventpay.data.firebase

import com.example.eventpay.domain.model.CheckInResult

data class DashboardStats(
    val totalEvents: Int = 0,
    val totalTickets: Int = 0,
    val totalCheckIns: Int = 0,
    val totalScanners: Int = 0
)

data class CheckInRecord(
    val id: String = "",
    val ticketId: String = "",
    val eventId: String = "",
    val scannedBy: String = "",
    val scannedByName: String = "",
    val scannedAt: Long = 0,
    val deviceId: String? = null,
    val result: CheckInResult = CheckInResult.SUCCESS
)
