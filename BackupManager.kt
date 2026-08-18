package com.aj.udharbook.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.aj.udharbook.database.AppDatabase
import kotlinx.serialization.json.Json

class BackupManager(
    private val context: Context,
    private val database: AppDatabase
) {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun createBackup(): String {
        val customers = database.customerDao().getAllCustomersOnce()
        val transactions = database.transactionDao().getAllTransactionsOnce()

        return json.encodeToString(
            BackupData.serializer(),
            BackupData(
                customers = customers,
                transactions = transactions
            )
        )
    }

    suspend fun saveBackupToFile(): String {
        val backupJson = createBackup()
        val fileName = "AJ_UdharBook_Backup_${System.currentTimeMillis()}.json"
        val file = java.io.File(context.getExternalFilesDir(null), fileName)
        file.writeText(backupJson)
        return file.absolutePath
    }

    suspend fun saveJsonToUri(uri: Uri, jsonText: String) {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(jsonText.toByteArray(Charsets.UTF_8))
            outputStream.flush()
        } ?: throw IllegalStateException("Backup file open नहीं हो सकी।")
    }

    suspend fun readBackupFromUri(uri: Uri): BackupData {
        val jsonText = context.contentResolver
            .openInputStream(uri)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: throw IllegalStateException("Backup file read नहीं हो सकी।")

        return json.decodeFromString(
            BackupData.serializer(),
            jsonText
        )
    }

    /**
     * Restores the exact customer and transaction IDs from the backup.
     * This is important because transactions reference customers by customerId.
     */
    suspend fun restoreBackup(
        backupData: BackupData,
        replaceExisting: Boolean = false
    ) {
        database.withTransaction {
            if (replaceExisting) {
                database.transactionDao().deleteAll()
                database.customerDao().deleteAll()
            }

            // Keep original IDs. CustomerDao uses REPLACE, so this is safe
            // for a restore and avoids unreliable name/mobile matching.
            if (backupData.customers.isNotEmpty()) {
                database.customerDao().insertAll(backupData.customers)
            }

            if (backupData.transactions.isNotEmpty()) {
                val validCustomerIds = database.customerDao()
                    .getAllCustomersOnce()
                    .map { it.id }
                    .toHashSet()

                val validTransactions = backupData.transactions.filter {
                    it.customerId in validCustomerIds
                }

                if (validTransactions.isNotEmpty()) {
                    database.transactionDao().insertAll(validTransactions)
                }
            }
        }
    }
}
