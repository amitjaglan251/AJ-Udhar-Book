package com.aj.udharbook.sync

import android.util.Log
import com.aj.udharbook.dao.CustomerDao
import com.aj.udharbook.dao.TransactionDao
import com.aj.udharbook.model.Customer
import com.aj.udharbook.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreSyncManager(
    private val customerDao: CustomerDao,
    private val transactionDao: TransactionDao
) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    fun isUserSignedIn(): Boolean = auth.currentUser != null

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    private fun userDocument() = auth.currentUser?.uid?.let { uid ->
        firestore.collection("users").document(uid)
    }

    private fun requireUserDocument() = userDocument()
        ?: throw IllegalStateException("User is not signed in")

    suspend fun syncCustomer(customer: Customer) {
        if (customer.id <= 0) return
        val data = hashMapOf<String, Any>(
            "id" to customer.id,
            "name" to customer.name,
            "mobile" to customer.mobile,
            "address" to customer.address,
            "createdAt" to customer.createdAt
        )
        requireUserDocument().collection("customers")
            .document(customer.id.toString()).set(data).await()
    }

    suspend fun deleteCustomer(customerId: Int) {
        if (customerId <= 0) return
        requireUserDocument().collection("customers")
            .document(customerId.toString()).delete().await()
    }

    suspend fun syncTransaction(transaction: Transaction) {
        if (transaction.id <= 0) return
        val data = hashMapOf<String, Any>(
            "id" to transaction.id,
            "customerId" to transaction.customerId,
            "amount" to transaction.amount,
            "type" to transaction.type,
            "note" to transaction.note,
            "timestamp" to transaction.timestamp
        )
        requireUserDocument().collection("transactions")
            .document(transaction.id.toString()).set(data).await()
    }

    suspend fun deleteTransaction(transactionId: Int) {
        if (transactionId <= 0) return
        requireUserDocument().collection("transactions")
            .document(transactionId.toString()).delete().await()
    }

    suspend fun syncCustomers(customers: List<Customer>) {
        if (!isUserSignedIn()) return
        customers.filter { it.id > 0 }.forEach { syncCustomer(it) }
    }

    suspend fun syncTransactions(transactions: List<Transaction>) {
        if (!isUserSignedIn()) return
        transactions.filter { it.id > 0 }.forEach { syncTransaction(it) }
    }

    suspend fun syncAll(
        customers: List<Customer>,
        transactions: List<Transaction>
    ) {
        if (!isUserSignedIn()) return
        syncCustomers(customers)
        syncTransactions(transactions)
    }

    suspend fun clearLocalData() {
        transactionDao.deleteAll()
        customerDao.deleteAll()
    }

    suspend fun restoreCloudToLocal() {
        if (!isUserSignedIn()) {
            throw IllegalStateException("User is not signed in")
        }

        val localCustomers = customerDao.getAllCustomersOnce()
        val localTransactions = transactionDao.getAllTransactionsOnce()

        Log.d(
            "AJ_RESTORE",
            "Start: localCustomers=${localCustomers.size}, localTransactions=${localTransactions.size}"
        )

        // Never lose existing local data: back it up before cloud restore.
        if (localCustomers.isNotEmpty() || localTransactions.isNotEmpty()) {
            try {
                syncAll(localCustomers, localTransactions)
                Log.d("AJ_RESTORE", "Local data backed up to Firestore")
            } catch (e: Exception) {
                Log.e("AJ_RESTORE", "Local backup failed", e)
            }
        }

        val user = requireUserDocument()

        val customerSnapshot =
            user.collection("customers").get().await()

        val cloudCustomers = customerSnapshot.documents.mapNotNull { document ->
            val id = document.getLong("id")?.toInt()
                ?: document.id.toIntOrNull()
                ?: return@mapNotNull null

            Customer(
                id = id,
                name = document.getString("name") ?: "",
                mobile = document.getString("mobile") ?: "",
                address = document.getString("address") ?: "",
                createdAt = document.getLong("createdAt")
                    ?: System.currentTimeMillis()
            )
        }

        Log.d(
            "AJ_RESTORE",
            "Cloud customers=${customerSnapshot.size()}, parsed=${cloudCustomers.size}"
        )

        val transactionSnapshot =
            user.collection("transactions").get().await()

        val cloudTransactions = transactionSnapshot.documents.mapNotNull { document ->
            val id = document.getLong("id")?.toInt()
                ?: document.id.toIntOrNull()
                ?: return@mapNotNull null

            val customerId = document.getLong("customerId")?.toInt()
                ?: return@mapNotNull null

            Transaction(
                id = id,
                customerId = customerId,
                amount = document.getDouble("amount")
                    ?: document.getLong("amount")?.toDouble()
                    ?: 0.0,
                type = document.getString("type") ?: "UDHAR",
                note = document.getString("note") ?: "",
                timestamp = document.getLong("timestamp")
                    ?: System.currentTimeMillis()
            )
        }

        Log.d(
            "AJ_RESTORE",
            "Cloud transactions=${transactionSnapshot.size()}, parsed=${cloudTransactions.size}"
        )

        // Customers MUST be restored first because Transaction.customerId
        // has a Room foreign-key relationship with Customer.id.
        if (localCustomers.isEmpty() && cloudCustomers.isNotEmpty()) {
            try {
                customerDao.insertAll(cloudCustomers)
                Log.d("AJ_RESTORE", "Inserted customers=${cloudCustomers.size}")
            } catch (e: Exception) {
                Log.e("AJ_RESTORE", "Customer restore failed", e)
            }
        }

        if (localTransactions.isEmpty() && cloudTransactions.isNotEmpty()) {
            val availableCustomerIds = customerDao
                .getAllCustomersOnce()
                .asSequence()
                .map { it.id }
                .toSet()

            val validTransactions = cloudTransactions.filter {
                it.customerId in availableCustomerIds
            }

            val skippedOrphans =
                cloudTransactions.size - validTransactions.size

            Log.d(
                "AJ_RESTORE",
                "Transaction validation: availableCustomers=${availableCustomerIds.size}, " +
                        "valid=${validTransactions.size}, skippedOrphans=$skippedOrphans"
            )

            // Insert one-by-one so a single stale/orphan transaction cannot
            // roll back the complete history batch.
            for (transaction in validTransactions) {
                try {
                    transactionDao.insert(transaction)
                    Log.d(
                        "AJ_RESTORE",
                        "Transaction restored: id=${transaction.id}, customerId=${transaction.customerId}"
                    )
                } catch (e: Exception) {
                    Log.e(
                        "AJ_RESTORE",
                        "Transaction restore failed: id=${transaction.id}, customerId=${transaction.customerId}",
                        e
                    )
                }
            }
        }

        val finalCustomers = customerDao.getAllCustomersOnce()
        val finalTransactions = transactionDao.getAllTransactionsOnce()

        Log.d(
            "AJ_RESTORE",
            "Complete: finalCustomers=${finalCustomers.size}, finalTransactions=${finalTransactions.size}"
        )

        try {
            syncAll(finalCustomers, finalTransactions)
            Log.d("AJ_RESTORE", "Final local state synced to Firestore")
        } catch (e: Exception) {
            Log.e("AJ_RESTORE", "Final sync failed", e)
        }
    }
}
