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
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao

    abstract fun transactionDao(): TransactionDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration 1 -> 2
         *
         * Old database:
         * createdAt
         * no foreign key
         *
         * New database:
         * timestamp
         * customerId -> customers.id
         * ON DELETE CASCADE
         */
        private val MIGRATION_1_2 =
            object : Migration(1, 2) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    // 1. Rename old table
                    database.execSQL(
                        "ALTER TABLE transactions " +
                                "RENAME TO transactions_old"
                    )

                    // 2. Create new table matching Room schema
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

                    // 3. Copy old data
                    // createdAt -> timestamp
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

                    // 4. Delete old table
                    database.execSQL(
                        "DROP TABLE transactions_old"
                    )
                }
            }

        fun getDatabase(
            context: Context
        ): AppDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance =
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "aj_udhar_book_db"
                    )
                        .addMigrations(
                            MIGRATION_1_2
                        )
                        .build()

                INSTANCE = instance

                instance
            }
        }
    }
}