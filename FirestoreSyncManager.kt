package com.aj.udharbook.sync

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

    private val auth =
        FirebaseAuth.getInstance()

    private val firestore =
        FirebaseFirestore.getInstance()

    // ==================================================
    // CURRENT USER
    // ==================================================

    fun isUserSignedIn(): Boolean {
        return auth.currentUser != null
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    // ==================================================
    // USER DOCUMENT
    // ==================================================

    private fun userDocument() =
        auth.currentUser?.uid?.let { uid ->
            firestore
                .collection("users")
                .document(uid)
        }

    private fun requireUserDocument() =
        userDocument()
            ?: throw IllegalStateException(
                "User is not signed in"
            )

    // ==================================================
    // CUSTOMER SYNC
    // LOCAL → FIRESTORE
    // ==================================================

    suspend fun syncCustomer(
        customer: Customer
    ) {
        if (customer.id <= 0) return

        val user =
            requireUserDocument()

        val data =
            hashMapOf<String, Any>(
                "id" to customer.id,
                "name" to customer.name,
                "mobile" to customer.mobile,
                "address" to customer.address,
                "createdAt" to customer.createdAt
            )

        user
            .collection("customers")
            .document(
                customer.id.toString()
            )
            .set(data)
            .await()
    }

    // ==================================================
    // CUSTOMER DELETE
    // ==================================================

    suspend fun deleteCustomer(
        customerId: Int
    ) {
        if (customerId <= 0) return

        val user =
            requireUserDocument()

        user
            .collection("customers")
            .document(
                customerId.toString()
            )
            .delete()
            .await()
    }

    // ==================================================
    // TRANSACTION SYNC
    // LOCAL → FIRESTORE
    // ==================================================

    suspend fun syncTransaction(
        transaction: Transaction
    ) {
        if (transaction.id <= 0) return

        val user =
            requireUserDocument()

        val data =
            hashMapOf<String, Any>(
                "id" to transaction.id,
                "customerId" to transaction.customerId,
                "amount" to transaction.amount,
                "type" to transaction.type,
                "note" to transaction.note,
                "timestamp" to transaction.timestamp
            )

        user
            .collection("transactions")
            .document(
                transaction.id.toString()
            )
            .set(data)
            .await()
    }

    // ==================================================
    // TRANSACTION DELETE
    // ==================================================

    suspend fun deleteTransaction(
        transactionId: Int
    ) {
        if (transactionId <= 0) return

        val user =
            requireUserDocument()

        user
            .collection("transactions")
            .document(
                transactionId.toString()
            )
            .delete()
            .await()
    }

    // ==================================================
    // SYNC CUSTOMERS
    // ==================================================

    suspend fun syncCustomers(
        customers: List<Customer>
    ) {
        if (!isUserSignedIn()) return

        for (customer in customers) {
            if (customer.id > 0) {
                syncCustomer(customer)
            }
        }
    }

    // ==================================================
    // SYNC TRANSACTIONS
    // ==================================================

    suspend fun syncTransactions(
        transactions: List<Transaction>
    ) {
        if (!isUserSignedIn()) return

        for (transaction in transactions) {
            if (transaction.id > 0) {
                syncTransaction(transaction)
            }
        }
    }

    // ==================================================
    // SYNC ALL
    // LOCAL → FIRESTORE
    // ==================================================

    suspend fun syncAll(
        customers: List<Customer>,
        transactions: List<Transaction>
    ) {
        if (!isUserSignedIn()) return

        syncCustomers(customers)
        syncTransactions(transactions)
    }

    // ==================================================
    // CLEAR LOCAL DATA
    // ==================================================

    suspend fun clearLocalData() {
        // First delete transactions as they depend on customers
        transactionDao.deleteAll()
        // Then delete customers
        customerDao.deleteAll()
    }

    // ==================================================
    // RESTORE CLOUD DATA
    // FIRESTORE → ROOM
    // ==================================================

    suspend fun restoreCloudToLocal() {
        if (!isUserSignedIn()) {
            throw IllegalStateException(
                "User is not signed in"
            )
        }

        val user =
            requireUserDocument()

        // GET CUSTOMERS FROM FIRESTORE
        val customerSnapshot =
            user
                .collection("customers")
                .get()
                .await()

        val cloudCustomers =
            customerSnapshot.documents.mapNotNull { document ->
                val id =
                    document.getLong("id")
                        ?.toInt()
                        ?: document.id.toIntOrNull()
                        ?: return@mapNotNull null

                val name =
                    document.getString("name") ?: ""

                val mobile =
                    document.getString("mobile") ?: ""

                val address =
                    document.getString("address") ?: ""

                val createdAt =
                    document.getLong("createdAt")
                        ?: System.currentTimeMillis()

                Customer(
                    id = id,
                    name = name,
                    mobile = mobile,
                    address = address,
                    createdAt = createdAt
                )
            }

        // GET TRANSACTIONS FROM FIRESTORE
        val transactionSnapshot =
            user
                .collection("transactions")
                .get()
                .await()

        val cloudTransactions =
            transactionSnapshot.documents.mapNotNull { document ->
                val id =
                    document.getLong("id")
                        ?.toInt()
                        ?: document.id.toIntOrNull()
                        ?: return@mapNotNull null

                val customerId =
                    document.getLong("customerId")
                        ?.toInt()
                        ?: return@mapNotNull null

                val amount =
                    document.getDouble("amount")
                        ?: document.getLong("amount")?.toDouble()
                        ?: 0.0

                val type =
                    document.getString("type") ?: "UDHAR"

                val note =
                    document.getString("note") ?: ""

                val timestamp =
                    document.getLong("timestamp")
                        ?: System.currentTimeMillis()

                Transaction(
                    id = id,
                    customerId = customerId,
                    amount = amount,
                    type = type,
                    note = note,
                    timestamp = timestamp
                )
            }

        // CHECK LOCAL ROOM DATA
        val localCustomers =
            customerDao.getAllCustomersOnce()

        val localTransactions =
            transactionDao.getAllTransactionsOnce()

        // RESTORE CUSTOMERS
        if (localCustomers.isEmpty() && cloudCustomers.isNotEmpty()) {
            customerDao.insertAll(cloudCustomers)
        }

        // RESTORE TRANSACTIONS
        if (localTransactions.isEmpty() && cloudTransactions.isNotEmpty()) {
            transactionDao.insertAll(cloudTransactions)
        }
    }
}
