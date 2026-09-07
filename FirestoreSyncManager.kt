package com.aj.udharbook.sync

import com.aj.udharbook.dao.CustomerDao
import com.aj.udharbook.dao.TransactionDao
import com.aj.udharbook.model.Customer
import com.aj.udharbook.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreSyncManager(
    private val customerDao: CustomerDao,
    private val transactionDao: TransactionDao
) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val sharedListeners = mutableMapOf<String, ListenerRegistration>()

    fun isUserSignedIn(): Boolean = auth.currentUser != null
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    private fun userDocument() = auth.currentUser?.uid?.let { uid ->
        firestore.collection("users").document(uid)
    }

    private fun requireUserDocument() = userDocument()
        ?: throw IllegalStateException("User is not signed in")

    suspend fun syncCustomer(customer: Customer) {
        if (customer.id <= 0) return
        requireUserDocument().collection("customers").document(customer.id.toString()).set(
            hashMapOf<String, Any>(
                "id" to customer.id,
                "name" to customer.name,
                "mobile" to customer.mobile,
                "address" to customer.address,
                "createdAt" to customer.createdAt,
                "sharedLedgerId" to customer.sharedLedgerId
            )
        ).await()
    }

    suspend fun deleteCustomer(customerId: Int) {
        if (customerId <= 0) return
        requireUserDocument().collection("customers").document(customerId.toString()).delete().await()
    }

    suspend fun syncTransaction(transaction: Transaction) {
        if (transaction.id <= 0) return
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
        if (transactionId <= 0) return
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
                createdAt = document.getLong("createdAt") ?: System.currentTimeMillis(),
                sharedLedgerId = document.getString("sharedLedgerId") ?: ""
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
        if (customerDao.getAllCustomersOnce().isEmpty() && cloudCustomers.isNotEmpty()) customerDao.insertAll(cloudCustomers)
        if (transactionDao.getAllTransactionsOnce().isEmpty() && cloudTransactions.isNotEmpty()) transactionDao.insertAll(cloudTransactions)
    }

    suspend fun createShareCode(customer: Customer): String {
        if (!isUserSignedIn()) throw IllegalStateException("Login required")
        val existing = customer.sharedLedgerId.trim()
        val code = if (existing.matches(Regex("\\d{6}"))) existing else findAvailableCode()
        val uid = getCurrentUserId() ?: throw IllegalStateException("Login required")
        val ledgerRef = firestore.collection("sharedLedgers").document(code)
        val existingLedger = ledgerRef.get().await()
        if (!existingLedger.exists()) {
            ledgerRef.set(
                hashMapOf<String, Any>(
                    "inviteCode" to code,
                    "ownerUid" to uid,
                    "ownerCustomerId" to customer.id,
                    "customerName" to customer.name,
                    "mobile" to customer.mobile.filter(Char::isDigit).takeLast(10),
                    "address" to customer.address,
                    "participantUids" to listOf(uid),
                    "createdAt" to System.currentTimeMillis()
                )
            ).await()
        }
        val updatedCustomer = customer.copy(sharedLedgerId = code)
        customerDao.update(updatedCustomer)
        syncCustomer(updatedCustomer)
        publishCustomerTransactions(updatedCustomer)
        return code
    }

    private suspend fun findAvailableCode(): String {
        repeat(30) {
            val code = SharedLedgerUtils.newInviteCode()
            if (!firestore.collection("sharedLedgers").document(code).get().await().exists()) return code
        }
        throw IllegalStateException("Could not create a share code. Please try again.")
    }

    private suspend fun publishCustomerTransactions(customer: Customer) {
        if (customer.sharedLedgerId.isBlank()) return
        transactionDao.getTransactionsByCustomerOnce(customer.id).forEach { transaction ->
            val syncKey = transaction.syncKey.ifBlank { "${auth.currentUser?.uid}:${transaction.id}:${transaction.timestamp}" }
            val shared = transaction.copy(syncKey = syncKey)
            if (shared.syncKey != transaction.syncKey) transactionDao.deleteBySyncKey(transaction.syncKey)
            firestore.collection("sharedLedgers").document(customer.sharedLedgerId)
                .collection("transactions").document(syncKey)
                .set(transactionMap(shared)).await()
        }
    }

    suspend fun joinShareCode(codeInput: String): Customer {
        if (!isUserSignedIn()) throw IllegalStateException("Login required")
        val code = codeInput.filter(Char::isDigit)
        if (!code.matches(Regex("\\d{6}"))) throw IllegalArgumentException("Enter a valid 6-digit code")
        val uid = getCurrentUserId() ?: throw IllegalStateException("Login required")
        val ledgerRef = firestore.collection("sharedLedgers").document(code)
        val snapshot = ledgerRef.get().await()
        if (!snapshot.exists()) throw IllegalArgumentException("Share code not found")
        val participants = (snapshot.get("participantUids") as? List<*>)?.filterIsInstance<String>()?.toMutableList() ?: mutableListOf()
        if (!participants.contains(uid)) participants.add(uid)
        ledgerRef.update("participantUids", participants).await()

        val name = snapshot.getString("customerName") ?: "Shared Customer"
        val mobile = snapshot.getString("mobile") ?: ""
        val address = snapshot.getString("address") ?: ""
        val ownerCustomerId = snapshot.getLong("ownerCustomerId")?.toInt() ?: 0
        var customer = customerDao.getCustomerBySharedLedgerOnce(code)
            ?: customerDao.getCustomerByMobileOnce(mobile)
        if (customer == null) {
            val localId = customerDao.insert(Customer(name = name, mobile = mobile, address = address, sharedLedgerId = code)).toInt()
            customer = customerDao.getCustomerByIdOnce(localId) ?: throw IllegalStateException("Could not create local customer")
        } else if (customer.sharedLedgerId != code) {
            customer = customer.copy(sharedLedgerId = code)
            customerDao.update(customer)
        }
        syncSharedLedgerToLocal(code, customer.id, ownerCustomerId)
        listenToSharedLedger(code, customer.id)
        return customer
    }

    suspend fun syncSharedLedgerTransaction(transaction: Transaction) {
        if (transaction.syncKey.isBlank()) return
        val customer = customerDao.getCustomerByIdOnce(transaction.customerId) ?: return
        val code = customer.sharedLedgerId.trim()
        if (!code.matches(Regex("\\d{6}")) || !isUserSignedIn()) return
        firestore.collection("sharedLedgers").document(code).collection("transactions")
            .document(transaction.syncKey).set(transactionMap(transaction)).await()
    }

    suspend fun deleteSharedLedgerTransaction(transaction: Transaction) {
        if (transaction.syncKey.isBlank() || !isUserSignedIn()) return
        val customer = customerDao.getCustomerByIdOnce(transaction.customerId) ?: return
        val code = customer.sharedLedgerId.trim()
        if (!code.matches(Regex("\\d{6}"))) return
        firestore.collection("sharedLedgers").document(code).collection("transactions")
            .document(transaction.syncKey).delete().await()
    }

    private fun transactionMap(transaction: Transaction): HashMap<String, Any> = hashMapOf(
        "syncKey" to transaction.syncKey,
        "customerId" to transaction.customerId,
        "amount" to transaction.amount,
        "type" to transaction.type,
        "note" to transaction.note,
        "timestamp" to transaction.timestamp
    )

    suspend fun syncSharedLedgerToLocal(code: String, localCustomerId: Int, ownerCustomerId: Int = 0) {
        val snapshot = firestore.collection("sharedLedgers").document(code).collection("transactions").get().await()
        val current = transactionDao.getTransactionsByCustomerOnce(localCustomerId).associateBy { it.syncKey }
        snapshot.documents.forEach { doc ->
            val syncKey = doc.getString("syncKey") ?: doc.id
            val transaction = Transaction(
                id = -(kotlin.math.abs(syncKey.hashCode()) + 1),
                customerId = localCustomerId,
                amount = doc.getDouble("amount") ?: doc.getLong("amount")?.toDouble() ?: 0.0,
                type = doc.getString("type") ?: "UDHAR",
                note = doc.getString("note") ?: "",
                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                syncKey = syncKey
            )
            if (!current.containsKey(syncKey)) transactionDao.insert(transaction)
        }
    }

    fun listenToSharedLedger(code: String, localCustomerId: Int) {
        if (!isUserSignedIn() || !code.matches(Regex("\\d{6}"))) return
        sharedListeners.remove(code)?.remove()
        val listener = firestore.collection("sharedLedgers").document(code).collection("transactions")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    snapshot.documents.forEach { doc ->
                        val syncKey = doc.getString("syncKey") ?: doc.id
                        val existing = transactionDao.getTransactionsByCustomerOnce(localCustomerId).firstOrNull { it.syncKey == syncKey }
                        if (doc.exists()) {
                            val transaction = Transaction(
                                id = existing?.id ?: -(kotlin.math.abs(syncKey.hashCode()) + 1),
                                customerId = localCustomerId,
                                amount = doc.getDouble("amount") ?: doc.getLong("amount")?.toDouble() ?: 0.0,
                                type = doc.getString("type") ?: "UDHAR",
                                note = doc.getString("note") ?: "",
                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                syncKey = syncKey
                            )
                            transactionDao.insert(transaction)
                        }
                    }
                }
            }
        sharedListeners[code] = listener
    }

    fun stopListening(code: String) {
        sharedListeners.remove(code)?.remove()
    }
}
