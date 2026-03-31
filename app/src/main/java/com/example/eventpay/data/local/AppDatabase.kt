package com.example.eventpay.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.eventpay.data.local.dao.EventDao
import com.example.eventpay.data.local.dao.TicketDao
import com.example.eventpay.data.local.dao.TransactionDao
import com.example.eventpay.data.local.dao.UserDao
import com.example.eventpay.data.model.Event
import com.example.eventpay.data.model.Ticket
import com.example.eventpay.data.model.Transaction
import com.example.eventpay.data.model.User

@Database(
    entities = [User::class, Event::class, Ticket::class, Transaction::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun eventDao(): EventDao
    abstract fun ticketDao(): TicketDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No schema changes between v5 and v6 — safe no-op migration
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "eventpay_database"
                )
                    .addMigrations(MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
