package com.aj.udharbook.sync

import com.aj.udharbook.dao.CustomerDao
import com.aj.udharbook.dao.TransactionDao
import com.aj.udharbook.model.Customer
import com.aj.udharbook.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirestoreSyncManager(
    private val customerDao: CustomerDao,
    private val transactionDao: TransactionDao
) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val sharedListeners = mutableMapOf<String, ListenerRegistration>()
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun isUserSignedIn(): Boolean = auth.currentUser != null
    fun getCurrentUserId(): String? = auth.currentUser?.uid
    suspend fun getCustomerByIdOnce(customerId: Int): Customer? = customerDao.getCustomerByIdOnce(customerId)

    private fun userDocument() = auth.currentUser?.uid?.let { uid -> firestore.collection("users").document(uid) }
    private fun requireUserDocument() = userDocument() ?: throw IllegalStateException("User is not signed in")

    suspend fun syncCustomer(customer: Customer) {
        if (customer.id <= 0) return
        requireUserDocument().collection("customers").document(customer.id.toString()).set(hashMapOf<String, Any>(
            "id" to customer.id, "name" to customer.name, "mobile" to customer.mobile,
            "address" to customer.address, "createdAt" to customer.createdAt, "sharedLedgerId" to customer.sharedLedgerId
        )).await()
    }

    suspend fun deleteCustomer(customerId: Int) {
        if (customerId <= 0) return
        requireUserDocument().collection("customers").document(customerId.toString()).delete().await()
    }

    suspend fun syncTransaction(transaction: Transaction) {
        if (transaction.id <= 0) return
        requireUserDocument().collection("transactions").document(transaction.id.toString()).set(hashMapOf<String, Any>(
            "id" to transaction.id, "customerId" to transaction.customerId, "amount" to transaction.amount,
            "type" to transaction.type, "note" to transaction.note, "timestamp" to transaction.timestamp,
            "syncKey" to transaction.syncKey
        )).await()
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
        sharedListeners.values.forEach { it.remove() }
        sharedListeners.clear()
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
                id = id, name = document.getString("name") ?: "", mobile = document.getString("mobile") ?: "",
                address = document.getString("address") ?: "", createdAt = document.getLong("createdAt") ?: System.currentTimeMillis(),
                sharedLedgerId = document.getString("sharedLedgerId") ?: ""
            )
        }
        val transactionSnapshot = user.collection("transactions").get().await()
        val cloudTransactions = transactionSnapshot.documents.mapNotNull { document ->
            val id = document.getLong("id")?.toInt() ?: document.id.toIntOrNull() ?: return@mapNotNull null
            val customerId = document.getLong("customerId")?.toInt() ?: return@mapNotNull null
            Transaction(
                id = id, customerId = customerId,
                amount = document.getDouble("amount") ?: document.getLong("amount")?.toDouble() ?: 0.0,
                type = document.getString("type") ?: "UDHAR", note = document.getString("note") ?: "",
                timestamp = document.getLong("timestamp") ?: System.currentTimeMillis(),
                syncKey = document.getString("syncKey") ?: ""
            )
        }
        if (customerDao.getAllCustomersOnce().isEmpty() && cloudCustomers.isNotEmpty()) customerDao.insertAll(cloudCustomers)
        if (transactionDao.getAllTransactionsOnce().isEmpty() && cloudTransactions.isNotEmpty()) transactionDao.insertAll(cloudTransactions)
        cloudCustomers.filter { SharedLedgerSecurity.isValidCode(it.sharedLedgerId) }.forEach { customer ->
            try {
                syncSharedLedgerToLocal(customer.sharedLedgerId, customer.id)
                listenToSharedLedger(customer.sharedLedgerId, customer.id)
            } catch (_: Exception) { }
        }
    }

    suspend fun createShareCode(customer: Customer): String {
        if (!isUserSignedIn()) throw IllegalStateException("Login required")
        val uid = getCurrentUserId() ?: throw IllegalStateException("Login required")
        val existing = customer.sharedLedgerId.trim()
        val code = if (SharedLedgerSecurity.isValidCode(existing)) existing else findAvailableCode()
        val ledgerRef = firestore.collection("sharedLedgers").document(code)
        val existingLedger = ledgerRef.get().await()
        if (!existingLedger.exists()) {
            ledgerRef.set(hashMapOf<String, Any>(
                "inviteCode" to code, "ownerUid" to uid, "ownerCustomerId" to customer.id,
                "customerName" to customer.name, "mobile" to customer.mobile.filter(Char::isDigit).takeLast(10),
                "address" to customer.address, "participantUids" to listOf(uid), "createdAt" to System.currentTimeMillis()
            )).await()
        } else if (existingLedger.getString("ownerUid") != uid) {
            throw IllegalStateException("Only the ledger owner can create or resend the invite")
        } else {
            ledgerRef.update(
                mapOf(
                    "ownerCustomerId" to customer.id,
                    "customerName" to customer.name,
                    "mobile" to customer.mobile.filter(Char::isDigit).takeLast(10),
                    "address" to customer.address
                )
            ).await()
        }
        val updatedCustomer = customer.copy(sharedLedgerId = code)
        customerDao.update(updatedCustomer)
        syncCustomer(updatedCustomer)
        publishCustomerTransactions(updatedCustomer)
        listenToSharedLedger(code, customer.id)
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
            if (transaction.syncKey.isBlank()) transactionDao.update(shared)
            firestore.collection("sharedLedgers").document(customer.sharedLedgerId).collection("transactions")
                .document(syncKey).set(transactionMap(shared)).await()
        }
    }

    suspend fun requestJoinShareCode(codeInput: String): String {
        if (!isUserSignedIn()) throw IllegalStateException("Login required")
        val code = codeInput.filter(Char::isDigit)
        if (!SharedLedgerSecurity.isValidCode(code)) throw IllegalArgumentException("Enter a valid 6-digit code")
        val uid = getCurrentUserId() ?: throw IllegalStateException("Login required")
        val requestRef = firestore.collection("sharedLedgers").document(code).collection("joinRequests").document(uid)
        val existing = requestRef.get().await()
        if (existing.exists()) return existing.getString("status") ?: SharedLedgerSecurity.PENDING
        requestRef.set(hashMapOf<String, Any>(
            "ledgerCode" to code,
            "requesterUid" to uid,
            "requesterName" to (auth.currentUser?.displayName ?: "App User"),
            "status" to SharedLedgerSecurity.PENDING,
            "createdAt" to System.currentTimeMillis()
        )).await()
        return SharedLedgerSecurity.PENDING
    }

    suspend fun getJoinRequestStatus(codeInput: String): String? {
        if (!isUserSignedIn()) throw IllegalStateException("Login required")
        val code = codeInput.filter(Char::isDigit)
        if (!SharedLedgerSecurity.isValidCode(code)) throw IllegalArgumentException("Enter a valid 6-digit code")
        val uid = getCurrentUserId() ?: throw IllegalStateException("Login required")
        val snapshot = firestore.collection("sharedLedgers").document(code).collection("joinRequests").document(uid).get().await()
        return if (snapshot.exists()) snapshot.getString("status") else null
    }

    suspend fun completeApprovedJoin(codeInput: String): Customer {
        if (!isUserSignedIn()) throw IllegalStateException("Login required")
        val code = codeInput.filter(Char::isDigit)
        if (!SharedLedgerSecurity.isValidCode(code)) throw IllegalArgumentException("Enter a valid 6-digit code")
        val uid = getCurrentUserId() ?: throw IllegalStateException("Login required")
        val requestRef = firestore.collection("sharedLedgers").document(code).collection("joinRequests").document(uid)
        val request = requestRef.get().await()
        if (!request.exists() || request.getString("status") != SharedLedgerSecurity.APPROVED) {
            throw IllegalStateException("Owner approval is still pending")
        }
        val ledgerRef = firestore.collection("sharedLedgers").document(code)
        val snapshot = ledgerRef.get().await()
        if (!snapshot.exists()) throw IllegalArgumentException("Shared ledger no longer exists")
        val name = snapshot.getString("customerName") ?: "Shared Customer"
        val mobile = snapshot.getString("mobile") ?: ""
        val address = snapshot.getString("address") ?: ""
        var customer = customerDao.getCustomerBySharedLedgerOnce(code) ?: customerDao.getCustomerByMobileOnce(mobile)
        if (customer == null) {
            val localId = customerDao.insert(Customer(name = name, mobile = mobile, address = address, sharedLedgerId = code)).toInt()
            customer = customerDao.getCustomerByIdOnce(localId) ?: throw IllegalStateException("Could not create local customer")
        } else if (customer.sharedLedgerId != code) {
            customer = customer.copy(sharedLedgerId = code)
            customerDao.update(customer)
        }
        syncSharedLedgerToLocal(code, customer.id)
        listenToSharedLedger(code, customer.id)
        return customer
    }

    suspend fun getOwnedJoinRequests(): List<SharedJoinRequest> {
        if (!isUserSignedIn()) throw IllegalStateException("Login required")
        val uid = getCurrentUserId() ?: throw IllegalStateException("Login required")
        val codes = customerDao.getAllCustomersOnce().map { it.sharedLedgerId }
            .filter { SharedLedgerSecurity.isValidCode(it) }.distinct()
        val results = mutableListOf<SharedJoinRequest>()
        codes.forEach { code ->
            val ledger = firestore.collection("sharedLedgers").document(code).get().await()
            if (ledger.getString("ownerUid") != uid) return@forEach
            val requests = ledger.reference.collection("joinRequests").get().await()
            requests.documents.forEach { doc ->
                results.add(SharedJoinRequest(
                    ledgerCode = code,
                    requesterUid = doc.getString("requesterUid") ?: doc.id,
                    requesterName = doc.getString("requesterName") ?: "App User",
                    status = doc.getString("status") ?: SharedLedgerSecurity.PENDING,
                    createdAt = doc.getLong("createdAt") ?: 0L
                ))
            }
        }
        return results.sortedByDescending { it.createdAt }
    }

    suspend fun approveJoinRequest(request: SharedJoinRequest) {
        updateJoinRequest(request, SharedLedgerSecurity.APPROVED)
    }

    suspend fun rejectJoinRequest(request: SharedJoinRequest) {
        updateJoinRequest(request, SharedLedgerSecurity.REJECTED)
    }

    private suspend fun updateJoinRequest(request: SharedJoinRequest, status: String) {
        if (!isUserSignedIn()) throw IllegalStateException("Login required")
        if (status != SharedLedgerSecurity.APPROVED && status != SharedLedgerSecurity.REJECTED) {
            throw IllegalArgumentException("Invalid join request status")
        }
        val uid = getCurrentUserId() ?: throw IllegalStateException("Login required")
        val ledgerRef = firestore.collection("sharedLedgers").document(request.ledgerCode)
        val requestRef = ledgerRef.collection("joinRequests").document(request.requesterUid)
        val ledger = ledgerRef.get().await()
        if (!ledger.exists() || ledger.getString("ownerUid") != uid) {
            throw IllegalStateException("Only the ledger owner can approve requests")
        }
        val participants = (ledger.get("participantUids") as? List<*>)
            ?.filterIsInstance<String>()?.toMutableList() ?: mutableListOf(uid)
        if (!participants.contains(uid)) participants.add(uid)
        if (status == SharedLedgerSecurity.APPROVED && !participants.contains(request.requesterUid)) {
            participants.add(request.requesterUid)
        }

        firestore.runBatch { batch ->
            if (status == SharedLedgerSecurity.APPROVED) {
                batch.update(ledgerRef, "participantUids", participants)
            }
            batch.update(requestRef, "status", status)
        }.await()
    }

    suspend fun syncSharedLedgerTransaction(transaction: Transaction) {
        if (transaction.syncKey.isBlank() || !isUserSignedIn()) return
        val customer = customerDao.getCustomerByIdOnce(transaction.customerId) ?: return
        val code = customer.sharedLedgerId.trim()
        if (!SharedLedgerSecurity.isValidCode(code)) return
        firestore.collection("sharedLedgers").document(code).collection("transactions").document(transaction.syncKey)
            .set(transactionMap(transaction)).await()
    }

    suspend fun deleteSharedLedgerTransaction(transaction: Transaction) {
        if (transaction.syncKey.isBlank() || !isUserSignedIn()) return
        val customer = customerDao.getCustomerByIdOnce(transaction.customerId) ?: return
        val code = customer.sharedLedgerId.trim()
        if (!SharedLedgerSecurity.isValidCode(code)) return
        firestore.collection("sharedLedgers").document(code).collection("transactions").document(transaction.syncKey).delete().await()
    }

    private fun transactionMap(transaction: Transaction): HashMap<String, Any> = hashMapOf(
        "syncKey" to transaction.syncKey, "customerId" to transaction.customerId, "amount" to transaction.amount,
        "type" to transaction.type, "note" to transaction.note, "timestamp" to transaction.timestamp
    )

    suspend fun syncSharedLedgerToLocal(code: String, localCustomerId: Int) {
        if (!SharedLedgerSecurity.isValidCode(code)) return
        val snapshot = firestore.collection("sharedLedgers").document(code).collection("transactions").get().await()
        val current = transactionDao.getTransactionsByCustomerOnce(localCustomerId).associateBy { it.syncKey }
        snapshot.documents.forEach { doc ->
            val syncKey = doc.getString("syncKey") ?: doc.id
            val existing = current[syncKey]
            transactionDao.insert(Transaction(
                id = existing?.id ?: -(kotlin.math.abs(syncKey.hashCode()) + 1), customerId = localCustomerId,
                amount = doc.getDouble("amount") ?: doc.getLong("amount")?.toDouble() ?: 0.0,
                type = doc.getString("type") ?: "UDHAR", note = doc.getString("note") ?: "",
                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(), syncKey = syncKey
            ))
        }
    }

    fun listenToSharedLedger(code: String, localCustomerId: Int) {
        if (!isUserSignedIn() || !SharedLedgerSecurity.isValidCode(code)) return
        sharedListeners.remove(code)?.remove()
        sharedListeners[code] = firestore.collection("sharedLedgers").document(code).collection("transactions")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                syncScope.launch {
                    snapshot.documentChanges.forEach { change ->
                        val doc = change.document
                        val syncKey = doc.getString("syncKey") ?: doc.id
                        when (change.type) {
                            com.google.firebase.firestore.DocumentChange.Type.ADDED,
                            com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                val existing = transactionDao.getTransactionsByCustomerOnce(localCustomerId)
                                    .firstOrNull { it.syncKey == syncKey }
                                transactionDao.insert(Transaction(
                                    id = existing?.id ?: -(kotlin.math.abs(syncKey.hashCode()) + 1),
                                    customerId = localCustomerId,
                                    amount = doc.getDouble("amount") ?: doc.getLong("amount")?.toDouble() ?: 0.0,
                                    type = doc.getString("type") ?: "UDHAR",
                                    note = doc.getString("note") ?: "",
                                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                    syncKey = syncKey
                                ))
                            }
                            com.google.firebase.firestore.DocumentChange.Type.REMOVED -> transactionDao.deleteBySyncKey(syncKey)
                        }
                    }
                }
            }
    }

    fun stopListening(code: String) { sharedListeners.remove(code)?.remove() }
}
