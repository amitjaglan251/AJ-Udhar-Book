package com.aj.udharbook.backup

import com.aj.udharbook.dao.CustomerDao
import com.aj.udharbook.dao.TransactionDao

class BackupRepository(
    private val customerDao: CustomerDao,
    private val transactionDao: TransactionDao
) {

    suspend fun createBackup(): BackupData {

        val customers =
            customerDao.getAllCustomersOnce()

        val transactions =
            transactionDao.getAllTransactionsOnce()

        return BackupData(
            customers = customers,
            transactions = transactions
        )
    }

    suspend fun restoreBackup(
        backupData: BackupData
    ) {

        transactionDao.deleteAll()

        customerDao.deleteAll()

        if (backupData.customers.isNotEmpty()) {
            customerDao.insertAll(
                backupData.customers
            )
        }

        if (backupData.transactions.isNotEmpty()) {
            transactionDao.insertAll(
                backupData.transactions
            )
        }
    }
}