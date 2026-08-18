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

    // =====================================================
    // CREATE BACKUP
    // =====================================================

    suspend fun createBackup(): String {

        val customers =
            database.customerDao().getAllCustomersOnce()

        val transactions =
            database.transactionDao().getAllTransactionsOnce()

        val backupData = BackupData(
            customers = customers,
            transactions = transactions
        )

        return json.encodeToString(
            BackupData.serializer(),
            backupData
        )
    }


    // =====================================================
    // SAVE BACKUP TO APP STORAGE
    // =====================================================

    suspend fun saveBackupToFile(): String {

        val backupJson = createBackup()

        val fileName =
            "AJ_UdharBook_Backup_${System.currentTimeMillis()}.json"

        val file = java.io.File(
            context.getExternalFilesDir(null),
            fileName
        )

        file.writeText(backupJson)

        return file.absolutePath
    }


    // =====================================================
    // SAVE JSON TO USER SELECTED FILE
    // =====================================================

    suspend fun saveJsonToUri(
        uri: Uri,
        jsonText: String
    ) {

        context.contentResolver
            .openOutputStream(uri)
            ?.use { outputStream ->

                outputStream.write(
                    jsonText.toByteArray(Charsets.UTF_8)
                )

                outputStream.flush()
            }
            ?: throw IllegalStateException(
                "Backup file open नहीं हो सकी।"
            )
    }


    // =====================================================
    // READ BACKUP FROM USER SELECTED FILE
    // =====================================================

    suspend fun readBackupFromUri(
        uri: Uri
    ): BackupData {

        val jsonText =
            context.contentResolver
                .openInputStream(uri)
                ?.bufferedReader()
                ?.use { reader ->
                    reader.readText()
                }
                ?: throw IllegalStateException(
                    "Backup file read नहीं हो सकी।"
                )

        return json.decodeFromString(
            BackupData.serializer(),
            jsonText
        )
    }


    // =====================================================
    // RESTORE BACKUP
    // =====================================================

    suspend fun restoreBackup(
        backupData: BackupData,
        replaceExisting: Boolean = false
    ) {

        database.withTransaction {

            // -------------------------------------------------
            // Optional: Remove Existing Data
            // -------------------------------------------------

            if (replaceExisting) {

                // पहले transactions हटाएँ क्योंकि
                // उनका foreign key customer से जुड़ा है।
                database.transactionDao().deleteAll()

                database.customerDao().deleteAll()
            }


            // -------------------------------------------------
            // Old Customer ID -> New Customer ID
            // -------------------------------------------------

            val customerIdMap =
                mutableMapOf<Int, Int>()


            // -------------------------------------------------
            // Restore Customers
            // -------------------------------------------------

            backupData.customers.forEach { oldCustomer ->

                // ID = 0 रखने से Room नया ID generate करेगा
                val newCustomer =
                    oldCustomer.copy(
                        id = 0
                    )

                database.customerDao().insert(
                    newCustomer
                )

                // अभी inserted customer को उसके
                // original ID से पढ़कर mapping बनाना है।
            }


            // -------------------------------------------------
            // Current Customers पढ़ें
            // -------------------------------------------------

            val restoredCustomers =
                database.customerDao()
                    .getAllCustomersOnce()


            // -------------------------------------------------
            // Customer Mapping
            // -------------------------------------------------

            backupData.customers.forEach { oldCustomer ->

                val matchingCustomer =
                    restoredCustomers.firstOrNull {

                        it.name == oldCustomer.name &&
                                it.mobile == oldCustomer.mobile &&
                                it.address == oldCustomer.address
                    }

                if (matchingCustomer != null) {

                    customerIdMap[
                        oldCustomer.id
                    ] = matchingCustomer.id
                }
            }


            // -------------------------------------------------
            // Restore Transactions
            // -------------------------------------------------

            backupData.transactions.forEach { oldTransaction ->

                val newCustomerId =
                    customerIdMap[
                        oldTransaction.customerId
                    ]

                if (newCustomerId != null) {

                    val newTransaction =
                        oldTransaction.copy(
                            id = 0,
                            customerId = newCustomerId
                        )

                    database.transactionDao().insert(
                        newTransaction
                    )
                }
            }
        }
    }
}