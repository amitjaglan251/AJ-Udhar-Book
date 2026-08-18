package com.aj.udharbook.repository

import com.aj.udharbook.dao.TransactionDao
import com.aj.udharbook.model.Transaction
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val transactionDao: TransactionDao
) {

    // ==================================================
    // LIVE TRANSACTION LIST
    // ==================================================

    val allTransactions =
        transactionDao.getAllTransactions()


    // ==================================================
    // INSERT TRANSACTION
    // ==================================================

    suspend fun insert(
        transaction: Transaction
    ): Long {

        return transactionDao.insert(transaction)
    }


    // ==================================================
    // UPDATE TRANSACTION
    // ==================================================

    suspend fun update(
        transaction: Transaction
    ) {

        transactionDao.update(transaction)
    }


    // ==================================================
    // DELETE TRANSACTION
    // ==================================================

    suspend fun delete(
        transaction: Transaction
    ) {

        transactionDao.delete(transaction)
    }


    // ==================================================
    // GET CUSTOMER TRANSACTIONS
    // ==================================================

    fun getTransactionsByCustomer(
        customerId: Int
    ): Flow<List<Transaction>> {

        return transactionDao
            .getTransactionsByCustomer(customerId)
    }


    // ==================================================
    // GET CUSTOMER BALANCE
    // ==================================================

    suspend fun getCustomerBalance(
        customerId: Int
    ): Double {

        return transactionDao
            .getCustomerBalance(customerId)
    }


    // ==================================================
    // GET ALL TRANSACTIONS ONCE
    // ==================================================

    suspend fun getAllTransactionsOnce(): List<Transaction> {

        return transactionDao
            .getAllTransactionsOnce()
    }
}