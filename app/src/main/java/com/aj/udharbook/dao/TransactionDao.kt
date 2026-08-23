package com.aj.udharbook.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aj.udharbook.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query(
        "SELECT * FROM transactions " +
                "WHERE customerId = :customerId " +
                "ORDER BY timestamp DESC"
    )
    fun getTransactionsByCustomer(
        customerId: Int
    ): Flow<List<Transaction>>

    @Query(
        "SELECT * FROM transactions " +
                "ORDER BY timestamp DESC"
    )
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query(
        "SELECT * FROM transactions " +
                "ORDER BY timestamp DESC"
    )
    suspend fun getAllTransactionsOnce(): List<Transaction>

    @Query(
        """
        SELECT COALESCE(
            SUM(
                CASE
                    WHEN UPPER(type) = 'UDHAR' THEN amount
                    WHEN UPPER(type) = 'PAYMENT' THEN -amount
                    ELSE 0
                END
            ),
            0
        )
        FROM transactions
        WHERE customerId = :customerId
        """
    )
    suspend fun getCustomerBalance(
        customerId: Int
    ): Double

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteById(transactionId: Int)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
