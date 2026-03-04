package com.example.eventpay.data.local

import androidx.room.TypeConverter
import com.example.eventpay.data.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    
    // UserRole converter
    @TypeConverter
    fun fromUserRole(role: UserRole): String = role.name
    
    @TypeConverter
    fun toUserRole(value: String): UserRole = try {
        UserRole.valueOf(value)
    } catch (e: IllegalArgumentException) {
        UserRole.SCANNER
    }
    
    // UserPreferences converter
    @TypeConverter
    fun fromUserPreferences(prefs: UserPreferences): String = Gson().toJson(prefs)
    
    @TypeConverter
    fun toUserPreferences(value: String): UserPreferences = try {
        Gson().fromJson(value, UserPreferences::class.java)
    } catch (e: Exception) {
        UserPreferences()
    }
    
    // EventCategory converter
    @TypeConverter
    fun fromEventCategory(category: EventCategory): String = category.name
    
    @TypeConverter
    fun toEventCategory(value: String): EventCategory = try {
        EventCategory.valueOf(value)
    } catch (e: IllegalArgumentException) {
        EventCategory.GENERAL
    }
    
    // EventStatus converter
    @TypeConverter
    fun fromEventStatus(status: EventStatus): String = status.name
    
    @TypeConverter
    fun toEventStatus(value: String): EventStatus = try {
        EventStatus.valueOf(value)
    } catch (e: IllegalArgumentException) {
        EventStatus.DRAFT
    }
    
    // List<String> converter for tags
    @TypeConverter
    fun fromStringList(list: List<String>): String = Gson().toJson(list)
    
    @TypeConverter
    fun toStringList(value: String): List<String> = try {
        val type = object : TypeToken<List<String>>() {}.type
        Gson().fromJson(value, type) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
    
    // TicketType converter
    @TypeConverter
    fun fromTicketType(type: TicketType): String = type.name
    
    @TypeConverter
    fun toTicketType(value: String): TicketType = try {
        TicketType.valueOf(value)
    } catch (e: IllegalArgumentException) {
        TicketType.STANDARD
    }
    
    // TicketStatus converter
    @TypeConverter
    fun fromTicketStatus(status: TicketStatus): String = status.name
    
    @TypeConverter
    fun toTicketStatus(value: String): TicketStatus = try {
        TicketStatus.valueOf(value)
    } catch (e: IllegalArgumentException) {
        TicketStatus.ACTIVE
    }
    
    // TransactionType converter
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name
    
    @TypeConverter
    fun toTransactionType(value: String): TransactionType = try {
        TransactionType.valueOf(value)
    } catch (e: IllegalArgumentException) {
        TransactionType.TICKET_PURCHASE
    }
    
    // PaymentMethod converter
    @TypeConverter
    fun fromPaymentMethod(method: PaymentMethod): String = method.name
    
    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod = try {
        PaymentMethod.valueOf(value)
    } catch (e: IllegalArgumentException) {
        PaymentMethod.WALLET
    }
    
    // TransactionStatus converter
    @TypeConverter
    fun fromTransactionStatus(status: TransactionStatus): String = status.name
    
    @TypeConverter
    fun toTransactionStatus(value: String): TransactionStatus = try {
        TransactionStatus.valueOf(value)
    } catch (e: IllegalArgumentException) {
        TransactionStatus.PENDING
    }
}
