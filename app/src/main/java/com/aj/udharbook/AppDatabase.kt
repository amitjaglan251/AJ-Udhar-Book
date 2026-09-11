package com.aj.udharbook.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aj.udharbook.dao.CustomerDao
import com.aj.udharbook.dao.TransactionDao
import com.aj.udharbook.model.Customer
import com.aj.udharbook.model.Transaction

@Database(
    entities = [
        Customer::class,
        Transaction::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao

    abstract fun transactionDao(): TransactionDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL(
                        "ALTER TABLE transactions RENAME TO transactions_old"
                    )

                    database.execSQL(
                        """
                        CREATE TABLE transactions (
                            id INTEGER NOT NULL,
                            customerId INTEGER NOT NULL,
                            amount REAL NOT NULL,
                            type TEXT NOT NULL,
                            note TEXT NOT NULL,
                            timestamp INTEGER NOT NULL,
                            PRIMARY KEY(id),
                            FOREIGN KEY(customerId)
                                REFERENCES customers(id)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        INSERT INTO transactions (
                            id,
                            customerId,
                            amount,
                            type,
                            note,
                            timestamp
                        )
                        SELECT
                            id,
                            customerId,
                            amount,
                            type,
                            note,
                            createdAt
                        FROM transactions_old
                        """.trimIndent()
                    )

                    database.execSQL("DROP TABLE transactions_old")
                }
            }

        /**
         * Migration 2 -> 3 restores the shared-ledger columns used by the
         * previous working version of AJ Udhar Book.
         *
         * Existing v2 customer and transaction data is preserved.
         */
        private val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL(
                        "ALTER TABLE customers ADD COLUMN sharedLedgerId TEXT NOT NULL DEFAULT ''"
                    )
                    database.execSQL(
                        "ALTER TABLE transactions ADD COLUMN syncKey TEXT NOT NULL DEFAULT ''"
                    )
                }
            }

        fun getDatabase(
            context: Context
        ): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aj_udhar_book_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
