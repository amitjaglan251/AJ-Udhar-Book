package com.aj.udharbook.sync

import com.aj.udharbook.dao.CustomerDao
import com.aj.udharbook.dao.TransactionDao
import com.aj.udharbook.model.Customer
import com.aj.udharbook.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Cloud backup/sync for the normal AJ Udhar Book customer and transaction data.
 * Shared Ledger / join-request functionality has been intentionally removed.
 */
class FirestoreSyncManager(
    private val customerDao: CustomerDao,
    private val transactionDao: TransactionDao
) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    fun isUserSignedIn(): Boolean = auth.currentUser != null
    fun getCurrentUserId(): String? = auth.currentUser?.uid
    suspend fun getCustomerByIdOnce(customerId: Int): Customer? = customerDao.getCustomerByIdOnce(customerId)

    private fun userDocument() = auth.currentUser?.uid?.let { uid ->
        firestore.collection("users").document(uid)
    }

    private fun requireUserDocument() =
        userDocument() ?: throw IllegalStateException("User is not signed in")

    suspend fun syncCustomer(customer: Customer) {
        if (customer.id <= 0 || !isUserSignedIn()) return
        requireUserDocument().collection("customers").document(customer.id.toString()).set(
            hashMapOf<String, Any>(
                "id" to customer.id,
                "name" to customer.name,
                "mobile" to customer.mobile,
                "address" to customer.address,
                "createdAt" to customer.createdAt
            )
        ).await()
    }

    suspend fun deleteCustomer(customerId: Int) {
        if (customerId <= 0 || !isUserSignedIn()) return
        requireUserDocument().collection("customers").document(customerId.toString()).delete().await()
    }

    suspend fun syncTransaction(transaction: Transaction) {
        if (transaction.id <= 0 || !isUserSignedIn()) return
        requireUserDocument().collection("transactions").document(transaction.id.toString()).set(
            hashMapOf<String, Any>(
                "id" to transaction.id,
                "customerId" to transaction.customerId,
                "amount" to transaction.amount,
                "type" to transaction.type,
                "note" to transaction.note,
                "timestamp" to transaction.timestamp,
                "syncKey" to transaction.syncKey
            )
        ).await()
    }

    suspend fun deleteTransaction(transactionId: Int) {
        if (transactionId <= 0 || !isUserSignedIn()) return
        requireUserDocument().collection("transactions").document(transactionId.toString()).delete().await()
    }

    suspend fun syncCustomers(customers: List<Customer>) {
        if (!isUserSignedIn()) return
        customers.filter { it.id > 0 }.forEach { syncCustomer(it) }
    }

    suspend fun syncTransactions(transactions: List<Transaction>) {
        if (!isUserSignedIn()) return
        transactions.filter { it.id > 0 }.forEach { syncTransaction(it) }
    }

    suspend fun syncAll(customers: List<Customer>, transactions: List<Transaction>) {
        if (!isUserSignedIn()) return
        syncCustomers(customers)
        syncTransactions(transactions)
    }

    suspend fun clearLocalData() {
        transactionDao.deleteAll()
        customerDao.deleteAll()
    }

    suspend fun restoreCloudToLocal() {
        if (!isUserSignedIn()) throw IllegalStateException("User is not signed in")
        val user = requireUserDocument()

        val customerSnapshot = user.collection("customers").get().await()
        val cloudCustomers = customerSnapshot.documents.mapNotNull { document ->
            val id = document.getLong("id")?.toInt() ?: document.id.toIntOrNull() ?: return@mapNotNull null
            Customer(
                id = id,
                name = document.getString("name") ?: "",
                mobile = document.getString("mobile") ?: "",
                address = document.getString("address") ?: "",
                createdAt = document.getLong("createdAt") ?: System.currentTimeMillis()
            )
        }

        val transactionSnapshot = user.collection("transactions").get().await()
        val cloudTransactions = transactionSnapshot.documents.mapNotNull { document ->
            val id = document.getLong("id")?.toInt() ?: document.id.toIntOrNull() ?: return@mapNotNull null
            val customerId = document.getLong("customerId")?.toInt() ?: return@mapNotNull null
            Transaction(
                id = id,
                customerId = customerId,
                amount = document.getDouble("amount") ?: document.getLong("amount")?.toDouble() ?: 0.0,
                type = document.getString("type") ?: "UDHAR",
                note = document.getString("note") ?: "",
                timestamp = document.getLong("timestamp") ?: System.currentTimeMillis(),
                syncKey = document.getString("syncKey") ?: ""
            )
        }

        if (customerDao.getAllCustomersOnce().isEmpty() && cloudCustomers.isNotEmpty()) {
            customerDao.insertAll(cloudCustomers)
        }
        if (transactionDao.getAllTransactionsOnce().isEmpty() && cloudTransactions.isNotEmpty()) {
            transactionDao.insertAll(cloudTransactions)
        }
    }
}
